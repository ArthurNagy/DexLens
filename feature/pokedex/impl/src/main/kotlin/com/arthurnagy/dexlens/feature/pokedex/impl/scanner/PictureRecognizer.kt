package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import com.arthurnagy.dexlens.core.domain.scan.PictureScores
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** The model behind the picture signal. Every call comes from the one thread [PictureRecognizer] owns. */
internal interface PictureEngine : AutoCloseable {
    /** Loads the model and its references. Throws if they can't be used. */
    fun load()

    /** Scores one [ColourSquare] of pixels against every species. */
    fun score(input: FloatArray): PictureScores
}

/** A picture job that may still be running. */
internal fun interface PendingScore {
    /** The scores, or null if they didn't arrive in time or the job failed. Blocks for a bounded time. */
    fun await(): PictureScores?
}

/** Scores frames on a thread of its own, so the camera thread can read text meanwhile. */
internal interface PictureRecognizer : AutoCloseable {
    /** Starts scoring a copy of [input], or returns null when the recogniser can't take a frame right now. */
    fun submit(input: FloatArray): PendingScore?
}

/**
 * Runs a [PictureEngine] on one thread, as LiteRT requires. It takes one frame at a time: while a frame is being scored,
 * or the model is still loading, new frames are turned away, so nothing queues behind a slow frame. After a failure it
 * stays silent and the scanner carries on with text alone.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class ThreadedPictureRecognizer(
    private val engine: PictureEngine,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val timeoutMs: Long = PICTURE_TIMEOUT_MS,
) : PictureRecognizer {
    /** True while loading or scoring. Starts true until the model has loaded. */
    private val busy = AtomicBoolean(true)
    private val failed = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val input = FloatArray(ColourSquare.SIZE * ColourSquare.SIZE * ColourSquare.CHANNELS)

    init {
        executor.execute {
            runCatchingEngine("load") { engine.load() }?.let { busy.store(false) }
        }
    }

    override fun submit(input: FloatArray): PendingScore? {
        if (closed.load() || failed.load() || !busy.compareAndSet(expectedValue = false, newValue = true)) return null
        // Safe: the previous job has finished with the buffer, or busy would still be set.
        input.copyInto(this.input)
        val future: Future<PictureScores?> = try {
            executor.submit<PictureScores?> {
                try {
                    // close() may have queued the engine's own close just ahead of this job.
                    if (closed.load()) null else runCatchingEngine("score") { engine.score(this.input) }
                } finally {
                    busy.store(false)
                }
            }
        } catch (e: RejectedExecutionException) {
            Timber.d(e, "picture recogniser already closed")
            busy.store(false)
            return null
        }
        return PendingScore { future.awaitOrNull() }
    }

    override fun close() {
        if (closed.exchange(true)) return
        try {
            executor.execute { runCatchingEngine("close") { engine.close() } }
        } catch (e: RejectedExecutionException) {
            Timber.d(e, "picture recogniser thread already stopped")
        }
        executor.shutdown()
    }

    /**
     * Runs an engine call, turning any failure into a permanent fallback to text alone. The engine wraps a native runtime
     * that can throw anything, and none of it may reach this executor's thread, where it would crash the app. This runs
     * on a plain thread, not in a coroutine, so there is no cancellation to rethrow.
     */
    private fun <T> runCatchingEngine(
        step: String,
        block: () -> T,
    ): T? = try {
        block()
    } catch (e: Exception) {
        fail(e, step)
    } catch (e: LinkageError) {
        // The native runtime is missing, as on the JVM in tests.
        fail(e, step)
    } catch (e: OutOfMemoryError) {
        // Too little memory for the model: the scanner still works on text.
        fail(e, step)
    }

    private fun fail(
        error: Throwable,
        step: String,
    ): Nothing? {
        Timber.w(error, "picture recognition failed to %s; scanning with text alone", step)
        failed.store(true)
        return null
    }

    private fun Future<PictureScores?>.awaitOrNull(): PictureScores? = try {
        get(timeoutMs, TimeUnit.MILLISECONDS)
    } catch (e: TimeoutException) {
        Timber.d(e, "picture frame too slow; counting it as text only")
        null
    } catch (e: ExecutionException) {
        Timber.w(e, "picture frame failed")
        null
    } catch (e: CancellationException) {
        Timber.d(e, "picture frame cancelled")
        null
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        null
    }

    companion object {
        /** How long the camera thread waits for a frame's picture scores before treating the frame as text only. */
        const val PICTURE_TIMEOUT_MS = 200L
    }
}
