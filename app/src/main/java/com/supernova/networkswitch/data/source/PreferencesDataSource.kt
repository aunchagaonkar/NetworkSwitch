package com.supernova.networkswitch.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val HIDE_LAUNCHER_ICON_KEY = booleanPreferencesKey("hide_launcher_icon")
        private val CONTROL_METHOD_KEY = stringPreferencesKey("control_method")
        private val TOGGLE_MODE_A_KEY = intPreferencesKey("toggle_mode_a")
        private val TOGGLE_MODE_B_KEY = intPreferencesKey("toggle_mode_b")
        private val TOGGLE_NEXT_IS_B_KEY = booleanPreferencesKey("toggle_next_is_b")

        private const val DEFAULT_HIDE_LAUNCHER_ICON = false
        private const val DEFAULT_CONTROL_METHOD = "SHIZUKU"

        private val DEFAULT_MODE_A = NetworkMode.LTE_ONLY
        private val DEFAULT_MODE_B = NetworkMode.NR_ONLY
        private const val DEFAULT_NEXT_IS_B = true
    }

    suspend fun setHideLauncherIcon(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIDE_LAUNCHER_ICON_KEY] = hide
        }
    }

    fun observeHideLauncherIcon(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[HIDE_LAUNCHER_ICON_KEY] ?: DEFAULT_HIDE_LAUNCHER_ICON
        }
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
        }
    }

    suspend fun getToggleModeConfig(): ToggleModeConfig {
        return dataStore.data.map { preferences ->
            val modeAValue = preferences[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value
            val modeBValue = preferences[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value
            val nextIsB = preferences[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B

            val modeA = NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A
            val modeB = NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B

            ToggleModeConfig(modeA, modeB, nextIsB)
        }.first()
    }

    suspend fun setToggleModeConfig(config: ToggleModeConfig) {
        dataStore.edit { preferences ->
            preferences[TOGGLE_MODE_A_KEY] = config.modeA.value
            preferences[TOGGLE_MODE_B_KEY] = config.modeB.value
            preferences[TOGGLE_NEXT_IS_B_KEY] = config.nextModeIsB
        }
    }

    fun observeToggleModeConfig(): Flow<ToggleModeConfig> {
        return dataStore.data.map { preferences ->
            val modeAValue = preferences[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value
            val modeBValue = preferences[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value
            val nextIsB = preferences[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B

            val modeA = NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A
            val modeB = NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B

            ToggleModeConfig(modeA, modeB, nextIsB)
        }
    }
}
