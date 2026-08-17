package com.supernova.networkswitch.service

import android.content.Context
import android.os.Build
import android.os.IBinder
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method

/**
 * Reads and writes the preferred network mode through `ITelephony`.
 *
 * `android.os.ServiceManager` and `com.android.internal.telephony.ITelephony` are
 * non-SDK APIs whose signatures drift between Android versions and OEM builds. Access
 * is reflective throughout so that a missing or changed method degrades to a logged
 * failure rather than a `NoSuchMethodError`.
 *
 * Must run in a privileged process — the root service or the Shizuku user service.
 * The setters require MODIFY_PHONE_STATE, which the app process does not hold.
 */
internal object TelephonyReflection {

    private const val TAG = "NetworkSwitch"
    private const val PHONE_PACKAGE = "com.android.phone"

    private val mapper get() = NetworkModeBitmaskMapper.platform
    private val reasonUser get() = NetworkModeBitmaskMapper.reasonUser

    /**
     * Exempts the telephony packages from non-SDK interface enforcement, once.
     *
     * Redundant in the Shizuku user service, which is documented as already exempt;
     * libsu gives no such guarantee for its root process. `addHiddenApiExemptions`
     * appends, where `setHiddenApiExemptions` would clear another library's entries.
     */
    private val hiddenApiExempted: Boolean by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@lazy true
        try {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/telephony/", "Lcom/android/internal/telephony/")
        } catch (e: Throwable) {
            Log.w(TAG, "Hidden API exemption failed; continuing since the process may already be exempt", e)
            false
        }
    }

    private fun getITelephony(caller: String): Any? = try {
        hiddenApiExempted // read to trigger the one-time exemption
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, Context.TELEPHONY_SERVICE) as? IBinder
        Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
    } catch (e: Exception) {
        Log.e(TAG, "$caller: Failed to get ITelephony", e)
        null
    }

    /** Returns the current RIL network mode, or -1 if it could not be determined. */
    fun getCurrentNetworkMode(subId: Int, caller: String): Int {
        try {
            val iTelephony = getITelephony(caller) ?: return -1
            val methods = iTelephony.javaClass.methods

            // Android 11+ reports an allowed-network-types bitmask.
            for (m in methods.named("getAllowedNetworkTypesForReason")) {
                try {
                    val bitmask = when {
                        m.parameterCount == 2 ->
                            m.invoke(iTelephony, subId, reasonUser) as Long
                        m.parameterCount == 3 && m.parameterTypes[2] == String::class.java ->
                            m.invoke(iTelephony, subId, reasonUser, PHONE_PACKAGE) as Long
                        else -> continue
                    }
                    val mode = mapper.toNetworkMode(bitmask)
                    Log.i(TAG, "$caller: getAllowedNetworkTypesForReason(subId=$subId) -> $mode (bitmask=$bitmask)")
                    return mode
                } catch (_: Exception) {
                    // Signature did not match after all; try the next overload.
                }
            }

            // Android 10 and below expose the RIL mode directly.
            for (m in methods.named("getPreferredNetworkType")) {
                try {
                    val mode = when {
                        m.parameterCount == 1 -> m.invoke(iTelephony, subId) as Int
                        m.parameterCount == 2 && m.parameterTypes[1] == String::class.java ->
                            m.invoke(iTelephony, subId, PHONE_PACKAGE) as Int
                        else -> continue
                    }
                    Log.i(TAG, "$caller: getPreferredNetworkType(subId=$subId) -> $mode")
                    return mode
                } catch (_: Exception) {
                    // Try the next overload.
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "$caller: Error in getCurrentNetworkMode", e)
        }
        return -1
    }

    /** Applies [networkMode], returning whether any of the three strategies took. */
    fun setNetworkMode(subId: Int, networkMode: Int, caller: String): Boolean {
        try {
            val iTelephony = getITelephony(caller) ?: return false
            val bitmask = mapper.toBitmask(networkMode)
            val methods = iTelephony.javaClass.methods

            for (m in methods.named("setAllowedNetworkTypesForReason")) {
                try {
                    when {
                        m.parameterCount == 3 ->
                            m.invoke(iTelephony, subId, reasonUser, bitmask)
                        m.parameterCount == 4 && m.parameterTypes[3] == String::class.java ->
                            m.invoke(iTelephony, subId, reasonUser, bitmask, PHONE_PACKAGE)
                        else -> continue
                    }
                    Log.i(TAG, "$caller: setAllowedNetworkTypesForReason(subId=$subId, mode=$networkMode) success")
                    return true
                } catch (_: Exception) {
                    // Try the next overload.
                }
            }

            for (m in methods.named("setAllowedNetworkTypes")) {
                try {
                    when {
                        m.parameterCount == 2 -> m.invoke(iTelephony, subId, bitmask)
                        m.parameterCount == 3 && m.parameterTypes[1].name == "long" ->
                            m.invoke(iTelephony, subId, bitmask, PHONE_PACKAGE)
                        else -> continue
                    }
                    Log.i(TAG, "$caller: setAllowedNetworkTypes(subId=$subId, bitmask=$bitmask) success")
                    return true
                } catch (_: Exception) {
                    // Try the next overload.
                }
            }

            for (m in methods.named("setPreferredNetworkType")) {
                if (m.parameterCount != 2) continue
                try {
                    m.invoke(iTelephony, subId, networkMode)
                    Log.i(TAG, "$caller: setPreferredNetworkType(subId=$subId, mode=$networkMode) success")
                    return true
                } catch (_: Exception) {
                    // Try the next overload.
                }
            }

            Log.w(TAG, "$caller: Failed to set network mode $networkMode for subId $subId")
        } catch (e: Throwable) {
            Log.e(TAG, "$caller: FATAL error in setNetworkMode", e)
        }
        return false
    }

    private fun Array<Method>.named(name: String) = filter { it.name == name }
}
