package com.supernova.networkswitch.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NetworkMode enum
 */
class NetworkModeTest {
    
    @Test
    fun testNetworkModeFromValue() {
        // Test basic modes
        assertEquals(NetworkMode.GSM_ONLY, NetworkMode.fromValue(1))
        assertEquals(NetworkMode.WCDMA_ONLY, NetworkMode.fromValue(2))
        assertEquals(NetworkMode.LTE_ONLY, NetworkMode.fromValue(11))
        assertEquals(NetworkMode.NR_ONLY, NetworkMode.fromValue(23))
        
        // Test combined modes
        assertEquals(NetworkMode.NR_LTE, NetworkMode.fromValue(24))
        assertEquals(NetworkMode.NR_LTE_GSM_WCDMA, NetworkMode.fromValue(26))
        
        // Test invalid value
        assertNull(NetworkMode.fromValue(-1))
        assertNull(NetworkMode.fromValue(999))
    }
    
    @Test
    fun testNetworkModeDisplayNames() {
        assertEquals("2G Only (GSM)", NetworkMode.GSM_ONLY.displayName)
        assertEquals("3G Only (WCDMA)", NetworkMode.WCDMA_ONLY.displayName)
        assertEquals("4G Only (LTE)", NetworkMode.LTE_ONLY.displayName)
        assertEquals("5G Only (NR)", NetworkMode.NR_ONLY.displayName)
        assertEquals("4G/5G (NR/LTE)", NetworkMode.NR_LTE.displayName)
    }
}