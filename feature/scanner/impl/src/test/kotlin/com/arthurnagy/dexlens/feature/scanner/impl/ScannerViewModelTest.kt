package com.arthurnagy.dexlens.feature.scanner.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.scan.BuildSpeciesSignalUseCase
import com.arthurnagy.dexlens.core.domain.scan.MatchDecision
import com.arthurnagy.dexlens.core.domain.scan.MatchOutcome
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesListUseCase
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.arthurnagy.dexlens.core.testing.speciesSummary
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test

internal class ScannerViewModelTest {
    private val pikachu = speciesSummary(25, "Pikachu", PokemonType.ELECTRIC)
    private val pidgey = speciesSummary(16, "Pidgey", PokemonType.NORMAL, PokemonType.FLYING)
    private val repository = FakeSpeciesRepository(
        summaries = listOf(pidgey, pikachu),
        scanNames = listOf(SpeciesNameHit(SpeciesId(25), "Pikachu"), SpeciesNameHit(SpeciesId(16), "Pidgey")),
    )

    private fun TestScope.viewModel() = ScannerViewModel(
        BuildSpeciesSignalUseCase(repository, TestDispatcherProvider(testScheduler)),
        GetSpeciesListUseCase(repository),
    )

    private fun frame(vararg lines: String) = AnalyzedFrame(
        lines = lines.map { AnalyzedLine(it, 0f, 0f, 10f, 10f) },
        cropWidth = 100,
        cropHeight = 20,
        bufferWidth = 1920,
        bufferHeight = 1080,
        processingMillis = 5,
    )

    private val ready = ScannerState.Initial.copy(loading = false)

    @Test
    fun loadsTheNameIndex() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { ready }
        }
    }

    @Test
    fun aLoadFailureIsShown() = runTest {
        repository.nextError = DomainError.Unknown("broken")
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { copy(loading = false, error = DomainError.Unknown("broken")) }
        }
    }

    // Scenario: Lock after repeated frames
    @Test
    fun threeMatchingFramesLock() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { ready }
            containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
            containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
            containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
            expectInternalState { copy(locked = listOf(pikachu), pendingHaptic = true) }
        }
    }

    // Scenario: Single frame does not lock
    @Test
    fun oneFrameDoesNotLock() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { ready }
            containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
            containerHost.onFrame(frame("nothing here"), debugEnabled = false)
        }
    }

    // Scenario: Several names offered as choices
    @Test
    fun twoSpeciesAreOfferedInNumberOrder() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { ready }
            repeat(3) { containerHost.onFrame(frame("PIKACHU", "PIDGEY"), debugEnabled = false) }
            expectInternalState { copy(locked = listOf(pidgey, pikachu), pendingHaptic = true) }
        }
    }

    // Scenario: Nickname keeps scanning
    @Test
    fun unknownTextKeepsScanning() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { ready }
            repeat(6) { containerHost.onFrame(frame("SPARKY"), debugEnabled = false) }
        }
    }

    // Scenario: Analysis pauses during result
    @Test
    fun framesAreIgnoredWhileAResultIsShown() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { ready }
            repeat(3) { containerHost.onFrame(frame("PIKACHU"), debugEnabled = false) }
            expectInternalState { copy(locked = listOf(pikachu), pendingHaptic = true) }
            repeat(3) { containerHost.onFrame(frame("PIDGEY"), debugEnabled = false) }
        }
    }

    // Scenario: Dismissing resumes scanning
    @Test
    fun dismissingResumesWithAClearHistory() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { ready }
            repeat(3) { containerHost.onFrame(frame("PIKACHU"), debugEnabled = false) }
            expectInternalState { copy(locked = listOf(pikachu), pendingHaptic = true) }
            containerHost.onHapticConsumed()
            expectInternalState { copy(pendingHaptic = false) }
            containerHost.onResultDismissed()
            expectInternalState { copy(locked = emptyList()) }
            containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
            containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
        }
    }

    // Scenario: Torch toggles
    @Test
    fun torchTogglesState() = runTest {
        viewModel().testWithInternalState(this, ready) {
            containerHost.onTorchToggled()
            expectInternalState { copy(torchOn = true) }
            containerHost.onTorchToggled()
            expectInternalState { copy(torchOn = false) }
        }
    }

    @Test
    fun debugFramesAreExposedOnlyWhenEnabled() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            runOnCreate()
            expectInternalState { ready }
            val debugFrame = frame("PIKACHU")
            containerHost.onFrame(debugFrame, debugEnabled = true)
            expectInternalState {
                copy(debug = ScanDebug(debugFrame, listOf(MatchDecision("pikachu", MatchOutcome.EXACT, setOf(SpeciesId(25))))))
            }
        }
    }

    @Test
    fun framesBeforeTheIndexLoadsAreIgnored() = runTest {
        viewModel().testWithInternalState(this, ScannerState.Initial) {
            containerHost.onFrame(frame("PIKACHU"), debugEnabled = false)
        }
    }
}
