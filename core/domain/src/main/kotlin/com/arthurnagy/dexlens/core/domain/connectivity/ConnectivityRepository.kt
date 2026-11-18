package com.arthurnagy.dexlens.core.domain.connectivity

import kotlinx.coroutines.flow.Flow

/** Whether the device can reach the internet right now. Reading it sends nothing over the network. */
interface ConnectivityRepository {
    /**
     * True while the default network has internet and Android has validated it, and it isn't blocked for this app.
     * Emits the current value as soon as it is collected, then each change. Never throws.
     */
    fun observeInternetAvailable(): Flow<Boolean>
}
