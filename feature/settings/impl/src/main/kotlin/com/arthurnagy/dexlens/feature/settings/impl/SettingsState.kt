package com.arthurnagy.dexlens.feature.settings.impl

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.Connectivity
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.ThemeMode

@Immutable
internal data class SettingsState(
    val themeMode: ThemeMode,
    val scannerEnabled: Boolean,
    val content: ContentPackState,
    /** Starts [Connectivity.Online], so the first frame looks as it did before connectivity was read: a tap then still works as today. */
    val connectivity: Connectivity,
    /** The installed version, or null while unknown or when it can't be read: then no version line shows. */
    val versionName: String?,
    val pendingError: DomainError?,
    /** A one-shot message to show, cleared by `onMessageConsumed()`. */
    @StringRes val pendingMessage: Int?,
) {
    companion object {
        val Initial = SettingsState(
            themeMode = ThemeMode.SYSTEM,
            scannerEnabled = true,
            content = ContentPackState.NotDownloaded,
            connectivity = Connectivity.Online,
            versionName = null,
            pendingError = null,
            pendingMessage = null,
        )
    }
}
