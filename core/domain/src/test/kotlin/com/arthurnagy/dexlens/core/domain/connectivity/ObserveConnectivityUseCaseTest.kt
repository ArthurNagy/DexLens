package com.arthurnagy.dexlens.core.domain.connectivity

import app.cash.turbine.test
import com.arthurnagy.dexlens.core.model.Connectivity
import com.arthurnagy.dexlens.core.testing.FakeConnectivityRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveConnectivityUseCaseTest {
    @Test
    fun emitsTheCurrentValueAndEachChange() = runTest {
        val repository = FakeConnectivityRepository(Connectivity.Offline)

        ObserveConnectivityUseCase(repository)().test {
            assertEquals(Connectivity.Offline, awaitItem())
            repository.connectivity.value = Connectivity.Online
            assertEquals(Connectivity.Online, awaitItem())
        }
    }
}
