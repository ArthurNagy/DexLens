package com.arthurnagy.dexlens.feature.scanner.impl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.domain.scan.FrameObservation
import com.arthurnagy.dexlens.core.domain.scan.ScanNameIndex
import com.arthurnagy.dexlens.core.domain.scan.TextNameSignal
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Runs the real bundled recognizer and the real matcher on images of game-style text, so a device run proves the
 * model loads with no download and that text recognition and matching work together.
 */
@RunWith(AndroidJUnit4::class)
internal class TextRecognitionDeviceTest {
    private val names = listOf(
        SpeciesNameHit(SpeciesId(25), "Pikachu"),
        SpeciesNameHit(SpeciesId(29), "Nidoran♀"),
        SpeciesNameHit(SpeciesId(32), "Nidoran♂"),
        SpeciesNameHit(SpeciesId(83), "Farfetch’d"),
        SpeciesNameHit(SpeciesId(122), "Mr. Mime"),
        SpeciesNameHit(SpeciesId(16), "Pidgey"),
        SpeciesNameHit(SpeciesId(143), "Snorlax"),
        SpeciesNameHit(SpeciesId(1), "Bulbasaur"),
    )
    private val signal = TextNameSignal(ScanNameIndex(names))
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private fun smooth(text: String): Bitmap = render(text, textSize = 96f, scale = 1)

    /** A blocky low-resolution font scaled up without smoothing, like a handheld's screen. */
    private fun pixelated(text: String): Bitmap = render(text, textSize = 16f, scale = 6)

    private fun render(
        text: String,
        textSize: Float,
        scale: Int,
    ): Bitmap {
        val paint = Paint().apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = scale == 1
        }
        val width = paint.measureText(text).toInt() + 40
        val height = (textSize * 2).toInt()
        val small = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(small).apply {
            drawColor(Color.WHITE)
            drawText(text, 20f, textSize * 1.4f, paint)
        }
        return if (scale == 1) small else Bitmap.createScaledBitmap(small, width * scale, height * scale, false)
    }

    private fun recognise(bitmap: Bitmap): List<String> {
        val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)), 20, TimeUnit.SECONDS)
        return result.textBlocks.flatMap { it.lines }.map { it.text }
    }

    private fun ids(lines: List<String>) = signal.analyze(FrameObservation(lines)).candidates.map { it.speciesId.value }.toSet()

    @Test
    fun readsNamesFromSmoothText() {
        val cases = mapOf("PIKACHU" to setOf(25), "MR.MIME" to setOf(122), "FARFETCH'D" to setOf(83), "SNORLAX" to setOf(143))
        cases.forEach { (text, expected) ->
            val lines = recognise(smooth(text))
            Log.i(TAG, "smooth '$text' read as $lines -> ${ids(lines)}")
            assertEquals(text, expected, ids(lines))
        }
    }

    @Test
    fun readsNameInsideASentence() {
        val lines = recognise(smooth("Wild PIDGEY appeared!"))
        Log.i(TAG, "sentence read as $lines -> ${ids(lines)}")
        assertEquals(setOf(16), ids(lines))
    }

    @Test
    fun nidoranIsFoundWithOrWithoutSymbol() {
        val lines = recognise(smooth("NIDORAN♀"))
        Log.i(TAG, "nidoran read as $lines -> ${ids(lines)}")
        assertTrue(lines.toString(), 29 in ids(lines))
    }

    /** Pixel fonts are harder. These are logged rather than asserted, to see how the recogniser copes on a device. */
    @Test
    fun logsPixelFontReadings() {
        listOf("PIKACHU", "BULBASAUR", "MR.MIME", "NIDORAN♀", "SNORLAX", "PIDGEY").forEach { text ->
            val lines = recognise(pixelated(text))
            Log.i(TAG, "pixel '$text' read as $lines -> ${ids(lines)}")
        }
    }

    private companion object {
        const val TAG = "DexScanDevice"
    }
}
