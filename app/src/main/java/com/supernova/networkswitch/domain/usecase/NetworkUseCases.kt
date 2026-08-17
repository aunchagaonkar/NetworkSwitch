package com.supernova.networkswitch.domain.usecase

import android.telephony.SubscriptionManager
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.model.SimQueryResult
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.repository.SimRepository
import javax.inject.Inject

class CheckCompatibilityUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): CompatibilityState {
        val controlMethod = preferencesRepository.getControlMethod()
        return networkControlRepository.checkCompatibility(controlMethod)
    }
}

class ToggleNetworkModeUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(subId: Int): Result<NetworkMode> {
        return try {
            val toggleConfig = preferencesRepository.getToggleModeConfig()
            
            // Get the next mode to switch to (no current mode detection needed)
            val targetMode = toggleConfig.getNextMode()
            
            // Set the network mode
            val result = networkControlRepository.setNetworkMode(subId, targetMode)
            
            if (result.isSuccess) {
                // Update the toggle state for next time
                val newConfig = toggleConfig.toggle()
                preferencesRepository.setToggleModeConfig(newConfig)
            }
            
            result.map { targetMode }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for getting current network mode
 */
class GetCurrentNetworkModeUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository
) {
    suspend operator fun invoke(subId: Int): Result<NetworkMode?> {
        return try {
            Result.success(networkControlRepository.getCurrentNetworkMode(subId))
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
 * Use case for getting toggle mode configuration
 */
class GetToggleModeConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): ToggleModeConfig {
        return preferencesRepository.getToggleModeConfig()
    }
}

/**
 * Use case for updating toggle mode configuration
 */
class UpdateToggleModeConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(config: ToggleModeConfig) {
        preferencesRepository.setToggleModeConfig(config)
    }
}

/**
 * Use case for getting available SIM cards in the device
 */
class GetAvailableSimsUseCase @Inject constructor(
    private val simRepository: SimRepository
) {
    suspend operator fun invoke(): SimQueryResult {
        return try {
            simRepository.getAvailableSimCards()
        } catch (e: Exception) {
            SimQueryResult.Failed(e)
        }
    }
}

/**
 * Use case for getting the selected subscription ID
 */
class GetSelectedSubscriptionIdUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): Int {
        return preferencesRepository.getSelectedSubscriptionId()
    }
}

/**
 * Use case for setting the selected subscription ID
 */
class SetSelectedSubscriptionIdUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(subscriptionId: Int) {
        preferencesRepository.setSelectedSubscriptionId(subscriptionId)
    }
}

/**
 * Use case for getting the effective subscription ID to use for network operations
 * Returns the user's selected subscription ID, or the default if "Auto" is selected
 * Includes validation to handle edge cases like removed SIM cards
 */
class GetEffectiveSubscriptionIdUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val simRepository: SimRepository
) {
    suspend operator fun invoke(): Int {
        val selectedSubId = preferencesRepository.getSelectedSubscriptionId()

        // If Auto mode (-1), use system default
        if (selectedSubId == -1) {
            return SubscriptionManager.getDefaultDataSubscriptionId()
        }

        val query = try {
            simRepository.getAvailableSimCards()
        } catch (e: Exception) {
            SimQueryResult.Failed(e)
        }

        // Only a successful query proves the SIM is gone. Anything else leaves the
        // selection intact, so a denied permission or a transient failure cannot
        // silently discard it.
        val sims = when (query) {
            is SimQueryResult.Loaded -> query.sims
            SimQueryResult.PermissionDenied, is SimQueryResult.Failed -> return selectedSubId
        }

        return if (sims.any { it.subscriptionId == selectedSubId }) {
            selectedSubId
        } else {
            try {
                preferencesRepository.setSelectedSubscriptionId(-1)
            } catch (e: Exception) {
                // Selection could not be reset; the default is still the safe target.
            }
            SubscriptionManager.getDefaultDataSubscriptionId()
        }
    }
}
