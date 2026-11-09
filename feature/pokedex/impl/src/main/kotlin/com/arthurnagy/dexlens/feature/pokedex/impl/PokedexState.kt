package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult

/** UI state for [PokedexScreen]. [query] is the search text the current [results] answer. */
@Immutable
internal data class PokedexState(
    val loading: Boolean,
    val error: DomainError?,
    val query: String,
    val results: List<SpeciesSearchResult>,
) {
    companion object {
        val Initial = PokedexState(loading = true, error = null, query = "", results = emptyList())
    }
}
