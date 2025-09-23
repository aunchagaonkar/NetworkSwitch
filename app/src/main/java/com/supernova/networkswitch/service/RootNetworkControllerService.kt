package com.supernova.networkswitch.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ServiceManager
import com.android.internal.telephony.ITelephony
import com.supernova.networkswitch.IRootController
import com.topjohnwu.superuser.ipc.RootService

/**
 * Modern root-based network controller service
 * Implements pure network mode switching with clean architecture principles
 */
class RootNetworkControllerService : RootService() {
    
    companion object {
        private val iTelephony: ITelephony by lazy {
            ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE))
        }
        
        private val reasonUser: Int by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("ALLOWED_NETWORK_TYPES_REASON_USER")
                .getInt(null)
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val modeLteOnly: Int by lazy {
            try {
                Class.forName("com.android.internal.telephony.RILConstants")
                    .getDeclaredField("NETWORK_MODE_LTE_ONLY")
                    .getInt(null)
            } catch (e: Exception) {
                11 // Fallback: NETWORK_MODE_LTE_GSM_WCDMA
            }
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi") 
        private val modeNrOnly: Int by lazy {
            try {
                Class.forName("com.android.internal.telephony.RILConstants")
                    .getDeclaredField("NETWORK_MODE_NR_ONLY")
                    .getInt(null)
            } catch (e: Exception) {
                23 // Fallback: NETWORK_MODE_NR_LTE
            }
        }
        
        // Android 12+ bitmasks for pure modes
        private val typeLteOnly: Long by lazy {
            try {
                Class.forName("android.telephony.TelephonyManager")
                    .getDeclaredField("NETWORK_TYPE_BITMASK_LTE")
                    .getLong(null)
            } catch (e: Exception) {
                524288L // Common LTE bitmask value
            }
        }
        
        private val typeNrOnly: Long by lazy {
            try {
                Class.forName("android.telephony.TelephonyManager")
                    .getDeclaredField("NETWORK_TYPE_BITMASK_NR")
                    .getLong(null)
            } catch (e: Exception) {
                2097152L // Common NR-only bitmask value
            }
        }
    }

    override fun onBind(intent: Intent) = object : IRootController.Stub() {
        
        override fun compatibilityCheck(subId: Int): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Test if we can read/write allowed network types
                    reasonUser
                    typeLteOnly
                    typeNrOnly
                    
                    val originalTypes = iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                    iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, originalTypes)
                    true
                } else {
                    // Test preferred network mode switching for Android 11 and below
                    modeLteOnly
                    modeNrOnly
                    val original = iTelephony.getPreferredNetworkType(subId)
                    iTelephony.setPreferredNetworkType(subId, original)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }

        override fun getNetworkState(subId: Int): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val currentTypes = iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                    // Check if NR (5G) is enabled
                    (currentTypes and typeNrOnly) != 0L
                } else {
                    val currentMode = iTelephony.getPreferredNetworkType(subId)
                    // Check if current mode is a 5G mode
                    currentMode == modeNrOnly || currentMode >= 23
                }
            } catch (e: Exception) {
                false
            }
        }

        override fun setNetworkState(subId: Int, enabled: Boolean) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (enabled) {
                        // Set pure 5G mode (NR only)
                        iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, typeNrOnly)
                    } else {
                        // Set pure 4G mode (LTE only)
                        iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, typeLteOnly)
                    }
                } else {
                    if (enabled) {
                        // Set pure 5G mode
                        iTelephony.setPreferredNetworkType(subId, modeNrOnly)
                    } else {
                        // Set pure 4G mode
                        iTelephony.setPreferredNetworkType(subId, modeLteOnly)
                    }
                }
            } catch (e: Exception) {
                // If pure modes fail, try with basic fallbacks
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val fallbackTypes = if (enabled) typeNrOnly or typeLteOnly else typeLteOnly
                        iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, fallbackTypes)
                    } else {
                        val fallbackMode = if (enabled) modeNrOnly else modeLteOnly
                        iTelephony.setPreferredNetworkType(subId, fallbackMode)
                    }
                } catch (fallbackException: Exception) {
                    throw Exception("Failed to set network mode: ${fallbackException.message}")
                }
            }
        }
    }
}
