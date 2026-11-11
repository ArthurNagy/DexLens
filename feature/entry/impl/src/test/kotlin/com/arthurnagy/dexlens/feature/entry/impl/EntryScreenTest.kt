package com.arthurnagy.dexlens.feature.entry.impl

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveFlavorEntriesUseCase
import com.arthurnagy.dexlens.core.domain.game.GetInGameDetailsUseCase
import com.arthurnagy.dexlens.core.domain.game.SetPreferredGameUseCase
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesDetailUseCase
import com.arthurnagy.dexlens.core.domain.species.GetTypeChartUseCase
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.EncounterMethod
import com.arthurnagy.dexlens.core.model.Form
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.LevelUpMove
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeFlavorTextRepository
import com.arthurnagy.dexlens.core.testing.FakeGameRepository
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.FakeTypeChartRepository
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import com.arthurnagy.dexlens.core.testing.RedBlue
import com.arthurnagy.dexlens.core.testing.ScarletViolet
import com.arthurnagy.dexlens.core.testing.SwordShield
import com.arthurnagy.dexlens.core.testing.bulbasaurDetail
import com.arthurnagy.dexlens.core.testing.sampleGameDetails
import com.arthurnagy.dexlens.core.testing.sampleTypeChart
import com.arthurnagy.dexlens.feature.entry.api.EntryKey
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertEquals

private const val PICTURE_TIMEOUT_MS = 5_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EntryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val detail = bulbasaurDetail()
    private val loaded = EntryState.loaded(detail, sampleTypeChart(), ArtworkLocation(null), null)
    private val described = loaded.copy(
        description = "A strange seed was planted on its back at birth.",
        inGame = InGameState(listOf(ScarletViolet, SwordShield, RedBlue), RedBlue, sampleGameDetails(RedBlue)),
    )

    private fun inGame(game: Game) =
        loaded.copy(inGame = InGameState(listOf(ScarletViolet, SwordShield, RedBlue), game, sampleGameDetails(game)))

    private fun show(
        state: EntryState,
        onBack: (() -> Unit)? = {},
        onOpen: (Int) -> Unit = {},
        onSelect: (Int) -> Unit = {},
        onSelectGame: (Int) -> Unit = {},
    ) {
        composeRule.setContent { AppTheme { EntryContent(state, onBack, onOpen, onSelect, onSelectGame) } }
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
        show(EntryState.loaded(genderless, sampleTypeChart(), ArtworkLocation(null), null))
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
            .loaded(detail.copy(varieties = detail.varieties + mega), sampleTypeChart(), ArtworkLocation(null), null)
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
            ObserveFlavorEntriesUseCase(FakeFlavorTextRepository()),
            GetInGameDetailsUseCase(
                FakeGameRepository(gamesByVariety = mapOf(1 to listOf(SwordShield))),
                FakeUserPreferencesRepository(ThemeMode.SYSTEM),
            ),
            SetPreferredGameUseCase(FakeUserPreferencesRepository(ThemeMode.SYSTEM)),
        )
        composeRule.setContent { AppTheme { EntryScreen(viewModel = viewModel, onNavigateBack = {}, onOpenSpecies = {}) } }
        composeRule.waitUntil(WAIT_MS) { composeRule.onAllNodesWithText("Seed Pokémon").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Seed Pokémon").assertExists()
        // The section is a lazy item further down, so it can only be found by scrolling once it has loaded.
        composeRule.waitUntil(WAIT_MS) {
            runCatching { composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:game-picker")) }.isSuccess
        }
        composeRule.onNodeWithText("Sword / Shield").assertExists()
    }

    // Scenario: No description without the download
    // Scenario: Entry without flavor text
    @Test
    fun noDescriptionWithoutDownload() {
        show(loaded)
        composeRule.onNodeWithTag("entry:description").assertDoesNotExist()
        composeRule.onNodeWithText("Pokédex entries").assertDoesNotExist()
    }

    // Scenario: Description from downloaded text
    @Test
    fun theDescriptionSitsUnderTheHeader() {
        show(described)
        composeRule.onNodeWithTag("entry:description").assertTextEquals("A strange seed was planted on its back at birth.")
    }

    /** Charizard-like entry with one alternate variety, [alternateId], and real picture files for [stored] ids. */
    private fun withPictures(
        alternateId: Int,
        vararg stored: Int,
    ): EntryState {
        val root = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "content-$alternateId").apply { deleteRecursively() }
        val png = ByteArrayOutputStream().also {
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        stored.forEach { id -> File(root, "artwork/$id.webp").apply { parentFile?.mkdirs() }.writeBytes(png.toByteArray()) }
        val mega = detail.varieties.single().copy(id = alternateId, name = "Mega Bulbasaur", isDefault = false)
        return EntryState
            .loaded(detail.copy(varieties = detail.varieties + mega), sampleTypeChart(), ArtworkLocation(root.path), null)
            .select(mega)
    }

    /** The placeholder letter only shows while no picture has loaded. */
    private fun waitForPicture() {
        composeRule.waitUntil(PICTURE_TIMEOUT_MS) { composeRule.onAllNodesWithText("M").fetchSemanticsNodes().isEmpty() }
    }

    // Scenario: Selecting a variety shows its picture
    @Test
    fun aVarietyShowsItsOwnPicture() {
        show(withPictures(alternateId = 10001, 10001))
        composeRule.onNodeWithTag("entry:art:10001").assertExists()
        composeRule.onNode(hasContentDescription("Artwork of Mega Bulbasaur"), useUnmergedTree = true).assertExists()
        waitForPicture()
    }

    // Scenario: Variety without its own picture
    @Test
    fun aVarietyWithoutAPictureFallsBackToTheSpecies() {
        show(withPictures(alternateId = 10002, 1))
        waitForPicture()
    }

    @Test
    fun formsAreListed() {
        val variety = detail.varieties.single()
        val forms =
            listOf(
                Form(1, "A", isDefault = true, isBattleOnly = false, isMega = false),
                Form(2, "B", isDefault = false, isBattleOnly = false, isMega = false),
            )
        val withForms = detail.copy(varieties = listOf(variety.copy(forms = forms)))
        show(EntryState.loaded(withForms, sampleTypeChart(), ArtworkLocation(null), null))
        scrollTo("Forms")
        composeRule.onNodeWithText("B").assertExists()
    }

    // Scenario: No names in other languages
    @Test
    fun namesInOtherLanguagesAreNotListed() {
        show(described)
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:location:1"))
        composeRule.onNodeWithText("Names in other languages").assertDoesNotExist()
        composeRule.onNodeWithText("Bulbizarre").assertDoesNotExist()
    }

    // Scenario: Moves learnt by level up
    @Test
    fun movesAreListedWithLevelTypeAndPower() {
        show(inGame(ScarletViolet))
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:move:2"))
        composeRule.onNodeWithTag("entry:move:1").assertContentDescriptionEquals("Level 3, Vine Whip, Grass, power 45")
        composeRule.onNodeWithTag("entry:move:2").assertContentDescriptionEquals("Level 6, Growth, Normal, no direct damage")
    }

    @Test
    fun anEvolutionMoveAndAMoveWithoutATypeAreWorded() {
        val details = sampleGameDetails(ScarletViolet).let {
            it.copy(moves = listOf(LevelUpMove(0, "Petal Dance", PokemonType.GRASS, 120), LevelUpMove(9, "Curse", null, null)))
        }
        show(loaded.copy(inGame = InGameState(listOf(ScarletViolet), ScarletViolet, details)))
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:move:1"))
        composeRule.onNodeWithTag("entry:move:0").assertContentDescriptionEquals("Learnt on evolving, Petal Dance, Grass, power 120")
        composeRule.onNodeWithTag("entry:move:1").assertContentDescriptionEquals("Level 9, Curse, no direct damage")
    }

    @Test
    fun aGameWithoutMovesSaysSo() {
        val details = sampleGameDetails(ScarletViolet).copy(moves = emptyList())
        show(loaded.copy(inGame = InGameState(listOf(ScarletViolet), ScarletViolet, details)))
        scrollTo("Learns no moves by level up in this game.")
        composeRule.onNodeWithText("Learns no moves by level up in this game.").assertExists()
    }

    // Scenario: Where to find
    // Scenario: Version-exclusive location
    @Test
    fun locationsShowMethodLevelsAndTheirVersion() {
        show(inGame(RedBlue))
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:location:1"))
        composeRule.onNodeWithTag("entry:location:0").assertContentDescriptionEquals("Route 1, Walking in grass or caves, Lv. 3–5")
        composeRule.onNodeWithTag("entry:location:1").assertContentDescriptionEquals("Cerulean City, Gift, Lv. 10, Blue only")
    }

    // Scenario: No location data for the game
    @Test
    fun aGameWithoutLocationDataSaysSo() {
        show(inGame(ScarletViolet))
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:locations-unavailable"))
        composeRule.onNodeWithText("Location data isn't available for this game yet.").assertExists()
    }

    // Scenario: Not found in the wild
    @Test
    fun aPokemonNotInTheWildSaysSo() {
        val details = sampleGameDetails(SwordShield).copy(encounters = emptyList())
        show(loaded.copy(inGame = InGameState(listOf(SwordShield), SwordShield, details)))
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:locations-none"))
        composeRule.onNodeWithText("Not found in the wild in this game.").assertExists()
    }

    @Test
    fun aMethodWithoutAStringUsesPokeApisWords() {
        val details = sampleGameDetails(RedBlue).let { sample ->
            sample.copy(
                encounters = listOf(
                    sample.encounters.first().copy(method = EncounterMethod("pokemon-ranger", "Receiving a Manaphy from Pokemon Ranger")),
                ),
            )
        }
        show(loaded.copy(inGame = InGameState(listOf(RedBlue), RedBlue, details)))
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:location:0"))
        composeRule.onNodeWithTag(
            "entry:location:0",
        ).assertContentDescriptionEquals("Route 1, Receiving a Manaphy from Pokemon Ranger, Lv. 3–5")
    }

    // Scenario: Picking a game updates the section
    @Test
    fun theGamePickerListsGamesAndReportsAPick() {
        var picked: Int? = null
        show(inGame(ScarletViolet), onSelectGame = { picked = it })
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:game-picker"))
        composeRule.onNodeWithTag("entry:game-picker").performClick()
        composeRule.onNodeWithTag("entry:game:${RedBlue.id}").assertExists()
        composeRule.onNodeWithTag("entry:game:${SwordShield.id}").performClick()
        assertEquals(SwordShield.id, picked)
        composeRule.onNodeWithTag("entry:game:${RedBlue.id}").assertDoesNotExist()
    }

    @Test
    fun aSpeciesWithoutEvolutionSaysSo() {
        val lone = detail.copy(evolutionLine = detail.evolutionLine.copy(children = emptyList()))
        show(EntryState.loaded(lone, sampleTypeChart(), ArtworkLocation(null), null))
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

    // Scenario: Entry shows a skeleton while loading
    @Test
    fun loadingShowsASkeleton() {
        show(EntryState.Loading)
        composeRule.onNodeWithTag("entry:list").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneLoading() {
        show(EntryState.Loading)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletLoading() {
        show(EntryState.Loading)
        composeRule.onRoot().captureRoboImage()
    }

    // Scenario: Placeholder shown without art
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phone() {
        show(described)
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
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneInGame() {
        show(described)
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:location:1"))
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletInGame() {
        show(described)
        composeRule.onNodeWithTag("entry:list").performScrollToNode(hasTestTag("entry:location:1"))
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
