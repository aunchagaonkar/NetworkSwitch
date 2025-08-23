package com.supernova.networkswitch.presentation.viewmodel

import android.telephony.SubscriptionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.supernova.networkswitch.domain.usecase.GetNetworkStateUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.UpdateControlMethodUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * MainViewModel with clean architecture using domain models and use cases
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkCompatibilityUseCase: CheckCompatibilityUseCase,
    private val getNetworkStateUseCase: GetNetworkStateUseCase,
    private val toggleNetworkModeUseCase: ToggleNetworkModeUseCase,
    private val updateControlMethodUseCase: UpdateControlMethodUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    // Current control method selection
    var selectedMethod by mutableStateOf(ControlMethod.SHIZUKU)
        private set
    
    // Network state - current 5G enabled status
    var networkState by mutableStateOf(false)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    // Compatibility state using domain model
    var compatibilityState by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set

    init {
        observeControlMethodPreference()
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
                    refreshNetworkState()
                }
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
     * Toggle network mode using domain use case
     */
    fun toggleNetworkMode() {
        if (isLoading) return
        
        isLoading = true
        viewModelScope.launch {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            
            toggleNetworkModeUseCase(subId)
                .onSuccess { newState ->
                    networkState = newState
                }
                .onFailure {
                    // On failure, refresh state to get current status
                    refreshNetworkState()
                }
            
            isLoading = false
        }
    }
    
    /**
     * Refresh current network state
     */
    private fun refreshNetworkState() {
        viewModelScope.launch {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            
            getNetworkStateUseCase(subId)
                .onSuccess { state ->
                    networkState = state
                }
                .onFailure {
                    networkState = false
                }
        }
    }
}
