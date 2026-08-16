package com.supernova.networkswitch.service

/**
 * Translates between RIL network-mode constants (the 0..33 values used by
 * `ITelephony.setPreferredNetworkType`) and the `NETWORK_TYPE_BITMASK_*` bitmasks
 * that `setAllowedNetworkTypesForReason` expects on Android 11+.
 *
 * [platform] resolves the bitmask values reflectively from
 * `android.telephony.TelephonyManager`, falling back to the AOSP value per field when
 * one is missing on an OEM build. [aosp] uses the AOSP values directly.
 */
class NetworkModeBitmaskMapper(
    private val gsm: Long,
    private val wcdma: Long,
    private val cdma: Long,
    private val evdo: Long,
    private val lte: Long,
    private val tdscdma: Long,
    private val nr: Long,
) {

    fun toBitmask(networkMode: Int): Long = when (networkMode) {
        0 -> gsm or wcdma
        1 -> gsm
        2 -> wcdma
        3 -> gsm or wcdma
        4 -> cdma or evdo
        5 -> cdma
        6 -> evdo
        7 -> gsm or wcdma or cdma or evdo
        8 -> lte or cdma or evdo
        9 -> lte or gsm or wcdma
        10 -> lte or cdma or evdo or gsm or wcdma
        11 -> lte
        12 -> lte or wcdma
        13 -> tdscdma
        14 -> tdscdma or wcdma
        15 -> lte or tdscdma
        16 -> tdscdma or gsm
        17 -> lte or tdscdma or gsm
        18 -> tdscdma or gsm or wcdma
        19 -> lte or tdscdma or wcdma
        20 -> lte or tdscdma or gsm or wcdma
        21 -> tdscdma or cdma or evdo or gsm or wcdma
        22 -> lte or tdscdma or cdma or evdo or gsm or wcdma
        23 -> nr
        24 -> nr or lte
        25 -> nr or lte or cdma or evdo
        26 -> nr or lte or gsm or wcdma
        27 -> nr or lte or cdma or evdo or gsm or wcdma
        28 -> nr or lte or wcdma
        29 -> nr or lte or tdscdma
        30 -> nr or lte or tdscdma or gsm
        31 -> nr or lte or tdscdma or wcdma
        32 -> nr or lte or tdscdma or gsm or wcdma
        33 -> nr or lte or tdscdma or cdma or evdo or gsm or wcdma
        else -> ALL_NETWORK_TYPES
    }

    /**
     * Inverse of [toBitmask], best-effort.
     *
     * The forward map is not injective, so mode 3 (GSM_UMTS) produces the same bitmask
     * as mode 0 (WCDMA_PREF) and reads back as mode 0. Both select the same radio
     * technologies; no other pair of modes collides.
     *
     * Modems commonly report bits beyond those the mode table defines. When no exact
     * match exists, fall back to the highest-generation technology in the bitmask.
     */
    fun toNetworkMode(bitmask: Long): Int {
        for (mode in 0..MAX_NETWORK_MODE) {
            if (toBitmask(mode) == bitmask) return mode
        }

        if (bitmask and nr != 0L && bitmask and lte != 0L) return 24
        if (bitmask and nr != 0L) return 23
        if (bitmask and lte != 0L) return 11
        if (bitmask and tdscdma != 0L) return 13
        if (bitmask and wcdma != 0L) return 2
        if (bitmask and gsm != 0L) return 1
        if (bitmask and cdma != 0L && bitmask and evdo != 0L) return 4
        if (bitmask and cdma != 0L) return 5
        if (bitmask and evdo != 0L) return 6

        return 0
    }

    companion object {
        const val MAX_NETWORK_MODE = 33

        /** Every bit the RIL mode table can produce. Returned for unrecognised modes. */
        const val ALL_NETWORK_TYPES = (1L shl 31) - 1

        private const val TELEPHONY_MANAGER = "android.telephony.TelephonyManager"

        // AOSP NETWORK_TYPE_BITMASK_* values: 1 << (NETWORK_TYPE_* - 1).
        private const val AOSP_GPRS = 1L shl 0
        private const val AOSP_EDGE = 1L shl 1
        private const val AOSP_UMTS = 1L shl 2
        private const val AOSP_CDMA = 1L shl 3
        private const val AOSP_EVDO_0 = 1L shl 4
        private const val AOSP_EVDO_A = 1L shl 5
        private const val AOSP_1xRTT = 1L shl 6
        private const val AOSP_HSDPA = 1L shl 7
        private const val AOSP_HSUPA = 1L shl 8
        private const val AOSP_HSPA = 1L shl 9
        private const val AOSP_EVDO_B = 1L shl 11
        private const val AOSP_LTE = 1L shl 12
        private const val AOSP_HSPAP = 1L shl 14
        private const val AOSP_GSM = 1L shl 15
        private const val AOSP_TD_SCDMA = 1L shl 16
        private const val AOSP_LTE_CA = 1L shl 18
        private const val AOSP_NR = 1L shl 19

        /** Uses AOSP constants only, so it needs no reflection and runs on the JVM. */
        val aosp = NetworkModeBitmaskMapper(
            gsm = AOSP_GSM or AOSP_GPRS or AOSP_EDGE,
            wcdma = AOSP_UMTS or AOSP_HSDPA or AOSP_HSUPA or AOSP_HSPA or AOSP_HSPAP,
            cdma = AOSP_CDMA or AOSP_1xRTT,
            evdo = AOSP_EVDO_0 or AOSP_EVDO_A or AOSP_EVDO_B,
            lte = AOSP_LTE or AOSP_LTE_CA,
            tdscdma = AOSP_TD_SCDMA,
            nr = AOSP_NR,
        )

        /** Mapper built from the running platform's constants, per-field AOSP fallback. */
        val platform: NetworkModeBitmaskMapper by lazy {
            NetworkModeBitmaskMapper(
                gsm = bitmask("GSM", AOSP_GSM) or
                    bitmask("GPRS", AOSP_GPRS) or
                    bitmask("EDGE", AOSP_EDGE),
                wcdma = bitmask("UMTS", AOSP_UMTS) or
                    bitmask("HSDPA", AOSP_HSDPA) or
                    bitmask("HSUPA", AOSP_HSUPA) or
                    bitmask("HSPA", AOSP_HSPA) or
                    bitmask("HSPAP", AOSP_HSPAP),
                cdma = bitmask("CDMA", AOSP_CDMA) or bitmask("1xRTT", AOSP_1xRTT),
                evdo = bitmask("EVDO_0", AOSP_EVDO_0) or
                    bitmask("EVDO_A", AOSP_EVDO_A) or
                    bitmask("EVDO_B", AOSP_EVDO_B),
                lte = bitmask("LTE", AOSP_LTE) or bitmask("LTE_CA", AOSP_LTE_CA),
                tdscdma = bitmask("TD_SCDMA", AOSP_TD_SCDMA),
                nr = bitmask("NR", AOSP_NR),
            )
        }

        /** `TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER`, 0 on AOSP. */
        val reasonUser: Int by lazy {
            staticField(TELEPHONY_MANAGER, "ALLOWED_NETWORK_TYPES_REASON_USER")?.getInt(null) ?: 0
        }

        private fun bitmask(suffix: String, fallback: Long): Long =
            staticField(TELEPHONY_MANAGER, "NETWORK_TYPE_BITMASK_$suffix")?.getLong(null) ?: fallback

        private fun staticField(className: String, fieldName: String) = try {
            Class.forName(className).getDeclaredField(fieldName).apply { isAccessible = true }
        } catch (_: Exception) {
            null
        }
    }
}
