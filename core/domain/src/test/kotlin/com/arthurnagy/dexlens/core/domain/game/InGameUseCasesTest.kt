package com.arthurnagy.dexlens.core.domain.game

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.InGame
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeGameRepository
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import com.arthurnagy.dexlens.core.testing.RedBlue
import com.arthurnagy.dexlens.core.testing.ScarletViolet
import com.arthurnagy.dexlens.core.testing.SwordShield
import com.arthurnagy.dexlens.core.testing.sampleGameDetails
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InGameUseCasesTest {
    private val games = FakeGameRepository(
        gamesByVariety = mapOf(
            1 to listOf(ScarletViolet, SwordShield, RedBlue),
            906 to listOf(ScarletViolet),
        ),
    )

    // Scenario: Newest game by default
    @Test
    fun withoutAPickTheNewestGameIsChosen() = runTest {
        val useCase = GetInGameDetailsUseCase(games, FakeUserPreferencesRepository(ThemeMode.SYSTEM))

        assertEquals(Ok(InGame(listOf(ScarletViolet, SwordShield, RedBlue), sampleGameDetails(ScarletViolet))), useCase(1, null))
    }

    // Scenario: Last picked game is remembered
    @Test
    fun theStoredPickIsChosenWhenListed() = runTest {
        val preferences = FakeUserPreferencesRepository(ThemeMode.SYSTEM)
        SetPreferredGameUseCase(preferences)(SwordShield.id)
        val useCase = GetInGameDetailsUseCase(games, preferences)

        assertEquals(SwordShield, useCase(1, null).chosen())
        assertEquals(SwordShield.id, preferences.preferredGame.first())
    }

    // Scenario: Newest game by default
    @Test
    fun aStoredPickWithoutDataFallsBackToTheNewest() = runTest {
        val useCase = GetInGameDetailsUseCase(games, FakeUserPreferencesRepository(ThemeMode.SYSTEM, initialPreferredGame = RedBlue.id))

        assertEquals(ScarletViolet, useCase(906, null).chosen())
    }

    @Test
    fun aRequestedGameWinsOverTheStoredPick() = runTest {
        val useCase = GetInGameDetailsUseCase(games, FakeUserPreferencesRepository(ThemeMode.SYSTEM, initialPreferredGame = SwordShield.id))

        assertEquals(RedBlue, useCase(1, RedBlue.id).chosen())
        assertEquals(SwordShield, useCase(1, 999).chosen())
    }

    @Test
    fun aVarietyWithoutGamesHasNoDetails() = runTest {
        val useCase = GetInGameDetailsUseCase(games, FakeUserPreferencesRepository(ThemeMode.SYSTEM))

        assertEquals(Ok(InGame(emptyList(), null)), useCase(42, null))
    }

    @Test
    fun failuresAreReturned() = runTest {
        games.nextError = DomainError.Unknown("disk")
        val preferences = FakeUserPreferencesRepository(ThemeMode.SYSTEM).apply { failNextWrite = DomainError.Unknown("full") }

        assertEquals(Err(DomainError.Unknown("disk")), GetInGameDetailsUseCase(games, preferences)(1, null))
        assertEquals(Err(DomainError.Unknown("full")), SetPreferredGameUseCase(preferences)(RedBlue.id))
    }

    private fun Result<InGame, DomainError>.chosen(): Game? = get()?.details?.game
}
