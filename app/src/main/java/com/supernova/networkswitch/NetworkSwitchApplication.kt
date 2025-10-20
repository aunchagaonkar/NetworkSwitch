package com.supernova.networkswitch

import android.app.Application
import com.supernova.networkswitch.data.source.PreferencesDataSource
import com.supernova.networkswitch.presentation.LauncherIcon
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class NetworkSwitchApplication : Application() {

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        observeLauncherIconState()
    }

    private fun observeLauncherIconState() {
        preferencesDataSource.observeHideLauncherIcon()
            .onEach { hide ->
                LauncherIcon.setEnabled(this, !hide)
            }
            .launchIn(applicationScope)
    }
}