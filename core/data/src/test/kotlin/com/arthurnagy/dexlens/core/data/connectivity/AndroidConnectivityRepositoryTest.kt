package com.arthurnagy.dexlens.core.data.connectivity

import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class AndroidConnectivityRepositoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val manager: ConnectivityManager get() = context.getSystemService(ConnectivityManager::class.java)
    private val shadow: ShadowConnectivityManager get() = shadowOf(manager)
    private val otherNetwork: Network = ShadowNetwork.newInstance(42)

    private fun capabilities(vararg capabilities: Int): NetworkCapabilities =
        ShadowNetworkCapabilities.newInstance().also { caps -> capabilities.forEach { shadowOf(caps).addCapability(it) } }

    private val validated get() = capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET, NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    /** Robolectric's default active network, given [caps]. */
    private fun activeNetworkWith(caps: NetworkCapabilities): Network {
        shadow.setDefaultNetworkActive(true)
        val network = checkNotNull(manager.activeNetwork)
        shadow.setNetworkCapabilities(network, caps)
        return network
    }

    private val callback: ConnectivityManager.NetworkCallback get() = shadow.networkCallbacks.single()

    @Test
    fun aValidatedActiveNetworkIsAvailable() = runTest {
        activeNetworkWith(validated)

        AndroidConnectivityRepository(context).observeInternetAvailable().test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Scenario: Network that needs sign-in counts as offline
    @Test
    fun internetWithoutValidationIsNotAvailable() = runTest {
        activeNetworkWith(capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET))

        AndroidConnectivityRepository(context).observeInternetAvailable().test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noActiveNetworkIsNotAvailable() = runTest {
        shadow.setDefaultNetworkActive(false)

        AndroidConnectivityRepository(context).observeInternetAvailable().test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun callbacksReportChangesWithoutRepeats() = runTest {
        val network = activeNetworkWith(capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET))

        AndroidConnectivityRepository(context).observeInternetAvailable().test {
            assertEquals(false, awaitItem())
            callback.onCapabilitiesChanged(network, validated)
            assertEquals(true, awaitItem())
            callback.onCapabilitiesChanged(network, validated)
            callback.onLost(network)
            assertEquals(false, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun losingAnotherNetworkChangesNothing() = runTest {
        activeNetworkWith(validated)

        AndroidConnectivityRepository(context).observeInternetAvailable().test {
            assertEquals(true, awaitItem())
            callback.onLost(otherNetwork)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aBlockedNetworkIsNotAvailableUntilAnotherNetworkTakesOver() = runTest {
        val network = activeNetworkWith(validated)

        AndroidConnectivityRepository(context).observeInternetAvailable().test {
            assertEquals(true, awaitItem())
            callback.onBlockedStatusChanged(network, true)
            assertEquals(false, awaitItem())
            // A different default network starts unblocked.
            callback.onCapabilitiesChanged(otherNetwork, validated)
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun stoppingCollectionUnregistersTheCallback() = runTest {
        activeNetworkWith(validated)

        AndroidConnectivityRepository(context).observeInternetAvailable().test {
            awaitItem()
            assertEquals(1, shadow.networkCallbacks.size)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(shadow.networkCallbacks.isEmpty())
    }

    // Robolectric's registerDefaultNetworkCallback never throws, so a missing ConnectivityManager is the failed
    // registration here: the flow reports available and stops without unregistering anything.
    @Test
    fun withoutAConnectivityManagerItReportsAvailable() = runTest {
        val noManager = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? = if (name == CONNECTIVITY_SERVICE) null else super.getSystemService(name)
        }

        AndroidConnectivityRepository(noManager).observeInternetAvailable().test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(shadow.networkCallbacks.isEmpty())
    }
}
