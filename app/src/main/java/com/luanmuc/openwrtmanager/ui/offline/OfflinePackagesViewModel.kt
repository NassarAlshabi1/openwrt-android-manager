package com.luanmuc.openwrtmanager.ui.offline

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.data.model.OfflineInstallResult
import com.luanmuc.openwrtmanager.data.model.OfflinePackageEntry
import com.luanmuc.openwrtmanager.data.model.OfflinePhase
import com.luanmuc.openwrtmanager.data.model.OfflineResolution
import com.luanmuc.openwrtmanager.data.model.OfflineRouterContext
import com.luanmuc.openwrtmanager.data.model.OfflineSpaceInfo
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.OfflinePackageRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.util.Loc
import com.luanmuc.openwrtmanager.util.OpenWrtVersions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 离线安装 ViewModel
 *
 * 流程：检测路由器 → 拉取官方 feed 索引 → 用户选包 → 依赖解析
 *      → 下载 .ipk 到手机（SHA256 校验）→ 上传路由器 → opkg 本地安装。
 * 支持“先下载、后安装”的两段式离线工作流。
 */
class OfflinePackagesViewModel(application: Application) : BaseViewModel(application) {

    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(application)
    private val offlineRepository = OfflinePackageRepository.getInstance(application)

    private val prefs = application.getSharedPreferences("offline_prefs", android.content.Context.MODE_PRIVATE)

    data class OfflineUiState(
        val phase: OfflinePhase = OfflinePhase.IDLE,
        val router: OfflineRouterContext? = null,
        val detectError: String? = null,
        val isSupportedVersion: Boolean = true,
        val versionOverride: String = "",
        val mirror: String = OfflinePackageRepository.DEFAULT_MIRROR,

        // 索引
        val indexLoading: Boolean = false,
        val indexProgress: Int = 0,
        val indexTotal: Int = OfflinePackageRepository.FEEDS.size,
        val indexCount: Int = 0,
        val indexError: String? = null,
        val kmodsLoaded: Boolean = false,
        val kmodsUnavailable: Boolean = false,

        // 列表与选择
        val installedNames: Set<String> = emptySet(),
        val searchQuery: String = "",
        val feedFilter: String? = null,
        val hideInstalled: Boolean = true,
        val selected: Set<String> = emptySet(),
        val resolution: OfflineResolution? = null,

        // 下载
        val currentFile: String? = null,
        val downloadedBytes: Long = 0,
        val totalBytes: Long = 0,
        val downloadedNames: Set<String> = emptySet(),

        // 上传/安装
        val uploadedBytes: Long = 0,
        val uploadTotal: Long = 0,
        val opLog: List<String> = emptyList(),
        val opResult: OfflineInstallResult? = null,
        val opError: String? = null,

        // 路由器空间
        val spaceInfo: OfflineSpaceInfo? = null
    ) {
        val isBusy: Boolean
            get() = phase == OfflinePhase.DETECTING || phase == OfflinePhase.LOADING_INDEX ||
                    phase == OfflinePhase.DOWNLOADING || phase == OfflinePhase.UPLOADING ||
                    phase == OfflinePhase.INSTALLING

        val allDownloaded: Boolean
            get() {
                val res = resolution ?: return false
                return res.toDownload.isNotEmpty() &&
                        res.toDownload.all { downloadedNames.contains(it.name) }
            }
    }

    private val _uiState = MutableStateFlow(OfflineUiState())
    val uiState: StateFlow<OfflineUiState> = _uiState.asStateFlow()

    // 索引数据（不进 UI state，避免大列表频繁复制）
    private var indexEntries: List<OfflinePackageEntry> = emptyList()
    private var kmodsEntries: List<OfflinePackageEntry> = emptyList()

    private var opJob: Job? = null

    init {
        initNetworkMonitor()
        start()
    }

    override fun refreshData() {
        if (!_uiState.value.isBusy) {
            start()
        }
    }

    private fun effectiveRouter(state: OfflineUiState = _uiState.value): OfflineRouterContext {
        val ctx = state.router ?: OfflineRouterContext()
        return if (state.versionOverride.isNotBlank() && state.versionOverride != ctx.version) {
            ctx.copy(version = state.versionOverride)
        } else ctx
    }

    // =====================================================================
    //  启动 / 检测
    // =====================================================================

    fun start() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                phase = OfflinePhase.DETECTING,
                detectError = null,
                mirror = prefs.getString("mirror", OfflinePackageRepository.DEFAULT_MIRROR)
                    ?: OfflinePackageRepository.DEFAULT_MIRROR
            )

            try {
                val active = ensureLogin()
                if (active == null) {
                    _uiState.value = _uiState.value.copy(
                        phase = OfflinePhase.FAILED,
                        detectError = Loc.s(R.string.offline_no_router)
                    )
                    return@launch
                }

                val ctx = offlineRepository.detectRouterContext()
                val supported = OpenWrtVersions.isSupported(ctx.version)
                _uiState.value = _uiState.value.copy(
                    phase = OfflinePhase.IDLE,
                    router = ctx,
                    isSupportedVersion = supported
                )

                // 加载已安装列表 + 空间信息
                launch { loadInstalled() }
                launch {
                    val space = offlineRepository.queryRouterSpace()
                    _uiState.value = _uiState.value.copy(spaceInfo = space)
                }
                loadIndex()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = OfflinePhase.FAILED,
                    detectError = e.message ?: Loc.s(R.string.offline_detect_failed, "unknown")
                )
            }
        }
    }

    private suspend fun ensureLogin(): Router? {
        val routers = routerRepository.getRoutersList()
        val activeId = routerRepository.getActiveRouterId()
        val activeRouter = if (activeId != null) {
            routers.find { it.id == activeId } ?: routers.firstOrNull()
        } else {
            routers.firstOrNull()
        } ?: return null

        if (!luciRepository.isLoggedIn()) {
            val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
            luciRepository.login(activeRouter.address, activeRouter.username, password)
        }
        return activeRouter
    }

    private suspend fun loadInstalled() {
        runCatching {
            val installed = offlineRepository.getInstalledPackageNames()
            if (installed.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(installedNames = installed)
                recomputeResolution()
            }
        }
    }

    // =====================================================================
    //  索引加载
    // =====================================================================

    fun loadIndex() {
        viewModelScope.launch {
            val state = _uiState.value
            val router = effectiveRouter(state)
            if (!router.isUsable) return@launch

            _uiState.value = state.copy(
                phase = OfflinePhase.LOADING_INDEX,
                indexLoading = true,
                indexError = null,
                indexProgress = 0
            )

            try {
                val entries = offlineRepository.fetchPackageIndex(state.mirror, router) { done, total, _ ->
                    _uiState.value = _uiState.value.copy(
                        indexProgress = done,
                        indexTotal = total
                    )
                }
                if (entries.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        phase = OfflinePhase.IDLE,
                        indexLoading = false,
                        indexError = Loc.s(R.string.offline_index_empty)
                    )
                    return@launch
                }
                indexEntries = entries
                _uiState.value = _uiState.value.copy(
                    phase = OfflinePhase.IDLE,
                    indexLoading = false,
                    indexCount = entries.size
                )
                refreshLocalFiles()
                recomputeResolution()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = OfflinePhase.IDLE,
                    indexLoading = false,
                    indexError = e.message ?: Loc.s(R.string.offline_index_failed, "")
                )
            }
        }
    }

    /** 扫描本地已下载文件（支持离线二次进入） */
    fun refreshLocalFiles() {
        val state = _uiState.value
        val router = effectiveRouter(state)
        val dir = offlineRepository.ipkDir(router)
        val names = mutableSetOf<String>()
        // filename -> name 映射
        val byFilename = (indexEntries + kmodsEntries).associateBy { it.filename }
        dir.listFiles()?.forEach { f ->
            val entry = byFilename[f.name]
            if (entry != null && entry.size > 0 && f.length() == entry.size) {
                names.add(entry.name)
            }
        }
        _uiState.value = state.copy(downloadedNames = names)
    }

    // =====================================================================
    //  列表 / 选择
    // =====================================================================

    fun setSearchQuery(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }

    fun setFeedFilter(feed: String?) {
        _uiState.value = _uiState.value.copy(feedFilter = feed)
    }

    fun toggleHideInstalled() {
        _uiState.value = _uiState.value.copy(hideInstalled = !_uiState.value.hideInstalled)
    }

    fun toggleSelect(name: String) {
        val state = _uiState.value
        val newSelected = if (state.selected.contains(name)) {
            state.selected - name
        } else {
            state.selected + name
        }
        _uiState.value = state.copy(selected = newSelected)
        recomputeResolution()
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selected = emptySet(), resolution = null)
    }

    /** 过滤后的可见列表 */
    fun getVisiblePackages(): List<OfflinePackageEntry> {
        val state = _uiState.value
        var list = indexEntries
        state.feedFilter?.let { feed ->
            list = list.filter { it.feed == feed }
        }
        if (state.hideInstalled) {
            list = list.filter { !state.installedNames.contains(it.name) }
        }
        val q = state.searchQuery.trim()
        if (q.isNotEmpty()) {
            list = list.filter {
                it.name.contains(q, true) || it.description.contains(q, true)
            }
        }
        // luci-app 优先，其次按名称排序
        return list.sortedWith(
            compareByDescending<OfflinePackageEntry> { it.name.startsWith("luci-app-") }
                .thenBy { it.name }
        )
    }

    /** 可用的 feed 过滤项（只显示有内容的 feed） */
    fun getAvailableFeeds(): List<String> {
        return indexEntries.map { it.feed }.distinct().sorted()
    }

    // =====================================================================
    //  依赖解析
    // =====================================================================

    fun recomputeResolution() {
        val state = _uiState.value
        if (state.selected.isEmpty()) {
            _uiState.value = state.copy(resolution = null)
            return
        }
        val resolution = OfflinePackageRepository.resolvePackages(
            selectedNames = state.selected,
            mainUniverse = indexEntries,
            kmodsUniverse = kmodsEntries,
            installed = state.installedNames
        )
        _uiState.value = state.copy(resolution = resolution)

        // 选中包涉及内核模块且 kmods 索引未加载时，按需拉取
        if (resolution.missingKmods.isNotEmpty() &&
            !state.kmodsLoaded && !state.kmodsUnavailable
        ) {
            viewModelScope.launch {
                runCatching {
                    val kmods = offlineRepository.fetchKmodsIndex(state.mirror, effectiveRouter())
                    if (kmods != null) {
                        kmodsEntries = kmods
                        _uiState.value = _uiState.value.copy(kmodsLoaded = true)
                    } else {
                        _uiState.value = _uiState.value.copy(kmodsUnavailable = true)
                    }
                    recomputeResolution()
                }
            }
        }
    }

    // =====================================================================
    //  下载 / 安装
    // =====================================================================

    fun downloadSelected(thenInstall: Boolean) {
        val state = _uiState.value
        val resolution = state.resolution ?: return
        if (resolution.toDownload.isEmpty()) return

        opJob = viewModelScope.launch {
            try {
                _uiState.value = state.copy(
                    phase = OfflinePhase.DOWNLOADING,
                    opError = null,
                    opResult = null,
                    opLog = emptyList(),
                    downloadedBytes = 0,
                    totalBytes = resolution.totalBytes
                )

                val router = effectiveRouter()
                val files = mutableListOf<File>()
                var bytesSoFar = 0L

                for (entry in resolution.toDownload) {
                    // 已下载过的直接复用
                    if (state.downloadedNames.contains(entry.name)) {
                        val f = File(offlineRepository.ipkDir(router), entry.filename)
                        if (f.exists()) {
                            files.add(f)
                            bytesSoFar += f.length()
                            _uiState.value = _uiState.value.copy(
                                downloadedBytes = bytesSoFar,
                                currentFile = entry.filename
                            )
                            continue
                        }
                    }
                    _uiState.value = _uiState.value.copy(currentFile = entry.filename)
                    val file = offlineRepository.downloadPackage(
                        mirror = _uiState.value.mirror,
                        router = router,
                        entry = entry,
                        onProgress = { delta ->
                            bytesSoFar += delta
                            _uiState.value = _uiState.value.copy(downloadedBytes = bytesSoFar)
                        }
                    )
                    files.add(file)
                }

                _uiState.value = _uiState.value.copy(
                    phase = OfflinePhase.IDLE,
                    downloadedNames = _uiState.value.downloadedNames +
                            resolution.toDownload.map { it.name }.toSet(),
                    currentFile = null
                )
                refreshLocalFiles()

                if (thenInstall) {
                    performInstall(resolution, files)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = OfflinePhase.FAILED,
                    currentFile = null,
                    opError = Loc.s(R.string.offline_download_failed, e.message ?: "")
                )
            }
        }
    }

    /** 仅安装（文件应已全部下载） */
    fun installDownloaded() {
        val state = _uiState.value
        val resolution = state.resolution ?: return

        val router = effectiveRouter(state)
        val dir = offlineRepository.ipkDir(router)

        val files = mutableListOf<File>()
        for (entry in resolution.toDownload) {
            val f = File(dir, entry.filename)
            if (f.exists() && (entry.size <= 0 || f.length() == entry.size)) {
                files.add(f)
            }
        }
        if (files.size != resolution.toDownload.size) {
            _uiState.value = state.copy(
                opError = Loc.s(R.string.offline_files_missing)
            )
            return
        }

        opJob = viewModelScope.launch { performInstall(resolution, files) }
    }

    private suspend fun performInstall(resolution: OfflineResolution, files: List<File>) {
        val state = _uiState.value

        // 阻断：硬缺失依赖或缺失内核模块
        if (resolution.hasHardMissing) {
            _uiState.value = state.copy(
                phase = OfflinePhase.FAILED,
                opError = Loc.s(R.string.offline_missing_deps, resolution.missing.joinToString(", "))
            )
            return
        }
        if (resolution.hasMissingKmods) {
            _uiState.value = state.copy(
                phase = OfflinePhase.FAILED,
                opError = Loc.s(R.string.offline_missing_kmods, resolution.missingKmods.joinToString(", "))
            )
            return
        }

        _uiState.value = state.copy(
            phase = OfflinePhase.UPLOADING,
            uploadedBytes = 0,
            uploadTotal = files.sumOf { it.length() }
        )

        val result = offlineRepository.uploadAndInstall(
            entries = resolution.toDownload,
            files = files,
            onUploadProgress = { uploaded, total ->
                _uiState.value = _uiState.value.copy(
                    uploadedBytes = uploaded,
                    uploadTotal = total
                )
            },
            onLog = { line ->
                _uiState.value = _uiState.value.copy(
                    opLog = _uiState.value.opLog + line
                )
            }
        )

        _uiState.value = if (result.success) {
            _uiState.value.copy(
                phase = OfflinePhase.DONE,
                opResult = result,
                installedNames = _uiState.value.installedNames +
                        resolution.toDownload.map { it.name }.toSet(),
                selected = emptySet(),
                resolution = null
            )
        } else {
            _uiState.value.copy(
                phase = OfflinePhase.FAILED,
                opResult = result,
                opError = describeInstallError(result)
            )
        }
    }

    private fun describeInstallError(result: OfflineInstallResult): String {
        val err = result.error ?: ""
        return when {
            err.startsWith("SPACE:") -> {
                val parts = err.removePrefix("SPACE:").split(",")
                Loc.s(
                    R.string.offline_space_tmp_insufficient,
                    parts.getOrNull(1)?.substringAfter('=') ?: "?",
                    parts.getOrNull(0)?.substringAfter('=') ?: "?"
                )
            }
            err.startsWith("UPLOAD:") ->
                Loc.s(R.string.offline_upload_failed, err.removePrefix("UPLOAD:"))
            err.startsWith("OPKG:") ->
                Loc.s(R.string.offline_install_failed, err.removePrefix("OPKG:"))
            else -> Loc.s(R.string.offline_install_failed, err)
        }
    }

    fun cancelOperation() {
        opJob?.cancel()
        opJob = null
        _uiState.value = _uiState.value.copy(
            phase = OfflinePhase.FAILED,
            currentFile = null,
            opError = Loc.s(R.string.offline_canceled)
        )
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(
            phase = OfflinePhase.IDLE,
            opError = null,
            opResult = null,
            opLog = emptyList()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(opError = null)
    }

    // =====================================================================
    //  高级设置（镜像 / 版本覆盖）
    // =====================================================================

    fun saveAdvancedSettings(mirror: String, versionOverride: String) {
        val m = mirror.trim().ifBlank { OfflinePackageRepository.DEFAULT_MIRROR }
        prefs.edit().putString("mirror", m).apply()
        _uiState.value = _uiState.value.copy(
            mirror = m,
            versionOverride = versionOverride.trim()
        )
        indexEntries = emptyList()
        kmodsEntries = emptyList()
        _uiState.value = _uiState.value.copy(
            indexCount = 0,
            kmodsLoaded = false,
            kmodsUnavailable = false,
            selected = emptySet(),
            resolution = null
        )
        loadIndex()
    }

    // =====================================================================
    //  调试模式
    // =====================================================================

        }
