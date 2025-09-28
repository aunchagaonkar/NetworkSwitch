package com.supernova.networkswitch.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ServiceManager
import com.android.internal.telephony.ITelephony
import com.supernova.networkswitch.IRootController
import com.topjohnwu.superuser.ipc.RootService

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
        
        private fun getBitmask(fieldName: String): Long {
            return try {
                Class.forName("android.telephony.TelephonyManager")
                    .getDeclaredField(fieldName)
                    .getLong(null)
            } catch (e: Exception) {
                0L
            }
        }
        
        private val bitmasks = mapOf(
            "GSM" to lazy { getBitmask("NETWORK_TYPE_BITMASK_GSM") },
            "GPRS" to lazy { getBitmask("NETWORK_TYPE_BITMASK_GPRS") },
            "EDGE" to lazy { getBitmask("NETWORK_TYPE_BITMASK_EDGE") },
            "UMTS" to lazy { getBitmask("NETWORK_TYPE_BITMASK_UMTS") },
            "CDMA" to lazy { getBitmask("NETWORK_TYPE_BITMASK_CDMA") },
            "EVDO_0" to lazy { getBitmask("NETWORK_TYPE_BITMASK_EVDO_0") },
            "EVDO_A" to lazy { getBitmask("NETWORK_TYPE_BITMASK_EVDO_A") },
            "1xRTT" to lazy { getBitmask("NETWORK_TYPE_BITMASK_1xRTT") },
            "HSDPA" to lazy { getBitmask("NETWORK_TYPE_BITMASK_HSDPA") },
            "HSUPA" to lazy { getBitmask("NETWORK_TYPE_BITMASK_HSUPA") },
            "HSPA" to lazy { getBitmask("NETWORK_TYPE_BITMASK_HSPA") },
            "EVDO_B" to lazy { getBitmask("NETWORK_TYPE_BITMASK_EVDO_B") },
            "LTE" to lazy { getBitmask("NETWORK_TYPE_BITMASK_LTE") },
            "EHRPD" to lazy { getBitmask("NETWORK_TYPE_BITMASK_EHRPD") },
            "HSPAP" to lazy { getBitmask("NETWORK_TYPE_BITMASK_HSPAP") },
            "TD_SCDMA" to lazy { getBitmask("NETWORK_TYPE_BITMASK_TD_SCDMA") },
            "LTE_CA" to lazy { getBitmask("NETWORK_TYPE_BITMASK_LTE_CA") },
            "IWLAN" to lazy { getBitmask("NETWORK_TYPE_BITMASK_IWLAN") },
            "NR" to lazy { getBitmask("NETWORK_TYPE_BITMASK_NR") }
        )
        
        private fun getMask(key: String) = bitmasks[key]?.value ?: 0L
        
        private fun get2GBitmask(): Long {
            return getMask("GSM") or getMask("GPRS") or getMask("EDGE") or getMask("CDMA") or getMask("1xRTT")
        }
        
        private fun get3GBitmask(): Long {
            return getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B") or getMask("EHRPD") or 
                   getMask("HSUPA") or getMask("HSDPA") or getMask("HSPA") or getMask("HSPAP") or 
                   getMask("UMTS") or getMask("TD_SCDMA")
        }
        
        private fun get4GBitmask(): Long {
            return getMask("LTE") or getMask("LTE_CA") or getMask("IWLAN")
        }
        
        private fun get5GBitmask(): Long {
            return getMask("NR")
        }
        
        private fun mapNetworkModeToBitmask(networkMode: Int): Long {
            return when (networkMode) {
                0 -> get2GBitmask() or get3GBitmask()
                1 -> getMask("GSM")
                2 -> getMask("UMTS")
                3 -> get2GBitmask() or get3GBitmask()
                4 -> getMask("CDMA") or getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B")
                5 -> getMask("CDMA")
                6 -> getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B")
                7 -> get2GBitmask() or get3GBitmask() or get4GBitmask()
                8 -> getMask("LTE") or getMask("CDMA") or getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B")
                9 -> getMask("LTE") or get2GBitmask() or get3GBitmask()
                10 -> getMask("LTE") or getMask("CDMA") or getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B") or get2GBitmask() or get3GBitmask()
                11 -> getMask("LTE")
                12 -> getMask("LTE") or get3GBitmask()
                13 -> getMask("TD_SCDMA")
                14 -> getMask("TD_SCDMA") or getMask("UMTS")
                15 -> getMask("LTE") or getMask("TD_SCDMA")
                16 -> getMask("TD_SCDMA") or get2GBitmask()
                17 -> getMask("LTE") or getMask("TD_SCDMA") or get2GBitmask()
                18 -> getMask("TD_SCDMA") or get2GBitmask() or get3GBitmask()
                19 -> getMask("LTE") or getMask("TD_SCDMA") or get3GBitmask()
                20 -> getMask("LTE") or getMask("TD_SCDMA") or get2GBitmask() or get3GBitmask()
                21 -> getMask("TD_SCDMA") or getMask("CDMA") or getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B") or get2GBitmask() or get3GBitmask()
                22 -> getMask("LTE") or getMask("TD_SCDMA") or getMask("CDMA") or getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B") or get2GBitmask() or get3GBitmask()
                23 -> getMask("NR")
                24 -> getMask("NR") or getMask("LTE")
                25 -> getMask("NR") or getMask("LTE") or getMask("CDMA") or getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B")
                26 -> getMask("NR") or getMask("LTE") or get2GBitmask() or get3GBitmask()
                27 -> getMask("NR") or getMask("LTE") or getMask("CDMA") or getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B") or get2GBitmask() or get3GBitmask()
                28 -> getMask("NR") or getMask("LTE") or get3GBitmask()
                29 -> getMask("NR") or getMask("LTE") or getMask("TD_SCDMA")
                30 -> getMask("NR") or getMask("LTE") or getMask("TD_SCDMA") or get2GBitmask()
                31 -> getMask("NR") or getMask("LTE") or getMask("TD_SCDMA") or get3GBitmask()
                32 -> getMask("NR") or getMask("LTE") or getMask("TD_SCDMA") or get2GBitmask() or get3GBitmask()
                33 -> getMask("NR") or getMask("LTE") or getMask("TD_SCDMA") or getMask("CDMA") or getMask("EVDO_0") or getMask("EVDO_A") or getMask("EVDO_B") or get2GBitmask() or get3GBitmask()
                else -> get2GBitmask() or get3GBitmask() or get4GBitmask()
            }
        }
        
        private fun mapBitmaskToNetworkMode(bitmask: Long): Int {
            for (mode in 0..33) {
                if (bitmask == mapNetworkModeToBitmask(mode)) {
                    return mode
                }
            }
            
            return when {
                bitmask == getMask("NR") -> 23
                bitmask == getMask("LTE") -> 11
                bitmask == getMask("GSM") -> 1
                bitmask == getMask("UMTS") -> 2
                bitmask == getMask("TD_SCDMA") -> 13
                bitmask == (getMask("NR") or getMask("LTE")) -> 24
                (bitmask and getMask("NR")) != 0L -> 23
                (bitmask and getMask("LTE")) != 0L -> 11
                (bitmask and get3GBitmask()) != 0L -> 2
                (bitmask and get2GBitmask()) != 0L -> 1
                else -> 0
            }
        }
    }

    override fun onBind(intent: Intent) = object : IRootController.Stub() {
        
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
                // fail
            }
        }
    }
}
