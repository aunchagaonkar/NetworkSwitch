package com.supernova.networkswitch.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.ServiceManager
import androidx.annotation.Keep
import com.android.internal.telephony.ITelephony
import com.supernova.networkswitch.IShizukuController

/**
 * Shizuku service for network control operations
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
        
        private val typeNr by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_NR")
                .getLong(null)
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val modeLte by lazy {
            Class.forName("com.android.internal.telephony.RILConstants")
                .getDeclaredField("NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA")
                .getInt(null)
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val modeNr by lazy {
            Class.forName("com.android.internal.telephony.RILConstants")
                .getDeclaredField("NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA")
                .getInt(null)
        }
        
        // Pure mode constants
        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val typeLteOnly: Long by lazy {
            try {
                Class.forName("android.telephony.TelephonyManager")
                    .getDeclaredField("NETWORK_TYPE_BITMASK_LTE")
                    .getLong(null)
            } catch (e: Exception) {
                524288L // Fallback LTE bitmask
            }
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val typeNrOnly: Long by lazy {
            try {
                Class.forName("android.telephony.TelephonyManager")
                    .getDeclaredField("NETWORK_TYPE_BITMASK_NR")
                    .getLong(null)
            } catch (e: Exception) {
                2097152L // Fallback NR bitmask
            }
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val modeLteOnly: Int by lazy {
            try {
                Class.forName("com.android.internal.telephony.RILConstants")
                    .getDeclaredField("NETWORK_MODE_LTE_ONLY")
                    .getInt(null)
            } catch (e: Exception) {
                11 // Fallback value
            }
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val modeNrOnly: Int by lazy {
            try {
                Class.forName("com.android.internal.telephony.RILConstants")
                    .getDeclaredField("NETWORK_MODE_NR_ONLY")
                    .getInt(null)
            } catch (e: Exception) {
                23 // Fallback value
            }
        }
    }

    @Keep
    constructor(context: Context) : this()

    override fun compatibilityCheck(subId: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Test pure modes on Android 12+
                val originalTypes = iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                
                // Test LTE-only
                iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, typeLteOnly)
                val lteTest = iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                
                // Test NR-only
                iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, typeNrOnly)
                val nrTest = iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                
                // Restore original
                iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, originalTypes)
                
                (lteTest == typeLteOnly) && (nrTest == typeNrOnly)
            } else {
                // Test preferred network modes for older Android
                val original = iTelephony.getPreferredNetworkType(subId)
                
                iTelephony.setPreferredNetworkType(subId, modeLteOnly)
                val lteTest = iTelephony.getPreferredNetworkType(subId) == modeLteOnly
                
                iTelephony.setPreferredNetworkType(subId, modeNrOnly)
                val nrTest = iTelephony.getPreferredNetworkType(subId) == modeNrOnly
                
                // Restore
                iTelephony.setPreferredNetworkType(subId, original)
                
                lteTest && nrTest
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun getNetworkState(subId: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser) and typeNr != 0L
            } else {
                val currentMode = iTelephony.getPreferredNetworkType(subId)
                currentMode == modeNrOnly || currentMode >= 23 // 5G modes
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun setNetworkState(subId: Int, enabled: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (enabled) {
                    // Use pure 5G mode (NR only)
                    iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, typeNrOnly)
                } else {
                    // Use pure 4G mode (LTE only)
                    iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, typeLteOnly)
                }
            } else {
                // For older Android versions
                if (enabled) {
                    iTelephony.setPreferredNetworkType(subId, modeNrOnly)
                } else {
                    iTelephony.setPreferredNetworkType(subId, modeLteOnly)
                }
            }
        } catch (_: Exception) {
            // Silently fail
        }
    }

    override fun destroy() {
        // Cleanup if needed
    }
}
