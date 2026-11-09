package com.arthurnagy.dexlens.core.data.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import com.arthurnagy.dexlens.core.data.species.FakeSpeciesDao
import com.arthurnagy.dexlens.core.database.FlavorTextEntity
import com.arthurnagy.dexlens.core.database.SpeciesEntity
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class OfflineContentPackRepositoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val files by lazy { ContentFileStore(context) }
    private val contentDao = FakeContentDao()
    private val species =
        FakeSpeciesDao(species = listOf(SpeciesEntity(1, "a", 1, 4, null, 1, isBaby = false, isLegendary = false, isMythical = false)))

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun clean() = files.clear()

    private fun TestScope.repository(): OfflineContentPackRepository = OfflineContentPackRepository(
        WorkManager.getInstance(context),
        files,
        contentDao,
        species,
        TestDispatcherProvider(testScheduler),
    )

    @Test
    fun startsAsNotDownloaded() = runTest {
        assertEquals(ContentPackState.NotDownloaded, repository().observeState().first())
        assertEquals(ArtworkLocation(null), repository().observeArtwork().first())
    }

    @Test
    fun partialContentIsInterrupted() = runTest {
        files.write(ArtKind.SPRITE, 1, byteArrayOf(1))
        assertEquals(ContentPackState.Interrupted(completed = 1, total = 3), repository().observeState().first())
    }

    // Scenario: Art offline after download
    @Test
    fun completeContentIsReadyAndExposesArtwork() = runTest {
        files.write(ArtKind.SPRITE, 1, byteArrayOf(1))
        files.write(ArtKind.ARTWORK, 1, byteArrayOf(1))
        files.markFlavorImported()
        files.markComplete()
        assertEquals(ContentPackState.Ready, repository().observeState().first())
        assertEquals(files.root.path + "/artwork/1.webp", repository().observeArtwork().first().artwork(SpeciesId(1)))
    }

    @Test
    fun aFinishedRunIsReadyEvenWithAMissingUpstreamFile() = runTest {
        files.write(ArtKind.SPRITE, 1, byteArrayOf(1))
        files.markComplete()
        assertEquals(ContentPackState.Ready, repository().observeState().first())
    }

    @Test
    fun anUnreadableDatabaseFallsBackToTheFilesOnDisk() = runTest {
        val broken = FakeSpeciesDao(failure = IllegalStateException("broken"))
        val repository =
            OfflineContentPackRepository(WorkManager.getInstance(context), files, contentDao, broken, TestDispatcherProvider(testScheduler))
        assertEquals(ContentPackState.NotDownloaded, repository.observeState().first())
    }

    @Test
    fun startingEnqueuesWorkAndCancelStopsIt() = runTest {
        val repository = repository()
        repository.observeState().test {
            assertEquals(ContentPackState.NotDownloaded, awaitItem())
            repository.start()
            assertEquals(ContentPackState.Downloading(completed = 0, total = 3), awaitItem())
            repository.cancel()
            assertEquals(ContentPackState.NotDownloaded, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Scenario: Delete frees content
    @Test
    fun deleteRemovesFilesAndText() = runTest {
        files.write(ArtKind.SPRITE, 1, byteArrayOf(1))
        contentDao.upsertAll(listOf(FlavorTextEntity(1, 1, 9, "Hi")))
        val repository = repository()

        assertEquals(Ok(Unit), repository.delete())

        assertFalse(files.exists(ArtKind.SPRITE, 1))
        assertTrue(contentDao.rows.isEmpty())
        assertEquals(ContentPackState.NotDownloaded, repository.observeState().first())
    }
}
