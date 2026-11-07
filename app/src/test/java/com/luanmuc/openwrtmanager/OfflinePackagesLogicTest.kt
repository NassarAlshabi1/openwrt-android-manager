package com.luanmuc.openwrtmanager

import com.luanmuc.openwrtmanager.data.model.OfflinePackageEntry
import com.luanmuc.openwrtmanager.data.model.OfflineRouterContext
import com.luanmuc.openwrtmanager.data.repository.OfflinePackageRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the offline installer pure logic:
 * index parsing, dependency resolution, URL building and helpers.
 */
class OfflinePackagesLogicTest {

    // ---------- URL builders ----------

    @Test
    fun `feed index url matches official layout`() {
        assertEquals(
            "https://downloads.openwrt.org/releases/23.05.5/packages/mipsel_24kc/luci/Packages.gz",
            OfflinePackageRepository.feedIndexUrl(
                "https://downloads.openwrt.org", "23.05.5", "mipsel_24kc", "luci"
            )
        )
    }

    @Test
    fun `mirror trailing slash is normalized`() {
        assertEquals(
            "https://mirror.example.org/releases/23.05.0/packages/x86_64/base/Packages.gz",
            OfflinePackageRepository.feedIndexUrl(
                "https://mirror.example.org/", "23.05.0", "x86_64", "base"
            )
        )
    }

    @Test
    fun `kmods url uses targets layout`() {
        assertEquals(
            "https://downloads.openwrt.org/releases/23.05.5/targets/ramips/mt7621/kmods/5.15.167-1-abc/Packages.gz",
            OfflinePackageRepository.kmodsIndexUrl(
                "https://downloads.openwrt.org", "23.05.5", "ramips/mt7621", "5.15.167-1-abc"
            )
        )
    }

    @Test
    fun `core feed url uses targets layout`() {
        assertEquals(
            "https://downloads.openwrt.org/releases/23.05.5/targets/ramips/mt7621/packages/Packages.gz",
            OfflinePackageRepository.coreIndexUrl(
                "https://downloads.openwrt.org", "23.05.5", "ramips/mt7621"
            )
        )
    }

    @Test
    fun `download url for kmod uses kmods path`() {
        val router = OfflineRouterContext(
            version = "23.05.5", architecture = "mipsel_24kc",
            kernelRelease = "5.15.167-1-abc", target = "ramips/mt7621"
        )
        val entry = OfflinePackageEntry(
            name = "kmod-nf-nat", filename = "kmod-nf-nat_5.15_ipk.ipk", feed = "kmods"
        )
        assertEquals(
            "https://downloads.openwrt.org/releases/23.05.5/targets/ramips/mt7621/kmods/5.15.167-1-abc/kmod-nf-nat_5.15_ipk.ipk",
            OfflinePackageRepository.downloadUrlFor(
                OfflinePackageRepository.DEFAULT_MIRROR, router, entry
            )
        )
    }

    @Test
    fun `download url for core feed uses targets packages path`() {
        val router = OfflineRouterContext(
            version = "23.05.5", architecture = "mipsel_24kc", target = "ramips/mt7621"
        )
        val entry = OfflinePackageEntry(
            name = "libstdcpp6", filename = "libstdcpp6_12.3.0-4_mipsel_24kc.ipk", feed = "core"
        )
        assertEquals(
            "https://downloads.openwrt.org/releases/23.05.5/targets/ramips/mt7621/packages/libstdcpp6_12.3.0-4_mipsel_24kc.ipk",
            OfflinePackageRepository.downloadUrlFor(
                OfflinePackageRepository.DEFAULT_MIRROR, router, entry
            )
        )
    }

    // ---------- Index parsing ----------

    @Test
    fun `parses stanzas with all fields`() {
        val index = """
            Package: luci-app-example
            Version: git-25.222.75657-7ce34fe
            Depends: libc, luci-base, libubus20220927 (>= 20220927)
            Provides: luci-app-example-virtual
            License: Apache-2.0
            Section: luci
            Architecture: all
            Installed-Size: 3394
            Filename: luci-app-example_git-25.222.75657-7ce34fe_all.ipk
            Size: 4234
            SHA256sum: 09477a37c26b018ddc431a431869fb213e9f2396b1cf70b36e332b6873137dd3
            Description: LuCI example application
             Long description line 2

            Package: libfoo
            Version: 1.0-1
            Depends: libc
            Architecture: mipsel_24kc
            Filename: libfoo_1.0-1_mipsel_24kc.ipk
            Size: 1000
            Description:  Foo library
        """.trimIndent()

        val entries = OfflinePackageRepository.parsePackagesIndex(index, "luci")

        assertEquals(2, entries.size)

        val first = entries[0]
        assertEquals("luci-app-example", first.name)
        assertEquals("git-25.222.75657-7ce34fe", first.version)
        // 版本约束被剥离
        assertEquals(listOf("libc", "luci-base", "libubus20220927"), first.depends)
        assertEquals(listOf("luci-app-example-virtual"), first.provides)
        assertEquals("all", first.architecture)
        assertEquals(4234L, first.size)
        assertEquals(3394L, first.installedSize)
        assertEquals("luci", first.feed)
        assertTrue(first.description.contains("Long description line 2"))

        val second = entries[1]
        assertEquals("libfoo", second.name)
        assertEquals("mipsel_24kc", second.architecture)
        assertEquals(listOf("libc"), second.depends)
    }

    @Test
    fun `architecture matching accepts all and exact`() {
        val e = OfflinePackageEntry(name = "x", architecture = "all")
        assertTrue(e.matchesArchitecture("mipsel_24kc"))
        assertFalse(OfflinePackageEntry(name = "x", architecture = "x86_64").matchesArchitecture("mipsel_24kc"))
        assertTrue(OfflinePackageEntry(name = "x", architecture = "mipsel_24kc").matchesArchitecture("mipsel_24kc"))
    }

    // ---------- Dependency resolution ----------

    private fun entry(
        name: String,
        depends: List<String> = emptyList(),
        provides: List<String> = emptyList(),
        size: Long = 100
    ) = OfflinePackageEntry(
        name = name, version = "1.0", depends = depends, provides = provides,
        filename = "$name.ipk", size = size, installedSize = size * 2,
        architecture = "all", feed = "luci"
    )

    @Test
    fun `resolves transitive dependencies`() {
        val universe = listOf(
            entry("luci-app-example", depends = listOf("luci-base", "libx")),
            entry("luci-base", depends = listOf("libc", "libubus")),
            entry("libx"),
            entry("libubus"),
            entry("libc")
        )
        val result = OfflinePackageRepository.resolvePackages(
            selectedNames = listOf("luci-app-example"),
            mainUniverse = universe,
            installed = setOf("libc") // libc 已安装
        )
        // libc 被跳过，其余全部进入下载列表
        assertEquals(setOf("luci-app-example", "luci-base", "libx", "libubus"), result.toDownload.map { it.name }.toSet())
        assertTrue(result.missing.isEmpty())
        assertTrue(result.missingKmods.isEmpty())
        assertEquals(400L, result.totalBytes)
    }

    @Test
    fun `already installed selection is skipped`() {
        val universe = listOf(entry("luci-base"), entry("luci-app-x", depends = listOf("luci-base")))
        val result = OfflinePackageRepository.resolvePackages(
            selectedNames = listOf("luci-app-x"),
            mainUniverse = universe,
            installed = setOf("luci-app-x", "luci-base")
        )
        assertTrue(result.toDownload.isEmpty())
        assertEquals(listOf("luci-app-x"), result.alreadyInstalled)
    }

    @Test
    fun `virtual provides are resolved`() {
        val universe = listOf(
            entry("luci-app-x", depends = listOf("virtual-thing")),
            entry("real-package", provides = listOf("virtual-thing"))
        )
        val result = OfflinePackageRepository.resolvePackages(
            selectedNames = listOf("luci-app-x"),
            mainUniverse = universe
        )
        assertEquals(listOf("luci-app-x", "real-package"), result.toDownload.map { it.name })
    }

    @Test
    fun `kernel dependency is always satisfied`() {
        val universe = listOf(entry("kmod-test", depends = listOf("kernel")))
        val result = OfflinePackageRepository.resolvePackages(
            selectedNames = listOf("kmod-test"),
            mainUniverse = universe
        )
        assertTrue(result.missing.isEmpty())
        assertEquals(1, result.toDownload.size)
    }

    @Test
    fun `libc provided virtual packages are treated as satisfied`() {
        // 23.05 musl: libpthread/librt/libdl/libatomic1 由 libc 提供，无 feed 条目
        val universe = listOf(
            entry("luci-app-x", depends = listOf("libc", "libpthread", "librt", "libatomic1", "libdl"))
        )
        val result = OfflinePackageRepository.resolvePackages(
            selectedNames = listOf("luci-app-x"),
            mainUniverse = universe
        )
        assertTrue(result.missing.isEmpty())
        assertEquals(listOf("luci-app-x"), result.toDownload.map { it.name })
    }

    @Test
    fun `kmod resolved from kmods universe`() {
        val main = listOf(entry("luci-app-wg", depends = listOf("kmod-wireguard")))
        val kmods = listOf(
            OfflinePackageEntry(
                name = "kmod-wireguard", depends = listOf("kernel"),
                filename = "kmod-wireguard.ipk", size = 500, feed = "kmods",
                architecture = "mipsel_24kc"
            )
        )
        val result = OfflinePackageRepository.resolvePackages(
            selectedNames = listOf("luci-app-wg"),
            mainUniverse = main,
            kmodsUniverse = kmods
        )
        assertTrue(result.missingKmods.isEmpty())
        assertEquals(setOf("luci-app-wg", "kmod-wireguard"), result.toDownload.map { it.name }.toSet())
    }

    @Test
    fun `missing kmod reported separately`() {
        val main = listOf(entry("luci-app-wg", depends = listOf("kmod-wireguard", "libfoo")))
        val result = OfflinePackageRepository.resolvePackages(
            selectedNames = listOf("luci-app-wg"),
            mainUniverse = main,
            installed = setOf("libfoo") // 非 kmod 依赖已安装
        )
        assertEquals(listOf("kmod-wireguard"), result.missingKmods)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun `circular dependencies do not loop forever`() {
        val universe = listOf(
            entry("a", depends = listOf("b")),
            entry("b", depends = listOf("a"))
        )
        val result = OfflinePackageRepository.resolvePackages(
            selectedNames = listOf("a"),
            mainUniverse = universe
        )
        assertEquals(setOf("a", "b"), result.toDownload.map { it.name }.toSet())
    }

    // ---------- Formatting ----------

    @Test
    fun `format bytes human readable`() {
        assertEquals("512 B", OfflinePackageRepository.formatBytes(512))
        assertEquals("1 KB", OfflinePackageRepository.formatBytes(1024))
        assertEquals("1.5 MB", OfflinePackageRepository.formatBytes(1024 * 1024 + 512 * 1024))
        assertEquals("?", OfflinePackageRepository.formatBytes(-1))
    }

    @Test
    fun `parse depend names strips constraints`() {
        assertEquals(
            listOf("knot-libs", "libc", "libatomic1"),
            OfflinePackageRepository.parseDependNames("knot-libs (=3.3.5-1), libc, libatomic1")
        )
        assertEquals(
            emptyList<String>(),
            OfflinePackageRepository.parseDependNames("")
        )
    }
}
