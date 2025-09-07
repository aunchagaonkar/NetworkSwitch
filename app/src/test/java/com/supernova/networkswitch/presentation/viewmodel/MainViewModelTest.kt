package com.supernova.networkswitch.presentation.viewmodel

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.*
import com.supernova.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import android.telephony.SubscriptionManager
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var checkCompatibilityUseCase: CheckCompatibilityUseCase
    private lateinit var getNetworkStateUseCase: GetNetworkStateUseCase
    private lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase
    private lateinit var updateControlMethodUseCase: UpdateControlMethodUseCase
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        checkCompatibilityUseCase = mockk()
        getNetworkStateUseCase = mockk()
        toggleNetworkModeUseCase = mockk()
        updateControlMethodUseCase = mockk()
        preferencesRepository = mockk()

        mockkStatic(SubscriptionManager::class)
        every { SubscriptionManager.getDefaultDataSubscriptionId() } returns 1

        coEvery { preferencesRepository.observeControlMethod() } returns flowOf(ControlMethod.SHIZUKU)
        coEvery { checkCompatibilityUseCase() } returns CompatibilityState.Pending
        coEvery { getNetworkStateUseCase(any()) } returns Result.success(false)
        coEvery { updateControlMethodUseCase(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createViewModel() {
        viewModel = MainViewModel(
            checkCompatibilityUseCase,
            getNetworkStateUseCase,
            toggleNetworkModeUseCase,
            updateControlMethodUseCase,
            preferencesRepository
        )
    }

    @Test
    fun `init calls required methods`() = runTest {
        createViewModel()
        coVerify { preferencesRepository.observeControlMethod() }
        coVerify { checkCompatibilityUseCase() }
        coVerify { getNetworkStateUseCase(any()) }
    }

    @Test
    fun `toggleNetworkMode success updates networkState`() = runTest {
        coEvery { toggleNetworkModeUseCase(any()) } returns Result.success(true)
        createViewModel()

        viewModel.toggleNetworkMode()

        assertTrue(viewModel.networkState)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `toggleNetworkMode failure refreshes network state`() = runTest {
        coEvery { toggleNetworkModeUseCase(any()) } returns Result.failure(Exception())
        coEvery { getNetworkStateUseCase(any()) } returns Result.success(false)
        createViewModel()

        viewModel.toggleNetworkMode()

        assertFalse(viewModel.networkState)
        assertFalse(viewModel.isLoading)
        coVerify(exactly = 2) { getNetworkStateUseCase(any()) } // Initial + refresh
    }

    @Test
    fun `retryCompatibilityCheck calls use case`() = runTest {
        createViewModel()
        viewModel.retryCompatibilityCheck()
        coVerify(exactly = 2) { checkCompatibilityUseCase() } // Initial + retry
    }

    @Test
    fun `refreshAllData calls required methods`() = runTest {
        createViewModel()
        viewModel.refreshAllData()
        coVerify(exactly = 2) { checkCompatibilityUseCase() } // Initial + refresh
        coVerify(exactly = 2) { getNetworkStateUseCase(any()) } // Initial + refresh
    }

    @Test
    fun `switchToMethod calls use case`() = runTest {
        createViewModel()
        viewModel.switchToMethod(ControlMethod.ROOT)
        coVerify { updateControlMethodUseCase(ControlMethod.ROOT) }
    }

    @Test
    fun `compatibilityState is updated on check`() = runTest {
        val expectedState = CompatibilityState.Compatible
        coEvery { checkCompatibilityUseCase() } returns expectedState

        createViewModel()

        assertEquals(expectedState, viewModel.compatibilityState)
    }
}
