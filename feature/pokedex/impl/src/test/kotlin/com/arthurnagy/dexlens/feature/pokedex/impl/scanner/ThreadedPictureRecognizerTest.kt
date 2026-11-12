package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import com.arthurnagy.dexlens.core.domain.scan.PictureScores
import com.arthurnagy.dexlens.core.domain.scan.SpeciesScore
import com.arthurnagy.dexlens.core.model.SpeciesId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ThreadedPictureRecognizerTest {
    private val pikachu = PictureScores.of(SpeciesScore(SpeciesId(25), 0.97f))
    private val input = FloatArray(ColourSquare.SIZE * ColourSquare.SIZE * ColourSquare.CHANNELS)
    private val executor = Executors.newSingleThreadExecutor()

    /** An engine that can be held at load or at scoring, and made to fail. */
    private class FakePictureEngine(
        private val scores: PictureScores,
        private val failLoad: Boolean = false,
        private val failScore: Boolean = false,
    ) : PictureEngine {
        val loadGate = CountDownLatch(0)
        var scoreGate = CountDownLatch(0)
        val scoring = CountDownLatch(1)
        var scored = 0
        val closed = CountDownLatch(1)
        var lastInput: FloatArray? = null

        override fun load() {
            loadGate.await()
            check(!failLoad) { "no model" }
        }

        override fun score(input: FloatArray): PictureScores {
            scored++
            lastInput = input.copyOf()
            scoring.countDown()
            scoreGate.await()
            check(!failScore) { "inference failed" }
            return scores
        }

        override fun close() = closed.countDown()
    }

    private fun recognizer(
        engine: PictureEngine,
        timeoutMs: Long = 2_000,
    ) = ThreadedPictureRecognizer(engine, executor, timeoutMs)

    /** Waits until everything queued on the recogniser's thread so far has run. */
    private fun drain() = executor.submit {}.get(2, TimeUnit.SECONDS)

    @AfterTest
    fun stop() {
        executor.shutdownNow()
    }

    @Test
    fun scoresAFrame() {
        val engine = FakePictureEngine(pikachu)
        val recognizer = recognizer(engine)
        drain()

        input[0] = 42f
        assertEquals(pikachu, recognizer.submit(input)?.await())
        assertEquals(42f, engine.lastInput?.get(0))
    }

    @Test
    fun turnsFramesAwayWhileLoading() {
        val gate = CountDownLatch(1)
        val engine = object : PictureEngine {
            override fun load() = gate.await()

            override fun score(input: FloatArray) = pikachu

            override fun close() = Unit
        }
        val recognizer = recognizer(engine)

        assertNull(recognizer.submit(input))
        gate.countDown()
        drain()
        assertNotNull(recognizer.submit(input))
    }

    @Test
    fun takesOneFrameAtATime() {
        val engine = FakePictureEngine(pikachu).apply { scoreGate = CountDownLatch(1) }
        val recognizer = recognizer(engine)
        drain()

        val first = recognizer.submit(input)
        assertNotNull(first)
        engine.scoring.await(2, TimeUnit.SECONDS)
        assertNull(recognizer.submit(input))
        engine.scoreGate.countDown()
        assertEquals(pikachu, first.await())
        drain()
        assertEquals(1, engine.scored)
    }

    // Scenario: Slow picture frame keeps names working
    @Test
    fun aSlowFrameTimesOutAndBlocksTheNextUntilItEnds() {
        val engine = FakePictureEngine(pikachu).apply { scoreGate = CountDownLatch(1) }
        val recognizer = recognizer(engine, timeoutMs = 50)
        drain()

        assertNull(recognizer.submit(input)?.await())
        assertNull(recognizer.submit(input))
        engine.scoreGate.countDown()
        drain()
        assertNotNull(recognizer.submit(input))
    }

    // Scenario: Model missing keeps names working
    @Test
    fun aFailedLoadSilencesItForGood() {
        val recognizer = recognizer(FakePictureEngine(pikachu, failLoad = true))
        drain()

        assertNull(recognizer.submit(input))
        assertNull(recognizer.submit(input))
    }

    @Test
    fun aFailedFrameSilencesItForGood() {
        val recognizer = recognizer(FakePictureEngine(pikachu, failScore = true))
        drain()

        assertNull(recognizer.submit(input)?.await())
        drain()
        assertNull(recognizer.submit(input))
    }

    @Test
    fun closingWhileAFrameRunsIsSafe() {
        val engine = FakePictureEngine(pikachu).apply { scoreGate = CountDownLatch(1) }
        val recognizer = recognizer(engine)
        drain()

        val pending = recognizer.submit(input)
        engine.scoring.await(2, TimeUnit.SECONDS)
        recognizer.close()
        assertNull(recognizer.submit(input))
        engine.scoreGate.countDown()
        assertEquals(pikachu, pending?.await())
        assertTrue(engine.closed.await(2, TimeUnit.SECONDS))
        recognizer.close()
    }

    @Test
    fun submitAfterCloseReturnsNull() {
        val recognizer = recognizer(FakePictureEngine(pikachu))
        drain()
        recognizer.close()

        assertNull(recognizer.submit(input))
    }
}
