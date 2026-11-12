package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import java.nio.ByteBuffer
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ColourSquareTest {
    private data class Rgb(
        val r: Int,
        val g: Int,
        val b: Int,
    )

    /**
     * A YUV_420_888 frame built from a colour per pixel. With [interleaved], U and V share one buffer with a pixel stride
     * of 2, as most cameras deliver them; otherwise each has its own plane with a stride of 1.
     */
    private class Frame(
        val width: Int,
        val height: Int,
        interleaved: Boolean,
        colourAt: (x: Int, y: Int) -> Rgb,
    ) {
        val y: PlaneView
        val u: PlaneView
        val v: PlaneView

        init {
            val luma = ByteArray(width * height)
            val chromaWidth = width / 2
            val chromaHeight = height / 2
            val cb = ByteArray(chromaWidth * chromaHeight)
            val cr = ByteArray(chromaWidth * chromaHeight)
            for (py in 0 until height) {
                for (px in 0 until width) {
                    val (r, g, b) = colourAt(px, py)
                    luma[py * width + px] = clamp(0.299 * r + 0.587 * g + 0.114 * b)
                    if (px % 2 == 0 && py % 2 == 0) {
                        val index = (py / 2) * chromaWidth + px / 2
                        cb[index] = clamp(128 - 0.168736 * r - 0.331264 * g + 0.5 * b)
                        cr[index] = clamp(128 + 0.5 * r - 0.418688 * g - 0.081312 * b)
                    }
                }
            }
            y = PlaneView(ByteBuffer.wrap(luma), width, 1)
            if (interleaved) {
                val shared = ByteArray(width * chromaHeight)
                for (i in cb.indices) {
                    val row = i / chromaWidth
                    val column = i % chromaWidth
                    shared[row * width + column * 2] = cb[i]
                    shared[row * width + column * 2 + 1] = cr[i]
                }
                u = PlaneView(ByteBuffer.wrap(shared), width, 2)
                v = PlaneView(ByteBuffer.wrap(shared, 1, shared.size - 1).slice(), width, 2)
            } else {
                u = PlaneView(ByteBuffer.wrap(cb), chromaWidth, 1)
                v = PlaneView(ByteBuffer.wrap(cr), chromaWidth, 1)
            }
        }

        private fun clamp(value: Double): Byte = value.roundToInt().coerceIn(0, 255).toByte()
    }

    private fun FloatArray.at(
        x: Int,
        y: Int,
    ): Rgb {
        val i = (y * ColourSquare.SIZE + x) * ColourSquare.CHANNELS
        return Rgb(this[i].roundToInt(), this[i + 1].roundToInt(), this[i + 2].roundToInt())
    }

    private fun assertNear(
        expected: Rgb,
        actual: Rgb,
    ) {
        val tolerance = 4
        val close = kotlin.math.abs(expected.r - actual.r) <= tolerance &&
            kotlin.math.abs(expected.g - actual.g) <= tolerance &&
            kotlin.math.abs(expected.b - actual.b) <= tolerance
        assertEquals(true, close, "expected $expected, got $actual")
    }

    private fun square(
        frame: Frame,
        region: CropRegion = CropRegion(0, 0, frame.width, frame.height),
        rotation: Int = 0,
    ) = ColourSquare().fill(frame.y, frame.u, frame.v, region, rotation)

    private val red = Rgb(220, 30, 30)
    private val green = Rgb(30, 200, 40)
    private val blue = Rgb(30, 40, 210)
    private val white = Rgb(250, 250, 250)

    @Test
    fun solidColoursConvertBack() {
        listOf(red, green, blue, white).forEach { colour ->
            val pixels = square(Frame(448, 448, interleaved = false) { _, _ -> colour })
            assertNear(colour, pixels.at(0, 0))
            assertNear(colour, pixels.at(223, 223))
        }
    }

    @Test
    fun interleavedChromaWorks() {
        val pixels = square(Frame(448, 448, interleaved = true) { x, _ -> if (x < 224) red else blue })
        assertNear(red, pixels.at(10, 100))
        assertNear(blue, pixels.at(210, 100))
    }

    // Scenario: Crop follows the aiming box
    @Test
    fun theSquareComesOutUprightInEveryOrientation() {
        // The frame holds red at the top left, green at the top right, blue at the bottom left, white at the bottom right.
        val frame = Frame(448, 448, interleaved = true) { x, y ->
            when {
                y < 224 && x < 224 -> red
                y < 224 -> green
                x < 224 -> blue
                else -> white
            }
        }
        // Turning the frame clockwise by the rotation brings each corner to its upright place.
        val uprightCorners = mapOf(
            0 to listOf(red, green, blue, white),
            90 to listOf(blue, red, white, green),
            180 to listOf(white, blue, green, red),
            270 to listOf(green, white, red, blue),
        )
        uprightCorners.forEach { (rotation, corners) ->
            val pixels = square(frame, rotation = rotation)
            assertNear(corners[0], pixels.at(20, 20))
            assertNear(corners[1], pixels.at(200, 20))
            assertNear(corners[2], pixels.at(20, 200))
            assertNear(corners[3], pixels.at(200, 200))
        }
    }

    @Test
    fun aWideCropUsesItsCentredSquare() {
        // 896 × 448 crop: only the middle 448 columns are green.
        val frame = Frame(896, 448, interleaved = false) { x, _ -> if (x in 224 until 672) green else red }
        val pixels = square(frame)
        assertNear(green, pixels.at(0, 112))
        assertNear(green, pixels.at(223, 112))
    }

    @Test
    fun aCropSmallerThanTheModelInputRepeatsPixels() {
        val pixels = square(Frame(100, 100, interleaved = false) { _, _ -> blue })
        assertNear(blue, pixels.at(223, 223))
    }
}
