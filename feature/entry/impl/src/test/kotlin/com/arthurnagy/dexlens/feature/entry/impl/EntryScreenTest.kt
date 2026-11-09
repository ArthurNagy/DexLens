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
import com.arthurnagy.dexlens.core.domain.content.GetFlavorEntriesUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesDetailUseCase
import com.arthurnagy.dexlens.core.domain.species.GetTypeChartUseCase
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.Form
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeFlavorTextRepository
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.FakeTypeChartRepository
import com.arthurnagy.dexlens.core.testing.bulbasaurDetail
import com.arthurnagy.dexlens.core.testing.sampleTypeChart
import com.arthurnagy.dexlens.feature.entry.api.EntryKey
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
    private val loaded = EntryState.loaded(detail, sampleTypeChart(), ArtworkLocation(null), emptyList())

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
        val genderless = detail.copy(genderRatio = detail.genderRatio.copy(femaleEighths = null))
        show(EntryState.loaded(genderless, sampleTypeChart(), ArtworkLocation(null), emptyList()))
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
        val withMega = EntryState
            .loaded(detail.copy(varieties = detail.varieties + mega), sampleTypeChart(), ArtworkLocation(null), emptyList())
            .select(mega)
        show(withMega, onSelect = { selected = it })
        composeRule.onNodeWithTag("entry:variety:1").performClick()
        assertEquals(1, selected)
        composeRule.onAllNodesWithText("Mega Bulbasaur").assertCountEquals(2)
        composeRule.onNodeWithText("Fairy").assertExists()
        scrollTo("Total")
        composeRule.onNode(hasContentDescription("HP: 99", substring = true), useUnmergedTree = true).assertExists()
    }

    @Test
    fun rootScreenLoadsFromTheViewModel() {
        val viewModel = EntryViewModel(
            EntryKey(1),
            GetSpeciesDetailUseCase(FakeSpeciesRepository(details = mapOf(SpeciesId(1) to detail))),
            GetTypeChartUseCase(FakeTypeChartRepository(sampleTypeChart())),
            ObserveArtworkUseCase(FakeContentPackRepository()),
            GetFlavorEntriesUseCase(FakeFlavorTextRepository()),
        )
        composeRule.setContent { AppTheme { EntryScreen(viewModel = viewModel, onNavigateBack = {}, onOpenSpecies = {}) } }
        composeRule.waitUntil(WAIT_MS) { composeRule.onAllNodesWithText("Seed Pokémon").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Seed Pokémon").assertExists()
    }

    // Scenario: Entry without flavor text
    @Test
    fun flavorSectionIsAbsentWithoutDownload() {
        show(loaded)
        composeRule.onNodeWithText("Pokédex entries").assertDoesNotExist()
    }

    @Test
    fun flavorEntriesAreShownWithTheirGames() {
        val flavor = listOf(FlavorEntry("A seed grows.", listOf("Red", "Blue")))
        show(EntryState.loaded(detail, sampleTypeChart(), ArtworkLocation(null), flavor))
        scrollTo("A seed grows.")
        composeRule.onNodeWithText("Red, Blue").assertExists()
    }

    @Test
    fun formsAndOtherNamesAreListed() {
        val variety = detail.varieties.single()
        val forms =
            listOf(
                Form(1, "A", isDefault = true, isBattleOnly = false, isMega = false),
                Form(2, "B", isDefault = false, isBattleOnly = false, isMega = false),
            )
        val withForms = detail.copy(varieties = listOf(variety.copy(forms = forms)))
        show(EntryState.loaded(withForms, sampleTypeChart(), ArtworkLocation(null), emptyList()))
        scrollTo("Forms")
        composeRule.onNodeWithText("B").assertExists()
        scrollTo("Bulbizarre")
        composeRule.onNodeWithText("French").assertExists()
    }

    @Test
    fun aSpeciesWithoutEvolutionSaysSo() {
        val lone = detail.copy(evolutionLine = detail.evolutionLine.copy(children = emptyList()))
        show(EntryState.loaded(lone, sampleTypeChart(), ArtworkLocation(null), emptyList()))
        scrollTo("This species does not evolve.")
        composeRule.onNodeWithText("This species does not evolve.").assertExists()
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
    @Config(qualifiers = "w841dp-h701dp-xxhdpi")
    fun foldable() {
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

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
