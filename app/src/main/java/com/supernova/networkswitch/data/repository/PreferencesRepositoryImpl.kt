package com.supernova.networkswitch.data.repository

import com.supernova.networkswitch.data.source.PreferencesDataSource
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
    
    override suspend fun getToggleModeConfig(): ToggleModeConfig {
        return preferencesDataSource.getToggleModeConfig()
    }
    
    override suspend fun setToggleModeConfig(config: ToggleModeConfig) {
        preferencesDataSource.setToggleModeConfig(config)
    }
    
    override fun observeToggleModeConfig(): Flow<ToggleModeConfig> {
        return preferencesDataSource.observeToggleModeConfig()
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
