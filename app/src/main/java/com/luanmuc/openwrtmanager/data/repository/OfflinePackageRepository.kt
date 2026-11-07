package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import com.luanmuc.openwrtmanager.data.model.OfflineInstallResult
import com.luanmuc.openwrtmanager.data.model.OfflinePackageEntry
import com.luanmuc.openwrtmanager.data.model.OfflineResolution
import com.luanmuc.openwrtmanager.data.model.OfflineRouterContext
import com.luanmuc.openwrtmanager.data.model.OfflineSpaceInfo
import com.luanmuc.openwrtmanager.data.model.PackageManagerType
import com.luanmuc.openwrtmanager.util.OpenWrtVersions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * 离线安装仓库
 *
 * 从 OpenWrt 官方软件源（downloads.openwrt.org 及镜像）下载 .ipk 包到手机，
 * 在手机端解析依赖关系，然后通过 ubus file write 上传到路由器并用
 * opkg 本地安装 —— 路由器全程无需联网。
 */
class OfflinePackageRepository private constructor(private val context: Context) {

    private val luciRepository = LuciRepository.getInstance(context)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    // =====================================================================
    //  Router context detection
    // =====================================================================

    /**
     * 读取路由器版本 / 架构 / 内核 / 目标平台。
     * 架构优先使用 `opkg print-architecture`（精确值）。
     */
    suspend fun detectRouterContext(): OfflineRouterContext = withContext(Dispatchers.IO) {
        val info = luciRepository.getFullSystemInfo()
        val normalized = OpenWrtVersions.normalize(
            info.firmwareVersion.ifBlank { info.release }
        )

        // opkg print-architecture 第一行: "arch mipsel_24kc 10"
        val archOut = runCatching {
            luciRepository.executeCommand("opkg print-architecture 2>/dev/null | head -n1")
        }.getOrNull()
        val preciseArch = archOut?.trim()
            ?.split(" ")
            ?.firstOrNull { it.isNotBlank() && it != "arch" }
            ?.trim().orEmpty()

        val kernelOut = runCatching {
            luciRepository.executeCommand("uname -r")
        }.getOrNull()?.trim().orEmpty()

        OfflineRouterContext(
            version = normalized,
            architecture = preciseArch.ifBlank { info.architecture },
            kernelRelease = kernelOut.ifBlank { info.kernelVersion },
            target = info.target,
            distribution = info.distribution,
            packageManager = info.packageManager
        )
    }

    /**
     * 读取路由器上已安装的包名集合（含虚拟包名）。
     *
     * 优先解析 /usr/lib/opkg/status（同时读取 Package 与 Provides，
     * 与 opkg 自身的依赖判定一致：如 libc 提供 libpthread/librt/libdl 等虚拟包）；
     * 失败时回退到 opkg list-installed。
     */
    suspend fun getInstalledPackageNames(): Set<String> = withContext(Dispatchers.IO) {
        // 1) opkg status：包名 + 提供的虚拟名
        val status = runCatching {
            luciRepository.executeCommand(
                "grep -E '^(Package|Provides):' /usr/lib/opkg/status 2>/dev/null | cut -d: -f2 | tr ',' ' '"
            )
        }.getOrNull()
        val fromStatus = status
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()
        if (fromStatus.isNotEmpty()) return@withContext fromStatus

        // 2) 回退：opkg list-installed
        val out = runCatching {
            luciRepository.executeCommand("opkg list-installed 2>/dev/null | cut -d' ' -f1")
        }.getOrNull() ?: return@withContext emptySet()
        out.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    // =====================================================================
    //  Index fetching / caching
    // =====================================================================

    /**
     * 下载并解析全部官方 feed 的 Packages 索引。
     *
     * 包含 5 个架构 feed（base/luci/packages/routing/telephony）
     * 与 target 专属的 core feed（libc/libstdcpp6/iptables-nft 等 toolchain 包，
     * 对应路由器 distfeeds.conf 中的 openwrt_core）。
     * 失败的 feed 会被跳过（只要有一个成功即返回结果）。
     * @param onProgress 每完成一个 feed 回调 (完成的feed数, 总feed数, feed名)
     */
    suspend fun fetchPackageIndex(
        mirror: String,
        router: OfflineRouterContext,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): List<OfflinePackageEntry> = withContext(Dispatchers.IO) {
        val result = mutableListOf<OfflinePackageEntry>()
        // 全部 feed：5 个架构 feed + core（需要 target）
        val feedCount = FEEDS.size + if (router.target.isNotBlank()) 1 else 0
        var done = 0
        FEEDS.forEach { feed ->
            val url = feedIndexUrl(mirror, router.version, router.architecture, feed)
            val text = runCatching { fetchIndexText(url, cacheTag(router, feed)) }.getOrNull()
            if (text != null) {
                result += parsePackagesIndex(text, feed)
            }
            done++
            onProgress(done, feedCount, feed)
        }
        if (router.target.isNotBlank()) {
            val url = coreIndexUrl(mirror, router.version, router.target)
            val text = runCatching { fetchIndexText(url, cacheTag(router, "core")) }.getOrNull()
            if (text != null) {
                result += parsePackagesIndex(text, "core")
            }
            done++
            onProgress(done, feedCount, "core")
        }
        result
    }

    /**
     * 仅下载 kmods 仓库索引（按需：选中包依赖内核模块时才调用）。
     */
    suspend fun fetchKmodsIndex(
        mirror: String,
        router: OfflineRouterContext
    ): List<OfflinePackageEntry>? = withContext(Dispatchers.IO) {
        if (router.target.isBlank() || router.kernelRelease.isBlank()) return@withContext null
        val url = kmodsIndexUrl(mirror, router.version, router.target, router.kernelRelease)
        val text = runCatching { fetchIndexText(url, cacheTag(router, "kmods")) }.getOrNull()
            ?: return@withContext null
        parsePackagesIndex(text, "kmods").ifEmpty { null }
    }

    private fun cacheTag(router: OfflineRouterContext, feed: String): File {
        return File(indexCacheDir(), "${sanitize(router.version)}_${sanitize(router.architecture)}_$feed.txt")
    }

    private fun indexCacheDir(): File {
        val dir = File(context.cacheDir, "offline_index")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun sanitize(s: String): String =
        s.replace(Regex("[^A-Za-z0-9._-]"), "_")

    /**
     * 下载 Packages.gz（优先网络），网络不可用时回退到本地缓存。
     */
    private fun fetchIndexText(url: String, cacheFile: File): String {
        // 网络获取
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val bytes = response.body?.bytes() ?: throw IllegalStateException("Empty body: $url")
                val text = gunzip(bytes)
                runCatching { cacheFile.writeText(text) }
                return text
            }
        }
        // 回退缓存
        if (cacheFile.exists()) {
            return cacheFile.readText()
        }
        throw IllegalStateException("HTTP error and no cache for $url")
    }

    private fun gunzip(data: ByteArray): String {
        return GZIPInputStream(data.inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    // =====================================================================
    //  Download of .ipk files
    // =====================================================================

    /** 本地存放 .ipk 的目录（按 版本_架构 分区，支持"先下载后安装"的离线流程） */
    fun ipkDir(router: OfflineRouterContext): File {
        val dir = File(context.cacheDir, "offline_ipk/${sanitize(router.version)}_${sanitize(router.architecture)}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 下载单个 .ipk 到手机。若本地已存在且 SHA256 匹配则直接复用。
     * @return 本地文件
     */
    suspend fun downloadPackage(
        mirror: String,
        router: OfflineRouterContext,
        entry: OfflinePackageEntry,
        onProgress: (deltaBytes: Long) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val dest = File(ipkDir(router), entry.filename.ifBlank { "${entry.name}.ipk" })

        if (dest.exists() && dest.length() == entry.size && verifySha256(dest, entry.sha256)) {
            return@withContext dest // 已缓存
        }

        val url = downloadUrlFor(mirror, router, entry)
        val tmp = File(dest.parentFile, dest.name + ".part")
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} for ${entry.name}")
            }
            val body = response.body ?: throw IllegalStateException("Empty body: ${entry.name}")
            FileOutputStream(tmp).use { out ->
                val src = body.byteStream()
                val buf = ByteArray(32 * 1024)
                while (true) {
                    val n = src.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    onProgress(n.toLong())
                }
                out.flush()
            }
        }

        if (entry.sha256.isNotBlank() && !verifySha256(tmp, entry.sha256)) {
            tmp.delete()
            throw IllegalStateException("SHA256 mismatch: ${entry.name}")
        }

        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        dest
    }

    private fun verifySha256(file: File, expected: String): Boolean {
        if (expected.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
            .equals(expected, ignoreCase = true)
    }

    // =====================================================================
    //  Upload + install on the router
    // =====================================================================

    /**
     * 检查路由器 /tmp 与 /overlay 剩余空间（字节）。
     */
    suspend fun queryRouterSpace(): OfflineSpaceInfo = withContext(Dispatchers.IO) {
        val out = runCatching {
            luciRepository.executeCommand("df -k /tmp /overlay 2>/dev/null | awk 'NR>1 {print \$NF, \$4}'")
        }.getOrNull().orEmpty()
        var tmpFree = -1L
        var overlayFree = -1L
        out.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            val parts = line.split(Regex("\\s+"))
            if (parts.size == 2) {
                val kb = parts[1].toLongOrNull() ?: return@forEach
                when {
                    parts[0] == "/tmp" && tmpFree < 0 -> tmpFree = kb * 1024
                    parts[0] == "/overlay" && overlayFree < 0 -> overlayFree = kb * 1024
                }
            }
        }
        OfflineSpaceInfo(tmpFree = tmpFree, overlayFree = overlayFree)
    }

    /**
     * 将 .ipk 文件上传到路由器并通过 opkg 本地安装（路由器无需联网）。
     *
     * 空间充足时：全部上传后一次 `opkg install *.ipk`（依赖关系由 opkg 统一解析）。
     * /tmp 空间不足时：按依赖优先顺序逐个“上传 → 安装 → 删除”，/tmp 峰值仅为最大单包。
     *
     * @param entries 对应的包条目（与 files 一一对应，用于依赖排序）
     * @param files 本地 .ipk 文件
     * @param onUploadProgress (已上传字节, 总字节)
     * @param onLog 安装过程日志
     */
    suspend fun uploadAndInstall(
        entries: List<OfflinePackageEntry>,
        files: List<File>,
        onUploadProgress: (Long, Long) -> Unit = { _, _ -> },
        onLog: (String) -> Unit = {}
    ): OfflineInstallResult = withContext(Dispatchers.IO) {
        if (files.isEmpty() || files.size != entries.size) {
            return@withContext OfflineInstallResult(error = "no files")
        }
        val totalBytes = files.sumOf { it.length() }
        var uploaded = 0L
        val remoteDir = REMOTE_DIR
        val pairs = files.indices.map { i -> entries[i] to files[i] }

        try {
            onLog("$ opkg offline install: ${files.size} package(s), ${formatBytes(totalBytes)}")
            luciRepository.execCommandFull("mkdir -p $remoteDir")

            val space = queryRouterSpace()

            // 上传单个文件（分块 base64）
            suspend fun uploadFile(file: File, remotePath: String, onChunk: (Int) -> Unit): Boolean {
                val data = file.readBytes()
                var offset = 0
                var chunkIndex = 0
                while (offset < data.size) {
                    val end = minOf(offset + UPLOAD_CHUNK_BINARY, data.size)
                    val b64 = Base64.getEncoder().encodeToString(data.copyOfRange(offset, end))
                    val ok = luciRepository.writeRemoteFile(
                        path = remotePath,
                        dataBase64 = b64,
                        append = chunkIndex > 0,
                        mode = 420 // 0644
                    )
                    if (!ok) return false
                    onChunk(end - offset)
                    offset = end
                    chunkIndex++
                }
                return true
            }

            // 单包安装命令
            fun installOneCmd(file: File) = "opkg install $remoteDir/${file.name} 2>&1"

            val batchMode = space.tmpFree < 0 || space.tmpFree >= totalBytes
            if (!batchMode) {
                val largest = files.maxOf { it.length() }
                if (space.tmpFree in 0..largest) {
                    return@withContext OfflineInstallResult(
                        success = false,
                        error = "SPACE:/tmp=${space.tmpFree},need=$largest"
                    )
                }
            }

            val outputBuilder = StringBuilder()
            var failed = false
            var failMsg: String? = null
            var installedCount = 0

            if (batchMode) {
                // ---- 批量模式：全部上传后一次安装 ----
                for ((_, file) in pairs) {
                    val ok = uploadFile(file, "$remoteDir/${file.name}") { chunkLen ->
                        uploaded += chunkLen
                        onUploadProgress(uploaded.coerceAtMost(totalBytes), totalBytes)
                    }
                    if (!ok) {
                        runCatching { luciRepository.execCommandFull("rm -rf $remoteDir") }
                        return@withContext OfflineInstallResult(
                            success = false,
                            error = "UPLOAD:${file.name}",
                            output = outputBuilder.toString()
                        )
                    }
                    onLog("+ ${file.name} (${file.length() / 1024} KB)")
                }
                onLog("$ opkg install ${files.size} package(s)")
                val exec = luciRepository.execCommandFull("cd $remoteDir && opkg install *.ipk 2>&1")
                val output = exec.combinedOutput
                if (output.isNotBlank()) {
                    outputBuilder.append(output).append('\n')
                    onLog(output)
                }
                installedCount = output.lineSequence().count { it.startsWith("Installing ") }
                failed = exec.code != 0 ||
                        output.contains("Collected errors") ||
                        output.contains("Cannot install package", ignoreCase = true)
                if (failed) failMsg = "OPKG:${exec.code}"
            } else {
                // ---- 顺序模式：依赖优先，逐个上传/安装/删除 ----
                val ordered = sortDependenciesFirst(
                    OfflineResolution(toDownload = entries)
                )
                for (entry in ordered) {
                    val idx = entries.indexOfFirst { it.name == entry.name }
                    if (idx < 0) continue
                    val file = files[idx]
                    val remotePath = "$remoteDir/${file.name}"
                    val ok = uploadFile(file, remotePath) { chunkLen ->
                        uploaded += chunkLen
                        onUploadProgress(uploaded.coerceAtMost(totalBytes), totalBytes)
                    }
                    if (!ok) {
                        failed = true
                        failMsg = "UPLOAD:${file.name}"
                        break
                    }
                    onLog("+ ${file.name} (${file.length() / 1024} KB)")
                    val exec = luciRepository.execCommandFull(installOneCmd(file))
                    val output = exec.combinedOutput
                    if (output.isNotBlank()) {
                        outputBuilder.append(output).append('\n')
                        onLog(output)
                    }
                    if (output.lineSequence().any { it.startsWith("Installing ") }) installedCount++
                    val pkgFailed = exec.code != 0 ||
                            output.contains("Collected errors") ||
                            output.contains("Cannot install package", ignoreCase = true)
                    runCatching { luciRepository.execCommandFull("rm -f $remotePath") }
                    if (pkgFailed) {
                        failed = true
                        failMsg = "OPKG:${exec.code}:${entry.name}"
                        break
                    }
                }
            }

            // ---- 清理 ----
            runCatching { luciRepository.execCommandFull("rm -rf $remoteDir") }
            onLog("$ rm -rf $remoteDir")

            OfflineInstallResult(
                success = !failed,
                output = outputBuilder.toString().trim(),
                installedCount = installedCount,
                error = failMsg
            )
        } catch (e: Exception) {
            runCatching { luciRepository.execCommandFull("rm -rf $remoteDir") }
            OfflineInstallResult(
                success = false,
                error = e.message ?: "upload/install failed"
            )
        }
    }

    // =====================================================================
    //  Companion: pure logic (unit-testable)
    // =====================================================================

    companion object {
        /** 官方软件源 */
        const val DEFAULT_MIRROR = "https://downloads.openwrt.org"

        /** 23.05 的 5 个官方 feed */
        val FEEDS = listOf("base", "luci", "packages", "routing", "telephony")

        /** 远程上传目录 */
        const val REMOTE_DIR = "/tmp/offline-pkgs"

        /**
         * 每块上传的二进制大小（base64 后约 44KB，
         * 小于 uhttpd ubus POST 上限 64KB 与 rpcd 写入上限 256KB）
         */
        const val UPLOAD_CHUNK_BINARY = 32 * 1024

        /**
         * 系统级 / 由 libc 提供的虚拟包。
         * 官方 feed 中没有可下载条目，永远视为已满足
         * （与 musl 合并 libpthread/librt/libdl/libatomic1 的 23.05 事实一致）。
         */
        val SYSTEM_PROVIDED = setOf(
            "kernel", "libc", "libpthread", "librt", "libdl", "libatomic1", "libssp"
        )

        @Volatile
        private var instance: OfflinePackageRepository? = null

        fun getInstance(context: Context): OfflinePackageRepository {
            return instance ?: synchronized(this) {
                instance ?: OfflinePackageRepository(context.applicationContext)
                    .also { instance = it }
            }
        }

        // ---------- URL builders ----------

        fun feedIndexUrl(mirror: String, version: String, arch: String, feed: String): String =
            "${mirror.trimEnd('/')}/releases/$version/packages/$arch/$feed/Packages.gz"

        /** target 专属 core feed（对应 distfeeds.conf 的 openwrt_core） */
        fun coreIndexUrl(mirror: String, version: String, target: String): String =
            "${mirror.trimEnd('/')}/releases/$version/targets/$target/packages/Packages.gz"

        fun kmodsIndexUrl(mirror: String, version: String, target: String, kernelRelease: String): String =
            "${mirror.trimEnd('/')}/releases/$version/targets/$target/kmods/$kernelRelease/Packages.gz"

        fun ipkUrl(mirror: String, version: String, arch: String, feed: String, filename: String): String =
            "${mirror.trimEnd('/')}/releases/$version/packages/$arch/$feed/$filename"

        fun coreIpkUrl(mirror: String, version: String, target: String, filename: String): String =
            "${mirror.trimEnd('/')}/releases/$version/targets/$target/packages/$filename"

        fun kmodsIpkUrl(mirror: String, version: String, target: String, kernelRelease: String, filename: String): String =
            "${mirror.trimEnd('/')}/releases/$version/targets/$target/kmods/$kernelRelease/$filename"

        /** kmod 包的下载地址（kmods 仓库位于 targets 下） */
        fun downloadUrlFor(mirror: String, router: OfflineRouterContext, entry: OfflinePackageEntry): String =
            when {
                entry.feed == "kmods" -> kmodsIpkUrl(mirror, router.version, router.target, router.kernelRelease, entry.filename)
                entry.feed == "core" -> coreIpkUrl(mirror, router.version, router.target, entry.filename)
                else -> ipkUrl(mirror, router.version, router.architecture, entry.feed, entry.filename)
            }

        // ---------- Index parsing ----------

        /**
         * 解析 Debian 风格的 Packages 索引文本。
         * Description 支持多行续行（以空格开头的行）。
         */
        fun parsePackagesIndex(content: String, feed: String): List<OfflinePackageEntry> {
            val entries = mutableListOf<OfflinePackageEntry>()
            val fields = linkedMapOf<String, StringBuilder>()

            fun flush() {
                if (fields.isEmpty()) return
                val get: (String) -> String = { key -> fields[key]?.toString()?.trim() ?: "" }
                val name = get("Package")
                if (name.isNotBlank()) {
                    entries.add(
                        OfflinePackageEntry(
                            name = name,
                            version = get("Version"),
                            depends = parseDependNames(get("Depends")),
                            provides = get("Provides").split(",")
                                .map { depNameOf(it) }
                                .filter { it.isNotEmpty() && it != name },
                            filename = get("Filename"),
                            size = get("Size").toLongOrNull() ?: 0L,
                            installedSize = get("Installed-Size").toLongOrNull() ?: 0L,
                            sha256 = get("SHA256sum"),
                            architecture = get("Architecture"),
                            section = get("Section"),
                            description = get("Description").replaceFirstChar { it.uppercaseChar() },
                            feed = feed
                        )
                    )
                }
                fields.clear()
            }

            content.lineSequence().forEach { rawLine ->
                val line = rawLine.trimEnd('\r')
                when {
                    line.isEmpty() -> flush()
                    line.startsWith(" ") || line.startsWith("\t") -> {
                        // Description 续行
                        val lastKey = fields.keys.lastOrNull() ?: return@forEach
                        fields[lastKey]?.append('\n')?.append(line.trim())
                    }
                    else -> {
                        val idx = line.indexOf(':')
                        if (idx > 0) {
                            val key = line.substring(0, idx).trim()
                            val value = line.substring(idx + 1).trimStart()
                            fields[key] = StringBuilder(value)
                        }
                    }
                }
            }
            flush()
            return entries
        }

        /**
         * "libc, libubus20220927 (>= 1.0), luci-base" -> ["libc", "libubus20220927", "luci-base"]
         */
        fun parseDependNames(dependsLine: String): List<String> {
            return dependsLine.split(",")
                .map { depNameOf(it) }
                .filter { it.isNotEmpty() }
        }

        /** 去掉版本约束，只留依赖名 */
        private fun depNameOf(spec: String): String {
            return spec.substringBefore('(').trim()
        }

        // ---------- Dependency resolution ----------

        /**
         * 计算选中包的依赖闭包。
         *
         * @param selectedNames 用户选中的包名
         * @param mainUniverse 5 个官方 feed 的全部条目
         * @param kmodsUniverse kmods feed 条目（可为空）
         * @param installed 路由器已安装的包名（含虚拟名，来自 opkg list-installed）
         */
        fun resolvePackages(
            selectedNames: Collection<String>,
            mainUniverse: List<OfflinePackageEntry>,
            kmodsUniverse: List<OfflinePackageEntry> = emptyList(),
            installed: Set<String> = emptySet()
        ): OfflineResolution {
            data class Universe(
                val byName: Map<String, OfflinePackageEntry>,
                val byProvides: Map<String, OfflinePackageEntry>
            )

            fun buildUniverse(list: List<OfflinePackageEntry>): Universe {
                val byName = LinkedHashMap<String, OfflinePackageEntry>()
                val byProvides = LinkedHashMap<String, OfflinePackageEntry>()
                for (e in list) {
                    if (!byName.containsKey(e.name)) byName[e.name] = e
                    for (p in e.provides) {
                        if (!byProvides.containsKey(p)) byProvides[p] = e
                    }
                    // 包本身也算 provides 自身（覆盖同名虚拟包优先级）
                    if (!byProvides.containsKey(e.name)) byProvides[e.name] = e
                }
                return Universe(byName, byProvides)
            }

            val main = buildUniverse(mainUniverse)
            val kmods = buildUniverse(kmodsUniverse)

            val toDownload = LinkedHashMap<String, OfflinePackageEntry>()
            val alreadyInstalled = mutableListOf<String>()
            val missing = mutableListOf<String>()
            val missingKmods = mutableListOf<String>()

            fun findEntry(name: String): OfflinePackageEntry? {
                val isKmod = name.startsWith("kmod-")
                if (isKmod) {
                    return kmods.byName[name] ?: kmods.byProvides[name]
                            ?: main.byName[name] ?: main.byProvides[name]
                }
                return main.byName[name] ?: main.byProvides[name]
            }

            val visited = mutableSetOf<String>()
            val queue = ArrayDeque<String>()
            selectedNames.forEach { name ->
                if (installed.contains(name)) {
                    alreadyInstalled.add(name)
                } else {
                    queue.add(name)
                }
            }

            while (queue.isNotEmpty()) {
                val name = queue.removeFirst()
                if (!visited.add(name)) continue

                // 内核/系统级虚拟包永远视为已满足
                if (SYSTEM_PROVIDED.contains(name)) continue
                if (installed.contains(name)) continue

                val entry = findEntry(name)
                if (entry == null) {
                    if (name.startsWith("kmod-")) {
                        missingKmods.add(name)
                    } else {
                        missing.add(name)
                    }
                    continue
                }
                toDownload[name] = entry
                entry.depends.forEach { dep -> queue.add(dep) }
            }

            val list = toDownload.values.toList()
            return OfflineResolution(
                toDownload = list,
                alreadyInstalled = alreadyInstalled.distinct(),
                missing = missing.distinct(),
                missingKmods = missingKmods.distinct(),
                totalBytes = list.sumOf { it.size },
                totalInstalledBytes = list.sumOf { it.installedSize }
            )
        }

        /**
         * 按依赖发现顺序排序（依赖在前），用于空间紧张时的逐个安装。
         */
        fun sortDependenciesFirst(resolution: OfflineResolution): List<OfflinePackageEntry> {
            return resolution.toDownload.asReversed()
        }

        /**
         * 将字节格式化为可读大小。
         */
        fun formatBytes(bytes: Long): String {
            if (bytes < 0) return "?"
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format("%.0f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            return String.format("%.2f GB", mb / 1024.0)
        }
    }
}
