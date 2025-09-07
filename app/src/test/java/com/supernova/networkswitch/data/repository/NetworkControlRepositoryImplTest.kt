package com.supernova.networkswitch.data.repository

import com.supernova.networkswitch.data.source.RootNetworkControlDataSource
import com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import android.telephony.SubscriptionManager
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NetworkControlRepositoryImplTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var rootDataSource: RootNetworkControlDataSource
    private lateinit var shizukuDataSource: ShizukuNetworkControlDataSource
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var repository: NetworkControlRepositoryImpl

    @Before
    fun setUp() {
        rootDataSource = mockk(relaxed = true)
        shizukuDataSource = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)

        mockkStatic(SubscriptionManager::class)
        every { SubscriptionManager.getDefaultDataSubscriptionId() } returns 1

        repository = NetworkControlRepositoryImpl(
            rootDataSource,
            shizukuDataSource,
            preferencesRepository
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `checkCompatibility uses RootDataSource when method is ROOT`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.ROOT

        repository.checkCompatibility(ControlMethod.ROOT)

        coVerify { rootDataSource.checkCompatibility(1) }
        coVerify(exactly = 0) { shizukuDataSource.checkCompatibility(any()) }
    }

    @Test
    fun `checkCompatibility uses ShizukuDataSource when method is SHIZUKU`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.SHIZUKU

        repository.checkCompatibility(ControlMethod.SHIZUKU)

        coVerify { shizukuDataSource.checkCompatibility(1) }
        coVerify(exactly = 0) { rootDataSource.checkCompatibility(any()) }
    }

    @Test
    fun `getFivegEnabled uses RootDataSource when method is ROOT`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.ROOT
        val subId = 1

        repository.getFivegEnabled(subId)

        coVerify { rootDataSource.getFivegEnabled(subId) }
        coVerify(exactly = 0) { shizukuDataSource.getFivegEnabled(any()) }
    }

    @Test
    fun `getFivegEnabled uses ShizukuDataSource when method is SHIZUKU`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.SHIZUKU
        val subId = 1

        repository.getFivegEnabled(subId)

        coVerify { shizukuDataSource.getFivegEnabled(subId) }
        coVerify(exactly = 0) { rootDataSource.getFivegEnabled(any()) }
    }

    @Test
    fun `setFivegEnabled uses RootDataSource when method is ROOT`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.ROOT
        val subId = 1
        val enabled = true

        repository.setFivegEnabled(subId, enabled)

        coVerify { rootDataSource.setFivegEnabled(subId, enabled) }
        coVerify(exactly = 0) { shizukuDataSource.setFivegEnabled(any(), any()) }
    }

    @Test
    fun `setFivegEnabled uses ShizukuDataSource when method is SHIZUKU`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.SHIZUKU
        val subId = 1
        val enabled = true

        repository.setFivegEnabled(subId, enabled)

        coVerify { shizukuDataSource.setFivegEnabled(subId, enabled) }
        coVerify(exactly = 0) { rootDataSource.setFivegEnabled(any(), any()) }
    }

    @Test
    fun `resetConnections calls reset on both data sources`() = runTest {
        repository.resetConnections()

        coVerify { rootDataSource.resetConnection() }
        coVerify { shizukuDataSource.resetConnection() }
    }
}
