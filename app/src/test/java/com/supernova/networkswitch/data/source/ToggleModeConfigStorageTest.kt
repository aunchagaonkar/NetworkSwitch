package com.supernova.networkswitch.data.source

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.util.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Per-SIM toggle configuration storage.
 */
class ToggleModeConfigStorageTest {

    private lateinit var dataStore: InMemoryPreferencesDataStore
    private lateinit var source: PreferencesDataSource

    private val simA = "sim-a"
    private val simB = "sim-b"

    private val defaults = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)

    @Before
    fun setUp() {
        dataStore = InMemoryPreferencesDataStore()
        source = PreferencesDataSource(dataStore)
    }

    @Test
    fun `should default when nothing is stored`() = runTest {
        assertEquals(defaults, source.getToggleModeConfig(simA))
    }

    @Test
    fun `should keep each SIM's configuration separate`() = runTest {
        source.setToggleModeConfig(simA, ToggleModeConfig(NetworkMode.GSM_ONLY, NetworkMode.LTE_ONLY))
        source.setToggleModeConfig(simB, ToggleModeConfig(NetworkMode.WCDMA_ONLY, NetworkMode.NR_ONLY))

        assertEquals(
            ToggleModeConfig(NetworkMode.GSM_ONLY, NetworkMode.LTE_ONLY),
            source.getToggleModeConfig(simA)
        )
        assertEquals(
            ToggleModeConfig(NetworkMode.WCDMA_ONLY, NetworkMode.NR_ONLY),
            source.getToggleModeConfig(simB)
        )
    }

    @Test
    fun `should emit again when the same SIM's configuration changes`() = runTest {
        val seen = mutableListOf<ToggleModeConfig>()
        val job = launch { source.observeToggleModeConfig(simA).toList(seen) }
        runCurrent()

        source.setToggleModeConfig(simA, ToggleModeConfig(NetworkMode.GSM_ONLY, NetworkMode.LTE_ONLY))
        runCurrent()
        job.cancel()

        assertEquals(defaults, seen.first())
        assertEquals(ToggleModeConfig(NetworkMode.GSM_ONLY, NetworkMode.LTE_ONLY), seen.last())
    }

    @Test
    fun `should observe writes for the same SIM`() = runTest {
        source.setToggleModeConfig(simA, ToggleModeConfig(NetworkMode.GSM_ONLY, NetworkMode.LTE_ONLY))

        assertEquals(
            ToggleModeConfig(NetworkMode.GSM_ONLY, NetworkMode.LTE_ONLY),
            source.observeToggleModeConfig(simA).first()
        )
    }

    @Test
    fun `should fall back to defaults for an unrecognised stored mode value`() = runTest {
        dataStore.edit {
            it[intPreferencesKey("toggle_mode_a_$simA")] = 9999
            it[intPreferencesKey("toggle_mode_b_$simA")] = 9999
        }

        assertEquals(defaults, source.getToggleModeConfig(simA))
    }

    @Test
    fun `should have no last applied mode until one is recorded`() = runTest {
        assertNull(source.getLastAppliedMode(simA))
    }

    @Test
    fun `should keep each SIM's last applied mode separate`() = runTest {
        source.setLastAppliedMode(simA, NetworkMode.GSM_UMTS)

        assertEquals(NetworkMode.GSM_UMTS, source.getLastAppliedMode(simA))
        assertNull(source.getLastAppliedMode(simB))
    }
}
