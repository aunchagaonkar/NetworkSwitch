package com.supernova.networkswitch.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.model.SimQueryResult
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.repository.SimRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@AndroidEntryPoint
class SimSwitchTileService : TileService() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var simRepository: SimRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listeningJob: Job? = null

    private var simQuery: SimQueryResult = SimQueryResult.Loaded(emptyList())
    private var selectedSubId: Int = -1

    private val availableSims: List<SimInfo>
        get() = (simQuery as? SimQueryResult.Loaded)?.sims ?: emptyList()

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            try {
                combine(
                    simRepository.observeAvailableSimCards(),
                    preferencesRepository.observeSelectedSubscriptionId()
                ) { query, subId -> query to subId }
                    .collect { (query, subId) ->
                        simQuery = query
                        selectedSubId = subId
                        withContext(Dispatchers.Main) {
                            refreshTileDisplay()
                        }
                    }
            } catch (_: CancellationException) {
                // Expected when the tile stops listening
            } catch (e: Exception) {
                Log.e(TAG, "SIM tile: failed to observe SIM state", e)
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

        serviceScope.launch {
            try {
                // A click can land before the observer's first emission, so make sure
                // the SIM set is known rather than treating "not yet loaded" as "none".
                if (availableSims.isEmpty()) {
                    simQuery = simRepository.getAvailableSimCards()
                }

                // Cannot cycle without a known SIM set, or with a single SIM
                if (simQuery !is SimQueryResult.Loaded || availableSims.size <= 1) {
                    withContext(Dispatchers.Main) {
                        refreshTileDisplay()
                    }
                    return@launch
                }

                // Cycle to the next SIM: Auto → SIM 1 → SIM 2 → ... → Auto
                val currentSubId = preferencesRepository.getSelectedSubscriptionId()
                val nextSubId = getNextSubscriptionId(currentSubId)

                preferencesRepository.setSelectedSubscriptionId(nextSubId)
                selectedSubId = nextSubId

                withContext(Dispatchers.Main) {
                    refreshTileDisplay()
                }
            } catch (_: CancellationException) {
                // Expected when the tile stops listening
            } catch (e: Exception) {
                Log.e(TAG, "SIM tile: failed to cycle SIM", e)
            }
        }
    }

    /**
     * Determine the next subscription ID in the cycle.
     * Order: Auto (-1) → SIM 1 → SIM 2 → ... → Auto (-1)
     */
    private fun getNextSubscriptionId(currentSubId: Int): Int {
        if (availableSims.isEmpty()) return -1

        if (currentSubId == -1) {
            // Currently on Auto, switch to first SIM
            return availableSims.first().subscriptionId
        }

        // Find current SIM index
        val currentIndex = availableSims.indexOfFirst { it.subscriptionId == currentSubId }

        return if (currentIndex == -1 || currentIndex >= availableSims.size - 1) {
            // Current SIM not found or is the last one, cycle back to Auto
            -1
        } else {
            // Move to next SIM
            availableSims[currentIndex + 1].subscriptionId
        }
    }

    private fun refreshTileDisplay() {
        try {
            val tile = qsTile ?: return

            if (simQuery is SimQueryResult.PermissionDenied) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "SIM Select"
                tile.subtitle = "Permission needed"
                tile.updateTile()
                return
            }

            if (simQuery is SimQueryResult.Failed) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "SIM Select"
                tile.subtitle = "Unavailable"
                tile.updateTile()
                return
            }

            if (availableSims.size <= 1) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "SIM Select"
                tile.subtitle = if (availableSims.size == 1) {
                    availableSims.first().displayName
                } else {
                    "No SIM detected"
                }
                tile.updateTile()
                return
            }

            // Multiple SIMs available — tile is active
            tile.state = Tile.STATE_ACTIVE

            if (selectedSubId == -1) {
                tile.label = "SIM: Auto"
                tile.subtitle = "System default"
            } else {
                val simInfo = availableSims.find { it.subscriptionId == selectedSubId }
                if (simInfo != null) {
                    tile.label = "SIM: ${simInfo.displayName}"
                    tile.subtitle = "Tap to switch"
                } else {
                    // Selected SIM no longer available
                    tile.label = "SIM: Auto"
                    tile.subtitle = "Previous SIM removed"
                }
            }

            tile.updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "SIM tile: failed to update tile", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "NetworkSwitch"
    }
}
