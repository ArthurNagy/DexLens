package com.arthurnagy.dexlens.feature.scanner.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.scan.BuildSpeciesSignalUseCase
import com.arthurnagy.dexlens.core.domain.scan.MatchDecision
import com.arthurnagy.dexlens.core.domain.scan.MatchOutcome
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesListUseCase
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.arthurnagy.dexlens.core.testing.speciesSummary
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ScannerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val ready = ScannerState.Initial.copy(loading = false)
    private val noArt = ArtworkLocation(null)
    private val pikachu = speciesSummary(25, "Pikachu", PokemonType.ELECTRIC)
    private val pidgey = speciesSummary(16, "Pidgey", PokemonType.NORMAL, PokemonType.FLYING)

    private fun show(
        state: ScannerState = ready,
        permission: CameraPermission = CameraPermission.GRANTED,
        cameraFailed: Boolean = false,
        torchAvailable: Boolean = true,
        debugEnabled: Boolean = false,
        onBack: () -> Unit = {},
        onTorch: () -> Unit = {},
        onRequest: () -> Unit = {},
        onSettings: () -> Unit = {},
        onOpen: (Int) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                ScannerContent(
                    state = state,
                    permission = permission,
                    cameraFailed = cameraFailed,
                    torchAvailable = torchAvailable,
                    debugEnabled = debugEnabled,
                    onNavigateBack = onBack,
                    onToggleTorch = onTorch,
                    onRequestPermission = onRequest,
                    onOpenAppSettings = onSettings,
                    onOpenSpecies = onOpen,
                    onDismissResult = onDismiss,
                    camera = { CameraStub() },
                )
            }
        }
    }

    @Composable
    private fun CameraStub() {
        Box(Modifier.fillMaxSize().background(Color.DarkGray))
    }

    // Scenario: Hint and aiming box shown
    @Test
    fun showsHintAndAimingBox() {
        show()
        composeRule.onNodeWithText("Point at the Pokémon's name").assertExists()
        composeRule.onNodeWithTag("scanner:aimBox").assertExists()
    }

    // Scenario: Torch toggles
    @Test
    fun torchButtonTogglesAndReflectsState() {
        var toggled = false
        show(onTorch = { toggled = true })
        composeRule.onNodeWithContentDescription("Turn torch on").performClick()
        assertTrue(toggled)
    }

    // Scenario: Torch toggles
    @Test
    fun torchButtonShowsItsCurrentState() {
        show(state = ready.copy(torchOn = true))
        composeRule.onNodeWithContentDescription("Turn torch off").assertExists()
    }

    @Test
    fun torchHiddenWithoutAFlash() {
        show(torchAvailable = false)
        composeRule.onNodeWithTag("scanner:torch").assertDoesNotExist()
    }

    // Scenario: Rationale before asking
    @Test
    fun rationaleOffersToAllowTheCamera() {
        var requested = false
        show(permission = CameraPermission.NEEDS_REQUEST, onRequest = { requested = true })
        composeRule.onNodeWithText("Read names with your camera").assertExists()
        composeRule.onNodeWithText("Allow camera").performClick()
        assertTrue(requested)
    }

    // Scenario: Denied permanently
    @Test
    fun permanentDenialOffersSettings() {
        var opened = false
        show(permission = CameraPermission.PERMANENTLY_DENIED, onSettings = { opened = true })
        composeRule.onNodeWithText("Open settings").performClick()
        assertTrue(opened)
    }

    @Test
    fun cameraFailureIsExplained() {
        show(cameraFailed = true)
        composeRule.onNodeWithText("This device has no camera DexLens can use.").assertExists()
    }

    // Scenario: Result opens the entry
    @Test
    fun sheetOpensTheEntry() {
        var opened = 0
        composeRule.setContent { AppTheme { ResultSheetContent(listOf(pikachu), noArt, onOpenSpecies = { opened = it }, onDismiss = {}) } }
        composeRule.onNodeWithText("Open entry").performClick()
        assertEquals(25, opened)
    }

    // Scenario: Dismissing resumes scanning
    @Test
    fun keepScanningDismisses() {
        var dismissed = false
        composeRule.setContent {
            AppTheme { ResultSheetContent(listOf(pikachu), noArt, onOpenSpecies = {}, onDismiss = { dismissed = true }) }
        }
        composeRule.onNodeWithText("Keep scanning").performClick()
        assertTrue(dismissed)
    }

    // Scenario: Several names offered as choices
    @Test
    fun severalSpeciesAreChoices() {
        var opened = 0
        composeRule.setContent {
            AppTheme { ResultSheetContent(listOf(pidgey, pikachu), noArt, onOpenSpecies = { opened = it }, onDismiss = {}) }
        }
        composeRule.onNodeWithText("Which one?").assertExists()
        composeRule.onNodeWithTag("scanner:result:16").performClick()
        assertEquals(16, opened)
    }

    @Test
    fun debugOverlayShowsDecisions() {
        val frame = AnalyzedFrame(listOf(AnalyzedLine("PIKACHU", 2f, 2f, 50f, 12f)), 100, 20, 1920, 1080, 42)
        val debug = ScanDebug(frame, listOf(MatchDecision("pikachu", MatchOutcome.EXACT, setOf(SpeciesId(25)))))
        show(state = ready.copy(debug = debug), debugEnabled = true)
        composeRule.onNodeWithText("EXACT: pikachu [25]").assertExists()
    }

    @Test
    fun debugOverlayAbsentInRelease() {
        val frame = AnalyzedFrame(emptyList(), 100, 20, 1920, 1080, 42)
        show(
            state = ready.copy(debug = ScanDebug(frame, listOf(MatchDecision("x", MatchOutcome.NO_MATCH, emptySet())))),
            debugEnabled = false,
        )
        composeRule.onNodeWithText("NO_MATCH: x []").assertDoesNotExist()
    }

    // Scenario: Rationale before asking
    @Test
    fun rootScreenAsksForTheCameraFirst() {
        val repository = FakeSpeciesRepository()
        val viewModel = ScannerViewModel(
            BuildSpeciesSignalUseCase(repository, TestDispatcherProvider(TestCoroutineScheduler())),
            GetSpeciesListUseCase(repository),
            ObserveArtworkUseCase(FakeContentPackRepository()),
        )
        composeRule.setContent {
            AppTheme { ScannerScreen(onNavigateBack = {}, onOpenSpecies = {}, viewModel = viewModel) }
        }
        composeRule.onNodeWithText("Allow camera").assertExists()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phone() {
        show()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w841dp-h701dp-xxhdpi")
    fun foldable() {
        show()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tablet() {
        show()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phonePermission() {
        show(permission = CameraPermission.NEEDS_REQUEST)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneResult() {
        composeRule.setContent { AppTheme { ResultSheetContent(listOf(pikachu), noArt, onOpenSpecies = {}, onDismiss = {}) } }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneChoices() {
        composeRule.setContent { AppTheme { ResultSheetContent(listOf(pidgey, pikachu), noArt, onOpenSpecies = {}, onDismiss = {}) } }
        composeRule.onRoot().captureRoboImage()
    }
}
