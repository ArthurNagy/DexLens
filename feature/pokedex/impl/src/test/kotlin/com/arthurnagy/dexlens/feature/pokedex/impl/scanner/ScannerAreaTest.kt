package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.designsystem.CameraColors
import com.arthurnagy.dexlens.core.designsystem.LightScheme
import com.arthurnagy.dexlens.core.domain.DomainError
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The scanner area on its own, on a Pixel 7: 411dp wide, a 274dp visible area and the sheet's 28dp corners. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
internal class ScannerAreaTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val ready = CameraStatus(CameraPermission.GRANTED, failed = false, torchAvailable = true)

    private var upright by mutableStateOf<Boolean?>(true)
    private var camera by mutableStateOf(ready)
    private var scanError by mutableStateOf<DomainError?>(null)
    private var alpha by mutableFloatStateOf(0f)
    private var viewfinderBox: AimBox? = null

    @Composable
    private fun ViewfinderStub(box: AimBox) {
        viewfinderBox = box
        Box(Modifier.fillMaxSize().background(Color.DarkGray).testTag("test:viewfinder"))
    }

    private fun show() {
        composeRule.setContent {
            AppTheme {
                Box(Modifier.testTag("test:host")) {
                    ScannerArea(
                        match = emptyList(),
                        scanError = scanError,
                        camera = camera,
                        cameraRunning = true,
                        topBarAlpha = { alpha },
                        height = HEIGHT,
                        cornerRadius = 28.dp,
                        cameraUprightPortrait = upright,
                        topInset = 0.dp,
                        onRequestPermission = {},
                        onOpenAppSettings = {},
                        viewfinder = { ViewfinderStub(it) },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun Dp.px() = with(composeRule.density) { roundToPx() }

    /** A pixel 2px into the strip under the sheet, and one 3px above the visible area's bottom, at the same x. */
    private fun stripAndAbove(): Pair<Color, Color> {
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val edge = HEIGHT.px()
        return image[2, edge + 2] to image[2, edge - 3]
    }

    /** The aiming box the viewfinder was handed, laid onto the viewfinder's own bounds: the region that is analysed. */
    private fun drawnBox(): DpRect {
        val viewfinder = bounds("test:viewfinder")
        val box = checkNotNull(viewfinderBox)
        val width = viewfinder.right - viewfinder.left
        val height = viewfinder.bottom - viewfinder.top
        return DpRect(
            left = viewfinder.left + width * box.left,
            top = viewfinder.top + height * box.top,
            right = viewfinder.left + width * box.right,
            bottom = viewfinder.top + height * box.bottom,
        )
    }

    private fun assertSame(
        expected: DpRect,
        actual: DpRect,
        what: String,
    ) {
        val onePixel = 1f / composeRule.density.density
        assertEquals(expected.left.value, actual.left.value, onePixel, "$what left")
        assertEquals(expected.top.value, actual.top.value, onePixel, "$what top")
        assertEquals(expected.right.value, actual.right.value, onePixel, "$what right")
        assertEquals(expected.bottom.value, actual.bottom.value, onePixel, "$what bottom")
    }

    // Scenario: Corner preview keeps the scanner layout
    @Test
    fun fillingTheCornersMovesNothingTheUserSees() {
        show()
        val area = bounds("pokedex:scannerArea")
        val viewfinder = bounds("test:viewfinder")
        assertEquals((area.top - 28.dp).value, viewfinder.top.value, 0.5f)
        assertEquals((area.bottom + 28.dp).value, viewfinder.bottom.value, 0.5f)
        val label = bounds("pokedex:scanLabel")
        val aimBox = bounds("pokedex:aimBox")
        assertSame(aimBox, drawnBox(), "the drawn box is the analysed box")

        upright = false
        composeRule.waitForIdle()
        assertSame(area, bounds("pokedex:scannerArea"), "visible area")
        assertSame(label, bounds("pokedex:scanLabel"), "label")
        assertSame(aimBox, bounds("pokedex:aimBox"), "aim box")
        assertSame(aimBox, drawnBox(), "the drawn box is the analysed box")
    }

    // Scenario: Corners where filling would enlarge the preview
    @Test
    fun withoutAGuaranteeTheAreaIsJustTheVisibleHeight() {
        upright = false
        show()
        listOf(false, null).forEach { orientation ->
            upright = orientation
            composeRule.waitForIdle()
            val area = bounds("pokedex:scannerArea")
            val viewfinder = bounds("test:viewfinder")
            assertEquals(area.top.value, viewfinder.top.value, 0.5f, "orientation $orientation")
            assertEquals(area.bottom.value, viewfinder.bottom.value, 0.5f, "orientation $orientation")
            val host = bounds("test:host")
            assertEquals(HEIGHT.value, (host.bottom - host.top).value, 0.5f, "orientation $orientation")
        }
    }

    // Scenario: Sheet corners fade with the camera
    @Test
    fun theStripUnderTheSheetFadesWithTheArea() {
        alpha = 0.5f
        show()
        val (halfway, aboveHalfway) = stripAndAbove()
        assertEquals(aboveHalfway, halfway)
        assertTrue(halfway != CameraColors.background, "not the dark background")

        alpha = 1f
        composeRule.waitForIdle()
        val (faded, aboveFaded) = stripAndAbove()
        assertEquals(LightScheme.surfaceContainer, faded)
        assertEquals(aboveFaded, faded)
    }

    // Scenario: Scanner messages fill the sheet corners
    @Test
    fun messagesLeaveTheDarkBackgroundBehindTheCornersAndStayCentred() {
        val states = listOf(
            Triple(ready.copy(permission = CameraPermission.NEEDS_REQUEST), null, "Allow camera"),
            Triple(ready.copy(failed = true), null, "This device has no camera DexLens can use."),
            // DomainError.Unknown's wording.
            Triple(ready, DomainError.Unknown("x"), "Something went wrong. Please try again."),
        )
        show()
        states.forEach { (status, error, text) ->
            camera = status
            scanError = error
            upright = true
            composeRule.waitForIdle()
            assertEquals(CameraColors.background, stripAndAbove().first, "behind the corners with $status $error")
            val area = bounds("pokedex:scannerArea")
            val filled = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot()
            assertTrue(filled.top >= area.top && filled.bottom <= area.bottom, "$text inside the visible area")
            assertEquals(((area.left + area.right) / 2).value, ((filled.left + filled.right) / 2).value, 1f, "$text centred")
            upright = false
            composeRule.waitForIdle()
            assertSame(filled, composeRule.onNodeWithText(text).getUnclippedBoundsInRoot(), "$text in the same place")
        }
    }

    private companion object {
        val HEIGHT = 274.dp
    }
}
