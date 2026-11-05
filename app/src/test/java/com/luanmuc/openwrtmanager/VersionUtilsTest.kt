package com.luanmuc.openwrtmanager

import com.luanmuc.openwrtmanager.util.VersionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for generic version utilities used by the
 * OpenWrt 23.05.x support matrix.
 */
class VersionUtilsTest {

    @Test
    fun `compareVersions handles equality`() {
        assertEquals(0, VersionUtils.compareVersions("23.05.0", "23.05.0"))
        assertEquals(0, VersionUtils.compareVersions("23.5.0", "23.05.0"))
    }

    @Test
    fun `compareVersions handles ordering`() {
        assertTrue(VersionUtils.compareVersions("23.05.6", "23.05.0") > 0)
        assertTrue(VersionUtils.compareVersions("23.05.0", "23.05.6") < 0)
        assertTrue(VersionUtils.compareVersions("24.10.0", "23.05.6") > 0)
        assertTrue(VersionUtils.compareVersions("22.03.7", "23.05.0") < 0)
    }

    @Test
    fun `compareVersions handles different lengths`() {
        // 23.05 == 23.05.0
        assertEquals(0, VersionUtils.compareVersions("23.05", "23.05.0"))
        // 23.05.0.1 > 23.05.0
        assertTrue(VersionUtils.compareVersions("23.05.0.1", "23.05.0") > 0)
    }

    @Test
    fun `compareVersions tolerates non numeric parts`() {
        // non-numeric parts fall back to 0
        assertEquals(0, VersionUtils.compareVersions("23.05.x", "23.05.0"))
    }

    @Test
    fun `isVersionInRange`() {
        assertTrue(VersionUtils.isVersionInRange("23.05.3", "23.05.0", "23.05.6"))
        assertTrue(VersionUtils.isVersionInRange("23.05.0", "23.05.0", "23.05.6"))
        assertTrue(VersionUtils.isVersionInRange("23.05.6", "23.05.0", "23.05.6"))
        assertFalse(VersionUtils.isVersionInRange("23.05.7", "23.05.0", "23.05.6"))
        assertFalse(VersionUtils.isVersionInRange("22.03.7", "23.05.0", "23.05.6"))
    }

    @Test
    fun `meetsMinimumVersion`() {
        assertTrue(VersionUtils.meetsMinimumVersion("23.05.0", "23.05.0"))
        assertTrue(VersionUtils.meetsMinimumVersion("23.05.6", "23.05.0"))
        assertFalse(VersionUtils.meetsMinimumVersion("22.03.7", "23.05.0"))
    }

    @Test
    fun `isUpdateAvailable`() {
        assertTrue(VersionUtils.isUpdateAvailable("23.05.0", "23.05.6"))
        assertFalse(VersionUtils.isUpdateAvailable("23.05.6", "23.05.6"))
        assertFalse(VersionUtils.isUpdateAvailable("24.10.0", "23.05.6"))
    }

    @Test
    fun `segment extraction`() {
        assertEquals(23, VersionUtils.getMajorVersion("23.05.2"))
        assertEquals(5, VersionUtils.getMinorVersion("23.05.2"))
        assertEquals(2, VersionUtils.getPatchVersion("23.05.2"))
    }

    @Test
    fun `preRelease detection`() {
        assertTrue(VersionUtils.isPreRelease("2.3.0-dev"))
        assertTrue(VersionUtils.isPreRelease("1.0.0-alpha"))
        assertTrue(VersionUtils.isPreRelease("1.0.0-beta.1"))
        assertTrue(VersionUtils.isPreRelease("1.0.0-rc1"))
        assertFalse(VersionUtils.isPreRelease("23.05.6"))
    }

    @Test
    fun `formatVersion`() {
        assertEquals("v23.05.6 (2)", VersionUtils.formatVersion("23.05.6", 2))
    }
}
