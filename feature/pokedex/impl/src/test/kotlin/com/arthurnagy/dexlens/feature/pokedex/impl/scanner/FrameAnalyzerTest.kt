package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.ExifData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.domain.scan.PictureScores
import com.arthurnagy.dexlens.core.domain.scan.SpeciesScore
import com.arthurnagy.dexlens.core.model.SpeciesId
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
    private val colour: Boolean = true,
) : ImageProxy {
    var closed = false
        private set

    private val plane = object : ImageProxy.PlaneProxy {
        override fun getRowStride(): Int = frameWidth

        override fun getPixelStride(): Int = 1

        override fun getBuffer(): ByteBuffer = ByteBuffer.wrap(ByteArray(frameWidth * frameHeight))
    }

    /** Interleaved chroma, as most cameras deliver it: U and V share one buffer with a pixel stride of 2. */
    private val chroma = ByteArray(frameWidth * frameHeight / 2) { 128.toByte() }

    private fun chromaPlane(offset: Int) = object : ImageProxy.PlaneProxy {
        override fun getRowStride(): Int = frameWidth

        override fun getPixelStride(): Int = 2

        override fun getBuffer(): ByteBuffer = ByteBuffer.wrap(chroma, offset, chroma.size - offset).slice()
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

    override fun getPlanes(): Array<ImageProxy.PlaneProxy> = if (colour) arrayOf(plane, chromaPlane(0), chromaPlane(1)) else arrayOf(plane)

    override fun getImageInfo(): ImageInfo = info

    override fun getImage(): Image? = null
}

@RunWith(AndroidJUnit4::class)
internal class FrameAnalyzerTest {
    private val box = AimBox(0.1f, 0.4f, 0.9f, 0.6f)
    private val line = "PIKACHU"

    private fun image(rotation: Int = 90) = FakeImageProxy(2000, 1000, rotation, Rect(0, 0, 2000, 1000))

    // Scenario: Crop follows the aiming box
    @Test
    fun recognizesTheCropAndReportsIt() {
        var requested: List<Int>? = null
        var frame: AnalyzedFrame? = null
        val analyzer = FrameAnalyzer(
            recognizer = { nv21, width, height, rotation ->
                requested = listOf(nv21.size, width, height, rotation)
                listOf(line)
            },
            pictures = { null },
            aimBox = { box },
            onFrame = { frame = it },
        )
        val image = image()

        analyzer.analyze(image)

        assertEquals(listOf(400 * 800 * 3 / 2, 400, 800, 90), requested)
        assertEquals(listOf(line), frame?.lines)
        assertTrue(image.closed)
    }

    @Test
    fun aFailedRecognitionIsSkipped() {
        var frame: AnalyzedFrame? = null
        val analyzer = FrameAnalyzer(recognizer = {
            _,
            _,
            _,
            _,
            ->
            null
        }, pictures = { null }, aimBox = { box }, onFrame = { frame = it })
        val image = image(rotation = 0)

        analyzer.analyze(image)

        assertNull(frame)
        assertTrue(image.closed)
    }

    @Test
    fun picturesAreScoredBesideTheText() {
        val scores = PictureScores.of(SpeciesScore(SpeciesId(25), 0.97f))
        val pictures = FakePictureRecognizer(scores)
        var frame: AnalyzedFrame? = null
        val analyzer = FrameAnalyzer(recognizer = {
            _,
            _,
            _,
            _,
            ->
            listOf(line)
        }, pictures = { pictures }, aimBox = { box }, onFrame = {
            frame =
                it
        })

        analyzer.analyze(image())

        assertEquals(1, pictures.submitted.size)
        assertEquals(ColourSquare.SIZE * ColourSquare.SIZE * ColourSquare.CHANNELS, pictures.submitted.single().size)
        assertEquals(AnalyzedFrame(listOf(line), scores), frame)
    }

    @Test
    fun aFrameWithOnlyPictureScoresStillCounts() {
        val scores = PictureScores.of(SpeciesScore(SpeciesId(25), 0.97f))
        var frame: AnalyzedFrame? = null
        val analyzer = FrameAnalyzer(recognizer = {
            _,
            _,
            _,
            _,
            ->
            null
        }, pictures = { FakePictureRecognizer(scores) }, aimBox = { box }, onFrame = {
            frame =
                it
        })

        analyzer.analyze(image())

        assertEquals(AnalyzedFrame(emptyList(), scores), frame)
    }

    @Test
    fun aBusyRecognizerLeavesTheFrameToText() {
        var frame: AnalyzedFrame? = null
        val analyzer = FrameAnalyzer(recognizer = {
            _,
            _,
            _,
            _,
            ->
            listOf(line)
        }, pictures = { FakePictureRecognizer(null, accepts = false) }, aimBox = { box }, onFrame = {
            frame =
                it
        })

        analyzer.analyze(image())

        assertEquals(AnalyzedFrame(listOf(line), null), frame)
    }

    @Test
    fun aGreyOnlyFrameSkipsThePicture() {
        val pictures = FakePictureRecognizer(PictureScores.of())
        val analyzer = FrameAnalyzer(recognizer = { _, _, _, _ -> listOf(line) }, pictures = { pictures }, aimBox = { box }, onFrame = {})

        analyzer.analyze(FakeImageProxy(2000, 1000, 90, Rect(0, 0, 2000, 1000), colour = false))

        assertTrue(pictures.submitted.isEmpty())
    }
}
