package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.designsystem.CameraColors
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraPermission
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraStatus

private val previewState = PokedexState.Initial.copy(
    loading = false,
    scannerEnabled = true,
    results = listOf(
        SpeciesSearchResult(SpeciesSummary(SpeciesId(1), "Bulbasaur", listOf(PokemonType.GRASS, PokemonType.POISON)), matchedName = null),
        SpeciesSearchResult(SpeciesSummary(SpeciesId(25), "Pikachu", listOf(PokemonType.ELECTRIC)), matchedName = null),
    ),
)

@PreviewLightDark
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Composable
private fun PokedexPreview() {
    AppTheme {
        PokedexContent(
            state = previewState,
            textFieldState = TextFieldState(),
            camera = CameraStatus(CameraPermission.GRANTED, failed = false, torchAvailable = true),
            canNavigateBack = false,
            onOpenSpecies = {},
            onOpenSettings = {},
            onRetry = {},
            onClearMatch = {},
            onToggleTorch = {},
            onRequestPermission = {},
            onOpenAppSettings = {},
            cameraUprightPortrait = { true },
            viewfinder = { Box(Modifier.fillMaxSize().background(CameraColors.toolbar)) },
        )
    }
}
