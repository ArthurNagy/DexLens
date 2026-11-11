package com.arthurnagy.dexlens.core.data.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.data.species.FakeSpeciesDao
import com.arthurnagy.dexlens.core.database.entity.SpeciesEntity
import com.arthurnagy.dexlens.core.database.entity.VarietyEntity
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.network.content.ContentRemoteDataSource
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalAtomicApi::class)
@RunWith(AndroidJUnit4::class)
internal class ContentPackDownloaderTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val TestScope.files get() = ContentFileStore(context, TestDispatcherProvider(testScheduler))

    private class FakeRemote(
        private val failOn: (Int) -> Exception? = { null },
        private val flavorCsv: String = "species_id,version_id,language_id,flavor_text\n1,1,9,Hello\n",
    ) : ContentRemoteDataSource {
        val requested = mutableListOf<Int>()
        val active = AtomicInt(0)
        var peak = 0
        var flavorCalls = 0

        override suspend fun artwork(id: Int): ByteArray {
            synchronized(this) { requested += id }
            peak = maxOf(peak, active.incrementAndFetch())
            yield()
            active.decrementAndFetch()
            failOn(id)?.let { throw it }
            return byteArrayOf(1, 2, 3)
        }

        override suspend fun <T> flavorText(read: suspend (InputStream) -> T): T {
            flavorCalls++
            return read(flavorCsv.byteInputStream())
        }
    }

    private fun species(
        count: Int,
        alternates: List<Int>,
    ) = FakeSpeciesDao(
        species = (1..count).map { SpeciesEntity(it, "s$it", 1, 4, null, 1, isBaby = false, isLegendary = false, isMythical = false) },
        varieties = alternates.map { VarietyEntity(it, 1, "v$it", "v$it", false, 2, 1, 1, 1, null, 1, 1, 1, 1, 1, 1) },
    )

    private fun TestScope.downloader(
        remote: ContentRemoteDataSource,
        count: Int = 2,
        alternates: List<Int> = emptyList(),
    ): ContentPackDownloader = ContentPackDownloader(
        remote,
        files,
        ImageTranscoder { png, _ -> png },
        FlavorTextImporter(FakeContentDao()) { 9 },
        species(count, alternates),
        TestDispatcherProvider(testScheduler),
    )

    private val progress = MutableStateFlow(ContentProgress(0, 0))

    @After
    fun clean() {
        File(context.filesDir, "content").deleteRecursively()
    }

    // Scenario: Progress shown
    @Test
    fun downloadsEverythingAndReportsProgress() = runTest {
        assertEquals(Ok(Unit), downloader(FakeRemote()).download(progress))
        assertEquals(ContentProgress(completed = 3, total = 3), progress.value)
        assertEquals(setOf(1, 2), files.storedIds())
        assertTrue(files.isFlavorImported())
        assertTrue(files.isComplete())
    }

    // Scenario: One picture for each variety
    @Test
    fun fetchesOnePicturePerVariety() = runTest {
        val remote = FakeRemote()

        downloader(remote, alternates = listOf(10034, 10035)).download(progress)

        assertEquals(listOf(1, 2, 10034, 10035), remote.requested.sorted())
        assertEquals(setOf(1, 2, 10034, 10035), files.storedIds())
        assertEquals(ContentProgress(completed = 5, total = 5), progress.value)
    }

    // Scenario: Resume skips finished files
    @Test
    fun finishedFilesAreNotFetchedAgain() = runTest {
        files.write(1, byteArrayOf(9))
        files.markFlavorImported()
        val remote = FakeRemote()

        assertEquals(Ok(Unit), downloader(remote).download(progress))

        assertEquals(listOf(2), remote.requested)
        assertEquals(0, remote.flavorCalls)
        assertEquals(ContentProgress(3, 3), progress.value)
    }

    // Scenario: Failure can be retried
    @Test
    fun aLostConnectionStopsAndKeepsFinishedFiles() = runTest {
        val remote = FakeRemote(failOn = { id -> if (id == 2) IOException("no network") else null })

        assertEquals(Err(DomainError.Network.Connectivity), downloader(remote).download(progress))

        assertEquals(setOf(1), files.storedIds())
        assertFalse(files.isFlavorImported())
    }

    // Scenario: Variety without official artwork is skipped
    @Test
    fun aMissingFileDoesNotStopTheDownload() = runTest {
        val notFound = HttpException(Response.error<Unit>(404, okhttp3.ResponseBody.Companion.create(null, "")))
        val remote = FakeRemote(failOn = { id -> if (id == 2) notFound else null })

        assertEquals(Ok(Unit), downloader(remote).download(progress))

        assertEquals(setOf(1), files.storedIds())
        assertTrue(files.isComplete())
    }

    // Scenario: Limited concurrency
    @Test
    fun neverExceedsTheConcurrencyLimit() = runTest {
        val remote = FakeRemote()
        downloader(remote, count = 40).download(progress)
        assertTrue(remote.peak in 1..6, "peak was ${remote.peak}")
    }

    @Test
    fun flavorFailureIsReported() = runTest {
        val remote = object : ContentRemoteDataSource {
            override suspend fun artwork(id: Int) = byteArrayOf()

            override suspend fun <T> flavorText(read: suspend (InputStream) -> T): T = throw IOException("down")
        }
        assertEquals(Err(DomainError.Network.Connectivity), downloader(remote).download(progress))
        assertFalse(files.isFlavorImported())
    }

    @Test
    fun aStorageFailureWhileListingIsReported() = runTest {
        val failing = FakeSpeciesDao(failure = IllegalStateException("broken"))
        val downloader = ContentPackDownloader(
            FakeRemote(),
            files,
            ImageTranscoder { png, _ -> png },
            FlavorTextImporter(FakeContentDao()) { 9 },
            failing,
            TestDispatcherProvider(testScheduler),
        )
        assertEquals(Err(DomainError.Unknown("broken")), downloader.download(progress))
    }
}
