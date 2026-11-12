package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.ThemeMode

@Immutable
internal data class SettingsState(
    val themeMode: ThemeMode,
    val scannerEnabled: Boolean,
    val content: ContentPackState,
    val pendingError: DomainError?,
) {
    companion object {
        val Initial = SettingsState(
            themeMode = ThemeMode.SYSTEM,
            scannerEnabled = true,
            content = ContentPackState.NotDownloaded,
            pendingError = null,
        )
    }
}
