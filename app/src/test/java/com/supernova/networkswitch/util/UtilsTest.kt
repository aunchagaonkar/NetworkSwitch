package com.supernova.networkswitch.util

import com.topjohnwu.superuser.Shell
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UtilsTest {

    @Before
    fun setUp() {
        mockkStatic(Shell::class)
        every { Shell.getShell() } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isRootGranted does not throw`() {
        every { Shell.isAppGrantedRoot() } returns true
        Utils.isRootGranted() // Should not throw

        every { Shell.isAppGrantedRoot() } returns false
        Utils.isRootGranted() // Should not throw

        every { Shell.isAppGrantedRoot() } returns null
        Utils.isRootGranted() // Should not throw
    }
}
