package com.supernova.networkswitch.domain.model

import org.junit.Assert.*
import org.junit.Test

class ToggleModeConfigTest {

    private val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)

    @Test
    fun `should target mode B when the radio is on mode A`() {
        assertEquals(
            ToggleState(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY),
            config.resolve(NetworkMode.LTE_ONLY, null)
        )
    }

    @Test
    fun `should target mode A when the radio is on mode B`() {
        assertEquals(
            ToggleState(NetworkMode.NR_ONLY, NetworkMode.LTE_ONLY),
            config.resolve(NetworkMode.NR_ONLY, null)
        )
    }

    @Test
    fun `should trust a readable mode over the last applied one`() {
        assertEquals(NetworkMode.LTE_ONLY, config.resolve(NetworkMode.NR_ONLY, NetworkMode.LTE_ONLY).next)
    }

    @Test
    fun `should restart at mode A when the radio is outside the pair`() {
        assertEquals(
            ToggleState(NetworkMode.NR_LTE_GSM_WCDMA, NetworkMode.LTE_ONLY),
            config.resolve(NetworkMode.NR_LTE_GSM_WCDMA, NetworkMode.LTE_ONLY)
        )
    }

    @Test
    fun `should leave mode A when it reads back as a mode with the same radios`() {
        val umts = ToggleModeConfig(NetworkMode.GSM_UMTS, NetworkMode.LTE_ONLY)

        assertEquals(
            ToggleState(NetworkMode.GSM_UMTS, NetworkMode.LTE_ONLY),
            umts.resolve(NetworkMode.WCDMA_PREF, null)
        )
    }

    @Test
    fun `should report a mode B of GSM_UMTS that reads back as WCDMA_PREF`() {
        val umts = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.GSM_UMTS)

        assertEquals(
            ToggleState(NetworkMode.GSM_UMTS, NetworkMode.LTE_ONLY),
            umts.resolve(NetworkMode.WCDMA_PREF, null)
        )
    }

    @Test
    fun `should target mode A when the mode is unreadable and nothing was applied`() {
        assertEquals(ToggleState(null, NetworkMode.LTE_ONLY), config.resolve(null, null))
    }

    @Test
    fun `should alternate when the mode can never be read`() {
        val first = config.resolve(null, null).next
        val second = config.resolve(null, first).next
        val third = config.resolve(null, second).next

        assertEquals(
            listOf(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, NetworkMode.LTE_ONLY),
            listOf(first, second, third)
        )
    }

    @Test
    fun `should target mode A when the mode is unreadable and mode B was last applied`() {
        assertEquals(ToggleState(null, NetworkMode.LTE_ONLY), config.resolve(null, NetworkMode.NR_ONLY))
    }

    @Test
    fun `should ignore a last applied mode no longer in the pair`() {
        assertEquals(NetworkMode.LTE_ONLY, config.resolve(null, NetworkMode.GSM_ONLY).next)
    }

    @Test
    fun `should handle a config whose two modes are identical`() {
        val same = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.LTE_ONLY)

        assertEquals(NetworkMode.LTE_ONLY, same.resolve(NetworkMode.LTE_ONLY, null).next)
        assertEquals(NetworkMode.LTE_ONLY, same.resolve(null, null).next)
    }
}
