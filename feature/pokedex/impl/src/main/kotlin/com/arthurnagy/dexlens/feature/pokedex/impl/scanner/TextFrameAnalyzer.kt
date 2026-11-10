package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/** Finds lines of text in a grey NV21 image. Returns null when recognition fails, so the frame is simply skipped. */
internal fun interface LineRecognizer {
    fun recognize(
        nv21: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
    ): List<AnalyzedLine>?
}

/**
 * Crops each frame to the aiming box and runs text recognition. It blocks the analysis thread, so with
 * `STRATEGY_KEEP_ONLY_LATEST` slow frames are dropped rather than queued.
 */
internal class TextFrameAnalyzer(
    private val recognizer: LineRecognizer,
    private val aimBox: () -> AimBox,
    private val onFrame: (AnalyzedFrame) -> Unit,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            process(image)
        } finally {
            image.close()
        }
    }

    private fun process(image: ImageProxy) {
        val started = clock()
        val rotation = image.imageInfo.rotationDegrees
        val view = image.cropRect
        val region = cropRegion(view.left, view.top, view.right, view.bottom, rotation, aimBox())
        val plane = image.planes[0]
        val nv21 = lumaCropToNv21(plane.buffer, plane.rowStride, region)
        val lines = recognizer.recognize(nv21, region.width, region.height, rotation) ?: return
        val upright = rotation == 0 || rotation == HALF_TURN
        onFrame(
            AnalyzedFrame(
                lines = lines,
                cropWidth = if (upright) region.width else region.height,
                cropHeight = if (upright) region.height else region.width,
                bufferWidth = image.width,
                bufferHeight = image.height,
                processingMillis = clock() - started,
            ),
        )
    }

    private companion object {
        const val HALF_TURN = 180
    }
}
