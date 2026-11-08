package com.luanmuc.openwrtmanager.service
import com.luanmuc.openwrtmanager.util.LogUtils

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.util.AppNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 路由器监控服务
 * 后台监控路由器状态，离线时发送通知
 */
class RouterMonitorService : Service() {
    
    companion object {
        private const val TAG = "RouterMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHECK_INTERVAL = 60 * 1000L // 60秒检查一次
        
        fun start(context: Context) {
            try {
                val intent = Intent(context, RouterMonitorService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                LogUtils.e(TAG, "启动服务失败", e)
            }
        }
        
        fun stop(context: Context) {
            try {
                val intent = Intent(context, RouterMonitorService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                LogUtils.e(TAG, "停止服务失败", e)
            }
        }
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var monitorJob: Job? = null
    private var lastOnlineState: Boolean = true
    private var isFirstCheck: Boolean = true
    
    override fun onCreate() {
        super.onCreate()
        LogUtils.d(TAG, "服务创建")
        
        // 创建前台通知
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        
        // 开始监控
        startMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.d(TAG, "服务启动")
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "服务销毁")
        stopMonitoring()
    }
    
    /**
     * 创建前台通知
     */
    private fun createForegroundNotification(): Notification {
        val channelId = AppNotificationManager.CHANNEL_ROUTER_STATUS
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(Loc.s(R.string.s0240))
            .setContentText(Loc.s(R.string.s0241))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    /**
     * 开始监控
     */
    private fun startMonitoring() {
        stopMonitoring()
        
        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    checkRouterStatus()
                } catch (e: Exception) {
                    LogUtils.e(TAG, "检查路由器状态失败", e)
                }
                delay(CHECK_INTERVAL)
            }
        }
    }
    
    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }
    
    /**
     * 检查路由器状态（真实 ubus 调用，失败即视为离线）
     */
    private suspend fun checkRouterStatus() {
        val luciRepository = LuciRepository.getInstance(this)
        val isOnline = try {
            luciRepository.getRouterStatus()
            true
        } catch (e: Exception) {
            false
        }

        // 状态变化时发送通知
        if (!isFirstCheck && isOnline != lastOnlineState) {
            if (isOnline) {
                sendOnlineNotification()
            } else {
                sendOfflineNotification()
            }
        }

        lastOnlineState = isOnline
        isFirstCheck = false
    }
    
    /**
     * 发送离线通知
     */
    private fun sendOfflineNotification() {
        handler.post {
            try {
                AppNotificationManager.showRouterOfflineNotification(
                    routerName = Loc.s(R.string.s0242)
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, "发送离线通知失败", e)
            }
        }
    }
    
    /**
     * 发送在线通知
     */
    private fun sendOnlineNotification() {
        handler.post {
            try {
                AppNotificationManager.showRouterOnlineNotification(
                    routerName = Loc.s(R.string.s0242)
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, "发送在线通知失败", e)
            }
        }
    }
}