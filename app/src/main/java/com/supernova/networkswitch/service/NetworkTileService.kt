package com.supernova.networkswitch.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import android.util.Log
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class NetworkTileService : TileService() {

    @Inject
    lateinit var getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase

    @Inject
    lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    /**
     * Scoped to the listening window: `qsTile` is only non-null between
     * onStartListening and onStopListening, so work outliving the service cannot
     * update the tile.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentNetworkMode: NetworkMode? = null
    private var toggleConfig: ToggleModeConfig? = null

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            try {
                preferencesRepository.observeToggleModeConfig().collect { newConfig ->
                    toggleConfig = newConfig
                    refreshNetworkState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tile: Failed to observe toggle config", e)
            }
        }
    }

    override fun onClick() {
        super.onClick()

        // Signal progress in the subtitle only; changing state would make the tile
        // unclickable until the toggle returns.
        qsTile?.let { tile ->
            tile.subtitle = getString(R.string.tile_switching)
            tile.updateTile()
        }

        if (isLocked) {
            unlockAndRun { performToggle() }
        } else {
            performToggle()
        }
    }

    private fun performToggle() {
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()

        serviceScope.launch {
            try {
                // The binder call into the root/Shizuku process can hang indefinitely.
                withTimeout(TOGGLE_TIMEOUT_MS) {
                    toggleNetworkModeUseCase(subId)
                        .onSuccess { newMode -> currentNetworkMode = newMode }
                        .onFailure { e -> Log.e(TAG, "Tile: Toggle failed", e) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tile: Error toggling network mode", e)
            } finally {
                refreshNetworkState()
            }
        }
    }

    private suspend fun refreshNetworkState() {
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()

        try {
            getCurrentNetworkModeUseCase(subId)
                .onSuccess { networkMode -> currentNetworkMode = networkMode }
                .onFailure { e -> Log.e(TAG, "Tile: Failed to read network mode", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Tile: Error refreshing network state", e)
        }

        withContext(Dispatchers.Main) { updateTileState() }
    }

    private fun updateTileState() {
        try {
            // Null outside the listening window; the next onStartListening repaints.
            val tile = qsTile ?: return
            val config = toggleConfig

            if (config != null) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = (currentNetworkMode ?: config.getCurrentMode()).displayName
                tile.subtitle = getString(
                    R.string.tile_next_mode,
                    config.getNextMode().displayName,
                )
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.network_switch)
                tile.subtitle = getString(R.string.tile_not_loaded)
            }
            tile.updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "Tile: Failed to update tile state", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "NetworkSwitch"
        const val TOGGLE_TIMEOUT_MS = 10_000L
    }
}
