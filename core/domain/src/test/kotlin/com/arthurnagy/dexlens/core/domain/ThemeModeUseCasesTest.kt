package com.arthurnagy.dexlens.core.domain

import app.cash.turbine.test
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeUseCasesTest {
    @Test
    fun observeEmitsTheStoredModeAndEachChange() = runTest {
        val repository = FakeUserPreferencesRepository(ThemeMode.SYSTEM)

        ObserveThemeModeUseCase(repository)().test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
            SetThemeModeUseCase(repository)(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun setReturnsTheRepositoryFailure() = runTest {
        val repository = FakeUserPreferencesRepository(ThemeMode.SYSTEM).apply { failNextWrite = DomainError.Unknown("disk full") }

        assertEquals(Err(DomainError.Unknown("disk full")), SetThemeModeUseCase(repository)(ThemeMode.LIGHT))
        assertEquals(Ok(Unit), SetThemeModeUseCase(repository)(ThemeMode.LIGHT))
    }
}
