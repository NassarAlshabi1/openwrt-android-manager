package com.luanmuc.openwrtmanager.ui.remote

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.util.DebugMode
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 远程管理 ViewModel
 */
class RemoteManageViewModel(application: Application) : BaseViewModel(application) {

    init {
        initNetworkMonitor()
        loadRemoteConfig()
    }

    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(application)

    private val _uiState = MutableStateFlow(RemoteManageUiState())
    val uiState: StateFlow<RemoteManageUiState> = _uiState.asStateFlow()

    data class RemoteManageUiState(
        val remoteEnabled: Boolean = false,
        val remoteAddress: String = "",
        val remotePort: Int = 443,
        val remoteProtocol: String = "https",
        val autoSwitchRemote: Boolean = true,
        val isRemoteMode: Boolean = false,
        val isTesting: Boolean = false,
        val testResult: String? = null,
        val testSuccess: Boolean = false
    )

    /**
     * 加载当前路由器的远程配置
     */
    fun loadRemoteConfig() {
        viewModelScope.launch {
            if (DebugMode.isDebugMode) {
                // 调试模式：使用演示路由器的配置
                val demoRouter = DebugMode.getDemoRouter()
                _uiState.value = _uiState.value.copy(
                    remoteEnabled = demoRouter.remoteEnabled,
                    remoteAddress = demoRouter.remoteAddress,
                    remotePort = demoRouter.remotePort,
                    remoteProtocol = demoRouter.remoteProtocol,
                    autoSwitchRemote = demoRouter.autoSwitchRemote
                )
            } else {
                val activeRouterId = routerRepository.getActiveRouterId()
                if (activeRouterId != null) {
                    val router = routerRepository.getRouterById(activeRouterId)
                    if (router != null) {
                        _uiState.value = _uiState.value.copy(
                            remoteEnabled = router.remoteEnabled,
                            remoteAddress = router.remoteAddress,
                            remotePort = router.remotePort,
                            remoteProtocol = router.remoteProtocol,
                            autoSwitchRemote = router.autoSwitchRemote
                        )
                    }
                }
            }
            // 检测当前网络环境
            detectNetworkMode()
        }
    }

    /**
     * 检测当前网络模式（本地/远程）
     */
    fun detectNetworkMode() {
        val isWifi = isWifiConnected()
        val shouldUseRemote = !isWifi && _uiState.value.remoteEnabled && _uiState.value.autoSwitchRemote
        _uiState.value = _uiState.value.copy(isRemoteMode = shouldUseRemote)
    }

    /**
     * 检查是否连接WiFi
     */
    private fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = getApplication<android.app.Application>()
                .getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) {
            true
        }
    }

    fun onRemoteEnabledChange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(remoteEnabled = enabled)
        saveRemoteConfig()
    }

    fun onRemoteAddressChange(address: String) {
        _uiState.value = _uiState.value.copy(remoteAddress = address)
        saveRemoteConfig()
    }

    fun onRemotePortChange(port: Int) {
        _uiState.value = _uiState.value.copy(remotePort = port)
        saveRemoteConfig()
    }

    fun onRemoteProtocolChange(protocol: String) {
        _uiState.value = _uiState.value.copy(remoteProtocol = protocol)
        saveRemoteConfig()
    }

    fun onAutoSwitchRemoteChange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSwitchRemote = enabled)
        saveRemoteConfig()
    }

    fun onForceRemoteMode(remote: Boolean) {
        _uiState.value = _uiState.value.copy(isRemoteMode = remote)
    }

    /**
     * 保存远程配置到路由器
     */
    private fun saveRemoteConfig() {
        viewModelScope.launch {
            val activeRouterId = routerRepository.getActiveRouterId()
            if (activeRouterId != null) {
                val router = routerRepository.getRouterById(activeRouterId)
                if (router != null) {
                    val updatedRouter = router.copy(
                        remoteEnabled = _uiState.value.remoteEnabled,
                        remoteAddress = _uiState.value.remoteAddress,
                        remotePort = _uiState.value.remotePort,
                        remoteProtocol = _uiState.value.remoteProtocol,
                        autoSwitchRemote = _uiState.value.autoSwitchRemote
                    )
                    routerRepository.updateRouter(updatedRouter)
                }
            }
        }
    }

    /**
     * 测试远程连接
     */
    fun testRemoteConnection() {
        val state = _uiState.value
        if (state.remoteAddress.isBlank()) {
            _uiState.value = state.copy(testResult = Loc.s(R.string.s0722), testSuccess = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isTesting = true, testResult = null)
            try {
                val address = "${state.remoteProtocol}://${state.remoteAddress}:${state.remotePort}"
                val activeRouterId = routerRepository.getActiveRouterId()
                val router = routerRepository.getRouterById(activeRouterId ?: "")

                if (router != null) {
                    val result = luciRepository.login(
                        address = address,
                        username = router.username,
                        password = router.encryptedPassword
                    )
                    if (result.isNotBlank() && !result.contains("error", ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            isTesting = false,
                            testResult = Loc.s(R.string.s0723),
                            testSuccess = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isTesting = false,
                            testResult = Loc.s(R.string.s0724, result.ifEmpty { Loc.s(R.string.s0115) }),
                            testSuccess = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = Loc.s(R.string.s0724, e.message),
                    testSuccess = false
                )
            }
        }
    }

    override fun refreshData() {
        loadRemoteConfig()
    }
}
