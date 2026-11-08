package com.luanmuc.openwrtmanager.util

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.luanmuc.openwrtmanager.MainActivity
import com.luanmuc.openwrtmanager.R

/**
 * 通知管理器
 * 负责管理所有APP通知。
 *
 * 每条通知在发出的同时写入持久化的历史记录（最多 50 条），
 * 通知设置页显示的就是这份真实历史；各渠道开关也真实控制
 * 对应通知是否允许发出。
 */
object AppNotificationManager {

    // 通知渠道ID
    const val CHANNEL_ROUTER_STATUS = "router_status"
    const val CHANNEL_FIRMWARE = "firmware_update"
    const val CHANNEL_NETWORK = "network_alert"
    const val CHANNEL_DEVICE = "device_event"

    // 通知ID
    private const val NOTIFICATION_OFFLINE = 1001
    private const val NOTIFICATION_FIRMWARE = 1002
    private const val NOTIFICATION_NEW_DEVICE = 1003
    private const val NOTIFICATION_NETWORK_ERROR = 1004

    private const val HISTORY_PREFS = "notification_history"
    private const val KEY_HISTORY = "history_json"
    private const val HISTORY_LIMIT = 50

    // 渠道开关（通知设置页控制，默认开启）
    private const val PREFS_SWITCHES = "notification_switches"
    private const val KEY_SWITCH_ROUTER_STATUS = "router_status_enabled"
    private const val KEY_SWITCH_FIRMWARE = "firmware_enabled"
    private const val KEY_SWITCH_NETWORK = "network_alert_enabled"
    private const val KEY_SWITCH_DEVICE = "device_event_enabled"

    private var context: Context? = null
    private var initialized = false
    private val gson = Gson()

    /** 一条历史记录 */
    data class NotificationRecord(
        val id: Long,
        val title: String,
        val content: String,
        val time: Long,
        val type: String
    )

    /**
     * 初始化通知管理器
     */
    fun init(context: Context) {
        if (initialized) return
        this.context = context.applicationContext
        initialized = true

        createNotificationChannels()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = context ?: return
            val nm = ctx.getSystemService(AndroidNotificationManager::class.java)

            // 路由器状态渠道
            val statusChannel = NotificationChannel(
                CHANNEL_ROUTER_STATUS,
                Loc.s(R.string.s0131),
                AndroidNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = Loc.s(R.string.s0552)
            }

            // 固件更新渠道
            val firmwareChannel = NotificationChannel(
                CHANNEL_FIRMWARE,
                Loc.s(R.string.s0553),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Loc.s(R.string.s0554)
            }

            // 网络告警渠道
            val networkChannel = NotificationChannel(
                CHANNEL_NETWORK,
                Loc.s(R.string.s0555),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Loc.s(R.string.s0556)
            }

            // 设备事件渠道
            val deviceChannel = NotificationChannel(
                CHANNEL_DEVICE,
                Loc.s(R.string.s0557),
                AndroidNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = Loc.s(R.string.s0558)
            }

            nm.createNotificationChannels(listOf(
                statusChannel, firmwareChannel, networkChannel, deviceChannel
            ))
        }
    }

    // ==================== 渠道开关 ====================

    private fun switchEnabled(key: String): Boolean = try {
        context?.getSharedPreferences(PREFS_SWITCHES, Context.MODE_PRIVATE)
            ?.getBoolean(key, true) ?: true
    } catch (e: Exception) {
        true
    }

    fun setRouterStatusEnabled(enabled: Boolean) = setSwitch(KEY_SWITCH_ROUTER_STATUS, enabled)
    fun setFirmwareEnabled(enabled: Boolean) = setSwitch(KEY_SWITCH_FIRMWARE, enabled)
    fun setNetworkAlertEnabled(enabled: Boolean) = setSwitch(KEY_SWITCH_NETWORK, enabled)
    fun setDeviceEventEnabled(enabled: Boolean) = setSwitch(KEY_SWITCH_DEVICE, enabled)

    fun isRouterStatusEnabled() = switchEnabled(KEY_SWITCH_ROUTER_STATUS)
    fun isFirmwareEnabled() = switchEnabled(KEY_SWITCH_FIRMWARE)
    fun isNetworkAlertEnabled() = switchEnabled(KEY_SWITCH_NETWORK)
    fun isDeviceEventEnabled() = switchEnabled(KEY_SWITCH_DEVICE)

    private fun setSwitch(key: String, enabled: Boolean) {
        try {
            context?.getSharedPreferences(PREFS_SWITCHES, Context.MODE_PRIVATE)
                ?.edit()?.putBoolean(key, enabled)?.apply()
        } catch (e: Exception) {
            // 忽略
        }
    }

    // ==================== 历史记录 ====================

    /**
     * 读取真实通知历史（新→旧）
     */
    fun getHistory(): List<NotificationRecord> {
        val ctx = context ?: return emptyList()
        return try {
            val json = ctx.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_HISTORY, null) ?: return emptyList()
            val type = object : TypeToken<List<NotificationRecord>>() {}.type
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, type) as? List<NotificationRecord> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 清空通知历史
     */
    fun clearHistory() {
        try {
            context?.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
                ?.edit()?.remove(KEY_HISTORY)?.apply()
        } catch (e: Exception) {
            // 忽略
        }
    }

    /** 写入一条历史记录（新记录在最前） */
    private fun record(title: String, content: String, type: String) {
        try {
            val ctx = context ?: return
            val current = getHistory().toMutableList()
            current.add(
                0,
                NotificationRecord(
                    id = System.currentTimeMillis(),
                    title = title,
                    content = content,
                    time = System.currentTimeMillis(),
                    type = type
                )
            )
            val trimmed = current.take(HISTORY_LIMIT)
            ctx.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HISTORY, gson.toJson(trimmed))
                .apply()
        } catch (e: Exception) {
            // 历史记录失败不影响通知本身
        }
    }

    // ==================== 通知 ====================

    /**
     * 显示路由器离线通知
     */
    fun showRouterOfflineNotification(routerName: String) {
        val ctx = context ?: return
        if (!isRouterStatusEnabled()) return

        val title = Loc.s(R.string.s0933)
        val text = Loc.s(R.string.s0934, routerName)
        record(title, text, TYPE_ROUTER_STATUS)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ROUTER_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_OFFLINE, notification)
        } catch (e: SecurityException) {
            // 通知权限未授予
        }
    }

    /**
     * 显示路由器在线通知
     */
    fun showRouterOnlineNotification(routerName: String) {
        val ctx = context ?: return
        if (!isRouterStatusEnabled()) return

        val title = Loc.s(R.string.s0935)
        val text = Loc.s(R.string.s0936, routerName)
        record(title, text, TYPE_ROUTER_STATUS)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ROUTER_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_OFFLINE, notification)
        } catch (e: SecurityException) {
            // 通知权限未授予
        }
    }

    /**
     * 显示固件更新通知
     */
    fun showFirmwareUpdateNotification(version: String, size: String) {
        val ctx = context ?: return
        if (!isFirmwareEnabled()) return

        val title = Loc.s(R.string.s0937)
        val text = Loc.s(R.string.s0938, version, size)
        record(title, text, TYPE_FIRMWARE)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate", "firmware")
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_FIRMWARE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_FIRMWARE, notification)
        } catch (e: SecurityException) {
            // 通知权限未授予
        }
    }

    /**
     * 显示新设备上线通知
     */
    fun showNewDeviceNotification(deviceName: String, deviceIp: String) {
        val ctx = context ?: return
        if (!isDeviceEventEnabled()) return

        val title = Loc.s(R.string.s0564)
        val text = Loc.s(R.string.s0940, deviceName, deviceIp)
        record(title, text, TYPE_DEVICE_EVENT)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate", "devices")
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_DEVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_NEW_DEVICE, notification)
        } catch (e: SecurityException) {
            // 通知权限未授予
        }
    }

    /**
     * 显示网络异常通知
     */
    fun showNetworkErrorNotification(message: String) {
        val ctx = context ?: return
        if (!isNetworkAlertEnabled()) return

        val title = Loc.s(R.string.s0941)
        val text = message
        record(title, text, TYPE_NETWORK_ALERT)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate", "diagnostic")
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_NETWORK)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_NETWORK_ERROR, notification)
        } catch (e: SecurityException) {
            // 通知权限未授予
        }
    }

    /**
     * 取消所有通知
     */
    fun cancelAll() {
        val ctx = context ?: return
        try {
            NotificationManagerCompat.from(ctx).cancelAll()
        } catch (e: Exception) {
            // 忽略
        }
    }

    // 历史记录类型标识（与 NotificationType 枚举名对应）
    const val TYPE_ROUTER_STATUS = "ROUTER_STATUS"
    const val TYPE_FIRMWARE = "FIRMWARE"
    const val TYPE_NETWORK_ALERT = "NETWORK_ALERT"
    const val TYPE_DEVICE_EVENT = "DEVICE_EVENT"
}
