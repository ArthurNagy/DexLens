package com.arthurnagy.dexlens.core.domain

import app.cash.turbine.test
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RecognitionScoresShownUseCasesTest {
    @Test
    fun observeStartsOffAndEmitsEachChange() = runTest {
        val repository = FakeUserPreferencesRepository(ThemeMode.SYSTEM)

        ObserveRecognitionScoresShownUseCase(repository)().test {
            assertEquals(false, awaitItem())
            SetRecognitionScoresShownUseCase(repository)(true)
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun setReturnsTheRepositoryFailure() = runTest {
        val repository = FakeUserPreferencesRepository(ThemeMode.SYSTEM).apply { failNextWrite = DomainError.Unknown("disk full") }

        assertEquals(Err(DomainError.Unknown("disk full")), SetRecognitionScoresShownUseCase(repository)(true))
        assertEquals(Ok(Unit), SetRecognitionScoresShownUseCase(repository)(true))
    }
}
