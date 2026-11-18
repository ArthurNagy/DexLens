package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.connectivity.ConnectivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ConnectivityRepository]: set [internetAvailable] to go online or offline. */
class FakeConnectivityRepository(
    internetAvailable: Boolean = true,
) : ConnectivityRepository {
    val internetAvailable = MutableStateFlow(internetAvailable)

    override fun observeInternetAvailable(): Flow<Boolean> = internetAvailable
}
