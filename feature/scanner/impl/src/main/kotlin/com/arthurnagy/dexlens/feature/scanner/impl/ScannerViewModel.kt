package com.arthurnagy.dexlens.feature.scanner.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.scan.BuildSpeciesSignalUseCase
import com.arthurnagy.dexlens.core.domain.scan.FrameObservation
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesListUseCase
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

/**
 * Turns recognised text into a lock-on decision. [onFrame] runs on the camera's analysis thread, so the matcher never
 * touches the main thread. Everything it needs and decides lives in [ScannerState].
 */
@HiltViewModel
internal class ScannerViewModel
    @Inject
    constructor(
        private val buildSignal: BuildSpeciesSignalUseCase,
        private val getSpeciesList: GetSpeciesListUseCase,
        private val observeArtwork: ObserveArtworkUseCase,
    ) : ViewModel(),
        OrbitContainerHost<ScannerState, ScannerState, Nothing> {
        override val container = orbitContainer<ScannerState, Nothing>(ScannerState.Initial) {
            load()
            observeArtworkLocation()
        }

        private fun load() = intent {
            buildSignal()
                .andThen { built -> getSpeciesList().map { list -> built to list } }
                .onOk { (built, list) ->
                    reduce { state.copy(loading = false, error = null, signal = built, speciesById = list.associateBy { it.id }) }
                }.onErr { error -> reduce { state.copy(loading = false, error = error) } }
        }

        private fun observeArtworkLocation() = intent {
            observeArtwork().collect { location -> reduce { state.copy(artwork = location) } }
        }

        /** Called for every analysed frame. [debugEnabled] keeps the trace out of release state. */
        fun onFrame(
            frame: AnalyzedFrame,
            debugEnabled: Boolean,
        ) {
            val current = container.stateFlow.value
            val signal = current.signal ?: return
            if (current.paused) return
            val result = signal.analyze(FrameObservation(frame.lines.map { it.text }))
            val candidates = result.candidates.map { it.speciesId }.toSet()
            val debug = if (debugEnabled) ScanDebug(frame, result.decisions) else null
            intent { reduce { state.afterFrame(candidates, debug) } }
        }

        fun onHapticConsumed() = intent {
            reduce { state.copy(pendingHaptic = false) }
        }

        /** Closes the result sheet and scans again. */
        fun onResultDismissed() = intent {
            reduce { state.copy(locked = emptyList(), pendingHaptic = false) }
        }

        fun onTorchToggled() = intent {
            reduce { state.copy(torchOn = !state.torchOn) }
        }
    }
