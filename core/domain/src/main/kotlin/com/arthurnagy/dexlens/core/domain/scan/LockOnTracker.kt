package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId

/**
 * Decides when candidates are stable. A species locks once it appears in 3 of the last 5 frames. Others visible in the
 * deciding frame and already seen before it are offered with it as choices. It is an immutable value: [observe] returns
 * the tracker that includes the new frame.
 */
data class LockOnTracker(
    private val history: List<Set<SpeciesId>>,
) {
    /** The species locked as of the latest frame, or an empty set. */
    val locked: Set<SpeciesId>
        get() {
            val latest = history.lastOrNull() ?: return emptySet()
            val counts = history.flatten().groupingBy { it }.eachCount()
            val stable = counts.filterValues { it >= REQUIRED_FRAMES }.keys
            if (stable.isEmpty()) return emptySet()
            return stable + latest.filter { (counts[it] ?: 0) >= REQUIRED_FRAMES - 1 }
        }

    /** Adds one frame's species. Windows with nothing in them collapse to [Empty], so idle frames change nothing. */
    fun observe(frame: Set<SpeciesId>): LockOnTracker {
        val window = (history + listOf(frame)).takeLast(WINDOW_SIZE)
        return if (window.all { it.isEmpty() }) Empty else LockOnTracker(window)
    }

    companion object {
        val Empty = LockOnTracker(emptyList())

        private const val WINDOW_SIZE = 5
        private const val REQUIRED_FRAMES = 3
    }
}
