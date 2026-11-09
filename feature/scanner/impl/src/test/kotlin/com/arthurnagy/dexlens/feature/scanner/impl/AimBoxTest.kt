package com.arthurnagy.dexlens.feature.scanner.impl

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

    @Test
    fun boxIsWiderThanTall() {
        val portrait = aimBoxFor(1080f, 2400f)
        assertTrue(portrait.right - portrait.left > portrait.bottom - portrait.top)
        val landscape = aimBoxFor(2400f, 1080f)
        assertTrue(landscape.left > portrait.left)
    }

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
}
