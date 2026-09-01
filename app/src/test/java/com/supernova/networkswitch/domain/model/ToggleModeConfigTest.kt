package com.supernova.networkswitch.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ToggleModeConfig
 */
class ToggleModeConfigTest {

    private val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)

    @Test
    fun `should target mode B when the radio is on mode A`() {
        assertEquals(NetworkMode.NR_ONLY, config.nextAfter(NetworkMode.LTE_ONLY))
    }

    @Test
    fun `should target mode A when the radio is on mode B`() {
        assertEquals(NetworkMode.LTE_ONLY, config.nextAfter(NetworkMode.NR_ONLY))
    }

    @Test
    fun `should target mode A when the radio mode is unknown`() {
        assertEquals(NetworkMode.LTE_ONLY, config.nextAfter(null))
    }

    @Test
    fun `should target mode A when the radio is on a mode outside the pair`() {
        assertEquals(NetworkMode.LTE_ONLY, config.nextAfter(NetworkMode.NR_LTE_GSM_WCDMA))
    }

    /** Two SIMs on different modes get different targets from one shared config. */
    @Test
    fun `should give each radio mode its own target from the same config`() {
        assertEquals(NetworkMode.NR_ONLY, config.nextAfter(NetworkMode.LTE_ONLY))
        assertEquals(NetworkMode.LTE_ONLY, config.nextAfter(NetworkMode.NR_ONLY))
    }

    @Test
    fun `should alternate when the radio follows the reported target`() {
        var actual: NetworkMode? = NetworkMode.LTE_ONLY

        actual = config.nextAfter(actual)
        assertEquals(NetworkMode.NR_ONLY, actual)

        actual = config.nextAfter(actual)
        assertEquals(NetworkMode.LTE_ONLY, actual)
    }

    @Test
    fun `should handle a config whose two modes are identical`() {
        val same = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.LTE_ONLY)

        assertEquals(NetworkMode.LTE_ONLY, same.nextAfter(NetworkMode.LTE_ONLY))
        assertEquals(NetworkMode.LTE_ONLY, same.nextAfter(null))
    }
}
