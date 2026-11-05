package com.luanmuc.openwrtmanager.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 格式化工具类
 * 提供数字、日期、文件大小等格式化功能
 */
object FormatUtils {
    
    // 数字格式化
    private val decimalFormat = DecimalFormat("#.##")
    private val integerFormat = DecimalFormat("#,###")
    
    // 日期格式化
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${decimalFormat.format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${decimalFormat.format(bytes / (1024.0 * 1024))} MB"
            else -> "${decimalFormat.format(bytes / (1024.0 * 1024 * 1024))} GB"
        }
    }
    
    /**
     * 格式化网速（字节/秒）
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
            bytesPerSecond < 1024 * 1024 -> "${decimalFormat.format(bytesPerSecond / 1024.0)} KB/s"
            else -> "${decimalFormat.format(bytesPerSecond / (1024.0 * 1024))} MB/s"
        }
    }
    
    /**
     * 格式化流量（字节）
     */
    fun formatTraffic(bytes: Long): String {
        return formatFileSize(bytes)
    }
    
    /**
     * 格式化运行时间（秒）
     */
    fun formatUptime(seconds: Long): String {
        val days = TimeUnit.SECONDS.toDays(seconds)
        val hours = TimeUnit.SECONDS.toHours(seconds) % 24
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        
        return when {
            days > 0 -> Loc.s(R.string.s0847, days, hours, minutes)
            hours > 0 -> Loc.s(R.string.s0522, hours, minutes)
            else -> Loc.s(R.string.s0523, minutes)
        }
    }
    
    /**
     * 格式化百分比
     */
    fun formatPercent(value: Float): String {
        return "${decimalFormat.format(value)}%"
    }
    
    /**
     * 格式化百分比（整数）
     */
    fun formatPercentInt(value: Int): String {
        return "$value%"
    }
    
    /**
     * 格式化日期时间
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化日期
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化数字（千分位）
     */
    fun formatNumber(number: Long): String {
        return integerFormat.format(number)
    }
    
    /**
     * 格式化数字（保留两位小数）
     */
    fun formatDecimal(number: Double): String {
        return decimalFormat.format(number)
    }
    
    /**
     * 格式化内存大小
     */
    fun formatMemory(kb: Long): String {
        return formatFileSize(kb * 1024)
    }
    
    /**
     * 相对时间格式化
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> Loc.s(R.string.s0841)
            minutes < 60 -> Loc.s(R.string.s0842, minutes)
            hours < 24 -> Loc.s(R.string.s0843, hours)
            days < 7 -> Loc.s(R.string.s0844, days)
            else -> formatDate(timestamp)
        }
    }
    
    /**
     * 格式化MAC地址
     */
    fun formatMacAddress(mac: String): String {
        return mac.uppercase(Locale.getDefault())
    }
    
    /**
     * 格式化IP地址
     */
    fun formatIpAddress(ip: String): String {
        return ip
    }
}