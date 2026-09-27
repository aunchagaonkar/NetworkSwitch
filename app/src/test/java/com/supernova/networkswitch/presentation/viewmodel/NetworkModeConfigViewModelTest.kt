package com.supernova.networkswitch.presentation.viewmodel

import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.GetEffectiveSubscriptionIdUseCase
import com.supernova.networkswitch.domain.usecase.GetSimNameUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.usecase.UpdateToggleModeConfigUseCase
import com.supernova.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkModeConfigViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var getToggleModeConfig: GetToggleModeConfigUseCase
    private lateinit var updateToggleModeConfig: UpdateToggleModeConfigUseCase
    private lateinit var getEffectiveSubscriptionId: GetEffectiveSubscriptionIdUseCase
    private lateinit var getSimName: GetSimNameUseCase

    private val loadedSubId = 1
    private val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)

    @Before
    fun setUp() {
        getToggleModeConfig = mockk()
        updateToggleModeConfig = mockk(relaxed = true)
        getEffectiveSubscriptionId = mockk()
        getSimName = mockk()

        coEvery { getEffectiveSubscriptionId() } returns loadedSubId
        coEvery { getToggleModeConfig(loadedSubId) } returns config
        coEvery { getSimName(loadedSubId) } returns "Jio (Slot 1)"
    }

    private fun createViewModel() = NetworkModeConfigViewModel(
        getToggleModeConfig,
        updateToggleModeConfig,
        getEffectiveSubscriptionId,
        getSimName
    )

    @Test
    fun `should label the SIM being configured`() = runTest {
        val viewModel = createViewModel()

        assertEquals("Jio (Slot 1)", viewModel.simName.value)
    }

    /** The selected SIM can change from the Quick Settings tile while this screen is open. */
    @Test
    fun `should save to the SIM that was loaded even if the selection changes`() = runTest {
        val viewModel = createViewModel()
        coEvery { getEffectiveSubscriptionId() } returns 3

        viewModel.updateModeA(NetworkMode.WCDMA_ONLY)
        viewModel.saveConfiguration()

        coVerify { updateToggleModeConfig(loadedSubId, any()) }
        coVerify(exactly = 0) { updateToggleModeConfig(3, any()) }
    }

    @Test
    fun `should not save identical modes`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateModeB(NetworkMode.LTE_ONLY)
        viewModel.saveConfiguration()

        coVerify(exactly = 0) { updateToggleModeConfig(any(), any()) }
    }
}
