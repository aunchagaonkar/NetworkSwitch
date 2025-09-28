package com.supernova.networkswitch.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ToggleModeConfig
 */
class ToggleModeConfigTest {
    
    @Test
    fun testGetNextMode() {
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        
        // When nextModeIsB = true, should return modeB
        assertEquals(NetworkMode.NR_ONLY, config.getNextMode())
        
        val config2 = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = false)
        
        // When nextModeIsB = false, should return modeA
        assertEquals(NetworkMode.LTE_ONLY, config2.getNextMode())
    }
    
    @Test
    fun testGetCurrentMode() {
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        
        // When nextModeIsB = true, current should be modeA
        assertEquals(NetworkMode.LTE_ONLY, config.getCurrentMode())
        
        val config2 = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = false)
        
        // When nextModeIsB = false, current should be modeB
        assertEquals(NetworkMode.NR_ONLY, config2.getCurrentMode())
    }
    
    @Test
    fun testToggle() {
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        
        val toggledConfig = config.toggle()
        
        // Should flip the nextModeIsB flag
        assertFalse(toggledConfig.nextModeIsB)
        assertEquals(NetworkMode.LTE_ONLY, toggledConfig.modeA)
        assertEquals(NetworkMode.NR_ONLY, toggledConfig.modeB)
    }
    
    @Test
    fun testAlternatingToggle() {
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        
        // First toggle: next is NR_ONLY, current is LTE_ONLY
        assertEquals(NetworkMode.NR_ONLY, config.getNextMode())
        assertEquals(NetworkMode.LTE_ONLY, config.getCurrentMode())
        
        val afterFirstToggle = config.toggle()
        
        // After toggle: next is LTE_ONLY, current is NR_ONLY
        assertEquals(NetworkMode.LTE_ONLY, afterFirstToggle.getNextMode())
        assertEquals(NetworkMode.NR_ONLY, afterFirstToggle.getCurrentMode())
        
        val afterSecondToggle = afterFirstToggle.toggle()
        
        // After second toggle: back to original state
        assertEquals(config.nextModeIsB, afterSecondToggle.nextModeIsB)
        assertEquals(NetworkMode.NR_ONLY, afterSecondToggle.getNextMode())
        assertEquals(NetworkMode.LTE_ONLY, afterSecondToggle.getCurrentMode())
    }
}