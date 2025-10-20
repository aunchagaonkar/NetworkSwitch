package com.supernova.networkswitch.data.repository

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
        val subId = android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
        return dataSource.checkCompatibility(subId)
    }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? {
        return try {
            val method = preferencesRepository.getControlMethod()
            val dataSource = getDataSource(method)
            dataSource.getCurrentNetworkMode(subId)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode): Result<Unit> {
        return try {
            val method = preferencesRepository.getControlMethod()
            val dataSource = getDataSource(method)
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

    override suspend fun requestPermission(method: ControlMethod): Boolean {
        val dataSource = getDataSource(method)
        return dataSource.requestPermission()
    }

    private fun getDataSource(method: ControlMethod): NetworkControlDataSource {
        return when (method) {
            ControlMethod.ROOT -> rootDataSource
            ControlMethod.SHIZUKU -> shizukuDataSource
        }
    }
}
