package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.scan.LockOnTracker
import com.arthurnagy.dexlens.core.domain.scan.MatchDecision
import com.arthurnagy.dexlens.core.domain.scan.SpeciesSignal
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

/** What the scanner knows and has decided. Part of `PokedexState`, because a match fills the same list as a search. */
@Immutable
internal data class ScanState(
    val error: DomainError?,
    /** The matcher, once its name index has loaded. */
    val signal: SpeciesSignal?,
    val speciesById: Map<SpeciesId, SpeciesSummary>,
    /** Recent frames' candidates. Empty again once a species locks. */
    val lockOn: LockOnTracker,
    /** The species listed by the last lock that added one. It stays until it is replaced or cleared. */
    val match: List<SpeciesSummary>,
    /** True once after a lock that changed [match], until the screen has played the haptic. */
    val pendingHaptic: Boolean,
    val torchOn: Boolean,
    val debug: ScanDebug?,
) {
    /**
     * This state after one analysed frame. A lock replaces [match] only if it adds a species outside it, so a name that
     * stays in view doesn't buzz every few frames, and a screen flickering between two names can't shrink the list.
     */
    fun afterFrame(
        candidates: Set<SpeciesId>,
        frameDebug: ScanDebug?,
    ): ScanState {
        val tracker = lockOn.observe(candidates)
        val species = tracker.locked.mapNotNull(speciesById::get).sortedBy { it.id.value }
        val scanned = copy(debug = frameDebug ?: debug)
        val listed = match.mapTo(mutableSetOf()) { it.id }
        return when {
            species.isEmpty() -> scanned.copy(lockOn = tracker)
            species.all { it.id in listed } -> scanned.copy(lockOn = LockOnTracker.Empty)
            else -> scanned.copy(lockOn = LockOnTracker.Empty, match = species, pendingHaptic = true)
        }
    }

    /** No match and no half-finished lock, so scanning starts clean. */
    fun withoutMatch(): ScanState = copy(lockOn = LockOnTracker.Empty, match = emptyList(), pendingHaptic = false)

    companion object {
        val Initial = ScanState(
            error = null,
            signal = null,
            speciesById = emptyMap(),
            lockOn = LockOnTracker.Empty,
            match = emptyList(),
            pendingHaptic = false,
            torchOn = false,
            debug = null,
        )
    }
}
