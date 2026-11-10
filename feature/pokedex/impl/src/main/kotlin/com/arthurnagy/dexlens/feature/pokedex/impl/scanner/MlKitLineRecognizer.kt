package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** ML Kit's bundled Latin recognizer: the model ships in the APK, so it works offline from the first launch. */
internal class MlKitLineRecognizer(
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
) : LineRecognizer,
    AutoCloseable {
    override fun recognize(
        nv21: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
    ): List<AnalyzedLine>? {
        val input = InputImage.fromByteArray(nv21, width, height, rotationDegrees, InputImage.IMAGE_FORMAT_NV21)
        return try {
            val text = Tasks.await(recognizer.process(input), RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                AnalyzedLine(line.text, box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat())
            }
        } catch (e: CancellationException) {
            // The sheet expanded and closed the recognizer while this frame was running.
            Timber.d(e, "text recognition cancelled")
            null
        } catch (e: ExecutionException) {
            Timber.w(e, "text recognition failed")
            null
        } catch (e: TimeoutException) {
            Timber.w(e, "text recognition timed out")
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Timber.w(e, "text recognition interrupted")
            null
        }
    }

    override fun close() = recognizer.close()

    private companion object {
        const val RECOGNITION_TIMEOUT_MS = 4_000L
    }
}
