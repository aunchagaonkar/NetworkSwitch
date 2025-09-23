package com.supernova.networkswitch.data.source

import com.supernova.networkswitch.domain.model.CompatibilityState

/**
 * Interface for network control data sources
 */
interface NetworkControlDataSource {
    suspend fun checkCompatibility(subId: Int): CompatibilityState
    suspend fun getNetworkState(subId: Int): Boolean
    suspend fun setNetworkState(subId: Int, enabled: Boolean)
    fun isConnected(): Boolean
}
