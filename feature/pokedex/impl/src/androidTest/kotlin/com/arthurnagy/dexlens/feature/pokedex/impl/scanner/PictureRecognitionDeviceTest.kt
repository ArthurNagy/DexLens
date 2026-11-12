package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arthurnagy.dexlens.core.domain.scan.RecognitionThresholds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Runs the real bundled encoder and reference file on LiteRT's CPU runtime. It proves the model loads from the APK with
 * no download, that the reference file parses, and that pictures with no Pokémon in them don't reach the candidate
 * score. The repository holds no official art, so recognising a real sprite is checked by hand on a device.
 */
@RunWith(AndroidJUnit4::class)
internal class PictureRecognitionDeviceTest {
    private val engine = LiteRtPictureEngine(InstrumentationRegistry.getInstrumentation().targetContext.assets)
    private val size = ColourSquare.SIZE * ColourSquare.SIZE * ColourSquare.CHANNELS

    @After
    fun close() = engine.close()

    // Scenario: Picture recognition in airplane mode
    @Test
    fun theBundledModelScoresAFrameOffline() {
        engine.load()
        val top = engine.score(FloatArray(size) { MID_GREY }).top(3)
        Log.i(TAG, "grey top 3: $top")
        assertEquals(3, top.size)
        assertTrue(top.zipWithNext().all { (first, second) -> first.score >= second.score })
    }

    // Scenario: Everyday scene scores nothing
    @Test
    fun blankAndNoisySquaresScoreBelowTheCandidateThreshold() {
        engine.load()
        val random = Random(SEED)
        val inputs = mapOf(
            "grey" to FloatArray(size) { MID_GREY },
            "noise" to FloatArray(size) { random.nextFloat() * MAX_CHANNEL },
        )
        inputs.forEach { (name, input) ->
            val best = engine.score(input).top(1).single()
            Log.i(TAG, "$name best: $best")
            assertTrue("$name scored $best", best.score < RecognitionThresholds.Default.candidate)
        }
    }

    private companion object {
        const val TAG = "PictureRecognitionTest"
        const val MID_GREY = 128f
        const val MAX_CHANNEL = 255f
        const val SEED = 7
    }
}
