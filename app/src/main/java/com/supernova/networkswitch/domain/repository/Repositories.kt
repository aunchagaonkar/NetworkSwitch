package com.supernova.networkswitch.domain.repository

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for network control operations
 */
interface NetworkControlRepository {
    /**
     * Check if network control is compatible with current device/method
     */
    suspend fun checkCompatibility(method: ControlMethod): CompatibilityState
    
    /**
     * Get current network mode
     */
    suspend fun getCurrentNetworkMode(subId: Int): NetworkMode?
    
    /**
     * Set network mode
     */
    suspend fun setNetworkMode(subId: Int, mode: NetworkMode): Result<Unit>
    
    /**
     * Reset connections - useful when switching control methods
     */
    suspend fun resetConnections()
}

/**
 * Repository interface for app preferences
 */
interface PreferencesRepository {
    /**
     * Get preferred control method
     */
    suspend fun getControlMethod(): ControlMethod
    
    /**
     * Set preferred control method
     */
    suspend fun setControlMethod(method: ControlMethod)
    
    /**
     * Observe control method changes
     */
    fun observeControlMethod(): Flow<ControlMethod>
    
    /**
     * Get toggle mode configuration
     */
    suspend fun getToggleModeConfig(): ToggleModeConfig
    
    /**
     * Set toggle mode configuration
     */
    suspend fun setToggleModeConfig(config: ToggleModeConfig)
    
    /**
     * Observe toggle mode configuration changes
     */
    fun observeToggleModeConfig(): Flow<ToggleModeConfig>
    
    /**
     * Get the selected subscription ID for the SIM card
     * Returns -1 if no specific SIM is selected (use default)
     */
    suspend fun getSelectedSubscriptionId(): Int
    
    /**
     * Set the selected subscription ID for the SIM card
     * Pass -1 to use the default subscription
     */
    suspend fun setSelectedSubscriptionId(subscriptionId: Int)
    
    /**
     * Observe changes to the selected subscription ID
     */
    fun observeSelectedSubscriptionId(): Flow<Int>
}

/**
 * Repository interface for SIM card operations
 */
interface SimRepository {
    /**
     * Get list of all available SIM cards in the device
     * Returns empty list if permission is not granted
     */
    suspend fun getAvailableSimCards(): List<SimInfo>
}
