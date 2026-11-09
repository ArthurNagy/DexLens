package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentOfferUseCase
import com.arthurnagy.dexlens.core.domain.species.SearchSpeciesUseCase
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

/** Searches as the query changes. [PokedexState.query] is the only copy of the query. */
@HiltViewModel
internal class PokedexViewModel
    @Inject
    constructor(
        private val searchSpecies: SearchSpeciesUseCase,
        private val observeArtwork: ObserveArtworkUseCase,
        private val observeContentOffer: ObserveContentOfferUseCase,
    ) : ViewModel(),
        OrbitContainerHost<PokedexState, PokedexState, Nothing> {
        override val container = orbitContainer<PokedexState, Nothing>(PokedexState.Initial) {
            observeArtworkLocation()
            checkOffer()
            search(state.query)
        }

        private fun observeArtworkLocation() = intent {
            observeArtwork().collect { location -> reduce { state.copy(artwork = location) } }
        }

        private fun checkOffer() = intent {
            if (observeContentOffer().first()) reduce { state.copy(pendingOffer = true) }
        }

        /** A slower answer to an older query is dropped, so results always match the text. */
        private suspend fun Syntax<PokedexState, Nothing>.search(query: String) {
            searchSpecies(query)
                .onOk { results -> answer(query) { copy(loading = false, error = null, results = results) } }
                .onErr { error -> answer(query) { copy(loading = false, error = error, results = emptyList()) } }
        }

        private suspend fun Syntax<PokedexState, Nothing>.answer(
            query: String,
            update: PokedexState.() -> PokedexState,
        ) {
            reduce { if (state.query == query) state.update() else state }
        }

        fun onQueryChanged(query: String) = intent {
            if (query == state.query) return@intent
            reduce { state.copy(query = query) }
            search(query)
        }

        fun onOfferConsumed() = intent {
            reduce { state.copy(pendingOffer = false) }
        }

        fun onRetry() = intent {
            reduce { state.copy(loading = true, error = null) }
            search(state.query)
        }
    }
