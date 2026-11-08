package com.luanmuc.openwrtmanager.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.AppNotificationManager
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 通知设置ViewModel
 *
 * 渠道开关真实控制 AppNotificationManager 是否发出对应通知；
 * 通知历史来自 AppNotificationManager 记录的真实通知。
 */
class NotificationViewModel(application: Application) : BaseViewModel(application) {

    // 通知开关状态
    private val _notificationEnabled = MutableStateFlow(true)
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled.asStateFlow()

    // 各个渠道的开关状态
    private val _routerStatusEnabled = MutableStateFlow(AppNotificationManager.isRouterStatusEnabled())
    val routerStatusEnabled: StateFlow<Boolean> = _routerStatusEnabled.asStateFlow()

    private val _firmwareEnabled = MutableStateFlow(AppNotificationManager.isFirmwareEnabled())
    val firmwareEnabled: StateFlow<Boolean> = _firmwareEnabled.asStateFlow()

    private val _networkAlertEnabled = MutableStateFlow(AppNotificationManager.isNetworkAlertEnabled())
    val networkAlertEnabled: StateFlow<Boolean> = _networkAlertEnabled.asStateFlow()

    private val _deviceEventEnabled = MutableStateFlow(AppNotificationManager.isDeviceEventEnabled())
    val deviceEventEnabled: StateFlow<Boolean> = _deviceEventEnabled.asStateFlow()

    // 通知历史记录（真实记录）
    private val _notificationHistory = MutableStateFlow<List<NotificationHistoryItem>>(emptyList())
    val notificationHistory: StateFlow<List<NotificationHistoryItem>> = _notificationHistory.asStateFlow()

    init {
        initNetworkMonitor()
        loadHistory()
    }

    /** 从 AppNotificationManager 加载真实通知历史 */
    private fun loadHistory() {
        _notificationHistory.value = AppNotificationManager.getHistory().map { r ->
            NotificationHistoryItem(
                id = r.id.toInt(),
                title = r.title,
                content = r.content,
                time = r.time,
                type = parseType(r.type)
            )
        }
    }

    private fun parseType(type: String): NotificationType = when (type) {
        AppNotificationManager.TYPE_FIRMWARE -> NotificationType.FIRMWARE
        AppNotificationManager.TYPE_NETWORK_ALERT -> NotificationType.NETWORK_ALERT
        AppNotificationManager.TYPE_DEVICE_EVENT -> NotificationType.DEVICE_EVENT
        else -> NotificationType.ROUTER_STATUS
    }

    fun setNotificationEnabled(enabled: Boolean) {
        _notificationEnabled.value = enabled
    }

    fun setRouterStatusEnabled(enabled: Boolean) {
        _routerStatusEnabled.value = enabled
        AppNotificationManager.setRouterStatusEnabled(enabled)
    }

    fun setFirmwareEnabled(enabled: Boolean) {
        _firmwareEnabled.value = enabled
        AppNotificationManager.setFirmwareEnabled(enabled)
    }

    fun setNetworkAlertEnabled(enabled: Boolean) {
        _networkAlertEnabled.value = enabled
        AppNotificationManager.setNetworkAlertEnabled(enabled)
    }

    fun setDeviceEventEnabled(enabled: Boolean) {
        _deviceEventEnabled.value = enabled
        AppNotificationManager.setDeviceEventEnabled(enabled)
    }

    fun clearHistory() {
        AppNotificationManager.clearHistory()
        _notificationHistory.value = emptyList()
    }

    override fun refreshData() {
        // 基类统一刷新接口：重新读取真实历史
        loadHistory()
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
