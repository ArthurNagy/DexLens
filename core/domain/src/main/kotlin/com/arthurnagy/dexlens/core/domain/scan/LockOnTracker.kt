package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId

/**
 * Decides when candidates are stable. A species locks once it appears in [requiredFrames] of the last [windowSize]
 * frames. Others visible in the deciding frame and already seen before it are offered with it as choices.
 */
class LockOnTracker(
    private val windowSize: Int = DEFAULT_WINDOW,
    private val requiredFrames: Int = DEFAULT_REQUIRED,
) {
    private val history = ArrayDeque<Set<SpeciesId>>()

    /** Adds one frame's species and returns those that are now locked, or an empty set. */
    fun observe(frame: Set<SpeciesId>): Set<SpeciesId> {
        history.addLast(frame)
        while (history.size > windowSize) history.removeFirst()
        val counts = history.flatten().groupingBy { it }.eachCount()
        val locked = counts.filterValues { it >= requiredFrames }.keys
        if (locked.isEmpty()) return emptySet()
        val nearlyLocked = frame.filter { (counts[it] ?: 0) >= requiredFrames - 1 }
        return locked + nearlyLocked
    }

    fun reset() = history.clear()

    private companion object {
        const val DEFAULT_WINDOW = 5
        const val DEFAULT_REQUIRED = 3
    }
}
