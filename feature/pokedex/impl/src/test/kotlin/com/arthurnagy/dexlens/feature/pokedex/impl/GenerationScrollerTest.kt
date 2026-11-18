package com.arthurnagy.dexlens.feature.pokedex.impl

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.GenerationStart
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.testing.nationalDexSummaries
import com.arthurnagy.dexlens.core.testing.nationalGenerationStarts
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraPermission
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraStatus
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.ScanState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The generation fast-scroll thumb and its screen-reader slider, over the home list in each layout. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
internal class GenerationScrollerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val summaries = nationalDexSummaries()
    private val fullList = PokedexState.Initial.copy(
        loading = false,
        scannerEnabled = true,
        results = summaries.map { SpeciesSearchResult(it, matchedName = null) },
        answeredQuery = "",
        generations = nationalGenerationStarts(),
    )
    private val camera = CameraStatus(CameraPermission.GRANTED, failed = false, torchAvailable = false)
    private var currentState by mutableStateOf(fullList)
    private val opened = mutableListOf<Int>()

    private fun show(
        state: PokedexState = fullList,
        text: TextFieldState = TextFieldState(),
        canNavigateBack: Boolean = false,
        // Production shows home as a 360dp list pane from medium width up.
        pane: Boolean = false,
        rtl: Boolean = false,
    ) {
        currentState = state
        val portrait = composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    PokedexContent(
                        state = currentState,
                        textFieldState = text,
                        camera = camera,
                        canNavigateBack = canNavigateBack,
                        onOpenSpecies = { opened += it },
                        onOpenSettings = {},
                        onRetry = {},
                        onClearMatch = {},
                        onToggleTorch = {},
                        onRequestPermission = {},
                        onOpenAppSettings = {},
                        cameraUprightPortrait = { portrait },
                        modifier = if (pane) Modifier.width(PANE_WIDTH) else Modifier,
                        viewfinder = { Box(Modifier.fillMaxSize().background(Color.DarkGray).testTag("test:viewfinder")) },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    // --- Helpers ---

    private fun node(tag: String): SemanticsNode = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()

    private fun bounds(tag: String): Rect = node(tag).boundsInRoot

    private fun exists(tag: String) = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun Dp.px() = with(composeRule.density) { toPx() }

    private val rowMatcher = SemanticsMatcher("a species row") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("pokedex:row:") == true
    }

    /** The species at the top of the list: the first row below the list's top padding. */
    private fun topRow(): Int {
        val listTop = bounds("pokedex:list").top + AppSpacing.xs.px()
        return composeRule
            .onAllNodes(rowMatcher, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { it.boundsInRoot.bottom > listTop + 1f }
            .minBy { it.boundsInRoot.top }
            .config[SemanticsProperties.TestTag]
            .removePrefix("pokedex:row:")
            .toInt()
    }

    /**
     * Holds the clock and lets [millis] pass. Waiting for idle doesn't run the thumb's hide delay, so the fade tests step
     * the clock by hand. (A lone `down` isn't dispatched while the clock is held, so gestures run before this.)
     */
    private fun hold(millis: Long) {
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(millis)
    }

    /** Scrolls the list by its semantics. The thumb shows, and stays until the clock is moved on. */
    private fun scrollTo(index: Int) {
        composeRule.onNodeWithTag("pokedex:list").performScrollToIndex(index)
        composeRule.waitForIdle()
    }

    /** Puts a finger on the thumb, which shows the ticks. */
    private fun pressThumb() {
        val centre = bounds("pokedex:fastScroll:control").center
        composeRule.onRoot().performTouchInput { down(centre) }
        composeRule.waitForIdle()
    }

    /** Moves the held finger to [generation]'s tick. */
    private fun moveTo(generation: Int) {
        val target = Offset(bounds("pokedex:fastScroll:control").center.x, bounds("pokedex:fastScroll:tick:$generation").center.y)
        composeRule.onRoot().performTouchInput { moveTo(target) }
        composeRule.waitForIdle()
    }

    private fun lift() {
        composeRule.onRoot().performTouchInput { up() }
        composeRule.waitForIdle()
    }

    private fun drag(generation: Int) {
        pressThumb()
        moveTo(generation)
        lift()
    }

    private fun expandSheet() {
        composeRule.onNodeWithTag("pokedex:search").performTouchInput { swipeUp(startY = centerY, endY = 0f) }
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    /**
     * The way a user brings the thumb up in the collapsed sheet: scroll the expanded list, collapse it with Back (which
     * keeps the position), then swipe the list down a little.
     */
    private fun revealInCollapsedSheet() {
        expandSheet()
        composeRule.onNodeWithTag("pokedex:list").performScrollToIndex(COLLAPSED_START)
        pressBack()
        composeRule.mainClock.advanceTimeBy(LONG_AFTER_MS)
        composeRule.waitForIdle()
        assertTrue(!exists("pokedex:fastScroll:thumb"), "no thumb once the list is still")
        composeRule.onNodeWithTag("pokedex:list").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + SHORT_SWIPE.dp.toPx(), durationMillis = SLOW_SWIPE_MS)
        }
        composeRule.waitForIdle()
        assertTrue(exists("pokedex:fastScroll:thumb"), "a short swipe down shows the thumb")
    }

    private fun assertState(text: String) {
        composeRule.onNodeWithTag(
            "pokedex:fastScroll:control",
        ).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text))
    }

    private fun assertNear(
        expected: Float,
        actual: Float,
        tolerance: Float = 1f,
    ) = assertTrue(abs(expected - actual) <= tolerance, "expected $expected ± $tolerance, was $actual")

    // --- Appearance ---

    // Scenario: Thumb appears while scrolling
    @Test
    fun scrollingShowsTheThumbOnTheEndEdge() {
        show(fullList.copy(scannerEnabled = false))
        composeRule.onNodeWithTag("pokedex:list").performTouchInput { swipeUp(startY = centerY, endY = top) }
        composeRule.waitForIdle()
        assertTrue(exists("pokedex:fastScroll:thumb"))
        assertNear(bounds("pokedex:list").right, bounds("pokedex:fastScroll:thumb").right, tolerance = AppSpacing.xs.px() + 1f)
    }

    // Scenario: Thumb on the left in right-to-left languages
    @Test
    fun rightToLeftPutsTheThumbOnTheLeft() {
        show(fullList.copy(scannerEnabled = false), rtl = true)
        scrollTo(INDEX_GEN_5)
        assertNear(bounds("pokedex:list").left, bounds("pokedex:fastScroll:thumb").left, tolerance = AppSpacing.xs.px() + 1f)
        pressThumb()
        assertTrue(bounds("pokedex:fastScroll:bubble").left >= bounds("pokedex:fastScroll:thumb").right)
    }

    // Scenario: Thumb fades after scrolling stops
    @Test
    fun theThumbFadesAfterScrollingStops() {
        show(fullList.copy(scannerEnabled = false))
        hold(0)
        composeRule.onNodeWithTag("pokedex:list").performScrollToIndex(INDEX_GEN_5)
        hold(SETTLE_MS)
        hold(STILL_SHOWN_MS - SETTLE_MS)
        assertTrue(exists("pokedex:fastScroll:thumb"), "still shown before the delay ends")
        hold(GONE_AFTER_MS)
        assertTrue(!exists("pokedex:fastScroll:thumb"))
        assertTrue(!exists("pokedex:fastScroll:bubble"))
        assertTrue(!exists("pokedex:fastScroll:tick:1"))
        assertTrue(exists("pokedex:fastScroll:control"), "the screen-reader control stays")
    }

    // Scenario: Still list keeps full-width rows
    @Test
    fun aStillListDrawsNoThumbAndRowsStayTappable() {
        show(fullList.copy(scannerEnabled = false))
        assertTrue(!exists("pokedex:fastScroll:thumb"))
        assertEquals(bounds("pokedex:list").width, bounds("pokedex:row:1").width)
        // Tap the end edge of whichever row sits under the hidden control.
        val control = bounds("pokedex:fastScroll:control").center
        val row = composeRule
            .onAllNodes(rowMatcher, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .first { control.y in it.boundsInRoot.top..it.boundsInRoot.bottom }
        val id = row.config[SemanticsProperties.TestTag].removePrefix("pokedex:row:").toInt()
        composeRule.onRoot().performTouchInput { click(Offset(row.boundsInRoot.right - END_TAP.dp.toPx(), control.y)) }
        composeRule.waitForIdle()
        assertEquals(listOf(id), opened)
    }

    // --- Dragging ---

    // Scenario: Dragging jumps to each generation's first species
    @Test
    fun draggingJumpsToEachGenerationsFirstSpecies() {
        show(fullList.copy(scannerEnabled = false))
        scrollTo(INDEX_GEN_2)
        pressThumb()
        for ((generation, first) in listOf(1 to 1, 4 to 387, 9 to 906)) {
            moveTo(generation)
            assertEquals(first, topRow())
            assertNear(bounds("pokedex:fastScroll:tick:$generation").center.y, bounds("pokedex:fastScroll:control").center.y)
        }
        lift()
    }

    // Scenario: Dragging jumps to each generation's first species
    @Test
    fun aSlowDragNeverJumpsBack() {
        show(fullList.copy(scannerEnabled = false))
        scrollTo(INDEX_GEN_2)
        pressThumb()
        moveTo(1)
        val end = bounds("pokedex:fastScroll:tick:9").center.y
        val seen = mutableListOf(topRow())
        while (bounds("pokedex:fastScroll:control").center.y < end - 1f && seen.size < MAX_STEPS) {
            composeRule.onRoot().performTouchInput { moveBy(Offset(0f, SLOW_STEP.dp.toPx())) }
            composeRule.waitForIdle()
            seen += topRow()
        }
        lift()
        assertEquals(906, seen.last())
        assertEquals(seen.sorted(), seen, "top rows only ever increase: $seen")
    }

    // Scenario: Bubble and ticks while dragging
    @Test
    fun holdingTheThumbShowsTheBubbleAndTicks() {
        show(fullList.copy(scannerEnabled = false))
        scrollTo(INDEX_GEN_2)
        pressThumb()
        moveTo(4)
        composeRule
            .onNodeWithTag("pokedex:fastScroll:bubble", useUnmergedTree = true)
            .assert(hasAnyDescendant(hasText("Generation 4 · Sinnoh")))
        assertTrue(bounds("pokedex:fastScroll:bubble").right <= bounds("pokedex:fastScroll:thumb").left)
        for (generation in 1..9) {
            composeRule
                .onNodeWithTag("pokedex:fastScroll:tick:$generation", useUnmergedTree = true)
                .assert(hasAnyDescendant(hasText("$generation")))
        }
        lift()
    }

    // Scenario: Bubble and ticks while dragging
    @Test
    fun aGenerationWithoutARegionNameShowsItsNumber() {
        show(fullList.copy(scannerEnabled = false, generations = nationalGenerationStarts() + GenerationStart(10, SpeciesId(1000))))
        composeRule.onNodeWithTag("pokedex:fastScroll:control").performSemanticsAction(SemanticsActions.SetProgress) { it(10f) }
        composeRule.waitForIdle()
        assertEquals(1000, topRow())
        assertState("Generation 10")
        pressThumb()
        composeRule
            .onNodeWithTag("pokedex:fastScroll:bubble", useUnmergedTree = true)
            .assert(hasAnyDescendant(hasText("Generation 10")))
        lift()
    }

    // Scenario: Lifting the finger clears the bubble
    @Test
    fun liftingClearsTheBubbleThenFades() {
        show(fullList.copy(scannerEnabled = false))
        scrollTo(INDEX_GEN_2)
        drag(4)
        assertTrue(!exists("pokedex:fastScroll:bubble"))
        assertTrue(!exists("pokedex:fastScroll:tick:4"))
        hold(LIFTED_STILL_SHOWN_MS)
        assertTrue(exists("pokedex:fastScroll:thumb"), "still shown before the delay ends")
        hold(LIFTED_GONE_AFTER_MS)
        assertTrue(!exists("pokedex:fastScroll:thumb"))
    }

    // Scenario: Thumb follows hand scrolling
    @Test
    fun theThumbFollowsTheTopRow() {
        show(fullList.copy(scannerEnabled = false))
        scrollTo(INDEX_GEN_2)
        pressThumb()
        val gen5 = bounds("pokedex:fastScroll:tick:5").center.y
        lift()
        scrollTo(INDEX_GEN_5)
        assertNear(gen5, bounds("pokedex:fastScroll:control").center.y)
    }

    // --- When there is no thumb ---

    // Scenario: No thumb over search results
    @Test
    fun searchResultsHaveNoThumb() {
        show(fullList.copy(scannerEnabled = false, query = "s", answeredQuery = "s"), text = TextFieldState("s"))
        scrollTo(INDEX_GEN_2)
        assertTrue(!exists("pokedex:fastScroll:thumb"))
        assertTrue(!exists("pokedex:fastScroll:control"))
    }

    // Scenario: No thumb over a scan match
    @Test
    fun aMatchHasNoThumb() {
        show(fullList.copy(scan = ScanState.Initial.copy(match = listOf(summaries[PIKACHU_INDEX]))))
        assertTrue(!exists("pokedex:fastScroll:thumb"))
        assertTrue(!exists("pokedex:fastScroll:control"))
    }

    // Scenario: No thumb while loading, failed or empty
    @Test
    fun loadingFailedOrEmptyHaveNoControl() {
        show(fullList.copy(loading = true, results = emptyList(), answeredQuery = null))
        assertTrue(!exists("pokedex:fastScroll:control"))
        currentState = fullList.copy(error = DomainError.Unknown("broken"), results = emptyList())
        composeRule.waitForIdle()
        assertTrue(!exists("pokedex:fastScroll:control"))
        currentState = fullList.copy(results = emptyList())
        composeRule.waitForIdle()
        assertTrue(!exists("pokedex:fastScroll:control"))
    }

    // Scenario: Thumb returns with the full list
    @Test
    fun theThumbReturnsWithTheFullList() {
        show(fullList.copy(scannerEnabled = false, query = "s", answeredQuery = "s"))
        assertTrue(!exists("pokedex:fastScroll:control"))
        currentState = fullList.copy(scannerEnabled = false)
        composeRule.waitForIdle()
        scrollTo(INDEX_GEN_5)
        assertTrue(exists("pokedex:fastScroll:thumb"))
    }

    // --- Screen readers ---

    // Scenario: TalkBack reaches the generation control
    @Test
    fun theControlIsASliderBeforeTheRows() {
        show(fullList.copy(scannerEnabled = false))
        composeRule
            .onNodeWithTag("pokedex:fastScroll:control")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(1f, 1f..9f, steps = 7)))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, -1f))
        assertState("Generation 1, Kanto")
        val group = node("pokedex:fastScroll:control").parent
        assertEquals(true, group?.config?.getOrNull(SemanticsProperties.IsTraversalGroup))
        assertTrue(group!!.children.any { it.config.getOrNull(SemanticsProperties.TestTag) == "pokedex:list" })
        // The search field comes first in the column above, so TalkBack goes search field, control, first row. Robolectric
        // doesn't expose the platform's traversal order, so the device check confirms the spoken order.
        assertTrue(bounds("pokedex:search").bottom <= group.boundsInRoot.top)
    }

    // Scenario: Adjusting the control jumps a generation
    @Test
    fun adjustingTheControlUpJumpsAGenerationAtATime() {
        show(fullList.copy(scannerEnabled = false))
        for ((value, first) in listOf(2f to 152, 3f to 252, 4f to 387)) {
            composeRule.onNodeWithTag("pokedex:fastScroll:control").performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
            composeRule.waitForIdle()
            assertEquals(first, topRow())
        }
        assertState("Generation 4, Sinnoh")
    }

    // Scenario: Adjusting the control down a generation
    @Test
    fun adjustingTheControlDownJumpsBack() {
        show(fullList.copy(scannerEnabled = false))
        val control = composeRule.onNodeWithTag("pokedex:fastScroll:control")
        control.performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        composeRule.waitForIdle()
        control.performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        composeRule.waitForIdle()
        assertEquals(252, topRow())
        assertState("Generation 3, Hoenn")
        control.performSemanticsAction(SemanticsActions.SetProgress) { it(LOW_VALUE) }
        composeRule.waitForIdle()
        assertEquals(1, topRow())
        control.performSemanticsAction(SemanticsActions.SetProgress) { it(HIGH_VALUE) }
        composeRule.waitForIdle()
        assertEquals(906, topRow())
        assertState("Generation 9, Paldea")
    }

    // Scenario: Control available while the thumb is hidden
    @Test
    fun theControlIsThereWhileTheThumbIsHidden() {
        show(fullList.copy(scannerEnabled = false))
        assertTrue(!exists("pokedex:fastScroll:thumb"))
        assertState("Generation 1, Kanto")
    }

    // Scenario: Control follows the list
    @Test
    fun theControlFollowsTheList() {
        show(fullList.copy(scannerEnabled = false))
        scrollTo(INDEX_GEN_7)
        assertState("Generation 7, Alola")
    }

    // --- Home layouts (home-screen) ---

    // Scenario: Thumb in the collapsed sheet
    @Test
    fun theThumbWorksInTheCollapsedSheet() {
        show()
        revealInCollapsedSheet()
        val search = bounds("pokedex:search")
        pressThumb()
        moveTo(9)
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(bounds("pokedex:fastScroll:tick:9").bottom <= root.bottom, "the ticks stay on screen")
        assertEquals(906, topRow())
        lift()
        assertEquals(search, bounds("pokedex:search"), "the sheet stays collapsed")
    }

    // Scenario: Thumb in the collapsed sheet
    @Test
    @Config(qualifiers = "w914dp-h411dp-xxhdpi")
    fun theThumbWorksInAShortCollapsedSheet() {
        show(pane = true)
        revealInCollapsedSheet()
        val search = bounds("pokedex:search")
        pressThumb()
        moveTo(9)
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(bounds("pokedex:fastScroll:tick:9").bottom <= root.bottom, "the ticks stay on screen")
        assertEquals(906, topRow())
        lift()
        assertEquals(search, bounds("pokedex:search"))
    }

    // Scenario: Swiping up in the collapsed sheet expands it first
    @Test
    fun swipingUpTheCollapsedListExpandsTheSheetFirst() {
        show()
        val collapsedSearch = bounds("pokedex:search")
        composeRule.onNodeWithTag("pokedex:list").performTouchInput {
            swipeUp(startY = centerY, endY = centerY - SHEET_SWIPE.dp.toPx(), durationMillis = SLOW_SWIPE_MS)
        }
        composeRule.waitForIdle()
        assertTrue(bounds("pokedex:search").top < collapsedSearch.top, "the sheet expanded")
        assertEquals(1, topRow())
        assertTrue(!exists("pokedex:fastScroll:thumb"))
    }

    // Scenario: Generation control in the collapsed sheet
    @Test
    fun theControlWorksInTheCollapsedSheet() {
        show()
        val search = bounds("pokedex:search")
        composeRule.onNodeWithTag("pokedex:fastScroll:control").performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        composeRule.waitForIdle()
        assertEquals(906, topRow())
        assertEquals(search, bounds("pokedex:search"))
    }

    // Scenario: Thumb in the expanded sheet
    @Test
    fun theThumbWorksInTheExpandedSheet() {
        show()
        expandSheet()
        val search = bounds("pokedex:search")
        scrollTo(INDEX_GEN_2)
        pressThumb()
        assertTrue(bounds("pokedex:fastScroll:tick:1").top >= search.bottom)
        moveTo(4)
        assertEquals(387, topRow())
        lift()
        assertEquals(search, bounds("pokedex:search"), "the sheet stays expanded")
    }

    // Scenario: Thumb with the scanner off
    @Test
    fun theThumbWorksWithTheScannerOff() {
        show(fullList.copy(scannerEnabled = false))
        scrollTo(INDEX_GEN_2)
        pressThumb()
        assertTrue(bounds("pokedex:fastScroll:tick:1").top >= bounds("pokedex:search").bottom)
        moveTo(4)
        assertEquals(387, topRow())
        lift()
    }

    // Scenario: Thumb beside an open entry
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun theThumbWorksBesideAnOpenEntry() {
        show(fullList.copy(scannerEnabled = false), pane = true, canNavigateBack = true)
        scrollTo(INDEX_GEN_2)
        pressThumb()
        moveTo(4)
        val paneRight = PANE_WIDTH.px()
        assertTrue(bounds("pokedex:fastScroll:control").right <= paneRight + 1f)
        assertTrue(bounds("pokedex:fastScroll:tick:9").right <= paneRight + 1f)
        assertTrue(bounds("pokedex:fastScroll:bubble").right <= paneRight + 1f)
        assertEquals(387, topRow())
        lift()
        assertEquals(emptyList(), opened, "no entry was opened, so the one beside the list stays")
    }

    // --- Goldens: held on generation 4, so the bubble and ticks show ---

    @Test
    fun phoneFastScroll() {
        show()
        revealInCollapsedSheet()
        pressThumb()
        moveTo(4)
        composeRule.onRoot().captureRoboImage()
        lift()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletFastScroll() {
        show(pane = true)
        revealInCollapsedSheet()
        pressThumb()
        moveTo(4)
        composeRule.onRoot().captureRoboImage()
        lift()
    }

    @Test
    fun phoneFastScrollRtl() {
        show(rtl = true)
        revealInCollapsedSheet()
        pressThumb()
        moveTo(4)
        composeRule.onRoot().captureRoboImage()
        lift()
    }

    private companion object {
        val PANE_WIDTH = 360.dp
        const val SETTLE_MS = 200L
        const val STILL_SHOWN_MS = 1_400L
        const val GONE_AFTER_MS = 500L
        const val LIFTED_STILL_SHOWN_MS = 1_300L
        const val LIFTED_GONE_AFTER_MS = 700L
        const val LONG_AFTER_MS = 2_000L
        const val INDEX_GEN_2 = 200
        const val INDEX_GEN_5 = 500
        const val INDEX_GEN_7 = 730
        const val COLLAPSED_START = 700
        const val PIKACHU_INDEX = 24
        const val SHORT_SWIPE = 80
        const val SHEET_SWIPE = 120
        const val SLOW_SWIPE_MS = 600L
        const val END_TAP = 8
        const val SLOW_STEP = 4
        const val MAX_STEPS = 400
        const val LOW_VALUE = 0.4f
        const val HIGH_VALUE = 12f
    }
}
