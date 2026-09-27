package com.supernova.networkswitch.data.source

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.supernova.networkswitch.IRootController
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.service.RootNetworkControllerService
import com.supernova.networkswitch.util.Utils
import com.topjohnwu.superuser.ipc.RootService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootNetworkControlDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkControlDataSource {

    @Volatile
    private var networkController: IRootController? = null
    /** Only accessed on the main thread. */
    private var pendingBind: CompletableDeferred<IRootController?>? = null
    private val bindLock = Mutex()

    private companion object {
        /** RootService.bind never calls back if the root process cannot start. */
        private const val BIND_TIMEOUT_MS = 5_000L
    }

    override suspend fun checkCompatibility(subId: Int): CompatibilityState {
        // Blocks until the su prompt is answered.
        val rootGranted = withContext(Dispatchers.IO) { Utils.isRootGranted() }
        if (!rootGranted) {
            return CompatibilityState.PermissionDenied(ControlMethod.ROOT)
        }

        // Root access alone does not mean the root service can bind.
        return if (getNetworkController() != null) {
            CompatibilityState.Compatible
        } else {
            CompatibilityState.Incompatible("Root service unavailable")
        }
    }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? {
        val controller = getNetworkController() ?: return null
        return try {
            val modeValue = controller.getCurrentNetworkMode(subId)
            if (modeValue == -1) null else NetworkMode.fromValue(modeValue)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode) {
        val controller = getNetworkController()
            ?: throw IllegalStateException("Root service unavailable")
        controller.setNetworkMode(subId, mode.value)
    }

    private suspend fun getNetworkController(): IRootController? = bindLock.withLock {
        networkController?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        networkController = null

        // libsu only binds on the main thread.
        withContext(Dispatchers.Main) {
            // libsu queues binds while the root process starts and runs them once it reports in.
            // It never relaunches a process that died; the next bind does, if it had reported in.
            val bind = pendingBind?.takeUnless { it.isCompleted } ?: startBind()
            val controller = withTimeoutOrNull(BIND_TIMEOUT_MS) { bind.await() }
            if (controller == null) pendingBind = null
            controller
        }
    }

    private fun startBind(): CompletableDeferred<IRootController?> {
        val bind = CompletableDeferred<IRootController?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                // Runs even after the waiting call timed out, so a late connection is reused.
                val controller = IRootController.Stub.asInterface(service)
                networkController = controller
                bind.complete(controller)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                networkController = null
            }
        }
        pendingBind = bind

        try {
            RootService.bind(Intent(context, RootNetworkControllerService::class.java), connection)
        } catch (e: Exception) {
            bind.complete(null)
        }
        return bind
    }
}
