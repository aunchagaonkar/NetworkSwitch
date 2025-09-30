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
 * Unit tests for ToggleNetworkModeUseCase with alternating logic
 */
class ToggleNetworkModeUseCaseTest {
    
    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: ToggleNetworkModeUseCase
    
    private val testSubId = 1
    
    @Before
    fun setUp() {
        networkControlRepository = mockk()
        preferencesRepository = mockk()
        useCase = ToggleNetworkModeUseCase(networkControlRepository, preferencesRepository)
    }
    
    @Test
    fun `should switch to mode B when nextModeIsB is true`() = runTest {
        // Given
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.success(Unit)
        
        val updatedConfig = config.toggle() // After successful switch, toggle the state
        coEvery { preferencesRepository.setToggleModeConfig(updatedConfig) } returns Unit
        
        // When
        val result = useCase(testSubId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(NetworkMode.NR_ONLY, result.getOrNull())
        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) }
        coVerify { preferencesRepository.setToggleModeConfig(updatedConfig) }
    }
    
    @Test
    fun `should switch to mode A when nextModeIsB is false`() = runTest {
        // Given
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = false)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)
        
        val updatedConfig = config.toggle() // After successful switch, toggle the state
        coEvery { preferencesRepository.setToggleModeConfig(updatedConfig) } returns Unit
        
        // When
        val result = useCase(testSubId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(NetworkMode.LTE_ONLY, result.getOrNull())
        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) }
        coVerify { preferencesRepository.setToggleModeConfig(updatedConfig) }
    }
    
    @Test
    fun `should handle alternating sequence correctly`() = runTest {
        // Given
        val initialConfig = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns initialConfig
        
        // First call should switch to NR_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.success(Unit)
        val afterFirstToggle = initialConfig.toggle()
        coEvery { preferencesRepository.setToggleModeConfig(afterFirstToggle) } returns Unit
        
        // When
        val firstResult = useCase(testSubId)
        
        // Then
        assertTrue(firstResult.isSuccess)
        assertEquals(NetworkMode.NR_ONLY, firstResult.getOrNull())
        
        // The next call should switch to LTE_ONLY
        coEvery { preferencesRepository.getToggleModeConfig() } returns afterFirstToggle
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)
        val afterSecondToggle = afterFirstToggle.toggle()
        coEvery { preferencesRepository.setToggleModeConfig(afterSecondToggle) } returns Unit
        
        val secondResult = useCase(testSubId)
        
        assertTrue(secondResult.isSuccess)
        assertEquals(NetworkMode.LTE_ONLY, secondResult.getOrNull())
    }
    
    @Test
    fun `should return failure when setNetworkMode fails and not update config`() = runTest {
        // Given
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        
        val exception = RuntimeException("Network mode not supported")
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.failure(exception)
        
        // When
        val result = useCase(testSubId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        // Config should not be updated when network mode change fails
        coVerify(exactly = 0) { preferencesRepository.setToggleModeConfig(any()) }
    }
    
    @Test
    fun `should handle repository exception`() = runTest {
        // Given
        coEvery { preferencesRepository.getToggleModeConfig() } throws RuntimeException("Repository error")
        
        // When
        val result = useCase(testSubId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }
}