package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.compose.runtime.Immutable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The aiming box as fractions of the surface it is drawn on: the visible camera area, or the taller viewfinder. */
@Immutable
internal data class AimBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * The largest box, centred left to right in the [width] × [height] camera area, that stays clear of the status bar
 * ([topInset]) and of the button row at the top end, which is [buttonRowHeight] tall and [reservedWidth] wide.
 *
 * It is the larger of two boxes. One starts below the button row and spans the width less [margin] on each side, up to
 * half again as wide as it is tall, which suits a name and wins on most phones. The other is a near-square beside the
 * buttons, held [reservedWidth] from both sides, which wins in short areas such as a phone in landscape.
 */
internal fun aimBoxFor(
    width: Float,
    height: Float,
    topInset: Float,
    reservedWidth: Float,
    buttonRowHeight: Float,
    margin: Float,
): AimBox {
    val belowButtons = run {
        val top = topInset + buttonRowHeight
        val boxHeight = (height - top - margin).coerceAtLeast(1f)
        val boxWidth = min(width - 2f * margin, boxHeight * MAX_WIDTH_OVER_HEIGHT).coerceAtLeast(1f)
        Placed(top, boxWidth, boxHeight)
    }
    val besideButtons = run {
        val available = height - topInset
        val boxWidth = min(width - 2f * reservedWidth, available * HEIGHT_SHARE * NEAR_SQUARE).coerceAtLeast(1f)
        val boxHeight = boxWidth / NEAR_SQUARE
        Placed(topInset + (available - boxHeight) / 2f, boxWidth, boxHeight)
    }
    val box = maxOf(belowButtons, besideButtons, compareBy { it.width * it.height })
    val left = (width - box.width) / 2f
    return AimBox(
        left = left / width,
        top = box.top / height,
        right = (left + box.width) / width,
        bottom = (box.top + box.height) / height,
    )
}

/**
 * The same box as fractions of a viewfinder that reaches [above] pixels above and [below] pixels below the
 * [visibleHeight] it was laid out in. Left and right don't change, because the width doesn't.
 */
internal fun AimBox.inViewfinder(
    visibleHeight: Float,
    above: Float,
    below: Float,
): AimBox {
    // Exactly the same box when nothing is added, so the unfilled layout analyses what it always has.
    if (above == 0f && below == 0f) return this
    val total = visibleHeight + above + below
    return copy(top = (top * visibleHeight + above) / total, bottom = (bottom * visibleHeight + above) / total)
}

/**
 * Whether the camera picture is sure to reach [radiusPx] past the [visibleHeightPx]-tall area both above and below at
 * the scale it has at that size, so a viewfinder that much taller keeps the same scale and centre. The picture both
 * camera streams share is between 4:3 and 16:9, so upright in portrait it is at least 4/3 of [widthPx] tall and upright
 * in landscape at least 9/16 of it. An unknown orientation never fits.
 */
internal fun cornersFit(
    widthPx: Int,
    visibleHeightPx: Int,
    radiusPx: Int,
    uprightPortrait: Boolean?,
): Boolean {
    if (uprightPortrait == null || radiusPx <= 0) return false
    val pictureHeight = if (uprightPortrait) widthPx * PORTRAIT_HEIGHT_OVER_WIDTH else widthPx * LANDSCAPE_HEIGHT_OVER_WIDTH
    return pictureHeight >= visibleHeightPx + 2 * radiusPx
}

private data class Placed(
    val top: Float,
    val width: Float,
    val height: Float,
)

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

private const val PORTRAIT_HEIGHT_OVER_WIDTH = 4f / 3f
private const val LANDSCAPE_HEIGHT_OVER_WIDTH = 9f / 16f
private const val HEIGHT_SHARE = 0.8f
private const val NEAR_SQUARE = 1.05f
private const val MAX_WIDTH_OVER_HEIGHT = 1.5f
private const val ROTATION_90 = 90
private const val ROTATION_180 = 180
private const val ROTATION_270 = 270
private const val NEUTRAL_CHROMA: Byte = 0x80.toByte()
