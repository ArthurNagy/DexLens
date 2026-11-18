package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.arthurnagy.dexlens.core.designsystem.AppTheme

/** On a phone the entry fills the window on its own and shows its Back arrow. */
@PreviewLightDark
@Composable
private fun EntryLoadingPreview() {
    EntryLoading(showBackArrow = true)
}

/** Beside the home list on a tablet or an unfolded foldable, the entry shows no Back arrow. */
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Composable
private fun EntryLoadingBesideListPreview() {
    EntryLoading(showBackArrow = false)
}

@Composable
private fun EntryLoading(showBackArrow: Boolean) {
    AppTheme {
        EntryContent(
            state = EntryState.Loading,
            onNavigateBack = {},
            showBackArrow = showBackArrow,
            onOpenSpecies = {},
            onSelectVariety = {},
            onSelectGame = {},
        )
    }
}
