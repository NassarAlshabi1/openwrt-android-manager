package com.luanmuc.openwrtmanager.ui.diagnostic_commands

import com.luanmuc.openwrtmanager.R

/**
 * Category of a diagnostic command (used for grouping and bulk selection).
 */
enum class DiagCategory {
    SYSTEM,
    MEMORY_STORAGE,
    NETWORK,
    WIRELESS,
    FIREWALL;

    val titleRes: Int
        get() = when (this) {
            SYSTEM -> R.string.diagcmd_cat_system
            MEMORY_STORAGE -> R.string.diagcmd_cat_memory
            NETWORK -> R.string.diagcmd_cat_network
            WIRELESS -> R.string.diagcmd_cat_wireless
            FIREWALL -> R.string.diagcmd_cat_firewall
        }
}

/**
 * Ready-made selection presets shown as chips above the command list.
 */
enum class DiagPreset {
    STANDARD,
    FULL,
    NETWORK,
    WIRELESS,
    PERFORMANCE;

    val labelRes: Int
        get() = when (this) {
            STANDARD -> R.string.diagcmd_preset_standard
            FULL -> R.string.diagcmd_preset_full
            NETWORK -> R.string.diagcmd_preset_network
            WIRELESS -> R.string.diagcmd_preset_wireless
            PERFORMANCE -> R.string.diagcmd_preset_performance
        }
}

/**
 * Definition of one diagnostic command in the catalog.
 *
 * All commands are read-only (they never change router state), safe to run
 * on a stock OpenWrt 23.05 installation, and available without installing
 * extra packages.
 *
 * @param id               stable identifier, also used to persist selection
 * @param category         grouping category shown in the UI
 * @param command          the exact command executed on the router
 * @param reportHeader     English header of this section in the exported report
 * @param titleRes         localized display title
 * @param descRes          localized explanation: what the command shows and
 *                         when it is useful to select it
 * @param defaultSelected  part of the STANDARD preset
 * @param isMeshDynamic    expands into per-mesh-interface sections at run time
 */
data class DiagCommandDef(
    val id: String,
    val category: DiagCategory,
    val command: String,
    val reportHeader: String,
    val titleRes: Int,
    val descRes: Int,
    val defaultSelected: Boolean = false,
    val isMeshDynamic: Boolean = false
)

/**
 * Catalog of all diagnostic commands offered by the app.
 *
 * 26 read-only commands across 5 categories:
 * System, Memory & Storage, Network, Wireless & Mesh and Firewall.
 */
object DiagCommandCatalog {

    val commands: List<DiagCommandDef> = listOf(
        // ==================== System ====================
        DiagCommandDef(
            id = "hostname",
            category = DiagCategory.SYSTEM,
            command = "hostname",
            reportHeader = "HOSTNAME",
            titleRes = R.string.diagcmd_cmd_hostname_title,
            descRes = R.string.diagcmd_cmd_hostname_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "board",
            category = DiagCategory.SYSTEM,
            command = "ubus call system board",
            reportHeader = "SYSTEM BOARD",
            titleRes = R.string.diagcmd_cmd_board_title,
            descRes = R.string.diagcmd_cmd_board_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "sysinfo",
            category = DiagCategory.SYSTEM,
            command = "ubus call system info",
            reportHeader = "SYSTEM INFO",
            titleRes = R.string.diagcmd_cmd_sysinfo_title,
            descRes = R.string.diagcmd_cmd_sysinfo_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "loadavg",
            category = DiagCategory.SYSTEM,
            command = "cat /proc/loadavg",
            reportHeader = "LOAD AVERAGE",
            titleRes = R.string.diagcmd_cmd_loadavg_title,
            descRes = R.string.diagcmd_cmd_loadavg_desc
        ),
        DiagCommandDef(
            id = "date",
            category = DiagCategory.SYSTEM,
            command = "date",
            reportHeader = "SYSTEM TIME",
            titleRes = R.string.diagcmd_cmd_date_title,
            descRes = R.string.diagcmd_cmd_date_desc
        ),
        DiagCommandDef(
            id = "ps",
            category = DiagCategory.SYSTEM,
            command = "ps w",
            reportHeader = "PROCESS LIST",
            titleRes = R.string.diagcmd_cmd_ps_title,
            descRes = R.string.diagcmd_cmd_ps_desc
        ),
        DiagCommandDef(
            id = "top",
            category = DiagCategory.SYSTEM,
            command = "top -bn1 | head -n 25",
            reportHeader = "TOP PROCESSES",
            titleRes = R.string.diagcmd_cmd_top_title,
            descRes = R.string.diagcmd_cmd_top_desc
        ),
        DiagCommandDef(
            id = "dmesg",
            category = DiagCategory.SYSTEM,
            command = "dmesg | tail -n 60",
            reportHeader = "KERNEL MESSAGES",
            titleRes = R.string.diagcmd_cmd_dmesg_title,
            descRes = R.string.diagcmd_cmd_dmesg_desc
        ),
        DiagCommandDef(
            id = "logread",
            category = DiagCategory.SYSTEM,
            command = "logread | tail -n 120",
            reportHeader = "SYSTEM LOG",
            titleRes = R.string.diagcmd_cmd_logread_title,
            descRes = R.string.diagcmd_cmd_logread_desc
        ),

        // ==================== Memory & storage ====================
        DiagCommandDef(
            id = "free",
            category = DiagCategory.MEMORY_STORAGE,
            command = "free",
            reportHeader = "MEMORY USAGE",
            titleRes = R.string.diagcmd_cmd_free_title,
            descRes = R.string.diagcmd_cmd_free_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "df",
            category = DiagCategory.MEMORY_STORAGE,
            command = "df -h",
            reportHeader = "DISK USAGE",
            titleRes = R.string.diagcmd_cmd_df_title,
            descRes = R.string.diagcmd_cmd_df_desc,
            defaultSelected = true
        ),

        // ==================== Network ====================
        DiagCommandDef(
            id = "ipaddr",
            category = DiagCategory.NETWORK,
            command = "ip addr",
            reportHeader = "INTERFACES AND IP ADDRESSES",
            titleRes = R.string.diagcmd_cmd_ipaddr_title,
            descRes = R.string.diagcmd_cmd_ipaddr_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "iproute",
            category = DiagCategory.NETWORK,
            command = "ip route",
            reportHeader = "ROUTING TABLE",
            titleRes = R.string.diagcmd_cmd_iproute_title,
            descRes = R.string.diagcmd_cmd_iproute_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "iprule",
            category = DiagCategory.NETWORK,
            command = "ip rule",
            reportHeader = "POLICY ROUTING RULES",
            titleRes = R.string.diagcmd_cmd_iprule_title,
            descRes = R.string.diagcmd_cmd_iprule_desc
        ),
        DiagCommandDef(
            id = "iplink",
            category = DiagCategory.NETWORK,
            command = "ip -s link",
            reportHeader = "INTERFACE STATISTICS",
            titleRes = R.string.diagcmd_cmd_iplink_title,
            descRes = R.string.diagcmd_cmd_iplink_desc
        ),
        DiagCommandDef(
            id = "resolv",
            category = DiagCategory.NETWORK,
            command = "cat /etc/resolv.conf",
            reportHeader = "DNS CONFIGURATION",
            titleRes = R.string.diagcmd_cmd_resolv_title,
            descRes = R.string.diagcmd_cmd_resolv_desc
        ),
        DiagCommandDef(
            id = "nslookup",
            category = DiagCategory.NETWORK,
            command = "nslookup www.google.com",
            reportHeader = "DNS RESOLUTION TEST",
            titleRes = R.string.diagcmd_cmd_nslookup_title,
            descRes = R.string.diagcmd_cmd_nslookup_desc
        ),
        DiagCommandDef(
            id = "ping",
            category = DiagCategory.NETWORK,
            command = "ping -c 4 -W 2 8.8.8.8",
            reportHeader = "INTERNET CONNECTIVITY TEST",
            titleRes = R.string.diagcmd_cmd_ping_title,
            descRes = R.string.diagcmd_cmd_ping_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "netstatus",
            category = DiagCategory.NETWORK,
            command = "ubus call network.interface dump",
            reportHeader = "NETWORK INTERFACE STATUS",
            titleRes = R.string.diagcmd_cmd_netstatus_title,
            descRes = R.string.diagcmd_cmd_netstatus_desc
        ),
        DiagCommandDef(
            id = "leases",
            category = DiagCategory.NETWORK,
            command = "cat /tmp/dhcp.leases",
            reportHeader = "DHCP CLIENTS",
            titleRes = R.string.diagcmd_cmd_leases_title,
            descRes = R.string.diagcmd_cmd_leases_desc
        ),
        DiagCommandDef(
            id = "netstat",
            category = DiagCategory.NETWORK,
            command = "netstat -tulpn",
            reportHeader = "LISTENING PORTS",
            titleRes = R.string.diagcmd_cmd_netstat_title,
            descRes = R.string.diagcmd_cmd_netstat_desc
        ),

        // ==================== Wireless & mesh ====================
        DiagCommandDef(
            id = "iwdev",
            category = DiagCategory.WIRELESS,
            command = "iw dev",
            reportHeader = "WIRELESS INTERFACES",
            titleRes = R.string.diagcmd_cmd_iwdev_title,
            descRes = R.string.diagcmd_cmd_iwdev_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "iwinfo",
            category = DiagCategory.WIRELESS,
            command = "iwinfo",
            reportHeader = "WIRELESS SURVEY",
            titleRes = R.string.diagcmd_cmd_iwinfo_title,
            descRes = R.string.diagcmd_cmd_iwinfo_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "wifistatus",
            category = DiagCategory.WIRELESS,
            command = "ubus call network.wireless status",
            reportHeader = "WIRELESS STATUS",
            titleRes = R.string.diagcmd_cmd_wifistatus_title,
            descRes = R.string.diagcmd_cmd_wifistatus_desc,
            defaultSelected = true
        ),
        DiagCommandDef(
            id = "mesh",
            category = DiagCategory.WIRELESS,
            command = "iw dev <mesh> station dump ; iw dev <mesh> mpath dump",
            reportHeader = "MESH NEIGHBORS AND PATHS",
            titleRes = R.string.diagcmd_cmd_mesh_title,
            descRes = R.string.diagcmd_cmd_mesh_desc,
            defaultSelected = true,
            isMeshDynamic = true
        ),

        // ==================== Firewall ====================
        DiagCommandDef(
            id = "nft",
            category = DiagCategory.FIREWALL,
            command = "nft list ruleset | head -n 250",
            reportHeader = "FIREWALL RULESET",
            titleRes = R.string.diagcmd_cmd_nft_title,
            descRes = R.string.diagcmd_cmd_nft_desc
        )
    )

    /** Command definitions grouped by category, in catalog order. */
    val byCategory: Map<DiagCategory, List<DiagCommandDef>> =
        commands.groupBy { it.category }

    fun byId(id: String): DiagCommandDef? = commands.firstOrNull { it.id == id }

    /**
     * The command ids selected by a preset.
     *
     * - STANDARD: the recommended quick health check (default selection).
     * - FULL: every command, for a complete report.
     * - NETWORK: connectivity, DNS, routes, interfaces and clients.
     * - WIRELESS: radios, interfaces, survey and mesh topology.
     * - PERFORMANCE: load, processes, memory, storage and logs.
     */
    fun presetSelection(preset: DiagPreset): Set<String> = when (preset) {
        DiagPreset.FULL -> commands.map { it.id }.toSet()
        DiagPreset.STANDARD -> commands.filter { it.defaultSelected }.map { it.id }.toSet()
        DiagPreset.NETWORK -> setOf(
            "hostname", "ipaddr", "iproute", "iprule", "iplink", "resolv",
            "nslookup", "ping", "netstatus", "leases", "netstat"
        )
        DiagPreset.WIRELESS -> setOf(
            "hostname", "iwdev", "iwinfo", "wifistatus", "mesh", "ipaddr", "iproute"
        )
        DiagPreset.PERFORMANCE -> setOf(
            "board", "sysinfo", "loadavg", "date", "ps", "top",
            "dmesg", "logread", "free", "df"
        )
    }
}
