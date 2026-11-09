package com.arthurnagy.dexlens.core.data.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class WebpTranscoderTest {
    private fun png(
        width: Int,
        height: Int,
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }

    private fun assertWebp(bytes: ByteArray) {
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals("WEBP", String(bytes, 8, 4))
    }

    // Scenario: Artwork is scaled and converted
    @Test
    fun scalesLargeArtworkDown() {
        val webp = WebpTranscoder().toWebp(png(475, 475), maxSize = 384)
        assertWebp(webp)
        val decoded = BitmapFactory.decodeByteArray(webp, 0, webp.size)
        assertEquals(384, decoded.width)
        assertEquals(384, decoded.height)
    }

    @Test
    fun keepsSmallerImagesAtTheirSize() {
        val webp = WebpTranscoder().toWebp(png(96, 96), maxSize = 384)
        assertWebp(webp)
        assertEquals(96, BitmapFactory.decodeByteArray(webp, 0, webp.size).width)
    }
}
