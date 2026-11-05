package com.luanmuc.openwrtmanager.util
import com.luanmuc.openwrtmanager.util.LogUtils

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 性能监控工具类
 * 提供性能监控和计时功能
 */
object PerformanceMonitor {
    
    private const val TAG = "PerformanceMonitor"
    
    // 计时器映射
    private val timers = ConcurrentHashMap<String, Long>()
    
    // 性能统计
    private val stats = ConcurrentHashMap<String, PerformanceStat>()
    
    // 是否启用
    var enabled = true
    
    /**
     * 性能统计数据类
     */
    data class PerformanceStat(
        val name: String,
        var count: Int = 0,
        var totalTime: Long = 0,
        var minTime: Long = Long.MAX_VALUE,
        var maxTime: Long = 0
    ) {
        val avgTime: Long
            get() = if (count > 0) totalTime / count else 0
    }
    
    /**
     * 开始计时
     */
    fun startTimer(name: String) {
        if (!enabled) return
        
        timers[name] = SystemClock.elapsedRealtime()
    }
    
    /**
     * 结束计时并返回耗时（毫秒）
     */
    fun stopTimer(name: String): Long {
        if (!enabled) return 0
        
        val startTime = timers.remove(name) ?: return 0
        val elapsed = SystemClock.elapsedRealtime() - startTime
        
        // 更新统计
        updateStats(name, elapsed)
        
        LogUtils.d(TAG, "[$name] 耗时: ${elapsed}ms")
        
        return elapsed
    }
    
    /**
     * 结束计时并记录日志
     */
    fun stopTimer(name: String, message: String): Long {
        val elapsed = stopTimer(name)
        if (enabled) {
            LogUtils.d(TAG, "[$name] $message - 耗时: ${elapsed}ms")
        }
        return elapsed
    }
    
    /**
     * 更新性能统计
     */
    private fun updateStats(name: String, elapsed: Long) {
        val stat = stats.getOrPut(name) { PerformanceStat(name) }
        stat.count++
        stat.totalTime += elapsed
        stat.minTime = minOf(stat.minTime, elapsed)
        stat.maxTime = maxOf(stat.maxTime, elapsed)
    }
    
    /**
     * 获取性能统计
     */
    fun getStats(name: String): PerformanceStat? {
        return stats[name]
    }
    
    /**
     * 获取所有性能统计
     */
    fun getAllStats(): Map<String, PerformanceStat> {
        return stats.toMap()
    }
    
    /**
     * 清除统计
     */
    fun clearStats(name: String) {
        stats.remove(name)
    }
    
    /**
     * 清除所有统计
     */
    fun clearAllStats() {
        stats.clear()
    }
    
    /**
     * 测量代码块执行时间
     */
    inline fun <T> measureTime(name: String, block: () -> T): T {
        if (!enabled) {
            return block()
        }
        
        startTimer(name)
        return try {
            block()
        } finally {
            stopTimer(name)
        }
    }
    
    /**
     * 测量代码块执行时间并返回结果和耗时
     */
    inline fun <T> measureTimeWithResult(name: String, block: () -> T): Pair<T, Long> {
        if (!enabled) {
            val result = block()
            return Pair(result, 0)
        }
        
        startTimer(name)
        val result = block()
        val elapsed = stopTimer(name)
        return Pair(result, elapsed)
    }
    
    /**
     * 检查是否超过阈值
     */
    fun checkThreshold(name: String, thresholdMs: Long): Boolean {
        val stat = stats[name] ?: return false
        return stat.avgTime > thresholdMs
    }
    
    /**
     * 获取慢操作列表（超过阈值的操作）
     */
    fun getSlowOperations(thresholdMs: Long): List<PerformanceStat> {
        return stats.values.filter { it.avgTime > thresholdMs }
            .sortedByDescending { it.avgTime }
    }
    
    /**
     * 打印性能报告
     */
    fun printReport() {
        if (!enabled) return
        
        LogUtils.d(TAG, "=== 性能监控报告 ===")
        
        if (stats.isEmpty()) {
            LogUtils.d(TAG, "暂无性能数据")
            return
        }
        
        val sortedStats = stats.values.sortedByDescending { it.totalTime }
        
        for (stat in sortedStats) {
            LogUtils.d(TAG, "${stat.name}:")
            LogUtils.d(TAG, "  调用次数: ${stat.count}")
            LogUtils.d(TAG, "  总耗时: ${stat.totalTime}ms")
            LogUtils.d(TAG, "  平均耗时: ${stat.avgTime}ms")
            LogUtils.d(TAG, "  最小耗时: ${stat.minTime}ms")
            LogUtils.d(TAG, "  最大耗时: ${stat.maxTime}ms")
        }
        
        LogUtils.d(TAG, "===================")
    }
    
    /**
     * 获取性能报告字符串
     */
    fun getReportString(): String {
        if (stats.isEmpty()) {
            return Loc.s(R.string.s0942)
        }
        
        val sb = StringBuilder()
        sb.append(Loc.s(R.string.s0943))
        
        val sortedStats = stats.values.sortedByDescending { it.totalTime }
        
        for (stat in sortedStats) {
            sb.append("${stat.name}:\n")
            sb.append(Loc.s(R.string.s0944, stat.count))
            sb.append(Loc.s(R.string.s0945, stat.totalTime))
            sb.append(Loc.s(R.string.s0946, stat.avgTime))
            sb.append(Loc.s(R.string.s0947, stat.minTime))
            sb.append(Loc.s(R.string.s0948, stat.maxTime))
        }
        
        sb.append("===================")
        
        return sb.toString()
    }
    
    /**
     * 内存使用情况
     */
    fun getMemoryUsage(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        return MemoryInfo(
            total = totalMemory,
            free = freeMemory,
            used = usedMemory,
            max = maxMemory,
            usedPercent = if (maxMemory > 0) (usedMemory * 100 / maxMemory).toInt() else 0
        )
    }
    
    /**
     * 内存信息数据类
     */
    data class MemoryInfo(
        val total: Long,
        val free: Long,
        val used: Long,
        val max: Long,
        val usedPercent: Int
    )
    
    /**
     * 打印内存使用情况
     */
    fun printMemoryUsage() {
        val memory = getMemoryUsage()
        LogUtils.d(TAG, "内存使用情况:")
        LogUtils.d(TAG, "  已用: ${FormatUtils.formatFileSize(memory.used)}")
        LogUtils.d(TAG, "  空闲: ${FormatUtils.formatFileSize(memory.free)}")
        LogUtils.d(TAG, "  总计: ${FormatUtils.formatFileSize(memory.total)}")
        LogUtils.d(TAG, "  最大: ${FormatUtils.formatFileSize(memory.max)}")
        LogUtils.d(TAG, "  使用率: ${memory.usedPercent}%")
    }
    
    /**
     * 触发GC（仅用于调试）
     */
    fun triggerGc() {
        if (!enabled) return
        
        LogUtils.d(TAG, "触发GC...")
        System.gc()
        System.runFinalization()
        System.gc()
        
        val memoryBefore = getMemoryUsage()
        LogUtils.d(TAG, "GC后内存使用: ${FormatUtils.formatFileSize(memoryBefore.used)} (${memoryBefore.usedPercent}%)")
    }
}