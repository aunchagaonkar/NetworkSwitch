package com.supernova.networkswitch.data.source

import android.content.ServiceConnection
import android.os.IBinder
import com.supernova.networkswitch.IRootController
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.util.Utils
import com.topjohnwu.superuser.ipc.RootService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RootNetworkControlDataSourceTest {

    private val dispatcher = StandardTestDispatcher()
    private val connection = slot<ServiceConnection>()
    private lateinit var source: RootNetworkControlDataSource

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils)
        every { Utils.isRootGranted() } returns true
        mockkStatic(RootService::class)
        source = RootNetworkControlDataSource(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun test(body: suspend TestScope.() -> Unit) = runTest(dispatcher) { body() }

    private fun controller(alive: Boolean = true): IRootController {
        val binder = mockk<IBinder> { every { pingBinder() } returns alive }
        return mockk(relaxed = true) { every { asBinder() } returns binder }
    }

    private fun serviceFor(controller: IRootController): IBinder =
        mockk { every { queryLocalInterface(any()) } returns controller }

    private fun bindNeverCallsBack() {
        every { RootService.bind(any(), capture(connection)) } just runs
    }

    private fun bindConnects(vararg controllers: IRootController) {
        val queue = ArrayDeque(controllers.toList())
        every { RootService.bind(any(), capture(connection)) } answers {
            connection.captured.onServiceConnected(null, serviceFor(queue.removeFirst()))
        }
    }

    @Test
    fun `should report permission denied without binding when root is not granted`() = test {
        every { Utils.isRootGranted() } returns false

        assertEquals(CompatibilityState.PermissionDenied(ControlMethod.ROOT), source.checkCompatibility(1))
        verify(exactly = 0) { RootService.bind(any(), any()) }
    }

    @Test
    fun `should report incompatible when the bind never calls back`() = test {
        bindNeverCallsBack()

        assertTrue(source.checkCompatibility(1) is CompatibilityState.Incompatible)
    }

    @Test
    fun `should fail a set when the service cannot be bound`() = test {
        bindNeverCallsBack()

        val thrown = runCatching { source.setNetworkMode(1, NetworkMode.LTE_ONLY) }.exceptionOrNull()

        assertEquals(IllegalStateException::class.java, thrown?.javaClass)
    }

    @Test
    fun `should pass a set through to the bound service`() = test {
        val controller = controller()
        bindConnects(controller)

        source.setNetworkMode(1, NetworkMode.LTE_ONLY)

        verify { controller.setNetworkMode(1, NetworkMode.LTE_ONLY.value) }
    }

    @Test
    fun `should reuse the first caller's connection for a caller waiting on the lock`() = test {
        val controller = controller()
        every { controller.getCurrentNetworkMode(1) } returns NetworkMode.LTE_ONLY.value
        bindNeverCallsBack()

        val first = async { source.getCurrentNetworkMode(1) }
        val second = async { source.getCurrentNetworkMode(1) }
        runCurrent()
        connection.captured.onServiceConnected(null, serviceFor(controller))

        assertEquals(NetworkMode.LTE_ONLY, first.await())
        assertEquals(NetworkMode.LTE_ONLY, second.await())
        verify(exactly = 1) { RootService.bind(any(), any()) }
    }

    @Test
    fun `should wait on a bind left in flight by a cancelled caller`() = test {
        val controller = controller()
        every { controller.getCurrentNetworkMode(1) } returns NetworkMode.LTE_ONLY.value
        bindNeverCallsBack()

        val cancelled = launch { source.getCurrentNetworkMode(1) }
        runCurrent()
        verify(exactly = 1) { RootService.bind(any(), any()) }
        cancelled.cancel()
        val retry = async { source.getCurrentNetworkMode(1) }
        runCurrent()
        connection.captured.onServiceConnected(null, serviceFor(controller))

        assertEquals(NetworkMode.LTE_ONLY, retry.await())
        verify(exactly = 1) { RootService.bind(any(), any()) }
    }

    @Test
    fun `should rebind when the cached service has died`() = test {
        bindConnects(controller(alive = false), controller())

        source.getCurrentNetworkMode(1)
        source.getCurrentNetworkMode(1)

        verify(exactly = 2) { RootService.bind(any(), any()) }
    }

    @Test
    fun `should bind afresh after a timeout and still reuse a late connection`() = test {
        val controller = controller()
        every { controller.getCurrentNetworkMode(1) } returns NetworkMode.LTE_ONLY.value
        bindNeverCallsBack()

        assertEquals(null, source.getCurrentNetworkMode(1))
        val first = connection.captured
        assertEquals(null, source.getCurrentNetworkMode(1))
        first.onServiceConnected(null, serviceFor(controller))

        assertEquals(NetworkMode.LTE_ONLY, source.getCurrentNetworkMode(1))
        verify(exactly = 2) { RootService.bind(any(), any()) }
    }
}
