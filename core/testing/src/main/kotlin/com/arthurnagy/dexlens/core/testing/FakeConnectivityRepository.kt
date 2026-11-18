package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.connectivity.ConnectivityRepository
import com.arthurnagy.dexlens.core.model.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ConnectivityRepository]: set [connectivity] to go online or offline. */
class FakeConnectivityRepository(
    connectivity: Connectivity = Connectivity.Online,
) : ConnectivityRepository {
    val connectivity = MutableStateFlow(connectivity)

    override fun observeConnectivity(): Flow<Connectivity> = connectivity
}
