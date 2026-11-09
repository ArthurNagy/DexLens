package com.arthurnagy.dexlens.core.data.species

import com.arthurnagy.dexlens.core.database.EvolutionEntity
import com.arthurnagy.dexlens.core.database.FormEntity
import com.arthurnagy.dexlens.core.model.EvolutionTrigger
import com.arthurnagy.dexlens.core.model.Gender
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.RelativePhysicalStats
import com.arthurnagy.dexlens.core.model.TimeOfDay
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class SpeciesMappersTest {
    private fun entity(trigger: String) = EvolutionEntity(
        1,
        2,
        1,
        trigger,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        needsRain = false,
        turnUpsideDown = false,
        usedMove = null,
        minMoveCount = null,
        minSteps = null,
        minDamageTaken = null,
    )

    @Test
    fun everyTriggerMaps() {
        val expected = mapOf(
            "level-up" to EvolutionTrigger.LEVEL_UP,
            "trade" to EvolutionTrigger.TRADE,
            "use-item" to EvolutionTrigger.USE_ITEM,
            "shed" to EvolutionTrigger.SHED,
            "spin" to EvolutionTrigger.SPIN,
            "tower-of-darkness" to EvolutionTrigger.TOWER_OF_DARKNESS,
            "tower-of-waters" to EvolutionTrigger.TOWER_OF_WATERS,
            "three-critical-hits" to EvolutionTrigger.THREE_CRITICAL_HITS,
            "take-damage" to EvolutionTrigger.TAKE_DAMAGE,
            "in-battle-level-up" to EvolutionTrigger.IN_BATTLE_LEVEL_UP,
            "agile-style-move" to EvolutionTrigger.AGILE_STYLE_MOVE,
            "strong-style-move" to EvolutionTrigger.STRONG_STYLE_MOVE,
            "recoil-damage" to EvolutionTrigger.RECOIL_DAMAGE,
            "use-move" to EvolutionTrigger.USE_MOVE,
            "three-defeated-bisharp" to EvolutionTrigger.THREE_DEFEATED_BISHARP,
            "gimmighoul-coins" to EvolutionTrigger.GIMMIGHOUL_COINS,
            "meltan-candies" to EvolutionTrigger.MELTAN_CANDIES,
            "something-new" to EvolutionTrigger.OTHER,
        )
        expected.forEach { (identifier, trigger) -> assertEquals(trigger, entity(identifier).toMethod(emptyMap()).trigger, identifier) }
    }

    @Test
    fun conditionsMap() {
        val method = entity("level-up")
            .copy(
                timeOfDay = "night",
                gender = 1,
                relativePhysicalStats = -1,
                knownMoveType = 18,
                partyType = 17,
                partySpeciesId = 223,
                tradeSpeciesId = 588,
            ).toMethod(mapOf(223 to "Remoraid", 588 to "Karrablast"))
        assertEquals(TimeOfDay.NIGHT, method.timeOfDay)
        assertEquals(Gender.FEMALE, method.gender)
        assertEquals(RelativePhysicalStats.DEFENSE_HIGHER, method.relativePhysicalStats)
        assertEquals(PokemonType.FAIRY, method.knownMoveType)
        assertEquals(PokemonType.DARK, method.partyType)
        assertEquals("Remoraid", method.partySpecies)
        assertEquals("Karrablast", method.tradeSpecies)
    }

    @Test
    fun remainingValueMappings() {
        assertEquals(TimeOfDay.DAY, entity("x").copy(timeOfDay = "day").toMethod(emptyMap()).timeOfDay)
        assertEquals(TimeOfDay.DUSK, entity("x").copy(timeOfDay = "dusk").toMethod(emptyMap()).timeOfDay)
        assertNull(entity("x").copy(timeOfDay = "unknown").toMethod(emptyMap()).timeOfDay)
        assertEquals(Gender.MALE, entity("x").copy(gender = 2).toMethod(emptyMap()).gender)
        assertNull(entity("x").copy(gender = 9).toMethod(emptyMap()).gender)
        assertEquals(
            RelativePhysicalStats.ATTACK_HIGHER,
            entity("x").copy(relativePhysicalStats = 1).toMethod(emptyMap()).relativePhysicalStats,
        )
        assertEquals(RelativePhysicalStats.EQUAL, entity("x").copy(relativePhysicalStats = 0).toMethod(emptyMap()).relativePhysicalStats)
    }

    @Test
    fun formsMap() {
        val form = FormEntity(1, 1, "a", "A", true, false, true, 1, null, null).toModel()
        assertEquals("A", form.name)
        assertEquals(true, form.isMega)
    }
}
