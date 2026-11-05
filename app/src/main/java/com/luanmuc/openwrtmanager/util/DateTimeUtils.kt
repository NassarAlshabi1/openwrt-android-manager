package com.luanmuc.openwrtmanager.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 日期时间工具类
 * 提供日期时间格式化和计算功能
 */
object DateTimeUtils {
    
    // 常用日期格式
    const val FORMAT_YYYY_MM_DD = "yyyy-MM-dd"
    const val FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss"
    const val FORMAT_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm"
    const val FORMAT_HH_MM_SS = "HH:mm:ss"
    const val FORMAT_HH_MM = "HH:mm"
    const val FORMAT_MM_DD = "MM-dd"
    const val FORMAT_MM_DD_HH_MM = "MM-dd HH:mm"
    
    /**
     * 格式化日期
     */
    fun formatDate(date: Date, pattern: String = FORMAT_YYYY_MM_DD_HH_MM_SS): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 格式化时间戳
     */
    fun formatTimestamp(timestamp: Long, pattern: String = FORMAT_YYYY_MM_DD_HH_MM_SS): String {
        return formatDate(Date(timestamp), pattern)
    }
    
    /**
     * 解析日期字符串
     */
    fun parseDate(dateString: String, pattern: String = FORMAT_YYYY_MM_DD_HH_MM_SS): Date? {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取当前时间字符串
     */
    fun getCurrentTimeString(pattern: String = FORMAT_YYYY_MM_DD_HH_MM_SS): String {
        return formatDate(Date(), pattern)
    }
    
    /**
     * 获取当前时间戳
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * 格式化相对时间
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 0 -> Loc.s(R.string.s0840)
            diff < TimeUnit.MINUTES.toMillis(1) -> Loc.s(R.string.s0841)
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                Loc.s(R.string.s0842, minutes)
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                Loc.s(R.string.s0843, hours)
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                Loc.s(R.string.s0844, days)
            }
            else -> formatTimestamp(timestamp, FORMAT_YYYY_MM_DD)
        }
    }
    
    /**
     * 格式化相对时间（简短版）
     */
    fun formatRelativeTimeShort(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 0 -> Loc.s(R.string.s0840)
            diff < TimeUnit.MINUTES.toMillis(1) -> Loc.s(R.string.s0841)
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                Loc.s(R.string.s0842, minutes)
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                Loc.s(R.string.s0843, hours)
            }
            isToday(timestamp) -> Loc.s(R.string.s0845)
            isYesterday(timestamp) -> Loc.s(R.string.s0846)
            isThisYear(timestamp) -> formatTimestamp(timestamp, FORMAT_MM_DD)
            else -> formatTimestamp(timestamp, FORMAT_YYYY_MM_DD)
        }
    }
    
    /**
     * 格式化持续时间
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val hours = TimeUnit.MINUTES.toHours(minutes)
        val days = TimeUnit.HOURS.toDays(hours)
        
        return when {
            days > 0 -> Loc.s(R.string.s0847, days, hours % 24, minutes % 60)
            hours > 0 -> Loc.s(R.string.s0848, hours, minutes % 60, seconds % 60)
            minutes > 0 -> Loc.s(R.string.s0849, minutes, seconds % 60)
            else -> Loc.s(R.string.s0644, seconds)
        }
    }
    
    /**
     * 格式化持续时间（简短版）
     */
    fun formatDurationShort(durationMs: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val hours = TimeUnit.MINUTES.toHours(minutes)
        val days = TimeUnit.HOURS.toDays(hours)
        
        return when {
            days > 0 -> Loc.s(R.string.s0521, days, hours % 24)
            hours > 0 -> Loc.s(R.string.s0739, hours, minutes % 60)
            minutes > 0 -> Loc.s(R.string.s0850, minutes, seconds % 60)
            else -> Loc.s(R.string.s0644, seconds)
        }
    }
    
    /**
     * 格式化运行时间（天:时:分:秒）
     */
    fun formatUptime(uptimeSeconds: Long): String {
        val days = uptimeSeconds / 86400
        val hours = (uptimeSeconds % 86400) / 3600
        val minutes = (uptimeSeconds % 3600) / 60
        val seconds = uptimeSeconds % 60
        
        return when {
            days > 0 -> String.format(Loc.s(R.string.s0851), days, hours, minutes, seconds)
            else -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
    
    /**
     * 检查是否是今天
     */
    fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.timeInMillis = timestamp
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * 检查是否是昨天
     */
    fun isYesterday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.timeInMillis = timestamp
        cal2.add(Calendar.DAY_OF_YEAR, -1)
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * 检查是否是今年
     */
    fun isThisYear(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.timeInMillis = timestamp
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }
    
    /**
     * 检查是否是工作日
     */
    fun isWeekday(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    }
    
    /**
     * 检查是否是周末
     */
    fun isWeekend(timestamp: Long): Boolean {
        return !isWeekday(timestamp)
    }
    
    /**
     * 获取星期几
     */
    fun getDayOfWeek(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> Loc.s(R.string.s0852)
            Calendar.MONDAY -> Loc.s(R.string.s0853)
            Calendar.TUESDAY -> Loc.s(R.string.s0854)
            Calendar.WEDNESDAY -> Loc.s(R.string.s0855)
            Calendar.THURSDAY -> Loc.s(R.string.s0856)
            Calendar.FRIDAY -> Loc.s(R.string.s0857)
            Calendar.SATURDAY -> Loc.s(R.string.s0858)
            else -> Loc.s(R.string.s0050)
        }
    }
    
    /**
     * 获取星期几（简短）
     */
    fun getDayOfWeekShort(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> Loc.s(R.string.s0859)
            Calendar.MONDAY -> Loc.s(R.string.s0860)
            Calendar.TUESDAY -> Loc.s(R.string.s0861)
            Calendar.WEDNESDAY -> Loc.s(R.string.s0862)
            Calendar.THURSDAY -> Loc.s(R.string.s0863)
            Calendar.FRIDAY -> Loc.s(R.string.s0864)
            Calendar.SATURDAY -> Loc.s(R.string.s0865)
            else -> Loc.s(R.string.s0050)
        }
    }
    
    /**
     * 获取今天开始的时间戳
     */
    fun getTodayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    /**
     * 获取今天结束的时间戳
     */
    fun getTodayEnd(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
    
    /**
     * 获取本周开始的时间戳
     */
    fun getWeekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    /**
     * 获取本月开始的时间戳
     */
    fun getMonthStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    /**
     * 计算两个日期之间的天数差
     */
    fun daysBetween(date1: Long, date2: Long): Long {
        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = date1
        cal1.set(Calendar.HOUR_OF_DAY, 0)
        cal1.set(Calendar.MINUTE, 0)
        cal1.set(Calendar.SECOND, 0)
        cal1.set(Calendar.MILLISECOND, 0)
        
        val cal2 = Calendar.getInstance()
        cal2.timeInMillis = date2
        cal2.set(Calendar.HOUR_OF_DAY, 0)
        cal2.set(Calendar.MINUTE, 0)
        cal2.set(Calendar.SECOND, 0)
        cal2.set(Calendar.MILLISECOND, 0)
        
        val diff = Math.abs(cal2.timeInMillis - cal1.timeInMillis)
        return TimeUnit.MILLISECONDS.toDays(diff)
    }
    
    /**
     * 添加天数
     */
    fun addDays(timestamp: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }
    
    /**
     * 添加小时
     */
    fun addHours(timestamp: Long, hours: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.add(Calendar.HOUR_OF_DAY, hours)
        return cal.timeInMillis
    }
    
    /**
     * 添加分钟
     */
    fun addMinutes(timestamp: Long, minutes: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.add(Calendar.MINUTE, minutes)
        return cal.timeInMillis
    }
    
    /**
     * 获取年龄
     */
    fun getAge(birthDate: Long): Int {
        val birthCal = Calendar.getInstance()
        birthCal.timeInMillis = birthDate
        
        val nowCal = Calendar.getInstance()
        
        var age = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
        
        if (nowCal.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        
        return age
    }
    
    /**
     * 格式化倒计时
     */
    fun formatCountdown(remainingMs: Long): String {
        if (remainingMs <= 0) return "00:00"
        
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 获取时间段描述
     */
    fun getTimeOfDayDescription(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 5..11 -> Loc.s(R.string.s0866)
            in 12..13 -> Loc.s(R.string.s0867)
            in 14..17 -> Loc.s(R.string.s0868)
            in 18..22 -> Loc.s(R.string.s0869)
            else -> Loc.s(R.string.s0870)
        }
    }
}