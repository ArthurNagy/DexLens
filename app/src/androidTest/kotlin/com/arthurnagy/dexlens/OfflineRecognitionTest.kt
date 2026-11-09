package com.arthurnagy.dexlens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.ConnectivityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Runs the bundled recognizer inside the app's final manifest, with ML Kit's upload components removed, and with no
 * network. Skipped unless airplane mode is on: run with `adb shell cmd connectivity airplane-mode enable` first.
 */
@RunWith(AndroidJUnit4::class)
class OfflineRecognitionTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // Scenario: Recognition in airplane mode
    @Test
    fun recognizesTextWithNoNetwork() {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        assumeTrue("airplane mode is off", connectivity.activeNetwork == null)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 96f
            typeface = Typeface.DEFAULT_BOLD
        }
        val bitmap = Bitmap.createBitmap(800, 200, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawText("PIKACHU", 20f, 140f, paint)
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)), 20, TimeUnit.SECONDS).text

        assertTrue(text, text.contains("PIKACHU"))
    }
}
