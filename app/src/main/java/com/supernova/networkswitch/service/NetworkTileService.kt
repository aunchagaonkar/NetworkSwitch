package com.supernova.networkswitch.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class NetworkTileService : TileService() {
    
    @Inject
    lateinit var getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase
    
    @Inject
    lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase
    
    @Inject
    lateinit var getToggleModeConfigUseCase: GetToggleModeConfigUseCase
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var currentNetworkMode: NetworkMode? = null
    private var toggleConfig: ToggleModeConfig? = null

    companion object {
        private val _isTileAdded = MutableStateFlow(false)
        val isTileAdded: StateFlow<Boolean> = _isTileAdded.asStateFlow()
    }

    override fun onStartListening() {
        super.onStartListening()
        _isTileAdded.value = true
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
        _isTileAdded.value = false
        // Clean up any ongoing operations when tile becomes inactive
    }

    override fun onClick() {
        super.onClick()
        
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        
        serviceScope.launch {
            try {
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
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        
        try {
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
                    subtitle = nextMode.displayName
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
        _isTileAdded.value = false
        serviceScope.cancel()
    }
}
