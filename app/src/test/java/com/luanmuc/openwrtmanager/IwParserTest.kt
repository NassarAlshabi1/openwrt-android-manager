package com.luanmuc.openwrtmanager

import com.luanmuc.openwrtmanager.util.IwParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the `iw dev` output parser used by the raw
 * diagnostic-commands feature (mesh interface discovery).
 */
class IwParserTest {

    private val sampleIwDev = """
        phy#0
        \tInterface phy0-mesh0
        \t\tifindex 43
        \t\twdev 0x100000001
        \t\taddr 00:11:22:33:44:55
        \t\ttype mesh point
        \t\tchannel 1 (2412 MHz), width: 20 MHz, center1: 2412 MHz
        \t\ttxpower 20.00 dBm
        \tInterface wlan0
        \t\tifindex 42
        \t\twdev 0x1
        \t\taddr 00:11:22:33:44:54
        \t\ttype managed
        \t\tchannel 1 (2412 MHz), width: 20 MHz, center1: 2412 MHz

        phy#1
        \tInterface phy1-mesh0
        \t\tifindex 57
        \t\twdev 0x200000001
        \t\taddr 00:11:22:33:44:56
        \t\ttype mesh point
        \t\tchannel 36 (5180 MHz), width: 20 MHz, center1: 5180 MHz
        \tInterface wlan1
        \t\tifindex 56
        \t\ttype managed
    """.trimIndent().replace("\\t", "\t")

    @Test
    fun `finds all mesh interfaces in order`() {
        assertEquals(
            listOf("phy0-mesh0", "phy1-mesh0"),
            IwParser.parseMeshInterfaces(sampleIwDev)
        )
    }

    @Test
    fun `ignores managed and ap interfaces`() {
        val out = """
            phy#0
            \tInterface wlan0
            \t\ttype managed
            \tInterface wlan0-1
            \t\ttype AP
        """.trimIndent().replace("\\t", "\t")
        assertTrue(IwParser.parseMeshInterfaces(out).isEmpty())
    }

    @Test
    fun `accepts bare mesh type without point suffix`() {
        val out = "phy#0\n\tInterface mesh0\n\t\ttype mesh"
        assertEquals(listOf("mesh0"), IwParser.parseMeshInterfaces(out))
    }

    @Test
    fun `blank or null output yields empty list`() {
        assertTrue(IwParser.parseMeshInterfaces(null).isEmpty())
        assertTrue(IwParser.parseMeshInterfaces("").isEmpty())
        assertTrue(IwParser.parseMeshInterfaces("   \n\t\n").isEmpty())
    }

    @Test
    fun `falls back to default interface when none discovered`() {
        assertEquals(
            listOf(IwParser.DEFAULT_MESH_INTERFACE),
            IwParser.meshInterfacesOrDefault("phy#0\n\tInterface wlan0\n\t\ttype managed")
        )
    }

    @Test
    fun `discovered interfaces are used as-is`() {
        assertEquals(
            listOf("phy1-mesh0"),
            IwParser.meshInterfacesOrDefault("phy#1\n\tInterface phy1-mesh0\n\t\ttype mesh point")
        )
    }

    @Test
    fun `command error output yields no interfaces`() {
        // e.g. router without wireless: "command failed: No such device (-19)"
        assertTrue(IwParser.parseMeshInterfaces("command failed: No such device (-19)").isEmpty())
    }
}
