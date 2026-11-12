package com.arthurnagy.dexlens.core.data

import com.arthurnagy.dexlens.core.datastore.UserPreferencesDataSource
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** DataStore-backed [UserPreferencesRepository]. Reads never throw; a failed write becomes a [DomainError] via [catchStorage]. */
internal class OfflineUserPreferencesRepository
    @Inject
    constructor(
        private val dataSource: UserPreferencesDataSource,
    ) : UserPreferencesRepository {
        override val themeMode: Flow<ThemeMode> = dataSource.themeMode

        override suspend fun setThemeMode(mode: ThemeMode): Result<Unit, DomainError> = catchStorage { dataSource.setThemeMode(mode) }

        override val contentOfferShown: Flow<Boolean> = dataSource.contentOfferShown

        override suspend fun setContentOfferShown(shown: Boolean): Result<Unit, DomainError> = catchStorage {
            dataSource.setContentOfferShown(shown)
        }

        override val scannerEnabled: Flow<Boolean> = dataSource.scannerEnabled

        override suspend fun setScannerEnabled(enabled: Boolean): Result<Unit, DomainError> = catchStorage {
            dataSource.setScannerEnabled(enabled)
        }

        override val recognitionScoresShown: Flow<Boolean> = dataSource.recognitionScoresShown

        override suspend fun setRecognitionScoresShown(shown: Boolean): Result<Unit, DomainError> = catchStorage {
            dataSource.setRecognitionScoresShown(shown)
        }

        override val preferredGame: Flow<Int?> = dataSource.preferredGame

        override suspend fun setPreferredGame(gameId: Int): Result<Unit, DomainError> = catchStorage { dataSource.setPreferredGame(gameId) }
    }
