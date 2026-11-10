package com.arthurnagy.dexlens.core.domain

import app.cash.turbine.test
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ScannerEnabledUseCasesTest {
    @Test
    fun observeEmitsTheStoredValueAndEachChange() = runTest {
        val repository = FakeUserPreferencesRepository(ThemeMode.SYSTEM)

        ObserveScannerEnabledUseCase(repository)().test {
            assertEquals(true, awaitItem())
            SetScannerEnabledUseCase(repository)(false)
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun setReturnsTheRepositoryFailure() = runTest {
        val repository = FakeUserPreferencesRepository(ThemeMode.SYSTEM).apply { failNextWrite = DomainError.Unknown("disk full") }

        assertEquals(Err(DomainError.Unknown("disk full")), SetScannerEnabledUseCase(repository)(false))
        assertEquals(Ok(Unit), SetScannerEnabledUseCase(repository)(false))
    }
}
