package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.model.Connectivity
import com.arthurnagy.dexlens.core.model.ContentPackState

@PreviewLightDark
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Composable
private fun SettingsPreview() {
    AppTheme { SettingsContent(SettingsState.Initial.copy(versionName = "0.1.0"), SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}

@PreviewLightDark
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun SettingsOfflinePreview() {
    AppTheme {
        SettingsContent(
            SettingsState.Initial.copy(content = ContentPackState.Interrupted(320, 2051), connectivity = Connectivity.Offline),
            SnackbarHostState(),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContentOfferOfflinePreview() {
    AppTheme {
        ContentOfferContent(
            state = ContentPackState.NotDownloaded,
            connectivity = Connectivity.Offline,
            networkSettingsUnavailable = false,
            onDownload = {},
            onConnect = {},
            onSkip = {},
            onHide = {},
        )
    }
}
