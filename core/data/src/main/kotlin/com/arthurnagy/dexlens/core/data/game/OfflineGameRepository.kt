package com.arthurnagy.dexlens.core.data.game

import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.data.species.DisplayLanguage
import com.arthurnagy.dexlens.core.database.EncounterRow
import com.arthurnagy.dexlens.core.database.GameDao
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.game.GameRepository
import com.arthurnagy.dexlens.core.model.EncounterMethod
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.GameDetails
import com.arthurnagy.dexlens.core.model.LevelUpMove
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.WildEncounter
import com.github.michaelbull.result.Result
import javax.inject.Inject

internal class OfflineGameRepository
    @Inject
    constructor(
        private val dao: GameDao,
        private val displayLanguage: DisplayLanguage,
    ) : GameRepository {
        override suspend fun games(varietyId: Int): Result<List<Game>, DomainError> = catchStorage {
            val groups = dao.versionGroups(dataVariety(varietyId))
            val versions = dao.versions(groups.map { it.id }, displayLanguage.languageId()).groupBy { it.versionGroupId }
            groups.map { group ->
                Game(id = group.id, versions = versions[group.id].orEmpty().map { it.name }, hasLocationData = group.hasEncounters)
            }
        }

        override suspend fun details(
            varietyId: Int,
            game: Game,
        ): Result<GameDetails, DomainError> = catchStorage {
            val variety = dataVariety(varietyId)
            val language = displayLanguage.languageId()
            val versions = dao.versions(listOf(game.id), language).associate { it.id to it.name }
            GameDetails(
                game = game,
                moves = dao.levelUpMoves(variety, game.id, language).map { row ->
                    LevelUpMove(level = row.level, name = row.name, type = typeOf(row.typeId), power = row.power)
                },
                encounters = encountersOf(dao.encounters(variety, game.id, language), versions),
            )
        }

        private suspend fun dataVariety(varietyId: Int): Int =
            if (dao.usesOwnData(varietyId)) varietyId else dao.defaultVariety(varietyId) ?: varietyId

        /** One row per location and way of finding it, across the game's versions, with the widest level range. */
        private fun encountersOf(
            rows: List<EncounterRow>,
            versions: Map<Int, String>,
        ): List<WildEncounter> = rows
            .groupBy { it.locationId to it.methodIdentifier }
            .values
            .map { same ->
                val foundIn = same.map { it.versionId }.toSet()
                WildEncounter(
                    location = same.first().location,
                    method = EncounterMethod(identifier = same.first().methodIdentifier, name = same.first().method),
                    minLevel = same.minOf { it.minLevel },
                    maxLevel = same.maxOf { it.maxLevel },
                    onlyIn = if (foundIn.containsAll(versions.keys)) emptyList() else versions.filterKeys { it in foundIn }.values.toList(),
                )
            }.sortedWith(compareBy({ it.minLevel }, { it.location }))

        private fun typeOf(id: Int): PokemonType? = PokemonType.entries.firstOrNull { it.id == id }
    }
