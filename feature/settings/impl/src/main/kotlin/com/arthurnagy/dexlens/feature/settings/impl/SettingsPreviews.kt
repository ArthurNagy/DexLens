package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.arthurnagy.dexlens.core.designsystem.AppTheme

@PreviewLightDark
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Composable
private fun SettingsPreview() {
    AppTheme { SettingsContent(SettingsState.Initial.copy(versionName = "0.1.0"), SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, {}) }
}
