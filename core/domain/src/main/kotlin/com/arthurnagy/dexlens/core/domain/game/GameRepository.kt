package com.arthurnagy.dexlens.core.domain.game

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.GameDetails
import com.github.michaelbull.result.Result

/** Level-up learnsets and wild encounters per game. A battle-only form reads its species' default variety. */
interface GameRepository {
    /** The games with moves or locations for the variety, newest first. */
    suspend fun games(varietyId: Int): Result<List<Game>, DomainError>

    suspend fun details(
        varietyId: Int,
        game: Game,
    ): Result<GameDetails, DomainError>
}
