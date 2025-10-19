package com.supernova.networkswitch.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetEffectiveSubscriptionIdUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class NetworkTileService : TileService() {
    
    @Inject
    lateinit var getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase
    
    @Inject
    lateinit var getEffectiveSubscriptionIdUseCase: GetEffectiveSubscriptionIdUseCase
    
    @Inject
    lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase
    
    @Inject
    lateinit var getToggleModeConfigUseCase: GetToggleModeConfigUseCase
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var currentNetworkMode: NetworkMode? = null
    private var toggleConfig: ToggleModeConfig? = null

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            try {
                // Observe toggle configuration changes
                preferencesRepository.observeToggleModeConfig().collect { newConfig ->
                    toggleConfig = newConfig
                    refreshNetworkState()
                }
            } catch (_: Exception) {
                // Handle errors silently
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        // Clean up any ongoing operations when tile becomes inactive
    }

    override fun onClick() {
        super.onClick()
        
        serviceScope.launch {
            try {
                // Use the user's selected subscription ID (or default if "Auto" selected)
                val subId = getEffectiveSubscriptionIdUseCase()
                
                toggleNetworkModeUseCase(subId)
                    .onSuccess { newMode ->
                        currentNetworkMode = newMode
                        withContext(Dispatchers.Main) {
                            updateTile()
                        }
                    }
                    .onFailure {
                        refreshNetworkState()
                    }
            } catch (_: Exception) {
                refreshNetworkState()
            }
        }
    }

    private suspend fun refreshNetworkState() {
        try {
            // Use the user's selected subscription ID (or default if "Auto" selected)
            val subId = getEffectiveSubscriptionIdUseCase()
            
            getCurrentNetworkModeUseCase(subId)
                .onSuccess { networkMode ->
                    currentNetworkMode = networkMode
                    withContext(Dispatchers.Main) {
                        updateTile()
                    }
                }
        } catch (_: Exception) {
            // Handle errors silently
        }
    }
    
    private fun updateTile() {
        try {
            qsTile?.apply {
                val config = toggleConfig
                
                if (config != null) {
                    state = Tile.STATE_ACTIVE
                    
                    // Show current and next modes
                    val currentMode = config.getCurrentMode()
                    val nextMode = config.getNextMode()
                    label = currentMode.displayName
                    subtitle = "${nextMode.displayName}"
                } else {
                    state = Tile.STATE_INACTIVE
                    label = "Network Mode"
                    subtitle = "Config not loaded"
                }
                updateTile()
            }
        } catch (_: Exception) {
            // Handle tile update errors silently
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
