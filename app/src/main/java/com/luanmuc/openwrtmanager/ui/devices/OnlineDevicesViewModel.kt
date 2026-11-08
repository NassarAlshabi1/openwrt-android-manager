package com.luanmuc.openwrtmanager.ui.devices

import android.app.Application

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.DeviceInfo
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 在线设备 ViewModel
 * 实现缓存优先策略：先显示缓存数据，同时后台发起网络请求
 */
class OnlineDevicesViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())
    private val cacheRepository = CacheRepository.getInstance(application)

    private val _uiState = MutableStateFlow(OnlineDevicesUiState())
    val uiState: StateFlow<OnlineDevicesUiState> = _uiState.asStateFlow()

    data class OnlineDevicesUiState(
        val devices: List<DeviceInfo> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val searchQuery: String = "",
        val hasRouter: Boolean = false,
        val sortBy: SortBy = SortBy.IP,
        val isFromCache: Boolean = false,
        val cacheTimestamp: Long? = null,
        val isOfflineMode: Boolean = false
    )

    enum class SortBy {
        IP, NAME, CONNECTED_TIME, TRAFFIC
    }

    init {
        initNetworkMonitor()
        observeRouters()
    }

    override fun refreshData() {
        loadDevices()
    }

    private fun observeRouters() {
        viewModelScope.launch {
            routerRepository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())

                if (routers.isNotEmpty() && _uiState.value.devices.isEmpty()) {
                    // 先加载缓存
                    loadFromCache()
                    // 然后从网络加载
                    loadDevices()
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

                val activeRouter = getActiveRouter() ?: return@launch

                val cachedDevices = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_ONLINE_DEVICES,
                    activeRouter.id,
                    Array<DeviceInfo>::class.java
                )?.toList()

                if (!cachedDevices.isNullOrEmpty()) {
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_ONLINE_DEVICES,
                        activeRouter.id
                    )

                    _uiState.value = _uiState.value.copy(
                        devices = sortDevices(cachedDevices, _uiState.value.sortBy),
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

    fun loadDevices() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.devices.isEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = isFirstLoad,
                error = null
            )

            try {
                // 调试模式：使用假数据

                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    val dhcpLeases = luciRepository.getDhcpLeases()
                    val arpTable = luciRepository.getArpTable()

                    // 合并DHCP租约和ARP表
                    val deviceMap = mutableMapOf<String, DeviceInfo>()
                    dhcpLeases.forEach { device ->
                        if (device.mac.isNotEmpty()) {
                            deviceMap[device.mac] = device
                        }
                    }
                    arpTable.forEach { device ->
                        if (device.mac.isNotEmpty()) {
                            val existing = deviceMap[device.mac]
                            if (existing != null) {
                                deviceMap[device.mac] = existing.copy(
                                    interfaceName = device.interfaceName
                                )
                            } else {
                                deviceMap[device.mac] = device
                            }
                        }
                    }

                    val devices = deviceMap.values.toList()
                    val now = System.currentTimeMillis()

                    // 保存到缓存
                    cacheRepository.saveCache(
                        CacheRepository.KEY_ONLINE_DEVICES,
                        activeRouter.id,
                        "List<DeviceInfo>",
                        devices.toTypedArray()
                    )

                    _uiState.value = _uiState.value.copy(
                        devices = sortDevices(devices, _uiState.value.sortBy),
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
                    CacheRepository.KEY_ONLINE_DEVICES,
                    it.id
                )
            } ?: false

            if (hasCache && _uiState.value.devices.isNotEmpty()) {
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

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSortBy(sortBy: SortBy) {
        _uiState.value = _uiState.value.copy(
            sortBy = sortBy,
            devices = sortDevices(_uiState.value.devices, sortBy)
        )
    }

    private fun sortDevices(devices: List<DeviceInfo>, sortBy: SortBy): List<DeviceInfo> {
        return when (sortBy) {
            SortBy.IP -> devices.sortedBy { it.ip }
            SortBy.NAME -> devices.sortedBy { it.hostname.ifEmpty { it.mac } }
            SortBy.CONNECTED_TIME -> devices.sortedByDescending { it.connectedTime }
            SortBy.TRAFFIC -> devices.sortedByDescending { it.rxBytes + it.txBytes }
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
}
