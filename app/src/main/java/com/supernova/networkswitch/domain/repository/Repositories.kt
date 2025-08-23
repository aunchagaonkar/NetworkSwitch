package com.supernova.networkswitch.domain.repository

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
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
     * Get current 5G enabled state
     */
    suspend fun getFivegEnabled(subId: Int): Boolean
    
    /**
     * Set 5G enabled state
     */
    suspend fun setFivegEnabled(subId: Int, enabled: Boolean): Result<Unit>
    
    /**
     * Observe connection state changes
     */
    fun observeConnectionState(): Flow<Boolean>
    
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
}
