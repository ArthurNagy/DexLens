package com.arthurnagy.dexlens.core.domain.about

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.OpenSourceLibrary
import com.arthurnagy.dexlens.core.model.OpenSourceLibraryDetail
import com.github.michaelbull.result.Result

/** The third-party libraries bundled in the installed build, read from the APK only. */
interface OpenSourceLibraryRepository {
    /** Every bundled library, ordered by name ignoring case. */
    suspend fun libraries(): Result<List<OpenSourceLibrary>, DomainError>

    /** The library with [id] (`group:artifact`) with its texts, or [DomainError.NotFound] when it isn't bundled. */
    suspend fun library(id: String): Result<OpenSourceLibraryDetail, DomainError>
}
