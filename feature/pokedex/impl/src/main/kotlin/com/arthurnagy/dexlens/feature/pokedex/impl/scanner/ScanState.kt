package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.scan.FrameEvidence
import com.arthurnagy.dexlens.core.domain.scan.LockOnTracker
import com.arthurnagy.dexlens.core.domain.scan.PictureScores
import com.arthurnagy.dexlens.core.domain.scan.SpeciesScore
import com.arthurnagy.dexlens.core.domain.scan.SpeciesSignal
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSummary

/** What one frame's aiming box showed: the lines of text read, and the picture scores if the picture signal answered. */
@Immutable
internal data class AnalyzedFrame(
    val lines: List<String>,
    val pictureScores: PictureScores?,
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
    /** The latest frame's best picture scores, kept only while the scores readout is on. */
    val pictureScores: List<SpeciesScore>,
) {
    /**
     * This state after one analysed frame. A lock replaces [match] only if it adds a species outside it, so a name that
     * stays in view doesn't buzz every few frames, and a screen flickering between two names can't shrink the list.
     */
    fun afterFrame(evidence: FrameEvidence): ScanState {
        val tracker = lockOn.observe(evidence)
        val species = tracker.locked.mapNotNull(speciesById::get).sortedBy { it.id.value }
        val listed = match.mapTo(mutableSetOf()) { it.id }
        return when {
            species.isEmpty() -> copy(lockOn = tracker)
            species.all { it.id in listed } -> copy(lockOn = LockOnTracker.Empty)
            else -> copy(lockOn = LockOnTracker.Empty, match = species, pendingHaptic = true)
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
            pictureScores = emptyList(),
        )
    }
}
