package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.SimConfigKey
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.model.ToggleState
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetToggleStateUseCaseTest {

    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: GetToggleStateUseCase

    private val subId = 3
    private val simKey = SimConfigKey.forSubscription(subId)

    @Before
    fun setUp() {
        networkControlRepository = mockk()
        preferencesRepository = mockk()
        useCase = GetToggleStateUseCase(networkControlRepository, preferencesRepository)
        coEvery { preferencesRepository.getToggleModeConfig(simKey) } returns
            ToggleModeConfig(NetworkMode.GSM_UMTS, NetworkMode.LTE_ONLY)
        coEvery { preferencesRepository.getLastAppliedMode(simKey) } returns null
    }

    @Test
    fun `should report the configured mode when the radio reads back an equivalent one`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(subId) } returns NetworkMode.WCDMA_PREF

        assertEquals(ToggleState(NetworkMode.GSM_UMTS, NetworkMode.LTE_ONLY), useCase(subId))
    }

    @Test
    fun `should use this SIM's last applied mode when the mode is unreadable`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(subId) } returns null
        coEvery { preferencesRepository.getLastAppliedMode(simKey) } returns NetworkMode.GSM_UMTS

        assertEquals(ToggleState(null, NetworkMode.LTE_ONLY), useCase(subId))
    }
}
