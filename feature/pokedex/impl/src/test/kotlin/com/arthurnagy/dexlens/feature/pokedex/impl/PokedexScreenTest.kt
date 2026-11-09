package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.species.SearchSpeciesUseCase
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.speciesSummary
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
class PokedexScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val loaded = PokedexState.Initial.copy(
        loading = false,
        results = listOf(
            SpeciesSearchResult(speciesSummary(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON), matchedName = null),
            SpeciesSearchResult(speciesSummary(4, "Charmander", PokemonType.FIRE), matchedName = null),
            SpeciesSearchResult(speciesSummary(7, "Squirtle", PokemonType.WATER), matchedName = null),
            SpeciesSearchResult(speciesSummary(25, "Pikachu", PokemonType.ELECTRIC), matchedName = "ピカチュウ"),
            SpeciesSearchResult(speciesSummary(122, "Mr. Mime", PokemonType.PSYCHIC, PokemonType.FAIRY), matchedName = null),
        ),
    )

    private fun rowExists(id: Int) =
        composeRule.onAllNodesWithTag("pokedex:row:$id", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun awaitRow(id: Int) = composeRule.waitUntil(WAIT_MS) { rowExists(id) }

    private fun awaitRowGone(id: Int) = composeRule.waitUntil(WAIT_MS) { !rowExists(id) }

    private fun show(
        state: PokedexState,
        onOpen: (Int) -> Unit = {},
        onRetry: () -> Unit = {},
        onScan: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme { PokedexContent(state, TextFieldState(), onOpen, onScan, onRetry) }
        }
    }

    // Scenario: List in Pokédex order
    @Test
    fun rowsShowNumberAndName() {
        show(loaded)
        composeRule.onNodeWithText("#0001", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Bulbasaur", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Fire", useUnmergedTree = true).assertExists()
    }

    // Scenario: Shows loading state
    @Test
    fun loadingShowsIndicator() {
        show(PokedexState.Initial)
        composeRule.onNodeWithTag("pokedex:list").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Loading").assertExists()
    }

    // Scenario: Shows error state
    @Test
    fun errorOffersRetry() {
        var retried = false
        show(PokedexState.Initial.copy(loading = false, error = DomainError.Unknown("x")), onRetry = { retried = true })
        composeRule.onNodeWithText("Try again").performClick()
        assertEquals(true, retried)
    }

    // Scenario: No results shows empty state
    @Test
    fun emptyResultsNameTheQuery() {
        show(PokedexState.Initial.copy(loading = false, query = "zzz"))
        composeRule.onNodeWithText("No Pokémon named “zzz”").assertExists()
    }

    // Scenario: Tapping opens the entry
    @Test
    fun tappingARowOpensIt() {
        var opened = 0
        show(loaded, onOpen = { opened = it })
        composeRule.onNodeWithTag("pokedex:row:25").performClick()
        assertEquals(25, opened)
    }

    // Scenario: Clearing restores the list
    @Test
    fun rootScreenSearchesAsYouTypeAndClears() {
        val summaries = loaded.results.map { it.summary }
        val repository = FakeSpeciesRepository(
            summaries = summaries,
            hits = mapOf("\"pika\"*" to listOf(SpeciesNameHit(SpeciesId(25), "Pikachu"))),
        )
        val viewModel = PokedexViewModel(SearchSpeciesUseCase(repository))
        composeRule.setContent {
            AppTheme { PokedexScreen(onOpenSpecies = {}, onOpenScanner = {}, canNavigateBack = false, viewModel = viewModel) }
        }
        awaitRow(1)
        composeRule.onNodeWithTag("pokedex:search").performTextInput("pika")
        awaitRowGone(1)
        composeRule.onNodeWithTag("pokedex:row:25", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("pokedex:clear").performClick()
        awaitRow(1)
    }

    @Test
    fun scanButtonOpensTheCamera() {
        var opened = false
        show(loaded, onScan = { opened = true })
        composeRule.onNodeWithTag("pokedex:scan").performClick()
        assertEquals(true, opened)
    }

    // Scenario: List item announces details
    @Test
    fun rowAnnouncesNumberNameAndTypes() {
        show(loaded)
        composeRule.onNodeWithTag("pokedex:row:1", useUnmergedTree = true).assertContentDescriptionEquals("#0001, Bulbasaur, Grass, Poison")
    }

    // Scenario: Search other language
    @Test
    fun matchedNameIsShown() {
        show(loaded)
        composeRule.onNodeWithText("ピカチュウ", useUnmergedTree = true).assertExists()
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
        composeRule.setContent {
            AppTheme { PokedexContent(loaded, TextFieldState(), {}, {}, {}) }
        }
        composeRule.onRoot().captureRoboImage()
    }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
