package com.luanmuc.openwrtmanager.ui.widget_config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 桌面小部件配置ViewModel
 */
class WidgetConfigViewModel(application: Application) : BaseViewModel(application) {
    
    // 小部件开关
    private val _routerStatusWidgetEnabled = MutableStateFlow(true)
    val routerStatusWidgetEnabled: StateFlow<Boolean> = _routerStatusWidgetEnabled.asStateFlow()
    
    private val _networkSpeedWidgetEnabled = MutableStateFlow(true)
    val networkSpeedWidgetEnabled: StateFlow<Boolean> = _networkSpeedWidgetEnabled.asStateFlow()
    
    // 更新间隔（分钟）
    private val _updateInterval = MutableStateFlow(60)
    val updateInterval: StateFlow<Int> = _updateInterval.asStateFlow()
    
    // 显示内容设置
    private val _showRouterName = MutableStateFlow(true)
    val showRouterName: StateFlow<Boolean> = _showRouterName.asStateFlow()
    
    private val _showOnlineStatus = MutableStateFlow(true)
    val showOnlineStatus: StateFlow<Boolean> = _showOnlineStatus.asStateFlow()
    
    private val _showDeviceCount = MutableStateFlow(true)
    val showDeviceCount: StateFlow<Boolean> = _showDeviceCount.asStateFlow()
    
    private val _showSpeed = MutableStateFlow(true)
    val showSpeed: StateFlow<Boolean> = _showSpeed.asStateFlow()
    
    // 外观设置
    private val _widgetTheme = MutableStateFlow("auto")
    val widgetTheme: StateFlow<String> = _widgetTheme.asStateFlow()
    
    private val _widgetOpacity = MutableStateFlow(100)
    val widgetOpacity: StateFlow<Int> = _widgetOpacity.asStateFlow()
    
    // 可用的更新间隔选项
    val updateIntervalOptions = listOf(15, 30, 60, 120, 300)
    
    // 可用的主题选项
    val themeOptions = listOf(
        "auto" to Loc.s(R.string.s0678),
        "light" to Loc.s(R.string.s0796),
        "dark" to Loc.s(R.string.s0797)
    )
    
    init {
        initNetworkMonitor()
        loadWidgetConfig()
    }
    
    private fun loadWidgetConfig() {
        viewModelScope.launch {
            try {
                // 从SharedPreferences加载配置
                val prefs = getApplication<Application>().getSharedPreferences(
                    "widget_config",
                    android.content.Context.MODE_PRIVATE
                )
                
                _routerStatusWidgetEnabled.value = prefs.getBoolean("router_status_enabled", true)
                _networkSpeedWidgetEnabled.value = prefs.getBoolean("network_speed_enabled", true)
                _updateInterval.value = prefs.getInt("update_interval", 60)
                _showRouterName.value = prefs.getBoolean("show_router_name", true)
                _showOnlineStatus.value = prefs.getBoolean("show_online_status", true)
                _showDeviceCount.value = prefs.getBoolean("show_device_count", true)
                _showSpeed.value = prefs.getBoolean("show_speed", true)
                _widgetTheme.value = prefs.getString("widget_theme", "auto") ?: "auto"
                _widgetOpacity.value = prefs.getInt("widget_opacity", 100)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 保存配置
     */
    private fun saveConfig() {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences(
                    "widget_config",
                    android.content.Context.MODE_PRIVATE
                )
                
                prefs.edit().apply {
                    putBoolean("router_status_enabled", _routerStatusWidgetEnabled.value)
                    putBoolean("network_speed_enabled", _networkSpeedWidgetEnabled.value)
                    putInt("update_interval", _updateInterval.value)
                    putBoolean("show_router_name", _showRouterName.value)
                    putBoolean("show_online_status", _showOnlineStatus.value)
                    putBoolean("show_device_count", _showDeviceCount.value)
                    putBoolean("show_speed", _showSpeed.value)
                    putString("widget_theme", _widgetTheme.value)
                    putInt("widget_opacity", _widgetOpacity.value)
                    apply()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 设置路由器状态小部件开关
     */
    fun setRouterStatusWidgetEnabled(enabled: Boolean) {
        _routerStatusWidgetEnabled.value = enabled
        saveConfig()
    }
    
    /**
     * 设置网速小部件开关
     */
    fun setNetworkSpeedWidgetEnabled(enabled: Boolean) {
        _networkSpeedWidgetEnabled.value = enabled
        saveConfig()
    }
    
    /**
     * 设置更新间隔
     */
    fun setUpdateInterval(interval: Int) {
        _updateInterval.value = interval
        saveConfig()
    }
    
    /**
     * 设置显示路由器名称
     */
    fun setShowRouterName(show: Boolean) {
        _showRouterName.value = show
        saveConfig()
    }
    
    /**
     * 设置显示在线状态
     */
    fun setShowOnlineStatus(show: Boolean) {
        _showOnlineStatus.value = show
        saveConfig()
    }
    
    /**
     * 设置显示设备数量
     */
    fun setShowDeviceCount(show: Boolean) {
        _showDeviceCount.value = show
        saveConfig()
    }
    
    /**
     * 设置显示速度
     */
    fun setShowSpeed(show: Boolean) {
        _showSpeed.value = show
        saveConfig()
    }
    
    /**
     * 设置小部件主题
     */
    fun setWidgetTheme(theme: String) {
        _widgetTheme.value = theme
        saveConfig()
    }
    
    /**
     * 设置小部件透明度
     */
    fun setWidgetOpacity(opacity: Int) {
        _widgetOpacity.value = opacity
        saveConfig()
    }
    
    /**
     * 格式化更新间隔
     */
    fun formatUpdateInterval(minutes: Int): String {
        return when {
            minutes < 60 -> Loc.s(R.string.s0523, minutes)
            minutes % 60 == 0 -> Loc.s(R.string.s0798, minutes / 60)
            else -> Loc.s(R.string.s0522, minutes / 60, minutes % 60)
        }
    }

    override fun refreshData() {
        // 基类统一刷新接口
    }
}
