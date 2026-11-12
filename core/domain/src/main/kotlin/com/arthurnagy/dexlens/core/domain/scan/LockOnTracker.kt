package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId

/**
 * Decides when candidates are stable, over the last [WINDOW_SIZE] frames. A species locks once:
 * - its name was read in [NAME_FRAMES] frames, or
 * - its name and its picture agreed in [AGREEING_FRAMES] frames, or
 * - the picture alone picked it in [PICTURE_FRAMES] frames.
 *
 * Other species whose name is visible in the deciding frame, and was read once before, are offered with it as choices.
 * A species seen only in the picture never rides along this way. It is an immutable value: [observe] returns the
 * tracker that includes the new frame.
 */
data class LockOnTracker(
    private val history: List<FrameEvidence>,
) {
    /** The species locked as of the latest frame, or an empty set. */
    val locked: Set<SpeciesId>
        get() {
            val latest = history.lastOrNull() ?: return emptySet()
            val named = count { it == Evidence.NAME || it == Evidence.BOTH }
            val agreeing = count { it == Evidence.BOTH }
            val pictured = count { it == Evidence.PICTURE }
            val ids = named.keys + pictured.keys
            val stable = ids.filterTo(LinkedHashSet()) { id ->
                named.getOrZero(id) >= NAME_FRAMES ||
                    agreeing.getOrZero(id) >= AGREEING_FRAMES ||
                    pictured.getOrZero(id) >= PICTURE_FRAMES
            }
            if (stable.isEmpty()) return emptySet()
            val companions = latest.species
                .filter { (id, evidence) -> evidence != Evidence.PICTURE && named.getOrZero(id) >= NAME_FRAMES - 1 }
                .keys
            return stable + companions
        }

    /** Adds one frame. Windows with nothing in them collapse to [Empty], so idle frames change nothing. */
    fun observe(frame: FrameEvidence): LockOnTracker {
        val window = (history + listOf(frame)).takeLast(WINDOW_SIZE)
        return if (window.all { it.isEmpty() }) Empty else LockOnTracker(window)
    }

    /** In how many frames of the window each species had evidence matching [predicate]. */
    private fun count(predicate: (Evidence) -> Boolean): Map<SpeciesId, Int> = history
        .flatMap { frame -> frame.species.filterValues(predicate).keys }
        .groupingBy { it }
        .eachCount()

    private fun Map<SpeciesId, Int>.getOrZero(id: SpeciesId): Int = this[id] ?: 0

    companion object {
        val Empty = LockOnTracker(emptyList())

        private const val WINDOW_SIZE = 5
        private const val NAME_FRAMES = 3
        private const val AGREEING_FRAMES = 2
        private const val PICTURE_FRAMES = 4
    }
}
