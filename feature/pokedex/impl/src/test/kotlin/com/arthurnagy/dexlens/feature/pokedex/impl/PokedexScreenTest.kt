package com.arthurnagy.dexlens.feature.pokedex.impl

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.designsystem.CameraColors
import com.arthurnagy.dexlens.core.designsystem.LightScheme
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.ObserveScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentOfferUseCase
import com.arthurnagy.dexlens.core.domain.scan.BuildSpeciesSignalUseCase
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesListUseCase
import com.arthurnagy.dexlens.core.domain.species.SearchSpeciesUseCase
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.arthurnagy.dexlens.core.testing.speciesSummary
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.AimBox
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraPermission
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraStatus
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.ScanState
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.aimBoxFor
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.scannerAreaHeight
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.topBarAlpha
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class PokedexScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pikachu = speciesSummary(25, "Pikachu", PokemonType.ELECTRIC)
    private val pidgey = speciesSummary(16, "Pidgey", PokemonType.NORMAL, PokemonType.FLYING)
    private val loaded = PokedexState.Initial.copy(
        loading = false,
        scannerEnabled = true,
        results = listOf(
            SpeciesSearchResult(speciesSummary(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON), matchedName = null),
            SpeciesSearchResult(speciesSummary(4, "Charmander", PokemonType.FIRE), matchedName = null),
            SpeciesSearchResult(speciesSummary(7, "Squirtle", PokemonType.WATER), matchedName = null),
            SpeciesSearchResult(pikachu, matchedName = "ピカチュウ"),
            SpeciesSearchResult(speciesSummary(122, "Mr. Mime", PokemonType.PSYCHIC, PokemonType.FAIRY), matchedName = null),
        ),
    )
    private val scannerOff = loaded.copy(scannerEnabled = false)
    private val matched = loaded.copy(scan = ScanState.Initial.copy(match = listOf(pikachu)))
    private val ready = CameraStatus(CameraPermission.GRANTED, failed = false, torchAvailable = true)
    private val needsPermission = ready.copy(permission = CameraPermission.NEEDS_REQUEST)

    private var viewfinderStarts = 0
    private var viewfinderRunning = false

    /** The aiming box the viewfinder was last handed: what the analyser would crop, in the viewfinder's own fractions. */
    private var viewfinderBox: AimBox? = null

    @Composable
    private fun ViewfinderStub(box: AimBox) {
        viewfinderBox = box
        DisposableEffect(Unit) {
            viewfinderStarts++
            viewfinderRunning = true
            onDispose { viewfinderRunning = false }
        }
        Box(Modifier.fillMaxSize().background(Color.DarkGray).testTag("test:viewfinder"))
    }

    private var currentState by mutableStateOf(loaded)
    private var homeShowing by mutableStateOf(true)

    private fun show(
        state: PokedexState = loaded,
        text: TextFieldState = TextFieldState(),
        camera: CameraStatus = ready,
        canNavigateBack: Boolean = false,
        onOpen: (Int) -> Unit = {},
        onRetry: () -> Unit = {},
        onSettings: () -> Unit = {},
        onClearMatch: () -> Unit = {},
        onTorch: () -> Unit = {},
        onRequest: () -> Unit = {},
        onAppSettings: () -> Unit = {},
        // What a back camera with sensor orientation 90 reports: portrait in portrait windows, landscape otherwise.
        uprightPortrait: Boolean? = portraitWindow(),
        // Production shows home as a 360dp list pane from medium width up.
        pane: Boolean = false,
    ) {
        currentState = state
        composeRule.setContent {
            AppTheme {
                if (homeShowing) {
                    PokedexContent(
                        state = currentState,
                        textFieldState = text,
                        camera = camera,
                        canNavigateBack = canNavigateBack,
                        onOpenSpecies = onOpen,
                        onOpenSettings = onSettings,
                        onRetry = onRetry,
                        onClearMatch = onClearMatch,
                        onToggleTorch = onTorch,
                        onRequestPermission = onRequest,
                        onOpenAppSettings = onAppSettings,
                        cameraUprightPortrait = { uprightPortrait },
                        modifier = if (pane) Modifier.width(PANE_WIDTH) else Modifier,
                        viewfinder = { ViewfinderStub(it) },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun portraitWindow() = composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    private fun rowExists(id: Int) =
        composeRule.onAllNodesWithTag("pokedex:row:$id", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun awaitRow(id: Int) = composeRule.waitUntil(WAIT_MS) { rowExists(id) }

    private fun awaitRowGone(id: Int) = composeRule.waitUntil(WAIT_MS) { !rowExists(id) }

    private fun tagExists(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun expandSheet() {
        composeRule.onNodeWithTag("pokedex:search").performTouchInput { swipeUp(startY = centerY, endY = 0f) }
        composeRule.waitForIdle()
    }

    private fun collapseSheet() {
        composeRule.onNodeWithTag("pokedex:search").performTouchInput { swipeDown(startY = centerY, endY = bottom + SWIPE_DISTANCE) }
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    // --- The list (pokedex-browse) ---

    // Scenario: List in Pokédex order
    @Test
    fun rowsShowNumberAndName() {
        show()
        composeRule.onNodeWithText("#0001", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Bulbasaur", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Fire", useUnmergedTree = true).assertExists()
    }

    // Scenario: Shows loading state
    @Test
    fun loadingShowsASkeleton() {
        show(PokedexState.Initial.copy(scannerEnabled = true))
        composeRule.onNodeWithTag("pokedex:list").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Loading").assertExists()
    }

    // Scenario: Shows error state
    @Test
    fun errorOffersRetry() {
        var retried = false
        show(PokedexState.Initial.copy(loading = false, scannerEnabled = true, error = DomainError.Unknown("x")), onRetry = {
            retried = true
        })
        composeRule.onNodeWithText("Try again").performClick()
        assertEquals(true, retried)
    }

    // Scenario: No results shows empty state
    @Test
    fun emptyResultsNameTheQuery() {
        show(PokedexState.Initial.copy(loading = false, scannerEnabled = true, query = "zzz"))
        composeRule.onNodeWithText("No Pokémon named “zzz”").assertExists()
    }

    // Scenario: Tapping opens the entry
    @Test
    fun tappingARowOpensIt() {
        var opened = 0
        show(onOpen = { opened = it })
        composeRule.onNodeWithTag("pokedex:row:25").performClick()
        assertEquals(25, opened)
    }

    // Scenario: List item announces details
    @Test
    fun rowAnnouncesNumberNameAndTypes() {
        show()
        composeRule.onNodeWithTag("pokedex:row:1", useUnmergedTree = true).assertContentDescriptionEquals("#0001, Bulbasaur, Grass, Poison")
    }

    // Scenario: Search other language
    @Test
    fun matchedNameIsShown() {
        show()
        composeRule.onNodeWithText("ピカチュウ", useUnmergedTree = true).assertExists()
    }

    // Scenario: Clearing restores the list
    // Scenario: Rationale before asking
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun rootScreenSearchesAsYouTypeAndAsksForTheCameraFirst() {
        val summaries = loaded.results.map { it.summary }
        val repository = FakeSpeciesRepository(
            summaries = summaries,
            hits = mapOf("\"pika\"*" to listOf(SpeciesNameHit(SpeciesId(25), "Pikachu"))),
        )
        val preferences = FakeUserPreferencesRepository(ThemeMode.SYSTEM, initialOfferShown = true)
        val viewModel = PokedexViewModel(
            SearchSpeciesUseCase(repository),
            ObserveArtworkUseCase(FakeContentPackRepository()),
            ObserveContentOfferUseCase(preferences),
            ObserveScannerEnabledUseCase(preferences),
            BuildSpeciesSignalUseCase(repository, TestDispatcherProvider(TestCoroutineScheduler())),
            GetSpeciesListUseCase(repository),
        )
        composeRule.setContent {
            AppTheme {
                PokedexScreen(onOpenSpecies = {}, onOpenSettings = {}, onShowOffer = {}, canNavigateBack = false, viewModel = viewModel)
            }
        }
        awaitRow(1)
        composeRule.onNodeWithText("Allow camera").assertExists()
        composeRule.onNodeWithTag("pokedex:search").performTextInput("pika")
        awaitRowGone(1)
        composeRule.onNodeWithTag("pokedex:row:25", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("pokedex:clear").performClick()
        awaitRow(1)
    }

    // --- Layout ---

    // Scenario: Home shows scanner and sheet
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun scannerSitsAboveTheSheet() {
        show()
        assertTrue(viewfinderRunning)
        val area = composeRule.onNodeWithTag("pokedex:scannerArea").getUnclippedBoundsInRoot()
        val search = composeRule.onNodeWithTag("pokedex:search").getUnclippedBoundsInRoot()
        assertEquals(area.right, composeRule.onRoot().getUnclippedBoundsInRoot().right)
        assertTrue(search.top >= area.bottom)
        // 3:2 (Robolectric reports no status bar inset).
        assertEquals((area.spanX / 1.5f).value, area.spanY.value, 1f)
        // The camera reaches the sheet's corner radius past the area at both ends.
        val viewfinder = composeRule.onNodeWithTag("test:viewfinder").getUnclippedBoundsInRoot()
        assertEquals((area.top - 28.dp).value, viewfinder.top.value, 1f)
        assertEquals((area.bottom + 28.dp).value, viewfinder.bottom.value, 1f)
    }

    // Scenario: No separate scan step
    @Test
    fun thereIsNoScanButton() {
        show()
        composeRule.onNodeWithTag("pokedex:scan").assertDoesNotExist()
        composeRule.onNodeWithText("Scan").assertDoesNotExist()
    }

    // Scenario: Short window keeps the list reachable
    @Test
    fun theAreaIsCappedInAShortWindow() {
        assertEquals(274f, scannerAreaHeight(411.dp, 914.dp, 0.dp).value, 1f)
        assertEquals(298f, scannerAreaHeight(411.dp, 914.dp, 24.dp).value, 1f)
        assertEquals(185f, scannerAreaHeight(914.dp, 411.dp, 0.dp).value, 1f)
    }

    // Scenario: Short window keeps the list reachable
    @Test
    @Config(qualifiers = "w914dp-h411dp-xxhdpi")
    fun theSearchFieldStaysVisibleInAShortWindow() {
        show()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        assertTrue(composeRule.onNodeWithTag("pokedex:search").getUnclippedBoundsInRoot().bottom <= root.bottom)
    }

    // Scenario: Wide window keeps list and entry together
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun aWideWindowShowsScannerAndSheet() {
        show()
        assertTrue(viewfinderRunning)
        composeRule.onNodeWithTag("pokedex:search").assertExists()
    }

    // Scenario: Entry beside the list keeps scanning
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun anEntryBesideTheListKeepsTheCameraRunning() {
        show(canNavigateBack = true)
        assertTrue(viewfinderRunning)
    }

    // --- The sheet's corners ---

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun Dp.px() = with(composeRule.density) { roundToPx() }

    /** The pixels just inside each of the sheet's rounded corners, and the pixels just above the sheet at the same x. */
    private fun cornerPixels(): List<Pair<Color, Color>> {
        val area = bounds("pokedex:scannerArea")
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val edge = area.bottom.px()
        return listOf(area.left.px() + 2, area.right.px() - 3).map { x -> image[x, edge + 2] to image[x, edge - 3] }
    }

    /** The aiming box the viewfinder was handed, laid onto the viewfinder's own bounds. */
    private fun analysedBox(): DpRect {
        val viewfinder = bounds("test:viewfinder")
        val box = checkNotNull(viewfinderBox)
        return DpRect(
            left = viewfinder.left + viewfinder.spanX * box.left,
            top = viewfinder.top + viewfinder.spanY * box.top,
            right = viewfinder.left + viewfinder.spanX * box.right,
            bottom = viewfinder.top + viewfinder.spanY * box.bottom,
        )
    }

    /** The aiming box laid out on the visible area alone, as the unfilled layout always has. */
    private fun visibleAimBox(): AimBox {
        val area = bounds("pokedex:scannerArea")
        val density = composeRule.density
        return with(density) {
            aimBoxFor(
                width = area.spanX.roundToPx().toFloat(),
                height = area.spanY.roundToPx().toFloat(),
                topInset = 0f,
                reservedWidth = AppDimens.scannerButtonRowWidth.toPx(),
                buttonRowHeight = AppDimens.scannerButtonRowHeight.toPx(),
                margin = AppSpacing.md.toPx(),
            )
        }
    }

    private fun assertCornersShowTheCamera() {
        cornerPixels().forEachIndexed { side, (corner, above) ->
            assertNotEquals(CameraColors.background, corner, "corner $side is not the dark background")
            assertEquals(above, corner, "corner $side shows what the camera shows just above the sheet")
        }
    }

    private fun assertCornersStayDarkAndNothingChanges() {
        val area = bounds("pokedex:scannerArea")
        val viewfinder = bounds("test:viewfinder")
        assertEquals(area.top.value, viewfinder.top.value, 0.5f)
        assertEquals(area.bottom.value, viewfinder.bottom.value, 0.5f)
        cornerPixels().forEachIndexed { side, (corner, _) -> assertEquals(CameraColors.background, corner, "corner $side") }
        assertEquals(visibleAimBox(), viewfinderBox, "the analysed region is the visible area's own box")
    }

    // Scenario: Camera behind the sheet corners
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun aPhoneInPortraitShowsTheCameraBehindTheCorners() {
        show()
        assertCornersShowTheCamera()
    }

    // Scenario: Camera behind the sheet corners
    @Test
    @Config(qualifiers = PORTRAIT_TABLET)
    fun aPortraitTabletPaneShowsTheCameraBehindTheCorners() {
        show(pane = true)
        assertCornersShowTheCamera()
    }

    // Scenario: Corners where filling would enlarge the preview
    // Scenario: Preview under the sheet not analysed
    @Test
    @Config(qualifiers = "w914dp-h411dp-xxhdpi")
    fun aPhoneInLandscapeKeepsItsCorners() {
        show(pane = true)
        assertCornersStayDarkAndNothingChanges()
    }

    // Scenario: Corners where filling would enlarge the preview
    // Scenario: Preview under the sheet not analysed
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun aLandscapeTabletPaneKeepsItsCorners() {
        show(pane = true)
        assertCornersStayDarkAndNothingChanges()
    }

    // Scenario: Corners where filling would enlarge the preview
    // Scenario: Preview under the sheet not analysed
    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun aCompactWindowOnALandscapeDisplayKeepsItsCorners() {
        show(uprightPortrait = false)
        assertCornersStayDarkAndNothingChanges()
    }

    // Scenario: Preview under the sheet not analysed
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun thePhoneAnalysesOnlyTheDrawnBox() {
        show()
        assertAnalysedBoxIsTheDrawnBox()
    }

    // Scenario: Preview under the sheet not analysed
    @Test
    @Config(qualifiers = PORTRAIT_TABLET)
    fun thePortraitTabletPaneAnalysesOnlyTheDrawnBox() {
        show(pane = true)
        assertAnalysedBoxIsTheDrawnBox()
    }

    private fun assertAnalysedBoxIsTheDrawnBox() {
        val analysed = analysedBox()
        val drawn = bounds("pokedex:aimBox")
        val onePixel = 1f / composeRule.density.density
        assertEquals(drawn.left.value, analysed.left.value, onePixel)
        assertEquals(drawn.top.value, analysed.top.value, onePixel)
        assertEquals(drawn.right.value, analysed.right.value, onePixel)
        assertEquals(drawn.bottom.value, analysed.bottom.value, onePixel)
        assertTrue(analysed.bottom < bounds("pokedex:scannerArea").bottom, "the analysed box ends above the sheet")
    }

    // Scenario: Corner preview keeps the scanner layout
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun thePhoneKeepsItsScannerLayout() {
        show()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        assertScannerLayoutKept(scannerAreaHeight(root.spanX, root.spanY, 0.dp))
    }

    // Scenario: Corner preview keeps the scanner layout
    @Test
    @Config(qualifiers = PORTRAIT_TABLET)
    fun thePortraitTabletPaneKeepsItsScannerLayout() {
        show(pane = true)
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        assertScannerLayoutKept(scannerAreaHeight(PANE_WIDTH, root.spanY, 0.dp))
    }

    private fun assertScannerLayoutKept(expectedHeight: Dp) {
        val area = bounds("pokedex:scannerArea")
        assertEquals(expectedHeight.value, area.spanY.value, 0.5f)
        // The sheet's top edge is the area's bottom.
        assertTrue(bounds("pokedex:search").top >= area.bottom)
        listOf("pokedex:aimBox", "pokedex:scanLabel", "pokedex:torch", "pokedex:settings").forEach {
            assertTrue(bounds(it).bottom <= area.bottom, "$it ends above the sheet")
        }
    }

    // --- The scanner area ---

    // Scenario: Hint and aiming box shown
    @Test
    fun showsHintAndAimingBox() {
        show()
        composeRule.onNodeWithText("Point at a Pokémon or its name").assertExists()
        composeRule.onNodeWithTag("pokedex:aimBox").assertExists()
    }

    // Scenario: Match named in the aiming box
    @Test
    fun theMatchReplacesTheHintAndIsAnnounced() {
        show(matched)
        composeRule.onNodeWithText("Point at a Pokémon or its name").assertDoesNotExist()
        composeRule
            .onNodeWithTag("pokedex:scanLabel")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("Pikachu"))))
    }

    // Scenario: Match filters the list
    @Test
    fun theMatchIsTheOnlyRow() {
        show(matched)
        awaitRow(25)
        assertFalse(rowExists(1))
    }

    // Scenario: Several names offered as choices
    @Test
    fun severalMatchesAreBothListed() {
        show(loaded.copy(scan = ScanState.Initial.copy(match = listOf(pidgey, pikachu))))
        awaitRow(16)
        awaitRow(25)
    }

    // Scenario: Match opens the entry
    @Test
    fun tappingTheMatchOpensItsEntry() {
        var opened = 0
        show(matched, onOpen = { opened = it })
        composeRule.onNodeWithTag("pokedex:row:25").performClick()
        assertEquals(25, opened)
    }

    // Scenario: Clearing a match restores the list
    @Test
    fun theClearButtonClearsAMatch() {
        var cleared = false
        show(matched, onClearMatch = { cleared = true })
        composeRule.onNodeWithTag("pokedex:clear").performClick()
        assertTrue(cleared)
    }

    @Test
    fun noClearButtonWithoutTextOrAMatch() {
        show()
        composeRule.onNodeWithTag("pokedex:clear").assertDoesNotExist()
    }

    // Scenario: Settings opens from the scanner area
    @Test
    fun settingsButtonOpensSettings() {
        var opened = false
        show(onSettings = { opened = true })
        composeRule.onNodeWithTag("pokedex:settings").performClick()
        assertEquals(true, opened)
    }

    // Scenario: Torch toggles
    @Test
    fun torchButtonToggles() {
        var toggled = false
        show(onTorch = { toggled = true })
        composeRule.onNodeWithContentDescription("Turn torch on").performClick()
        assertTrue(toggled)
    }

    // Scenario: Torch toggles
    @Test
    fun torchButtonShowsItsCurrentState() {
        show(loaded.copy(scan = ScanState.Initial.copy(torchOn = true)))
        composeRule.onNodeWithContentDescription("Turn torch off").assertExists()
    }

    @Test
    fun torchHiddenWithoutAFlash() {
        show(camera = ready.copy(torchAvailable = false))
        composeRule.onNodeWithTag("pokedex:torch").assertDoesNotExist()
    }

    // Scenario: Rationale before asking
    @Test
    fun rationaleOffersToAllowTheCamera() {
        var requested = false
        show(camera = needsPermission, onRequest = { requested = true })
        composeRule.onNodeWithText("Recognise Pokémon by camera").assertExists()
        composeRule.onNodeWithText("Allow camera").performClick()
        assertTrue(requested)
    }

    // Scenario: System dialog waits for a tap
    @Test
    fun theDialogIsNotRequestedUntilTheTap() {
        var requested = false
        show(camera = needsPermission, onRequest = { requested = true })
        assertFalse(requested)
        assertFalse(viewfinderRunning)
    }

    // Scenario: Denied permanently
    @Test
    fun permanentDenialOffersSettings() {
        var opened = false
        show(camera = ready.copy(permission = CameraPermission.PERMANENTLY_DENIED), onAppSettings = { opened = true })
        composeRule.onNodeWithText("to recognise Pokémon. You can still search by typing.", substring = true).assertExists()
        composeRule.onNodeWithText("Open settings").performClick()
        assertTrue(opened)
    }

    // Scenario: Search works without the camera
    @Test
    fun theListWorksWithoutTheCamera() {
        show(camera = needsPermission)
        awaitRow(1)
        composeRule.onNodeWithTag("pokedex:search").assertExists()
        composeRule.onNodeWithTag("pokedex:torch").assertDoesNotExist()
    }

    // Scenario: Search works without the camera
    @Test
    fun cameraFailureIsExplainedAndTheListStays() {
        show(camera = ready.copy(failed = true))
        composeRule.onNodeWithText("This device has no camera DexLens can use.").assertExists()
        awaitRow(1)
        assertFalse(viewfinderRunning)
    }

    @Test
    fun aMatcherFailureIsExplained() {
        show(loaded.copy(scan = ScanState.Initial.copy(error = DomainError.Unknown("x"))))
        assertFalse(viewfinderRunning)
    }

    // --- The sheet ---

    // Scenario: Tapping search expands the sheet
    // Scenario: Expanding stops the camera
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun focusingSearchExpandsTheSheetAndStopsTheCamera() {
        show()
        assertTrue(viewfinderRunning)
        composeRule.onNodeWithTag("pokedex:search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pokedex:search").assertIsFocused()
        assertFalse(viewfinderRunning)
        composeRule.onNodeWithTag("pokedex:torch").assertDoesNotExist()
        composeRule.onNodeWithTag("pokedex:aimBox").assertDoesNotExist()
    }

    // Scenario: Top bar above the expanded sheet
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun theExpandedSheetHasASurfaceColouredTopBar() {
        show()
        expandSheet()
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        assertEquals(LightScheme.surfaceContainer, image[image.width / 4, TOP_BAR_PIXEL])
        // The sheet keeps the plain surface, so the bar reads as its own band above it.
        val searchTop = with(composeRule.density) {
            composeRule.onNodeWithTag("pokedex:search").getUnclippedBoundsInRoot().top.roundToPx()
        }
        assertEquals(LightScheme.surface, image[image.width / 4, searchTop - SHEET_PIXEL_ABOVE_SEARCH])
    }

    // Scenario: Top bar above the expanded sheet
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun theCollapsedSheetLeavesTheCameraShowing() {
        show()
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        assertNotEquals(LightScheme.surfaceContainer, image[image.width / 4, TOP_BAR_PIXEL])
    }

    // Scenario: Top bar above the expanded sheet
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun theTitleAppearsOnlyOnTheTopBar() {
        show()
        composeRule.onNodeWithText("DexLens").assertDoesNotExist()
        expandSheet()
        val title = composeRule.onNode(hasText("DexLens") and isHeading())
        title.assertIsDisplayed()
        val settings = composeRule.onNodeWithTag("pokedex:settings").getUnclippedBoundsInRoot()
        assertTrue(title.getUnclippedBoundsInRoot().right <= settings.left, "title beside settings")
    }

    // Scenario: Top bar above the expanded sheet
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun statusBarIconsFollowTheTopBar() {
        val window = composeRule.activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        composeRule.runOnUiThread { controller.isAppearanceLightStatusBars = true }
        show()
        assertFalse(controller.isAppearanceLightStatusBars, "light icons over the camera")
        expandSheet()
        assertTrue(controller.isAppearanceLightStatusBars, "the theme's dark icons on the top bar")
    }

    // Scenario: Camera fades into the top bar
    @Test
    fun theCameraFadesWithTheSheet() {
        assertEquals(0f, topBarAlpha(sheetOffset = 800f, collapsedOffset = 800f, expandedOffset = 200f))
        assertEquals(0.5f, topBarAlpha(sheetOffset = 500f, collapsedOffset = 800f, expandedOffset = 200f))
        assertEquals(1f, topBarAlpha(sheetOffset = 200f, collapsedOffset = 800f, expandedOffset = 200f))
    }

    // Scenario: Dragging up expands the sheet
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun draggingUpExpandsWithoutFocus() {
        show()
        expandSheet()
        assertFalse(viewfinderRunning)
        val focused = composeRule.onNodeWithTag("pokedex:search").fetchSemanticsNode().config[SemanticsProperties.Focused]
        assertFalse(focused)
    }

    // Scenario: Settings stays while searching
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun settingsStaysAboveTheExpandedSheet() {
        show()
        expandSheet()
        val settings = composeRule.onNodeWithTag("pokedex:settings").getUnclippedBoundsInRoot()
        val search = composeRule.onNodeWithTag("pokedex:search").getUnclippedBoundsInRoot()
        assertTrue(settings.bottom <= search.top, "settings ${settings.bottom} above search ${search.top}")
    }

    // Scenario: Collapsing restarts the camera
    // Scenario: Torch returns with the camera
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun collapsingRestartsTheCameraWithItsTorchState() {
        show(loaded.copy(scan = ScanState.Initial.copy(torchOn = true)))
        expandSheet()
        assertFalse(viewfinderRunning)
        collapseSheet()
        assertTrue(viewfinderRunning)
        assertEquals(2, viewfinderStarts)
        composeRule.onNodeWithContentDescription("Turn torch off").assertExists()
    }

    // Scenario: Covering home stops the camera
    @Test
    fun leavingTheScreenStopsTheCamera() {
        show()
        assertTrue(viewfinderRunning)
        homeShowing = false
        composeRule.waitForIdle()
        assertFalse(viewfinderRunning)
        homeShowing = true
        composeRule.waitForIdle()
        assertTrue(viewfinderRunning)
    }

    // Scenario: Collapsing clears the typed text
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun collapsingClearsTheTypedText() {
        val text = TextFieldState()
        show(text = text)
        expandSheet()
        composeRule.runOnUiThread { text.setTextAndPlaceCursorAtEnd("pika") }
        collapseSheet()
        assertEquals("", text.text.toString())
    }

    // Scenario: Back collapses the expanded sheet
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun backCollapsesTheExpandedSheet() {
        val text = TextFieldState()
        show(text = text)
        expandSheet()
        composeRule.runOnUiThread { text.setTextAndPlaceCursorAtEnd("pika") }
        pressBack()
        assertTrue(viewfinderRunning)
        assertEquals("", text.text.toString())
    }

    // Scenario: Clearing text keeps the sheet expanded
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun clearingTextKeepsTheSheetExpanded() {
        val text = TextFieldState()
        show(text = text)
        expandSheet()
        composeRule.runOnUiThread { text.setTextAndPlaceCursorAtEnd("pika") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pokedex:clear").performClick()
        composeRule.waitForIdle()
        assertEquals("", text.text.toString())
        assertFalse(viewfinderRunning)
    }

    // Scenario: Back clears a match
    @Test
    fun backClearsAMatchWhileCollapsed() {
        var cleared = false
        show(matched, onClearMatch = { cleared = true })
        pressBack()
        assertTrue(cleared)
    }

    // Scenario: Back clears a match
    @Test
    fun backLeavesAMatchWithAnEntryBesideIt() {
        var cleared = false
        show(matched, canNavigateBack = true, onClearMatch = { cleared = true })
        pressBack()
        assertFalse(cleared)
    }

    // Scenario: Match kept when the sheet is dragged
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun draggingTheSheetKeepsTheMatch() {
        var cleared = false
        show(matched, onClearMatch = { cleared = true })
        expandSheet()
        collapseSheet()
        assertFalse(cleared)
        awaitRow(25)
    }

    // Scenario: Returning from an entry keeps the search
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun typedTextStartsTheSheetExpandedOnReturn() {
        val text = TextFieldState("pika")
        show(loaded.copy(query = "pika"), text = text)
        assertFalse(viewfinderRunning)
        assertEquals("pika", text.text.toString())
    }

    // --- Scanner switched off ---

    // Scenario: Camera stays off at launch
    @Test
    fun nothingStartsUntilTheSwitchIsRead() {
        show(PokedexState.Initial.copy(loading = false, results = loaded.results))
        assertEquals(0, viewfinderStarts)
        composeRule.onNodeWithTag("pokedex:search").assertDoesNotExist()
    }

    // Scenario: Camera stays off at launch
    @Test
    fun theCameraNeverStartsWhenTheSwitchReadsOff() {
        show(PokedexState.Initial.copy(loading = false, results = loaded.results))
        currentState = scannerOff
        composeRule.waitForIdle()
        assertEquals(0, viewfinderStarts)
        awaitRow(1)
    }

    // Scenario: Scanner off shows only the list
    // Scenario: Scanner off has no corner gap
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun scannerOffShowsTheListBelowTheStrip() {
        show(scannerOff)
        assertEquals(0, viewfinderStarts)
        composeRule.onNodeWithTag("pokedex:scannerArea").assertDoesNotExist()
        // The list's surface starts right below the top bar: no camera, no corners, no gap.
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val barBottom = with(composeRule.density) { AppDimens.scannerButtonRowHeight.roundToPx() }
        assertEquals(LightScheme.surfaceContainer, image[2, barBottom - 2])
        assertEquals(LightScheme.surface, image[2, barBottom + 1])
        assertEquals(LightScheme.surface, image[image.width - 3, barBottom + 1])
        composeRule.onNodeWithTag("pokedex:aimBox").assertDoesNotExist()
        composeRule.onNodeWithTag("pokedex:torch").assertDoesNotExist()
        composeRule.onNode(hasText("DexLens") and isHeading()).assertIsDisplayed()
        val settings = composeRule.onNodeWithTag("pokedex:settings").getUnclippedBoundsInRoot()
        val search = composeRule.onNodeWithTag("pokedex:search").getUnclippedBoundsInRoot()
        assertTrue(settings.bottom <= search.top)
        val before = search.top
        collapseSheet()
        assertEquals(before, composeRule.onNodeWithTag("pokedex:search").getUnclippedBoundsInRoot().top)
    }

    // Scenario: No permission prompt with the scanner off
    @Test
    fun noPermissionPromptWithTheScannerOff() {
        show(scannerOff, camera = needsPermission)
        composeRule.onNodeWithText("Allow camera").assertDoesNotExist()
        composeRule.onNodeWithText("Recognise Pokémon by camera").assertDoesNotExist()
    }

    // Scenario: Search works with the scanner off
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun typingSearchesWithTheScannerOff() {
        val repository = FakeSpeciesRepository(
            summaries = loaded.results.map { it.summary },
            hits = mapOf("\"pika\"*" to listOf(SpeciesNameHit(SpeciesId(25), "Pikachu"))),
        )
        val preferences = FakeUserPreferencesRepository(ThemeMode.SYSTEM, initialOfferShown = true, initialScannerEnabled = false)
        val viewModel = PokedexViewModel(
            SearchSpeciesUseCase(repository),
            ObserveArtworkUseCase(FakeContentPackRepository()),
            ObserveContentOfferUseCase(preferences),
            ObserveScannerEnabledUseCase(preferences),
            BuildSpeciesSignalUseCase(repository, TestDispatcherProvider(TestCoroutineScheduler())),
            GetSpeciesListUseCase(repository),
        )
        composeRule.setContent {
            AppTheme {
                PokedexScreen(onOpenSpecies = {}, onOpenSettings = {}, onShowOffer = {}, canNavigateBack = false, viewModel = viewModel)
            }
        }
        awaitRow(1)
        composeRule.onNodeWithTag("pokedex:search").performTextInput("pika")
        awaitRowGone(1)
        composeRule.onNodeWithTag("pokedex:row:25", useUnmergedTree = true).assertExists()
    }

    // Scenario: Search works with the scanner off
    @Test
    fun theClearButtonWorksWithTheScannerOff() {
        val text = TextFieldState("pika")
        show(scannerOff.copy(query = "pika"), text = text)
        composeRule.onNodeWithTag("pokedex:clear").performClick()
        composeRule.waitForIdle()
        assertEquals("", text.text.toString())
    }

    // Scenario: Back clears typed text with the scanner off
    @Test
    fun backClearsTypedTextWithTheScannerOff() {
        val text = TextFieldState("pika")
        show(scannerOff.copy(query = "pika"), text = text)
        pressBack()
        assertEquals("", text.text.toString())
    }

    // Scenario: Back leaves with the scanner off
    @Test
    fun backIsNotHandledWithAnEmptySearchAndTheScannerOff() {
        var offeredToSystem = false
        show(scannerOff)
        composeRule.runOnUiThread {
            // Below the screen's handlers it would never run if the screen had taken Back for itself.
            composeRule.activity.onBackPressedDispatcher.addCallback(
                object : androidx.activity.OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        offeredToSystem = true
                    }
                },
            )
        }
        pressBack()
        assertTrue(offeredToSystem)
    }

    // Scenario: Turning the scanner off keeps a search
    @Test
    fun theTextSurvivesTurningTheScannerOff() {
        val text = TextFieldState("pika")
        show(loaded.copy(query = "pika"), text = text)
        currentState = scannerOff.copy(query = "pika")
        composeRule.waitForIdle()
        assertEquals("pika", text.text.toString())
        assertEquals(0, viewfinderStarts)
    }

    // Scenario: Turning the scanner on shows the camera
    @Test
    fun turningTheScannerOnStartsTheCamera() {
        show(scannerOff)
        currentState = loaded
        composeRule.waitForIdle()
        assertTrue(viewfinderRunning)
        composeRule.onNodeWithTag("pokedex:aimBox").assertExists()
    }

    // Scenario: Turning the scanner on during a search
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun turningTheScannerOnDuringASearchKeepsTheCameraOff() {
        val text = TextFieldState("pika")
        show(scannerOff.copy(query = "pika"), text = text)
        currentState = loaded.copy(query = "pika")
        composeRule.waitForIdle()
        assertEquals("pika", text.text.toString())
        assertEquals(0, viewfinderStarts)
        val settings = composeRule.onNodeWithTag("pokedex:settings").getUnclippedBoundsInRoot()
        val search = composeRule.onNodeWithTag("pokedex:search").getUnclippedBoundsInRoot()
        // Expanded: the search field sits right below the strip, not below a 3:2 scanner area.
        assertTrue(search.top - settings.bottom < 100.dp, "search ${search.top} below settings ${settings.bottom}")
    }

    // --- Goldens ---

    private fun expanded(state: PokedexState = loaded) {
        show(state)
        expandSheet()
    }

    // Scenario: Placeholder shown without art
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

    // Full window width, which production never shows on a tablet: the list-pane goldens below are the real layouts.
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tablet() {
        show()
        composeRule.onRoot().captureRoboImage()
    }

    // Scenario: Corners where filling would enlarge the preview
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletListPane() {
        show(pane = true)
        composeRule.onRoot().captureRoboImage()
    }

    // Scenario: Corners where filling would enlarge the preview
    @Test
    @Config(qualifiers = "w914dp-h411dp-xxhdpi")
    fun phoneLandscapePane() {
        show(pane = true)
        composeRule.onRoot().captureRoboImage()
    }

    // Scenario: Camera behind the sheet corners
    @Test
    @Config(qualifiers = PORTRAIT_TABLET)
    fun tabletPortraitPane() {
        show(pane = true)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun phoneDark() {
        show()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneLoading() {
        show(PokedexState.Initial.copy(scannerEnabled = true))
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletLoading() {
        show(PokedexState.Initial.copy(scannerEnabled = true))
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneMatch() {
        show(matched)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletMatch() {
        show(matched)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneExpanded() {
        expanded()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletExpanded() {
        expanded()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phonePermission() {
        show(camera = needsPermission)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletPermission() {
        show(camera = needsPermission)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneScannerOff() {
        show(scannerOff)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletScannerOff() {
        show(scannerOff)
        composeRule.onRoot().captureRoboImage()
    }

    private companion object {
        const val WAIT_MS = 5_000L
        const val TOP_BAR_PIXEL = 20
        const val SHEET_PIXEL_ABOVE_SEARCH = 4
        const val SWIPE_DISTANCE = 1_500f
        val PANE_WIDTH = 360.dp
        const val PORTRAIT_TABLET = "w800dp-h1280dp-xhdpi"
    }
}

private val DpRect.spanX: Dp get() = right - left

private val DpRect.spanY: Dp get() = bottom - top
