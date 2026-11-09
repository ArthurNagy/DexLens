package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentOfferUseCase
import com.arthurnagy.dexlens.core.domain.species.SearchSpeciesUseCase
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

/** Searches as the query changes. `collectLatest` drops an older query's work, so results always match the text. */
@HiltViewModel
internal class PokedexViewModel
    @Inject
    constructor(
        private val searchSpecies: SearchSpeciesUseCase,
        private val observeArtwork: ObserveArtworkUseCase,
        private val observeContentOffer: ObserveContentOfferUseCase,
    ) : ViewModel(),
        OrbitContainerHost<PokedexState, PokedexState, Nothing> {
        private val queries = MutableStateFlow("")
        private val retries = MutableStateFlow(0)

        override val container = orbitContainer<PokedexState, Nothing>(PokedexState.Initial) {
            observeQueries()
            observeArtworkLocation()
            checkOffer()
        }

        private fun observeArtworkLocation() = intent {
            observeArtwork().collect { location -> reduce { state.copy(artwork = location) } }
        }

        private fun checkOffer() = intent {
            if (observeContentOffer().first()) reduce { state.copy(pendingOffer = true) }
        }

        private fun observeQueries() = intent {
            combine(queries, retries) { query, _ -> query }.collectLatest { query -> runSearch(query) }
        }

        private suspend fun Syntax<PokedexState, Nothing>.runSearch(query: String) {
            searchSpecies(query)
                .onOk { results -> reduce { state.copy(loading = false, error = null, query = query, results = results) } }
                .onErr { error -> reduce { state.copy(loading = false, error = error, query = query, results = emptyList()) } }
        }

        fun onQueryChanged(query: String) {
            queries.value = query
        }

        fun onOfferConsumed() = intent {
            reduce { state.copy(pendingOffer = false) }
        }

        fun onRetry() = intent {
            reduce { state.copy(loading = true, error = null) }
            retries.value += 1
        }
    }
