package com.arthurnagy.dexlens.feature.scanner.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.scan.LockOnTracker
import com.arthurnagy.dexlens.core.domain.scan.MatchDecision
import com.arthurnagy.dexlens.core.domain.scan.SpeciesSignal
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSummary

/** A recognised line of text, in the coordinates of the cropped aiming-box image. */
@Immutable
internal data class AnalyzedLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** One frame's recognised text, with sizes for mapping it back onto the aiming box. */
@Immutable
internal data class AnalyzedFrame(
    val lines: List<AnalyzedLine>,
    val cropWidth: Int,
    val cropHeight: Int,
    val bufferWidth: Int,
    val bufferHeight: Int,
    val processingMillis: Long,
)

/** The latest frame and its match decisions, drawn by the debug-only overlay. */
@Immutable
internal data class ScanDebug(
    val frame: AnalyzedFrame,
    val decisions: List<MatchDecision>,
)

@Immutable
internal data class ScannerState(
    val loading: Boolean,
    val error: DomainError?,
    /** The matcher, once its name index has loaded. */
    val signal: SpeciesSignal?,
    val speciesById: Map<SpeciesId, SpeciesSummary>,
    /** Recent frames' candidates. Empty again once a species locks, so dismissing a result starts clean. */
    val lockOn: LockOnTracker,
    /** Locked species: empty while scanning. Analysis is paused while this is not empty. */
    val locked: List<SpeciesSummary>,
    /** True once after a lock, until the screen has played the haptic. */
    val pendingHaptic: Boolean,
    val torchOn: Boolean,
    val debug: ScanDebug?,
    val artwork: ArtworkLocation,
) {
    val paused: Boolean get() = locked.isNotEmpty()

    /** This state after one analysed frame. A frame already in flight when a result appears changes nothing. */
    fun afterFrame(
        candidates: Set<SpeciesId>,
        frameDebug: ScanDebug?,
    ): ScannerState {
        if (paused) return this
        val tracker = lockOn.observe(candidates)
        val species = tracker.locked.mapNotNull(speciesById::get).sortedBy { it.id.value }
        val scanned = copy(debug = frameDebug ?: debug)
        return if (species.isEmpty()) {
            scanned.copy(lockOn = tracker)
        } else {
            scanned.copy(lockOn = LockOnTracker.Empty, locked = species, pendingHaptic = true)
        }
    }

    companion object {
        val Initial = ScannerState(
            loading = true,
            error = null,
            signal = null,
            speciesById = emptyMap(),
            lockOn = LockOnTracker.Empty,
            locked = emptyList(),
            pendingHaptic = false,
            torchOn = false,
            debug = null,
            artwork = ArtworkLocation(null),
        )
    }
}
