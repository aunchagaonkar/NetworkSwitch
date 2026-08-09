package com.supernova.networkswitch.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import com.supernova.networkswitch.IRootController
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.hiddenapibypass.HiddenApiBypass

class RootNetworkControllerService : RootService() {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.setHiddenApiExemptions("Landroid/telephony/", "Lcom/android/internal/telephony/")
            } catch (e: Throwable) {
                Log.e("NetworkSwitch", "Root: HiddenApiBypass failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "NetworkSwitch"

        private fun getITelephony(): Any? {
            return try {
                val binder = ServiceManager.getService(Context.TELEPHONY_SERVICE)
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                asInterfaceMethod.invoke(null, binder)
            } catch (e: Exception) {
                Log.e(TAG, "Root: Failed to get ITelephony", e)
                null
            }
        }

        private fun getStaticLongField(className: String, fieldName: String, fallback: Long): Long {
            return try {
                val clazz = Class.forName(className)
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.getLong(null)
            } catch (_: Exception) {
                fallback
            }
        }

        private fun getStaticIntField(className: String, fieldName: String, fallback: Int): Int {
            return try {
                val clazz = Class.forName(className)
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.getInt(null)
            } catch (_: Exception) {
                fallback
            }
        }

        private val reasonUser by lazy {
            getStaticIntField("android.telephony.TelephonyManager", "ALLOWED_NETWORK_TYPES_REASON_USER", 0)
        }

        private val BITMASK_GSM by lazy {
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_GSM", 1L shl 15) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_GPRS", 1L shl 0) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_EDGE", 1L shl 1)
        }
        private val BITMASK_WCDMA by lazy {
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_UMTS", 1L shl 2) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_HSDPA", 1L shl 7) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_HSUPA", 1L shl 8) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_HSPA", 1L shl 9) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_HSPAP", 1L shl 14)
        }
        private val BITMASK_CDMA by lazy {
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_CDMA", 1L shl 3) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_1xRTT", 1L shl 6)
        }
        private val BITMASK_EVDO by lazy {
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_EVDO_0", 1L shl 4) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_EVDO_A", 1L shl 5) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_EVDO_B", 1L shl 11)
        }
        private val BITMASK_LTE by lazy {
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_LTE", 1L shl 12) or
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_LTE_CA", 1L shl 18)
        }
        private val BITMASK_TDSCDMA by lazy {
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_TD_SCDMA", 1L shl 16)
        }
        private val BITMASK_NR by lazy {
            getStaticLongField("android.telephony.TelephonyManager", "NETWORK_TYPE_BITMASK_NR", 1L shl 19)
        }

        private fun mapNetworkModeToBitmask(networkMode: Int): Long {
            return when (networkMode) {
                0 -> BITMASK_GSM or BITMASK_WCDMA
                1 -> BITMASK_GSM
                2 -> BITMASK_WCDMA
                3 -> BITMASK_GSM or BITMASK_WCDMA
                4 -> BITMASK_CDMA or BITMASK_EVDO
                5 -> BITMASK_CDMA
                6 -> BITMASK_EVDO
                7 -> BITMASK_GSM or BITMASK_WCDMA or BITMASK_CDMA or BITMASK_EVDO
                8 -> BITMASK_LTE or BITMASK_CDMA or BITMASK_EVDO
                9 -> BITMASK_LTE or BITMASK_GSM or BITMASK_WCDMA
                10 -> BITMASK_LTE or BITMASK_CDMA or BITMASK_EVDO or BITMASK_GSM or BITMASK_WCDMA
                11 -> BITMASK_LTE
                12 -> BITMASK_LTE or BITMASK_WCDMA
                13 -> BITMASK_TDSCDMA
                14 -> BITMASK_TDSCDMA or BITMASK_WCDMA
                15 -> BITMASK_LTE or BITMASK_TDSCDMA
                16 -> BITMASK_TDSCDMA or BITMASK_GSM
                17 -> BITMASK_LTE or BITMASK_TDSCDMA or BITMASK_GSM
                18 -> BITMASK_TDSCDMA or BITMASK_GSM or BITMASK_WCDMA
                19 -> BITMASK_LTE or BITMASK_TDSCDMA or BITMASK_WCDMA
                20 -> BITMASK_LTE or BITMASK_TDSCDMA or BITMASK_GSM or BITMASK_WCDMA
                21 -> BITMASK_TDSCDMA or BITMASK_CDMA or BITMASK_EVDO or BITMASK_GSM or BITMASK_WCDMA
                22 -> BITMASK_LTE or BITMASK_TDSCDMA or BITMASK_CDMA or BITMASK_EVDO or BITMASK_GSM or BITMASK_WCDMA
                23 -> BITMASK_NR
                24 -> BITMASK_NR or BITMASK_LTE
                25 -> BITMASK_NR or BITMASK_LTE or BITMASK_CDMA or BITMASK_EVDO
                26 -> BITMASK_NR or BITMASK_LTE or BITMASK_GSM or BITMASK_WCDMA
                27 -> BITMASK_NR or BITMASK_LTE or BITMASK_CDMA or BITMASK_EVDO or BITMASK_GSM or BITMASK_WCDMA
                28 -> BITMASK_NR or BITMASK_LTE or BITMASK_WCDMA
                29 -> BITMASK_NR or BITMASK_LTE or BITMASK_TDSCDMA
                30 -> BITMASK_NR or BITMASK_LTE or BITMASK_TDSCDMA or BITMASK_GSM
                31 -> BITMASK_NR or BITMASK_LTE or BITMASK_TDSCDMA or BITMASK_WCDMA
                32 -> BITMASK_NR or BITMASK_LTE or BITMASK_TDSCDMA or BITMASK_GSM or BITMASK_WCDMA
                33 -> BITMASK_NR or BITMASK_LTE or BITMASK_TDSCDMA or BITMASK_CDMA or BITMASK_EVDO or BITMASK_GSM or BITMASK_WCDMA
                else -> (1L shl 31) - 1 // All
            }
        }

        private fun mapBitmaskToNetworkMode(bitmask: Long): Int {
            // Check exact matches for 0-33
            for (i in 0..33) {
                if (mapNetworkModeToBitmask(i) == bitmask) return i
            }
            
            // Handle cases where bitmask might have additional bits
            if ((bitmask and BITMASK_NR) != 0L && (bitmask and BITMASK_LTE) != 0L) return 24
            if ((bitmask and BITMASK_NR) != 0L) return 23
            if ((bitmask and BITMASK_LTE) != 0L) return 11
            
            return 0 
        }
    }

    override fun onBind(intent: Intent) = object : IRootController.Stub() {

        override fun compatibilityCheck(subId: Int): Boolean {
            return getITelephony() != null
        }

        override fun getCurrentNetworkMode(subId: Int): Int {
            try {
                val iTelephony = getITelephony() ?: return -1
                val methods = iTelephony.javaClass.methods
                
                for (m in methods) {
                    if (m.name == "getAllowedNetworkTypesForReason") {
                        try {
                            val bitmask = if (m.parameterCount == 2) {
                                m.invoke(iTelephony, subId, reasonUser) as Long
                            } else if (m.parameterCount == 3 && m.parameterTypes[2] == String::class.java) {
                                m.invoke(iTelephony, subId, reasonUser, "com.android.phone") as Long
                            } else {
                                continue
                            }
                            
                            val mode = mapBitmaskToNetworkMode(bitmask)
                            Log.i(TAG, "Root: getCurrentNetworkMode(subId=$subId) -> $mode (bitmask=$bitmask)")
                            return mode
                        } catch (_: Exception) {
                            // Try next
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Root: Error in getCurrentNetworkMode", e)
            }
            return -1
        }

        override fun setNetworkMode(subId: Int, networkMode: Int) {
            try {
                val iTelephony = getITelephony() ?: return
                val bitmask = mapNetworkModeToBitmask(networkMode)
                val methods = iTelephony.javaClass.methods

                var success = false
                
                // Try setAllowedNetworkTypesForReason
                for (m in methods) {
                    if (m.name == "setAllowedNetworkTypesForReason") {
                        try {
                            if (m.parameterCount == 3) {
                                m.invoke(iTelephony, subId, reasonUser, bitmask)
                                success = true
                            } else if (m.parameterCount == 4 && m.parameterTypes[3] == String::class.java) {
                                m.invoke(iTelephony, subId, reasonUser, bitmask, "com.android.phone")
                                success = true
                            }
                            if (success) {
                                Log.i(TAG, "Root: setAllowedNetworkTypesForReason(subId=$subId, mode=$networkMode) success")
                                break
                            }
                        } catch (_: Exception) {
                            // Try next
                        }
                    }
                }

                if (!success) {
                    for (m in methods) {
                        if (m.name == "setAllowedNetworkTypes") {
                            try {
                                if (m.parameterCount == 2) {
                                    m.invoke(iTelephony, subId, bitmask)
                                    success = true
                                } else if (m.parameterCount == 3 && m.parameterTypes[1].name == "long") {
                                    m.invoke(iTelephony, subId, bitmask, "com.android.phone")
                                    success = true
                                }
                                if (success) {
                                    Log.i(TAG, "Root: setAllowedNetworkTypes(subId=$subId, bitmask=$bitmask) success")
                                    break
                                }
                            } catch (_: Exception) {
                                // Try next
                            }
                        }
                    }
                }

                if (!success) {
                    for (m in methods) {
                        if (m.name == "setPreferredNetworkType" && m.parameterCount == 2) {
                            try {
                                m.invoke(iTelephony, subId, networkMode)
                                success = true
                                Log.i(TAG, "Root: setPreferredNetworkType(subId=$subId, mode=$networkMode) success")
                                break
                            } catch (_: Exception) {
                                // Try next
                            }
                        }
                    }
                }
                
                if (!success) {
                    Log.w(TAG, "Root: Failed to set network mode $networkMode for subId $subId")
                }

            } catch (e: Throwable) {
                Log.e(TAG, "Root: FATAL error in setNetworkMode", e)
            }
        }
    }
}
