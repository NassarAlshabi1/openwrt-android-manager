package com.luanmuc.openwrtmanager.data.model

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 设备备注信息
 */
data class DeviceNote(
    val mac: String = "",
    val noteName: String = "",
    val group: String = Loc.s(R.string.s0002),
    val tags: List<String> = emptyList(),
    val isBlocked: Boolean = false,
    val speedLimit: Int = 0,  // 0表示不限速，单位KB/s
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/**
 * 设备分组
 */
data class DeviceGroup(
    val id: String = "",
    val name: String = "",
    val deviceCount: Int = 0,
    val color: String = "#1677FF"
)

/**
 * 设备上下线历史记录
 */
data class DeviceHistory(
    val mac: String = "",
    val ip: String = "",
    val hostname: String = "",
    val eventType: DeviceEventType = DeviceEventType.ONLINE,
    val timestamp: Long = 0
)

/**
 * 设备事件类型
 */
enum class DeviceEventType {
    ONLINE,
    OFFLINE,
    BLOCKED,
    UNBLOCKED,
    SPEED_LIMITED,
    SPEED_UNLIMITED;
    val displayName: String
        get() = when (this) {
            ONLINE -> Loc.s(R.string.enum_060)
            OFFLINE -> Loc.s(R.string.enum_061)
            BLOCKED -> Loc.s(R.string.enum_062)
            UNBLOCKED -> Loc.s(R.string.enum_063)
            SPEED_LIMITED -> Loc.s(R.string.enum_064)
            SPEED_UNLIMITED -> Loc.s(R.string.enum_065)
        }
}

/**
 * 设备限速配置
 */
data class SpeedLimitConfig(
    val mac: String = "",
    val uploadLimit: Int = 0,  // 上传限速，KB/s，0表示不限速
    val downloadLimit: Int = 0,  // 下载限速，KB/s，0表示不限速
    val enabled: Boolean = false
)
