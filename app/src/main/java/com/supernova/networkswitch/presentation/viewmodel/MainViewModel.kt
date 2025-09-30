package com.supernova.networkswitch.presentation.viewmodel

import android.telephony.SubscriptionManager
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
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.UpdateControlMethodUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkCompatibilityUseCase: CheckCompatibilityUseCase,
    private val getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase,
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

    init {
        observeControlMethodPreference()
        loadToggleModeConfig()
        checkCompatibility()
        refreshNetworkState()
    }
    
    /**
     * Observe control method preference changes
     */
    private fun observeControlMethodPreference() {
        viewModelScope.launch {
            preferencesRepository.observeControlMethod().collectLatest { preferredMethod ->
                if (selectedMethod != preferredMethod) {
                    selectedMethod = preferredMethod
                    checkCompatibility()
                    loadToggleModeConfig()
                    refreshNetworkState()
                }
            }
        }
        
        // Also observe toggle mode config changes
        viewModelScope.launch {
            preferencesRepository.observeToggleModeConfig().collectLatest { newConfig ->
                toggleModeConfig = newConfig
                // Force UI update when config changes
                refreshNetworkState()
            }
        }
    }
    
    /**
     * Check compatibility using domain use case
     */
    private fun checkCompatibility() {
        viewModelScope.launch {
            compatibilityState = CompatibilityState.Pending
            compatibilityState = checkCompatibilityUseCase()
        }
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
        checkCompatibility()
    }
    
    /**
     * Refresh all data when app resumes
     */
    fun refreshAllData() {
        checkCompatibility()
        refreshNetworkState()
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
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            
            toggleNetworkModeUseCase(subId)
                .onSuccess { newMode ->
                    currentNetworkMode = newMode
                }
                .onFailure {
                    // On failure, refresh state to get current status
                    refreshNetworkState()
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
    
    /**
     * Refresh current network state
     */
    private fun refreshNetworkState() {
        viewModelScope.launch {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            
            getCurrentNetworkModeUseCase(subId)
                .onSuccess { mode ->
                    currentNetworkMode = mode
                }
                .onFailure {
                    currentNetworkMode = null
                }
        }
    }
}
