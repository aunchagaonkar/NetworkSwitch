package com.supernova.networkswitch

import org.junit.Test
import org.junit.Assert.*
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode

class NetworkSwitchUnitTest {
    
    @Test
    fun controlMethod_enum_values_are_correct() {
        val methods = ControlMethod.values()
        assertEquals(2, methods.size)
        assertTrue(methods.contains(ControlMethod.ROOT))
        assertTrue(methods.contains(ControlMethod.SHIZUKU))
    }
    
    @Test
    fun networkMode_enum_values_contain_expected_modes() {
        val modes = NetworkMode.values()
        assertTrue(modes.size > 10)
        assertTrue(modes.contains(NetworkMode.LTE_ONLY))
        assertTrue(modes.contains(NetworkMode.NR_ONLY))
        assertTrue(modes.contains(NetworkMode.GSM_ONLY))
        assertTrue(modes.contains(NetworkMode.WCDMA_ONLY))
    }
    
    @Test
    fun networkMode_fromValue_works_correctly() {
        assertEquals(NetworkMode.LTE_ONLY, NetworkMode.fromValue(11))
        assertEquals(NetworkMode.NR_ONLY, NetworkMode.fromValue(23))
        assertNull(NetworkMode.fromValue(-1))
    }
}