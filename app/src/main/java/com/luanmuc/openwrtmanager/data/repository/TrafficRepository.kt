package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.luanmuc.openwrtmanager.data.model.DeviceTraffic
import com.luanmuc.openwrtmanager.data.model.TrafficHistoryPoint
import com.luanmuc.openwrtmanager.data.model.TrafficStats
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * 流量统计Repository
 * 负责流量统计、历史记录、设备流量排行
 */
class TrafficRepository private constructor(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("traffic_stats", Context.MODE_PRIVATE)
    
    // 流量历史记录（按路由器存储）
    private val historyCache = ConcurrentHashMap<String, MutableList<TrafficHistoryPoint>>()
    
    // 设备流量统计
    private val deviceTrafficCache = ConcurrentHashMap<String, MutableMap<String, DeviceTraffic>>()
    
    // 上次记录的流量值（用于计算增量）
    private val lastRxBytes = ConcurrentHashMap<String, Long>()
    private val lastTxBytes = ConcurrentHashMap<String, Long>()
    
    /**
     * 获取流量统计数据
     */
    suspend fun getTrafficStats(routerId: String): TrafficStats {
        
        // 真实模式：从历史记录计算
        val history = getTrafficHistory(routerId)
        val now = System.currentTimeMillis()
        
        var todayRx = 0L
        var todayTx = 0L
        var weekRx = 0L
        var weekTx = 0L
        var monthRx = 0L
        var monthTx = 0L
        var totalRx = 0L
        var totalTx = 0L
        
        val dayStart = now - 24 * 60 * 60 * 1000
        val weekStart = now - 7 * 24 * 60 * 60 * 1000
        val monthStart = now - 30 * 24 * 60 * 60 * 1000
        
        for (point in history) {
            totalRx += point.rxBytes
            totalTx += point.txBytes
            
            if (point.timestamp > dayStart) {
                todayRx += point.rxBytes
                todayTx += point.txBytes
            }
            if (point.timestamp > weekStart) {
                weekRx += point.rxBytes
                weekTx += point.txBytes
            }
            if (point.timestamp > monthStart) {
                monthRx += point.rxBytes
                monthTx += point.txBytes
            }
        }
        
        return TrafficStats(
            totalRx = totalRx,
            totalTx = totalTx,
            todayRx = todayRx,
            todayTx = todayTx,
            weekRx = weekRx,
            weekTx = weekTx,
            monthRx = monthRx,
            monthTx = monthTx
        )
    }
    
    /**
     * 获取流量历史记录
     */
    fun getTrafficHistory(routerId: String): List<TrafficHistoryPoint> {
        return historyCache.getOrPut(routerId) { mutableListOf() }.toList()
    }
    
    /**
     * 记录流量数据点
     */
    fun recordTrafficPoint(routerId: String, rxBytes: Long, txBytes: Long) {
        val history = historyCache.getOrPut(routerId) { mutableListOf() }
        
        // 计算增量
        val lastRx = lastRxBytes[routerId] ?: rxBytes
        val lastTx = lastTxBytes[routerId] ?: txBytes
        
        val rxDelta = max(0, rxBytes - lastRx)
        val txDelta = max(0, txBytes - lastTx)
        
        lastRxBytes[routerId] = rxBytes
        lastTxBytes[routerId] = txBytes
        
        // 添加历史记录点
        history.add(
            TrafficHistoryPoint(
                timestamp = System.currentTimeMillis(),
                rxBytes = rxDelta,
                txBytes = txDelta
            )
        )
        
        // 只保留最近1000个点
        if (history.size > 1000) {
            history.removeAt(0)
        }
    }
    
    /**
     * 获取设备流量排行
     */
    suspend fun getDeviceTrafficRanking(routerId: String): List<DeviceTraffic> {
        
        val devices = deviceTrafficCache[routerId]?.values?.toList() ?: emptyList()
        return devices.sortedByDescending { it.totalBytes }
    }
    
    /**
     * 更新设备流量
     */
    fun updateDeviceTraffic(routerId: String, devices: List<DeviceTraffic>) {
        val map = deviceTrafficCache.getOrPut(routerId) { mutableMapOf() }
        for (device in devices) {
            map[device.mac] = device
        }
    }
    
    /**
     * 假数据：流量统计
     */
    private fun getFakeTrafficStats(): TrafficStats {
        return TrafficStats(
            totalRx = 15L * 1024 * 1024 * 1024,  // 15GB
            totalTx = 8L * 1024 * 1024 * 1024,    // 8GB
            todayRx = 500L * 1024 * 1024,         // 500MB
            todayTx = 300L * 1024 * 1024,         // 300MB
            weekRx = 3L * 1024 * 1024 * 1024,     // 3GB
            weekTx = 2L * 1024 * 1024 * 1024,     // 2GB
            monthRx = 12L * 1024 * 1024 * 1024,   // 12GB
            monthTx = 6L * 1024 * 1024 * 1024     // 6GB
        )
    }
    
    /**
     * 假数据：设备流量排行
     */
    private fun getFakeDeviceTraffic(): List<DeviceTraffic> {
        return listOf(
            DeviceTraffic(
                mac = "AA:BB:CC:DD:EE:01",
                ip = "192.168.1.100",
                hostname = "iPhone-15-Pro",
                rxBytes = 2L * 1024 * 1024 * 1024,
                txBytes = 500L * 1024 * 1024,
                totalBytes = 2500L * 1024 * 1024
            ),
            DeviceTraffic(
                mac = "AA:BB:CC:DD:EE:02",
                ip = "192.168.1.101",
                hostname = "MacBook-Pro",
                rxBytes = 1500L * 1024 * 1024,
                txBytes = 800L * 1024 * 1024,
                totalBytes = 2300L * 1024 * 1024
            ),
            DeviceTraffic(
                mac = "AA:BB:CC:DD:EE:03",
                ip = "192.168.1.102",
                hostname = "Mi-14-Ultra",
                rxBytes = 800L * 1024 * 1024,
                txBytes = 200L * 1024 * 1024,
                totalBytes = 1000L * 1024 * 1024
            ),
            DeviceTraffic(
                mac = "AA:BB:CC:DD:EE:04",
                ip = "192.168.1.103",
                hostname = "Smart-TV",
                rxBytes = 500L * 1024 * 1024,
                txBytes = 50L * 1024 * 1024,
                totalBytes = 550L * 1024 * 1024
            ),
            DeviceTraffic(
                mac = "AA:BB:CC:DD:EE:05",
                ip = "192.168.1.104",
                hostname = "iPad-Pro",
                rxBytes = 300L * 1024 * 1024,
                txBytes = 100L * 1024 * 1024,
                totalBytes = 400L * 1024 * 1024
            )
        )
    }
    
    companion object {
        @Volatile
        private var instance: TrafficRepository? = null
        
        fun getInstance(context: Context): TrafficRepository {
            return instance ?: synchronized(this) {
                instance ?: TrafficRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
