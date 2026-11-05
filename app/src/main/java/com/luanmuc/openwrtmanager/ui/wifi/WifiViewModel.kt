package com.luanmuc.openwrtmanager.ui.wifi

import android.app.Application

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.model.WifiInterface
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.DebugMode
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * WiFi设置 ViewModel
 * 实现缓存优先策略：先显示缓存数据，同时后台发起网络请求
 */
class WifiViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())
    private val cacheRepository = CacheRepository.getInstance(application)

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    data class WifiUiState(
        val wifi2g: WifiConfig = WifiConfig(),
        val wifi5g: WifiConfig = WifiConfig(),
        val guestWifi: WifiConfig = WifiConfig(),
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val error: String? = null,
        val success: String? = null,
        val hasRouter: Boolean = false,
        val has5g: Boolean = false,
        val isFromCache: Boolean = false,
        val cacheTimestamp: Long? = null,
        val isOfflineMode: Boolean = false
    )

    data class WifiConfig(
        val enabled: Boolean = true,
        val ssid: String = "",
        val password: String = "",
        val channel: String = "auto",
        val bandwidth: String = "20",
        val txpower: String = "20",
        val encryption: String = "psk2"
    )

    /**
     * WiFi配置缓存数据类
     */
    data class WifiConfigCache(
        val wifi2g: WifiConfig = WifiConfig(),
        val wifi5g: WifiConfig = WifiConfig(),
        val guestWifi: WifiConfig = WifiConfig(),
        val has5g: Boolean = false
    )

    init {
        initNetworkMonitor()
        observeRouters()
    }

    override fun refreshData() {
        loadWifiConfig()
    }

    private fun observeRouters() {

        viewModelScope.launch {
            routerRepository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())
                if (routers.isNotEmpty()) {
                    // 先加载缓存
                    loadFromCache()
                    // 然后从网络加载
                    loadWifiConfig()
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

                val cachedConfig = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_WIFI_DEVICES,
                    activeRouter.id,
                    WifiConfigCache::class.java
                )

                if (cachedConfig != null) {
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_WIFI_DEVICES,
                        activeRouter.id
                    )

                    _uiState.value = _uiState.value.copy(
                        wifi2g = cachedConfig.wifi2g,
                        wifi5g = cachedConfig.wifi5g,
                        guestWifi = cachedConfig.guestWifi,
                        has5g = cachedConfig.has5g,
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

    fun loadWifiConfig() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.wifi2g.ssid.isEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = isFirstLoad,
                error = null,
                success = null
            )

            try {
                // 调试模式：使用假数据
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(800)
                    val wifi2g = WifiConfig(
                        enabled = true,
                        ssid = "OpenWrt-2.4G",
                        password = "12345678",
                        channel = "auto",
                        bandwidth = "20",
                        txpower = "20",
                        encryption = "psk2"
                    )
                    val wifi5g = WifiConfig(
                        enabled = true,
                        ssid = "OpenWrt-5G",
                        password = "12345678",
                        channel = "auto",
                        bandwidth = "80",
                        txpower = "23",
                        encryption = "psk2"
                    )
                    val guestWifi = WifiConfig(
                        enabled = false,
                        ssid = "OpenWrt-Guest",
                        password = "guest123",
                        channel = "auto",
                        bandwidth = "20",
                        txpower = "10",
                        encryption = "psk2"
                    )
                    _uiState.value = _uiState.value.copy(
                        wifi2g = wifi2g,
                        wifi5g = wifi5g,
                        guestWifi = guestWifi,
                        has5g = true,
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

                    val wifiDevices = luciRepository.getWifiDevices()
                    val has5g = wifiDevices.size > 1

                    // 加载2.4G配置
                    val iface2g = luciRepository.getWifiDeviceInfo("radio0")
                    val wifi2gHtmode = luciRepository.getUciConfigValue("wireless", "radio0", "htmode") ?: "HT20"
                    val wifi2gBandwidth = when {
                        wifi2gHtmode.contains("160") -> "160"
                        wifi2gHtmode.contains("80") -> "80"
                        wifi2gHtmode.contains("40") -> "40"
                        else -> "20"
                    }
                    val wifi2g = WifiConfig(
                        enabled = iface2g.isUp,
                        ssid = iface2g.ssid,
                        channel = iface2g.channel.toString(),
                        txpower = iface2g.txpower.toString(),
                        bandwidth = wifi2gBandwidth,
                        encryption = iface2g.encryption.ifEmpty { "psk2" }
                    )

                    // 加载5G配置
                    val wifi5g = if (has5g) {
                        val iface5g = luciRepository.getWifiDeviceInfo("radio1")
                        val wifi5gHtmode = luciRepository.getUciConfigValue("wireless", "radio1", "htmode") ?: "VHT80"
                        val wifi5gBandwidth = when {
                            wifi5gHtmode.contains("160") -> "160"
                            wifi5gHtmode.contains("80") -> "80"
                            wifi5gHtmode.contains("40") -> "40"
                            else -> "20"
                        }
                        WifiConfig(
                            enabled = iface5g.isUp,
                            ssid = iface5g.ssid,
                            channel = iface5g.channel.toString(),
                            txpower = iface5g.txpower.toString(),
                            bandwidth = wifi5gBandwidth,
                            encryption = iface5g.encryption.ifEmpty { "psk2" }
                        )
                    } else {
                        WifiConfig()
                    }

                    // 访客网络（简化处理，使用默认配置）
                    val guestWifi = WifiConfig(
                        enabled = false,
                        ssid = "",
                        channel = "auto",
                        txpower = "10"
                    )

                    val now = System.currentTimeMillis()

                    // 保存到缓存
                    val configCache = WifiConfigCache(
                        wifi2g = wifi2g,
                        wifi5g = wifi5g,
                        guestWifi = guestWifi,
                        has5g = has5g
                    )
                    cacheRepository.saveCache(
                        CacheRepository.KEY_WIFI_DEVICES,
                        activeRouter.id,
                        "WifiConfigCache",
                        configCache
                    )

                    _uiState.value = _uiState.value.copy(
                        wifi2g = wifi2g,
                        wifi5g = wifi5g,
                        guestWifi = guestWifi,
                        has5g = has5g,
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
                    CacheRepository.KEY_WIFI_DEVICES,
                    it.id
                )
            } ?: false

            if (hasCache && _uiState.value.wifi2g.ssid.isNotEmpty()) {
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

    fun saveWifiConfig(band: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                error = null,
                success = null
            )

            try {
                // 调试模式：模拟保存
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(1000)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        success = Loc.s(R.string.s0807)
                    )
                    return@launch
                }

                val config = when (band) {
                    "2g" -> _uiState.value.wifi2g
                    "5g" -> _uiState.value.wifi5g
                    else -> _uiState.value.guestWifi
                }

                val radio = when (band) {
                    "2g" -> "radio0"
                    "5g" -> "radio1"
                    else -> "radio0"
                }

                // 保存WiFi配置
                luciRepository.setUciConfig("wireless", radio, "channel", config.channel)
                luciRepository.setUciConfig("wireless", radio, "txpower", config.txpower)
                // 保存信道宽度（htmode）
                val htmode = when (config.bandwidth) {
                    "20" -> if (band == "5g") "VHT20" else "HT20"
                    "40" -> if (band == "5g") "VHT40" else "HT40"
                    "80" -> "VHT80"
                    "160" -> "VHT160"
                    else -> "HT20"
                }
                luciRepository.setUciConfig("wireless", radio, "htmode", htmode)

                // 保存接口配置
                val iface = if (band == "guest") "guest" else "default_radio${band.last()}"
                luciRepository.setUciConfig("wireless", "default_$radio", "ssid", config.ssid)
                luciRepository.setUciConfig("wireless", "default_$radio", "disabled", if (config.enabled) "0" else "1")

                if (config.password.isNotEmpty()) {
                    luciRepository.setUciConfig("wireless", "default_$radio", "key", config.password)
                    luciRepository.setUciConfig("wireless", "default_$radio", "encryption", config.encryption)
                }

                // 提交配置
                luciRepository.commitUci("wireless")
                // 重启WiFi
                luciRepository.restartWifi()

                // 保存成功后更新缓存
                val activeRouter = getActiveRouter()
                activeRouter?.let {
                    val configCache = WifiConfigCache(
                        wifi2g = _uiState.value.wifi2g,
                        wifi5g = _uiState.value.wifi5g,
                        guestWifi = _uiState.value.guestWifi,
                        has5g = _uiState.value.has5g
                    )
                    cacheRepository.saveCache(
                        CacheRepository.KEY_WIFI_DEVICES,
                        it.id,
                        "WifiConfigCache",
                        configCache
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    success = Loc.s(R.string.s0807)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: Loc.s(R.string.s0547)
                )
            }
        }
    }

    fun updateWifi2g(config: WifiConfig) {
        _uiState.value = _uiState.value.copy(wifi2g = config)
    }

    fun updateWifi5g(config: WifiConfig) {
        _uiState.value = _uiState.value.copy(wifi5g = config)
    }

    fun updateGuestWifi(config: WifiConfig) {
        _uiState.value = _uiState.value.copy(guestWifi = config)
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
