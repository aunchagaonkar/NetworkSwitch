package com.supernova.networkswitch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.GetEffectiveSubscriptionIdUseCase
import com.supernova.networkswitch.domain.usecase.GetSimNameUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.usecase.UpdateToggleModeConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for network mode configuration screen
 */
@HiltViewModel
class NetworkModeConfigViewModel @Inject constructor(
    private val getToggleModeConfigUseCase: GetToggleModeConfigUseCase,
    private val updateToggleModeConfigUseCase: UpdateToggleModeConfigUseCase,
    private val getEffectiveSubscriptionId: GetEffectiveSubscriptionIdUseCase,
    private val getSimName: GetSimNameUseCase
) : ViewModel() {
    
    private val _currentConfig = MutableStateFlow(
        ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)
    )
    val currentConfig: StateFlow<ToggleModeConfig> = _currentConfig.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _configSaved = MutableStateFlow(false)
    val configSaved: StateFlow<Boolean> = _configSaved.asStateFlow()

    /** The SIM being configured; null when there is only one SIM to configure. */
    private val _simName = MutableStateFlow<String?>(null)
    val simName: StateFlow<String?> = _simName.asStateFlow()

    /**
     * Resolved once, so a save lands on the SIM that was loaded even if the selected SIM
     * changes while the screen is open. Null until loading finishes.
     */
    private var subId: Int? = null
    
    init {
        loadCurrentConfiguration()
    }
    
    private fun loadCurrentConfiguration() {
        viewModelScope.launch {
            try {
                val resolved = getEffectiveSubscriptionId()
                subId = resolved
                _simName.value = getSimName(resolved)
                _currentConfig.value = getToggleModeConfigUseCase(resolved)
            } catch (e: Exception) {
                // Use default configuration if loading fails
                _currentConfig.value = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)
            }
        }
    }
    
    fun updateModeA(mode: NetworkMode) {
        _currentConfig.value = _currentConfig.value.copy(modeA = mode)
    }
    
    fun updateModeB(mode: NetworkMode) {
        _currentConfig.value = _currentConfig.value.copy(modeB = mode)
    }
    
    fun saveConfiguration() {
        if (_currentConfig.value.modeA == _currentConfig.value.modeB) {
            return // Don't save if both modes are the same
        }
        val target = subId ?: return
        
        _isLoading.value = true
        viewModelScope.launch {
            try {
                updateToggleModeConfigUseCase(target, _currentConfig.value)
                _configSaved.value = true
            } catch (e: Exception) {
                // Handle error (could show toast or snackbar)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resetSavedState() {
        _configSaved.value = false
    }
}