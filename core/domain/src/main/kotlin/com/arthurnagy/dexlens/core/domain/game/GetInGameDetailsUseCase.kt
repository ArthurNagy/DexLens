package com.arthurnagy.dexlens.core.domain.game

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.InGame
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The games a variety appears in and one game's details. The game is [requestedGameId] when listed, else the one the
 * user last picked when listed, else the newest.
 */
class GetInGameDetailsUseCase
    @Inject
    constructor(
        private val games: GameRepository,
        private val preferences: UserPreferencesRepository,
    ) : UseCase {
        suspend operator fun invoke(
            varietyId: Int,
            requestedGameId: Int?,
        ): Result<InGame, DomainError> = games.games(varietyId).andThen { listed ->
            val chosen = listed.withId(requestedGameId) ?: listed.withId(preferences.preferredGame.first()) ?: listed.firstOrNull()
            if (chosen == null) {
                Ok(InGame(listed, details = null))
            } else {
                games.details(varietyId, chosen).map { details -> InGame(listed, details) }
            }
        }

        private fun List<Game>.withId(id: Int?): Game? = id?.let { wanted -> firstOrNull { it.id == wanted } }
    }
