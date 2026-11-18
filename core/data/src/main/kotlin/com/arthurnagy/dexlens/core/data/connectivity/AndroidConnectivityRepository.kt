package com.arthurnagy.dexlens.core.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.domain.connectivity.ConnectivityRepository
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Reads the default network from [ConnectivityManager]: nothing is sent over the network. "Available" means the
 * default network has internet, Android has validated it (so not behind a sign-in page) and it isn't blocked for this
 * app. When connectivity can't be read at all, it reports available, so the screens keep today's behaviour.
 */
internal class AndroidConnectivityRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ConnectivityRepository {
        override fun observeInternetAvailable(): Flow<Boolean> = callbackFlow {
            val callback = DefaultNetworkCallback { available -> trySend(available) }
            val registered = catchStorage {
                val manager = checkNotNull(context.getSystemService(ConnectivityManager::class.java)) { "No ConnectivityManager" }
                val active = manager.activeNetwork
                callback.seed(active, active?.let(manager::getNetworkCapabilities))
                manager.registerDefaultNetworkCallback(callback)
                manager
            }
            registered.onErr { trySend(true) }
            // Unregistering a callback that was never registered throws, so only undo a successful registration.
            awaitClose { registered.onOk { manager -> manager.unregisterNetworkCallback(callback) } }
        }.distinctUntilChanged()
    }

/**
 * Follows the current default network. The seed runs before registration and the callbacks then run on the system's
 * connectivity thread one at a time, so the fields need no locking.
 */
private class DefaultNetworkCallback(
    private val send: (Boolean) -> Unit,
) : ConnectivityManager.NetworkCallback() {
    private var current: Network? = null
    private var usable = false
    private var blocked = false

    fun seed(
        network: Network?,
        capabilities: NetworkCapabilities?,
    ) {
        current = network
        usable = capabilities.isUsable()
        publish()
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
    ) {
        if (network != current) {
            current = network
            blocked = false
        }
        usable = networkCapabilities.isUsable()
        publish()
    }

    override fun onBlockedStatusChanged(
        network: Network,
        blocked: Boolean,
    ) {
        if (network != current) return
        this.blocked = blocked
        publish()
    }

    override fun onLost(network: Network) {
        if (network != current) return
        current = null
        usable = false
        blocked = false
        publish()
    }

    private fun publish() = send(usable && !blocked)
}

private fun NetworkCapabilities?.isUsable(): Boolean = this != null &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
