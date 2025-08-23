package com.supernova.networkswitch

import org.junit.Test
import org.junit.Assert.*
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode

/**
 * Unit tests for the Network Switch application domain models.
 */
class NetworkSwitchUnitTest {
    
    @Test
    fun controlMethod_enum_values_are_correct() {
        val methods = ControlMethod.values()
        assertEquals(2, methods.size)
        assertTrue(methods.contains(ControlMethod.ROOT))
        assertTrue(methods.contains(ControlMethod.SHIZUKU))
    }
    
    @Test
    fun networkMode_enum_values_are_correct() {
        val modes = NetworkMode.values()
        assertEquals(2, modes.size)
        assertTrue(modes.contains(NetworkMode.FOUR_G_ONLY))
        assertTrue(modes.contains(NetworkMode.FIVE_G_ONLY))
    }
    
    @Test
    fun networkMode_toggle_logic_works() {
        var currentMode = NetworkMode.FOUR_G_ONLY
        currentMode = if (currentMode == NetworkMode.FOUR_G_ONLY) 
            NetworkMode.FIVE_G_ONLY else NetworkMode.FOUR_G_ONLY
        assertEquals(NetworkMode.FIVE_G_ONLY, currentMode)
        
        currentMode = if (currentMode == NetworkMode.FOUR_G_ONLY) 
            NetworkMode.FIVE_G_ONLY else NetworkMode.FOUR_G_ONLY
        assertEquals(NetworkMode.FOUR_G_ONLY, currentMode)
    }
}