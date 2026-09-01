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
    
    var selectedMethod by mutableStateOf(ControlMethod.SHIZUKU)
        private set
    
    var currentNetworkMode by mutableStateOf<NetworkMode?>(null)
        private set
    
    var toggleModeConfig by mutableStateOf(ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY))
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var compatibilityState by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set

    private var refreshJob: Job? = null

    init {
        observeControlMethodPreference()
        loadToggleModeConfig()
        refresh()
    }
    
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
        
        viewModelScope.launch {
            preferencesRepository.observeToggleModeConfig().collectLatest { newConfig ->
                toggleModeConfig = newConfig
            }
        }
    }
    
    /**
     * Sole writer of [compatibilityState] and [currentNetworkMode].
     *
     * The mode read is only meaningful once compatibility is confirmed, so both happen
     * in one coroutine; routing every caller through here keeps the two from disagreeing.
     * An in-flight refresh is cancelled, so the newest request wins.
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
    
    fun switchToMethod(method: ControlMethod) {
        viewModelScope.launch {
            updateControlMethodUseCase(method)
        }
    }
    
    fun retryCompatibilityCheck() {
        refresh()
    }
    
    /** Called on resume; the activity has no other signal that state may be stale. */
    fun refreshAllData() {
        refresh()
    }
    
    private fun loadToggleModeConfig() {
        viewModelScope.launch {
            try {
                toggleModeConfig = getToggleModeConfigUseCase()
            } catch (e: Exception) {
                toggleModeConfig = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)
            }
        }
    }
    
    fun toggleNetworkMode() {
        if (isLoading) return
        
        isLoading = true
        viewModelScope.launch {
            val subId = getEffectiveSubscriptionIdUseCase()
            
            toggleNetworkModeUseCase(subId)
                .onSuccess { newMode ->
                    currentNetworkMode = newMode
                }
                .onFailure {
                    refresh()
                }
            
            isLoading = false
        }
    }
    
    fun getToggleButtonText(): String {
        val nextMode = toggleModeConfig.nextAfter(currentNetworkMode)
        return "Switch to ${nextMode.displayName}"
    }
    
}
