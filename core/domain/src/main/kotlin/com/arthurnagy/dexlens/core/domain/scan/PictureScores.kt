package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId

/** One species and how closely the frame resembles its best reference picture, as a cosine similarity. */
data class SpeciesScore(
    val speciesId: SpeciesId,
    val score: Float,
)

/**
 * The picture signal's verdict on one frame: a score for every species, so a name ranked below the first few can still
 * be checked against the picture.
 */
class PictureScores internal constructor(
    /** Indexed by National Dex number. A species without references stays at negative infinity. */
    private val bySpecies: FloatArray,
) {
    /** The species' score, or negative infinity when the gallery has no picture of it. */
    fun score(id: SpeciesId): Float = bySpecies.getOrElse(id.value) { Float.NEGATIVE_INFINITY }

    /** The [count] best-scoring species, best first. */
    fun top(count: Int): List<SpeciesScore> = bySpecies.indices
        .filter { bySpecies[it] > Float.NEGATIVE_INFINITY }
        .sortedByDescending { bySpecies[it] }
        .take(count)
        .map { SpeciesScore(SpeciesId(it), bySpecies[it]) }

    companion object {
        /** Scores for the given species only, every other species scoring nothing. For fakes and tests. */
        fun of(vararg scores: SpeciesScore): PictureScores {
            val array = FloatArray(SPECIES_SLOTS) { Float.NEGATIVE_INFINITY }
            scores.forEach { array[it.speciesId.value] = it.score }
            return PictureScores(array)
        }

        /** One slot per National Dex number up to [MAX_SPECIES_ID], plus the unused slot 0. */
        internal const val SPECIES_SLOTS = MAX_SPECIES_ID + 1
    }
}

/** The highest National Dex number the bundled data knows. */
internal const val MAX_SPECIES_ID = 1025

/**
 * Every score cut-off the scanner uses, in one place so tuning is a one-line change. The model's own tests on clean,
 * simulated captures suggested 0.94 and 0.957; phone photos of real game screens score lower even when the first guess is
 * right (a Sword / Shield battle scored 0.81, empty scenes about 0.75), so the values were lowered on device.
 */
data class RecognitionThresholds(
    /** A species counts as seen in the picture from this score. */
    val candidate: Float,
    /** A species can lock from the picture alone from this score, still needing four of the last five frames. */
    val imageOnly: Float,
    /** How far ahead of the others a species must be to count as a clear winner. */
    val leadMargin: Float,
) {
    companion object {
        val Default = RecognitionThresholds(candidate = 0.90f, imageOnly = 0.92f, leadMargin = 0.015f)
    }
}
