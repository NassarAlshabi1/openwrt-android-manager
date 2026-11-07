package com.luanmuc.openwrtmanager.util

/**
 * Pure parser for the output of `iw dev` on OpenWrt.
 *
 * `iw dev` lists every wireless interface grouped by phy:
 *
 * ```
 * phy#0
 *         Interface phy0-mesh0
 *                 ifindex 43
 *                 wdev 0x100000001
 *                 addr 00:11:22:33:44:55
 *                 type mesh point
 *                 channel 1 (2412 MHz), width: 20 MHz, center1: 2412 MHz
 *         Interface wlan0
 *                 ifindex 42
 *                 type managed
 * ```
 *
 * The parser extracts the names of all interfaces whose type is
 * "mesh point" (802.11s), so mesh diagnostics can run against every
 * mesh interface actually present on the router instead of a hardcoded
 * `phy0-mesh0`.
 */
object IwParser {

    private val interfaceLine = Regex("^\\s*Interface\\s+(\\S+)\\s*$")
    private val meshTypeLine = Regex("^\\s*type\\s+mesh(\\s+point)?\\s*$")

    /** Fallback used when discovery finds no mesh interface. */
    const val DEFAULT_MESH_INTERFACE = "phy0-mesh0"

    /**
     * Returns the names of all 802.11s mesh interfaces found in the
     * `iw dev` output, in order of appearance. Empty list when the
     * output contains no mesh interface (or is blank).
     */
    fun parseMeshInterfaces(iwDevOutput: String?): List<String> {
        if (iwDevOutput.isNullOrBlank()) return emptyList()
        val result = mutableListOf<String>()
        var currentInterface: String? = null
        iwDevOutput.lineSequence().forEach { line ->
            val iface = interfaceLine.find(line)?.groupValues?.get(1)
            if (iface != null) {
                currentInterface = iface
                return@forEach
            }
            if (meshTypeLine.containsMatchIn(line)) {
                currentInterface?.let { if (it !in result) result.add(it) }
            }
        }
        return result
    }

    /**
     * Mesh interfaces to use for diagnostics: the discovered ones, or
     * the conventional default `phy0-mesh0` when none were found.
     */
    fun meshInterfacesOrDefault(iwDevOutput: String?): List<String> {
        val found = parseMeshInterfaces(iwDevOutput)
        return if (found.isEmpty()) listOf(DEFAULT_MESH_INTERFACE) else found
    }
}
