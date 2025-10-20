package com.supernova.networkswitch.data.repository

import android.telephony.SubscriptionManager
import com.supernova.networkswitch.data.source.NetworkControlDataSource
import com.supernova.networkswitch.data.source.RootNetworkControlDataSource
import com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NetworkControlRepository that delegates to appropriate data source
 */
@Singleton
class NetworkControlRepositoryImpl @Inject constructor(
    private val rootDataSource: RootNetworkControlDataSource,
    private val shizukuDataSource: ShizukuNetworkControlDataSource,
    private val preferencesRepository: PreferencesRepository
) : NetworkControlRepository {
    
    override suspend fun checkCompatibility(method: ControlMethod): CompatibilityState {
        val dataSource = getDataSource(method)
        // Use selected subscription ID for compatibility check
        val subId = getEffectiveSubscriptionId()
        return dataSource.checkCompatibility(subId)
    }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? {
        return try {
            val method = preferencesRepository.getControlMethod()
            val dataSource = getDataSource(method)
            // Use the provided subId (which should come from getEffectiveSubscriptionId in callers)
            dataSource.getCurrentNetworkMode(subId)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode): Result<Unit> {
        return try {
            val method = preferencesRepository.getControlMethod()
            val dataSource = getDataSource(method)
            // Use the provided subId (which should come from getEffectiveSubscriptionId in callers)
            dataSource.setNetworkMode(subId, mode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset connections for both data sources - useful when switching control methods
     */
    override suspend fun resetConnections() {
        rootDataSource.resetConnection()
        shizukuDataSource.resetConnection()
    }
    
    /**
     * Get the effective subscription ID to use for network operations
     * Returns the user's selected subscription ID from preferences, or the default if -1
     */
    private suspend fun getEffectiveSubscriptionId(): Int {
        val selectedSubId = preferencesRepository.getSelectedSubscriptionId()
        return if (selectedSubId == -1) {
            // User selected "Auto" - use system default
            SubscriptionManager.getDefaultDataSubscriptionId()
        } else {
            // User selected a specific SIM
            selectedSubId
        }
    }

    private fun getDataSource(method: ControlMethod): NetworkControlDataSource {
        return when (method) {
            ControlMethod.ROOT -> rootDataSource
            ControlMethod.SHIZUKU -> shizukuDataSource
        }
    }
}
