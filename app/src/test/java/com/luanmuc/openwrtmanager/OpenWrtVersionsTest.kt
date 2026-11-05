package com.luanmuc.openwrtmanager

import com.luanmuc.openwrtmanager.util.OpenWrtVersions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the OpenWrt 23.05.0 - 23.05.6 version support matrix.
 */
class OpenWrtVersionsTest {

    // ---------- normalize ----------

    @Test
    fun `normalize keeps canonical 23_05 versions`() {
        assertEquals("23.05.0", OpenWrtVersions.normalize("23.05.0"))
        assertEquals("23.05.3", OpenWrtVersions.normalize("23.05.3"))
        assertEquals("23.05.6", OpenWrtVersions.normalize("23.05.6"))
    }

    @Test
    fun `normalize pads short minor segment`() {
        // user-facing notation 23.5.x -> 23.05.x
        assertEquals("23.05.0", OpenWrtVersions.normalize("23.5.0"))
        assertEquals("23.05.4", OpenWrtVersions.normalize("23.5.4"))
        assertEquals("23.05.6", OpenWrtVersions.normalize("23.5.6"))
    }

    @Test
    fun `normalize strips OpenWrt prefix`() {
        assertEquals("23.05.2", OpenWrtVersions.normalize("OpenWrt 23.05.2"))
    }

    @Test
    fun `normalize strips build revision suffix`() {
        assertEquals(
            "23.05.0",
            OpenWrtVersions.normalize("23.05.0 r23497-6637af95aa")
        )
        assertEquals(
            "23.05.2",
            OpenWrtVersions.normalize("OpenWrt 23.05.2 r23497-6637af95aa")
        )
    }

    @Test
    fun `normalize trims whitespace`() {
        assertEquals("23.05.1", OpenWrtVersions.normalize("   23.05.1   "))
    }

    // ---------- isSupported ----------

    @Test
    fun `all 23_05 point releases are supported`() {
        for (v in OpenWrtVersions.SUPPORTED_VERSIONS) {
            assertTrue("expected $v to be supported", OpenWrtVersions.isSupported(v))
        }
    }

    @Test
    fun `short notation 23_5_x is supported`() {
        for (patch in 0..6) {
            assertTrue(
                "expected 23.5.$patch to be supported",
                OpenWrtVersions.isSupported("23.5.$patch")
            )
        }
    }

    @Test
    fun `versions below 23_05_0 are not supported`() {
        assertFalse(OpenWrtVersions.isSupported("22.03.7"))
        assertFalse(OpenWrtVersions.isSupported("21.02.1"))
        assertFalse(OpenWrtVersions.isSupported("23.04.9"))
    }

    @Test
    fun `versions above 23_05_6 are not supported`() {
        assertFalse(OpenWrtVersions.isSupported("23.05.7"))
        assertFalse(OpenWrtVersions.isSupported("24.10.0"))
        assertFalse(OpenWrtVersions.isSupported("25.01.0"))
    }

    @Test
    fun `known point releases`() {
        assertTrue(OpenWrtVersions.isKnownPointRelease("23.05.3"))
        assertTrue(OpenWrtVersions.isKnownPointRelease("23.5.3"))
        // in-range but not an exact point release
        assertFalse(OpenWrtVersions.isKnownPointRelease("23.05.2.1"))
    }

    // ---------- latest / comparison ----------

    @Test
    fun `latest supported is 23_05_6`() {
        assertEquals("23.05.6", OpenWrtVersions.latestSupported())
    }

    @Test
    fun `compareWithLatest detects updates`() {
        // 23.05.0 < 23.05.6 -> update available
        assertTrue(OpenWrtVersions.compareWithLatest("23.05.0") < 0)
        // 23.05.6 == latest -> up to date
        assertEquals(0, OpenWrtVersions.compareWithLatest("23.05.6"))
        // newer than supported line
        assertTrue(OpenWrtVersions.compareWithLatest("24.10.0") > 0)
    }

    @Test
    fun `supported range description`() {
        assertEquals("23.05.0 - 23.05.6", OpenWrtVersions.supportedRangeDescription())
    }

    @Test
    fun `support matrix has seven point releases`() {
        assertEquals(7, OpenWrtVersions.SUPPORTED_VERSIONS.size)
        assertEquals("23.05.0", OpenWrtVersions.MIN_SUPPORTED_VERSION)
        assertEquals("23.05.6", OpenWrtVersions.MAX_SUPPORTED_VERSION)
    }
}
