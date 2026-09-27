package com.supernova.networkswitch.data.source

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.supernova.networkswitch.IShizukuController
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.service.ShizukuControllerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import com.supernova.networkswitch.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class  ShizukuNetworkControlDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkControlDataSource {
    
    @Volatile
    private var userService: IShizukuController? = null
    private val _isConnected = MutableStateFlow(false)
    
    private companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_ID = 8

        /** bindUserService never calls back if the service cannot start. */
        private const val BIND_TIMEOUT_MS = 5_000L
    }

    override suspend fun checkCompatibility(subId: Int): CompatibilityState {
        return try {
            // Check if Shizuku service is running
            if (!Shizuku.pingBinder()) {
                // Any cached binder belongs to the process that just went away.
                resetConnection()
                return CompatibilityState.Incompatible("Shizuku service not running")
            }
            
            // Check if Shizuku permission is granted
            val permission = try {
                Shizuku.checkSelfPermission()
            } catch (e: Exception) {
                PackageManager.PERMISSION_DENIED
            }
            
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return CompatibilityState.PermissionDenied(com.supernova.networkswitch.domain.model.ControlMethod.SHIZUKU)
            }

            // A reachable Shizuku with permission still does not mean the user service
            // is bound, and reads go through that binding.
            if (!ensureServiceBinding()) {
                return CompatibilityState.Incompatible("Shizuku user service unavailable")
            }

            CompatibilityState.Compatible
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CompatibilityState.Incompatible("Shizuku not available: ${e.message}")
        }
    }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? {
        return if (ensureServiceBinding()) {
            try {
                val modeValue = userService?.getCurrentNetworkMode(subId) ?: -1
                if (modeValue == -1) null else NetworkMode.fromValue(modeValue)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode) {
        if (ensureServiceBinding()) {
            val service = userService
                ?: throw IllegalStateException("Shizuku user service unavailable")
            service.setNetworkMode(subId, mode.value)
        } else {
            throw SecurityException("Shizuku permission not granted or service binding failed")
        }
    }


    private fun resetConnection() {
        userService = null
        _isConnected.value = false
    }

    /**
     * Ensure service binding for actual operations (not compatibility checks)
     */
    private suspend fun ensureServiceBinding(): Boolean {
        // Check permissions first
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        
        // onServiceDisconnected is not guaranteed to have run yet, so trust the binder
        // itself rather than the connected flag.
        val cached = userService
        if (cached != null && _isConnected.value && cached.asBinder()?.pingBinder() == true) {
            return true
        }
        resetConnection()
        
        return try {
            withTimeoutOrNull(BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val args = Shizuku.UserServiceArgs(ComponentName(context, ShizukuControllerService::class.java))
                    .processNameSuffix("service")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE)
                    .tag("NetworkSwitch")

                val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
                        if (binder != null && binder.pingBinder()) {
                            userService = IShizukuController.Stub.asInterface(binder)
                            _isConnected.value = true
                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }
                    }

                    override fun onServiceDisconnected(componentName: ComponentName?) {
                        userService = null
                        _isConnected.value = false
                    }
                }

                try {
                    Shizuku.bindUserService(args, serviceConnection)
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }
            } ?: false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _isConnected.value = false
            false
        }
    }
}
