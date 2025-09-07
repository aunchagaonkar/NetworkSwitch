package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NetworkUseCasesTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var checkCompatibilityUseCase: CheckCompatibilityUseCase
    private lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase
    private lateinit var getNetworkStateUseCase: GetNetworkStateUseCase
    private lateinit var updateControlMethodUseCase: UpdateControlMethodUseCase
    private lateinit var resetConnectionsUseCase: ResetConnectionsUseCase

    @Before
    fun setUp() {
        networkControlRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)

        checkCompatibilityUseCase = CheckCompatibilityUseCase(networkControlRepository, preferencesRepository)
        toggleNetworkModeUseCase = ToggleNetworkModeUseCase(networkControlRepository)
        getNetworkStateUseCase = GetNetworkStateUseCase(networkControlRepository)
        updateControlMethodUseCase = UpdateControlMethodUseCase(preferencesRepository)
        resetConnectionsUseCase = ResetConnectionsUseCase(networkControlRepository)
    }

    @Test
    fun `CheckCompatibilityUseCase returns state from repository`() = runTest {
        val expectedState = CompatibilityState.Compatible
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.ROOT
        coEvery { networkControlRepository.checkCompatibility(any()) } returns expectedState

        val result = checkCompatibilityUseCase()

        assertEquals(expectedState, result)
    }

    @Test
    fun `ToggleNetworkModeUseCase toggles from true to false`() = runTest {
        coEvery { networkControlRepository.getFivegEnabled(any()) } returns true
        coEvery { networkControlRepository.setFivegEnabled(any(), false) } returns Result.success(Unit)

        val result = toggleNetworkModeUseCase(1)

        coVerify { networkControlRepository.setFivegEnabled(1, false) }
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `ToggleNetworkModeUseCase toggles from false to true`() = runTest {
        coEvery { networkControlRepository.getFivegEnabled(any()) } returns false
        coEvery { networkControlRepository.setFivegEnabled(any(), true) } returns Result.success(Unit)

        val result = toggleNetworkModeUseCase(1)

        coVerify { networkControlRepository.setFivegEnabled(1, true) }
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `ToggleNetworkModeUseCase handles failure`() = runTest {
        val exception = RuntimeException("Test Exception")
        coEvery { networkControlRepository.getFivegEnabled(any()) } throws exception

        val result = toggleNetworkModeUseCase(1)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `GetNetworkStateUseCase returns state from repository`() = runTest {
        coEvery { networkControlRepository.getFivegEnabled(any()) } returns true

        val result = getNetworkStateUseCase(1)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `GetNetworkStateUseCase handles failure`() = runTest {
        val exception = RuntimeException("Test Exception")
        coEvery { networkControlRepository.getFivegEnabled(any()) } throws exception

        val result = getNetworkStateUseCase(1)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `UpdateControlMethodUseCase calls repository`() = runTest {
        val method = ControlMethod.SHIZUKU
        updateControlMethodUseCase(method)
        coVerify { preferencesRepository.setControlMethod(method) }
    }

    @Test
    fun `ResetConnectionsUseCase calls repository`() = runTest {
        resetConnectionsUseCase()
        coVerify { networkControlRepository.resetConnections() }
    }
}
