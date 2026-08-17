package com.supernova.networkswitch.service

import android.content.Intent
import com.supernova.networkswitch.IRootController
import com.topjohnwu.superuser.ipc.RootService

/** libsu root service for network control. Runs as root in a separate process. */
class RootNetworkControllerService : RootService() {

    override fun onBind(intent: Intent) = object : IRootController.Stub() {

        override fun compatibilityCheck(subId: Int): Boolean =
            getCurrentNetworkMode(subId) != -1

        override fun getCurrentNetworkMode(subId: Int): Int =
            TelephonyReflection.getCurrentNetworkMode(subId, CALLER)

        override fun setNetworkMode(subId: Int, networkMode: Int) {
            TelephonyReflection.setNetworkMode(subId, networkMode, CALLER)
        }
    }

    private companion object {
        const val CALLER = "Root"
    }
}
