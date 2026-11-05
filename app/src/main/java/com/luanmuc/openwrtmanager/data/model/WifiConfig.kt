package com.luanmuc.openwrtmanager.data.model

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * WiFi定时配置
 */
data class WifiSchedule(
    val id: String = "",
    val name: String = "",
    val interfaceName: String = "wlan0",
    val enabled: Boolean = false,
    val startTime: String = "22:00",  // HH:mm
    val endTime: String = "06:00",    // HH:mm
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),  // 1-7 周一到周日
    val action: WifiScheduleAction = WifiScheduleAction.DISABLE
)

/**
 * 定时动作
 */
enum class WifiScheduleAction {
    DISABLE,
    ENABLE;
    val displayName: String
        get() = when (this) {
            DISABLE -> Loc.s(R.string.enum_050)
            ENABLE -> Loc.s(R.string.enum_051)
        }
}

/**
 * 访客网络配置
 */
data class GuestNetworkConfig(
    val enabled: Boolean = false,
    val ssid: String = "OpenWrt-Guest",
    val password: String = "",
    val interfaceName: String = "wlan0-1",
    val maxClients: Int = 10,
    val isolated: Boolean = true,  // 客户端隔离
    val duration: Int = 0,  // 有效时长，小时，0表示永久
    val createdAt: Long = 0
)

/**
 * WiFi信道信息
 */
data class WifiChannelInfo(
    val channel: Int = 0,
    val frequency: Int = 0,  // MHz
    val band: WifiBand = WifiBand.BAND_2G,
    val utilization: Int = 0,  // 利用率，%
    val noise: Int = 0,  // 噪声，dBm
    val activeStations: Int = 0  // 活跃站点数
)

/**
 * WiFi频段
 */
enum class WifiBand(val displayName: String, val frequencyRange: String) {
    BAND_2G("2.4G", "2400-2483 MHz"),
    BAND_5G("5G", "5150-5825 MHz"),
    BAND_6G("6G", "5925-7125 MHz")
}

/**
 * WiFi信号强度等级
 */
enum class WifiSignalLevel(val minDbm: Int) {
    EXCELLENT(-50),
    GOOD(-60),
    FAIR(-70),
    WEAK(-80),
    BAD(-100);
    val displayName: String
        get() = when (this) {
            EXCELLENT -> Loc.s(R.string.enum_042)
            GOOD -> Loc.s(R.string.enum_031)
            FAIR -> Loc.s(R.string.enum_043)
            WEAK -> Loc.s(R.string.enum_052)
            BAD -> Loc.s(R.string.enum_045)
        }
}
