package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.model.SimQueryResult
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
    fun `should pass through the loaded SIM list`() = runTest {
        val simList = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1 (Slot 1)"),
            SimInfo(subscriptionId = 2, simSlotIndex = 1, displayName = "SIM 2 (Slot 2)")
        )
        coEvery { simRepository.getAvailableSimCards() } returns SimQueryResult.Loaded(simList)

        assertEquals(SimQueryResult.Loaded(simList), useCase())
    }

    @Test
    fun `should report an empty device as loaded, not as a failure`() = runTest {
        coEvery { simRepository.getAvailableSimCards() } returns SimQueryResult.Loaded(emptyList())

        assertEquals(SimQueryResult.Loaded(emptyList()), useCase())
    }

    /**
     * A denied permission must stay distinguishable from a device with no SIMs, since
     * only the latter justifies discarding a stored SIM selection.
     */
    @Test
    fun `should report permission denied distinctly from an empty list`() = runTest {
        coEvery { simRepository.getAvailableSimCards() } returns SimQueryResult.PermissionDenied

        val result = useCase()

        assertEquals(SimQueryResult.PermissionDenied, result)
        assertNotEquals(SimQueryResult.Loaded(emptyList()), result)
    }

    @Test
    fun `should report a repository failure as Failed`() = runTest {
        val cause = IllegalStateException("SubscriptionManager unavailable")
        coEvery { simRepository.getAvailableSimCards() } returns SimQueryResult.Failed(cause)

        assertEquals(cause, (useCase() as SimQueryResult.Failed).cause)
    }

    @Test
    fun `should wrap an unexpected throw as Failed`() = runTest {
        val thrown = RuntimeException("boom")
        coEvery { simRepository.getAvailableSimCards() } throws thrown

        assertEquals(thrown, (useCase() as SimQueryResult.Failed).cause)
    }

    @Test
    fun `should handle single SIM device`() = runTest {
        val singleSim = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "My Carrier (Slot 1)")
        )
        coEvery { simRepository.getAvailableSimCards() } returns SimQueryResult.Loaded(singleSim)

        val result = useCase() as SimQueryResult.Loaded

        assertEquals(1, result.sims.size)
        assertEquals(1, result.sims.first().subscriptionId)
    }

    @Test
    fun `should handle triple SIM device`() = runTest {
        val tripleSim = listOf(
            SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1 (Slot 1)"),
            SimInfo(subscriptionId = 2, simSlotIndex = 1, displayName = "SIM 2 (Slot 2)"),
            SimInfo(subscriptionId = 3, simSlotIndex = 2, displayName = "SIM 3 (Slot 3)")
        )
        coEvery { simRepository.getAvailableSimCards() } returns SimQueryResult.Loaded(tripleSim)

        assertEquals(3, (useCase() as SimQueryResult.Loaded).sims.size)
    }
}
