package com.supernova.networkswitch.service

import com.supernova.networkswitch.service.NetworkModeBitmaskMapper.Companion.MAX_NETWORK_MODE
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for NetworkModeBitmaskMapper.
 *
 * Uses the AOSP mapper: TelephonyManager is unavailable in JVM unit tests, so the
 * reflective [NetworkModeBitmaskMapper.platform] cannot be exercised here.
 */
class NetworkModeBitmaskMapperTest {

    private val mapper = NetworkModeBitmaskMapper.aosp

    // Restated rather than imported, so that editing a production constant fails a test.
    private val gprs = 1L shl 0
    private val edge = 1L shl 1
    private val umts = 1L shl 2
    private val cdma = 1L shl 3
    private val evdo0 = 1L shl 4
    private val onexRtt = 1L shl 6
    private val hspap = 1L shl 14
    private val gsm = 1L shl 15
    private val tdscdma = 1L shl 16
    private val lte = 1L shl 12
    private val lteCa = 1L shl 18
    private val nr = 1L shl 19

    @Test
    fun `known modes map to the expected bitmasks`() {
        assertEquals(gsm or gprs or edge, mapper.toBitmask(1))
        assertEquals(lte or lteCa, mapper.toBitmask(11))
        assertEquals(tdscdma, mapper.toBitmask(13))
        assertEquals(nr, mapper.toBitmask(23))
        assertEquals(nr or lte or lteCa, mapper.toBitmask(24))
        assertEquals(cdma or onexRtt, mapper.toBitmask(5))
    }

    @Test
    fun `unrecognised modes map to all network types`() {
        assertEquals(NetworkModeBitmaskMapper.ALL_NETWORK_TYPES, mapper.toBitmask(-1))
        assertEquals(NetworkModeBitmaskMapper.ALL_NETWORK_TYPES, mapper.toBitmask(34))
        assertEquals(NetworkModeBitmaskMapper.ALL_NETWORK_TYPES, mapper.toBitmask(9999))
    }

    @Test
    fun `every mode produces a non-zero bitmask`() {
        for (mode in 0..MAX_NETWORK_MODE) {
            assertNotEquals("mode $mode produced an empty bitmask", 0L, mapper.toBitmask(mode))
        }
    }

    /** Pins the exact set of modes that share a bitmask with a lower-numbered mode. */
    @Test
    fun `mode 3 is the only mode that cannot round-trip`() {
        val collisions = (0..MAX_NETWORK_MODE).filter { mode ->
            mapper.toNetworkMode(mapper.toBitmask(mode)) != mode
        }
        assertEquals(listOf(3), collisions)
        assertEquals(0, mapper.toNetworkMode(mapper.toBitmask(3)))
    }

    @Test
    fun `all modes except the known collision round-trip exactly`() {
        for (mode in 0..MAX_NETWORK_MODE) {
            if (mode == 3) continue
            assertEquals(
                "mode $mode failed to round-trip",
                mode,
                mapper.toNetworkMode(mapper.toBitmask(mode)),
            )
        }
    }

    /** Modems commonly report bits beyond those the mode table defines. */
    @Test
    fun `bitmasks with unknown extra bits fall back to the highest generation present`() {
        val junk = 1L shl 30

        assertEquals("NR + LTE should report NR_LTE", 24, mapper.toNetworkMode(nr or lte or junk))
        assertEquals("NR alone should report NR_ONLY", 23, mapper.toNetworkMode(nr or junk))
        assertEquals("LTE alone should report LTE_ONLY", 11, mapper.toNetworkMode(lte or junk))
        assertEquals("TD-SCDMA should report TDSCDMA_ONLY", 13, mapper.toNetworkMode(tdscdma or junk))
        assertEquals("WCDMA should report WCDMA_ONLY", 2, mapper.toNetworkMode(hspap or junk))
        assertEquals("GSM should report GSM_ONLY", 1, mapper.toNetworkMode(gprs or junk))
        assertEquals("CDMA + EVDO should report CDMA", 4, mapper.toNetworkMode(cdma or evdo0 or junk))
        assertEquals("CDMA alone should report CDMA_NO_EVDO", 5, mapper.toNetworkMode(onexRtt or junk))
        assertEquals("EVDO alone should report EVDO_NO_CDMA", 6, mapper.toNetworkMode(evdo0 or junk))
    }

    @Test
    fun `a bitmask with no recognised technology reports mode 0`() {
        assertEquals(0, mapper.toNetworkMode(0L))
        assertEquals(0, mapper.toNetworkMode(1L shl 30))
    }

    @Test
    fun `reverse mapping never throws and always returns a valid mode`() {
        val random = Random(20260809) // fixed seed: a failure must be reproducible
        repeat(200_000) {
            val bitmask = random.nextLong()
            val mode = mapper.toNetworkMode(bitmask)
            assertTrue(
                "bitmask $bitmask produced out-of-range mode $mode",
                mode in 0..MAX_NETWORK_MODE,
            )
        }
    }
}
