package com.luanmuc.openwrtmanager.service
import com.luanmuc.openwrtmanager.util.LogUtils

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.luanmuc.openwrtmanager.widget.NetworkSpeedWidget
import com.luanmuc.openwrtmanager.widget.RouterStatusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Widget更新服务
 * 定期更新桌面小部件
 */
class WidgetUpdateService : Service() {
    
    companion object {
        private const val TAG = "WidgetUpdateService"
        private const val UPDATE_INTERVAL = 60 * 1000L // 60秒更新一次
        
        fun start(context: Context) {
            try {
                val intent = Intent(context, WidgetUpdateService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                LogUtils.e(TAG, "启动服务失败", e)
            }
        }
        
        fun stop(context: Context) {
            try {
                val intent = Intent(context, WidgetUpdateService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                LogUtils.e(TAG, "停止服务失败", e)
            }
        }
    }
    
    private var updateJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        LogUtils.d(TAG, "服务创建")
        startUpdates()
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
        stopUpdates()
    }
    
    /**
     * 开始定期更新
     */
    private fun startUpdates() {
        stopUpdates()
        
        updateJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    updateAllWidgets()
                } catch (e: Exception) {
                    LogUtils.e(TAG, "更新Widget失败", e)
                }
                delay(UPDATE_INTERVAL)
            }
        }
    }
    
    /**
     * 停止定期更新
     */
    private fun stopUpdates() {
        updateJob?.cancel()
        updateJob = null
    }
    
    /**
     * 更新所有Widget
     */
    private suspend fun updateAllWidgets() {
        try {
            LogUtils.d(TAG, "更新所有Widget")
            
            val appWidgetManager = AppWidgetManager.getInstance(this)
            
            // 更新路由器状态小部件
            updateRouterStatusWidget(appWidgetManager)
            
            // 更新网速小部件
            updateNetworkSpeedWidget(appWidgetManager)
            
        } catch (e: Exception) {
            LogUtils.e(TAG, "更新所有Widget失败", e)
        }
    }
    
    /**
     * 更新路由器状态小部件
     */
    private fun updateRouterStatusWidget(appWidgetManager: AppWidgetManager) {
        try {
            val componentName = ComponentName(this, RouterStatusWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                LogUtils.d(TAG, "更新路由器状态Widget，数量: ${appWidgetIds.size}")
                
                val intent = Intent(this, RouterStatusWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(intent)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "更新路由器状态Widget失败", e)
        }
    }
    
    /**
     * 更新网速小部件
     */
    private fun updateNetworkSpeedWidget(appWidgetManager: AppWidgetManager) {
        try {
            val componentName = ComponentName(this, NetworkSpeedWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                LogUtils.d(TAG, "更新网速Widget，数量: ${appWidgetIds.size}")
                
                val intent = Intent(this, NetworkSpeedWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(intent)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "更新网速Widget失败", e)
        }
    }
}