package com.luanmuc.openwrtmanager.util

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.luanmuc.openwrtmanager.MainActivity
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 通知管理器
 * 负责管理所有APP通知
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
    
    private var context: Context? = null
    private var initialized = false
    
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
    
    /**
     * 显示路由器离线通知
     */
    fun showRouterOfflineNotification(routerName: String) {
        val ctx = context ?: return
        
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ROUTER_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(Loc.s(R.string.s0933))
            .setContentText(Loc.s(R.string.s0934, routerName))
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
        
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ROUTER_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(Loc.s(R.string.s0935))
            .setContentText(Loc.s(R.string.s0936, routerName))
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
            .setContentTitle(Loc.s(R.string.s0937))
            .setContentText(Loc.s(R.string.s0938, version, size))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(Loc.s(R.string.s0939, version, size)))
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
            .setContentTitle(Loc.s(R.string.s0564))
            .setContentText(Loc.s(R.string.s0940, deviceName, deviceIp))
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
            .setContentTitle(Loc.s(R.string.s0941))
            .setContentText(message)
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
}
