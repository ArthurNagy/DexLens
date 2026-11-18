package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.ObserveScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentOfferUseCase
import com.arthurnagy.dexlens.core.domain.scan.BuildSpeciesSignalUseCase
import com.arthurnagy.dexlens.core.domain.scan.FrameObservation
import com.arthurnagy.dexlens.core.domain.scan.combineSignals
import com.arthurnagy.dexlens.core.domain.species.GetGenerationStartsUseCase
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesListUseCase
import com.arthurnagy.dexlens.core.domain.species.SearchSpeciesUseCase
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.AnalyzedFrame
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import timber.log.Timber
import javax.inject.Inject

/**
 * Fills one list from two inputs: the typed query and the camera. [PokedexState.query] is the only copy of the query.
 * [onFrame] runs on the camera's analysis thread, so the matcher never touches the main thread.
 */
@HiltViewModel
internal class PokedexViewModel
    @Inject
    constructor(
        private val searchSpecies: SearchSpeciesUseCase,
        private val observeArtwork: ObserveArtworkUseCase,
        private val observeContentOffer: ObserveContentOfferUseCase,
        private val observeScannerEnabled: ObserveScannerEnabledUseCase,
        private val buildSignal: BuildSpeciesSignalUseCase,
        private val getSpeciesList: GetSpeciesListUseCase,
        private val getGenerationStarts: GetGenerationStartsUseCase,
    ) : ViewModel(),
        OrbitContainerHost<PokedexState, PokedexState, Nothing> {
        override val container = orbitContainer<PokedexState, Nothing>(PokedexState.Initial) {
            observeArtworkLocation()
            observeScannerSwitch()
            checkOffer()
            loadMatcher()
            loadGenerations()
            search(state.query)
        }

        private fun observeArtworkLocation() = intent {
            observeArtwork().collect { location -> reduce { state.copy(artwork = location) } }
        }

        /** Turning the scanner off also ends what it was doing, so turning it back on starts clean. */
        private fun observeScannerSwitch() = intent {
            observeScannerEnabled().collect { enabled ->
                reduce {
                    val scan = if (enabled) state.scan else state.scan.withoutMatch().copy(torchOn = false)
                    state.copy(scannerEnabled = enabled, scan = scan)
                }
            }
        }

        private fun checkOffer() = intent {
            if (observeContentOffer().first()) reduce { state.copy(pendingOffer = true) }
        }

        private fun loadMatcher() = intent {
            buildSignal()
                .andThen { built -> getSpeciesList().map { list -> built to list } }
                .onOk { (built, list) ->
                    reduce { state.copy(scan = state.scan.copy(error = null, signal = built, speciesById = list.associateBy { it.id })) }
                }.onErr { error -> reduce { state.copy(scan = state.scan.copy(error = error)) } }
        }

        /** Without them the list still works, just without the generation thumb, so a failure is only logged. */
        private fun loadGenerations() = intent {
            getGenerationStarts()
                .onOk { starts -> if (starts.isNotEmpty()) reduce { state.copy(generations = starts) } }
                .onErr { error -> Timber.w("generation starts unavailable: %s", error) }
        }

        /** A slower answer to an older query is dropped, so results always match the text. */
        private suspend fun Syntax<PokedexState, Nothing>.search(query: String) {
            searchSpecies(query)
                .onOk { results -> answer(query) { copy(loading = false, error = null, results = results, answeredQuery = query) } }
                .onErr { error -> answer(query) { copy(loading = false, error = error, results = emptyList(), answeredQuery = query) } }
        }

        private suspend fun Syntax<PokedexState, Nothing>.answer(
            query: String,
            update: PokedexState.() -> PokedexState,
        ) {
            reduce { if (state.query == query) state.update() else state }
        }

        /** Typed text takes over from a match in the same step, so the two never disagree. */
        fun onQueryChanged(query: String) = intent {
            if (query == state.query) return@intent
            reduce { state.copy(query = query, scan = if (query.isBlank()) state.scan else state.scan.withoutMatch()) }
            search(query)
        }

        /** Called for every analysed frame. */
        fun onFrame(frame: AnalyzedFrame) {
            val current = container.stateFlow.value
            val signal = current.scan.signal ?: return
            if (!current.scanning) return
            val evidence = combineSignals(signal.analyze(FrameObservation(frame.lines)), frame.pictureScores)
            if (!evidence.isEmpty()) Timber.d("frame evidence %s", evidence.species)
            // Checked again here: typing may have started while the frame was being read.
            intent {
                reduce {
                    if (!state.scanning) return@reduce state
                    val scan = state.scan.afterFrame(evidence)
                    if (scan.match != state.scan.match) Timber.i("locked %s", scan.match.map { it.name })
                    state.copy(scan = scan)
                }
            }
        }

        private val PokedexState.scanning: Boolean get() = scannerEnabled == true && query.isBlank()

        fun onMatchCleared() = intent {
            reduce { state.copy(scan = state.scan.withoutMatch()) }
        }

        fun onHapticConsumed() = intent {
            reduce { state.copy(scan = state.scan.copy(pendingHaptic = false)) }
        }

        fun onTorchToggled() = intent {
            reduce { state.copy(scan = state.scan.copy(torchOn = !state.scan.torchOn)) }
        }

        fun onOfferConsumed() = intent {
            reduce { state.copy(pendingOffer = false) }
        }

        fun onRetry() = intent {
            reduce { state.copy(loading = true, error = null) }
            search(state.query)
        }
    }
