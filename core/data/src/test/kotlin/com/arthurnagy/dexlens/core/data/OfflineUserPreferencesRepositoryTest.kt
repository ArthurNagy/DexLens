package com.arthurnagy.dexlens.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.arthurnagy.dexlens.core.datastore.UserPreferencesDataSource
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class OfflineUserPreferencesRepositoryTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Test
    fun storesAndEmitsTheThemeMode() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmpFolder.newFolder().resolve("prefs.preferences_pb")
        }
        val repository = OfflineUserPreferencesRepository(UserPreferencesDataSource(dataStore))

        assertEquals(Ok(Unit), repository.setThemeMode(ThemeMode.LIGHT))
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
    }

    @Test
    fun storesAndEmitsTheScannerSwitch() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmpFolder.newFolder().resolve("prefs.preferences_pb")
        }
        val repository = OfflineUserPreferencesRepository(UserPreferencesDataSource(dataStore))

        assertEquals(true, repository.scannerEnabled.first())
        assertEquals(Ok(Unit), repository.setScannerEnabled(false))
        assertEquals(false, repository.scannerEnabled.first())
    }

    @Test
    fun storesAndEmitsTheRecognitionScoresSwitch() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmpFolder.newFolder().resolve("prefs.preferences_pb")
        }
        val repository = OfflineUserPreferencesRepository(UserPreferencesDataSource(dataStore))

        assertEquals(false, repository.recognitionScoresShown.first())
        assertEquals(Ok(Unit), repository.setRecognitionScoresShown(true))
        assertEquals(true, repository.recognitionScoresShown.first())
    }

    @Test
    fun aFailedScoresSwitchWriteBecomesUnknown() = runTest {
        val repository = OfflineUserPreferencesRepository(UserPreferencesDataSource(FailingDataStore))

        assertEquals(Err(DomainError.Unknown("disk full")), repository.setRecognitionScoresShown(true))
    }

    @Test
    fun thePreferredGameIsStored() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmpFolder.newFolder().resolve("prefs.preferences_pb")
        }
        val repository = OfflineUserPreferencesRepository(UserPreferencesDataSource(dataStore))

        assertEquals(null, repository.preferredGame.first())
        assertEquals(Ok(Unit), repository.setPreferredGame(25))
        assertEquals(25, repository.preferredGame.first())
        assertEquals(
            Err(DomainError.Unknown("disk full")),
            OfflineUserPreferencesRepository(UserPreferencesDataSource(FailingDataStore)).setPreferredGame(25),
        )
    }

    @Test
    fun aFailedScannerSwitchWriteBecomesUnknown() = runTest {
        val repository = OfflineUserPreferencesRepository(UserPreferencesDataSource(FailingDataStore))

        assertEquals(Err(DomainError.Unknown("disk full")), repository.setScannerEnabled(false))
    }

    @Test
    fun aFailedWriteBecomesUnknown() = runTest {
        val repository = OfflineUserPreferencesRepository(UserPreferencesDataSource(FailingDataStore))

        assertEquals(Err(DomainError.Unknown("disk full")), repository.setThemeMode(ThemeMode.DARK))
    }

    private object FailingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf()

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("disk full")
    }
}
