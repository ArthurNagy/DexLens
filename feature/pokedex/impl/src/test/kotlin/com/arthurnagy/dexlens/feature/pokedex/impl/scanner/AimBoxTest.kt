package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import org.junit.Test
import java.nio.ByteBuffer
import kotlin.math.roundToInt
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AimBoxTest {
    private val box = AimBox(left = 0.1f, top = 0.4f, right = 0.9f, bottom = 0.6f)

    // Scenario: Crop follows the aiming box
    @Test
    fun uprightBufferMapsDirectly() {
        assertEquals(CropRegion(100, 800, 800, 400), cropRegion(0, 0, 1000, 2000, 0, box))
    }

    // Scenario: Crop follows the aiming box
    @Test
    fun portraitPhoneBufferIsRotatedNinety() {
        // A 2000×1000 landscape sensor buffer shown upright in portrait.
        assertEquals(CropRegion(800, 100, 400, 800), cropRegion(0, 0, 2000, 1000, 90, box))
    }

    // Scenario: Crop follows the aiming box
    @Test
    fun otherRotationsMirrorTheBox() {
        assertEquals(CropRegion(100, 800, 800, 400), cropRegion(0, 0, 1000, 2000, 180, box))
        assertEquals(CropRegion(800, 100, 400, 800), cropRegion(0, 0, 2000, 1000, 270, box))
    }

    // Scenario: Crop follows the aiming box
    @Test
    fun viewportOffsetIsApplied() {
        assertEquals(CropRegion(640, 100, 360, 800), cropRegion(400, 0, 1600, 1000, 90, AimBox(0.1f, 0.2f, 0.9f, 0.5f)))
    }

    @Test
    fun cropIsEvenForNv21() {
        val region = cropRegion(1, 1, 1001, 1001, 0, AimBox(0.123f, 0.127f, 0.456f, 0.789f))
        assertEquals(0, region.left % 2)
        assertEquals(0, region.top % 2)
        assertEquals(0, region.width % 2)
        assertEquals(0, region.height % 2)
    }

    // Scenario: Aiming box clear of the controls
    @Test
    fun boxIsCentredAndClearOfTheControlsInEveryWindow() {
        // Pixel 6 portrait, a phone in landscape with a capped area, a small phone and a tablet's list pane, in pixels.
        val windows = listOf(
            Window(width = 1080f, height = 793f, topInset = 73f),
            Window(width = 2400f, height = 486f, topInset = 0f),
            Window(width = 960f, height = 720f, topInset = 72f),
            Window(width = 1200f, height = 800f, topInset = 0f),
        )
        windows.forEach { window ->
            val box = aimBoxFor(window.width, window.height, window.topInset, RESERVED, ROW_HEIGHT, MARGIN)
            val width = (box.right - box.left) * window.width
            val height = (box.bottom - box.top) * window.height
            assertTrue(width / height in 1.04f..1.51f, "proportions in $window")
            assertTrue(box.top * window.height > window.topInset, "below the status bar in $window")
            assertTrue(box.bottom * window.height < window.height, "inside the area in $window")
            assertEquals(box.left, 1f - box.right, 0.001f, "centred in $window")
            val belowButtons = box.top * window.height >= window.topInset + ROW_HEIGHT - 1f
            val besideButtons = (1f - box.right) * window.width >= RESERVED - 1f
            assertTrue(belowButtons || besideButtons, "clear of the buttons in $window")
        }
    }

    // Scenario: Aiming box clear of the controls
    @Test
    fun aPortraitPhoneGetsAWideBoxBelowTheButtons() {
        val density = 2.625f
        val box = aimBoxFor(411.4f * density, 298f * density, 24f * density, 104f * density, 56f * density, 16f * density)
        assertEquals(303f, (box.right - box.left) * 411.4f, 1f)
        assertEquals(202f, (box.bottom - box.top) * 298f, 1f)
        assertEquals(80f, box.top * 298f, 1f)
    }

    // Scenario: Aiming box clear of the controls
    @Test
    fun aShortLandscapeAreaKeepsTheNearSquareBesideTheButtons() {
        val box = aimBoxFor(2400f, 486f, 0f, RESERVED, ROW_HEIGHT, MARGIN)
        val width = (box.right - box.left) * 2400f
        val height = (box.bottom - box.top) * 486f
        assertEquals(1.05f, width / height, 0.01f)
        assertTrue(box.top * 486f < ROW_HEIGHT, "beside the button row, not below it")
    }

    private data class Window(
        val width: Float,
        val height: Float,
        val topInset: Float,
    )

    @Test
    fun lumaCropCopiesRowsAndFillsChroma() {
        // A 6×4 plane with a stride of 8.
        val stride = 8
        val plane = ByteBuffer.wrap(ByteArray(stride * 4) { it.toByte() })
        val nv21 = lumaCropToNv21(plane, stride, CropRegion(left = 2, top = 1, width = 4, height = 2))
        assertContentEquals(byteArrayOf(10, 11, 12, 13, 18, 19, 20, 21), nv21.copyOf(8))
        assertEquals(4 * 2 * 3 / 2, nv21.size)
        assertEquals(0x80.toByte(), nv21[8])
    }

    // --- The sheet's corners ---

    /** A scanner area in pixels, with the sheet's 28dp corner radius at the same density, rounded as the layout does. */
    private data class Area(
        val name: String,
        val density: Float,
        val widthDp: Float,
        val heightDp: Float,
        val topInsetDp: Float,
        val uprightPortrait: Boolean,
    ) {
        val widthPx = (widthDp * density).roundToInt()
        val heightPx = (heightDp * density).roundToInt()
        val radiusPx = (CORNER_DP * density).roundToInt()

        fun fits(uprightPortrait: Boolean? = this.uprightPortrait) = cornersFit(widthPx, heightPx, radiusPx, uprightPortrait)

        fun box() = aimBoxFor(
            widthPx.toFloat(),
            heightPx.toFloat(),
            topInsetDp * density,
            RESERVED_DP * density,
            ROW_HEIGHT_DP * density,
            MARGIN_DP * density,
        )
    }

    private val phonePortrait = Area("phone portrait", 2.625f, 411.4f, 298f, 24f, uprightPortrait = true)
    private val portraitTabletPane = Area("portrait tablet pane", 2f, 360f, 240f, 0f, uprightPortrait = true)
    private val phoneLandscapePane = Area("phone landscape pane", 2.625f, 360f, 185f, 0f, uprightPortrait = false)
    private val landscapeTabletPane = Area("landscape tablet pane", 2f, 360f, 240f, 0f, uprightPortrait = false)
    private val compactOnLandscapeDisplay = Area("compact window, landscape display", 2f, 400f, 291f, 24f, uprightPortrait = false)

    // Scenario: Camera behind the sheet corners
    @Test
    fun cornersFillOnAPhoneInPortraitAndAPortraitTabletPane() {
        assertTrue(phonePortrait.fits(), "phone portrait")
        assertTrue(portraitTabletPane.fits(), "portrait tablet pane")
    }

    // Scenario: Corners where filling would enlarge the preview
    @Test
    fun cornersStayWhereFillingWouldEnlargeThePreview() {
        assertFalse(phoneLandscapePane.fits(), "phone landscape pane")
        assertFalse(landscapeTabletPane.fits(), "landscape tablet pane")
        assertFalse(compactOnLandscapeDisplay.fits(), "compact window on a landscape display")
        // The window's own portrait shape would wrongly fill: the camera's orientation is the input, not the window's.
        assertTrue(compactOnLandscapeDisplay.fits(uprightPortrait = true))
        listOf(phonePortrait, portraitTabletPane, phoneLandscapePane).forEach {
            assertFalse(it.fits(uprightPortrait = null), "unknown orientation in ${it.name}")
        }
    }

    // Scenario: Corners where filling would enlarge the preview
    @Test
    fun theRuleIsExactAtItsBoundary() {
        // 300 wide in portrait guarantees 400 tall: 380 + 2 × 10 fits, one pixel more does not.
        assertTrue(cornersFit(widthPx = 300, visibleHeightPx = 380, radiusPx = 10, uprightPortrait = true))
        assertFalse(cornersFit(widthPx = 300, visibleHeightPx = 381, radiusPx = 10, uprightPortrait = true))
        // 320 wide in landscape guarantees 180 tall.
        assertTrue(cornersFit(widthPx = 320, visibleHeightPx = 160, radiusPx = 10, uprightPortrait = false))
        assertFalse(cornersFit(widthPx = 320, visibleHeightPx = 161, radiusPx = 10, uprightPortrait = false))
        assertFalse(cornersFit(widthPx = 300, visibleHeightPx = 100, radiusPx = 0, uprightPortrait = true), "no corners to fill")
    }

    // Scenario: Preview under the sheet not analysed
    @Test
    fun aBoxInAViewfinderOfTheSameHeightIsUnchanged() {
        assertEquals(box, box.inViewfinder(782f, 0f, 0f))
    }

    // Scenario: Preview under the sheet not analysed
    @Test
    fun fillingTheCornersKeepsTheAnalysedSensorRegion() {
        // The picture both streams share, 16:9 or 4:3, from a sensor buffer turned 90° to be upright in portrait.
        val pictures = listOf(Picture(bufferWidth = 1920, bufferHeight = 1080), Picture(bufferWidth = 1440, bufferHeight = 1080))
        listOf(phonePortrait, portraitTabletPane).forEach { area ->
            assertTrue(area.fits(), area.name)
            pictures.forEach { picture ->
                val visible = picture.viewportCrop(area.widthPx, area.heightPx)
                val taller = picture.viewportCrop(area.widthPx, area.heightPx + 2 * area.radiusPx)
                val box = area.box()
                val today = cropRegion(visible.left, visible.top, visible.right, visible.bottom, ROTATION, box)
                val filled = cropRegion(
                    taller.left,
                    taller.top,
                    taller.right,
                    taller.bottom,
                    ROTATION,
                    box.inViewfinder(area.heightPx.toFloat(), area.radiusPx.toFloat(), area.radiusPx.toFloat()),
                )
                val where = "${area.name}, ${picture.bufferWidth}×${picture.bufferHeight}"
                assertEquals(today.left.toFloat(), filled.left.toFloat(), 2f, "left in $where")
                assertEquals(today.top.toFloat(), filled.top.toFloat(), 2f, "top in $where")
                assertEquals((today.left + today.width).toFloat(), (filled.left + filled.width).toFloat(), 2f, "right in $where")
                assertEquals((today.top + today.height).toFloat(), (filled.top + filled.height).toFloat(), 2f, "bottom in $where")
                // At 90° upright rows are buffer columns: the region stays inside what the visible area shows.
                assertTrue(filled.left > visible.left, "below the clipped top strip in $where")
                assertTrue(filled.left + filled.width < visible.right, "above the sheet in $where")
            }
        }
    }

    /**
     * A sensor buffer that CameraX turns 90° to be upright, and the crop rect a `FILL_CENTER` viewport of the given
     * upright size gets in it.
     */
    private data class Picture(
        val bufferWidth: Int,
        val bufferHeight: Int,
    ) {
        fun viewportCrop(
            viewWidth: Int,
            viewHeight: Int,
        ): BufferRect {
            val uprightWidth = bufferHeight.toFloat()
            val uprightHeight = bufferWidth.toFloat()
            val scale = maxOf(viewWidth / uprightWidth, viewHeight / uprightHeight)
            val cropWidth = viewWidth / scale
            val cropHeight = viewHeight / scale
            val left = (uprightWidth - cropWidth) / 2f
            val top = (uprightHeight - cropHeight) / 2f
            // Upright rows run along the buffer's columns; upright columns run against its rows.
            return BufferRect(
                left = top.roundToInt(),
                top = (uprightWidth - left - cropWidth).roundToInt(),
                right = (top + cropHeight).roundToInt(),
                bottom = (uprightWidth - left).roundToInt(),
            )
        }
    }

    private data class BufferRect(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )

    private companion object {
        const val RESERVED = 273f
        const val ROW_HEIGHT = 147f
        const val MARGIN = 42f
        const val CORNER_DP = 28f
        const val RESERVED_DP = 104f
        const val ROW_HEIGHT_DP = 56f
        const val MARGIN_DP = 16f
        const val ROTATION = 90
    }
}
