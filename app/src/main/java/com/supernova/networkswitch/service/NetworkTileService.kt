package com.supernova.networkswitch.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
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
        android.util.Log.d("NetworkSwitch", "Tile: onClick triggered")
        android.widget.Toast.makeText(this, "Switching network...", android.widget.Toast.LENGTH_SHORT).show()
        
        // Provide immediate visual feedback
        qsTile?.let {
            it.state = Tile.STATE_UNAVAILABLE
            it.subtitle = "Processing..."
            it.updateTile()
        }
        
        if (isLocked) {
            android.util.Log.d("NetworkSwitch", "Tile: Device is locked, unlocking and running...")
            unlockAndRun {
                performToggle()
            }
        } else {
            performToggle()
        }
    }

    private fun performToggle() {
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        android.util.Log.d("NetworkSwitch", "Tile: performToggle for subId: $subId")
        
        // Use a more persistent scope to ensure work completes even if service is destroyed
        CoroutineScope(ProcessLifecycleOwner.get().lifecycleScope.coroutineContext + Dispatchers.IO).launch {
            try {
                toggleNetworkModeUseCase(subId)
                    .onSuccess { newMode ->
                        android.util.Log.d("NetworkSwitch", "Tile: Toggle success, new mode: ${newMode.displayName}")
                        currentNetworkMode = newMode
                        withContext(Dispatchers.Main) {
                            updateTileState()
                        }
                    }
                    .onFailure { e ->
                        android.util.Log.e("NetworkSwitch", "Tile: Toggle failed", e)
                        refreshNetworkState()
                    }
            } catch (e: Exception) {
                android.util.Log.e("NetworkSwitch", "Tile: Error in performToggle", e)
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
                        updateTileState()
                    }
                }
        } catch (_: Exception) {
            // Handle errors silently
        }
    }
    
    private fun updateTileState() {
        try {
            val tile = qsTile ?: return
            val config = toggleConfig
            
            if (config != null) {
                tile.state = Tile.STATE_ACTIVE
                
                // Show current mode as label and next mode as subtitle
                val currentMode = currentNetworkMode ?: config.getCurrentMode()
                tile.label = currentMode.displayName
                tile.subtitle = "Next: ${config.getNextMode().displayName}"
                
                // Update icon based on mode if needed
                // tile.icon = Icon.createWithResource(this, R.drawable.ic_...)
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Network Switch"
                tile.subtitle = "Tap to load"
            }
            tile.updateTile()
            android.util.Log.d("NetworkSwitch", "Tile: updateTileState finished")
        } catch (e: Exception) {
            android.util.Log.e("NetworkSwitch", "Tile: Failed to update tile state", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
