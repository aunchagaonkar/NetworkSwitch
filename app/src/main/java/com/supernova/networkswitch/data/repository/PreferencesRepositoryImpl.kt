package com.supernova.networkswitch.data.repository

import com.supernova.networkswitch.data.source.PreferencesDataSource
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : PreferencesRepository {
    
    override suspend fun getControlMethod(): ControlMethod {
        return preferencesDataSource.getControlMethod()
    }
    
    override suspend fun setControlMethod(method: ControlMethod) {
        preferencesDataSource.setControlMethod(method)
    }
    
    override fun observeControlMethod(): Flow<ControlMethod> {
        return preferencesDataSource.observeControlMethod()
    }
    
    override suspend fun getToggleModeConfig(simKey: String): ToggleModeConfig {
        return preferencesDataSource.getToggleModeConfig(simKey)
    }
    
    override suspend fun setToggleModeConfig(simKey: String, config: ToggleModeConfig) {
        preferencesDataSource.setToggleModeConfig(simKey, config)
    }
    
    override fun observeToggleModeConfig(simKey: String): Flow<ToggleModeConfig> {
        return preferencesDataSource.observeToggleModeConfig(simKey)
    }
    
    override suspend fun getLastAppliedMode(simKey: String): NetworkMode? {
        return preferencesDataSource.getLastAppliedMode(simKey)
    }

    override suspend fun setLastAppliedMode(simKey: String, mode: NetworkMode) {
        preferencesDataSource.setLastAppliedMode(simKey, mode)
    }

    override suspend fun getSelectedSubscriptionId(): Int {
        return preferencesDataSource.getSelectedSubscriptionId()
    }
    
    override suspend fun setSelectedSubscriptionId(subscriptionId: Int) {
        preferencesDataSource.setSelectedSubscriptionId(subscriptionId)
    }
    
    override fun observeSelectedSubscriptionId(): Flow<Int> {
        return preferencesDataSource.observeSelectedSubscriptionId()
    }
}
