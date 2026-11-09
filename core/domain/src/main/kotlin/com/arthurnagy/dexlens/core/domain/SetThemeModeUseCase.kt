package com.arthurnagy.dexlens.core.domain

import com.arthurnagy.dexlens.core.model.ThemeMode
import com.github.michaelbull.result.Result
import javax.inject.Inject

/** Stores the user's theme choice. */
class SetThemeModeUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        suspend operator fun invoke(mode: ThemeMode): Result<Unit, DomainError> = repository.setThemeMode(mode)
    }
