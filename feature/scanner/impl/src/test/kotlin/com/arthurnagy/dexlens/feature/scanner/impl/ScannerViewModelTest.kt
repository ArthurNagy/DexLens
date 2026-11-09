package com.arthurnagy.dexlens.feature.scanner.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.scan.BuildSpeciesSignalUseCase
import com.arthurnagy.dexlens.core.domain.scan.LockOnTracker
import com.arthurnagy.dexlens.core.domain.scan.MatchDecision
import com.arthurnagy.dexlens.core.domain.scan.MatchOutcome
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesListUseCase
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.arthurnagy.dexlens.core.testing.speciesSummary
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.OrbitScopedTestContextInternal
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

internal class ScannerViewModelTest {
    private val pikachu = speciesSummary(25, "Pikachu", PokemonType.ELECTRIC)
    private val pidgey = speciesSummary(16, "Pidgey", PokemonType.NORMAL, PokemonType.FLYING)
    private val content = FakeContentPackRepository()
    private val repository = FakeSpeciesRepository(
        summaries = listOf(pidgey, pikachu),
        scanNames = listOf(SpeciesNameHit(SpeciesId(25), "Pikachu"), SpeciesNameHit(SpeciesId(16), "Pidgey")),
    )

    private fun TestScope.viewModel() = ScannerViewModel(
        BuildSpeciesSignalUseCase(repository, TestDispatcherProvider(testScheduler)),
        GetSpeciesListUseCase(repository),
        ObserveArtworkUseCase(content),
    )

    private fun frame(vararg lines: String) = AnalyzedFrame(
        lines = lines.map { AnalyzedLine(it, 0f, 0f, 10f, 10f) },
        cropWidth = 100,
        cropHeight = 20,
        bufferWidth = 1920,
        bufferHeight = 1080,
        processingMillis = 5,
    )

    private fun ScannerState.seen(vararg species: SpeciesSummary) = copy(lockOn = lockOn.observe(species.map { it.id }.toSet()))

    private fun ScannerState.lockedOn(vararg species: SpeciesSummary) =
        copy(lockOn = LockOnTracker.Empty, locked = species.toList(), pendingHaptic = true)

    /** Runs [block] once the name index has loaded. The matcher is built inside the ViewModel, so this reads it back. */
    private fun scanning(block: suspend OrbitScopedTestContextInternal<ScannerState, ScannerState, Nothing, ScannerViewModel>.() -> Unit) =
        runTest {
            viewModel().testWithInternalState(this, ScannerState.Initial) {
                runOnCreate()
                val ready = awaitInternalState()
                assertFalse(ready.loading)
                assertNotNull(ready.signal)
                assertEquals(mapOf(pidgey.id to pidgey, pikachu.id to pikachu), ready.speciesById)
                block()
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun loadsTheNameIndex() = scanning { expectNoItems() }

    @Test
    fun aLoadFailureIsShown() = runTest {
        repository.nextError = DomainError.Unknown("broken")
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { copy(loading = false, error = DomainError.Unknown("broken")) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun artworkFollowsTheDownload() = scanning {
        content.artwork.value = ArtworkLocation("/art")
        expectInternalState { copy(artwork = ArtworkLocation("/art")) }
    }

    // Scenario: Lock after repeated frames
    @Test
    fun threeMatchingFramesLock() = scanning {
        repeat(3) { containerHost.onFrame(frame("PIKACHU"), debugEnabled = false) }
        expectInternalState { seen(pikachu) }
        expectInternalState { seen(pikachu) }
        expectInternalState { lockedOn(pikachu) }
    }

    // Scenario: Single frame does not lock
    @Test
    fun oneFrameDoesNotLock() = scanning {
        containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
        expectInternalState { seen(pikachu) }
        containerHost.onFrame(frame("nothing here"), debugEnabled = false)
        expectInternalState { seen() }
        expectNoItems()
    }

    // Scenario: Several names offered as choices
    @Test
    fun twoSpeciesAreOfferedInNumberOrder() = scanning {
        repeat(3) { containerHost.onFrame(frame("PIKACHU", "PIDGEY"), debugEnabled = false) }
        expectInternalState { seen(pikachu, pidgey) }
        expectInternalState { seen(pikachu, pidgey) }
        expectInternalState { lockedOn(pidgey, pikachu) }
    }

    // Scenario: Nickname keeps scanning
    @Test
    fun unknownTextKeepsScanning() = scanning {
        repeat(6) { containerHost.onFrame(frame("SPARKY"), debugEnabled = false) }
        expectNoItems()
    }

    // Scenario: Analysis pauses during result
    @Test
    fun framesAreIgnoredWhileAResultIsShown() = scanning {
        repeat(3) { containerHost.onFrame(frame("PIKACHU"), debugEnabled = false) }
        skipItems(2)
        expectInternalState { lockedOn(pikachu) }
        repeat(3) { containerHost.onFrame(frame("PIDGEY"), debugEnabled = false) }
        expectNoItems()
    }

    @Test
    fun aFrameAlreadyInFlightWhenAResultAppearsChangesNothing() {
        val locked = ScannerState.Initial.copy(locked = listOf(pikachu))
        assertEquals(locked, locked.afterFrame(setOf(pidgey.id), frameDebug = null))
    }

    // Scenario: Dismissing resumes scanning
    @Test
    fun dismissingResumesWithAClearHistory() = scanning {
        repeat(3) { containerHost.onFrame(frame("PIKACHU"), debugEnabled = false) }
        skipItems(2)
        expectInternalState { lockedOn(pikachu) }
        containerHost.onHapticConsumed()
        expectInternalState { copy(pendingHaptic = false) }
        containerHost.onResultDismissed()
        expectInternalState { copy(locked = emptyList()) }
        repeat(2) { containerHost.onFrame(frame("PIKACHU"), debugEnabled = false) }
        expectInternalState { seen(pikachu) }
        expectInternalState { seen(pikachu) }
        expectNoItems()
    }

    // Scenario: Torch toggles
    @Test
    fun torchTogglesState() = scanning {
        containerHost.onTorchToggled()
        expectInternalState { copy(torchOn = true) }
        containerHost.onTorchToggled()
        expectInternalState { copy(torchOn = false) }
    }

    @Test
    fun debugFramesAreExposedOnlyWhenEnabled() = scanning {
        val debugFrame = frame("PIKACHU")
        containerHost.onFrame(debugFrame, debugEnabled = true)
        expectInternalState {
            seen(pikachu).copy(debug = ScanDebug(debugFrame, listOf(MatchDecision("pikachu", MatchOutcome.EXACT, setOf(SpeciesId(25))))))
        }
    }

    @Test
    fun framesBeforeTheIndexLoadsAreIgnored() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
            expectNoItems()
        }
    }
}
