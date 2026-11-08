package com.luanmuc.openwrtmanager.ui.network

import android.app.Application

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 网络设置 ViewModel
 * 实现缓存优先策略：先显示缓存数据，同时后台发起网络请求
 */
class NetworkViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())
    private val cacheRepository = CacheRepository.getInstance(application)

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    data class NetworkUiState(
        val lanIp: String = "",
        val lanNetmask: String = "",
        val lanDhcpEnabled: Boolean = true,
        val lanDhcpStart: String = "",
        val lanDhcpLimit: String = "",
        val lanDhcpLease: String = "",
        val wanProto: String = "dhcp",
        val wanIp: String = "",
        val wanNetmask: String = "",
        val wanGateway: String = "",
        val wanDns: String = "",
        val wanUsername: String = "",
        val wanPassword: String = "",
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val error: String? = null,
        val success: String? = null,
        val hasRouter: Boolean = false,
        val isFromCache: Boolean = false,
        val cacheTimestamp: Long? = null,
        val isOfflineMode: Boolean = false
    )

    /**
     * 网络配置缓存数据类
     */
    data class NetworkConfigCache(
        val lanIp: String = "",
        val lanNetmask: String = "",
        val lanDhcpEnabled: Boolean = true,
        val lanDhcpStart: String = "",
        val lanDhcpLimit: String = "",
        val lanDhcpLease: String = "",
        val wanProto: String = "dhcp",
        val wanIp: String = "",
        val wanNetmask: String = "",
        val wanGateway: String = "",
        val wanDns: String = "",
        val wanUsername: String = "",
        val wanPassword: String = ""
    )

    init {
        initNetworkMonitor()
        observeRouters()
    }

    override fun refreshData() {
        loadNetworkConfig()
    }

    private fun observeRouters() {
        viewModelScope.launch {
            routerRepository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())
                if (routers.isNotEmpty()) {
                    // 先加载缓存
                    loadFromCache()
                    // 然后从网络加载
                    loadNetworkConfig()
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

                val cachedConfig = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_LAN_CONFIG,
                    activeRouter.id,
                    NetworkConfigCache::class.java
                )

                if (cachedConfig != null) {
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_LAN_CONFIG,
                        activeRouter.id
                    )

                    _uiState.value = _uiState.value.copy(
                        lanIp = cachedConfig.lanIp,
                        lanNetmask = cachedConfig.lanNetmask,
                        lanDhcpEnabled = cachedConfig.lanDhcpEnabled,
                        lanDhcpStart = cachedConfig.lanDhcpStart,
                        lanDhcpLimit = cachedConfig.lanDhcpLimit,
                        lanDhcpLease = cachedConfig.lanDhcpLease,
                        wanProto = cachedConfig.wanProto,
                        wanIp = cachedConfig.wanIp,
                        wanNetmask = cachedConfig.wanNetmask,
                        wanGateway = cachedConfig.wanGateway,
                        wanDns = cachedConfig.wanDns,
                        wanUsername = cachedConfig.wanUsername,
                        wanPassword = cachedConfig.wanPassword,
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

    fun loadNetworkConfig() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.lanIp.isEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = isFirstLoad,
                error = null,
                success = null
            )

            try {
                // 调试模式：使用假数据

                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    // 加载LAN配置
                    val lanConfig = luciRepository.getUciConfig("network", "lan")
                    val lanIp = lanConfig["ipaddr"] ?: "192.168.1.1"
                    val lanNetmask = lanConfig["netmask"] ?: "255.255.255.0"

                    // 加载DHCP配置
                    val dhcpConfig = luciRepository.getUciConfig("dhcp", "lan")
                    val lanDhcpEnabled = (dhcpConfig["ignore"] ?: "0") != "1"
                    val lanDhcpStart = dhcpConfig["start"] ?: "100"
                    val lanDhcpLimit = dhcpConfig["limit"] ?: "150"
                    val lanDhcpLease = dhcpConfig["leasetime"] ?: "12h"

                    // 加载WAN配置
                    val wanConfig = luciRepository.getUciConfig("network", "wan")
                    val wanProto = wanConfig["proto"] ?: "dhcp"
                    val wanIp = wanConfig["ipaddr"] ?: ""
                    val wanNetmask = wanConfig["netmask"] ?: ""
                    val wanGateway = wanConfig["gateway"] ?: ""
                    val wanDns = wanConfig["dns"] ?: ""
                    val wanUsername = wanConfig["username"] ?: ""
                    val wanPassword = wanConfig["password"] ?: ""

                    val now = System.currentTimeMillis()

                    // 保存到缓存
                    val configCache = NetworkConfigCache(
                        lanIp = lanIp,
                        lanNetmask = lanNetmask,
                        lanDhcpEnabled = lanDhcpEnabled,
                        lanDhcpStart = lanDhcpStart,
                        lanDhcpLimit = lanDhcpLimit,
                        lanDhcpLease = lanDhcpLease,
                        wanProto = wanProto,
                        wanIp = wanIp,
                        wanNetmask = wanNetmask,
                        wanGateway = wanGateway,
                        wanDns = wanDns,
                        wanUsername = wanUsername,
                        wanPassword = wanPassword
                    )
                    cacheRepository.saveCache(
                        CacheRepository.KEY_LAN_CONFIG,
                        activeRouter.id,
                        "NetworkConfigCache",
                        configCache
                    )

                    _uiState.value = _uiState.value.copy(
                        lanIp = lanIp,
                        lanNetmask = lanNetmask,
                        lanDhcpEnabled = lanDhcpEnabled,
                        lanDhcpStart = lanDhcpStart,
                        lanDhcpLimit = lanDhcpLimit,
                        lanDhcpLease = lanDhcpLease,
                        wanProto = wanProto,
                        wanIp = wanIp,
                        wanNetmask = wanNetmask,
                        wanGateway = wanGateway,
                        wanDns = wanDns,
                        wanUsername = wanUsername,
                        wanPassword = wanPassword,
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
                    CacheRepository.KEY_LAN_CONFIG,
                    it.id
                )
            } ?: false

            if (hasCache && _uiState.value.lanIp.isNotEmpty()) {
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

    fun saveLanConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                error = null,
                success = null
            )

            try {
                // 调试模式：模拟保存

                // 保存LAN配置
                luciRepository.setUciConfig("network", "lan", "ipaddr", _uiState.value.lanIp)
                luciRepository.setUciConfig("network", "lan", "netmask", _uiState.value.lanNetmask)
                // 保存DHCP配置
                luciRepository.setUciConfig("dhcp", "lan", "ignore", if (_uiState.value.lanDhcpEnabled) "0" else "1")
                luciRepository.setUciConfig("dhcp", "lan", "start", _uiState.value.lanDhcpStart)
                luciRepository.setUciConfig("dhcp", "lan", "limit", _uiState.value.lanDhcpLimit)
                luciRepository.setUciConfig("dhcp", "lan", "leasetime", _uiState.value.lanDhcpLease)
                // 提交配置
                luciRepository.commitUci("network")
                luciRepository.commitUci("dhcp")

                // 保存成功后更新缓存
                val activeRouter = getActiveRouter()
                activeRouter?.let {
                    val configCache = NetworkConfigCache(
                        lanIp = _uiState.value.lanIp,
                        lanNetmask = _uiState.value.lanNetmask,
                        lanDhcpEnabled = _uiState.value.lanDhcpEnabled,
                        lanDhcpStart = _uiState.value.lanDhcpStart,
                        lanDhcpLimit = _uiState.value.lanDhcpLimit,
                        lanDhcpLease = _uiState.value.lanDhcpLease,
                        wanProto = _uiState.value.wanProto,
                        wanIp = _uiState.value.wanIp,
                        wanNetmask = _uiState.value.wanNetmask,
                        wanGateway = _uiState.value.wanGateway,
                        wanDns = _uiState.value.wanDns,
                        wanUsername = _uiState.value.wanUsername,
                        wanPassword = _uiState.value.wanPassword
                    )
                    cacheRepository.saveCache(
                        CacheRepository.KEY_LAN_CONFIG,
                        it.id,
                        "NetworkConfigCache",
                        configCache
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    success = Loc.s(R.string.s0546)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: Loc.s(R.string.s0547)
                )
            }
        }
    }

    fun saveWanConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                error = null,
                success = null
            )

            try {
                // 调试模式：模拟保存

                // 保存WAN配置
                luciRepository.setUciConfig("network", "wan", "proto", _uiState.value.wanProto)
                if (_uiState.value.wanProto == "static") {
                    luciRepository.setUciConfig("network", "wan", "ipaddr", _uiState.value.wanIp)
                    luciRepository.setUciConfig("network", "wan", "netmask", _uiState.value.wanNetmask)
                    luciRepository.setUciConfig("network", "wan", "gateway", _uiState.value.wanGateway)
                    luciRepository.setUciConfig("network", "wan", "dns", _uiState.value.wanDns)
                } else if (_uiState.value.wanProto == "pppoe") {
                    luciRepository.setUciConfig("network", "wan", "username", _uiState.value.wanUsername)
                    luciRepository.setUciConfig("network", "wan", "password", _uiState.value.wanPassword)
                }
                // 提交配置
                luciRepository.commitUci("network")

                // 保存成功后更新缓存
                val activeRouter = getActiveRouter()
                activeRouter?.let {
                    val configCache = NetworkConfigCache(
                        lanIp = _uiState.value.lanIp,
                        lanNetmask = _uiState.value.lanNetmask,
                        lanDhcpEnabled = _uiState.value.lanDhcpEnabled,
                        lanDhcpStart = _uiState.value.lanDhcpStart,
                        lanDhcpLimit = _uiState.value.lanDhcpLimit,
                        lanDhcpLease = _uiState.value.lanDhcpLease,
                        wanProto = _uiState.value.wanProto,
                        wanIp = _uiState.value.wanIp,
                        wanNetmask = _uiState.value.wanNetmask,
                        wanGateway = _uiState.value.wanGateway,
                        wanDns = _uiState.value.wanDns,
                        wanUsername = _uiState.value.wanUsername,
                        wanPassword = _uiState.value.wanPassword
                    )
                    cacheRepository.saveCache(
                        CacheRepository.KEY_LAN_CONFIG,
                        it.id,
                        "NetworkConfigCache",
                        configCache
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    success = Loc.s(R.string.s0548)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: Loc.s(R.string.s0547)
                )
            }
        }
    }

    fun updateLanIp(value: String) {
        _uiState.value = _uiState.value.copy(lanIp = value)
    }

    fun updateLanNetmask(value: String) {
        _uiState.value = _uiState.value.copy(lanNetmask = value)
    }

    fun updateLanDhcpEnabled(value: Boolean) {
        _uiState.value = _uiState.value.copy(lanDhcpEnabled = value)
    }

    fun updateLanDhcpStart(value: String) {
        _uiState.value = _uiState.value.copy(lanDhcpStart = value)
    }

    fun updateLanDhcpLimit(value: String) {
        _uiState.value = _uiState.value.copy(lanDhcpLimit = value)
    }

    fun updateLanDhcpLease(value: String) {
        _uiState.value = _uiState.value.copy(lanDhcpLease = value)
    }

    fun updateWanProto(value: String) {
        _uiState.value = _uiState.value.copy(wanProto = value)
    }

    fun updateWanIp(value: String) {
        _uiState.value = _uiState.value.copy(wanIp = value)
    }

    fun updateWanNetmask(value: String) {
        _uiState.value = _uiState.value.copy(wanNetmask = value)
    }

    fun updateWanGateway(value: String) {
        _uiState.value = _uiState.value.copy(wanGateway = value)
    }

    fun updateWanDns(value: String) {
        _uiState.value = _uiState.value.copy(wanDns = value)
    }

    fun updateWanUsername(value: String) {
        _uiState.value = _uiState.value.copy(wanUsername = value)
    }

    fun updateWanPassword(value: String) {
        _uiState.value = _uiState.value.copy(wanPassword = value)
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
