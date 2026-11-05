package com.luanmuc.openwrtmanager.ui.firewall

import android.app.Application

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.PortForwardRule
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
 * 防火墙 ViewModel
 * 实现缓存优先策略：先显示缓存数据，同时后台发起网络请求
 */
class FirewallViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())
    private val cacheRepository = CacheRepository.getInstance(application)

    private val _uiState = MutableStateFlow(FirewallUiState())
    val uiState: StateFlow<FirewallUiState> = _uiState.asStateFlow()

    data class FirewallUiState(
        val portForwards: List<PortForwardRule> = emptyList(),
        val firewallEnabled: Boolean = true,
        val dmzEnabled: Boolean = false,
        val dmzIp: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: String? = null,
        val hasRouter: Boolean = false,
        val isFromCache: Boolean = false,
        val cacheTimestamp: Long? = null,
        val isOfflineMode: Boolean = false
    )

    init {
        initNetworkMonitor()
        observeRouters()
    }

    override fun refreshData() {
        loadFirewallConfig()
    }

    private fun observeRouters() {

        viewModelScope.launch {
            routerRepository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())
                if (routers.isNotEmpty()) {
                    // 先加载缓存
                    loadFromCache()
                    // 然后从网络加载
                    loadFirewallConfig()
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

                val cachedPortForwards = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_PORT_FORWARDS,
                    activeRouter.id,
                    Array<PortForwardRule>::class.java
                )?.toList()

                if (!cachedPortForwards.isNullOrEmpty()) {
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_PORT_FORWARDS,
                        activeRouter.id
                    )

                    _uiState.value = _uiState.value.copy(
                        portForwards = cachedPortForwards,
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

    fun loadFirewallConfig() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.portForwards.isEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = isFirstLoad,
                error = null,
                success = null
            )

            try {
                // 调试模式：使用假数据
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(600)
                    val portForwards = DebugMode.getFakePortForwards()
                    _uiState.value = _uiState.value.copy(
                        portForwards = portForwards,
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

                    // 加载端口转发规则
                    val portForwards = luciRepository.getPortForwards()
                    val now = System.currentTimeMillis()

                    // 保存到缓存
                    cacheRepository.saveCache(
                        CacheRepository.KEY_PORT_FORWARDS,
                        activeRouter.id,
                        "List<PortForwardRule>",
                        portForwards.toTypedArray()
                    )

                    _uiState.value = _uiState.value.copy(
                        portForwards = portForwards,
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
                    CacheRepository.KEY_PORT_FORWARDS,
                    it.id
                )
            } ?: false

            if (hasCache && _uiState.value.portForwards.isNotEmpty()) {
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

    fun deletePortForward(name: String) {
        viewModelScope.launch {
            try {
                // 调试模式：模拟删除
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(500)
                    val portForwards = _uiState.value.portForwards.filter { it.name != name }
                    _uiState.value = _uiState.value.copy(
                        portForwards = portForwards,
                        success = Loc.s(R.string.s0415)
                    )
                    return@launch
                }

                // 删除端口转发规则
                luciRepository.deletePortForward(name)
                luciRepository.commitUci("firewall")

                // 删除成功后清除缓存并重新加载
                val activeRouter = getActiveRouter()
                activeRouter?.let {
                    cacheRepository.deleteCache(
                        CacheRepository.KEY_PORT_FORWARDS,
                        it.id
                    )
                }

                // 重新加载
                loadFirewallConfig()
                _uiState.value = _uiState.value.copy(success = Loc.s(R.string.s0415))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: Loc.s(R.string.s0298))
            }
        }
    }

    fun addPortForward(rule: PortForwardRule) {
        viewModelScope.launch {
            try {
                // 调试模式：模拟添加
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(800)
                    val portForwards = _uiState.value.portForwards + rule
                    _uiState.value = _uiState.value.copy(
                        portForwards = portForwards,
                        success = Loc.s(R.string.s0416)
                    )
                    return@launch
                }

                // 添加端口转发规则
                luciRepository.addPortForward(rule)
                luciRepository.commitUci("firewall")

                // 添加成功后清除缓存并重新加载
                val activeRouter = getActiveRouter()
                activeRouter?.let {
                    cacheRepository.deleteCache(
                        CacheRepository.KEY_PORT_FORWARDS,
                        it.id
                    )
                }

                // 重新加载
                loadFirewallConfig()
                _uiState.value = _uiState.value.copy(success = Loc.s(R.string.s0416))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: Loc.s(R.string.s0339))
            }
        }
    }

    fun editPortForward(ruleName: String, rule: PortForwardRule) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val activeRouter = getActiveRouter()
                if (activeRouter == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = Loc.s(R.string.s0417)
                    )
                    return@launch
                }

                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(800)
                    // 演示模式：更新本地列表
                    val updatedList = _uiState.value.portForwards.map {
                        if (it.name == ruleName) rule.copy(name = ruleName) else it
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        portForwards = updatedList,
                        success = Loc.s(R.string.s0418)
                    )
                    return@launch
                }

                // 真实模式：调用LuCI API更新
                luciRepository.updatePortForward(ruleName, rule)

                // 更新成功后清除缓存并重新加载
                cacheRepository.deleteCache(
                    CacheRepository.KEY_PORT_FORWARDS,
                    activeRouter.id
                )

                loadFirewallConfig()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = Loc.s(R.string.s0418)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: Loc.s(R.string.s0259)
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
}
