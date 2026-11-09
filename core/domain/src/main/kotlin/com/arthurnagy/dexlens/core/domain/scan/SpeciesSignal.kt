package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId

/** What the camera saw in one frame. A future image-model signal can add the frame's pixels here. */
data class FrameObservation(
    val lines: List<String>,
)

/** A species one signal believes is in the frame. */
data class SpeciesCandidate(
    val speciesId: SpeciesId,
    val matchedText: String,
    val kind: MatchKind,
)

enum class MatchKind { EXACT, CONFUSABLE, FUZZY }

/** Why a piece of text did or didn't match. Only the debug overlay reads these. */
data class MatchDecision(
    val text: String,
    val outcome: MatchOutcome,
    val speciesIds: Set<SpeciesId>,
)

enum class MatchOutcome { EXACT, CONFUSABLE, FUZZY, AMBIGUOUS, IGNORED, NO_MATCH }

data class SignalResult(
    val candidates: List<SpeciesCandidate>,
    val decisions: List<MatchDecision>,
)

/**
 * One source of evidence about which species is in a frame. Text matching is the only signal today. An
 * image-recognition model can implement this later, and [LockOnTracker] would combine both without changing.
 */
fun interface SpeciesSignal {
    fun analyze(observation: FrameObservation): SignalResult
}
