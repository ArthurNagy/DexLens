package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/** One plane of a YUV_420_888 frame. */
internal class PlaneView(
    val buffer: ByteBuffer,
    val rowStride: Int,
    val pixelStride: Int,
)

/**
 * Turns the centred square of a crop into the picture model's input: [SIZE] × [SIZE] upright RGB pixels as floats from 0
 * to 255, row by row. Each output pixel averages the camera pixels under it, which matches the antialiased resize the
 * model was trained with better than sampling one pixel would. Not thread-safe: it reuses its buffers between frames.
 */
internal class ColourSquare {
    /** The latest square, valid until the next [fill]. */
    val pixels = FloatArray(SIZE * SIZE * CHANNELS)

    private val luma = FloatArray(SIZE * SIZE)
    private val blue = FloatArray(SIZE * SIZE)
    private val red = FloatArray(SIZE * SIZE)
    private var row = ByteArray(0)

    /**
     * Fills [pixels] from the centred square of [region]. [rotationDegrees] is how far the frame must turn clockwise to
     * be upright, as in `ImageInfo.rotationDegrees`.
     */
    fun fill(
        y: PlaneView,
        u: PlaneView,
        v: PlaneView,
        region: CropRegion,
        rotationDegrees: Int,
    ): FloatArray {
        val side = min(region.width, region.height) and 1.inv()
        val left = (region.left + (region.width - side) / 2) and 1.inv()
        val top = (region.top + (region.height - side) / 2) and 1.inv()
        average(y, left, top, side, luma)
        average(u, left / 2, top / 2, side / 2, blue)
        average(v, left / 2, top / 2, side / 2, red)
        for (outY in 0 until SIZE) {
            for (outX in 0 until SIZE) {
                val cell = cellUnder(outX, outY, rotationDegrees)
                val lum = luma[cell]
                val cb = blue[cell] - CHROMA_ZERO
                val cr = red[cell] - CHROMA_ZERO
                val out = (outY * SIZE + outX) * CHANNELS
                pixels[out] = channel(lum + RED_FROM_CR * cr)
                pixels[out + 1] = channel(lum - GREEN_FROM_CB * cb - GREEN_FROM_CR * cr)
                pixels[out + 2] = channel(lum + BLUE_FROM_CB * cb)
            }
        }
        return pixels
    }

    /** Averages a [count] × [count] block of [plane] starting at ([startX], [startY]) into a [SIZE] × [SIZE] grid. */
    private fun average(
        plane: PlaneView,
        startX: Int,
        startY: Int,
        count: Int,
        out: FloatArray,
    ) {
        val length = (count - 1) * plane.pixelStride + 1
        if (row.size < length) row = ByteArray(length)
        val sums = LongArray(SIZE)
        for (cellY in 0 until SIZE) {
            sums.fill(0)
            val firstRow = rangeStart(cellY, count)
            val endRow = rangeEnd(cellY, count, firstRow)
            for (r in firstRow until endRow) {
                plane.buffer.position((startY + r) * plane.rowStride + startX * plane.pixelStride)
                plane.buffer.get(row, 0, length)
                for (cellX in 0 until SIZE) {
                    val first = rangeStart(cellX, count)
                    val end = rangeEnd(cellX, count, first)
                    var sum = 0L
                    for (c in first until end) sum += row[c * plane.pixelStride].toInt() and BYTE_MASK
                    sums[cellX] += sum
                }
            }
            for (cellX in 0 until SIZE) {
                val first = rangeStart(cellX, count)
                val samples = (endRow - firstRow) * (rangeEnd(cellX, count, first) - first)
                out[cellY * SIZE + cellX] = sums[cellX].toFloat() / samples
            }
        }
    }

    companion object {
        /** The picture model's input width and height. */
        const val SIZE = 224
        const val CHANNELS = 3

        private const val CHROMA_ZERO = 128f
        private const val BYTE_MASK = 0xFF

        // BT.601 full range, which camera YUV_420_888 frames use.
        private const val RED_FROM_CR = 1.402f
        private const val GREEN_FROM_CB = 0.344136f
        private const val GREEN_FROM_CR = 0.714136f
        private const val BLUE_FROM_CB = 1.772f
        private const val MAX_CHANNEL = 255f

        private const val ROTATION_90 = 90
        private const val ROTATION_180 = 180
        private const val ROTATION_270 = 270
        private const val LAST = SIZE - 1

        private fun rangeStart(
            cell: Int,
            count: Int,
        ): Int = cell * count / SIZE

        /** At least one sample per cell, so a crop smaller than [SIZE] repeats pixels instead of leaving gaps. */
        private fun rangeEnd(
            cell: Int,
            count: Int,
            start: Int,
        ): Int = min(max((cell + 1) * count / SIZE, start + 1), count)

        private fun channel(value: Float): Float = value.coerceIn(0f, MAX_CHANNEL)

        /** The grid cell, in frame orientation, under upright output pixel ([x], [y]). */
        private fun cellUnder(
            x: Int,
            y: Int,
            rotationDegrees: Int,
        ): Int = when (rotationDegrees) {
            ROTATION_90 -> (LAST - x) * SIZE + y
            ROTATION_180 -> (LAST - y) * SIZE + (LAST - x)
            ROTATION_270 -> x * SIZE + (LAST - y)
            else -> y * SIZE + x
        }
    }
}
