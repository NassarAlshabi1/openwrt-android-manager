package com.luanmuc.openwrtmanager.util

/**
 * OpenWrt firmware version support matrix.
 *
 * Supported release line: 23.05.0 - 23.05.6
 * (also accepts the short form 23.5.0 - 23.5.6, normalized internally)
 */
object OpenWrtVersions {

    /** Minimum supported firmware version */
    const val MIN_SUPPORTED_VERSION = "23.05.0"

    /** Maximum supported firmware version */
    const val MAX_SUPPORTED_VERSION = "23.05.6"

    /** All supported 23.05.x point releases */
    val SUPPORTED_VERSIONS: List<String> = listOf(
        "23.05.0",
        "23.05.1",
        "23.05.2",
        "23.05.3",
        "23.05.4",
        "23.05.5",
        "23.05.6"
    )

    /**
     * Normalize a raw firmware version string:
     *  - strips surrounding whitespace
     *  - removes an "OpenWrt " prefix (e.g. from DISTRIB_DESCRIPTION)
     *  - strips a build revision suffix (e.g. " r23497-6637af95aa")
     *  - pads the minor segment to two digits ("23.5.3" -> "23.05.3")
     */
    fun normalize(version: String): String {
        var v = version.trim()
        if (v.startsWith("OpenWrt", ignoreCase = true)) {
            v = v.removePrefix("OpenWrt").removePrefix("openwrt").trim()
        }
        // strip build revision suffix like "r23497-6637af95aa"
        v = v.substringBefore(" r").trim()
        // strip anything after the third segment (e.g. "-rc2")
        val segments = v.split(" ").firstOrNull() ?: v
        val parts = segments.split(".")
        val normalized = parts.mapIndexed { index, part ->
            val trimmed = part.trim()
            if (index == 1) {
                val minor = trimmed.toIntOrNull()
                if (minor != null && trimmed.length == 1) {
                    minor.toString().padStart(2, '0')
                } else {
                    trimmed
                }
            } else {
                trimmed
            }
        }
        return normalized.joinToString(".")
    }

    /**
     * True when the (normalized) version lies within the supported
     * 23.05.0 - 23.05.6 range.
     */
    fun isSupported(version: String): Boolean {
        val v = normalize(version)
        if (v.isBlank()) return false
        val min = VersionUtils.compareVersions(v, MIN_SUPPORTED_VERSION)
        val max = VersionUtils.compareVersions(v, MAX_SUPPORTED_VERSION)
        return min >= 0 && max <= 0
    }

    /**
     * True when the version is an exact known point release (23.05.0..23.05.6).
     */
    fun isKnownPointRelease(version: String): Boolean {
        return SUPPORTED_VERSIONS.contains(normalize(version))
    }

    /**
     * The newest supported firmware version.
     */
    fun latestSupported(): String = MAX_SUPPORTED_VERSION

    /**
     * Compare a firmware version against the latest supported release.
     * @return positive when an update is available within the supported line,
     *         0 when up to date, negative when newer than the supported line.
     */
    fun compareWithLatest(version: String): Int {
        return VersionUtils.compareVersions(normalize(version), MAX_SUPPORTED_VERSION)
    }

    /**
     * Human readable summary of the supported range.
     */
    fun supportedRangeDescription(): String {
        return "$MIN_SUPPORTED_VERSION - $MAX_SUPPORTED_VERSION"
    }
}
