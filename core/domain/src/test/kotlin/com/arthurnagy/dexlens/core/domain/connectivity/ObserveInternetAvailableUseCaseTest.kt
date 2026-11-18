package com.arthurnagy.dexlens.core.domain.connectivity

import app.cash.turbine.test
import com.arthurnagy.dexlens.core.testing.FakeConnectivityRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveInternetAvailableUseCaseTest {
    @Test
    fun emitsTheCurrentValueAndEachChange() = runTest {
        val repository = FakeConnectivityRepository(internetAvailable = false)

        ObserveInternetAvailableUseCase(repository)().test {
            assertEquals(false, awaitItem())
            repository.internetAvailable.value = true
            assertEquals(true, awaitItem())
        }
    }
}
