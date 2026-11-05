package com.luanmuc.openwrtmanager.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 通知设置ViewModel
 */
class NotificationViewModel(application: Application) : BaseViewModel(application) {
    
    // 通知开关状态
    private val _notificationEnabled = MutableStateFlow(true)
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled.asStateFlow()
    
    // 各个渠道的开关状态
    private val _routerStatusEnabled = MutableStateFlow(true)
    val routerStatusEnabled: StateFlow<Boolean> = _routerStatusEnabled.asStateFlow()
    
    private val _firmwareEnabled = MutableStateFlow(true)
    val firmwareEnabled: StateFlow<Boolean> = _firmwareEnabled.asStateFlow()
    
    private val _networkAlertEnabled = MutableStateFlow(true)
    val networkAlertEnabled: StateFlow<Boolean> = _networkAlertEnabled.asStateFlow()
    
    private val _deviceEventEnabled = MutableStateFlow(true)
    val deviceEventEnabled: StateFlow<Boolean> = _deviceEventEnabled.asStateFlow()
    
    // 通知历史记录
    private val _notificationHistory = MutableStateFlow<List<NotificationHistoryItem>>(emptyList())
    val notificationHistory: StateFlow<List<NotificationHistoryItem>> = _notificationHistory.asStateFlow()
    
    init {
        initNetworkMonitor()
        // 加载演示数据
        loadDemoData()
    }
    
    private fun loadDemoData() {
        // 演示模式的通知历史
        val demoHistory = listOf(
            NotificationHistoryItem(
                id = 1,
                title = Loc.s(R.string.s0562),
                content = Loc.s(R.string.s0563),
                time = System.currentTimeMillis() - 3600000,
                type = NotificationType.ROUTER_STATUS
            ),
            NotificationHistoryItem(
                id = 2,
                title = Loc.s(R.string.s0564),
                content = Loc.s(R.string.s0565),
                time = System.currentTimeMillis() - 7200000,
                type = NotificationType.DEVICE_EVENT
            ),
            NotificationHistoryItem(
                id = 3,
                title = Loc.s(R.string.s0566),
                content = Loc.s(R.string.s0567),
                time = System.currentTimeMillis() - 86400000,
                type = NotificationType.FIRMWARE
            )
        )
        _notificationHistory.value = demoHistory
    }
    
    fun setNotificationEnabled(enabled: Boolean) {
        _notificationEnabled.value = enabled
    }
    
    fun setRouterStatusEnabled(enabled: Boolean) {
        _routerStatusEnabled.value = enabled
    }
    
    fun setFirmwareEnabled(enabled: Boolean) {
        _firmwareEnabled.value = enabled
    }
    
    fun setNetworkAlertEnabled(enabled: Boolean) {
        _networkAlertEnabled.value = enabled
    }
    
    fun setDeviceEventEnabled(enabled: Boolean) {
        _deviceEventEnabled.value = enabled
    }
    
    fun clearHistory() {
        _notificationHistory.value = emptyList()
    }

    override fun refreshData() {
        // 基类统一刷新接口
    }
}

/**
 * 通知历史记录项
 */
data class NotificationHistoryItem(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val time: Long = 0,
    val type: NotificationType = NotificationType.ROUTER_STATUS
)

/**
 * 通知类型
 */
enum class NotificationType {
    ROUTER_STATUS,
    FIRMWARE,
    NETWORK_ALERT,
    DEVICE_EVENT;
    val displayName: String
        get() = when (this) {
            ROUTER_STATUS -> Loc.s(R.string.enum_004)
            FIRMWARE -> Loc.s(R.string.enum_017)
            NETWORK_ALERT -> Loc.s(R.string.enum_018)
            DEVICE_EVENT -> Loc.s(R.string.enum_019)
        }
}
