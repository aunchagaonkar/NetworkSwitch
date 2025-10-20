package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.repository.SimRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetAvailableSimsUseCase
 */
class GetAvailableSimsUseCaseTest {
    
    private lateinit var simRepository: SimRepository
    private lateinit var useCase: GetAvailableSimsUseCase
    
    @Before
    fun setUp() {
        simRepository = mockk()
        useCase = GetAvailableSimsUseCase(simRepository)
    }
    
    @Test
    fun `should return success with list of SIMs when repository returns data`() = runTest {
        // Given
        val simList = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1 (Slot 1)"),
            SimInfo(subscriptionId = 2, simSlotIndex = 1, displayName = "SIM 2 (Slot 2)")
        )
        coEvery { simRepository.getAvailableSimCards() } returns simList
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(simList, result.getOrNull())
        assertEquals(2, result.getOrNull()?.size)
    }
    
    @Test
    fun `should return success with empty list when no SIMs available`() = runTest {
        // Given
        coEvery { simRepository.getAvailableSimCards() } returns emptyList()
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(emptyList<SimInfo>(), result.getOrNull())
    }
    
    @Test
    fun `should return success with empty list when permission not granted`() = runTest {
        // Given (permission denied case returns empty list)
        coEvery { simRepository.getAvailableSimCards() } returns emptyList()
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `should return failure when repository throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Failed to access SubscriptionManager")
        coEvery { simRepository.getAvailableSimCards() } throws exception
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `should handle single SIM device`() = runTest {
        // Given
        val singleSim = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "My Carrier (Slot 1)")
        )
        coEvery { simRepository.getAvailableSimCards() } returns singleSim
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(1, result.getOrNull()?.first()?.subscriptionId)
    }
    
    @Test
    fun `should handle triple SIM device`() = runTest {
        // Given
        val tripleSim = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1 (Slot 1)"),
            SimInfo(subscriptionId = 2, simSlotIndex = 1, displayName = "SIM 2 (Slot 2)"),
            SimInfo(subscriptionId = 3, simSlotIndex = 2, displayName = "SIM 3 (Slot 3)")
        )
        coEvery { simRepository.getAvailableSimCards() } returns tripleSim
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }
}
