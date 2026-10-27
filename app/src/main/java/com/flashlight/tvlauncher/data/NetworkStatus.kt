package com.flashlight.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkKind { Wifi, Ethernet, Other, None }

/** Тип текущего подключения; обновляется при смене сети */
fun Context.networkKind(): Flow<NetworkKind> = callbackFlow {
    val connectivity = getSystemService(ConnectivityManager::class.java)
    fun kindOf(capabilities: NetworkCapabilities?): NetworkKind = when {
        capabilities == null -> NetworkKind.None
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkKind.Wifi
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.Ethernet
        else -> NetworkKind.Other
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(kindOf(capabilities))
        }

        override fun onLost(network: Network) {
            trySend(NetworkKind.None)
        }
    }
    trySend(kindOf(connectivity.getNetworkCapabilities(connectivity.activeNetwork)))
    connectivity.registerDefaultNetworkCallback(callback)
    awaitClose { connectivity.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

fun Context.openNetworkSettings() {
    val opened = runCatching {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
    if (!opened) openSystemSettings()
}
