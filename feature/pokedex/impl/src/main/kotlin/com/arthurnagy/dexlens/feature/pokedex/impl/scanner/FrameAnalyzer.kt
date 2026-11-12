package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/** Finds lines of text in a grey NV21 image. Returns null when recognition fails, so the frame is simply skipped. */
internal fun interface LineRecognizer {
    fun recognize(
        nv21: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
    ): List<String>?
}

/**
 * Crops each frame to the aiming box and reads it twice at once: the picture recogniser scores a colour square of it on
 * its own thread while text recognition reads the grey crop here. It blocks the analysis thread, so with
 * `STRATEGY_KEEP_ONLY_LATEST` slow frames are dropped rather than queued.
 */
internal class FrameAnalyzer(
    private val recognizer: LineRecognizer,
    private val pictures: () -> PictureRecognizer?,
    private val aimBox: () -> AimBox,
    private val onFrame: (AnalyzedFrame) -> Unit,
) : ImageAnalysis.Analyzer {
    private val square = ColourSquare()

    override fun analyze(image: ImageProxy) {
        try {
            process(image)
        } finally {
            image.close()
        }
    }

    private fun process(image: ImageProxy) {
        val rotation = image.imageInfo.rotationDegrees
        val view = image.cropRect
        val region = cropRegion(view.left, view.top, view.right, view.bottom, rotation, aimBox())
        val planes = image.planes
        val pending = pictures()?.takeIf { planes.size >= YUV_PLANES }?.submit(
            square.fill(planes[0].view(), planes[1].view(), planes[2].view(), region, rotation),
        )
        val nv21 = lumaCropToNv21(planes[0].buffer, planes[0].rowStride, region)
        val lines = recognizer.recognize(nv21, region.width, region.height, rotation)
        val scores = pending?.await()
        if (lines == null && scores == null) return
        onFrame(AnalyzedFrame(lines.orEmpty(), scores))
    }

    private fun ImageProxy.PlaneProxy.view() = PlaneView(buffer, rowStride, pixelStride)

    private companion object {
        const val YUV_PLANES = 3
    }
}
