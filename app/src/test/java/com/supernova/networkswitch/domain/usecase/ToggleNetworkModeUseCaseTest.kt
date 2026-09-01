package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ToggleNetworkModeUseCase
 */
class ToggleNetworkModeUseCaseTest {

    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: ToggleNetworkModeUseCase

    private val testSubId = 1
    private val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)

    @Before
    fun setUp() {
        networkControlRepository = mockk()
        preferencesRepository = mockk()
        useCase = ToggleNetworkModeUseCase(networkControlRepository, preferencesRepository)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
    }

    @Test
    fun `should switch to mode B when the SIM is on mode A`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.success(Unit)

        val result = useCase(testSubId)

        assertEquals(NetworkMode.NR_ONLY, result.getOrNull())
        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) }
    }

    @Test
    fun `should switch to mode A when the SIM is on mode B`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.NR_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)

        val result = useCase(testSubId)

        assertEquals(NetworkMode.LTE_ONLY, result.getOrNull())
        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) }
    }

    /** A SIM already on mode B must not be told to switch to mode B. */
    @Test
    fun `should target each SIM from its own mode`() = runTest {
        val otherSubId = 2
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { networkControlRepository.getCurrentNetworkMode(otherSubId) } returns NetworkMode.NR_ONLY
        coEvery { networkControlRepository.setNetworkMode(any(), any()) } returns Result.success(Unit)

        assertEquals(NetworkMode.NR_ONLY, useCase(testSubId).getOrNull())
        assertEquals(NetworkMode.LTE_ONLY, useCase(otherSubId).getOrNull())
    }

    @Test
    fun `should target mode A when the current mode cannot be read`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns null
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)

        assertEquals(NetworkMode.LTE_ONLY, useCase(testSubId).getOrNull())
    }

    @Test
    fun `should target mode A when the SIM is on a mode outside the pair`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.NR_LTE_GSM_WCDMA
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)

        assertEquals(NetworkMode.LTE_ONLY, useCase(testSubId).getOrNull())
    }

    @Test
    fun `should propagate a failure to apply the mode`() = runTest {
        val failure = RuntimeException("binder died")
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.failure(failure)

        val result = useCase(testSubId)

        assertTrue(result.isFailure)
        assertEquals(failure, result.exceptionOrNull())
    }

    @Test
    fun `should report a thrown error as a failure`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } throws IllegalStateException("no service")

        val result = useCase(testSubId)

        assertTrue(result.isFailure)
    }

    /** The target is derived from the radio on each call, so a toggle persists nothing. */
    @Test
    fun `should not write the toggle configuration`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { networkControlRepository.setNetworkMode(any(), any()) } returns Result.success(Unit)

        useCase(testSubId)

        coVerify(exactly = 0) { preferencesRepository.setToggleModeConfig(any()) }
    }
}
