package com.supernova.networkswitch

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.lsposed.hiddenapibypass.HiddenApiBypass
import android.os.Build

@HiltAndroidApp
class NetworkSwitchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("Landroid/telephony/", "Lcom/android/internal/telephony/")
        }
    }
}
