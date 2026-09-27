package com.supernova.networkswitch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.SimConfigKey
import com.supernova.networkswitch.domain.model.ToggleState
import com.supernova.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.supernova.networkswitch.domain.usecase.GetSimNameUseCase
import com.supernova.networkswitch.domain.usecase.GetEffectiveSubscriptionIdUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleStateUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.UpdateControlMethodUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkCompatibilityUseCase: CheckCompatibilityUseCase,
    private val getToggleStateUseCase: GetToggleStateUseCase,
    private val getEffectiveSubscriptionIdUseCase: GetEffectiveSubscriptionIdUseCase,
    private val toggleNetworkModeUseCase: ToggleNetworkModeUseCase,
    private val updateControlMethodUseCase: UpdateControlMethodUseCase,
    private val getSimNameUseCase: GetSimNameUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    var selectedMethod by mutableStateOf(ControlMethod.SHIZUKU)
        private set
    
    var currentNetworkMode by mutableStateOf<NetworkMode?>(null)
        private set
    
    private var nextMode by mutableStateOf<NetworkMode?>(null)

    val canToggle: Boolean
        get() = nextMode != null
    
    var isLoading by mutableStateOf(false)
        private set
    
    var compatibilityState by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set

    /** The SIM the toggle acts on; null when there is only one SIM to act on. */
    var activeSimName by mutableStateOf<String?>(null)
        private set

    private var refreshJob: Job? = null


    init {
        observeControlMethodPreference()
        refresh()
    }
    
    private fun observeControlMethodPreference() {
        viewModelScope.launch {
            preferencesRepository.observeControlMethod().collectLatest { preferredMethod ->
                if (selectedMethod != preferredMethod) {
                    selectedMethod = preferredMethod
                    refresh()
                }
            }
        }
        
        viewModelScope.launch {
            preferencesRepository.observeSelectedSubscriptionId()
                .mapLatest { SimConfigKey.forSubscription(getEffectiveSubscriptionIdUseCase()) }
                .distinctUntilChanged()
                .flatMapLatest { preferencesRepository.observeToggleModeConfig(it) }
                .drop(1) // init already refreshes
                .collect { refresh() }
        }

        viewModelScope.launch {
            preferencesRepository.observeSelectedSubscriptionId()
                .collectLatest { loadActiveSim() }
        }
    }

    private suspend fun loadActiveSim() {
        activeSimName = getSimNameUseCase(getEffectiveSubscriptionIdUseCase())
    }
    
    /**
     * Sole writer of [compatibilityState]. The mode read is only meaningful once
     * compatibility is confirmed, so both happen in one coroutine and are published
     * together. An in-flight refresh is cancelled, so the newest request wins.
     */
    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            compatibilityState = CompatibilityState.Pending
            val state = checkCompatibilityUseCase()
            val toggle = if (state is CompatibilityState.Compatible) readToggleState() else null
            compatibilityState = state
            showToggleState(toggle)
        }
    }

    private suspend fun readToggleState(): ToggleState? = try {
        getToggleStateUseCase(getEffectiveSubscriptionIdUseCase())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun showToggleState(state: ToggleState?) {
        currentNetworkMode = state?.current
        nextMode = state?.next
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
        viewModelScope.launch { loadActiveSim() }
    }
    
    fun toggleNetworkMode() {
        if (isLoading) return
        
        isLoading = true
        viewModelScope.launch {
            val subId = getEffectiveSubscriptionIdUseCase()
            
            toggleNetworkModeUseCase(subId)
                .onSuccess {
                    // A refresh still running may have read the mode from before the set.
                    if (refreshJob?.isActive == true) refresh() else showToggleState(it)
                }
                .onFailure {
                    refresh()
                }
            
            isLoading = false
        }
    }
    
    fun getToggleButtonText(): String {
        val next = nextMode ?: return "Switch"
        return "Switch to ${next.displayName}"
    }
    
}
