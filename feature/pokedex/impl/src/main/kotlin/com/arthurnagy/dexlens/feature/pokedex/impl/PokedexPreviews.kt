package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.model.SpeciesSummary

private val previewState = PokedexState.Initial.copy(
    loading = false,
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
    AppTheme { PokedexContent(previewState, TextFieldState(), {}, {}, {}, {}) }
}
