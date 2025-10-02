package com.supernova.networkswitch.data.source

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.NetworkMode

interface NetworkControlDataSource {
    suspend fun checkCompatibility(subId: Int): CompatibilityState
    suspend fun getCurrentNetworkMode(subId: Int): NetworkMode?
    suspend fun setNetworkMode(subId: Int, mode: NetworkMode)
    fun isConnected(): Boolean
    fun resetConnection()
    suspend fun requestPermission(): Boolean
}
