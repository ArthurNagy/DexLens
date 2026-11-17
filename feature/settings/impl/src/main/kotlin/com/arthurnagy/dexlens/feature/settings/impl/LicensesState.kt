package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.OpenSourceLibrary

@Immutable
internal data class LicensesState(
    val libraries: List<OpenSourceLibrary>,
    val isLoading: Boolean,
    val pendingError: DomainError?,
) {
    companion object {
        val Initial = LicensesState(libraries = emptyList(), isLoading = true, pendingError = null)
    }
}
