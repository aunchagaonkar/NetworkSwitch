package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import javax.inject.Inject

/**
 * Use case for checking network control compatibility
 */
class CheckCompatibilityUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): CompatibilityState {
        val controlMethod = preferencesRepository.getControlMethod()
        return networkControlRepository.checkCompatibility(controlMethod)
    }
}

/**
 * Use case for toggling network mode (4G/5G)
 */
class ToggleNetworkModeUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository
) {
    suspend operator fun invoke(subId: Int): Result<Boolean> {
        return try {
            val currentState = networkControlRepository.getFivegEnabled(subId)
            val newState = !currentState
            networkControlRepository.setFivegEnabled(subId, newState)
                .map { newState }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for getting current network state
 */
class GetNetworkStateUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository
) {
    suspend operator fun invoke(subId: Int): Result<Boolean> {
        return try {
            Result.success(networkControlRepository.getFivegEnabled(subId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for updating control method preference
 */
class UpdateControlMethodUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(method: ControlMethod) {
        preferencesRepository.setControlMethod(method)
    }
}

/**
 * Use case for resetting network connections when switching methods
 */
class ResetConnectionsUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository
) {
    suspend operator fun invoke() {
        networkControlRepository.resetConnections()
    }
}
