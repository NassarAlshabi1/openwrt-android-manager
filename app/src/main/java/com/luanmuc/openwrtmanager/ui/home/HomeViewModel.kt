package com.luanmuc.openwrtmanager.ui.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.*
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciException
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.DebugMode
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 首页 ViewModel
 * 实现缓存优先策略：先显示缓存数据，同时后台发起网络请求
 */
class HomeViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())
    private val cacheRepository = CacheRepository.getInstance(application)
    val dashboardConfig = DashboardConfig.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    data class HomeUiState(
        val activeRouter: Router? = null,
        val routerStatus: RouterStatus? = null,
        val wanStatus: NetworkInterface? = null,
        val onlineDevices: List<DeviceInfo> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val hasRouter: Boolean = false,
        val isRefreshing: Boolean = false,
        val cpuHistory: List<CpuDataPoint> = emptyList(),
        val trafficHistory: List<TrafficDataPoint> = emptyList(),
        val autoRefresh: Boolean = true,
        val downloadSpeed: Long = 0,
        val uploadSpeed: Long = 0,
        val isFromCache: Boolean = false,
        val cacheTimestamp: Long? = null,
        val isOfflineMode: Boolean = false,
        val portStatus: List<PortStatus> = emptyList(),
        val deviceCapabilities: DeviceCapabilities = DeviceCapabilities()
    )

    init {
        // 初始化网络监听
        initNetworkMonitor()
        observeRouters()
    }
    
    /**
     * 刷新数据（网络恢复时自动调用）
     */
    override fun refreshData() {
        _uiState.value.activeRouter?.let { router ->
            loadAllData(router)
        }
    }
    
    /**
     * 网络恢复时的处理
     */
    override fun onNetworkRestored() {
        super.onNetworkRestored()
        _uiState.value = _uiState.value.copy(isOfflineMode = false)
    }
    
    /**
     * 网络断开时的处理
     */
    override fun onNetworkLost() {
        super.onNetworkLost()
        _uiState.value = _uiState.value.copy(isOfflineMode = true)
    }

    /**
     * 观察路由器数据变化
     */
    private fun observeRouters() {
        viewModelScope.launch {
            try {
                combine(
                    routerRepository.routers,
                    routerRepository.activeRouterId,
                    DebugMode.isDebugModeFlow
                ) { routers, activeId, isDebugMode ->
                    val activeRouter = if (activeId != null) {
                        routers.find { it.id == activeId } ?: routers.firstOrNull()
                    } else {
                        routers.firstOrNull()
                    }
                    Triple(routers, activeRouter, isDebugMode)
                }.collect { (routers, activeRouter, isDebugMode) ->
                    try {
                        _uiState.value = _uiState.value.copy(
                            activeRouter = activeRouter,
                            hasRouter = routers.isNotEmpty()
                        )
                        
                        // 调试模式变化时，清空现有数据强制重新加载
                        if (isDebugMode && _uiState.value.routerStatus != null && !DebugMode.isDemoRouter(activeRouter?.id ?: "")) {
                            _uiState.value = _uiState.value.copy(routerStatus = null)
                        }
                        
                        if (activeRouter != null && _uiState.value.routerStatus == null) {
                            // 先加载缓存
                            loadFromCache(activeRouter)
                            // 然后从网络加载
                            loadAllData(activeRouter)
                            startAutoRefresh()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 从缓存加载数据
     */
    private fun loadFromCache(router: Router) {
        viewModelScope.launch {
            try {
                // 调试模式不使用缓存
                if (DebugMode.isDebugMode) {
                    return@launch
                }

                val cachedStatus = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_ROUTER_STATUS,
                    router.id,
                    RouterStatus::class.java
                )

                val cachedWan = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_WAN_STATUS,
                    router.id,
                    NetworkInterface::class.java
                )

                val cachedDevices = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_ONLINE_DEVICES,
                    router.id,
                    Array<DeviceInfo>::class.java
                )?.toList()

                if (cachedStatus != null || cachedWan != null || cachedDevices != null) {
                    val now = System.currentTimeMillis()
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_ROUTER_STATUS,
                        router.id
                    )

                    _uiState.value = _uiState.value.copy(
                        routerStatus = cachedStatus,
                        wanStatus = cachedWan,
                        onlineDevices = cachedDevices ?: emptyList(),
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

    /**
     * 开始自动刷新
     */
    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (_uiState.value.autoRefresh && _uiState.value.activeRouter != null) {
                delay(5000)
                if (_uiState.value.autoRefresh && !_uiState.value.isLoading) {
                    refreshSilent()
                }
            }
        }
    }

    /**
     * 静默刷新
     */
    private fun refreshSilent() {
        _uiState.value.activeRouter?.let { router ->
            viewModelScope.launch {
                try {
                    // 调试模式：使用假数据
                    if (DebugMode.isDebugMode) {
                        DebugMode.simulateDelay(300)
                        val status = DebugMode.getFakeRouterStatus()
                        val wan = DebugMode.getFakeWanStatus()
                        val devices = DebugMode.getFakeOnlineDevices()
                        val now = System.currentTimeMillis()

                        val newCpuPoint = CpuDataPoint(time = now, usage = status.cpuUsage + (Math.random() * 10 - 5).toFloat())
                        val cpuHistory = (_uiState.value.cpuHistory + newCpuPoint).takeLast(20)

                        val wanRx = wan.rxBytes + (Math.random() * 100000).toLong()
                        val wanTx = wan.txBytes + (Math.random() * 50000).toLong()
                        val newTrafficPoint = TrafficDataPoint(time = now, rx = wanRx, tx = wanTx)
                        val trafficHistory = (_uiState.value.trafficHistory + newTrafficPoint).takeLast(20)

                        // 计算实时速度
                        val lastPoint = _uiState.value.trafficHistory.lastOrNull()
                        val downloadSpeed = if (lastPoint != null && now > lastPoint.time) {
                            val timeDiff = (now - lastPoint.time) / 1000.0
                            if (timeDiff > 0) {
                                ((wanRx - lastPoint.rx) / timeDiff).toLong()
                            } else 0
                        } else 0

                        val uploadSpeed = if (lastPoint != null && now > lastPoint.time) {
                            val timeDiff = (now - lastPoint.time) / 1000.0
                            if (timeDiff > 0) {
                                ((wanTx - lastPoint.tx) / timeDiff).toLong()
                            } else 0
                        } else 0

                        _uiState.value = _uiState.value.copy(
                            routerStatus = status.copy(
                                cpuUsage = newCpuPoint.usage,
                                onlineDevices = devices.size
                            ),
                            wanStatus = wan.copy(rxBytes = wanRx, txBytes = wanTx),
                            onlineDevices = devices,
                            cpuHistory = cpuHistory,
                            trafficHistory = trafficHistory,
                            downloadSpeed = downloadSpeed,
                            uploadSpeed = uploadSpeed,
                            isFromCache = false,
                            isOfflineMode = false,
                            error = null
                        )
                        return@launch
                    }

                    val password = EncryptionUtil.decrypt(router.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(router.address, router.username, password)
                    }

                    val status = luciRepository.getRouterStatus()
                    val wan = luciRepository.getWanStatus()
                    val devices = luciRepository.getDhcpLeases()
                    val now = System.currentTimeMillis()

                    val newCpuPoint = CpuDataPoint(time = now, usage = status.cpuUsage)
                    val cpuHistory = (_uiState.value.cpuHistory + newCpuPoint).takeLast(20)

                    val wanRx = wan?.rxBytes ?: 0
                    val wanTx = wan?.txBytes ?: 0
                    val newTrafficPoint = TrafficDataPoint(time = now, rx = wanRx, tx = wanTx)
                    val trafficHistory = (_uiState.value.trafficHistory + newTrafficPoint).takeLast(20)

                    // 计算实时速度
                    val lastPoint = _uiState.value.trafficHistory.lastOrNull()
                    val downloadSpeed = if (lastPoint != null && now > lastPoint.time) {
                        val timeDiff = (now - lastPoint.time) / 1000.0
                        if (timeDiff > 0) {
                            ((wanRx - lastPoint.rx) / timeDiff).toLong()
                        } else 0
                    } else 0

                    val uploadSpeed = if (lastPoint != null && now > lastPoint.time) {
                        val timeDiff = (now - lastPoint.time) / 1000.0
                        if (timeDiff > 0) {
                            ((wanTx - lastPoint.tx) / timeDiff).toLong()
                        } else 0
                    } else 0

                    // 保存到缓存
                    cacheRepository.saveCache(
                        CacheRepository.KEY_ROUTER_STATUS,
                        router.id,
                        "RouterStatus",
                        status
                    )
                    if (wan != null) {
                        cacheRepository.saveCache(
                            CacheRepository.KEY_WAN_STATUS,
                            router.id,
                            "NetworkInterface",
                            wan
                        )
                    }
                    cacheRepository.saveCache(
                        CacheRepository.KEY_ONLINE_DEVICES,
                        router.id,
                        "List<DeviceInfo>",
                        devices.toTypedArray()
                    )

                    _uiState.value = _uiState.value.copy(
                        routerStatus = status.copy(
                            onlineDevices = devices.size,
                            wanConnected = wan?.isConnected == true || wan?.isUp == true,
                            wanIp = wan?.ipaddr ?: "",
                            wanUptime = wan?.uptime ?: 0
                        ),
                        wanStatus = wan,
                        onlineDevices = devices,
                        cpuHistory = cpuHistory,
                        trafficHistory = trafficHistory,
                        downloadSpeed = downloadSpeed,
                        uploadSpeed = uploadSpeed,
                        isFromCache = false,
                        isOfflineMode = false,
                        cacheTimestamp = now,
                        error = null
                    )
                } catch (e: Exception) {
                    // 静默刷新失败不显示错误，但如果当前是缓存数据，保持离线模式
                    if (_uiState.value.isFromCache) {
                        _uiState.value = _uiState.value.copy(isOfflineMode = true)
                    }
                }
            }
        }
    }

    /**
     * 加载所有数据
     */
    private fun loadAllData(router: Router) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.routerStatus == null,
                isRefreshing = _uiState.value.routerStatus != null,
                error = null
            )

            try {
                // 调试模式：使用假数据
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(800)
                    val status = DebugMode.getFakeRouterStatus()
                    val wan = DebugMode.getFakeWanStatus()
                    val devices = DebugMode.getFakeOnlineDevices()
                    val now = System.currentTimeMillis()

                    val cpuHistory = listOf(CpuDataPoint(time = now, usage = status.cpuUsage))
                    val trafficHistory = listOf(
                        TrafficDataPoint(
                            time = now,
                            rx = wan.rxBytes,
                            tx = wan.txBytes
                        )
                    )

                    val portStatus = DebugMode.getFakePortStatus()
                    val deviceCapabilities = DebugMode.getFakeDeviceCapabilities()
                    
                    _uiState.value = _uiState.value.copy(
                        routerStatus = status,
                        wanStatus = wan,
                        onlineDevices = devices,
                        portStatus = portStatus,
                        deviceCapabilities = deviceCapabilities,
                        isLoading = false,
                        isRefreshing = false,
                        cpuHistory = cpuHistory,
                        trafficHistory = trafficHistory,
                        isFromCache = false,
                        isOfflineMode = false,
                        error = null
                    )
                    return@launch
                }

                val password = EncryptionUtil.decrypt(router.encryptedPassword)
                luciRepository.login(router.address, router.username, password)

                val status = luciRepository.getRouterStatus()
                val wan = luciRepository.getWanStatus()
                val devices = luciRepository.getDhcpLeases()
                val now = System.currentTimeMillis()

                val cpuHistory = listOf(CpuDataPoint(time = now, usage = status.cpuUsage))
                val trafficHistory = listOf(
                    TrafficDataPoint(
                        time = now,
                        rx = wan?.rxBytes ?: 0,
                        tx = wan?.txBytes ?: 0
                    )
                )

                // 保存到缓存
                cacheRepository.saveCache(
                    CacheRepository.KEY_ROUTER_STATUS,
                    router.id,
                    "RouterStatus",
                    status
                )
                if (wan != null) {
                    cacheRepository.saveCache(
                        CacheRepository.KEY_WAN_STATUS,
                        router.id,
                        "NetworkInterface",
                        wan
                    )
                }
                cacheRepository.saveCache(
                    CacheRepository.KEY_ONLINE_DEVICES,
                    router.id,
                    "List<DeviceInfo>",
                    devices.toTypedArray()
                )

                // 加载网口状态和设备能力
                val portStatus = try {
                    luciRepository.getPortStatus()
                } catch (e: Exception) {
                    emptyList()
                }
                val deviceCapabilities = try {
                    luciRepository.detectDeviceCapabilities()
                } catch (e: Exception) {
                    DeviceCapabilities()
                }

                _uiState.value = _uiState.value.copy(
                    routerStatus = status.copy(
                        onlineDevices = devices.size,
                        wanConnected = wan?.isConnected == true,
                        wanIp = wan?.ipaddr ?: "",
                        wanUptime = wan?.uptime ?: 0
                    ),
                    wanStatus = wan,
                    onlineDevices = devices,
                    isLoading = false,
                    isRefreshing = false,
                    cpuHistory = cpuHistory,
                    trafficHistory = trafficHistory,
                    isFromCache = false,
                    isOfflineMode = false,
                    cacheTimestamp = now,
                    error = null
                )
            } catch (e: LuciException) {
                handleLoadError(router, e.message ?: Loc.s(R.string.s0524))
            } catch (e: Exception) {
                handleLoadError(router, e.message ?: Loc.s(R.string.s0524))
            }
        }
    }

    /**
     * 处理加载错误
     */
    private fun handleLoadError(router: Router, errorMsg: String) {
        viewModelScope.launch {
            // 检查是否有缓存
            val hasCache = cacheRepository.hasCache(
                CacheRepository.KEY_ROUTER_STATUS,
                router.id
            )

            if (hasCache) {
                // 有缓存，显示离线模式
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOfflineMode = true,
                    error = null
                )
            } else {
                // 没有缓存，显示错误
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = errorMsg
                )
            }
        }
    }

    /**
     * 手动刷新
     */
    fun refresh() {
        _uiState.value.activeRouter?.let {
            loadAllData(it)
        }
    }

    /**
     * 重启路由器
     */
    fun reboot() {
        viewModelScope.launch {
            try {
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(1000)
                    return@launch
                }
                luciRepository.reboot()
                // 重启后清除缓存
                _uiState.value.activeRouter?.let { router ->
                    cacheRepository.clearRouterCache(router.id)
                }
            } catch (e: Exception) {
                // 重启会断开连接，忽略错误
            }
        }
    }

    /**
     * 关机
     */
    fun shutdown() {
        viewModelScope.launch {
            try {
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(1000)
                    return@launch
                }
                luciRepository.shutdown()
                // 关机后清除缓存
                _uiState.value.activeRouter?.let { router ->
                    cacheRepository.clearRouterCache(router.id)
                }
            } catch (e: Exception) {
                // 关机会断开连接，忽略错误
            }
        }
    }

    /**
     * 切换自动刷新
     */
    fun toggleAutoRefresh() {
        val newAutoRefresh = !_uiState.value.autoRefresh
        _uiState.value = _uiState.value.copy(autoRefresh = newAutoRefresh)
        if (newAutoRefresh) {
            startAutoRefresh()
        } else {
            refreshJob?.cancel()
        }
    }

    /**
     * 格式化运行时间
     */
    fun formatUptime(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            days > 0 -> Loc.s(R.string.s0521, days, hours)
            hours > 0 -> Loc.s(R.string.s0522, hours, minutes)
            else -> Loc.s(R.string.s0523, minutes)
        }
    }

    /**
     * 格式化字节数
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1073741824 -> String.format("%.2f GB", bytes / 1073741824.0)
            bytes >= 1048576 -> String.format("%.2f MB", bytes / 1048576.0)
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}

data class CpuDataPoint(
    val time: Long,
    val usage: Float
)

data class TrafficDataPoint(
    val time: Long,
    val rx: Long,
    val tx: Long
)
