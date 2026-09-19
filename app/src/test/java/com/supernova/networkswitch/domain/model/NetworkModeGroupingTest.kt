package com.supernova.networkswitch.domain.model

import org.junit.Assert.*
import org.junit.Test

class NetworkModeGroupingTest {

    @Test
    fun `common and advanced together cover every mode exactly once`() {
        val combined = NetworkMode.common + NetworkMode.advanced

        assertEquals(NetworkMode.entries.size, combined.size)
        assertEquals(NetworkMode.entries.toSet(), combined.toSet())
    }

    @Test
    fun `common offers one mode per radio generation`() {
        assertEquals(
            listOf(
                NetworkMode.GSM_ONLY,
                NetworkMode.WCDMA_ONLY,
                NetworkMode.LTE_ONLY,
                NetworkMode.NR_ONLY,
                NetworkMode.NR_LTE
            ),
            NetworkMode.common
        )
    }

    @Test
    fun `advanced excludes the common modes`() {
        assertTrue(NetworkMode.advanced.none { it in NetworkMode.common })
    }
}
