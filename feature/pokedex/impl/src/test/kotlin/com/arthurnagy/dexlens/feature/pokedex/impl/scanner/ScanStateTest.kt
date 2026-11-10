package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import com.arthurnagy.dexlens.core.domain.scan.LockOnTracker
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.arthurnagy.dexlens.core.testing.speciesSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class ScanStateTest {
    private val pikachu = speciesSummary(25, "Pikachu", PokemonType.ELECTRIC)
    private val pidgey = speciesSummary(16, "Pidgey", PokemonType.NORMAL, PokemonType.FLYING)
    private val idle = ScanState.Initial.copy(speciesById = mapOf(pikachu.id to pikachu, pidgey.id to pidgey))
    private val matched = idle.copy(match = listOf(pikachu))

    private fun ScanState.frames(
        count: Int,
        vararg seen: SpeciesSummary,
    ): ScanState = (1..count).fold(this) { state, _ -> state.afterFrame(seen.mapTo(mutableSetOf()) { it.id }, frameDebug = null) }

    // Scenario: Analysis continues during a match
    @Test
    fun framesAreStillAnalysedDuringAMatch() {
        assertNotEquals(matched.lockOn, matched.frames(1, pidgey).lockOn)
    }

    // Scenario: Different name replaces the match
    @Test
    fun aLockOutsideTheMatchReplacesItAndBuzzes() {
        val after = matched.frames(3, pidgey)
        assertEquals(listOf(pidgey), after.match)
        assertTrue(after.pendingHaptic)
        assertEquals(LockOnTracker.Empty, after.lockOn)
    }

    // Scenario: Same name changes nothing
    @Test
    fun aLockInsideTheMatchChangesNothing() {
        val after = matched.frames(3, pikachu)
        assertEquals(listOf(pikachu), after.match)
        assertFalse(after.pendingHaptic)
        assertEquals(LockOnTracker.Empty, after.lockOn)
    }

    // Scenario: Same name changes nothing
    @Test
    fun aLockOnPartOfASeveralNameMatchCannotShrinkIt() {
        val several = idle.copy(match = listOf(pidgey, pikachu))
        assertEquals(listOf(pidgey, pikachu), several.frames(3, pikachu).match)
    }

    // Scenario: Match stays when the name leaves
    @Test
    fun theMatchStaysWhenNothingIsInView() {
        val after = matched.frames(6)
        assertEquals(listOf(pikachu), after.match)
        assertFalse(after.pendingHaptic)
    }

    @Test
    fun withoutMatchStartsClean() {
        val cleared = matched.copy(pendingHaptic = true).frames(1, pidgey).withoutMatch()
        assertEquals(emptyList(), cleared.match)
        assertFalse(cleared.pendingHaptic)
        assertEquals(LockOnTracker.Empty, cleared.lockOn)
    }
}
