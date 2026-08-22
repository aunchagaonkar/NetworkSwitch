package com.supernova.networkswitch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetEffectiveSubscriptionIdUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.UpdateControlMethodUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkCompatibilityUseCase: CheckCompatibilityUseCase,
    private val getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase,
    private val getEffectiveSubscriptionIdUseCase: GetEffectiveSubscriptionIdUseCase,
    private val toggleNetworkModeUseCase: ToggleNetworkModeUseCase,
    private val updateControlMethodUseCase: UpdateControlMethodUseCase,
    private val getToggleModeConfigUseCase: GetToggleModeConfigUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    // Current control method selection
    var selectedMethod by mutableStateOf(ControlMethod.SHIZUKU)
        private set
    
    // Current network mode
    var currentNetworkMode by mutableStateOf<NetworkMode?>(null)
        private set
    
    // Toggle mode configuration
    var toggleModeConfig by mutableStateOf(ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY))
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    // Compatibility state using domain model
    var compatibilityState by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set

    private var refreshJob: Job? = null

    init {
        observeControlMethodPreference()
        loadToggleModeConfig()
        refresh()
    }
    
    /**
     * Observe control method preference changes
     */
    private fun observeControlMethodPreference() {
        viewModelScope.launch {
            preferencesRepository.observeControlMethod().collectLatest { preferredMethod ->
                if (selectedMethod != preferredMethod) {
                    selectedMethod = preferredMethod
                    loadToggleModeConfig()
                    refresh()
                }
            }
        }
        
        // Also observe toggle mode config changes
        viewModelScope.launch {
            preferencesRepository.observeToggleModeConfig().collectLatest { newConfig ->
                toggleModeConfig = newConfig
            }
        }
    }
    
    /**
     * Re-read compatibility and, if the control method works, the current network mode.
     *
     * Both values are refreshed in one coroutine because the mode read is only
     * meaningful once compatibility is confirmed. Every caller goes through here so the
     * two cannot be updated independently and disagree. A refresh already in flight is
     * cancelled, so the newest request wins.
     */
    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            compatibilityState = CompatibilityState.Pending
            val state = checkCompatibilityUseCase()
            compatibilityState = state
            currentNetworkMode = if (state is CompatibilityState.Compatible) readCurrentMode() else null
        }
    }

    private suspend fun readCurrentMode(): NetworkMode? {
        val subId = getEffectiveSubscriptionIdUseCase()
        return getCurrentNetworkModeUseCase(subId).getOrNull()
    }
    
    /**
     * Switch control method
     */
    fun switchToMethod(method: ControlMethod) {
        viewModelScope.launch {
            updateControlMethodUseCase(method)
        }
    }
    
    /**
     * Retry compatibility check
     */
    fun retryCompatibilityCheck() {
        refresh()
    }
    
    /**
     * Refresh all data when app resumes
     */
    fun refreshAllData() {
        refresh()
    }
    
    /**
     * Load toggle mode configuration
     */
    private fun loadToggleModeConfig() {
        viewModelScope.launch {
            try {
                toggleModeConfig = getToggleModeConfigUseCase()
            } catch (e: Exception) {
                // Use default configuration if loading fails
                toggleModeConfig = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)
            }
        }
    }
    
    /**
     * Toggle network mode using configurable modes
     */
    fun toggleNetworkMode() {
        if (isLoading) return
        
        isLoading = true
        viewModelScope.launch {
            // Use the user's selected subscription ID (or default if "Auto" selected)
            val subId = getEffectiveSubscriptionIdUseCase()
            
            toggleNetworkModeUseCase(subId)
                .onSuccess { newMode ->
                    currentNetworkMode = newMode
                }
                .onFailure {
                    // The mode may or may not have changed; re-read rather than guess.
                    refresh()
                }
            
            isLoading = false
        }
    }
    
    /**
     * Get display text for current network mode and next toggle mode
     */
    fun getToggleButtonText(): String {
        val nextMode = toggleModeConfig.getNextMode()
        return "Switch to ${nextMode.displayName}"
    }
    
}
