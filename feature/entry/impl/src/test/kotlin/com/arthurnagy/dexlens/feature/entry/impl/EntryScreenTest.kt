package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.testing.bulbasaurDetail
import com.arthurnagy.dexlens.core.testing.sampleTypeChart
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EntryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val detail = bulbasaurDetail()
    private val loaded = EntryState.loaded(detail, sampleTypeChart())

    private fun show(
        state: EntryState,
        onBack: (() -> Unit)? = {},
        onOpen: (Int) -> Unit = {},
        onSelect: (Int) -> Unit = {},
    ) {
        composeRule.setContent { AppTheme { EntryContent(state, onBack, onOpen, onSelect) } }
    }

    private fun scrollTo(text: String) {
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasText(text))
    }

    // Scenario: Entry shows full data
    @Test
    fun showsAllSections() {
        show(loaded)
        composeRule.onNodeWithText("#0001").assertExists()
        composeRule.onNodeWithText("Seed Pokémon").assertExists()
        composeRule.onNodeWithText("Grass").assertExists()
        composeRule.onNodeWithText("Poison").assertExists()
        scrollTo("0.7 m")
        composeRule.onNodeWithText("0.7 m").assertExists()
        composeRule.onNodeWithText("6.9 kg").assertExists()
        composeRule.onNodeWithText("Grass, Monster").assertExists()
        composeRule.onNodeWithText("87.5% male, 12.5% female", useUnmergedTree = true).assertExists()
        scrollTo("Total")
        listOf("HP", "Attack", "Defense", "Sp. Atk", "Sp. Def", "Speed").forEach { label ->
            composeRule.onNode(hasContentDescription(label, substring = true), useUnmergedTree = true).assertExists()
        }
        scrollTo("Overgrow")
        composeRule.onNodeWithText("Overgrow").assertExists()
        scrollTo("Chlorophyll")
        composeRule.onNodeWithText("Hidden").assertExists()
        scrollTo("Weak to")
        composeRule.onNodeWithText("Weak to").assertExists()
    }

    // Scenario: Genderless species
    @Test
    fun genderlessIsWorded() {
        show(EntryState.loaded(detail.copy(genderRatio = detail.genderRatio.copy(femaleEighths = null)), sampleTypeChart()))
        scrollTo("Genderless")
        composeRule.onNodeWithText("Genderless").assertExists()
    }

    // Scenario: Unknown species
    @Test
    fun errorOffersAWayBack() {
        var back = false
        show(EntryState.Failed(DomainError.NotFound), onBack = { back = true })
        composeRule.onNodeWithText("That item is no longer available.").assertExists()
        composeRule.onNodeWithText("Go back").performClick()
        assertEquals(true, back)
    }

    // Scenario: Evolution line with triggers
    @Test
    fun evolutionLineShowsTriggers() {
        show(loaded)
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:evolution:2"))
        composeRule.onNodeWithTag("entry:evolution:2").assertContentDescriptionContains("Level 16", substring = true)
    }

    // Scenario: Tapping evolution opens entry
    @Test
    fun tappingEvolutionOpensIt() {
        var opened = 0
        show(loaded, onOpen = { opened = it })
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:evolution:2"))
        composeRule.onNodeWithTag("entry:evolution:2").performClick()
        assertEquals(2, opened)
    }

    // Scenario: Selecting a variety updates the entry
    @Test
    fun selectingAVarietyChip() {
        var selected: Int? = null
        val mega = detail.varieties.single().copy(
            id = 2,
            name = "Mega Bulbasaur",
            isDefault = false,
            types = listOf(PokemonType.GRASS, PokemonType.FAIRY),
            stats = detail.varieties.single().stats.copy(hp = 99),
        )
        val withMega = EntryState.loaded(detail.copy(varieties = detail.varieties + mega), sampleTypeChart()).select(mega)
        show(withMega, onSelect = { selected = it })
        composeRule.onNodeWithTag("entry:variety:1").performClick()
        assertEquals(1, selected)
        composeRule.onAllNodesWithText("Mega Bulbasaur").assertCountEquals(2)
        composeRule.onNodeWithText("Fairy").assertExists()
        scrollTo("Total")
        composeRule.onNode(hasContentDescription("HP: 99", substring = true), useUnmergedTree = true).assertExists()
    }

    @Test
    fun abilityExpandsToFullText() {
        show(loaded)
        scrollTo("Overgrow")
        composeRule.onNodeWithText("Strengthens Grass moves at low HP.").performClick()
        composeRule.onNodeWithText("Strengthens Grass moves to inflict 1.5× damage at 1/3 max HP or less.").assertExists()
    }

    @Test
    fun loadingShowsNoList() {
        show(EntryState.Loading)
        composeRule.onNodeWithTag("entry:list").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Loading").assertExists()
    }

    // Scenario: Placeholder shown without art
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phone() {
        show(loaded)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tablet() {
        show(loaded)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun phoneDark() {
        show(loaded)
        composeRule.onRoot().captureRoboImage()
    }
}
