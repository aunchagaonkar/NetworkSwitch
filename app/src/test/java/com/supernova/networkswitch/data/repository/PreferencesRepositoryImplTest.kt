package com.supernova.networkswitch.data.repository

import com.supernova.networkswitch.data.source.PreferencesDataSource
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.util.CoroutineTestRule
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PreferencesRepositoryImplTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var mockDataSource: PreferencesDataSource
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setUp() {
        mockDataSource = mockk(relaxed = true)
        repository = PreferencesRepositoryImpl(mockDataSource)
    }

    @Test
    fun `getControlMethod calls data source`() = runTest {
        repository.getControlMethod()
        coVerify { mockDataSource.getControlMethod() }
    }

    @Test
    fun `setControlMethod calls data source`() = runTest {
        val method = ControlMethod.ROOT
        repository.setControlMethod(method)
        coVerify { mockDataSource.setControlMethod(method) }
    }

    @Test
    fun `observeControlMethod calls data source`() {
        repository.observeControlMethod()
        verify { mockDataSource.observeControlMethod() }
    }
}
