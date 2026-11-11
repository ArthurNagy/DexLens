package com.arthurnagy.dexlens.feature.entry.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveFlavorEntriesUseCase
import com.arthurnagy.dexlens.core.domain.game.GetInGameDetailsUseCase
import com.arthurnagy.dexlens.core.domain.game.SetPreferredGameUseCase
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesDetailUseCase
import com.arthurnagy.dexlens.core.domain.species.GetTypeChartUseCase
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.Game
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test
import kotlin.test.assertEquals

class EntryViewModelTest {
    private val chart = sampleTypeChart()
    private val base = bulbasaurDetail()
    private val baseVariety = base.varieties.single()
    private val mega = baseVariety.copy(
        id = 2,
        name = "Mega Bulbasaur",
        isDefault = false,
        types = listOf(PokemonType.GRASS, PokemonType.FIRE),
    )
    private val detail = base.copy(varieties = base.varieties + mega)
    private val repository = FakeSpeciesRepository(details = mapOf(SpeciesId(1) to detail))
    private val charts = FakeTypeChartRepository(chart)

    private val content = FakeContentPackRepository()
    private val flavor = FakeFlavorTextRepository()
    private val allGames = listOf(ScarletViolet, SwordShield, RedBlue)
    private val games = FakeGameRepository(gamesByVariety = mapOf(baseVariety.id to allGames, mega.id to listOf(SwordShield, RedBlue)))
    private val preferences = FakeUserPreferencesRepository(ThemeMode.SYSTEM)

    private fun viewModel(
        speciesId: Int = 1,
        content: FakeContentPackRepository = this.content,
        flavor: FakeFlavorTextRepository = this.flavor,
    ) = EntryViewModel(
        EntryKey(speciesId),
        GetSpeciesDetailUseCase(repository),
        GetTypeChartUseCase(charts),
        ObserveArtworkUseCase(content),
        ObserveFlavorEntriesUseCase(flavor),
        GetInGameDetailsUseCase(games, preferences),
        SetPreferredGameUseCase(preferences),
    )

    private val loaded = EntryState.loaded(detail, chart, ArtworkLocation(null), description = null)

    private fun inGame(
        game: Game,
        listed: List<Game> = allGames,
    ) = InGameState(listed, game, sampleGameDetails(game))

    private val withGames = loaded.copy(inGame = inGame(ScarletViolet))

    // Scenario: Entry shows full data
    @Test
    fun loadsTheSpeciesItsMatchupsAndTheNewestGame() = runTest {
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            expectInternalState { withGames }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Unknown species
    @Test
    fun anUnknownSpeciesEndsInNotFound() = runTest {
        viewModel(speciesId = 999).testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { EntryState.Failed(DomainError.NotFound) }
        }
    }

    @Test
    fun aChartFailureIsAnError() = runTest {
        charts.nextError = DomainError.Unknown("broken")
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { EntryState.Failed(DomainError.Unknown("broken")) }
        }
    }

    @Test
    fun aGameFailureLeavesTheSectionOut() = runTest {
        games.nextError = DomainError.Unknown("broken")
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Selecting a variety updates the entry
    @Test
    fun selectingAVarietyRecalculatesMatchupsAndReloadsItsGames() = runTest {
        viewModel().testWithInternalState(this, withGames) {
            containerHost.onSelectVariety(2)
            expectInternalState { withGames.select(mega) }
            expectInternalState { withGames.select(mega).copy(inGame = inGame(SwordShield, listOf(SwordShield, RedBlue))) }
        }
    }

    // Scenario: Picking a game updates the section
    @Test
    fun pickingAGameShowsItAtOnceThenItsDetails() = runTest {
        viewModel().testWithInternalState(this, withGames) {
            containerHost.onSelectGame(RedBlue.id)
            expectInternalState { withGames.copy(inGame = InGameState(allGames, RedBlue, sampleGameDetails(ScarletViolet))) }
            expectInternalState { withGames.copy(inGame = inGame(RedBlue)) }
        }
    }

    // Scenario: Last picked game is remembered
    @Test
    fun aPickedGameIsStoredForOtherEntries() = runTest {
        viewModel().testWithInternalState(this, withGames) {
            containerHost.onSelectGame(SwordShield.id)
            expectInternalState { withGames.copy(inGame = InGameState(allGames, SwordShield, sampleGameDetails(ScarletViolet))) }
            expectInternalState { withGames.copy(inGame = inGame(SwordShield)) }
        }
        assertEquals(SwordShield.id, preferences.preferredGame.first())
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            expectInternalState { loaded.copy(inGame = inGame(SwordShield)) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun pickingAnUnlistedGameDoesNothing() = runTest {
        viewModel().testWithInternalState(this, withGames) {
            containerHost.onSelectGame(999)
        }
        viewModel().testWithInternalState(this, loaded) {
            containerHost.onSelectGame(RedBlue.id)
        }
    }

    // Scenario: Art appears when the download finishes
    @Test
    fun anOpenEntryPicksUpArtworkAndKeepsTheSelectedVariety() = runTest {
        val location = ArtworkLocation("/art")
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            expectInternalState { withGames }
            content.artwork.value = location
            expectInternalState { withGames.copy(artwork = location) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Flavor text appears in an open entry
    @Test
    fun anOpenEntryPicksUpTheDescription() = runTest {
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            expectInternalState { withGames }
            flavor.entries.value = listOf(FlavorEntry("Newest text.", listOf("Scarlet")), FlavorEntry("Older text.", listOf("Red")))
            expectInternalState { withGames.copy(description = "Newest text.") }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun selectingAnUnknownVarietyDoesNothing() = runTest {
        viewModel().testWithInternalState(this, loaded) {
            containerHost.onSelectVariety(42)
        }
    }

    // Scenario: Description from downloaded text
    @Test
    fun loadsDownloadedArtAndDescription() = runTest {
        val location = ArtworkLocation("/art")
        val downloadedText = FakeFlavorTextRepository(listOf(FlavorEntry("A seed grows.", listOf("Scarlet"))))
        viewModel(
            content = FakeContentPackRepository(artwork = location),
            flavor = downloadedText,
        ).testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { EntryState.loaded(detail, chart, location, "A seed grows.") }
            cancelAndIgnoreRemainingItems()
        }
    }
}
