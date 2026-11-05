package com.luanmuc.openwrtmanager.ui.addrouter

import com.luanmuc.openwrtmanager.data.repository.SettingsRepository

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.LuciException
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 添加路由器 ViewModel
 */
class AddRouterViewModel(application: Application) : BaseViewModel(application) {
    
    init {
        initNetworkMonitor()
    }
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())

    private val _uiState = MutableStateFlow(AddRouterUiState())
    val uiState: StateFlow<AddRouterUiState> = _uiState.asStateFlow()

    data class AddRouterUiState(
        val name: String = "",
        val address: String = "",
        val username: String = "root",
        val password: String = "",
        val isConnecting: Boolean = false,
        val isSuccess: Boolean = false,
        val error: String? = null,
        val errorType: ErrorType? = null,
        val isEditMode: Boolean = false,
        val editingRouterId: String? = null,
        // 预加载状态
        val isPreloading: Boolean = false,
        val preloadProgress: Float = 0f,
        val preloadCurrentItem: String = "",
        val preloadTotal: Int = 0,
        val preloadCurrent: Int = 0
    )

    enum class ErrorType {
        NETWORK, AUTH, NOT_FOUND, TIMEOUT, UNKNOWN
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun onAddressChange(address: String) {
        _uiState.value = _uiState.value.copy(address = address, error = null)
    }

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    /**
     * 连接并保存路由器
     */
    fun connectAndSave(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.address.isBlank()) {
            _uiState.value = state.copy(error = Loc.s(R.string.s0255))
            return
        }
        if (state.username.isBlank()) {
            _uiState.value = state.copy(error = Loc.s(R.string.s0256))
            return
        }

        // 自动补全地址格式
        val normalizedAddress = normalizeAddress(state.address)

        viewModelScope.launch {
            _uiState.value = state.copy(
                isConnecting = true,
                error = null,
                address = normalizedAddress
            )
            try {
                // 尝试连接认证
                luciRepository.login(
                    address = normalizedAddress,
                    username = state.username,
                    password = state.password
                )

                // 获取系统信息用于设置设备名称
                var deviceName = state.name
                if (deviceName.isBlank()) {
                    try {
                        val sysInfo = luciRepository.getSystemInfo()
                        val boardInfo = luciRepository.getBoardInfo()
                        deviceName = boardInfo["hostname"]?.toString()
                            ?: sysInfo["hostname"]?.toString()
                            ?: normalizedAddress
                    } catch (e: Exception) {
                        deviceName = normalizedAddress
                    }
                }

                // 加密密码并保存
                val encryptedPassword = EncryptionUtil.encrypt(state.password)
                val router = Router(
                    id = EncryptionUtil.generateId(),
                    name = deviceName,
                    address = normalizedAddress,
                    username = state.username,
                    encryptedPassword = encryptedPassword,
                    isConnected = true,
                    lastConnected = System.currentTimeMillis()
                )

                routerRepository.addRouter(router)
                routerRepository.setActiveRouter(router.id)

                // 检查是否开启自动预加载
                val settingsRepository = SettingsRepository.getInstance(getApplication())
                if (settingsRepository.autoPreloadEnabled) {
                    // 开始预加载
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isPreloading = true,
                        preloadProgress = 0f,
                        preloadCurrentItem = Loc.s(R.string.s0257)
                    )

                    // 在后台进行预加载
                    viewModelScope.launch {
                        try {
                            luciRepository.preloadAllData(router.id) { progress ->
                                _uiState.value = _uiState.value.copy(
                                    preloadProgress = progress.percentage,
                                    preloadCurrentItem = progress.currentItem,
                                    preloadCurrent = progress.current,
                                    preloadTotal = progress.total
                                )
                            }
                        } catch (e: Exception) {
                            // 预加载失败不影响使用
                        } finally {
                            _uiState.value = _uiState.value.copy(
                                isPreloading = false,
                                isSuccess = true
                            )
                            onSuccess()
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isSuccess = true
                    )
                    onSuccess()
                }
            } catch (e: LuciException) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = e.message,
                    errorType = when (e.type) {
                        com.luanmuc.openwrtmanager.data.repository.ErrorType.NETWORK_ERROR -> ErrorType.NETWORK
                        com.luanmuc.openwrtmanager.data.repository.ErrorType.TIMEOUT -> ErrorType.TIMEOUT
                        com.luanmuc.openwrtmanager.data.repository.ErrorType.AUTH_FAILED -> ErrorType.AUTH
                        com.luanmuc.openwrtmanager.data.repository.ErrorType.NOT_FOUND -> ErrorType.NOT_FOUND
                        else -> ErrorType.UNKNOWN
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = e.message ?: Loc.s(R.string.s0258),
                    errorType = ErrorType.UNKNOWN
                )
            }
        }
    }

    /**
     * 规范化地址格式
     */
    private fun normalizeAddress(address: String): String {
        var addr = address.trim()
        if (addr.isEmpty()) return addr

        // 自动添加 http:// 前缀
        if (!addr.startsWith("http://") && !addr.startsWith("https://")) {
            addr = "http://$addr"
        }

        // 确保以 / 结尾
        if (!addr.endsWith("/")) {
            addr = "$addr/"
        }

        return addr
    }
    
    override fun refreshData() {
        // 添加路由器页面不需要自动刷新
    }
    
    /**
     * 加载要编辑的路由器
     */
    fun loadRouter(routerId: String) {
        viewModelScope.launch {
            val router = routerRepository.getRouterById(routerId)
            if (router != null) {
                _uiState.value = _uiState.value.copy(
                    name = router.name,
                    address = router.address,
                    username = router.username,
                    password = "",  // 密码不预填，需要重新输入
                    isEditMode = true,
                    editingRouterId = routerId
                )
            }
        }
    }
    
    /**
     * 更新路由器
     */
    fun updateRouter(onSuccess: () -> Unit) {
        val state = _uiState.value
        val routerId = state.editingRouterId ?: return

        if (state.address.isBlank()) {
            _uiState.value = state.copy(error = Loc.s(R.string.s0255))
            return
        }
        if (state.username.isBlank()) {
            _uiState.value = state.copy(error = Loc.s(R.string.s0256))
            return
        }

        val normalizedAddress = normalizeAddress(state.address)

        viewModelScope.launch {
            _uiState.value = state.copy(
                isConnecting = true,
                error = null,
                address = normalizedAddress
            )
            try {
                // 如果密码不为空，尝试重新连接验证
                if (state.password.isNotBlank()) {
                    luciRepository.login(
                        address = normalizedAddress,
                        username = state.username,
                        password = state.password
                    )
                }

                // 获取现有路由器
                val existingRouter = routerRepository.getRouterById(routerId)
                if (existingRouter != null) {
                    // 加密密码（如果有新密码）
                    val encryptedPassword = if (state.password.isNotBlank()) {
                        EncryptionUtil.encrypt(state.password)
                    } else {
                        existingRouter.encryptedPassword
                    }
                    
                    val updatedRouter = existingRouter.copy(
                        name = state.name.ifBlank { normalizedAddress },
                        address = normalizedAddress,
                        username = state.username,
                        encryptedPassword = encryptedPassword
                    )
                    
                    routerRepository.updateRouter(updatedRouter)
                }

                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    isSuccess = true
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = e.message ?: Loc.s(R.string.s0259),
                    errorType = ErrorType.UNKNOWN
                )
            }
        }
    }
}
