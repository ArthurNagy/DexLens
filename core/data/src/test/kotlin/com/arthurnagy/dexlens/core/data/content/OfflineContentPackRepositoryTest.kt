package com.arthurnagy.dexlens.core.data.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import com.arthurnagy.dexlens.core.data.species.FakeSpeciesDao
import com.arthurnagy.dexlens.core.database.entity.FlavorTextEntity
import com.arthurnagy.dexlens.core.database.entity.SpeciesEntity
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class OfflineContentPackRepositoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val TestScope.files get() = ContentFileStore(context, TestDispatcherProvider(testScheduler))
    private val contentDao = FakeContentDao()
    private val species =
        FakeSpeciesDao(species = listOf(SpeciesEntity(1, "a", 1, 4, null, 1, isBaby = false, isLegendary = false, isMythical = false)))

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun clean() {
        File(context.filesDir, "content").deleteRecursively()
    }

    private fun TestScope.repository(): OfflineContentPackRepository = OfflineContentPackRepository(
        WorkManager.getInstance(context),
        files,
        contentDao,
        species,
    )

    @Test
    fun startsAsNotDownloaded() = runTest {
        assertEquals(ContentPackState.NotDownloaded, repository().observeState().first())
        assertEquals(ArtworkLocation(null), repository().observeArtwork().first())
    }

    @Test
    fun partialContentIsInterrupted() = runTest {
        files.write(1, byteArrayOf(1))
        assertEquals(ContentPackState.Interrupted(completed = 1, total = 2), repository().observeState().first())
    }

    // Scenario: Art appears when the download finishes
    @Test
    fun artworkIsExposedOnlyOnceTheDownloadIsComplete() = runTest {
        files.write(1, byteArrayOf(1))
        val repository = repository()
        assertEquals(ArtworkLocation(null), repository.observeArtwork().first())

        files.markFlavorImported()
        files.markComplete()

        assertEquals(ArtworkLocation(files.root.path), repository.observeArtwork().first())
    }

    // Scenario: Art offline after download
    @Test
    fun completeContentIsReadyAndExposesArtwork() = runTest {
        files.write(1, byteArrayOf(1))
        files.markFlavorImported()
        files.markComplete()
        assertEquals(ContentPackState.Ready, repository().observeState().first())
        assertEquals(files.root.path + "/artwork/1.webp", repository().observeArtwork().first().artwork(SpeciesId(1)))
    }

    @Test
    fun aFinishedRunIsReadyEvenWithAMissingUpstreamFile() = runTest {
        files.write(1, byteArrayOf(1))
        files.markComplete()
        assertEquals(ContentPackState.Ready, repository().observeState().first())
    }

    @Test
    fun anUnreadableDatabaseFallsBackToTheFilesOnDisk() = runTest {
        val broken = FakeSpeciesDao(failure = IllegalStateException("broken"))
        val repository = OfflineContentPackRepository(WorkManager.getInstance(context), files, contentDao, broken)
        assertEquals(ContentPackState.NotDownloaded, repository.observeState().first())
    }

    @Test
    fun startingEnqueuesWorkAndCancelStopsIt() = runTest {
        val repository = repository()
        repository.observeState().test {
            assertEquals(ContentPackState.NotDownloaded, awaitItem())
            repository.start()
            assertEquals(ContentPackState.Downloading(completed = 0, total = 2), awaitItem())
            repository.cancel()
            assertEquals(ContentPackState.NotDownloaded, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Scenario: Delete frees content
    @Test
    fun deleteRemovesFilesAndText() = runTest {
        files.write(1, byteArrayOf(1))
        contentDao.upsertAll(listOf(FlavorTextEntity(1, 1, 9, "Hi")))
        val repository = repository()

        assertEquals(Ok(Unit), repository.delete())

        assertEquals(emptySet(), files.storedIds())
        assertTrue(contentDao.rows.isEmpty())
        assertEquals(ContentPackState.NotDownloaded, repository.observeState().first())
    }
}
