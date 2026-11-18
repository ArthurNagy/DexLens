package com.arthurnagy.dexlens.core.domain.connectivity

import com.arthurnagy.dexlens.core.model.Connectivity
import kotlinx.coroutines.flow.Flow

/** Whether the device can reach the internet right now. Reading it sends nothing over the network. */
interface ConnectivityRepository {
    /**
     * [Connectivity.Online] while the default network has internet, Android has validated it and it isn't blocked
     * for this app; [Connectivity.Offline] otherwise. Emits the current value as soon as it is collected, then each
     * change. Never throws.
     */
    fun observeConnectivity(): Flow<Connectivity>
}
