package com.luanmuc.openwrtmanager.ui.plugins

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.PackageInfo
import com.luanmuc.openwrtmanager.data.model.RepoInfo
import com.luanmuc.openwrtmanager.data.model.RecommendedPlugin
import com.luanmuc.openwrtmanager.data.model.FullSystemInfo
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 插件页 ViewModel
 * 实现缓存优先策略：先显示缓存数据，同时后台发起网络请求
 */
class PluginsViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())
    private val cacheRepository = CacheRepository.getInstance(application)

    private val _uiState = MutableStateFlow(PluginsUiState())
    val uiState: StateFlow<PluginsUiState> = _uiState.asStateFlow()

    // 插件分类
    enum class PluginCategory(val filter: String) {
        ALL(""),
        SYSTEM("system"),
        NETWORK("network"),
        SERVICE("service"),
        UTILITIES("utilities"),
        MULTIMEDIA("multimedia"),
        ADMINISTRATION("admin"),
        OTHER("other");
        val displayName: String
            get() = when (this) {
                ALL -> Loc.s(R.string.enum_020)
                SYSTEM -> Loc.s(R.string.enum_021)
                NETWORK -> Loc.s(R.string.enum_022)
                SERVICE -> Loc.s(R.string.enum_023)
                UTILITIES -> Loc.s(R.string.enum_024)
                MULTIMEDIA -> Loc.s(R.string.enum_025)
                ADMINISTRATION -> Loc.s(R.string.enum_026)
                OTHER -> Loc.s(R.string.enum_027)
            }
    }
    
    // 排序方式
    enum class SortType {
        NAME,
        SIZE,
        INSTALLED;
        val displayName: String
            get() = when (this) {
                NAME -> Loc.s(R.string.enum_028)
                SIZE -> Loc.s(R.string.enum_029)
                INSTALLED -> Loc.s(R.string.enum_030)
            }
    }
    
    data class PluginsUiState(
        val installedPackages: List<PackageInfo> = emptyList(),
        val availablePackages: List<PackageInfo> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val searchQuery: String = "",
        val hasRouter: Boolean = false,
        val actionLoading: String? = null,
        val isFromCache: Boolean = false,
        val cacheTimestamp: Long? = null,
        val isOfflineMode: Boolean = false,
        val selectedCategory: PluginCategory = PluginCategory.ALL,
        val sortType: SortType = SortType.NAME,
        val selectedPackage: PackageInfo? = null,
        val showDetail: Boolean = false,
        val installProgress: Map<String, Int> = emptyMap(),
        val isUpdatingRepo: Boolean = false,
        val repos: List<RepoInfo> = emptyList(),
        val recommendedPlugins: List<RecommendedPlugin> = emptyList(),
        val isUploadingIpk: Boolean = false,
        val uploadProgress: Int = 0,
        val systemInfo: FullSystemInfo = FullSystemInfo(),
        val isLoadingSystemInfo: Boolean = false,
        val architectureWarning: String? = null
    )

    init {
        initNetworkMonitor()
        observeRouters()
    }

    override fun refreshData() {
        loadPackages()
    }

    private fun observeRouters() {
        viewModelScope.launch {
            routerRepository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())

                if (routers.isNotEmpty() && _uiState.value.installedPackages.isEmpty()) {
                    // 先加载缓存，然后从网络刷新，并加载系统信息
                    loadFromCache()
                    loadPackages()
                    loadSystemInfo()
                }
            }
        }
    }

    /**
     * 从缓存加载数据
     */
    private fun loadFromCache() {
        viewModelScope.launch {
            try {
                val activeRouter = getActiveRouter() ?: return@launch

                val cachedInstalled = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_INSTALLED_PACKAGES,
                    activeRouter.id,
                    Array<PackageInfo>::class.java
                )?.toList()

                val cachedAvailable = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_AVAILABLE_PACKAGES,
                    activeRouter.id,
                    Array<PackageInfo>::class.java
                )?.toList()

                if (!cachedInstalled.isNullOrEmpty() || !cachedAvailable.isNullOrEmpty()) {
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_INSTALLED_PACKAGES,
                        activeRouter.id
                    )

                    _uiState.value = _uiState.value.copy(
                        installedPackages = cachedInstalled ?: emptyList(),
                        availablePackages = cachedAvailable ?: emptyList(),
                        isFromCache = true,
                        cacheTimestamp = cacheTime,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadPackages() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.installedPackages.isEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = isFirstLoad,
                error = null
            )

            try {
                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    val installed = luciRepository.getInstalledPackages()
                    val available = luciRepository.getAvailablePackages()
                    val now = System.currentTimeMillis()

                    // 保存到缓存
                    cacheRepository.saveCache(
                        CacheRepository.KEY_INSTALLED_PACKAGES,
                        activeRouter.id,
                        "List<PackageInfo>",
                        installed.toTypedArray()
                    )
                    cacheRepository.saveCache(
                        CacheRepository.KEY_AVAILABLE_PACKAGES,
                        activeRouter.id,
                        "List<PackageInfo>",
                        available.toTypedArray()
                    )

                    _uiState.value = _uiState.value.copy(
                        installedPackages = installed,
                        availablePackages = available,
                        isLoading = false,
                        isFromCache = false,
                        isOfflineMode = false,
                        cacheTimestamp = now,
                        error = null
                    )
                }
            } catch (e: Exception) {
                handleLoadError(e.message ?: Loc.s(R.string.s0329))
            }
        }
    }

    /**
     * 处理加载错误
     */
    private fun handleLoadError(errorMsg: String) {
        viewModelScope.launch {
            // 检查是否有缓存
            val activeRouter = getActiveRouter()
            val hasCache = activeRouter?.let {
                cacheRepository.hasCache(
                    CacheRepository.KEY_INSTALLED_PACKAGES,
                    it.id
                )
            } ?: false

            if (hasCache && _uiState.value.installedPackages.isNotEmpty()) {
                // 有缓存，显示离线模式
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOfflineMode = true,
                    error = null
                )
            } else {
                // 没有缓存，显示错误
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
        }
    }

    fun installPackage(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = name)
            try {
                // 查找包信息
                val pkg = _uiState.value.availablePackages.find { it.name == name }
                    ?: _uiState.value.installedPackages.find { it.name == name }

                // 架构验证
                if (pkg != null) {
                    val isValid = luciRepository.validatePackageArchitecture(pkg, _uiState.value.systemInfo)

                    if (!isValid) {
                        _uiState.value = _uiState.value.copy(
                            actionLoading = null,
                            architectureWarning = Loc.s(R.string.s0609, pkg.name, _uiState.value.systemInfo.architecture, pkg.architecture)
                        )
                        return@launch
                    }
                }

                val success = luciRepository.installPackage(name)
                if (success) {
                    // 安装成功后刷新并清除缓存
                    val activeRouter = getActiveRouter()
                    activeRouter?.let {
                        cacheRepository.deleteCache(
                            CacheRepository.KEY_INSTALLED_PACKAGES,
                            it.id
                        )
                        cacheRepository.deleteCache(
                            CacheRepository.KEY_AVAILABLE_PACKAGES,
                            it.id
                        )
                    }
                    loadPackages()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: Loc.s(R.string.s0579)
                )
            } finally {
                _uiState.value = _uiState.value.copy(actionLoading = null)
            }
        }
    }

    fun removePackage(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = name)
            try {
                val success = luciRepository.removePackage(name)
                if (success) {
                    // 卸载成功后刷新并清除缓存
                    val activeRouter = getActiveRouter()
                    activeRouter?.let {
                        cacheRepository.deleteCache(
                            CacheRepository.KEY_INSTALLED_PACKAGES,
                            it.id
                        )
                        cacheRepository.deleteCache(
                            CacheRepository.KEY_AVAILABLE_PACKAGES,
                            it.id
                        )
                    }
                    loadPackages()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: Loc.s(R.string.s0582)
                )
            } finally {
                _uiState.value = _uiState.value.copy(actionLoading = null)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setCategory(category: PluginCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setSortType(sortType: SortType) {
        _uiState.value = _uiState.value.copy(sortType = sortType)
    }

    fun showPackageDetail(pkg: PackageInfo) {
        _uiState.value = _uiState.value.copy(
            selectedPackage = pkg,
            showDetail = true
        )
    }

    fun hidePackageDetail() {
        _uiState.value = _uiState.value.copy(
            selectedPackage = null,
            showDetail = false
        )
    }

    /**
     * 获取过滤和排序后的已安装插件列表
     */
    fun getFilteredInstalled(): List<PackageInfo> {
        val state = _uiState.value
        var list = state.installedPackages

        // 搜索过滤
        if (state.searchQuery.isNotEmpty()) {
            list = list.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.description.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // 分类过滤
        if (state.selectedCategory != PluginCategory.ALL) {
            list = list.filter {
                it.category.contains(state.selectedCategory.filter, ignoreCase = true)
            }
        }

        // 排序
        list = when (state.sortType) {
            SortType.NAME -> list.sortedBy { it.name.lowercase() }
            SortType.SIZE -> list.sortedByDescending { it.size }
            SortType.INSTALLED -> list.sortedBy { it.name.lowercase() }
        }

        return list
    }

    /**
     * 获取过滤和排序后的可用插件列表
     */
    fun getFilteredAvailable(): List<PackageInfo> {
        val state = _uiState.value
        var list = state.availablePackages

        // 搜索过滤
        if (state.searchQuery.isNotEmpty()) {
            list = list.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.description.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // 分类过滤
        if (state.selectedCategory != PluginCategory.ALL) {
            list = list.filter {
                it.category.contains(state.selectedCategory.filter, ignoreCase = true)
            }
        }

        // 排序
        list = when (state.sortType) {
            SortType.NAME -> list.sortedBy { it.name.lowercase() }
            SortType.SIZE -> list.sortedByDescending { it.size }
            SortType.INSTALLED -> list.sortedBy { it.name.lowercase() }
        }

        return list
    }

    /**
     * 获取所有可用分类
     */
    fun getAvailableCategories(): List<PluginCategory> {
        return PluginCategory.values().toList()
    }

    /**
     * 更新软件源
     */
    fun updateRepo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingRepo = true, error = null)
            try {
                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    val success = luciRepository.updatePackageLists()
                    if (success) {
                        // 清除缓存，重新加载
                        cacheRepository.deleteCache(
                            CacheRepository.KEY_INSTALLED_PACKAGES,
                            activeRouter.id
                        )
                        cacheRepository.deleteCache(
                            CacheRepository.KEY_AVAILABLE_PACKAGES,
                            activeRouter.id
                        )
                        loadPackages()
                    }
                }
                _uiState.value = _uiState.value.copy(isUpdatingRepo = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingRepo = false,
                    error = e.message ?: Loc.s(R.string.s0259)
                )
            }
        }
    }

    /**
     * 加载软件源列表
     */
    fun loadRepos() {
        viewModelScope.launch {
            try {
                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    val repos = luciRepository.getPackageRepos()
                    _uiState.value = _uiState.value.copy(repos = repos)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 添加软件源
     */
    fun addRepo(name: String, url: String, enabled: Boolean = true) {
        viewModelScope.launch {
            try {
                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    val success = luciRepository.addPackageRepo(name, url, enabled)
                    if (success) {
                        loadRepos()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 删除软件源
     */
    fun removeRepo(name: String) {
        viewModelScope.launch {
            try {
                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    val success = luciRepository.removePackageRepo(name)
                    if (success) {
                        loadRepos()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 切换软件源启用状态
     */
    fun toggleRepo(name: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    val success = luciRepository.setRepoEnabled(name, enabled)
                    if (success) {
                        loadRepos()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取推荐插件列表
     */
    fun getRecommendedPlugins(): List<RecommendedPlugin> {
        return listOf(
            RecommendedPlugin(
                name = "luci-app-ddns",
                displayName = Loc.s(R.string.s0177),
                description = Loc.s(R.string.s0610),
                category = "network",
                icon = "🌐"
            ),
            RecommendedPlugin(
                name = "luci-app-samba4",
                displayName = Loc.s(R.string.s0611),
                description = Loc.s(R.string.s0612),
                category = "network",
                icon = "📁"
            ),
            RecommendedPlugin(
                name = "luci-app-transmission",
                displayName = Loc.s(R.string.s0613),
                description = Loc.s(R.string.s0614),
                category = "network",
                icon = "⬇️"
            ),
            RecommendedPlugin(
                name = "luci-app-adblock",
                displayName = Loc.s(R.string.s0157),
                description = Loc.s(R.string.s0615),
                category = "network",
                icon = "🚫"
            ),
            RecommendedPlugin(
                name = "luci-app-wireguard",
                displayName = "WireGuard",
                description = Loc.s(R.string.s0616),
                category = "network",
                icon = "🔒"
            ),
            RecommendedPlugin(
                name = "luci-app-upnp",
                displayName = "UPnP",
                description = Loc.s(R.string.s0173),
                category = "network",
                icon = "🔌"
            ),
            RecommendedPlugin(
                name = "luci-app-wol",
                displayName = Loc.s(R.string.s0175),
                description = Loc.s(R.string.s0176),
                category = "network",
                icon = "⏰"
            ),
            RecommendedPlugin(
                name = "luci-app-statistics",
                displayName = Loc.s(R.string.s0179),
                description = Loc.s(R.string.s0617),
                category = "admin",
                icon = "📊"
            )
        )
    }

    /**
     * 安装本地IPK文件
     *
     * 真实实现：分块 base64 上传到路由器 /tmp，然后用 opkg 安装。
     */
    fun installIpk(fileName: String, fileData: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploadingIpk = true,
                uploadProgress = 0
            )
            try {
                val activeRouter = getActiveRouter()
                if (activeRouter == null) {
                    _uiState.value = _uiState.value.copy(
                        isUploadingIpk = false,
                        error = Loc.s(R.string.offline_no_router)
                    )
                    return@launch
                }
                if (!luciRepository.isLoggedIn()) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    luciRepository.login(activeRouter.address, activeRouter.username, password)
                }

                val safeName = fileName.substringAfterLast('/').filter { !it.isWhitespace() }
                if (safeName.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isUploadingIpk = false,
                        error = Loc.s(R.string.s0579)
                    )
                    return@launch
                }

                val remotePath = "/tmp/$safeName"

                // 分块 base64 上传（真实进度）
                val chunkSize = 24 * 1024
                var offset = 0
                var chunkIndex = 0
                while (offset < fileData.size) {
                    val end = minOf(offset + chunkSize, fileData.size)
                    val b64 = android.util.Base64.encodeToString(
                        fileData.copyOfRange(offset, end),
                        android.util.Base64.NO_WRAP
                    )
                    val ok = luciRepository.writeRemoteFile(
                        path = remotePath,
                        dataBase64 = b64,
                        append = chunkIndex > 0,
                        mode = 420 // 0644
                    )
                    if (!ok) {
                        runCatching { luciRepository.execCommandFull("rm -f $remotePath") }
                        _uiState.value = _uiState.value.copy(
                            isUploadingIpk = false,
                            uploadProgress = 0,
                            error = Loc.s(R.string.plugins_ipk_upload_failed)
                        )
                        return@launch
                    }
                    offset = end
                    chunkIndex++
                    _uiState.value = _uiState.value.copy(
                        uploadProgress = (offset * 100 / fileData.size).coerceIn(0, 99)
                    )
                }

                // 真实 opkg 安装
                val exec = luciRepository.execCommandFull("opkg install $remotePath 2>&1; rm -f $remotePath")
                val output = (exec.stdout + "\n" + exec.stderr).trim()
                val success = exec.code == 0 &&
                        !output.contains("Collected errors") &&
                        !output.contains("Cannot install package", ignoreCase = true)

                _uiState.value = _uiState.value.copy(
                    isUploadingIpk = false,
                    uploadProgress = 100,
                    error = if (success) null else (output.ifBlank { Loc.s(R.string.s0579) })
                )
                if (success) {
                    val router = getActiveRouter()
                    router?.let {
                        cacheRepository.deleteCache(CacheRepository.KEY_INSTALLED_PACKAGES, it.id)
                        cacheRepository.deleteCache(CacheRepository.KEY_AVAILABLE_PACKAGES, it.id)
                    }
                    loadPackages()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingIpk = false,
                    uploadProgress = 0,
                    error = e.message ?: Loc.s(R.string.s0579)
                )
            }
        }
    }

    private suspend fun getActiveRouter(): Router? {
        val routers = routerRepository.getRoutersList()
        val activeId = routerRepository.getActiveRouterId()
        return if (activeId != null) {
            routers.find { it.id == activeId } ?: routers.firstOrNull()
        } else {
            routers.firstOrNull()
        }
    }

    /**
     * 加载系统信息（用于架构验证）
     */
    fun loadSystemInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSystemInfo = true)
            try {
                val info = luciRepository.getFullSystemInfo()
                _uiState.value = _uiState.value.copy(
                    systemInfo = info,
                    isLoadingSystemInfo = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSystemInfo = false
                )
            }
        }
    }

    /**
     * 清除架构警告
     */
    fun clearArchitectureWarning() {
        _uiState.value = _uiState.value.copy(architectureWarning = null)
    }

    /**
     * 强制安装（忽略架构警告）
     */
    fun forceInstallPackage(name: String) {
        clearArchitectureWarning()
        // 这里可以添加强制安装的逻辑
        // 为了安全，暂时不实现强制安装
    }
}
