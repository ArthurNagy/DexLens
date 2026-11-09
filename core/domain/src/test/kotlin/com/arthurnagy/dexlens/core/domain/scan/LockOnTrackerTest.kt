package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LockOnTrackerTest {
    private val pikachu = SpeciesId(25)
    private val pidgey = SpeciesId(16)
    private val tracker = LockOnTracker()

    // Scenario: Lock after repeated frames
    @Test
    fun locksAfterThreeOfFive() {
        assertTrue(tracker.observe(setOf(pikachu)).isEmpty())
        assertTrue(tracker.observe(emptySet()).isEmpty())
        assertTrue(tracker.observe(setOf(pikachu)).isEmpty())
        assertEquals(setOf(pikachu), tracker.observe(setOf(pikachu)))
    }

    // Scenario: Single frame does not lock
    @Test
    fun singleFrameDoesNotLock() {
        assertTrue(tracker.observe(setOf(pikachu)).isEmpty())
        assertTrue(tracker.observe(emptySet()).isEmpty())
        assertTrue(tracker.observe(emptySet()).isEmpty())
    }

    // Scenario: Several names offered as choices
    @Test
    fun severalNamesOffered() {
        repeat(2) { tracker.observe(setOf(pikachu, pidgey)) }
        assertEquals(setOf(pikachu, pidgey), tracker.observe(setOf(pikachu, pidgey)))
    }

    // Scenario: Nickname keeps scanning
    @Test
    fun noCandidatesNeverLock() {
        repeat(10) { assertTrue(tracker.observe(emptySet()).isEmpty()) }
    }

    @Test
    fun oldFramesExpire() {
        tracker.observe(setOf(pikachu))
        tracker.observe(setOf(pikachu))
        repeat(4) { tracker.observe(emptySet()) }
        assertTrue(tracker.observe(setOf(pikachu)).isEmpty())
    }

    // Scenario: Dismissing resumes scanning
    @Test
    fun resetClearsHistory() {
        repeat(2) { tracker.observe(setOf(pikachu)) }
        tracker.reset()
        assertTrue(tracker.observe(setOf(pikachu)).isEmpty())
    }
}
