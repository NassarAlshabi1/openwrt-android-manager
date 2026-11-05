package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.luanmuc.openwrtmanager.data.model.GuestNetworkConfig
import com.luanmuc.openwrtmanager.data.model.WifiBand
import com.luanmuc.openwrtmanager.data.model.WifiChannelInfo
import com.luanmuc.openwrtmanager.data.model.WifiSchedule
import com.luanmuc.openwrtmanager.data.model.WifiScheduleAction
import com.luanmuc.openwrtmanager.util.DebugMode
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * WiFi配置Repository
 * 负责WiFi定时开关、访客网络、信道分析等功能
 */
class WifiConfigRepository private constructor(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("wifi_config", Context.MODE_PRIVATE)
    
    /**
     * 获取WiFi定时配置列表
     */
    fun getWifiSchedules(): List<WifiSchedule> {
        val json = prefs.getString("schedules", null) ?: return getDefaultSchedules()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val daysArr = obj.optJSONArray("days")
                val days = if (daysArr != null) {
                    List(daysArr.length()) { j -> daysArr.getInt(j) }
                } else {
                    listOf(1, 2, 3, 4, 5, 6, 7)
                }
                WifiSchedule(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", ""),
                    interfaceName = obj.optString("interfaceName", "wlan0"),
                    enabled = obj.optBoolean("enabled", false),
                    startTime = obj.optString("startTime", "22:00"),
                    endTime = obj.optString("endTime", "06:00"),
                    days = days,
                    action = WifiScheduleAction.valueOf(obj.optString("action", "DISABLE"))
                )
            }
        } catch (e: Exception) {
            getDefaultSchedules()
        }
    }
    
    /**
     * 获取默认定时配置
     */
    private fun getDefaultSchedules(): List<WifiSchedule> {
        return listOf(
            WifiSchedule(
                id = "night_mode",
                name = Loc.s(R.string.s0239),
                interfaceName = "wlan0",
                enabled = false,
                startTime = "22:00",
                endTime = "06:00",
                days = listOf(1, 2, 3, 4, 5, 6, 7),
                action = WifiScheduleAction.DISABLE
            )
        )
    }
    
    /**
     * 保存WiFi定时配置
     */
    fun saveWifiSchedules(schedules: List<WifiSchedule>) {
        val arr = JSONArray()
        for (schedule in schedules) {
            arr.put(JSONObject().apply {
                put("id", schedule.id)
                put("name", schedule.name)
                put("interfaceName", schedule.interfaceName)
                put("enabled", schedule.enabled)
                put("startTime", schedule.startTime)
                put("endTime", schedule.endTime)
                put("days", JSONArray(schedule.days))
                put("action", schedule.action.name)
            })
        }
        prefs.edit().putString("schedules", arr.toString()).apply()
    }
    
    /**
     * 获取访客网络配置
     */
    suspend fun getGuestNetworkConfig(): GuestNetworkConfig {
        if (DebugMode.isDebugMode) {
            return getFakeGuestConfig()
        }
        
        // 真实模式：从路由器获取
        return GuestNetworkConfig()
    }
    
    /**
     * 启用访客网络
     */
    suspend fun enableGuestNetwork(config: GuestNetworkConfig): Boolean {
        if (DebugMode.isDebugMode) {
            delay(1000)
            return true
        }
        
        return false
    }
    
    /**
     * 禁用访客网络
     */
    suspend fun disableGuestNetwork(): Boolean {
        if (DebugMode.isDebugMode) {
            delay(1000)
            return true
        }
        
        return false
    }
    
    /**
     * 扫描WiFi信道
     */
    suspend fun scanWifiChannels(band: WifiBand): List<WifiChannelInfo> {
        if (DebugMode.isDebugMode) {
            delay(2000)
            return getFakeChannelInfo(band)
        }
        
        return emptyList()
    }
    
    /**
     * 设置WiFi发射功率
     */
    suspend fun setTxPower(interfaceName: String, power: Int): Boolean {
        if (DebugMode.isDebugMode) {
            delay(500)
            return true
        }
        
        return false
    }
    
    /**
     * 设置WiFi信道
     */
    suspend fun setChannel(interfaceName: String, channel: Int): Boolean {
        if (DebugMode.isDebugMode) {
            delay(500)
            return true
        }
        
        return false
    }
    
    /**
     * 假数据：访客网络配置
     */
    private fun getFakeGuestConfig(): GuestNetworkConfig {
        return GuestNetworkConfig(
            enabled = false,
            ssid = "OpenWrt-Guest",
            password = "",
            interfaceName = "wlan0-1",
            maxClients = 10,
            isolated = true,
            duration = 0,
            createdAt = 0
        )
    }
    
    /**
     * 假数据：信道信息
     */
    private fun getFakeChannelInfo(band: WifiBand): List<WifiChannelInfo> {
        return if (band == WifiBand.BAND_2G) {
            listOf(
                WifiChannelInfo(1, 2412, WifiBand.BAND_2G, 30, -95, 2),
                WifiChannelInfo(2, 2417, WifiBand.BAND_2G, 25, -96, 1),
                WifiChannelInfo(3, 2422, WifiBand.BAND_2G, 20, -97, 0),
                WifiChannelInfo(4, 2427, WifiBand.BAND_2G, 35, -94, 3),
                WifiChannelInfo(5, 2432, WifiBand.BAND_2G, 40, -93, 4),
                WifiChannelInfo(6, 2437, WifiBand.BAND_2G, 60, -90, 8),
                WifiChannelInfo(7, 2442, WifiBand.BAND_2G, 45, -92, 5),
                WifiChannelInfo(8, 2447, WifiBand.BAND_2G, 30, -95, 2),
                WifiChannelInfo(9, 2452, WifiBand.BAND_2G, 25, -96, 1),
                WifiChannelInfo(10, 2457, WifiBand.BAND_2G, 20, -97, 0),
                WifiChannelInfo(11, 2462, WifiBand.BAND_2G, 50, -91, 6),
                WifiChannelInfo(12, 2467, WifiBand.BAND_2G, 15, -98, 0),
                WifiChannelInfo(13, 2472, WifiBand.BAND_2G, 10, -99, 0)
            )
        } else {
            listOf(
                WifiChannelInfo(36, 5180, WifiBand.BAND_5G, 20, -97, 1),
                WifiChannelInfo(40, 5200, WifiBand.BAND_5G, 15, -98, 0),
                WifiChannelInfo(44, 5220, WifiBand.BAND_5G, 25, -96, 2),
                WifiChannelInfo(48, 5240, WifiBand.BAND_5G, 30, -95, 3),
                WifiChannelInfo(149, 5745, WifiBand.BAND_5G, 40, -93, 4),
                WifiChannelInfo(153, 5765, WifiBand.BAND_5G, 35, -94, 3),
                WifiChannelInfo(157, 5785, WifiBand.BAND_5G, 45, -92, 5),
                WifiChannelInfo(161, 5805, WifiBand.BAND_5G, 30, -95, 2),
                WifiChannelInfo(165, 5825, WifiBand.BAND_5G, 20, -97, 1)
            )
        }
    }
    
    companion object {
        @Volatile
        private var instance: WifiConfigRepository? = null
        
        fun getInstance(context: Context): WifiConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: WifiConfigRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
