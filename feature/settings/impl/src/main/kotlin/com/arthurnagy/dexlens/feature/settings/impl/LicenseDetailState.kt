package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError

/**
 * A library's licence page as rows, so a notice of a few megabytes renders lazily rather than as one text. [name] and
 * [version] head the page; [notFound] replaces the rows with a message when the library isn't bundled.
 */
@Immutable
internal data class LicenseDetailState(
    val name: String?,
    val version: String?,
    val rows: List<LicenceRow>,
    val isLoading: Boolean,
    val notFound: Boolean,
    val pendingError: DomainError?,
) {
    companion object {
        val Initial = LicenseDetailState(name = null, version = null, rows = emptyList(), isLoading = true, notFound = false, pendingError = null)
    }
}

/** One row of a licence page. */
@Immutable
internal sealed interface LicenceRow {
    /** The copyright holder's name. */
    data class Holder(
        val text: String,
    ) : LicenceRow

    /** A licence's name, or a bundled notice's file name. */
    data class SectionTitle(
        val text: String,
    ) : LicenceRow

    /** One paragraph of a licence or notice text. */
    data class Paragraph(
        val text: String,
    ) : LicenceRow
}
