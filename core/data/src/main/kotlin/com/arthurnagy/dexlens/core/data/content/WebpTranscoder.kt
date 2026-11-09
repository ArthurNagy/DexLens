package com.arthurnagy.dexlens.core.data.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/** Re-encodes a downloaded picture. Faked in tests, where there is no Android image codec. */
internal fun interface ImageTranscoder {
    /** Returns WebP bytes, scaled so the long side is at most [maxSize] when it is set. */
    fun toWebp(
        png: ByteArray,
        maxSize: Int?,
    ): ByteArray
}

internal class WebpTranscoder
    @Inject
    constructor() : ImageTranscoder {
        override fun toWebp(
            png: ByteArray,
            maxSize: Int?,
        ): ByteArray {
            val source = BitmapFactory.decodeByteArray(png, 0, png.size) ?: error("Not a decodable image")
            val scaled = maxSize?.let { source.scaledToFit(it) } ?: source
            val output = ByteArrayOutputStream(png.size / 2)
            scaled.compress(webpFormat(), WEBP_QUALITY, output)
            if (scaled !== source) scaled.recycle()
            source.recycle()
            return output.toByteArray()
        }

        private fun Bitmap.scaledToFit(maxSize: Int): Bitmap {
            val longest = max(width, height)
            if (longest <= maxSize) return this
            val ratio = maxSize.toFloat() / longest
            return scale((width * ratio).roundToInt(), (height * ratio).roundToInt())
        }

        @Suppress("DEPRECATION") // WEBP is the only lossy option before API 30.
        private fun webpFormat(): Bitmap.CompressFormat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

        private companion object {
            const val WEBP_QUALITY = 80
        }
    }
