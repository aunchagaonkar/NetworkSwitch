package com.supernova.networkswitch.util

import com.topjohnwu.superuser.Shell

object Utils {
    fun isRootGranted(): Boolean {
        Shell.getShell()
        return Shell.isAppGrantedRoot() == true
    }
}
