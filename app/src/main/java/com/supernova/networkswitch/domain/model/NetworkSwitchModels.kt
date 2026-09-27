package com.supernova.networkswitch.domain.model

enum class ControlMethod {
    ROOT,
    SHIZUKU
}

enum class NetworkMode(val displayName: String, val value: Int) {
    // Basic modes
    GSM_ONLY("2G Only (GSM)", 1),
    WCDMA_ONLY("3G Only (WCDMA)", 2),
    LTE_ONLY("4G Only (LTE)", 11),
    NR_ONLY("5G Only (NR)", 23),
    
    // Preferred modes  
    WCDMA_PREF("2G/3G (3G Preferred)", 0),
    GSM_UMTS("2G/3G (Auto)", 3),
    
    // Combined modes
    LTE_GSM_WCDMA("2G/3G/4G (LTE/GSM/WCDMA)", 9),
    LTE_WCDMA("3G/4G (LTE/WCDMA)", 12),
    NR_LTE("4G/5G (NR/LTE)", 24),
    NR_LTE_GSM_WCDMA("2G/3G/4G/5G (NR/LTE/GSM/WCDMA)", 26),
    NR_LTE_WCDMA("3G/4G/5G (NR/LTE/WCDMA)", 28),
    
    // CDMA modes for US carriers
    CDMA("CDMA (Auto)", 4),
    CDMA_NO_EVDO("CDMA Only", 5),
    EVDO_NO_CDMA("EvDo Only", 6),
    LTE_CDMA_EVDO("CDMA/4G (LTE/CDMA/EvDo)", 8),
    NR_LTE_CDMA_EVDO("CDMA/4G/5G (NR/LTE/CDMA/EvDo)", 25),
    
    // Global modes
    GLOBAL("Global (All)", 7),
    LTE_CDMA_EVDO_GSM_WCDMA("Global 4G (LTE/CDMA/EvDo/GSM/WCDMA)", 10),
    NR_LTE_CDMA_EVDO_GSM_WCDMA("Global 5G (NR/LTE/CDMA/EvDo/GSM/WCDMA)", 27),
    
    // TD-SCDMA modes for China
    TDSCDMA_ONLY("TD-SCDMA Only", 13),
    TDSCDMA_WCDMA("3G (TD-SCDMA/WCDMA)", 14),
    LTE_TDSCDMA("4G (LTE/TD-SCDMA)", 15),
    TDSCDMA_GSM("2G/TD-SCDMA", 16),
    LTE_TDSCDMA_GSM("2G/4G (LTE/TD-SCDMA/GSM)", 17),
    TDSCDMA_GSM_WCDMA("2G/3G (TD-SCDMA/GSM/WCDMA)", 18),
    LTE_TDSCDMA_WCDMA("3G/4G (LTE/TD-SCDMA/WCDMA)", 19),
    LTE_TDSCDMA_GSM_WCDMA("2G/3G/4G (LTE/TD-SCDMA/GSM/WCDMA)", 20),
    TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 3G (TD-SCDMA/CDMA/EvDo/GSM/WCDMA)", 21),
    LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 4G (LTE/TD-SCDMA/CDMA/EvDo/GSM/WCDMA)", 22),
    NR_LTE_TDSCDMA("4G/5G (NR/LTE/TD-SCDMA)", 29),
    NR_LTE_TDSCDMA_GSM("2G/4G/5G (NR/LTE/TD-SCDMA/GSM)", 30),
    NR_LTE_TDSCDMA_WCDMA("3G/4G/5G (NR/LTE/TD-SCDMA/WCDMA)", 31),
    NR_LTE_TDSCDMA_GSM_WCDMA("2G/3G/4G/5G (NR/LTE/TD-SCDMA/GSM/WCDMA)", 32),
    NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 5G + TD-SCDMA (All Networks)", 33);

    /**
     * GSM_UMTS and WCDMA_PREF allow the same radios, so on Android 11+ they store the
     * same bitmask and GSM_UMTS reads back as WCDMA_PREF.
     */
    infix fun sameRadiosAs(other: NetworkMode?): Boolean =
        this == other || setOf(this, other) == setOf(WCDMA_PREF, GSM_UMTS)

    companion object {
        /**
         * Get NetworkMode by RIL constant value
         */
        fun fromValue(value: Int): NetworkMode? {
            return values().find { it.value == value }
        }

        /**
         * The modes worth offering first: the full list is RIL terminology covering
         * radios most devices do not have.
         */
        val common: List<NetworkMode> = listOf(
            GSM_ONLY,
            WCDMA_ONLY,
            LTE_ONLY,
            NR_ONLY,
            NR_LTE
        )

        /** The modes not in [common], in declaration order. */
        val advanced: List<NetworkMode> = entries - common.toSet()
    }
}

/**
 * Configuration for the two modes that can be toggled between
 */
data class ToggleModeConfig(
    val modeA: NetworkMode,
    val modeB: NetworkMode
) {
    /**
     * Matches modes by [NetworkMode.sameRadiosAs]. When [actual] is unreadable, alternates
     * from [lastApplied]; a mode outside the pair restarts at [modeA].
     */
    fun resolve(actual: NetworkMode?, lastApplied: NetworkMode?): ToggleState = when {
        modeA sameRadiosAs actual -> ToggleState(modeA, modeB)
        modeB sameRadiosAs actual -> ToggleState(modeB, modeA)
        actual == null && lastApplied == modeA -> ToggleState(null, modeB)
        else -> ToggleState(actual, modeA)
    }
}

data class ToggleState(
    val current: NetworkMode?,
    val next: NetworkMode
)

sealed class CompatibilityState {
    object Pending : CompatibilityState()
    object Compatible : CompatibilityState()
    data class Incompatible(val reason: String) : CompatibilityState()
    data class PermissionDenied(val method: ControlMethod) : CompatibilityState()
}

/**
 * Represents information about a SIM card in the device
 */
data class SimInfo(
    val subscriptionId: Int,
    val simSlotIndex: Int,
    val displayName: String
)

/**
 * Values for the stored SIM selection.
 */
object SubscriptionSelection {
    /**
     * Follow the system default data subscription instead of a fixed SIM.
     *
     * Equal to `SubscriptionManager.INVALID_SUBSCRIPTION_ID`, so a subscription ID read
     * back from the platform can never be distinguished from this. Only compare against
     * it for a value that came from the stored selection.
     */
    const val AUTO = -1
}

/**
 * Storage keys for per-SIM configuration.
 */
object SimConfigKey {
    /**
     * Keyed on the subscription ID, which Android assigns as the row id of its SIM
     * table. Rows persist, so a SIM normally keeps its id across removal and
     * reinsertion; a rebuilt table or a re-downloaded eSIM profile gets a new id and
     * that SIM falls back to defaults.
     */
    fun forSubscription(subId: Int): String = "sub_$subId"
}

/**
 * Outcome of querying the device for SIM cards.
 *
 * [Loaded] is the only outcome that reports the true set of SIMs. The others mean the
 * set could not be determined, which is not the same as the device having no SIMs — a
 * stored SIM selection must survive them.
 */
sealed interface SimQueryResult {
    data class Loaded(val sims: List<SimInfo>) : SimQueryResult
    data object PermissionDenied : SimQueryResult
    data class Failed(val cause: Throwable) : SimQueryResult
}
