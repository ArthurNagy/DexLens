package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.game.GameRepository
import com.arthurnagy.dexlens.core.model.EncounterMethod
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.GameDetails
import com.arthurnagy.dexlens.core.model.LevelUpMove
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.WildEncounter
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

/** In-memory [GameRepository]: [gamesByVariety] lists each variety's games, newest first, and [detailsFor] answers every game. */
class FakeGameRepository(
    var gamesByVariety: Map<Int, List<Game>> = emptyMap(),
    var detailsFor: (varietyId: Int, game: Game) -> GameDetails = { _, game -> sampleGameDetails(game) },
) : GameRepository {
    var nextError: DomainError? = null
    val detailRequests = mutableListOf<Pair<Int, Int>>()

    override suspend fun games(varietyId: Int): Result<List<Game>, DomainError> {
        nextError?.let {
            nextError = null
            return Err(it)
        }
        return Ok(gamesByVariety[varietyId].orEmpty())
    }

    override suspend fun details(
        varietyId: Int,
        game: Game,
    ): Result<GameDetails, DomainError> {
        detailRequests += varietyId to game.id
        return Ok(detailsFor(varietyId, game))
    }
}

val ScarletViolet = Game(id = 25, versions = listOf("Scarlet", "Violet"), hasLocationData = false)
val SwordShield = Game(id = 20, versions = listOf("Sword", "Shield"), hasLocationData = true)
val RedBlue = Game(id = 1, versions = listOf("Red", "Blue"), hasLocationData = true)

/** Bulbasaur-like details: three moves, and a location limited to one version when the game has locations. */
fun sampleGameDetails(game: Game): GameDetails = GameDetails(
    game = game,
    moves = listOf(
        LevelUpMove(level = 1, name = "Tackle", type = PokemonType.NORMAL, power = 40),
        LevelUpMove(level = 3, name = "Vine Whip", type = PokemonType.GRASS, power = 45),
        LevelUpMove(level = 6, name = "Growth", type = PokemonType.NORMAL, power = null),
    ),
    encounters = if (game.hasLocationData) {
        listOf(
            WildEncounter(
                location = "Route 1",
                method = EncounterMethod("walk", "Walking in tall grass or a cave"),
                minLevel = 3,
                maxLevel = 5,
                onlyIn = emptyList(),
            ),
            WildEncounter(
                location = "Cerulean City",
                method = EncounterMethod("gift", "Receive as a gift"),
                minLevel = 10,
                maxLevel = 10,
                onlyIn = listOf("Blue"),
            ),
        )
    } else {
        emptyList()
    },
)
