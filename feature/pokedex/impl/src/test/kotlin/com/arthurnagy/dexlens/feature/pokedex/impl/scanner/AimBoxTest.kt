package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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

    private companion object {
        const val RESERVED = 273f
        const val ROW_HEIGHT = 147f
        const val MARGIN = 42f
    }
}
