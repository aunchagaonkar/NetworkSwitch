package com.supernova.networkswitch.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import androidx.datastore.preferences.core.edit
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PreferencesDataSourceTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var preferencesDataSource: PreferencesDataSource

    private val controlMethodKey = stringPreferencesKey("control_method")

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true)
        mockkStatic("androidx.datastore.preferences.core.PreferencesKt")
        coEvery { mockDataStore.edit(any()) } returns mockk()
        preferencesDataSource = PreferencesDataSource(mockDataStore)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getControlMethod returns ROOT when DataStore has ROOT`() = runTest {
        val preferences = mockk<Preferences>()
        coEvery { preferences[controlMethodKey] } returns "ROOT"
        coEvery { mockDataStore.data } returns flowOf(preferences)

        val result = preferencesDataSource.getControlMethod()

        assertEquals(ControlMethod.ROOT, result)
    }

    @Test
    fun `getControlMethod returns SHIZUKU when DataStore has SHIZUKU`() = runTest {
        val preferences = mockk<Preferences>()
        coEvery { preferences[controlMethodKey] } returns "SHIZUKU"
        coEvery { mockDataStore.data } returns flowOf(preferences)

        val result = preferencesDataSource.getControlMethod()

        assertEquals(ControlMethod.SHIZUKU, result)
    }

    @Test
    fun `getControlMethod returns default SHIZUKU when DataStore is empty`() = runTest {
        val preferences = mockk<Preferences>()
        coEvery { preferences[controlMethodKey] } returns null
        coEvery { mockDataStore.data } returns flowOf(preferences)

        val result = preferencesDataSource.getControlMethod()

        assertEquals(ControlMethod.SHIZUKU, result)
    }

    @Test
    fun `getControlMethod returns default SHIZUKU for invalid value`() = runTest {
        val preferences = mockk<Preferences>()
        coEvery { preferences[controlMethodKey] } returns "INVALID_VALUE"
        coEvery { mockDataStore.data } returns flowOf(preferences)

        val result = preferencesDataSource.getControlMethod()

        assertEquals(ControlMethod.SHIZUKU, result)
    }

    @Test
    fun `setControlMethod calls edit on DataStore with ROOT`() = runTest {
        preferencesDataSource.setControlMethod(ControlMethod.ROOT)

        coVerify {
            mockDataStore.edit(any())
        }
    }

    @Test
    fun `setControlMethod calls edit on DataStore with SHIZUKU`() = runTest {
        preferencesDataSource.setControlMethod(ControlMethod.SHIZUKU)

        coVerify {
            mockDataStore.edit(any())
        }
    }

    @Test
    fun `observeControlMethod emits correct values`() = runTest {
        val preferencesRoot = mockk<Preferences>()
        coEvery { preferencesRoot[controlMethodKey] } returns "ROOT"

        val preferencesShizuku = mockk<Preferences>()
        coEvery { preferencesShizuku[controlMethodKey] } returns "SHIZUKU"

        coEvery { mockDataStore.data } returns flowOf(preferencesRoot, preferencesShizuku)

        val results = mutableListOf<ControlMethod>()
        preferencesDataSource.observeControlMethod().collect {
            results.add(it)
        }

        assertEquals(2, results.size)
        assertEquals(ControlMethod.ROOT, results[0])
        assertEquals(ControlMethod.SHIZUKU, results[1])
    }

    @Test
    fun `observeControlMethod emits default value when empty`() = runTest {
        val emptyPreferences = mockk<Preferences>()
        coEvery { emptyPreferences[controlMethodKey] } returns null
        coEvery { mockDataStore.data } returns flowOf(emptyPreferences)

        val result = preferencesDataSource.observeControlMethod().first()

        assertEquals(ControlMethod.SHIZUKU, result)
    }
}
