package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.TypeEffectiveness
import com.arthurnagy.dexlens.core.testing.sampleTypeChart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class CalculateTypeMatchupsTest {
    private val chart = sampleTypeChart()

    // Scenario: Dual type multiplies
    @Test
    fun dualTypeMultiplies() {
        val matchups = calculateTypeMatchups(chart, listOf(PokemonType.GRASS, PokemonType.POISON))
        assertTrue(TypeEffectiveness(PokemonType.FIRE, 2.0) in matchups.weaknesses)
        assertTrue(TypeEffectiveness(PokemonType.PSYCHIC, 2.0) in matchups.weaknesses)
        assertTrue(TypeEffectiveness(PokemonType.GRASS, 0.25) in matchups.resistances)
        assertTrue(TypeEffectiveness(PokemonType.WATER, 0.5) in matchups.resistances)
        assertEquals(0.25, matchups.resistances.first().multiplier, 0.0)
    }

    // Scenario: Four times weakness
    @Test
    fun fourTimesWeakness() {
        val matchups = calculateTypeMatchups(chart, listOf(PokemonType.BUG, PokemonType.STEEL))
        assertEquals(TypeEffectiveness(PokemonType.FIRE, 4.0), matchups.weaknesses.first())
    }

    // Scenario: Immunity
    @Test
    fun immunity() {
        val matchups = calculateTypeMatchups(chart, listOf(PokemonType.GHOST, PokemonType.POISON))
        assertEquals(listOf(PokemonType.NORMAL, PokemonType.FIGHTING), matchups.immunities)
    }
}
