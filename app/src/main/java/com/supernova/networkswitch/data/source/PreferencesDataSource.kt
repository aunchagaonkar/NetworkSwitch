package com.supernova.networkswitch.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.supernova.networkswitch.domain.model.ControlMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-based preferences data source
 * Modern replacement for SharedPreferences-based SettingsManager
 */
@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val CONTROL_METHOD_KEY = stringPreferencesKey("control_method")
        private const val DEFAULT_CONTROL_METHOD = "SHIZUKU"
    }
    
    /**
     * Get preferred control method
     */
    suspend fun getControlMethod(): ControlMethod {
        return dataStore.data.map { preferences ->
            val methodString = preferences[CONTROL_METHOD_KEY] ?: DEFAULT_CONTROL_METHOD
            try {
                ControlMethod.valueOf(methodString)
            } catch (e: IllegalArgumentException) {
                ControlMethod.SHIZUKU // Fallback to SHIZUKU if invalid value
            }
        }.first() // Use first() to get the current value without hanging
    }
    
    /**
     * Set preferred control method
     */
    suspend fun setControlMethod(method: ControlMethod) {
        dataStore.edit { preferences ->
            preferences[CONTROL_METHOD_KEY] = method.name
        }
    }
    
    /**
     * Observe control method changes
     */
    fun observeControlMethod(): Flow<ControlMethod> {
        return dataStore.data.map { preferences ->
            val methodString = preferences[CONTROL_METHOD_KEY] ?: DEFAULT_CONTROL_METHOD
            try {
                ControlMethod.valueOf(methodString)
            } catch (e: IllegalArgumentException) {
                ControlMethod.SHIZUKU // Fallback to SHIZUKU if invalid value
            }
        }
    }
}
