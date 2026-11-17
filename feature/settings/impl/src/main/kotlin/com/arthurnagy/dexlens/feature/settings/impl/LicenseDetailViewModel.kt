package com.arthurnagy.dexlens.feature.settings.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.DispatcherProvider
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.about.GetOpenSourceLibraryUseCase
import com.arthurnagy.dexlens.core.model.OpenSourceLibraryDetail
import com.arthurnagy.dexlens.feature.settings.api.LicenseDetailKey
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/**
 * Loads one library and turns its texts into rows off the main thread. Keeps only the rows, so no text is held twice.
 */
@HiltViewModel(assistedFactory = LicenseDetailViewModel.Factory::class)
internal class LicenseDetailViewModel
    @AssistedInject
    constructor(
        @Assisted private val key: LicenseDetailKey,
        private val getLibrary: GetOpenSourceLibraryUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel(),
        OrbitContainerHost<LicenseDetailState, LicenseDetailState, Nothing> {
        override val container = orbitContainer<LicenseDetailState, Nothing>(LicenseDetailState.Initial) {
            load()
        }

        private fun load() = intent {
            getLibrary(key.libraryId)
                .onOk { detail ->
                    val rows = withContext(dispatchers.default) { detail.toRows() }
                    reduce {
                        state.copy(name = detail.library.name, version = detail.library.version, rows = rows, isLoading = false)
                    }
                }.onErr { error ->
                    reduce {
                        if (error == DomainError.NotFound) {
                            state.copy(isLoading = false, notFound = true)
                        } else {
                            state.copy(isLoading = false, pendingError = error)
                        }
                    }
                }
        }

        fun onErrorConsumed() = intent {
            reduce { state.copy(pendingError = null) }
        }

        @AssistedFactory
        interface Factory {
            fun create(key: LicenseDetailKey): LicenseDetailViewModel
        }
    }

/** The holder, then each licence's name and paragraphs, then each bundled notice's file name and paragraphs. */
internal fun OpenSourceLibraryDetail.toRows(): List<LicenceRow> = buildList {
    copyrightHolder?.let { add(LicenceRow.Holder(it)) }
    licenses.forEach { licence -> addSection(licence.name, licence.text) }
    notices.forEach { notice -> addSection(notice.fileName, notice.text) }
}

private fun MutableList<LicenceRow>.addSection(
    title: String,
    text: String,
) {
    add(LicenceRow.SectionTitle(title))
    text
        .split(BLANK_LINE)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { paragraph -> paragraph.chunkedAtLines().forEach { add(LicenceRow.Paragraph(it)) } }
}

/**
 * Cuts a long paragraph at line breaks into pieces of at most [MAX_ROW_LINES] lines and about [MAX_ROW_CHARS]
 * characters, so no single row is slow to lay out. Real notices hold paragraphs of 700 lines (Eigen's file list).
 */
internal fun String.chunkedAtLines(): List<String> {
    if (length <= MAX_ROW_CHARS && count { it == '\n' } < MAX_ROW_LINES) return listOf(this)
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    var lines = 0
    lineSequence().forEach { line ->
        if (current.isNotEmpty() && (lines >= MAX_ROW_LINES || current.length + line.length + 1 > MAX_ROW_CHARS)) {
            chunks += current.toString()
            current.clear()
            lines = 0
        }
        if (current.isNotEmpty()) current.append('\n')
        current.append(line)
        lines++
    }
    if (current.isNotEmpty()) chunks += current.toString()
    return chunks
}

/** A line break, any whitespace-only lines, and another line break. */
private val BLANK_LINE = Regex("""\r?\n[ \t\r]*\n""")

internal const val MAX_ROW_LINES = 40
internal const val MAX_ROW_CHARS = 4_000
