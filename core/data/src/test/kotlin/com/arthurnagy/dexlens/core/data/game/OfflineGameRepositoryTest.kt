package com.arthurnagy.dexlens.core.data.game

import com.arthurnagy.dexlens.core.database.EncounterRow
import com.arthurnagy.dexlens.core.database.GameDao
import com.arthurnagy.dexlens.core.database.LevelUpMoveRow
import com.arthurnagy.dexlens.core.database.VersionRow
import com.arthurnagy.dexlens.core.database.entity.VersionGroupEntity
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.EncounterMethod
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.LevelUpMove
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.WildEncounter
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class OfflineGameRepositoryTest {
    private val dao = FakeGameDao()
    private val repository = OfflineGameRepository(dao) { ENGLISH }

    private fun <T> Result<T, DomainError>.ok(): T = getOrThrow { error("unexpected $it") }

    // Scenario: Only games with data are offered
    @Test
    fun gamesAreTheVarietysGroupsNamedAfterTheirVersions() = runTest {
        assertEquals(
            listOf(
                Game(SWORD_SHIELD, listOf("Sword", "Shield"), hasLocationData = true),
                Game(RED_BLUE, listOf("Red", "Blue"), hasLocationData = true),
            ),
            repository.games(BULBASAUR).ok(),
        )
        assertEquals(emptyList(), repository.games(UNKNOWN).ok())
    }

    // Scenario: Moves learnt by level up
    @Test
    fun movesKeepTheirOrderTypeAndPower() = runTest {
        val details = repository.details(BULBASAUR, swordShield).ok()

        assertEquals(
            listOf(
                LevelUpMove(1, "Tackle", PokemonType.NORMAL, 40),
                LevelUpMove(3, "Vine Whip", PokemonType.GRASS, 45),
                LevelUpMove(9, "Curse", null, null),
            ),
            details.moves,
        )
    }

    // Scenario: Where to find
    @Test
    fun encountersMergeVersionsMethodsAndLevels() = runTest {
        val details = repository.details(BULBASAUR, swordShield).ok()

        assertEquals(
            WildEncounter("Route 1", EncounterMethod("walk", "Walking in tall grass or a cave"), 2, 7, emptyList()),
            details.encounters[0],
        )
        assertEquals(
            WildEncounter("Route 1", EncounterMethod("headbutt", "Headbutting a low encounter rate tree"), 3, 8, listOf("Shield")),
            details.encounters[1],
        )
    }

    // Scenario: Version-exclusive location
    @Test
    fun aLocationInOneVersionNamesIt() = runTest {
        val details = repository.details(BULBASAUR, swordShield).ok()

        assertEquals(WildEncounter("Lake", EncounterMethod("surf", "Surfing"), 20, 20, listOf("Shield")), details.encounters.last())
        assertEquals(3, details.encounters.size)
    }

    // Scenario: Variety without its own moves
    @Test
    fun aBattleOnlyFormReadsTheDefaultVariety() = runTest {
        assertEquals(repository.games(BULBASAUR).ok(), repository.games(MEGA).ok())
        assertEquals(repository.details(BULBASAUR, swordShield).ok(), repository.details(MEGA, swordShield).ok())
    }

    // Scenario: Variety with its own moves
    @Test
    fun aRegionalFormReadsItsOwnRows() = runTest {
        assertEquals(listOf(Game(SWORD_SHIELD, listOf("Sword", "Shield"), hasLocationData = true)), repository.games(REGIONAL).ok())
        assertEquals(listOf("Powder Snow"), repository.details(REGIONAL, swordShield).ok().moves.map { it.name })
    }

    @Test
    fun aStorageFailureBecomesAnError() = runTest {
        dao.failure = IllegalStateException("disk")

        assertTrue(repository.games(BULBASAUR).isErr)
        assertTrue(repository.details(BULBASAUR, swordShield).isErr)
        dao.failure = null
        assertTrue(repository.games(BULBASAUR).isOk)
    }

    private val swordShield = Game(SWORD_SHIELD, listOf("Sword", "Shield"), hasLocationData = true)

    private class FakeGameDao : GameDao {
        var failure: Exception? = null

        private fun check() {
            failure?.let { throw it }
        }

        override suspend fun usesOwnData(varietyId: Int): Boolean = varietyId != MEGA && varietyId != UNKNOWN

        override suspend fun defaultVariety(varietyId: Int): Int? = if (varietyId == MEGA) BULBASAUR else null

        override suspend fun versionGroups(varietyId: Int): List<VersionGroupEntity> {
            check()
            return when (varietyId) {
                BULBASAUR -> listOf(VersionGroupEntity(SWORD_SHIELD, 8, 22, true), VersionGroupEntity(RED_BLUE, 1, 3, true))
                REGIONAL -> listOf(VersionGroupEntity(SWORD_SHIELD, 8, 22, true))
                else -> emptyList()
            }
        }

        override suspend fun versions(
            versionGroupIds: List<Int>,
            languageId: Int,
        ): List<VersionRow> = listOf(
            VersionRow(RED, RED_BLUE, "Red"),
            VersionRow(BLUE, RED_BLUE, "Blue"),
            VersionRow(SWORD, SWORD_SHIELD, "Sword"),
            VersionRow(SHIELD, SWORD_SHIELD, "Shield"),
        ).filter { it.versionGroupId in versionGroupIds }

        override suspend fun levelUpMoves(
            varietyId: Int,
            versionGroupId: Int,
            languageId: Int,
        ): List<LevelUpMoveRow> {
            check()
            return if (varietyId == REGIONAL) {
                listOf(LevelUpMoveRow(1, "Powder Snow", 15, 40))
            } else {
                listOf(LevelUpMoveRow(1, "Tackle", 1, 40), LevelUpMoveRow(3, "Vine Whip", 12, 45), LevelUpMoveRow(9, "Curse", 10001, null))
            }
        }

        override suspend fun encounters(
            varietyId: Int,
            versionGroupId: Int,
            languageId: Int,
        ): List<EncounterRow> = if (varietyId == BULBASAUR) {
            listOf(
                EncounterRow(SWORD, ROUTE_1, "Route 1", 1, "walk", "Walking in tall grass or a cave", 1, 2, 5),
                EncounterRow(SHIELD, ROUTE_1, "Route 1", 22, "headbutt", "Headbutting a low encounter rate tree", 7, 3, 7),
                EncounterRow(SHIELD, ROUTE_1, "Route 1", 23, "headbutt", "Headbutting a normal encounter rate tree", 7, 4, 8),
                EncounterRow(SHIELD, ROUTE_1, "Route 1", 1, "walk", "Walking in tall grass or a cave", 1, 3, 7),
                EncounterRow(SHIELD, LAKE, "Lake", 5, "surf", "Surfing", 5, 20, 20),
            )
        } else {
            emptyList()
        }
    }

    private companion object {
        const val ENGLISH = 9
        const val BULBASAUR = 1
        const val MEGA = 10033
        const val REGIONAL = 10103
        const val UNKNOWN = 99999
        const val RED_BLUE = 1
        const val SWORD_SHIELD = 20
        const val RED = 1
        const val BLUE = 2
        const val SWORD = 33
        const val SHIELD = 34
        const val ROUTE_1 = 100
        const val LAKE = 200
    }
}
