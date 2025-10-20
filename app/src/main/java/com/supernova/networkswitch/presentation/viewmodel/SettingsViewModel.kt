package com.supernova.networkswitch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.GetAvailableSimsUseCase
import com.supernova.networkswitch.domain.usecase.GetSelectedSubscriptionIdUseCase
import com.supernova.networkswitch.domain.usecase.SetSelectedSubscriptionIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import javax.inject.Inject

/**
 * Settings screen ViewModel using clean architecture
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkControlRepository: NetworkControlRepository,
    private val getAvailableSimsUseCase: GetAvailableSimsUseCase,
    private val getSelectedSubscriptionIdUseCase: GetSelectedSubscriptionIdUseCase,
    private val setSelectedSubscriptionIdUseCase: SetSelectedSubscriptionIdUseCase
) : ViewModel() {
    
    val controlMethod: StateFlow<ControlMethod> = preferencesRepository.observeControlMethod()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = ControlMethod.SHIZUKU
        )
    
    // Compatibility status for each method
    var rootCompatibility by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set
    
    var shizukuCompatibility by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set
    
    // Available SIM cards in the device
    private val _availableSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val availableSims: StateFlow<List<SimInfo>> = _availableSims.asStateFlow()
    
    // Currently selected subscription ID
    val selectedSubscriptionId: StateFlow<Int> = preferencesRepository.observeSelectedSubscriptionId()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = -1 // Default: use system default
        )
    
    // Loading state for SIM detection
    private val _isLoadingSims = MutableStateFlow(false)
    val isLoadingSims: StateFlow<Boolean> = _isLoadingSims.asStateFlow()
    
    // Error state for SIM operations
    private val _simError = MutableStateFlow<String?>(null)
    val simError: StateFlow<String?> = _simError.asStateFlow()
    
    init {
        checkAllCompatibility()
        loadAvailableSims()
        observeSelectedSimValidity()
    }
    
    fun updateControlMethod(method: ControlMethod) {
        viewModelScope.launch {
            preferencesRepository.setControlMethod(method)
        }
    }
    
    fun retryCompatibilityCheck() {
        checkAllCompatibility()
    }
    
    private fun checkAllCompatibility() {
        viewModelScope.launch {
            rootCompatibility = CompatibilityState.Pending
            shizukuCompatibility = CompatibilityState.Pending
            
            // Check both methods in parallel
            val rootResult = async { networkControlRepository.checkCompatibility(ControlMethod.ROOT) }
            val shizukuResult = async { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
            
            rootCompatibility = rootResult.await()
            shizukuCompatibility = shizukuResult.await()
        }
    }
    
    /**
     * Load all available SIM cards in the device
     */
    private fun loadAvailableSims() {
        viewModelScope.launch {
            _isLoadingSims.value = true
            _simError.value = null
            try {
                val result = getAvailableSimsUseCase()
                if (result.isSuccess) {
                    val sims = result.getOrNull() ?: emptyList()
                    _availableSims.value = sims
                    
                    // Check if previously selected SIM is still available
                    validateSelectedSim(sims)
                } else {
                    _availableSims.value = emptyList()
                    _simError.value = "Failed to detect SIM cards"
                }
            } catch (e: Exception) {
                _availableSims.value = emptyList()
                _simError.value = "Error: ${e.message}"
            } finally {
                _isLoadingSims.value = false
            }
        }
    }
    
    /**
     * Validate that the currently selected SIM is still available
     * Reset to Auto if the selected SIM was removed
     */
    private suspend fun validateSelectedSim(availableSims: List<SimInfo>) {
        val currentSelection = selectedSubscriptionId.value
        
        // Skip validation if Auto mode (-1) or if no selection
        if (currentSelection == -1) return
        
        // Check if the selected SIM is still in the available list
        val isStillAvailable = availableSims.any { it.subscriptionId == currentSelection }
        
        if (!isStillAvailable) {
            // Selected SIM was removed, reset to Auto mode
            try {
                setSelectedSubscriptionIdUseCase(-1)
                _simError.value = "Previously selected SIM was removed. Switched to Auto mode."
            } catch (e: Exception) {
                _simError.value = "Failed to update SIM selection"
            }
        }
    }
    
    /**
     * Observe selected SIM validity and auto-correct if removed
     */
    // Mutex to prevent redundant validation calls in a thread-safe way
    private val simValidationMutex = kotlinx.coroutines.sync.Mutex()

    private fun observeSelectedSimValidity() {
        viewModelScope.launch {
            selectedSubscriptionId.collectLatest { subId ->
                if (subId != -1 && _availableSims.value.isNotEmpty()) {
                    val isValid = _availableSims.value.any { it.subscriptionId == subId }
                    if (!isValid) {
                        simValidationMutex.lock()
                        try {
                            validateSelectedSim(_availableSims.value)
                        } finally {
                            simValidationMutex.unlock()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Refresh the list of available SIM cards
     * Useful when permission is granted or SIM cards are changed
     */
    fun refreshAvailableSims() {
        loadAvailableSims()
    }
    
    /**
     * Select a specific SIM card for network operations
     * @param subscriptionId The subscription ID of the SIM to select, or -1 for default
     */
    fun selectSim(subscriptionId: Int) {
        viewModelScope.launch {
            try {
                // Validate the selection before saving
                if (subscriptionId != -1) {
                    val isValid = _availableSims.value.any { it.subscriptionId == subscriptionId }
                    if (!isValid) {
                        _simError.value = "Selected SIM is not available"
                        return@launch
                    }
                }
                
                setSelectedSubscriptionIdUseCase(subscriptionId)
                _simError.value = null // Clear any previous errors
            } catch (e: Exception) {
                _simError.value = "Failed to save SIM selection: ${e.message}"
            }
        }
    }
    
    /**
     * Clear any SIM-related error messages
     */
    fun clearSimError() {
        _simError.value = null
    }
    
    /**
     * Get the currently selected SIM info object
     * Returns null if no specific SIM is selected or if the selected SIM is not available
     */
    fun getSelectedSimInfo(): SimInfo? {
        val currentSubscriptionId = selectedSubscriptionId.value
        if (currentSubscriptionId == -1) {
            return null // No specific SIM selected
        }
        return _availableSims.value.find { it.subscriptionId == currentSubscriptionId }
    }
}
