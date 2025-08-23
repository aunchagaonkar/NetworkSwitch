package com.supernova.networkswitch.domain.model

/**
 * Enumeration of control methods for network switching
 */
enum class ControlMethod {
    ROOT,
    SHIZUKU
}

/**
 * Network mode types
 */
enum class NetworkMode {
    FOUR_G_ONLY,
    FIVE_G_ONLY
}

/**
 * Network switch configuration
 */
data class NetworkSwitchConfig(
    val controlMethod: ControlMethod,
    val isEnabled: Boolean,
    val currentMode: NetworkMode
)

/**
 * Compatibility state for network switching
 */
sealed class CompatibilityState {
    object Pending : CompatibilityState()
    object Compatible : CompatibilityState()
    data class Incompatible(val reason: String) : CompatibilityState()
    data class PermissionDenied(val method: ControlMethod) : CompatibilityState()
}
