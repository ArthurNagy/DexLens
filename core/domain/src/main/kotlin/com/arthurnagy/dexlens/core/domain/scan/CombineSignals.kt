package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId

/** What supports a species in one frame. */
enum class Evidence {
    /** The species' name was read, and the picture didn't agree or wasn't available. */
    NAME,

    /** The picture alone, in a frame where no name was read. */
    PICTURE,

    /** The name was read and the picture agrees. */
    BOTH,
}

/** The species one frame supports, and how. */
data class FrameEvidence(
    val species: Map<SpeciesId, Evidence>,
) {
    fun isEmpty(): Boolean = species.isEmpty()

    companion object {
        val Empty = FrameEvidence(emptyMap())
    }
}

/**
 * Combines one frame's text and picture signals. A name counts for more than the picture:
 * - the picture narrows a piece of text that matched several species, when one of them clearly leads;
 * - in a frame with any name, only named species count, so a name always beats a disagreeing picture;
 * - in a frame without a name, the picture alone counts once it is very sure, offering at most [MAX_PICTURE_CHOICES]
 *   species when none clearly leads.
 *
 * [picture] is null when the picture signal had no answer for the frame, which leaves today's text-only behaviour.
 */
fun combineSignals(
    text: SignalResult,
    picture: PictureScores?,
    thresholds: RecognitionThresholds = RecognitionThresholds.Default,
): FrameEvidence {
    val names = LinkedHashSet<SpeciesId>()
    for (decision in text.decisions) {
        if (decision.speciesIds.isEmpty()) continue
        val narrowed = if (decision.speciesIds.size > 1) picture?.clearWinner(decision.speciesIds, thresholds) else null
        when {
            narrowed != null -> names += narrowed
            decision.outcome == MatchOutcome.AMBIGUOUS -> Unit
            else -> names += decision.speciesIds
        }
    }
    if (names.isNotEmpty()) {
        return FrameEvidence(
            names.associateWith { id ->
                if (picture != null && picture.score(id) >= thresholds.candidate) Evidence.BOTH else Evidence.NAME
            },
        )
    }
    val ranked = picture?.top(MAX_PICTURE_CHOICES) ?: return FrameEvidence.Empty
    val best = ranked.firstOrNull()?.score ?: return FrameEvidence.Empty
    if (best < thresholds.imageOnly) return FrameEvidence.Empty
    return FrameEvidence(
        ranked
            .filter { it.score >= thresholds.imageOnly && best - it.score <= thresholds.leadMargin }
            .associate { it.speciesId to Evidence.PICTURE },
    )
}

/** The one species among [ids] the picture clearly prefers, or null when it isn't sure enough to choose. */
private fun PictureScores.clearWinner(
    ids: Set<SpeciesId>,
    thresholds: RecognitionThresholds,
): SpeciesId? {
    val ranked = ids.map { it to score(it) }.sortedByDescending { it.second }
    val (best, bestScore) = ranked.first()
    val runnerUp = ranked.getOrNull(1)?.second ?: Float.NEGATIVE_INFINITY
    return best.takeIf { bestScore >= thresholds.candidate && bestScore - runnerUp >= thresholds.leadMargin }
}

/** The most species the picture alone offers at once. */
internal const val MAX_PICTURE_CHOICES = 3
