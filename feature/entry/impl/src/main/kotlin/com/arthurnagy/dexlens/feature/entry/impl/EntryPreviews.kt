package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.arthurnagy.dexlens.core.designsystem.AppTheme

@PreviewLightDark
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Composable
private fun EntryLoadingPreview() {
    AppTheme {
        EntryContent(EntryState.Loading, onNavigateBack = {
        }, showBackArrow = true, onOpenSpecies = {}, onSelectVariety = {}, onSelectGame = {})
    }
}
