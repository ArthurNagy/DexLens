package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LockOnTrackerTest {
    private val pikachu = 25
    private val pidgey = 16
    private val eevee = 133
    private val vaporeon = 134

    private val none = FrameEvidence.Empty

    private fun named(vararg ids: Int) = FrameEvidence(ids.associate { SpeciesId(it) to Evidence.NAME })

    private fun pictured(vararg ids: Int) = FrameEvidence(ids.associate { SpeciesId(it) to Evidence.PICTURE })

    private fun agreeing(vararg ids: Int) = FrameEvidence(ids.associate { SpeciesId(it) to Evidence.BOTH })

    private fun after(vararg frames: FrameEvidence): LockOnTracker = frames.fold(LockOnTracker.Empty) { tracker, frame ->
        tracker.observe(frame)
    }

    // Scenario: Lock after repeated frames
    @Test
    fun locksAfterThreeOfFive() {
        assertTrue(after(named(pikachu), none, named(pikachu)).lockedIds.isEmpty())
        assertEquals(setOf(pikachu), after(named(pikachu), none, named(pikachu), named(pikachu)).lockedIds)
    }

    // Scenario: Single frame does not lock
    @Test
    fun singleFrameDoesNotLock() {
        assertTrue(after(named(pikachu), none, none).lockedIds.isEmpty())
    }

    // Scenario: Several names offered as choices
    @Test
    fun severalNamesOffered() {
        val both = named(pikachu, pidgey)
        assertEquals(setOf(pikachu, pidgey), after(both, both, both).lockedIds)
    }

    @Test
    fun aNameNotSeenBeforeTheDecidingFrameIsNotOffered() {
        assertEquals(setOf(pikachu), after(named(pikachu), named(pikachu), named(pikachu, pidgey)).lockedIds)
    }

    // Scenario: Nickname keeps scanning
    @Test
    fun noCandidatesNeverLock() {
        assertTrue(after(*Array(10) { none }).lockedIds.isEmpty())
    }

    // Scenario: Sprite without a name locks
    @Test
    fun pictureAloneLocksAfterFourOfFive() {
        assertEquals(setOf(pikachu), after(pictured(pikachu), pictured(pikachu), none, pictured(pikachu), pictured(pikachu)).lockedIds)
    }

    // Scenario: Three picture frames are not enough
    @Test
    fun threePictureFramesDoNotLock() {
        assertTrue(after(pictured(pikachu), none, pictured(pikachu), none, pictured(pikachu)).lockedIds.isEmpty())
    }

    // Scenario: Weak picture score does not lock
    @Test
    fun picturesBelowTheImageOnlyScoreNeverLock() {
        // combineSignals leaves a frame empty when the best score is below the image-only threshold.
        assertTrue(after(*Array(5) { combineSignals(SignalResult(emptyList(), emptyList()), weak) }).lockedIds.isEmpty())
    }

    // Scenario: Close picture scores offered as choices
    @Test
    fun closePicturesLockTogether() {
        val close = pictured(pikachu, pidgey)
        assertEquals(setOf(pidgey, pikachu), after(close, close, close, close).lockedIds)
    }

    // Scenario: At most three picture choices
    @Test
    fun atMostThreePicturesLock() {
        val ids = (1..5).toList()
        // Each frame offers at most three species, so no window can hold four species four times.
        val frames = listOf(
            pictured(ids[0], ids[1], ids[2]),
            pictured(ids[0], ids[1], ids[3]),
            pictured(ids[0], ids[2], ids[3]),
            pictured(ids[1], ids[2], ids[3]),
            pictured(ids[0], ids[1], ids[2]),
        )
        val locked = after(*frames.toTypedArray()).lockedIds
        assertEquals(setOf(ids[0], ids[1], ids[2]), locked)
    }

    // Scenario: Picture-only species does not ride along
    @Test
    fun aPictureOnlySpeciesIsNotOfferedWithANameLock() {
        val locked = after(pictured(pidgey), pictured(pidgey), named(pikachu), pictured(pidgey), named(pikachu), named(pikachu)).lockedIds
        assertEquals(setOf(pikachu), locked)
    }

    // Scenario: Agreement locks after two frames
    @Test
    fun agreementLocksAfterTwoFrames() {
        assertTrue(after(agreeing(pikachu)).lockedIds.isEmpty())
        assertEquals(setOf(pikachu), after(agreeing(pikachu), none, agreeing(pikachu)).lockedIds)
    }

    // Scenario: Name alone still needs three frames
    @Test
    fun nameAloneNeedsThreeFrames() {
        assertTrue(after(named(pikachu), named(pikachu)).lockedIds.isEmpty())
    }

    // Scenario: Name and picture disagree
    @Test
    fun theNameWinsADisagreement() {
        // combineSignals drops Vaporeon from frames that read EEVEE, so it only has the two frames without a name.
        val frames = arrayOf(named(eevee), pictured(vaporeon), named(eevee), pictured(vaporeon), named(eevee))
        assertEquals(setOf(eevee), after(*frames).lockedIds)
    }

    @Test
    fun idleFramesLeaveTheTrackerEmpty() {
        assertSame(LockOnTracker.Empty, LockOnTracker.Empty.observe(none))
    }

    @Test
    fun oldFramesExpire() {
        val tracker = after(named(pikachu), named(pikachu), none, none, none, none)
        assertTrue(tracker.observe(named(pikachu)).lockedIds.isEmpty())
    }

    @Test
    fun aWindowThatEmptiesCollapses() {
        val frames = Array(6) { index -> if (index == 0) named(pikachu) else none }
        assertSame(LockOnTracker.Empty, after(*frames))
    }

    private val weak = PictureScores.of(SpeciesScore(SpeciesId(pikachu), 0.95f))

    private val LockOnTracker.lockedIds: Set<Int> get() = locked.map { it.value }.toSet()
}
