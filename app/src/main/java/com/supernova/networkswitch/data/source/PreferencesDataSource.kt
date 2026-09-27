package com.supernova.networkswitch.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.SubscriptionSelection
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val CONTROL_METHOD_KEY = stringPreferencesKey("control_method")

        private fun modeAKey(simKey: String) = intPreferencesKey("toggle_mode_a_$simKey")
        private fun modeBKey(simKey: String) = intPreferencesKey("toggle_mode_b_$simKey")
        private fun lastAppliedModeKey(simKey: String) = intPreferencesKey("last_applied_mode_$simKey")
        private val SELECTED_SUBSCRIPTION_ID_KEY = intPreferencesKey("selected_subscription_id")
        
        private const val DEFAULT_CONTROL_METHOD = "SHIZUKU"
        
        private val DEFAULT_MODE_A = NetworkMode.LTE_ONLY
        private val DEFAULT_MODE_B = NetworkMode.NR_ONLY
        
        private const val DEFAULT_SUBSCRIPTION_ID = SubscriptionSelection.AUTO
    }
    
    private fun parseControlMethod(methodString: String?): ControlMethod {
        return try {
            ControlMethod.valueOf(methodString ?: DEFAULT_CONTROL_METHOD)
        } catch (e: IllegalArgumentException) {
            ControlMethod.SHIZUKU
        }
    }
    
    suspend fun getControlMethod(): ControlMethod {
        return dataStore.data.map { preferences ->
            parseControlMethod(preferences[CONTROL_METHOD_KEY])
        }.first()
    }
    
    suspend fun setControlMethod(method: ControlMethod) {
        dataStore.edit { preferences ->
            preferences[CONTROL_METHOD_KEY] = method.name
        }
    }
    
    fun observeControlMethod(): Flow<ControlMethod> {
        return dataStore.data.map { preferences ->
            parseControlMethod(preferences[CONTROL_METHOD_KEY])
        }.distinctUntilChanged()
    }
    
    suspend fun getToggleModeConfig(simKey: String): ToggleModeConfig {
        return observeToggleModeConfig(simKey).first()
    }

    suspend fun setToggleModeConfig(simKey: String, config: ToggleModeConfig) {
        dataStore.edit { preferences ->
            preferences[modeAKey(simKey)] = config.modeA.value
            preferences[modeBKey(simKey)] = config.modeB.value
        }
    }

    fun observeToggleModeConfig(simKey: String): Flow<ToggleModeConfig> {
        return dataStore.data.map { readToggleModeConfig(it, simKey) }.distinctUntilChanged()
    }

    private fun readToggleModeConfig(preferences: Preferences, simKey: String): ToggleModeConfig {
        val modeAValue = preferences[modeAKey(simKey)] ?: DEFAULT_MODE_A.value
        val modeBValue = preferences[modeBKey(simKey)] ?: DEFAULT_MODE_B.value

        return ToggleModeConfig(
            NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A,
            NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B
        )
    }

    suspend fun getLastAppliedMode(simKey: String): NetworkMode? {
        val value = dataStore.data.first()[lastAppliedModeKey(simKey)] ?: return null
        return NetworkMode.fromValue(value)
    }

    suspend fun setLastAppliedMode(simKey: String, mode: NetworkMode) {
        dataStore.edit { preferences ->
            preferences[lastAppliedModeKey(simKey)] = mode.value
        }
    }

    
    /**
     * Get the selected subscription ID for the SIM card
     * Returns [SubscriptionSelection.AUTO] if no specific SIM is selected
     */
    suspend fun getSelectedSubscriptionId(): Int {
        return dataStore.data.map { preferences ->
            preferences[SELECTED_SUBSCRIPTION_ID_KEY] ?: DEFAULT_SUBSCRIPTION_ID
        }.first()
    }
    
    /**
     * Set the selected subscription ID for the SIM card
     * Pass [SubscriptionSelection.AUTO] to use the default subscription
     */
    suspend fun setSelectedSubscriptionId(subscriptionId: Int) {
        dataStore.edit { preferences ->
            preferences[SELECTED_SUBSCRIPTION_ID_KEY] = subscriptionId
        }
    }
    
    /**
     * Observe changes to the selected subscription ID
     */
    fun observeSelectedSubscriptionId(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[SELECTED_SUBSCRIPTION_ID_KEY] ?: DEFAULT_SUBSCRIPTION_ID
        }.distinctUntilChanged()
    }
}
