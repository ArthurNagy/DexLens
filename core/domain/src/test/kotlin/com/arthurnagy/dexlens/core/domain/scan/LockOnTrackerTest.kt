package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LockOnTrackerTest {
    private val pikachu = SpeciesId(25)
    private val pidgey = SpeciesId(16)

    private fun after(vararg frames: Set<SpeciesId>): LockOnTracker = frames.fold(LockOnTracker.Empty) { tracker, frame ->
        tracker.observe(frame)
    }

    // Scenario: Lock after repeated frames
    @Test
    fun locksAfterThreeOfFive() {
        assertTrue(after(setOf(pikachu), emptySet(), setOf(pikachu)).locked.isEmpty())
        assertEquals(setOf(pikachu), after(setOf(pikachu), emptySet(), setOf(pikachu), setOf(pikachu)).locked)
    }

    // Scenario: Single frame does not lock
    @Test
    fun singleFrameDoesNotLock() {
        assertTrue(after(setOf(pikachu), emptySet(), emptySet()).locked.isEmpty())
    }

    // Scenario: Several names offered as choices
    @Test
    fun severalNamesOffered() {
        val both = setOf(pikachu, pidgey)
        assertEquals(both, after(both, both, both).locked)
    }

    @Test
    fun aNameNotSeenBeforeTheDecidingFrameIsNotOffered() {
        assertEquals(setOf(pikachu), after(setOf(pikachu), setOf(pikachu), setOf(pikachu, pidgey)).locked)
    }

    // Scenario: Nickname keeps scanning
    @Test
    fun noCandidatesNeverLock() {
        assertTrue(after(*Array(10) { emptySet() }).locked.isEmpty())
    }

    @Test
    fun idleFramesLeaveTheTrackerEmpty() {
        assertSame(LockOnTracker.Empty, LockOnTracker.Empty.observe(emptySet()))
    }

    @Test
    fun oldFramesExpire() {
        val tracker = after(setOf(pikachu), setOf(pikachu), emptySet(), emptySet(), emptySet(), emptySet())
        assertTrue(tracker.observe(setOf(pikachu)).locked.isEmpty())
    }

    @Test
    fun aWindowThatEmptiesCollapses() {
        val frames = Array(6) { index -> if (index == 0) setOf(pikachu) else emptySet() }
        assertSame(LockOnTracker.Empty, after(*frames))
    }
}
