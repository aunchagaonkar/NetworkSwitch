package com.supernova.networkswitch.presentation

import android.content.Context
import android.content.pm.PackageManager
import com.supernova.networkswitch.BuildConfig

object LauncherIcon {

    private val LAUNCHER_ACTIVITY_ALIAS = if (BuildConfig.DEBUG) {
        "com.supernova.networkswitch.debug.presentation.ui.activity.Launcher"
    } else {
        "com.supernova.networkswitch.presentation.ui.activity.Launcher"
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val componentName = android.content.ComponentName(
            context,
            LAUNCHER_ACTIVITY_ALIAS
        )

        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        context.packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }
}