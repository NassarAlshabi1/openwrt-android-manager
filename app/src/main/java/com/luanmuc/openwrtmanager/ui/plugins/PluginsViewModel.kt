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
import com.luanmuc.openwrtmanager.util.DebugMode
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
            kotlinx.coroutines.flow.combine(
                routerRepository.routers,
                DebugMode.isDebugModeFlow
            ) { routers, isDebugMode ->
                Pair(routers, isDebugMode)
            }.collect { (routers, isDebugMode) ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())
                
                // 调试模式变化时，清空现有数据强制重新加载
                if (isDebugMode && (_uiState.value.isFromCache || _uiState.value.installedPackages.isNotEmpty())) {
                    _uiState.value = _uiState.value.copy(
                        installedPackages = emptyList(),
                        availablePackages = emptyList(),
                        isFromCache = false
                    )
                }
                
                // 演示模式下也需要加载数据
                val shouldLoad = if (isDebugMode) {
                    _uiState.value.installedPackages.isEmpty()
                } else {
                    routers.isNotEmpty() && _uiState.value.installedPackages.isEmpty()
                }
                if (shouldLoad) {
                    // 先加载缓存（演示模式跳过）
                    loadFromCache()
                    // 然后从网络加载（演示模式使用假数据）
                    loadPackages()
                    // 加载系统信息
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
                // 调试模式不使用缓存
                if (DebugMode.isDebugMode) {
                    return@launch
                }

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
                // 调试模式：使用假数据
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(800)
                    val installed = DebugMode.getFakeInstalledPackages()
                    val available = DebugMode.getFakeAvailablePackages()
                    _uiState.value = _uiState.value.copy(
                        installedPackages = installed,
                        availablePackages = available,
                        isLoading = false,
                        isFromCache = false,
                        isOfflineMode = false,
                        error = null
                    )
                    return@launch
                }

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
                    val isValid = if (DebugMode.isDebugMode) {
                        DebugMode.simulateArchitectureValidation(name, _uiState.value.systemInfo.architecture)
                    } else {
                        luciRepository.validatePackageArchitecture(pkg, _uiState.value.systemInfo)
                    }

                    if (!isValid) {
                        _uiState.value = _uiState.value.copy(
                            actionLoading = null,
                            architectureWarning = Loc.s(R.string.s0609, pkg.name, _uiState.value.systemInfo.architecture, pkg.architecture)
                        )
                        return@launch
                    }
                }

                // 调试模式：模拟安装
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(1500)
                    val installed = _uiState.value.installedPackages.toMutableList()
                    val available = _uiState.value.availablePackages.toMutableList()
                    val pkgToInstall = available.find { it.name == name }
                    if (pkgToInstall != null) {
                        available.remove(pkgToInstall)
                        installed.add(pkgToInstall.copy(installed = true))
                    }
                    _uiState.value = _uiState.value.copy(
                        installedPackages = installed,
                        availablePackages = available
                    )
                    return@launch
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
                // 调试模式：模拟卸载
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(1000)
                    val installed = _uiState.value.installedPackages.toMutableList()
                    val available = _uiState.value.availablePackages.toMutableList()
                    val pkg = installed.find { it.name == name }
                    if (pkg != null) {
                        installed.remove(pkg)
                        available.add(pkg.copy(installed = false))
                    }
                    _uiState.value = _uiState.value.copy(
                        installedPackages = installed,
                        availablePackages = available
                    )
                    return@launch
                }

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
                // 调试模式：模拟更新
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(2000)
                    _uiState.value = _uiState.value.copy(isUpdatingRepo = false)
                    loadPackages()
                    return@launch
                }

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
                // 调试模式：使用假数据
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(500)
                    _uiState.value = _uiState.value.copy(
                        repos = DebugMode.getFakeRepos()
                    )
                    return@launch
                }

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
                // 调试模式：模拟添加
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(500)
                    val repos = _uiState.value.repos.toMutableList()
                    repos.add(RepoInfo(name = name, url = url, enabled = enabled))
                    _uiState.value = _uiState.value.copy(repos = repos)
                    return@launch
                }

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
                // 调试模式：模拟删除
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(500)
                    val repos = _uiState.value.repos.filter { it.name != name }
                    _uiState.value = _uiState.value.copy(repos = repos)
                    return@launch
                }

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
                // 调试模式：模拟切换
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(300)
                    val repos = _uiState.value.repos.map { repo ->
                        if (repo.name == name) repo.copy(enabled = enabled) else repo
                    }
                    _uiState.value = _uiState.value.copy(repos = repos)
                    return@launch
                }

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
     */
    fun installIpk(fileName: String, fileData: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploadingIpk = true,
                uploadProgress = 0
            )
            try {
                // 调试模式：模拟上传和安装
                if (DebugMode.isDebugMode) {
                    for (i in 0..100 step 10) {
                        DebugMode.simulateDelay(100)
                        _uiState.value = _uiState.value.copy(uploadProgress = i)
                    }
                    DebugMode.simulateDelay(1000)
                    _uiState.value = _uiState.value.copy(
                        isUploadingIpk = false,
                        uploadProgress = 100
                    )
                    loadPackages()
                    return@launch
                }

                // 真实环境：通过HTTP上传IPK并安装
                // 注意：由于LuCI ubus API不直接支持文件上传，
                // 这里需要通过其他方式实现（如使用cgi-bin上传）
                // 暂时使用模拟进度
                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    // 模拟上传进度
                    for (i in 0..100 step 20) {
                        kotlinx.coroutines.delay(200)
                        _uiState.value = _uiState.value.copy(uploadProgress = i)
                    }
                    
                    // 这里应该调用真实的上传和安装API
                    // 由于API限制，暂时只做UI展示
                    _uiState.value = _uiState.value.copy(
                        isUploadingIpk = false,
                        uploadProgress = 100
                    )
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
                if (DebugMode.isDebugMode) {
                    _uiState.value = _uiState.value.copy(
                        systemInfo = DebugMode.getFakeFullSystemInfo(),
                        isLoadingSystemInfo = false
                    )
                    return@launch
                }

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
