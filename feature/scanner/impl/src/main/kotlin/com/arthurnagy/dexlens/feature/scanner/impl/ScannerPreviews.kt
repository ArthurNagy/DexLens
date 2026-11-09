package com.arthurnagy.dexlens.feature.scanner.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.designsystem.CameraColors

@PreviewLightDark
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Composable
private fun ScannerPreview() {
    AppTheme {
        ScannerContent(
            state = ScannerState.Initial.copy(loading = false),
            permission = CameraPermission.GRANTED,
            cameraFailed = false,
            torchAvailable = true,
            debugEnabled = false,
            onNavigateBack = {},
            onToggleTorch = {},
            onRequestPermission = {},
            onOpenAppSettings = {},
            onOpenSpecies = {},
            onDismissResult = {},
            camera = { Box(Modifier.fillMaxSize().background(CameraColors.toolbar)) },
        )
    }
}
