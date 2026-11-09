package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult

/** UI state for [PokedexScreen]. [results] answer [query], or the query before it until the new answer arrives. */
@Immutable
internal data class PokedexState(
    val loading: Boolean,
    val error: DomainError?,
    val query: String,
    val results: List<SpeciesSearchResult>,
    val artwork: ArtworkLocation,
    /** True once on first launch, until the screen has shown the download offer. */
    val pendingOffer: Boolean,
) {
    companion object {
        val Initial =
            PokedexState(
                loading = true,
                error = null,
                query = "",
                results = emptyList(),
                artwork = ArtworkLocation(null),
                pendingOffer = false,
            )
    }
}
