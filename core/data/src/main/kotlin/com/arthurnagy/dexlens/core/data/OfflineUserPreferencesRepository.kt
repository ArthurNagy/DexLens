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
    }
