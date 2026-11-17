package com.arthurnagy.dexlens.core.data.about

import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.domain.DispatcherProvider
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.about.OpenSourceLibraryRepository
import com.arthurnagy.dexlens.core.model.LibraryLicense
import com.arthurnagy.dexlens.core.model.LibraryNotice
import com.arthurnagy.dexlens.core.model.OpenSourceLibrary
import com.arthurnagy.dexlens.core.model.OpenSourceLibraryDetail
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The libraries in the APK, from the files :app's build generates (#22): AboutLibraries' list in `raw/aboutlibraries`,
 * parsed once per process and shared by the list and detail screens, and the notices Google embeds in its AARs under
 * `assets/embedded_notices/`, read only when a library is opened.
 */
@Singleton
internal class BundledOpenSourceLibraryRepository
    @Inject
    constructor(
        private val source: NoticesSource,
        private val dispatchers: DispatcherProvider,
    ) : OpenSourceLibraryRepository {
        private val mutex = Mutex()
        private var parsed: List<Library>? = null

        override suspend fun libraries(): Result<List<OpenSourceLibrary>, DomainError> = parsedLibraries().map { libraries ->
            libraries.map { it.toModel() }
        }

        override suspend fun library(id: String): Result<OpenSourceLibraryDetail, DomainError> = parsedLibraries().andThen { libraries ->
            val library = libraries.firstOrNull { it.uniqueId == id } ?: return@andThen Err(DomainError.NotFound)
            catchStorage {
                withContext(dispatchers.io) {
                    OpenSourceLibraryDetail(
                        library = library.toModel(),
                        copyrightHolder = library.holder(),
                        licenses = library.licenses.map {
                            LibraryLicense(name = it.name, text = it.licenseContent.orEmpty().withoutTemplateCopyright())
                        },
                        notices = embeddedNotices(id),
                    )
                }
            }
        }

        /** The parsed list in name order, ignoring case; parsed on the first call only. */
        private suspend fun parsedLibraries(): Result<List<Library>, DomainError> = mutex.withLock {
            parsed?.let { return@withLock Ok(it) }
            catchStorage {
                withContext(dispatchers.io) {
                    val libraries = Libs.Builder().withJson(source.librariesJson()).build().libraries
                    // The parser logs malformed JSON and returns nothing, but every build bundles libraries.
                    check(libraries.isNotEmpty()) { "The bundled library list is empty or malformed" }
                    libraries.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                }
            }.map { libraries -> libraries.also { parsed = it } }
        }

        /** The notice files embedded in the library's artifact, skipping the offsets index that isn't text. */
        private fun embeddedNotices(id: String): List<LibraryNotice> {
            val index = Json.parseToJsonElement(source.asset(NOTICES_INDEX)).jsonObject
            return index
                .filterKeys { coordinate -> coordinate.substringBeforeLast(':') == id }
                .values
                .flatMap { files -> files.jsonObject.map { (fileName, path) -> fileName to path.jsonPrimitive.content } }
                .filterNot { (fileName, _) -> fileName == OFFSETS_INDEX }
                .distinctBy { (_, path) -> path }
                .map { (fileName, path) -> LibraryNotice(fileName = fileName, text = source.asset(path)) }
        }

        private fun Library.toModel() = OpenSourceLibrary(
            id = uniqueId,
            name = name,
            version = artifactVersion,
            licenseNames = licenses.map {
                it.name
            },
        )

        /** The organisation, else the developers, else unknown. */
        private fun Library.holder(): String? = organization?.name?.takeIf { it.isNotBlank() }
            ?: developers.mapNotNull { developer ->
                developer.name?.takeIf { it.isNotBlank() }
            }.takeIf { it.isNotEmpty() }?.joinToString(", ")

        private companion object {
            const val NOTICES_INDEX = "embedded_notices/index.json"
            const val OFFSETS_INDEX = "third_party_licenses.json"
        }
    }

/**
 * Removes an SPDX template's placeholder copyright line, such as BSD-3-Clause's `Copyright (c) <year> <owner>.` or ISC's
 * `Copyright <year> <owner>`: a line that, trimmed, starts with "Copyright" (any case) and holds both placeholders. A
 * real copyright line never holds them.
 */
internal fun String.withoutTemplateCopyright(): String = lineSequence()
    .filterNot { line ->
        line.trim().startsWith("Copyright", ignoreCase = true) && "<year>" in line && "<owner>" in line
    }.joinToString("\n")
    .trim()
