package com.supernova.networkswitch.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.ServiceManager
import androidx.annotation.Keep
import com.android.internal.telephony.ITelephony
import com.supernova.networkswitch.IShizukuController

/**
 * Simple Shizuku service for network control operations
 */
class ShizukuControllerService() : IShizukuController.Stub() {

    companion object {
        private val iTelephony by lazy {
            ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE))
        }
        
        private val reasonUser by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("ALLOWED_NETWORK_TYPES_REASON_USER")
                .getInt(null)
        }
        
        // Get network type bitmasks from Android TelephonyManager constants
        private val NETWORK_TYPE_BITMASK_GSM by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_GSM")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_GPRS by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_GPRS")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_EDGE by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_EDGE")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_UMTS by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_UMTS")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_CDMA by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_CDMA")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_EVDO_0 by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_EVDO_0")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_EVDO_A by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_EVDO_A")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_1xRTT by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_1xRTT")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_HSDPA by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_HSDPA")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_HSUPA by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_HSUPA")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_HSPA by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_HSPA")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_EVDO_B by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_EVDO_B")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_LTE by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_LTE")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_EHRPD by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_EHRPD")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_HSPAP by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_HSPAP")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_TD_SCDMA by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_TD_SCDMA")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_LTE_CA by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_LTE_CA")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_IWLAN by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_IWLAN")
                .getLong(null)
        }
        
        private val NETWORK_TYPE_BITMASK_NR by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_NR")
                .getLong(null)
        }
        
        // Combined bitmasks for common network classes
        private fun get2GBitmask(): Long {
            return try {
                NETWORK_TYPE_BITMASK_GSM or NETWORK_TYPE_BITMASK_GPRS or 
                NETWORK_TYPE_BITMASK_EDGE or NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_1xRTT
            } catch (e: Exception) {
                1L or 2L or 4L or 16L or 128L
            }
        }
        
        private fun get3GBitmask(): Long {
            return try {
                NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or 
                NETWORK_TYPE_BITMASK_EVDO_B or NETWORK_TYPE_BITMASK_EHRPD or 
                NETWORK_TYPE_BITMASK_HSUPA or NETWORK_TYPE_BITMASK_HSDPA or 
                NETWORK_TYPE_BITMASK_HSPA or NETWORK_TYPE_BITMASK_HSPAP or 
                NETWORK_TYPE_BITMASK_UMTS or NETWORK_TYPE_BITMASK_TD_SCDMA
            } catch (e: Exception) {
                32L or 64L or 2048L or 8192L or 512L or 256L or 1024L or 16384L or 8L or 32768L
            }
        }
        
        private fun get4GBitmask(): Long {
            return try {
                NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_LTE_CA or NETWORK_TYPE_BITMASK_IWLAN
            } catch (e: Exception) {
                4096L or 65536L or 131072L
            }
        }
        
        private fun get5GBitmask(): Long {
            return try {
                NETWORK_TYPE_BITMASK_NR
            } catch (e: Exception) {
                524288L
            }
        }
        
        /**
         * Map RIL network mode constants to network type bitmasks for Android 12+
         */
        private fun mapNetworkModeToBitmask(networkMode: Int): Long {
            return when (networkMode) {
                0 -> get2GBitmask() or get3GBitmask() // WCDMA_PREF
                1 -> NETWORK_TYPE_BITMASK_GSM // GSM_ONLY
                2 -> NETWORK_TYPE_BITMASK_UMTS // WCDMA_ONLY
                3 -> get2GBitmask() or get3GBitmask() // GSM_UMTS
                4 -> NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B // CDMA
                5 -> NETWORK_TYPE_BITMASK_CDMA // CDMA_NO_EVDO
                6 -> NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B // EVDO_NO_CDMA
                7 -> get2GBitmask() or get3GBitmask() or get4GBitmask() // GLOBAL
                8 -> NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B // LTE_CDMA_EVDO
                9 -> NETWORK_TYPE_BITMASK_LTE or get2GBitmask() or get3GBitmask() // LTE_GSM_WCDMA
                10 -> NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B or get2GBitmask() or get3GBitmask() // LTE_CDMA_EVDO_GSM_WCDMA
                11 -> NETWORK_TYPE_BITMASK_LTE // LTE_ONLY
                12 -> NETWORK_TYPE_BITMASK_LTE or get3GBitmask() // LTE_WCDMA
                13 -> NETWORK_TYPE_BITMASK_TD_SCDMA // TDSCDMA_ONLY
                14 -> NETWORK_TYPE_BITMASK_TD_SCDMA or NETWORK_TYPE_BITMASK_UMTS // TDSCDMA_WCDMA
                15 -> NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA // LTE_TDSCDMA
                16 -> NETWORK_TYPE_BITMASK_TD_SCDMA or get2GBitmask() // TDSCDMA_GSM
                17 -> NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA or get2GBitmask() // LTE_TDSCDMA_GSM
                18 -> NETWORK_TYPE_BITMASK_TD_SCDMA or get2GBitmask() or get3GBitmask() // TDSCDMA_GSM_WCDMA
                19 -> NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA or get3GBitmask() // LTE_TDSCDMA_WCDMA
                20 -> NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA or get2GBitmask() or get3GBitmask() // LTE_TDSCDMA_GSM_WCDMA
                21 -> NETWORK_TYPE_BITMASK_TD_SCDMA or NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B or get2GBitmask() or get3GBitmask() // TDSCDMA_CDMA_EVDO_GSM_WCDMA
                22 -> NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA or NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B or get2GBitmask() or get3GBitmask() // LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA
                23 -> NETWORK_TYPE_BITMASK_NR // NR_ONLY
                24 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE // NR_LTE
                25 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B // NR_LTE_CDMA_EVDO
                26 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or get2GBitmask() or get3GBitmask() // NR_LTE_GSM_WCDMA
                27 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B or get2GBitmask() or get3GBitmask() // NR_LTE_CDMA_EVDO_GSM_WCDMA
                28 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or get3GBitmask() // NR_LTE_WCDMA
                29 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA // NR_LTE_TDSCDMA
                30 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA or get2GBitmask() // NR_LTE_TDSCDMA_GSM
                31 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA or get3GBitmask() // NR_LTE_TDSCDMA_WCDMA
                32 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA or get2GBitmask() or get3GBitmask() // NR_LTE_TDSCDMA_GSM_WCDMA
                33 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_TD_SCDMA or NETWORK_TYPE_BITMASK_CDMA or NETWORK_TYPE_BITMASK_EVDO_0 or NETWORK_TYPE_BITMASK_EVDO_A or NETWORK_TYPE_BITMASK_EVDO_B or get2GBitmask() or get3GBitmask() // NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA
                else -> get2GBitmask() or get3GBitmask() or get4GBitmask() // Default fallback
            }
        }
        
        /**
         * Map bitmask back to RIL network mode for getCurrentNetworkMode
         */
        private fun mapBitmaskToNetworkMode(bitmask: Long): Int {
            // Try exact matches for all modes by comparing with their expected bitmasks
            for (mode in 0..33) {
                try {
                    if (bitmask == mapNetworkModeToBitmask(mode)) {
                        return mode
                    }
                } catch (_: Exception) {
                    // Skip invalid modes
                }
            }
            
            // If no exact match found, use simple fallback logic
            return when {
                bitmask == NETWORK_TYPE_BITMASK_NR -> 23 // NR_ONLY
                bitmask == NETWORK_TYPE_BITMASK_LTE -> 11 // LTE_ONLY
                bitmask == NETWORK_TYPE_BITMASK_GSM -> 1 // GSM_ONLY
                bitmask == NETWORK_TYPE_BITMASK_UMTS -> 2 // WCDMA_ONLY
                bitmask == NETWORK_TYPE_BITMASK_TD_SCDMA -> 13 // TDSCDMA_ONLY
                bitmask == (NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE) -> 24 // NR_LTE
                (bitmask and NETWORK_TYPE_BITMASK_NR) != 0L -> 23 // Has 5G, default to NR_ONLY
                (bitmask and NETWORK_TYPE_BITMASK_LTE) != 0L -> 11 // Has LTE, default to LTE_ONLY
                (bitmask and get3GBitmask()) != 0L -> 2 // Has 3G, default to WCDMA_ONLY
                (bitmask and get2GBitmask()) != 0L -> 1 // Has 2G, default to GSM_ONLY
                else -> 0 // WCDMA_PREF as ultimate fallback
            }
        }
    }

    @Keep
    constructor(context: Context) : this()

    override fun compatibilityCheck(subId: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                reasonUser
                iTelephony.setAllowedNetworkTypesForReason(
                    subId,
                    reasonUser,
                    iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                )
            } else {
                iTelephony.setPreferredNetworkType(
                    subId,
                    iTelephony.getPreferredNetworkType(subId)
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun getCurrentNetworkMode(subId: Int): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val currentBitmask = iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                mapBitmaskToNetworkMode(currentBitmask)
            } else {
                iTelephony.getPreferredNetworkType(subId)
            }
        } catch (_: Exception) {
            -1
        }
    }

    override fun setNetworkMode(subId: Int, networkMode: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val networkTypeBitmask = mapNetworkModeToBitmask(networkMode)
                iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, networkTypeBitmask)
            } else {
                iTelephony.setPreferredNetworkType(subId, networkMode)
            }
        } catch (_: Exception) {
            // Silently fail
        }
    }

    override fun destroy() {
        // Cleanup if needed
    }
}
