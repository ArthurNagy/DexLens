package com.arthurnagy.dexlens.feature.scanner.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.scan.BuildSpeciesSignalUseCase
import com.arthurnagy.dexlens.core.domain.scan.FrameObservation
import com.arthurnagy.dexlens.core.domain.scan.LockOnTracker
import com.arthurnagy.dexlens.core.domain.scan.SpeciesSignal
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesListUseCase
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

/**
 * Turns recognised text into a lock-on decision. [onFrame] runs on the camera's analysis thread, so the matcher never
 * touches the main thread. Only the lock itself and debug output reach the UI state.
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
        @Volatile
        private var signal: SpeciesSignal? = null

        @Volatile
        private var summaries: Map<SpeciesId, SpeciesSummary> = emptyMap()

        @Volatile
        private var paused = false

        private val tracker = LockOnTracker()

        override val container = orbitContainer<ScannerState, Nothing>(ScannerState.Initial) {
            load()
        }

        fun load() = intent {
            buildSignal()
                .andThen { built -> getSpeciesList().map { list -> built to list } }
                .onOk { (built, list) ->
                    signal = built
                    summaries = list.associateBy { it.id }
                    val artwork = observeArtwork().first()
                    reduce { state.copy(loading = false, error = null, artwork = artwork) }
                }.onErr { error -> reduce { state.copy(loading = false, error = error) } }
        }

        /** Called for every analysed frame. [debugEnabled] keeps the trace out of release state. */
        fun onFrame(
            frame: AnalyzedFrame,
            debugEnabled: Boolean,
        ) {
            val current = signal ?: return
            if (paused) return
            val result = current.analyze(FrameObservation(frame.lines.map { it.text }))
            val locked = synchronized(tracker) { tracker.observe(result.candidates.map { it.speciesId }.toSet()) }
            if (debugEnabled) intent { reduce { state.copy(debug = ScanDebug(frame, result.decisions)) } }
            if (locked.isEmpty()) return
            val species = locked.mapNotNull { summaries[it] }.sortedBy { it.id.value }
            if (species.isEmpty()) return
            paused = true
            intent { reduce { state.copy(locked = species, pendingHaptic = true) } }
        }

        fun onHapticConsumed() = intent {
            reduce { state.copy(pendingHaptic = false) }
        }

        /** Closes the result sheet and scans again from a clean history. */
        fun onResultDismissed() = intent {
            synchronized(tracker) { tracker.reset() }
            paused = false
            reduce { state.copy(locked = emptyList(), pendingHaptic = false) }
        }

        fun onTorchToggled() = intent {
            reduce { state.copy(torchOn = !state.torchOn) }
        }
    }
