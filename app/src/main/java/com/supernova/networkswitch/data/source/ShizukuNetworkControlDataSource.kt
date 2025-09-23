package com.supernova.networkswitch.data.source

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.supernova.networkswitch.IShizukuController
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.service.ShizukuControllerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import com.supernova.networkswitch.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Shizuku-based network control data source
 */
@Singleton
class  ShizukuNetworkControlDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkControlDataSource {
    
    private var userService: IShizukuController? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
    
    private companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_ID = 8
    }

    override suspend fun checkCompatibility(subId: Int): CompatibilityState {
        return try {
            // Check if Shizuku service is running
            if (!Shizuku.pingBinder()) {
                return CompatibilityState.Incompatible("Shizuku service not running")
            }
            
            // Check if Shizuku permission is granted
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return CompatibilityState.PermissionDenied(com.supernova.networkswitch.domain.model.ControlMethod.SHIZUKU)
            }
            
            // Both service and permission checks passed
            CompatibilityState.Compatible
        } catch (e: Exception) {
            CompatibilityState.Incompatible("Shizuku not available: ${e.message}")
        }
    }

    override suspend fun getNetworkState(subId: Int): Boolean {
        return if (ensureServiceBinding()) {
            try {
                userService?.getNetworkState(subId) ?: false
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override suspend fun setNetworkState(subId: Int, enabled: Boolean) {
        if (ensureServiceBinding()) {
            try {
                userService?.setNetworkState(subId, enabled)
            } catch (e: Exception) {
                throw e
            }
        } else {
            throw SecurityException("Shizuku permission not granted or service binding failed")
        }
    }

    override fun isConnected(): Boolean = _isConnected.value
    
    /**
     * Reset connection state - useful when switching control methods
     */
    fun resetConnection() {
        userService = null
        _isConnected.value = false
    }

    /**
     * Simple permission and service check without delays or complex logic
     */
    private fun hasPermissionAndService(): Boolean {
        return try {
            // Only check if service is already connected, don't call Shizuku APIs that might block
            userService != null && _isConnected.value
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ensure service binding for actual operations (not compatibility checks)
     */
    private suspend fun ensureServiceBinding(): Boolean {
        // Check permissions first
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        
        // If service is already connected, return true
        if (userService != null && _isConnected.value) {
            return true
        }
        
        // Bind service asynchronously with timeout
        return try {
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

                continuation.invokeOnCancellation {
                    userService = null
                    _isConnected.value = false
                }

                try {
                    Shizuku.bindUserService(args, serviceConnection)
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }
        } catch (e: Exception) {
            _isConnected.value = false
            false
        }
    }
}
