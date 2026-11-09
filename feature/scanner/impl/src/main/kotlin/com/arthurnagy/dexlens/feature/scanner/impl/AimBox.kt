package com.arthurnagy.dexlens.feature.scanner.impl

import androidx.compose.runtime.Immutable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The aiming box as fractions of the visible camera area. */
@Immutable
internal data class AimBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** A wide strip, because a name is one line of text. On wide windows it is narrower so it still fits a TV-style name. */
internal fun aimBoxFor(
    width: Float,
    height: Float,
): AimBox {
    val isPortrait = height >= width
    val horizontalInset = if (isPortrait) 0.08f else 0.2f
    val verticalSize = if (isPortrait) 0.18f else 0.3f
    val top = 0.5f - verticalSize / 2f
    return AimBox(left = horizontalInset, top = top, right = 1f - horizontalInset, bottom = top + verticalSize)
}

/** Integer region of the image buffer. */
internal data class CropRegion(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

/**
 * Maps the on-screen [box] onto the image buffer. [viewLeft]..[viewBottom] is `ImageProxy.cropRect`, the part of the
 * buffer the viewport shows, and [rotationDegrees] is how far the buffer must turn clockwise to be upright.
 * Width and height come out even, because NV21 requires it.
 */
internal fun cropRegion(
    viewLeft: Int,
    viewTop: Int,
    viewRight: Int,
    viewBottom: Int,
    rotationDegrees: Int,
    box: AimBox,
): CropRegion {
    val bufferWidth = (viewRight - viewLeft).toFloat()
    val bufferHeight = (viewBottom - viewTop).toFloat()

    fun toBuffer(
        u: Float,
        v: Float,
    ): Pair<Float, Float> = when (rotationDegrees) {
        ROTATION_90 -> Pair(v * bufferWidth, (1f - u) * bufferHeight)
        ROTATION_180 -> Pair((1f - u) * bufferWidth, (1f - v) * bufferHeight)
        ROTATION_270 -> Pair((1f - v) * bufferWidth, u * bufferHeight)
        else -> Pair(u * bufferWidth, v * bufferHeight)
    }

    val first = toBuffer(box.left, box.top)
    val second = toBuffer(box.right, box.bottom)
    val left = viewLeft + min(first.first, second.first).roundToInt()
    val top = viewTop + min(first.second, second.second).roundToInt()
    val right = viewLeft + max(first.first, second.first).roundToInt()
    val bottom = viewTop + max(first.second, second.second).roundToInt()
    val alignedLeft = left and 1.inv()
    val alignedTop = top and 1.inv()
    return CropRegion(
        left = alignedLeft,
        top = alignedTop,
        width = ((right - alignedLeft) and 1.inv()).coerceAtLeast(2),
        height = ((bottom - alignedTop) and 1.inv()).coerceAtLeast(2),
    )
}

/** Packs [region] of a luma plane into a grey NV21 image: text recognition only needs brightness. */
internal fun lumaCropToNv21(
    plane: java.nio.ByteBuffer,
    rowStride: Int,
    region: CropRegion,
): ByteArray {
    val lumaSize = region.width * region.height
    val output = ByteArray(lumaSize + lumaSize / 2)
    for (row in 0 until region.height) {
        plane.position(region.top * rowStride + row * rowStride + region.left)
        plane.get(output, row * region.width, region.width)
    }
    output.fill(NEUTRAL_CHROMA, lumaSize, output.size)
    return output
}

private const val ROTATION_90 = 90
private const val ROTATION_180 = 180
private const val ROTATION_270 = 270
private const val NEUTRAL_CHROMA: Byte = 0x80.toByte()
