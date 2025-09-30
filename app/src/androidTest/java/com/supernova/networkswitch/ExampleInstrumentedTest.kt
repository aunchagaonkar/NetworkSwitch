package com.supernova.networkswitch

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.supernova.networkswitch.presentation.ui.activity.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Rule

@RunWith(AndroidJUnit4::class)
class NetworkSwitchInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.supernova.networkswitch", appContext.packageName)
    }

    @Test
    fun mainActivityLaunches() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull(activity)
            assertTrue(activity is MainActivity)
        }
    }
}