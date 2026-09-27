package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.SimConfigKey
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.model.ToggleState
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ToggleNetworkModeUseCaseTest {

    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: ToggleNetworkModeUseCase

    private val testSubId = 1
    private val simKey = SimConfigKey.forSubscription(testSubId)
    private val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)

    @Before
    fun setUp() {
        networkControlRepository = mockk()
        preferencesRepository = mockk()
        useCase = ToggleNetworkModeUseCase(networkControlRepository, preferencesRepository)
        coEvery { preferencesRepository.getToggleModeConfig(any()) } returns config
        coEvery { preferencesRepository.getLastAppliedMode(any()) } returns null
        coEvery { preferencesRepository.setLastAppliedMode(any(), any()) } just runs
        coEvery { networkControlRepository.setNetworkMode(any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `should switch to mode B when the SIM is on mode A`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY

        val result = useCase(testSubId)

        assertEquals(ToggleState(NetworkMode.NR_ONLY, NetworkMode.LTE_ONLY), result.getOrNull())
        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) }
    }

    @Test
    fun `should switch to mode A when the SIM is on mode B`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.NR_ONLY

        val result = useCase(testSubId)

        assertEquals(ToggleState(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY), result.getOrNull())
        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) }
    }

    @Test
    fun `should leave a mode A of GSM_UMTS that reads back as WCDMA_PREF`() = runTest {
        coEvery { preferencesRepository.getToggleModeConfig(simKey) } returns
            ToggleModeConfig(NetworkMode.GSM_UMTS, NetworkMode.LTE_ONLY)
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.WCDMA_PREF

        useCase(testSubId)

        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) }
    }

    @Test
    fun `should record the applied mode`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY

        useCase(testSubId)

        coVerify { preferencesRepository.setLastAppliedMode(simKey, NetworkMode.NR_ONLY) }
    }

    @Test
    fun `should alternate from the last applied mode when the mode is unreadable`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns null
        coEvery { preferencesRepository.getLastAppliedMode(simKey) } returns NetworkMode.LTE_ONLY

        useCase(testSubId)

        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) }
    }

    @Test
    fun `should target each SIM from its own mode`() = runTest {
        val otherSubId = 2
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { networkControlRepository.getCurrentNetworkMode(otherSubId) } returns NetworkMode.NR_ONLY

        useCase(testSubId)
        useCase(otherSubId)

        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) }
        coVerify { networkControlRepository.setNetworkMode(otherSubId, NetworkMode.LTE_ONLY) }
    }

    @Test
    fun `should propagate a failure to apply the mode without recording it`() = runTest {
        val failure = RuntimeException("binder died")
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.failure(failure)

        val result = useCase(testSubId)

        assertEquals(failure, result.exceptionOrNull())
        coVerify(exactly = 0) { preferencesRepository.setLastAppliedMode(any(), any()) }
    }

    @Test
    fun `should still succeed when the applied mode cannot be recorded`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { preferencesRepository.setLastAppliedMode(any(), any()) } throws IOException("disk full")

        val result = useCase(testSubId)

        assertEquals(ToggleState(NetworkMode.NR_ONLY, NetworkMode.LTE_ONLY), result.getOrNull())
    }

    @Test
    fun `should not turn cancellation into a failure`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { preferencesRepository.setLastAppliedMode(any(), any()) } throws CancellationException("timed out")

        val thrown = runCatching { useCase(testSubId) }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `should report a thrown error as a failure`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } throws IllegalStateException("no service")

        assertTrue(useCase(testSubId).isFailure)
    }

    @Test
    fun `should not write the toggle configuration`() = runTest {
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY

        useCase(testSubId)

        coVerify(exactly = 0) { preferencesRepository.setToggleModeConfig(any(), any()) }
    }
}
