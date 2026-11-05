package com.luanmuc.openwrtmanager.ui.system

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.luanmuc.openwrtmanager.data.model.LogEntry
import com.luanmuc.openwrtmanager.data.model.ProcessInfo
import com.luanmuc.openwrtmanager.data.model.FullSystemInfo
import com.luanmuc.openwrtmanager.data.model.PackageManagerType
import com.luanmuc.openwrtmanager.data.model.RepoPreset
import com.luanmuc.openwrtmanager.util.DebugMode
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 系统管理 ViewModel
 * 实现缓存优先策略：先显示缓存数据，同时后台发起网络请求
 */
class SystemViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())
    private val cacheRepository = CacheRepository.getInstance(application)

    private val _uiState = MutableStateFlow(SystemUiState())
    val uiState: StateFlow<SystemUiState> = _uiState.asStateFlow()

    data class SystemUiState(
        val logs: List<LogEntry> = emptyList(),
        val processes: List<ProcessInfo> = emptyList(),
        val isLoadingLogs: Boolean = false,
        val isLoadingProcesses: Boolean = false,
        val error: String? = null,
        val hasRouter: Boolean = false,
        val selectedTab: Int = 0,
        val logsFromCache: Boolean = false,
        val processesFromCache: Boolean = false,
        val logsCacheTimestamp: Long? = null,
        val processesCacheTimestamp: Long? = null,
        val isOfflineMode: Boolean = false,
        val cpuUsage: Float = 0f,
        val memoryUsage: Float = 0f,
        val cpuHistory: List<Float> = emptyList(),
        val memoryHistory: List<Float> = emptyList(),
        val systemInfo: FullSystemInfo = FullSystemInfo(),
        val isLoadingSystemInfo: Boolean = false,
        val installedPackagesCount: Int = 0,
        val availablePackagesCount: Int = 0,
        val reposCount: Int = 0,
        val presetRepos: List<RepoPreset> = emptyList(),
        val isSwitchingRepo: Boolean = false
    )

    init {
        initNetworkMonitor()
        observeRouters()
        startRealtimeMonitoring()
        // 调试模式下设置初始CPU内存值
        if (DebugMode.isDebugMode) {
            _uiState.value = _uiState.value.copy(
                cpuUsage = 35f,
                memoryUsage = 52f,
                cpuHistory = listOf(30f, 35f, 32f, 38f, 35f),
                memoryHistory = listOf(48f, 50f, 52f, 51f, 52f)
            )
        }
    }
    
    /**
     * 启动实时监控（CPU、内存）
     */
    private fun startRealtimeMonitoring() {
        viewModelScope.launch {
            while (true) {
                if (DebugMode.isDebugMode) {
                    // 调试模式：生成假数据
                    val newCpu = (30 + Math.random() * 40).toFloat()
                    val newMemory = (40 + Math.random() * 30).toFloat()
                    
                    val newCpuHistory = (_uiState.value.cpuHistory + newCpu).takeLast(30)
                    val newMemoryHistory = (_uiState.value.memoryHistory + newMemory).takeLast(30)
                    
                    _uiState.value = _uiState.value.copy(
                        cpuUsage = newCpu,
                        memoryUsage = newMemory,
                        cpuHistory = newCpuHistory,
                        memoryHistory = newMemoryHistory
                    )
                }
                delay(2000) // 每2秒更新一次
            }
        }
    }

    override fun refreshData() {
        loadLogs()
        loadProcesses()
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
                
                // 调试模式变化时，清空缓存数据强制重新加载
                if (isDebugMode && (_uiState.value.logsFromCache || _uiState.value.processesFromCache)) {
                    _uiState.value = _uiState.value.copy(
                        logs = emptyList(),
                        processes = emptyList(),
                        systemInfo = FullSystemInfo(),
                        logsFromCache = false,
                        processesFromCache = false
                    )
                }
                
                if (routers.isNotEmpty() || isDebugMode) {
                    // 先加载缓存（演示模式跳过）
                    loadLogsFromCache()
                    loadProcessesFromCache()
                    // 然后从网络加载（演示模式使用假数据）
                    loadLogs()
                    loadProcesses()
                    // 加载系统信息
                    if (_uiState.value.systemInfo.hostname.isEmpty()) {
                        loadSystemInfo()
                    }
                }
            }
        }
    }

    fun setSelectedTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == 0 && _uiState.value.systemInfo.hostname.isEmpty()) {
            // 系统信息Tab：加载系统信息
            loadSystemInfo()
        } else if (tab == 2 && _uiState.value.processes.isEmpty()) {
            // 进程管理Tab：加载进程列表
            loadProcessesFromCache()
            loadProcesses()
        }
    }

    /**
     * 从缓存加载日志
     */
    private fun loadLogsFromCache() {
        viewModelScope.launch {
            try {
                // 调试模式不使用缓存
                if (DebugMode.isDebugMode) {
                    return@launch
                }

                val activeRouter = getActiveRouter() ?: return@launch

                val cachedLogs = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_SYSTEM_LOG,
                    activeRouter.id,
                    Array<LogEntry>::class.java
                )?.toList()

                if (!cachedLogs.isNullOrEmpty()) {
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_SYSTEM_LOG,
                        activeRouter.id
                    )

                    _uiState.value = _uiState.value.copy(
                        logs = cachedLogs,
                        logsFromCache = true,
                        logsCacheTimestamp = cacheTime,
                        isLoadingLogs = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 从缓存加载进程
     */
    private fun loadProcessesFromCache() {
        viewModelScope.launch {
            try {
                // 调试模式不使用缓存
                if (DebugMode.isDebugMode) {
                    return@launch
                }

                val activeRouter = getActiveRouter() ?: return@launch

                val cachedProcesses = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_PROCESS_LIST,
                    activeRouter.id,
                    Array<ProcessInfo>::class.java
                )?.toList()

                if (!cachedProcesses.isNullOrEmpty()) {
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_PROCESS_LIST,
                        activeRouter.id
                    )

                    _uiState.value = _uiState.value.copy(
                        processes = cachedProcesses.sortedByDescending { it.cpu },
                        processesFromCache = true,
                        processesCacheTimestamp = cacheTime,
                        isLoadingProcesses = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadLogs() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.logs.isEmpty()
            _uiState.value = _uiState.value.copy(
                isLoadingLogs = isFirstLoad,
                error = null
            )

            try {
                // 调试模式：使用假数据
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(600)
                    val logs = DebugMode.getFakeSystemLog()
                    _uiState.value = _uiState.value.copy(
                        logs = logs,
                        isLoadingLogs = false,
                        logsFromCache = false,
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

                    val logs = luciRepository.getSystemLog()
                    val now = System.currentTimeMillis()

                    // 保存到缓存
                    cacheRepository.saveCache(
                        CacheRepository.KEY_SYSTEM_LOG,
                        activeRouter.id,
                        "List<LogEntry>",
                        logs.toTypedArray()
                    )

                    _uiState.value = _uiState.value.copy(
                        logs = logs,
                        isLoadingLogs = false,
                        logsFromCache = false,
                        isOfflineMode = false,
                        logsCacheTimestamp = now,
                        error = null
                    )
                }
            } catch (e: Exception) {
                handleLogsLoadError(e.message ?: Loc.s(R.string.s0329))
            }
        }
    }

    /**
     * 处理日志加载错误
     */
    private fun handleLogsLoadError(errorMsg: String) {
        viewModelScope.launch {
            // 检查是否有缓存
            val activeRouter = getActiveRouter()
            val hasCache = activeRouter?.let {
                cacheRepository.hasCache(
                    CacheRepository.KEY_SYSTEM_LOG,
                    it.id
                )
            } ?: false

            if (hasCache && _uiState.value.logs.isNotEmpty()) {
                // 有缓存，显示离线模式
                _uiState.value = _uiState.value.copy(
                    isLoadingLogs = false,
                    isOfflineMode = true,
                    error = null
                )
            } else {
                // 没有缓存，显示错误
                _uiState.value = _uiState.value.copy(
                    isLoadingLogs = false,
                    error = errorMsg
                )
            }
        }
    }

    fun loadProcesses() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.processes.isEmpty()
            _uiState.value = _uiState.value.copy(
                isLoadingProcesses = isFirstLoad,
                error = null
            )

            try {
                // 调试模式：使用假数据
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(800)
                    val processes = DebugMode.getFakeProcessList()
                    _uiState.value = _uiState.value.copy(
                        processes = processes.sortedByDescending { it.cpu },
                        isLoadingProcesses = false,
                        processesFromCache = false,
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

                    val processes = luciRepository.getProcessList()
                    val now = System.currentTimeMillis()

                    // 保存到缓存
                    cacheRepository.saveCache(
                        CacheRepository.KEY_PROCESS_LIST,
                        activeRouter.id,
                        "List<ProcessInfo>",
                        processes.toTypedArray()
                    )

                    _uiState.value = _uiState.value.copy(
                        processes = processes.sortedByDescending { it.cpu },
                        isLoadingProcesses = false,
                        processesFromCache = false,
                        isOfflineMode = false,
                        processesCacheTimestamp = now,
                        error = null
                    )
                }
            } catch (e: Exception) {
                handleProcessesLoadError(e.message ?: Loc.s(R.string.s0329))
            }
        }
    }

    /**
     * 处理进程加载错误
     */
    private fun handleProcessesLoadError(errorMsg: String) {
        viewModelScope.launch {
            // 检查是否有缓存
            val activeRouter = getActiveRouter()
            val hasCache = activeRouter?.let {
                cacheRepository.hasCache(
                    CacheRepository.KEY_PROCESS_LIST,
                    it.id
                )
            } ?: false

            if (hasCache && _uiState.value.processes.isNotEmpty()) {
                // 有缓存，显示离线模式
                _uiState.value = _uiState.value.copy(
                    isLoadingProcesses = false,
                    isOfflineMode = true,
                    error = null
                )
            } else {
                // 没有缓存，显示错误
                _uiState.value = _uiState.value.copy(
                    isLoadingProcesses = false,
                    error = errorMsg
                )
            }
        }
    }

    fun killProcess(pid: Int) {
        viewModelScope.launch {
            try {
                // 调试模式：模拟杀死进程
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(500)
                    val processes = _uiState.value.processes.filter { it.pid != pid }
                    _uiState.value = _uiState.value.copy(processes = processes)
                    return@launch
                }

                luciRepository.killProcess(pid)
                // 杀死进程后清除缓存并重新加载
                val activeRouter = getActiveRouter()
                activeRouter?.let {
                    cacheRepository.deleteCache(
                        CacheRepository.KEY_PROCESS_LIST,
                        it.id
                    )
                }
                loadProcesses()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: Loc.s(R.string.s0311)
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

    // ==================== 系统信息 ====================

    /**
     * 加载系统信息
     */
    fun loadSystemInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSystemInfo = true)
            try {
                if (DebugMode.isDebugMode) {
                    // 调试模式：使用假数据
                    val fakeInfo = DebugMode.getFakeFullSystemInfo()
                    val presetRepos = luciRepository.getPresetRepos(fakeInfo)
                    _uiState.value = _uiState.value.copy(
                        systemInfo = fakeInfo,
                        isLoadingSystemInfo = false,
                        installedPackagesCount = DebugMode.getFakeInstalledPackages().size,
                        availablePackagesCount = DebugMode.getFakeAvailablePackages().size,
                        reposCount = DebugMode.getFakeRepos().size,
                        presetRepos = presetRepos
                    )
                    return@launch
                }

                val activeRouter = getActiveRouter() ?: return@launch

                // 从缓存加载
                val cachedInfo = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_SYSTEM_INFO,
                    activeRouter.id,
                    FullSystemInfo::class.java
                )

                // 从网络加载
                val systemInfo = luciRepository.getFullSystemInfo()
                val presetRepos = luciRepository.getPresetRepos(systemInfo)

                // 获取软件源数量
                val repos = try {
                    luciRepository.getPackageRepos()
                } catch (e: Exception) {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    systemInfo = systemInfo,
                    isLoadingSystemInfo = false,
                    reposCount = repos.size,
                    presetRepos = presetRepos
                )

                // 保存到缓存
                cacheRepository.saveCache(
                    CacheRepository.KEY_SYSTEM_INFO,
                    activeRouter.id,
                    "FullSystemInfo",
                    systemInfo
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSystemInfo = false,
                    error = e.message ?: Loc.s(R.string.s0762)
                )
            }
        }
    }

    /**
     * 切换软件源
     */
    fun switchRepo(preset: RepoPreset) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSwitchingRepo = true)
            try {
                val success = luciRepository.switchMirrorRepo(preset, _uiState.value.systemInfo)
                if (success) {
                    // 切换成功，重新加载软件源列表
                    loadSystemInfo()
                }
                _uiState.value = _uiState.value.copy(
                    isSwitchingRepo = false,
                    error = if (!success) Loc.s(R.string.s0763) else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSwitchingRepo = false,
                    error = e.message ?: Loc.s(R.string.s0763)
                )
            }
        }
    }

    /**
     * 自动配置官方软件源
     */
    fun autoConfigureOfficialRepos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSwitchingRepo = true)
            try {
                val success = luciRepository.autoConfigureOfficialRepos(_uiState.value.systemInfo)
                if (success) {
                    loadSystemInfo()
                }
                _uiState.value = _uiState.value.copy(
                    isSwitchingRepo = false,
                    error = if (!success) Loc.s(R.string.s0764) else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSwitchingRepo = false,
                    error = e.message ?: Loc.s(R.string.s0764)
                )
            }
        }
    }
}
