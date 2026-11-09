package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.model.EvolutionMethod
import com.arthurnagy.dexlens.core.model.EvolutionTrigger
import com.arthurnagy.dexlens.core.model.Gender
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.RelativePhysicalStats
import com.arthurnagy.dexlens.core.model.TimeOfDay
import com.arthurnagy.dexlens.core.testing.levelUpMethod
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class EvolutionTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val base: EvolutionMethod = levelUpMethod(1).copy(minLevel = null)

    private fun assertText(
        expected: String,
        method: EvolutionMethod,
    ) {
        composeRule.setContent { Text(evolutionMethodText(method)) }
        composeRule.onNodeWithText(expected).assertExists()
    }

    private fun word(
        trigger: EvolutionTrigger,
        expected: String,
        adjust: EvolutionMethod.() -> EvolutionMethod = { this },
    ) = assertText(expected, base.copy(trigger = trigger).adjust())

    // Scenario: Evolution line with triggers
    @Test
    fun levelUp() = assertText("Level 16", levelUpMethod(16))

    @Test
    fun plainLevelUp() = word(EvolutionTrigger.LEVEL_UP, "Level up")

    @Test
    fun trade() = word(EvolutionTrigger.TRADE, "Trade")

    @Test
    fun tradeHolding() = word(EvolutionTrigger.TRADE, "Trade holding Metal Coat") { copy(heldItem = "Metal Coat") }

    @Test
    fun useItem() = word(EvolutionTrigger.USE_ITEM, "Use Water Stone") { copy(item = "Water Stone") }

    @Test
    fun shed() = word(EvolutionTrigger.SHED, "Empty party slot and a spare Poké Ball")

    @Test
    fun spin() = word(EvolutionTrigger.SPIN, "Spin around")

    @Test
    fun towers() {
        word(EvolutionTrigger.TOWER_OF_DARKNESS, "Train at the Tower of Darkness")
    }

    @Test
    fun towerOfWaters() = word(EvolutionTrigger.TOWER_OF_WATERS, "Train at the Tower of Waters")

    @Test
    fun criticalHits() = word(EvolutionTrigger.THREE_CRITICAL_HITS, "Land 3 critical hits in one battle")

    @Test
    fun takeDamage() = word(
        EvolutionTrigger.TAKE_DAMAGE,
        "Take 49 damage, then pass under the stone archway",
    ) { copy(minDamageTaken = 49) }

    @Test
    fun inBattle() = word(EvolutionTrigger.IN_BATTLE_LEVEL_UP, "Level up during battle")

    @Test
    fun agile() = word(EvolutionTrigger.AGILE_STYLE_MOVE, "Use Psyshield Bash in Agile Style") { copy(usedMove = "Psyshield Bash") }

    @Test
    fun strong() = word(EvolutionTrigger.STRONG_STYLE_MOVE, "Use Barb Barrage in Strong Style") { copy(usedMove = "Barb Barrage") }

    @Test
    fun recoil() = word(EvolutionTrigger.RECOIL_DAMAGE, "Take 294 recoil damage") { copy(minDamageTaken = 294) }

    @Test
    fun useMove() = word(EvolutionTrigger.USE_MOVE, "Use Rage Fist 20 times") { copy(usedMove = "Rage Fist", minMoveCount = 20) }

    @Test
    fun bisharp() = word(EvolutionTrigger.THREE_DEFEATED_BISHARP, "Defeat 3 Bisharp that lead Pawniard")

    @Test
    fun coins() = word(EvolutionTrigger.GIMMIGHOUL_COINS, "Collect 999 Gimmighoul Coins")

    @Test
    fun candies() = word(EvolutionTrigger.MELTAN_CANDIES, "Feed 400 Meltan Candies")

    @Test
    fun other() = word(EvolutionTrigger.OTHER, "Special conditions")

    @Test
    fun conditionsAreJoined() = assertText(
        "Level up, holding Oval Stone, at night",
        base.copy(heldItem = "Oval Stone", timeOfDay = TimeOfDay.NIGHT),
    )

    @Test
    fun allConditions() = assertText(
        "Level up, during the day, knowing Rollout, knowing a Fairy move, with high friendship, with high Beauty, " +
            "with high affection, with Attack above Defense, with Remoraid in the party, with a Dark type in the party, " +
            "for Karrablast, at Mt. Coronet, in Galar, female only, in the rain, with the console upside down, after 1000 steps",
        base.copy(
            timeOfDay = TimeOfDay.DAY,
            knownMove = "Rollout",
            knownMoveType = PokemonType.FAIRY,
            minHappiness = 160,
            minBeauty = 171,
            minAffection = 2,
            relativePhysicalStats = RelativePhysicalStats.ATTACK_HIGHER,
            partySpecies = "Remoraid",
            partyType = PokemonType.DARK,
            tradeSpecies = "Karrablast",
            location = "Mt. Coronet",
            region = "Galar",
            gender = Gender.FEMALE,
            needsRain = true,
            turnUpsideDown = true,
            minSteps = 1000,
        ),
    )

    @Test
    fun duskDefenseAndMale() = assertText(
        "Level up, at dusk, with Defense above Attack, male only",
        base.copy(timeOfDay = TimeOfDay.DUSK, relativePhysicalStats = RelativePhysicalStats.DEFENSE_HIGHER, gender = Gender.MALE),
    )

    @Test
    fun equalStats() = assertText("Level up, with equal Attack and Defense", base.copy(relativePhysicalStats = RelativePhysicalStats.EQUAL))
}
