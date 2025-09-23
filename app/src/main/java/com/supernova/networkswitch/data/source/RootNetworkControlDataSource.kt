package com.supernova.networkswitch.data.source

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.supernova.networkswitch.IRootController
import com.supernova.networkswitch.service.RootNetworkControllerService
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.util.Utils
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Root-based network control data source
 */
@Singleton
class RootNetworkControlDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkControlDataSource {
    
    private var networkController: IRootController? = null
    private var isServiceConnected = false

    override suspend fun checkCompatibility(subId: Int): CompatibilityState {
        return if (!Utils.isRootGranted()) {
            CompatibilityState.PermissionDenied(com.supernova.networkswitch.domain.model.ControlMethod.ROOT)
        } else {
            CompatibilityState.Compatible
        }
    }

    override suspend fun getNetworkState(subId: Int): Boolean {
        val controller = getNetworkController()
        return controller?.getNetworkState(subId) ?: false
    }

    override suspend fun setNetworkState(subId: Int, enabled: Boolean) {
        val controller = getNetworkController()
        controller?.setNetworkState(subId, enabled)
    }

    override fun isConnected(): Boolean = isServiceConnected
    
    /**
     * Reset connection state - useful when switching control methods
     */
    fun resetConnection() {
        networkController = null
        isServiceConnected = false
    }

    private suspend fun getNetworkController(): IRootController? {
        if (networkController != null && isServiceConnected) {
            return networkController
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                var isResumed = false
                
                val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        if (!isResumed) {
                            isResumed = true
                            networkController = IRootController.Stub.asInterface(service)
                            isServiceConnected = true
                            continuation.resume(networkController)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        networkController = null
                        isServiceConnected = false
                    }
                }
                
                continuation.invokeOnCancellation {
                    isServiceConnected = false
                    networkController = null
                }
                
                try {
                    RootService.bind(
                        Intent(context, RootNetworkControllerService::class.java),
                        serviceConnection
                    )
                } catch (e: Exception) {
                    if (!isResumed) {
                        isResumed = true
                        continuation.resumeWithException(e)
                    }
                }
            }
        } catch (e: Exception) {
            isServiceConnected = false
            networkController = null
            throw e
        }
    }
}
