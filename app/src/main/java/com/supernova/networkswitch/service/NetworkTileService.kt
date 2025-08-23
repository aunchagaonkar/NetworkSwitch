package com.supernova.networkswitch.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.usecase.GetNetworkStateUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class NetworkTileService : TileService() {
    
    @Inject
    lateinit var getNetworkStateUseCase: GetNetworkStateUseCase
    
    @Inject
    lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var preferredMethod = ControlMethod.SHIZUKU
    private var currentNetworkState = false

    override fun onStartListening() {
        super.onStartListening()
        // Load preferred method and initial state
        serviceScope.launch {
            try {
                preferredMethod = preferencesRepository.getControlMethod()
                refreshNetworkState()
            } catch (e: Exception) {
                // Silently handle errors during initialization
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        // Clean up any ongoing operations when tile becomes inactive
    }

    override fun onClick() {
        super.onClick()
        
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        
        serviceScope.launch {
            try {
                // Update preferred method from settings in case it changed
                preferredMethod = preferencesRepository.getControlMethod()
                
                // Toggle network mode using use case
                toggleNetworkModeUseCase(subId)
                    .onSuccess {
                        // Refresh state to show current status for user feedback
                        refreshNetworkState()
                    }
                    .onFailure {
                        // On failure, still try to refresh state
                        refreshNetworkState()
                    }
            } catch (e: Exception) {
                // Silently handle any errors during toggle operation
                try {
                    refreshNetworkState()
                } catch (refreshException: Exception) {
                    // Silently handle refresh errors too
                }
            }
        }
    }

    private fun refreshNetworkState() {
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        
        serviceScope.launch {
            try {
                getNetworkStateUseCase(subId)
                    .onSuccess { networkState ->
                        currentNetworkState = networkState
                        withContext(Dispatchers.Main) {
                            updateTile(currentNetworkState)
                        }
                    }
                    .onFailure {
                        // Silently handle any errors during state refresh
                    }
            } catch (e: Exception) {
                // Silently handle any errors during state refresh
            }
        }
    }
    
    private fun updateTile(is5gEnabled: Boolean) {
        try {
            qsTile?.apply {
                state = if (is5gEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = if (is5gEnabled) "5G Mode" else "4G Mode"
                subtitle = if (is5gEnabled) "NR Only" else "LTE Only"
                updateTile()
            }
        } catch (e: Exception) {
            // Silently handle any tile update errors
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            serviceScope.cancel()
        } catch (e: Exception) {
            // Silently handle any cleanup errors
        }
    }
}
