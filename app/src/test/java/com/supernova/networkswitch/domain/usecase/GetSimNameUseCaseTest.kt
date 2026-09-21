package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.model.SimQueryResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetSimNameUseCaseTest {

    private lateinit var getAvailableSims: GetAvailableSimsUseCase
    private lateinit var useCase: GetSimNameUseCase

    private val jio = SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "Jio (Slot 1)")
    private val airtel = SimInfo(subscriptionId = 3, simSlotIndex = 1, displayName = "Airtel (Slot 2)")

    @Before
    fun setUp() {
        getAvailableSims = mockk()
        useCase = GetSimNameUseCase(getAvailableSims)
    }

    @Test
    fun `should name the SIM when there are several to tell apart`() = runTest {
        coEvery { getAvailableSims() } returns SimQueryResult.Loaded(listOf(jio, airtel))

        assertEquals("Airtel (Slot 2)", useCase(3))
    }

    @Test
    fun `should not name the only SIM`() = runTest {
        coEvery { getAvailableSims() } returns SimQueryResult.Loaded(listOf(jio))

        assertNull(useCase(1))
    }

    @Test
    fun `should not guess a name when the SIM set is unreadable`() = runTest {
        coEvery { getAvailableSims() } returns SimQueryResult.PermissionDenied

        assertNull(useCase(1))
    }

    @Test
    fun `should not name a subscription that is not present`() = runTest {
        coEvery { getAvailableSims() } returns SimQueryResult.Loaded(listOf(jio, airtel))

        assertNull(useCase(99))
    }
}
