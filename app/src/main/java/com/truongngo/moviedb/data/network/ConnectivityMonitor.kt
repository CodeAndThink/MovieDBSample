package com.truongngo.moviedb.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class ConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    val isConnected: Flow<Boolean> = callbackFlow {
        // Serialize the initial snapshot with callbacks so an old snapshot cannot win.
        val lock = Any()
        var currentNetwork: Network? = null
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                synchronized(lock) { currentNetwork = network }
                // Wait for onCapabilitiesChanged: onAvailable does not imply Internet access.
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                synchronized(lock) {
                    if (network == currentNetwork) trySend(capabilities.hasInternet())
                }
            }

            override fun onLost(network: Network) {
                synchronized(lock) {
                    if (network == currentNetwork) {
                        currentNetwork = null
                        trySend(false)
                    }
                }
            }
        }
        synchronized(lock) {
            manager.registerDefaultNetworkCallback(callback)
            currentNetwork = manager.activeNetwork
            trySend(manager.getNetworkCapabilities(currentNetwork)?.hasInternet() == true)
        }
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun NetworkCapabilities.hasInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
