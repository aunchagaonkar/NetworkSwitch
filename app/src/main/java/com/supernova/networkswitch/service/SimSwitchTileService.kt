package com.supernova.networkswitch.service

import android.Manifest
import android.content.pm.PackageManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.supernova.networkswitch.domain.model.SimInfo
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

    private var availableSims: List<SimInfo> = emptyList()
    private var selectedSubId: Int = -1

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            try {
                // Load available SIMs first
                availableSims = try {
                    simRepository.getAvailableSimCards()
                } catch (_: Exception) {
                    emptyList()
                }

                // Then observe the selected subscription ID and update tile
                preferencesRepository.observeSelectedSubscriptionId().collect { subId ->
                    selectedSubId = subId
                    withContext(Dispatchers.Main) {
                        refreshTileDisplay()
                    }
                }
            } catch (_: CancellationException) {
                // Expected when job is cancelled
            } catch (_: Exception) {
                // Handle errors silently
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
                // Check permission
                if (!hasReadPhoneStatePermission()) {
                    withContext(Dispatchers.Main) {
                        refreshTileDisplay()
                    }
                    return@launch
                }

                // Refresh available SIMs in case they changed
                availableSims = try {
                    simRepository.getAvailableSimCards()
                } catch (_: Exception) {
                    emptyList()
                }

                // Single SIM or no SIMs — nothing to cycle
                if (availableSims.size <= 1) {
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
                // Expected when job is cancelled
            } catch (_: Exception) {
                // Handle errors silently
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

            if (!hasReadPhoneStatePermission()) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "SIM Select"
                tile.subtitle = "Permission needed"
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
        } catch (_: Exception) {
            // Handle tile update errors silently
        }
    }

    private fun hasReadPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
