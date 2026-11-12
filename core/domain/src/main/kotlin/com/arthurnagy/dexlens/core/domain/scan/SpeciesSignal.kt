package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId

/** The text read in one frame's aiming box. The picture signal reaches [combineSignals] separately. */
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

/**
 * Why a piece of text did or didn't match. [combineSignals] reads these to let the picture choose between several
 * species one piece of text matched.
 */
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

/** Finds species in one frame's text. [combineSignals] merges its result with the picture signal's. */
fun interface SpeciesSignal {
    fun analyze(observation: FrameObservation): SignalResult
}
