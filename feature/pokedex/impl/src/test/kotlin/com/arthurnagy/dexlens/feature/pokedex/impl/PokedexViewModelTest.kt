package com.arthurnagy.dexlens.feature.pokedex.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.ObserveScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentOfferUseCase
import com.arthurnagy.dexlens.core.domain.scan.BuildSpeciesSignalUseCase
import com.arthurnagy.dexlens.core.domain.scan.LockOnTracker
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesListUseCase
import com.arthurnagy.dexlens.core.domain.species.SearchSpeciesUseCase
import com.arthurnagy.dexlens.core.domain.species.SpeciesRepository
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.arthurnagy.dexlens.core.testing.speciesSummary
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.AnalyzedFrame
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.OrbitScopedTestContextInternal
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private typealias Context = OrbitScopedTestContextInternal<PokedexState, PokedexState, Nothing, PokedexViewModel>

internal class PokedexViewModelTest {
    private val pikachu = speciesSummary(25, "Pikachu", PokemonType.ELECTRIC)
    private val pidgey = speciesSummary(16, "Pidgey", PokemonType.NORMAL, PokemonType.FLYING)
    private val bulbasaur = speciesSummary(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON)
    private val repository = FakeSpeciesRepository(
        summaries = listOf(bulbasaur, pidgey, pikachu),
        scanNames = listOf(SpeciesNameHit(SpeciesId(25), "Pikachu"), SpeciesNameHit(SpeciesId(16), "Pidgey")),
    )
    private var listError: DomainError? = null
    private val species = object : SpeciesRepository by repository {
        override suspend fun summaries(): Result<List<SpeciesSummary>, DomainError> = listError?.let { Err(it) } ?: repository.summaries()
    }
    private val everySpecies = listOf(bulbasaur, pidgey, pikachu).map { SpeciesSearchResult(it, matchedName = null) }

    private fun results(vararg ids: Int) = everySpecies.filter { it.summary.id.value in ids }

    private val content = FakeContentPackRepository()
    private val preferences = FakeUserPreferencesRepository(ThemeMode.SYSTEM, initialOfferShown = true)

    private fun TestScope.viewModel(
        speciesRepository: SpeciesRepository = species,
        userPreferences: FakeUserPreferencesRepository = preferences,
        artwork: FakeContentPackRepository = content,
    ) = PokedexViewModel(
        SearchSpeciesUseCase(speciesRepository),
        ObserveArtworkUseCase(artwork),
        ObserveContentOfferUseCase(userPreferences),
        ObserveScannerEnabledUseCase(userPreferences),
        BuildSpeciesSignalUseCase(speciesRepository, TestDispatcherProvider(testScheduler)),
        GetSpeciesListUseCase(speciesRepository),
    )

    private fun frame(vararg lines: String) = AnalyzedFrame(lines.toList())

    private fun PokedexState.seen(vararg seen: SpeciesSummary) = copy(
        scan = scan.copy(
            lockOn = scan.lockOn.observe(
                seen.map {
                    it.id
                }.toSet(),
            ),
        ),
    )

    private fun PokedexState.lockedOn(vararg locked: SpeciesSummary) =
        copy(scan = scan.copy(lockOn = LockOnTracker.Empty, match = locked.toList(), pendingHaptic = true))

    /** Waits for the list, the matcher and the switch, which load side by side. */
    private suspend fun Context.settled(): PokedexState {
        var seen = awaitInternalState()
        while (seen.loading || seen.scan.signal == null || seen.scannerEnabled == null) seen = awaitInternalState()
        return seen
    }

    private fun home(block: suspend Context.(PokedexState) -> Unit) = runTest {
        viewModel().testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            block(settled())
            cancelAndIgnoreRemainingItems()
        }
    }

    /** A home screen with Pikachu already matched and its haptic played. */
    private fun matched(block: suspend Context.(PokedexState) -> Unit) = home {
        repeat(3) { containerHost.onFrame(frame("PIKACHU")) }
        skipItems(2)
        expectInternalState { lockedOn(pikachu) }
        containerHost.onHapticConsumed()
        expectInternalState { copy(scan = scan.copy(pendingHaptic = false)) }
        block(awaitLatest())
    }

    private fun Context.awaitLatest(): PokedexState = containerHost.container.stateFlow.value

    // Scenario: List in Pokédex order
    @Test
    fun loadsEverySpeciesInOrder() = home { ready ->
        assertEquals(everySpecies, ready.results)
        assertNull(ready.error)
    }

    // Scenario: Prefix updates as you type
    @Test
    fun aQueryUpdatesResultsWithoutSubmitting() = home {
        repository.hits = mapOf("\"pika\"*" to listOf(SpeciesNameHit(SpeciesId(25), "Pikachu")))
        containerHost.onQueryChanged("pika")
        expectInternalState { copy(query = "pika") }
        expectInternalState { copy(results = results(25)) }
    }

    @Test
    fun anOlderAnswerNeverReplacesANewerOne() = runTest {
        repository.hits = mapOf(
            "\"pi\"*" to listOf(SpeciesNameHit(SpeciesId(1), "Bulbasaur")),
            "\"pika\"*" to listOf(SpeciesNameHit(SpeciesId(25), "Pikachu")),
        )
        val slowFirstQuery = CompletableDeferred<Unit>()
        val slow = object : SpeciesRepository by repository {
            override suspend fun searchNames(ftsQuery: String): Result<List<SpeciesNameHit>, DomainError> {
                if (ftsQuery == "\"pi\"*") slowFirstQuery.await()
                return repository.searchNames(ftsQuery)
            }
        }
        val viewModel = viewModel(speciesRepository = slow)
        viewModel.testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            settled()
            containerHost.onQueryChanged("pi")
            expectInternalState { copy(query = "pi") }
            containerHost.onQueryChanged("pika")
            expectInternalState { copy(query = "pika") }
            expectInternalState { copy(results = results(25)) }
            slowFirstQuery.complete(Unit)
            cancelAndIgnoreRemainingItems()
            assertEquals(results(25), viewModel.container.stateFlow.value.results)
        }
    }

    @Test
    fun theSameTextIsNotSearchedAgain() = home {
        repository.nextError = DomainError.Unknown("searched again")
        containerHost.onQueryChanged("")
        expectNoItems()
    }

    // Scenario: Shows error state
    @Test
    fun anErrorIsShownAndRetryRecovers() = runTest {
        listError = DomainError.Unknown("broken")
        viewModel().testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            var seen = awaitInternalState()
            while (seen.loading) seen = awaitInternalState()
            assertEquals(DomainError.Unknown("broken"), seen.error)
            listError = null
            containerHost.onRetry()
            while (seen.loading || seen.error != null) seen = awaitInternalState()
            assertEquals(everySpecies, seen.results)
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Offer on first launch
    @Test
    fun firstLaunchAsksForTheOfferOnce() = runTest {
        val fresh = FakeUserPreferencesRepository(ThemeMode.SYSTEM, initialOfferShown = false)
        viewModel(userPreferences = fresh).testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            var seen = awaitInternalState()
            while (!seen.pendingOffer) seen = awaitInternalState()
            containerHost.onOfferConsumed()
            while (seen.pendingOffer) seen = awaitInternalState()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun artworkLocationIsShownInTheList() = runTest {
        val location = ArtworkLocation("/art")
        viewModel(artwork = FakeContentPackRepository(artwork = location)).testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            var seen = awaitInternalState()
            while (seen.artwork != location) seen = awaitInternalState()
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Works after skipping
    @Test
    fun skippingLeavesTheListWorkingWithPlaceholders() = home { ready ->
        assertEquals(everySpecies, ready.results)
        assertEquals(ArtworkLocation(null), ready.artwork)
    }

    @Test
    fun loadsTheNameIndex() = home { ready ->
        assertNotNull(ready.scan.signal)
        assertEquals(setOf(bulbasaur.id, pidgey.id, pikachu.id), ready.scan.speciesById.keys)
    }

    @Test
    fun aMatcherFailureLeavesTheListWorking() = runTest {
        val broken = object : SpeciesRepository by species {
            override suspend fun scanNames(): Result<List<SpeciesNameHit>, DomainError> = Err(DomainError.Unknown("broken"))
        }
        viewModel(speciesRepository = broken).testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            var seen = awaitInternalState()
            while (seen.loading || seen.scan.error == null) seen = awaitInternalState()
            assertEquals(DomainError.Unknown("broken"), seen.scan.error)
            assertEquals(everySpecies, seen.results)
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Scanner on by default
    @Test
    fun theScannerIsOnUntilTurnedOff() = home { ready ->
        assertEquals(true, ready.scannerEnabled)
    }

    // Scenario: Lock after repeated frames
    @Test
    fun threeMatchingFramesLock() = home {
        repeat(3) { containerHost.onFrame(frame("PIKACHU")) }
        expectInternalState { seen(pikachu) }
        expectInternalState { seen(pikachu) }
        expectInternalState { lockedOn(pikachu) }
    }

    // Scenario: Single frame does not lock
    @Test
    fun oneFrameDoesNotLock() = home {
        containerHost.onFrame(frame("PIKACHU"))
        expectInternalState { seen(pikachu) }
        containerHost.onFrame(frame("nothing here"))
        expectInternalState { seen() }
        expectNoItems()
    }

    // Scenario: Several names offered as choices
    @Test
    fun twoSpeciesAreListedInNumberOrder() = home {
        repeat(3) { containerHost.onFrame(frame("PIKACHU", "PIDGEY")) }
        expectInternalState { seen(pikachu, pidgey) }
        expectInternalState { seen(pikachu, pidgey) }
        expectInternalState { lockedOn(pidgey, pikachu) }
        assertEquals(results(16, 25), awaitLatest().listed)
    }

    // Scenario: Nickname keeps scanning
    @Test
    fun unknownTextKeepsScanning() = home {
        repeat(6) { containerHost.onFrame(frame("SPARKY")) }
        expectNoItems()
    }

    // Scenario: Match filters the list
    @Test
    fun aLockListsOnlyTheMatch() = matched { state ->
        assertEquals(results(25), state.listed)
        assertEquals(everySpecies, state.results)
        assertTrue(state.matchListed)
    }

    // Scenario: Typing replaces a match
    @Test
    fun typingClearsTheMatchAndTheLockHistory() = matched {
        repository.hits = mapOf("\"bul\"*" to listOf(SpeciesNameHit(SpeciesId(1), "Bulbasaur")))
        containerHost.onQueryChanged("bul")
        expectInternalState { copy(query = "bul", scan = scan.copy(match = emptyList(), lockOn = LockOnTracker.Empty)) }
        expectInternalState { copy(results = results(1)) }
        assertEquals(results(1), awaitLatest().listed)
    }

    @Test
    fun framesAreIgnoredWhileTextIsTyped() = home {
        containerHost.onQueryChanged("zzz")
        expectInternalState { copy(query = "zzz") }
        expectInternalState { copy(results = emptyList()) }
        containerHost.onFrame(frame("PIKACHU"))
        expectNoItems()
    }

    // Scenario: Clearing a match restores the list
    @Test
    fun clearingTheMatchRestoresTheFullList() = matched {
        containerHost.onMatchCleared()
        expectInternalState { copy(scan = scan.copy(match = emptyList())) }
        assertEquals(everySpecies, awaitLatest().listed)
    }

    // Scenario: Torch toggles
    @Test
    fun torchTogglesState() = home {
        containerHost.onTorchToggled()
        expectInternalState { copy(scan = scan.copy(torchOn = true)) }
        containerHost.onTorchToggled()
        expectInternalState { copy(scan = scan.copy(torchOn = false)) }
    }

    @Test
    fun framesBeforeTheIndexLoadsAreIgnored() = runTest {
        viewModel().testWithInternalState(this, PokedexState.Initial) {
            containerHost.onFrame(frame("PIKACHU"))
            expectNoItems()
        }
    }

    // Scenario: Turning the scanner off clears a match
    @Test
    fun turningTheScannerOffClearsTheMatchAndTheTorch() = matched {
        containerHost.onTorchToggled()
        expectInternalState { copy(scan = scan.copy(torchOn = true)) }
        preferences.setScannerEnabled(false)
        expectInternalState {
            copy(scannerEnabled = false, scan = scan.copy(match = emptyList(), lockOn = LockOnTracker.Empty, torchOn = false))
        }
        assertEquals(everySpecies, awaitLatest().listed)
    }

    // Scenario: Turning the scanner off keeps a search
    @Test
    fun turningTheScannerOffKeepsTheQuery() = home {
        containerHost.onQueryChanged("zzz")
        expectInternalState { copy(query = "zzz") }
        expectInternalState { copy(results = emptyList()) }
        preferences.setScannerEnabled(false)
        expectInternalState { copy(scannerEnabled = false) }
        assertEquals("zzz", awaitLatest().query)
    }

    @Test
    fun framesAreIgnoredWithTheScannerOff() = home {
        preferences.setScannerEnabled(false)
        expectInternalState { copy(scannerEnabled = false) }
        containerHost.onFrame(frame("PIKACHU"))
        expectNoItems()
    }
}
