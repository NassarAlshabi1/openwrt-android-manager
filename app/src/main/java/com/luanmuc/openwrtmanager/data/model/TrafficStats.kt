package com.luanmuc.openwrtmanager.data.model

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 流量统计数据
 */
data class TrafficStats(
    val totalRx: Long = 0,
    val totalTx: Long = 0,
    val todayRx: Long = 0,
    val todayTx: Long = 0,
    val weekRx: Long = 0,
    val weekTx: Long = 0,
    val monthRx: Long = 0,
    val monthTx: Long = 0
)

/**
 * 设备流量统计
 */
data class DeviceTraffic(
    val mac: String = "",
    val ip: String = "",
    val hostname: String = "",
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val totalBytes: Long = 0
)

/**
 * 流量历史记录点
 */
data class TrafficHistoryPoint(
    val timestamp: Long = 0,
    val rxBytes: Long = 0,
    val txBytes: Long = 0
)

/**
 * 流量统计周期
 */
enum class TrafficPeriod {
    TODAY,
    WEEK,
    MONTH,
    YEAR;
    val displayName: String
        get() = when (this) {
            TODAY -> Loc.s(R.string.enum_046)
            WEEK -> Loc.s(R.string.enum_047)
            MONTH -> Loc.s(R.string.enum_048)
            YEAR -> Loc.s(R.string.enum_049)
        }
}
