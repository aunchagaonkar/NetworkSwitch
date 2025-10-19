package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for subscription ID preference use cases
 */
class SubscriptionIdPreferencesUseCaseTest {
    
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var getUseCase: GetSelectedSubscriptionIdUseCase
    private lateinit var setUseCase: SetSelectedSubscriptionIdUseCase
    
    @Before
    fun setUp() {
        preferencesRepository = mockk()
        getUseCase = GetSelectedSubscriptionIdUseCase(preferencesRepository)
        setUseCase = SetSelectedSubscriptionIdUseCase(preferencesRepository)
    }
    
    @Test
    fun `GetSelectedSubscriptionIdUseCase should return saved subscription ID`() = runTest {
        // Given
        val expectedSubId = 1
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns expectedSubId
        
        // When
        val result = getUseCase()
        
        // Then
        assertEquals(expectedSubId, result)
    }
    
    @Test
    fun `GetSelectedSubscriptionIdUseCase should return -1 when no selection made`() = runTest {
        // Given
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns -1
        
        // When
        val result = getUseCase()
        
        // Then
        assertEquals(-1, result)
    }
    
    @Test
    fun `SetSelectedSubscriptionIdUseCase should save subscription ID`() = runTest {
        // Given
        val subIdToSave = 2
        coEvery { preferencesRepository.setSelectedSubscriptionId(subIdToSave) } returns Unit
        
        // When
        setUseCase(subIdToSave)
        
        // Then
        coVerify { preferencesRepository.setSelectedSubscriptionId(subIdToSave) }
    }
    
    @Test
    fun `SetSelectedSubscriptionIdUseCase should save -1 for Auto mode`() = runTest {
        // Given
        coEvery { preferencesRepository.setSelectedSubscriptionId(-1) } returns Unit
        
        // When
        setUseCase(-1)
        
        // Then
        coVerify { preferencesRepository.setSelectedSubscriptionId(-1) }
    }
    
    @Test
    fun `should handle complete flow of saving and retrieving subscription ID`() = runTest {
        // Given
        val subId = 1
        coEvery { preferencesRepository.setSelectedSubscriptionId(subId) } returns Unit
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns subId
        
        // When
        setUseCase(subId)
        val retrieved = getUseCase()
        
        // Then
        assertEquals(subId, retrieved)
        coVerify { preferencesRepository.setSelectedSubscriptionId(subId) }
        coVerify { preferencesRepository.getSelectedSubscriptionId() }
    }
}
