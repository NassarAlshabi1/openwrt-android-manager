package com.luanmuc.openwrtmanager.service
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.data.repository.TrafficRepository
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.util.LogUtils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 流量统计服务
 * 后台持续从路由器读取 WAN 接口计数器并记录流量数据
 */
class TrafficStatsService : Service() {

    companion object {
        private const val TAG = "TrafficStatsService"
        private const val STATS_INTERVAL = 30 * 1000L // 30秒统计一次

        fun start(context: Context) {
            try {
                val intent = Intent(context, TrafficStatsService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                LogUtils.e(TAG, "启动服务失败", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, TrafficStatsService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                LogUtils.e(TAG, "停止服务失败", e)
            }
        }
    }

    private var statsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        LogUtils.d(TAG, "服务创建")
        startStats()
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
        stopStats()
    }

    /**
     * 开始流量统计
     */
    private fun startStats() {
        stopStats()

        statsJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    recordTrafficPoint()
                } catch (e: Exception) {
                    LogUtils.e(TAG, "记录流量数据失败", e)
                }
                delay(STATS_INTERVAL)
            }
        }
    }

    /**
     * 停止流量统计
     */
    private fun stopStats() {
        statsJob?.cancel()
        statsJob = null
    }

    /**
     * 记录流量数据点：从路由器读取 WAN 接口的 rx/tx 计数器，
     * 交给 TrafficRepository 计算增量并保存历史。
     */
    private suspend fun recordTrafficPoint() {
        try {
            val routerRepository = RouterRepository.getInstance(this)
            val luciRepository = LuciRepository.getInstance(this)
            val trafficRepository = TrafficRepository.getInstance(this)

            // 只在有已配置路由器且已登录时记录
            val routers = routerRepository.getRoutersList()
            val activeId = routerRepository.getActiveRouterId()
            val activeRouter = (routers.find { it.id == activeId } ?: routers.firstOrNull()) ?: return

            if (!luciRepository.isLoggedIn()) {
                val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                luciRepository.login(activeRouter.address, activeRouter.username, password)
            }

            val wan = luciRepository.getWanStatus() ?: return
            trafficRepository.recordTrafficPoint(
                routerId = activeRouter.id,
                rxBytes = wan.rxBytes,
                txBytes = wan.txBytes
            )
            LogUtils.d(TAG, "流量记录: rx=${wan.rxBytes}, tx=${wan.txBytes}")
        } catch (e: Exception) {
            LogUtils.e(TAG, "记录流量数据点失败", e)
        }
    }
}
