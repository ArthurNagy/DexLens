package com.arthurnagy.dexlens.feature.scanner.impl

import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.ExifData
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A fake frame, as CameraX's guidance prefers to mocking ImageProxy. */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private class FakeImageProxy(
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val rotation: Int,
    private val rect: Rect,
) : ImageProxy {
    var closed = false
        private set

    private val plane = object : ImageProxy.PlaneProxy {
        override fun getRowStride(): Int = frameWidth

        override fun getPixelStride(): Int = 1

        override fun getBuffer(): ByteBuffer = ByteBuffer.wrap(ByteArray(frameWidth * frameHeight))
    }

    private val info = object : ImageInfo {
        override fun getTagBundle(): TagBundle = TagBundle.emptyBundle()

        override fun getTimestamp(): Long = 0

        override fun getRotationDegrees(): Int = rotation

        override fun getSensorToBufferTransformMatrix(): Matrix = Matrix()

        override fun populateExifData(exifBuilder: ExifData.Builder) = Unit
    }

    override fun close() {
        closed = true
    }

    override fun getCropRect(): Rect = rect

    override fun setCropRect(rect: Rect?) = Unit

    override fun getFormat(): Int = android.graphics.ImageFormat.YUV_420_888

    override fun getHeight(): Int = frameHeight

    override fun getWidth(): Int = frameWidth

    override fun getPlanes(): Array<ImageProxy.PlaneProxy> = arrayOf(plane)

    override fun getImageInfo(): ImageInfo = info

    override fun getImage(): Image? = null
}

@RunWith(AndroidJUnit4::class)
internal class TextFrameAnalyzerTest {
    private val box = AimBox(0.1f, 0.4f, 0.9f, 0.6f)
    private val line = AnalyzedLine("PIKACHU", 0f, 0f, 1f, 1f)

    private fun image(rotation: Int = 90) = FakeImageProxy(2000, 1000, rotation, Rect(0, 0, 2000, 1000))

    // Scenario: Crop follows the aiming box
    @Test
    fun recognizesTheCropAndReportsIt() {
        var requested: List<Int>? = null
        var frame: AnalyzedFrame? = null
        val analyzer = TextFrameAnalyzer(
            recognizer = { nv21, width, height, rotation ->
                requested = listOf(nv21.size, width, height, rotation)
                listOf(line)
            },
            aimBox = { box },
            isPaused = { false },
            onFrame = { frame = it },
            clock = { 100L },
        )
        val image = image()

        analyzer.analyze(image)

        assertEquals(listOf(400 * 800 * 3 / 2, 400, 800, 90), requested)
        assertEquals(listOf(line), frame?.lines)
        assertEquals(800, frame?.cropWidth)
        assertEquals(400, frame?.cropHeight)
        assertTrue(image.closed)
    }

    // Scenario: Analysis pauses during result
    @Test
    fun pausedFramesAreClosedWithoutRecognition() {
        var recognized = false
        val analyzer = TextFrameAnalyzer(
            recognizer = { _, _, _, _ ->
                recognized = true
                emptyList()
            },
            aimBox = { box },
            isPaused = { true },
            onFrame = {},
        )
        val image = image()

        analyzer.analyze(image)

        assertEquals(false, recognized)
        assertTrue(image.closed)
    }

    @Test
    fun aFailedRecognitionIsSkipped() {
        var frame: AnalyzedFrame? = null
        val analyzer = TextFrameAnalyzer(recognizer = {
            _,
            _,
            _,
            _,
            ->
            null
        }, aimBox = { box }, isPaused = { false }, onFrame = { frame = it })
        val image = image(rotation = 0)

        analyzer.analyze(image)

        assertNull(frame)
        assertTrue(image.closed)
    }
}
