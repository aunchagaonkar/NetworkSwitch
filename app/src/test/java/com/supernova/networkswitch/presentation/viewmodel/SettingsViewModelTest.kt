package com.supernova.networkswitch.presentation.viewmodel

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.GetAvailableSimsUseCase
import com.supernova.networkswitch.domain.usecase.GetSelectedSubscriptionIdUseCase
import com.supernova.networkswitch.domain.usecase.SetSelectedSubscriptionIdUseCase
import com.supernova.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var getAvailableSimsUseCase: GetAvailableSimsUseCase
    private lateinit var getSelectedSubscriptionIdUseCase: GetSelectedSubscriptionIdUseCase
    private lateinit var setSelectedSubscriptionIdUseCase: SetSelectedSubscriptionIdUseCase

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        preferencesRepository = mockk(relaxed = true)
        networkControlRepository = mockk(relaxed = true)
        getAvailableSimsUseCase = mockk(relaxed = true)
        getSelectedSubscriptionIdUseCase = mockk(relaxed = true)
        setSelectedSubscriptionIdUseCase = mockk(relaxed = true)

        coEvery { preferencesRepository.observeControlMethod() } returns flowOf(ControlMethod.SHIZUKU)
        coEvery { preferencesRepository.observeSelectedSubscriptionId() } returns flowOf(-1)
        coEvery { getAvailableSimsUseCase() } returns Result.success(emptyList())
        coEvery { getSelectedSubscriptionIdUseCase() } returns -1
    }

    private fun createViewModel() {
        viewModel = SettingsViewModel(
            preferencesRepository,
            networkControlRepository,
            getAvailableSimsUseCase,
            getSelectedSubscriptionIdUseCase,
            setSelectedSubscriptionIdUseCase
        )
    }

    @Test
    fun `init calls checkAllCompatibility`() = runTest {
        createViewModel()
        coVerify { networkControlRepository.checkCompatibility(ControlMethod.ROOT) }
        coVerify { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
    }

    @Test
    fun `checkAllCompatibility updates compatibility states`() = runTest {
        val rootState = CompatibilityState.Compatible
        val shizukuState = CompatibilityState.Incompatible("Test Reason")
        coEvery { networkControlRepository.checkCompatibility(ControlMethod.ROOT) } returns rootState
        coEvery { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) } returns shizukuState

        createViewModel()

        assertEquals(rootState, viewModel.rootCompatibility)
        assertEquals(shizukuState, viewModel.shizukuCompatibility)
    }

    @Test
    fun `updateControlMethod calls repository`() = runTest {
        createViewModel()
        val method = ControlMethod.ROOT
        viewModel.updateControlMethod(method)
        coVerify { preferencesRepository.setControlMethod(method) }
    }

    @Test
    fun `retryCompatibilityCheck calls checkAllCompatibility`() = runTest {
        createViewModel()
        viewModel.retryCompatibilityCheck()
        coVerify(exactly = 2) { networkControlRepository.checkCompatibility(ControlMethod.ROOT) }
        coVerify(exactly = 2) { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
    }

    @Test
    fun `clearSimError resets simError state`() = runTest {
        createViewModel()
        // Manually set error by trying to select invalid SIM
        viewModel.selectSim(999) // Invalid SIM ID
        viewModel.clearSimError()
        assertEquals(null, viewModel.simError.value)
    }

    @Test
    fun `selectSim sets error for invalid SIM`() = runTest {
        // Mock available SIMs to contain only simId = 1
        coEvery { getAvailableSimsUseCase() } returns Result.success(
            listOf(SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1"))
        )
        createViewModel()
        val invalidSimId = 999
        viewModel.selectSim(invalidSimId)
        assertEquals("Selected SIM is not available", viewModel.simError.value)
    }

    @Test
    fun `selectSim clears error for valid SIM`() = runTest {
        val validSimId = 1
        // Mock available SIMs to contain simId = 1
        coEvery { getAvailableSimsUseCase() } returns Result.success(
            listOf(SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1"))
        )
        createViewModel()
        viewModel.selectSim(validSimId)
        assertEquals(null, viewModel.simError.value)
    }
}
