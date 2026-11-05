package com.luanmuc.openwrtmanager.ui.plugin_detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.model.PluginDetail
import com.luanmuc.openwrtmanager.data.model.PluginReview
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.PluginMarketRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.util.DebugMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 插件详情ViewModel
 */
class PluginDetailViewModel(application: Application) : BaseViewModel(application) {

    init {
        initNetworkMonitor()
    }
    
    private val pluginMarketRepository = PluginMarketRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(application)
    private val routerRepository = RouterRepository.getInstance(application)
    private val cacheRepository = CacheRepository.getInstance(application)
    
    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 插件详情
    private val _pluginDetail = MutableStateFlow<PluginDetail?>(null)
    val pluginDetail: StateFlow<PluginDetail?> = _pluginDetail.asStateFlow()
    
    // 插件评论
    private val _reviews = MutableStateFlow<List<PluginReview>>(emptyList())
    val reviews: StateFlow<List<PluginReview>> = _reviews.asStateFlow()
    
    // 是否正在安装
    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()
    
    // 操作结果
    private val _operationResult = MutableStateFlow<String?>(null)
    val operationResult: StateFlow<String?> = _operationResult.asStateFlow()
    
    // 错误状态
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // 当前包名
    private var currentPackageName: String = ""
    
    /**
     * 加载插件详情
     */
    fun loadPluginDetail(packageName: String) {
        currentPackageName = packageName
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // 加载插件详情
                val detail = pluginMarketRepository.getPluginDetail(packageName)
                _pluginDetail.value = detail
                
                // 加载评论
                val reviews = pluginMarketRepository.getPluginReviews(packageName)
                _reviews.value = reviews
                
            } catch (e: Exception) {
                _error.value = e.message ?: Loc.s(R.string.s0329)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 安装插件
     */
    fun installPlugin() {
        viewModelScope.launch {
            _isInstalling.value = true
            _operationResult.value = null
            _error.value = null
            try {
                // 调试模式：模拟安装
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(2000)
                    _operationResult.value = Loc.s(R.string.s0145)
                    _pluginDetail.value = _pluginDetail.value?.copy(isInstalled = true)
                    return@launch
                }
                
                val success = luciRepository.installPackage(currentPackageName)
                if (success) {
                    // 清除缓存
                    clearPackageCache()
                    _operationResult.value = Loc.s(R.string.s0145)
                    _pluginDetail.value = _pluginDetail.value?.copy(isInstalled = true)
                } else {
                    _error.value = Loc.s(R.string.s0579)
                }
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.s0580, e.message)
            } finally {
                _isInstalling.value = false
            }
        }
    }
    
    /**
     * 卸载插件
     */
    fun uninstallPlugin() {
        viewModelScope.launch {
            _isInstalling.value = true
            _operationResult.value = null
            _error.value = null
            try {
                // 调试模式：模拟卸载
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(1500)
                    _operationResult.value = Loc.s(R.string.s0581)
                    _pluginDetail.value = _pluginDetail.value?.copy(isInstalled = false)
                    return@launch
                }
                
                val success = luciRepository.removePackage(currentPackageName)
                if (success) {
                    // 清除缓存
                    clearPackageCache()
                    _operationResult.value = Loc.s(R.string.s0581)
                    _pluginDetail.value = _pluginDetail.value?.copy(isInstalled = false)
                } else {
                    _error.value = Loc.s(R.string.s0582)
                }
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.s0583, e.message)
            } finally {
                _isInstalling.value = false
            }
        }
    }
    
    /**
     * 更新插件
     */
    fun updatePlugin() {
        viewModelScope.launch {
            _isInstalling.value = true
            _operationResult.value = null
            _error.value = null
            try {
                // 调试模式：模拟更新
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(2500)
                    _operationResult.value = Loc.s(R.string.s0340)
                    _pluginDetail.value = _pluginDetail.value?.copy(
                        isUpdateAvailable = false,
                        version = _pluginDetail.value?.latestVersion ?: ""
                    )
                    return@launch
                }
                
                // 更新就是重新安装
                val success = luciRepository.installPackage(currentPackageName)
                if (success) {
                    // 清除缓存
                    clearPackageCache()
                    _operationResult.value = Loc.s(R.string.s0340)
                    _pluginDetail.value = _pluginDetail.value?.copy(
                        isUpdateAvailable = false,
                        version = _pluginDetail.value?.latestVersion ?: ""
                    )
                } else {
                    _error.value = Loc.s(R.string.s0259)
                }
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.s0584, e.message)
            } finally {
                _isInstalling.value = false
            }
        }
    }
    
    /**
     * 清除插件相关缓存
     */
    private suspend fun clearPackageCache() {
        try {
            val routers = routerRepository.getRoutersList()
            val activeId = routerRepository.getActiveRouterId()
            val activeRouter = if (activeId != null) {
                routers.find { it.id == activeId } ?: routers.firstOrNull()
            } else {
                routers.firstOrNull()
            }
            activeRouter?.let {
                cacheRepository.deleteCache(CacheRepository.KEY_INSTALLED_PACKAGES, it.id)
                cacheRepository.deleteCache(CacheRepository.KEY_AVAILABLE_PACKAGES, it.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 清除操作结果
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(date)
    }

    override fun refreshData() {
        // 基类统一刷新接口
    }
}
