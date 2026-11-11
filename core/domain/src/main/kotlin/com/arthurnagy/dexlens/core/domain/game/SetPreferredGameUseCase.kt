package com.arthurnagy.dexlens.core.domain.game

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import com.github.michaelbull.result.Result
import javax.inject.Inject

/** Remembers the game the user picked, so other entries start on it. */
class SetPreferredGameUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        suspend operator fun invoke(gameId: Int): Result<Unit, DomainError> = repository.setPreferredGame(gameId)
    }
