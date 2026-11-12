package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import com.arthurnagy.dexlens.core.domain.scan.PictureScores

/** Answers every frame with [scores], or turns frames away when it doesn't [accepts] them. */
internal class FakePictureRecognizer(
    private val scores: PictureScores?,
    private val accepts: Boolean = true,
) : PictureRecognizer {
    val submitted = mutableListOf<FloatArray>()
    var closed = false
        private set

    override fun submit(input: FloatArray): PendingScore? {
        if (!accepts || closed) return null
        submitted += input.copyOf()
        return PendingScore { scores }
    }

    override fun close() {
        closed = true
    }
}
