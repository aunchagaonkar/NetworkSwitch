package com.supernova.networkswitch.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.SimConfigKey
import com.supernova.networkswitch.domain.model.ToggleState
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.GetEffectiveSubscriptionIdUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleStateUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@AndroidEntryPoint
class NetworkTileService : TileService() {

    @Inject
    lateinit var getToggleStateUseCase: GetToggleStateUseCase

    @Inject
    lateinit var getEffectiveSubscriptionIdUseCase: GetEffectiveSubscriptionIdUseCase
    
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
    private var listeningJob: Job? = null

    private var toggleState: ToggleState? = null

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            try {
                preferencesRepository.observeSelectedSubscriptionId()
                    .mapLatest { SimConfigKey.forSubscription(getEffectiveSubscriptionIdUseCase()) }
                    .distinctUntilChanged()
                    .flatMapLatest { preferencesRepository.observeToggleModeConfig(it) }
                    .collect { refreshNetworkState() }
            } catch (_: CancellationException) {
                // Expected when the tile stops listening
            } catch (e: Exception) {
                Log.e(TAG, "Tile: Failed to observe toggle config", e)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
        listeningJob = null
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
        serviceScope.launch {
            try {
                // The binder call into the root/Shizuku process can hang indefinitely.
                withTimeout(TOGGLE_TIMEOUT_MS) {
                    val subId = getEffectiveSubscriptionIdUseCase()
                    toggleNetworkModeUseCase(subId)
                        .onSuccess { toggleState = it }
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
        try {
            toggleState = getToggleStateUseCase(getEffectiveSubscriptionIdUseCase())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tile: Error refreshing network state", e)
        }

        withContext(Dispatchers.Main) { updateTileState() }
    }

    private fun updateTileState() {
        try {
            // Null outside the listening window; the next onStartListening repaints.
            val tile = qsTile ?: return
            val state = toggleState

            if (state != null) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = state.current?.displayName ?: getString(R.string.tile_not_loaded)
                tile.subtitle = getString(R.string.tile_next_mode, state.next.displayName)
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
