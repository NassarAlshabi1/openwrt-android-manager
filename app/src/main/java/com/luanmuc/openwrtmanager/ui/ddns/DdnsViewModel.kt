package com.luanmuc.openwrtmanager.ui.ddns

import android.app.Application

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.DdnsConfig
import com.luanmuc.openwrtmanager.data.model.PluginDependencies
import com.luanmuc.openwrtmanager.data.model.PluginInstallStatus
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * DDNS ViewModel
 * 实现缓存优先策略：先显示缓存数据，同时后台发起网络请求
 */
class DdnsViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())
    private val cacheRepository = CacheRepository.getInstance(application)

    private val _uiState = MutableStateFlow(DdnsUiState())
    val uiState: StateFlow<DdnsUiState> = _uiState.asStateFlow()

    data class DdnsUiState(
        val ddnsConfigs: List<DdnsConfig> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: String? = null,
        val hasRouter: Boolean = false,
        val isFromCache: Boolean = false,
        val cacheTimestamp: Long? = null,
        val isOfflineMode: Boolean = false,
        val pluginStatus: PluginInstallStatus? = null,
        val isInstallingPlugin: Boolean = false,
        val installProgress: Int = 0,
        val installMessage: String = ""
    )

    init {
        initNetworkMonitor()
        observeRouters()
        checkPluginDependency()
    }

    /**
     * 检测DDNS插件依赖
     */
    fun checkPluginDependency() {
        viewModelScope.launch {
            try {
                val luci = LuciRepository.getInstance(getApplication())
                val status = luci.checkPluginDependency(PluginDependencies.DDNS)
                _uiState.value = _uiState.value.copy(pluginStatus = status)
            } catch (e: Exception) {
                // 忽略检测错误
            }
        }
    }

    /**
     * 安装DDNS插件
     */
    fun installPlugin(onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isInstallingPlugin = true, installProgress = 0, installMessage = "")
                val luci = LuciRepository.getInstance(getApplication())
                val success = luci.installPluginDependency(PluginDependencies.DDNS) { progress, message ->
                    _uiState.value = _uiState.value.copy(installProgress = progress, installMessage = message)
                }
                if (success) {
                    val status = luci.checkPluginDependency(PluginDependencies.DDNS)
                    _uiState.value = _uiState.value.copy(
                        pluginStatus = status,
                        success = Loc.s(R.string.s0336)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = Loc.s(R.string.s0337, _uiState.value.installMessage))
                }
                onDone(success)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = Loc.s(R.string.s0147, e.message))
                onDone(false)
            } finally {
                _uiState.value = _uiState.value.copy(isInstallingPlugin = false)
            }
        }
    }

    override fun refreshData() {
        loadDdnsConfig()
    }

    /**
     * 添加DDNS配置
     */
    fun addDdns(config: DdnsConfig) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
                val activeRouter = getActiveRouter() ?: return@launch
                val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                if (!luciRepository.isLoggedIn()) {
                    luciRepository.login(activeRouter.address, activeRouter.username, password)
                }
                val success = luciRepository.addDdnsConfig(config)
                if (success) {
                    loadDdnsConfig()
                    _uiState.value = _uiState.value.copy(success = Loc.s(R.string.s0338))
                } else {
                    _uiState.value = _uiState.value.copy(error = Loc.s(R.string.s0339))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: Loc.s(R.string.s0339))
            }
        }
    }

    /**
     * 删除DDNS配置
     */
    fun deleteDdns(sectionName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
                val activeRouter = getActiveRouter() ?: return@launch
                val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                if (!luciRepository.isLoggedIn()) {
                    luciRepository.login(activeRouter.address, activeRouter.username, password)
                }
                val success = luciRepository.deleteDdnsConfig(sectionName)
                if (success) {
                    loadDdnsConfig()
                    _uiState.value = _uiState.value.copy(success = Loc.s(R.string.s0297))
                } else {
                    _uiState.value = _uiState.value.copy(error = Loc.s(R.string.s0298))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: Loc.s(R.string.s0298))
            }
        }
    }

    /**
     * 更新DDNS配置
     */
    fun updateDdns(sectionName: String, config: DdnsConfig) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
                val activeRouter = getActiveRouter() ?: return@launch
                val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                if (!luciRepository.isLoggedIn()) {
                    luciRepository.login(activeRouter.address, activeRouter.username, password)
                }
                val success = luciRepository.updateDdnsConfig(sectionName, config)
                if (success) {
                    loadDdnsConfig()
                    _uiState.value = _uiState.value.copy(success = Loc.s(R.string.s0340))
                } else {
                    _uiState.value = _uiState.value.copy(error = Loc.s(R.string.s0259))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: Loc.s(R.string.s0259))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(success = null)
    }

    private fun observeRouters() {
        viewModelScope.launch {
            routerRepository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())
                if (routers.isNotEmpty()) {
                    // 先加载缓存
                    loadFromCache()
                    // 然后从网络加载
                    loadDdnsConfig()
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

                val cachedConfigs = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_DDNS_CONFIGS,
                    activeRouter.id,
                    Array<DdnsConfig>::class.java
                )?.toList()

                if (!cachedConfigs.isNullOrEmpty()) {
                    val cacheTime = cacheRepository.getCacheTimestamp(
                        CacheRepository.KEY_DDNS_CONFIGS,
                        activeRouter.id
                    )

                    _uiState.value = _uiState.value.copy(
                        ddnsConfigs = cachedConfigs,
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

    fun loadDdnsConfig() {
        viewModelScope.launch {
            val isFirstLoad = _uiState.value.ddnsConfigs.isEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = isFirstLoad,
                error = null,
                success = null
            )

            try {
                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    // 加载DDNS配置
                    val configs = luciRepository.getDdnsConfigs()
                    val now = System.currentTimeMillis()

                    // 保存到缓存
                    cacheRepository.saveCache(
                        CacheRepository.KEY_DDNS_CONFIGS,
                        activeRouter.id,
                        "List<DdnsConfig>",
                        configs.toTypedArray()
                    )

                    _uiState.value = _uiState.value.copy(
                        ddnsConfigs = configs,
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
                    CacheRepository.KEY_DDNS_CONFIGS,
                    it.id
                )
            } ?: false

            if (hasCache && _uiState.value.ddnsConfigs.isNotEmpty()) {
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
