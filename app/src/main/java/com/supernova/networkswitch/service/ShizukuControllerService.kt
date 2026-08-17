package com.supernova.networkswitch.service

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.supernova.networkswitch.IShizukuController

/**
 * Shizuku user service for network control.
 *
 * Runs as shell (or root) in a separate process that Shizuku starts via app_process,
 * which is not subject to non-SDK interface restrictions.
 */
class ShizukuControllerService() : IShizukuController.Stub() {

    /** Shizuku instantiates the service through this constructor. */
    @Keep
    constructor(context: Context) : this()

    override fun compatibilityCheck(subId: Int): Boolean =
        getCurrentNetworkMode(subId) != -1

    override fun getCurrentNetworkMode(subId: Int): Int =
        TelephonyReflection.getCurrentNetworkMode(subId, CALLER)

    override fun setNetworkMode(subId: Int, networkMode: Int) {
        TelephonyReflection.setNetworkMode(subId, networkMode, CALLER)
    }

    override fun destroy() {
        Log.d(TAG, "ShizukuControllerService: destroy")
    }

    private companion object {
        const val TAG = "NetworkSwitch"
        const val CALLER = "Shizuku"
    }
}
