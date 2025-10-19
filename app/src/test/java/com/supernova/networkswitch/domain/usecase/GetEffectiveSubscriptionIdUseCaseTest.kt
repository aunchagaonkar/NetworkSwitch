package com.supernova.networkswitch.domain.usecase

import android.telephony.SubscriptionManager
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.repository.SimRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetEffectiveSubscriptionIdUseCase
 */
class GetEffectiveSubscriptionIdUseCaseTest {
    
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var simRepository: SimRepository
    private lateinit var useCase: GetEffectiveSubscriptionIdUseCase
    
    private val defaultSubId = 100
    
    @Before
    fun setUp() {
        preferencesRepository = mockk()
        simRepository = mockk()
        useCase = GetEffectiveSubscriptionIdUseCase(preferencesRepository, simRepository)
        
        // Mock SubscriptionManager static method
        mockkStatic(SubscriptionManager::class)
        every { SubscriptionManager.getDefaultDataSubscriptionId() } returns defaultSubId
    }
    
    @After
    fun tearDown() {
        unmockkStatic(SubscriptionManager::class)
    }
    
    @Test
    fun `should return default subscription ID when selected is -1 (Auto mode)`() = runTest {
        // Given
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns -1
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(defaultSubId, result)
    }
    
    @Test
    fun `should return specific subscription ID when selected by user and SIM exists`() = runTest {
        // Given
        val selectedSubId = 42
        val availableSims = listOf(
            SimInfo(subscriptionId = 42, simSlotIndex = 0, displayName = "SIM 1")
        )
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns selectedSubId
        coEvery { simRepository.getAvailableSimCards() } returns availableSims
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(selectedSubId, result)
    }
    
    @Test
    fun `should fallback to default when selected SIM was removed`() = runTest {
        // Given
        val selectedSubId = 42
        val availableSims = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1")
        )
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns selectedSubId
        coEvery { simRepository.getAvailableSimCards() } returns availableSims
        coEvery { preferencesRepository.setSelectedSubscriptionId(-1) } returns Unit
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(defaultSubId, result)
        coVerify { preferencesRepository.setSelectedSubscriptionId(-1) }
    }
    
    @Test
    fun `should use selected ID when cannot check SIM availability`() = runTest {
        // Given
        val selectedSubId = 42
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns selectedSubId
        coEvery { simRepository.getAvailableSimCards() } throws SecurityException("Permission denied")
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(selectedSubId, result)
    }
    
    @Test
    fun `should return subscription ID 1 when user selected SIM 1 and it exists`() = runTest {
        // Given
        val availableSims = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1"),
            SimInfo(subscriptionId = 2, simSlotIndex = 1, displayName = "SIM 2")
        )
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns 1
        coEvery { simRepository.getAvailableSimCards() } returns availableSims
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(1, result)
    }
    
    @Test
    fun `should return subscription ID 2 when user selected SIM 2 and it exists`() = runTest {
        // Given
        val availableSims = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1"),
            SimInfo(subscriptionId = 2, simSlotIndex = 1, displayName = "SIM 2")
        )
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns 2
        coEvery { simRepository.getAvailableSimCards() } returns availableSims
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(2, result)
    }
    
    @Test
    fun `should handle invalid subscription ID of INVALID_SUBSCRIPTION_ID constant`() = runTest {
        // Given (SubscriptionManager.INVALID_SUBSCRIPTION_ID = -1)
        coEvery { preferencesRepository.getSelectedSubscriptionId() } returns SubscriptionManager.INVALID_SUBSCRIPTION_ID
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(defaultSubId, result)
    }
}
