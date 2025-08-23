package com.supernova.networkswitch.data.repository

import com.supernova.networkswitch.data.source.NetworkControlDataSource
import com.supernova.networkswitch.data.source.RootNetworkControlDataSource
import com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    
    private val _connectionState = MutableStateFlow(false)
    
    override suspend fun checkCompatibility(method: ControlMethod): CompatibilityState {
        val dataSource = getDataSource(method)
        val subId = android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
        return dataSource.checkCompatibility(subId)
    }

    override suspend fun getFivegEnabled(subId: Int): Boolean {
        val method = preferencesRepository.getControlMethod()
        val dataSource = getDataSource(method)
        return dataSource.getFivegEnabled(subId)
    }

    override suspend fun setFivegEnabled(subId: Int, enabled: Boolean): Result<Unit> {
        return try {
            val method = preferencesRepository.getControlMethod()
            val dataSource = getDataSource(method)
            dataSource.setFivegEnabled(subId, enabled)
            _connectionState.value = dataSource.isConnected()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeConnectionState(): Flow<Boolean> = _connectionState.asStateFlow()
    
    /**
     * Reset connections for both data sources - useful when switching control methods
     */
    override suspend fun resetConnections() {
        rootDataSource.resetConnection()
        shizukuDataSource.resetConnection()
        _connectionState.value = false
    }

    private fun getDataSource(method: ControlMethod): NetworkControlDataSource {
        return when (method) {
            ControlMethod.ROOT -> rootDataSource
            ControlMethod.SHIZUKU -> shizukuDataSource
        }
    }
}
