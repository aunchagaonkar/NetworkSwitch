package com.supernova.networkswitch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
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
    private val networkControlRepository: NetworkControlRepository
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
    
    init {
        checkAllCompatibility()
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
}
