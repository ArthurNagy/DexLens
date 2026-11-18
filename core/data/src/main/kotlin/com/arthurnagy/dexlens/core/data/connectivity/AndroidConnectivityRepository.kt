package com.arthurnagy.dexlens.core.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.domain.connectivity.ConnectivityRepository
import com.arthurnagy.dexlens.core.model.Connectivity
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.runCatching
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject

/**
 * Reads the default network from [ConnectivityManager]: nothing is sent over the network. [Connectivity.Online] means the
 * default network has internet, Android has validated it (so not behind a sign-in page) and it isn't blocked for this
 * app. When connectivity can't be read at all, it reports [Connectivity.Online], so the screens keep today's behaviour.
 */
internal class AndroidConnectivityRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ConnectivityRepository {
        override fun observeConnectivity(): Flow<Connectivity> = callbackFlow {
            val callback = DefaultNetworkCallback { connectivity -> trySend(connectivity) }
            val registered = catchStorage {
                val manager = checkNotNull(context.getSystemService(ConnectivityManager::class.java)) { "No ConnectivityManager" }
                val active = manager.activeNetwork
                callback.seed(active, active?.let(manager::getNetworkCapabilities))
                manager.registerDefaultNetworkCallback(callback)
                manager
            }
            registered.onErr { trySend(Connectivity.Online) }
            // Unregistering a callback that was never registered throws, so only undo a successful registration. Any
            // other platform failure on teardown stops here too, so it never reaches the collector.
            awaitClose {
                registered.onOk { manager ->
                    runCatching { manager.unregisterNetworkCallback(callback) }
                        .onErr { e -> Timber.w(e, "unregisterNetworkCallback failed") }
                }
            }
        }.distinctUntilChanged()
    }

/**
 * Follows the current default network. The seed runs before registration and the callbacks then run on the system's
 * connectivity thread one at a time, so the fields need no locking.
 */
private class DefaultNetworkCallback(
    private val send: (Connectivity) -> Unit,
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

    private fun publish() = send(if (usable && !blocked) Connectivity.Online else Connectivity.Offline)
}

private fun NetworkCapabilities?.isUsable(): Boolean = this != null &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
