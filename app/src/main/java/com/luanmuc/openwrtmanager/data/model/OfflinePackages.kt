package com.luanmuc.openwrtmanager.data.model

/**
 * Offline package installation models.
 *
 * The offline installer downloads .ipk files from the official OpenWrt
 * package feeds (e.g. downloads.openwrt.org) to the phone, resolves
 * dependencies locally, uploads them to the router via the ubus "file"
 * object and installs them with `opkg install` — no internet connection
 * required on the router side.
 */

/**
 * Router context required to select matching packages.
 */
data class OfflineRouterContext(
    /** Normalized firmware version, e.g. "23.05.5" */
    val version: String = "",
    /** opkg architecture, e.g. "mipsel_24kc" (precise, from opkg print-architecture) */
    val architecture: String = "",
    /** Kernel release, e.g. "5.15.167-1-144de9e5..." (used for the kmods repo) */
    val kernelRelease: String = "",
    /** Build target, e.g. "ramips/mt7621" (used for the kmods repo) */
    val target: String = "",
    /** Distribution, e.g. "OpenWrt" */
    val distribution: String = "OpenWrt",
    /** Package manager detected on the router */
    val packageManager: PackageManagerType = PackageManagerType.OPKG
) {
    val isUsable: Boolean
        get() = version.isNotBlank() && architecture.isNotBlank()
}

/**
 * One entry parsed from a feed "Packages" index.
 */
data class OfflinePackageEntry(
    /** Package name, e.g. "luci-app-firewall" */
    val name: String = "",
    val version: String = "",
    /** Raw dependency names with constraints stripped, e.g. ["libc", "luci-base"] */
    val depends: List<String> = emptyList(),
    /** Virtual names provided by this package */
    val provides: List<String> = emptyList(),
    /** .ipk file name inside the feed, e.g. "luci_..._all.ipk" */
    val filename: String = "",
    /** Download size in bytes */
    val size: Long = 0,
    /** Installed size in bytes */
    val installedSize: Long = 0,
    val sha256: String = "",
    /** "mipsel_24kc" or "all" */
    val architecture: String = "",
    val section: String = "",
    val description: String = "",
    /** Feed this entry was found in: base/luci/packages/routing/telephony/kmods */
    val feed: String = ""
) {
    /** True if the entry is compatible with the given router architecture */
    fun matchesArchitecture(routerArch: String): Boolean {
        return architecture == "all" || architecture == "noarch" ||
                architecture.isBlank() || architecture == routerArch
    }
}

/**
 * Result of dependency resolution for a set of selected packages.
 */
data class OfflineResolution(
    /** Entries that must be downloaded (selected + missing deps), deduplicated */
    val toDownload: List<OfflinePackageEntry> = emptyList(),
    /** Names among the selection already installed on the router */
    val alreadyInstalled: List<String> = emptyList(),
    /** Dependency names that cannot be satisfied from any fetched feed */
    val missing: List<String> = emptyList(),
    /** Kernel module deps missing from the kmods repository */
    val missingKmods: List<String> = emptyList(),
    /** Total download size in bytes */
    val totalBytes: Long = 0,
    /** Total installed size in bytes (for space estimation) */
    val totalInstalledBytes: Long = 0
) {
    val hasHardMissing: Boolean get() = missing.isNotEmpty()
    val hasMissingKmods: Boolean get() = missingKmods.isNotEmpty()
}

/**
 * Free space info of the router (in bytes).
 */
data class OfflineSpaceInfo(
    val tmpFree: Long = 0,
    val overlayFree: Long = 0
)

/**
 * Result of an offline installation run.
 */
data class OfflineInstallResult(
    val success: Boolean = false,
    val output: String = "",
    val installedCount: Int = 0,
    val warnings: List<String> = emptyList(),
    val error: String? = null
)

/**
 * Phases of the offline install state machine.
 */
enum class OfflinePhase {
    IDLE,          // nothing running
    DETECTING,     // reading version/arch/kernel from the router
    LOADING_INDEX, // downloading feed indexes
    RESOLVING,     // computing dependency closure
    DOWNLOADING,   // downloading .ipk files to the phone
    UPLOADING,     // uploading .ipk files to the router
    INSTALLING,    // running opkg install on the router
    DONE,          // success
    FAILED         // failure
}
