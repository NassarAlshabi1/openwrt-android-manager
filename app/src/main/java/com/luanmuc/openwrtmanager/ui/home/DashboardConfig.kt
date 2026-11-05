package com.luanmuc.openwrtmanager.ui.home

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 首页仪表板配置
 * 支持卡片排序、显示/隐藏、大小调整
 * 
 * 特性：
 * - 长按进入编辑模式
 * - 拖拽排序
 * - 显示/隐藏卡片
 * - 卡片大小调整
 * - 预设布局切换
 */
class DashboardConfig(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
    
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<List<DashboardCard>> = _config.asStateFlow()
    
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    /**
     * 保存配置
     */
    fun saveConfig(cards: List<DashboardCard>) {
        val sortedCards = cards.sortedBy { it.order }
        saveConfigToPrefs(sortedCards)
        _config.value = sortedCards
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        val defaultConfig = getDefaultConfig()
        saveConfigToPrefs(defaultConfig)
        _config.value = defaultConfig
    }
    
    /**
     * 切换编辑模式
     */
    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }
    
    /**
     * 退出编辑模式
     */
    fun exitEditMode() {
        _isEditMode.value = false
    }
    
    /**
     * 切换卡片可见性
     */
    fun toggleCardVisibility(cardId: CardType) {
        val currentConfig = _config.value.toMutableList()
        val index = currentConfig.indexOfFirst { it.id == cardId }
        if (index >= 0) {
            val card = currentConfig[index]
            currentConfig[index] = card.copy(visible = !card.visible)
            saveConfig(currentConfig)
        }
    }
    
    /**
     * 调整卡片大小
     */
    fun changeCardSize(cardId: CardType, newSize: CardSize) {
        val currentConfig = _config.value.toMutableList()
        val index = currentConfig.indexOfFirst { it.id == cardId }
        if (index >= 0) {
            val card = currentConfig[index]
            currentConfig[index] = card.copy(size = newSize)
            saveConfig(currentConfig)
        }
    }
    
    /**
     * 移动卡片位置
     */
    fun moveCard(fromPosition: Int, toPosition: Int) {
        val currentConfig = _config.value.toMutableList()
        if (fromPosition in currentConfig.indices && toPosition in currentConfig.indices) {
            val card = currentConfig.removeAt(fromPosition)
            currentConfig.add(toPosition, card)
            // 更新order
            val updatedCards = currentConfig.mapIndexed { index, dashboardCard ->
                dashboardCard.copy(order = index)
            }
            saveConfig(updatedCards)
        }
    }
    
    /**
     * 应用预设布局
     */
    fun applyPresetLayout(preset: PresetLayout) {
        val presetConfig = getPresetConfig(preset)
        saveConfig(presetConfig)
    }
    
    /**
     * 从SharedPreferences加载配置
     */
    private fun loadConfig(): List<DashboardCard> {
        val jsonString = prefs.getString("dashboard_config", null)
        return if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                val cards = mutableListOf<DashboardCard>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    cards.add(
                        DashboardCard(
                            id = CardType.valueOf(jsonObject.getString("id")),
                            visible = jsonObject.getBoolean("visible"),
                            size = CardSize.valueOf(jsonObject.getString("size")),
                            order = jsonObject.getInt("order")
                        )
                    )
                }
                cards.sortedBy { it.order }
            } catch (e: Exception) {
                getDefaultConfig()
            }
        } else {
            getDefaultConfig()
        }
    }
    
    /**
     * 保存配置到SharedPreferences
     */
    private fun saveConfigToPrefs(cards: List<DashboardCard>) {
        try {
            val jsonArray = JSONArray()
            cards.forEach { card ->
                val jsonObject = JSONObject()
                jsonObject.put("id", card.id.name)
                jsonObject.put("visible", card.visible)
                jsonObject.put("size", card.size.name)
                jsonObject.put("order", card.order)
                jsonArray.put(jsonObject)
            }
            prefs.edit().putString("dashboard_config", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取默认配置
     */
    private fun getDefaultConfig(): List<DashboardCard> {
        return listOf(
            DashboardCard(
                id = CardType.ROUTER_STATUS,
                visible = true,
                size = CardSize.LARGE,
                order = 0
            ),
            DashboardCard(
                id = CardType.NETWORK_SPEED,
                visible = true,
                size = CardSize.MEDIUM,
                order = 1
            ),
            DashboardCard(
                id = CardType.QUICK_ACTIONS,
                visible = true,
                size = CardSize.LARGE,
                order = 2
            ),
            DashboardCard(
                id = CardType.SYSTEM_STATUS,
                visible = true,
                size = CardSize.MEDIUM,
                order = 3
            ),
            DashboardCard(
                id = CardType.ONLINE_DEVICES,
                visible = true,
                size = CardSize.MEDIUM,
                order = 4
            ),
            DashboardCard(
                id = CardType.WIFI_STATUS,
                visible = true,
                size = CardSize.MEDIUM,
                order = 5
            ),
            DashboardCard(
                id = CardType.PORT_STATUS,
                visible = true,
                size = CardSize.MEDIUM,
                order = 6
            )
        )
    }
    
    /**
     * 获取预设布局配置
     */
    private fun getPresetConfig(preset: PresetLayout): List<DashboardCard> {
        return when (preset) {
            PresetLayout.DEFAULT -> getDefaultConfig()
            
            PresetLayout.COMPACT -> listOf(
                DashboardCard(CardType.ROUTER_STATUS, true, CardSize.MEDIUM, 0),
                DashboardCard(CardType.NETWORK_SPEED, true, CardSize.MEDIUM, 1),
                DashboardCard(CardType.QUICK_ACTIONS, true, CardSize.MEDIUM, 2),
                DashboardCard(CardType.SYSTEM_STATUS, true, CardSize.SMALL, 3),
                DashboardCard(CardType.ONLINE_DEVICES, true, CardSize.SMALL, 4)
            )
            
            PresetLayout.DETAILED -> listOf(
                DashboardCard(CardType.ROUTER_STATUS, true, CardSize.LARGE, 0),
                DashboardCard(CardType.NETWORK_SPEED, true, CardSize.LARGE, 1),
                DashboardCard(CardType.QUICK_ACTIONS, true, CardSize.LARGE, 2),
                DashboardCard(CardType.SYSTEM_STATUS, true, CardSize.MEDIUM, 3),
                DashboardCard(CardType.ONLINE_DEVICES, true, CardSize.MEDIUM, 4),
                DashboardCard(CardType.WIFI_STATUS, true, CardSize.MEDIUM, 5),
                DashboardCard(CardType.PLUGINS, true, CardSize.MEDIUM, 6),
                DashboardCard(CardType.FIREWALL, true, CardSize.SMALL, 7),
                DashboardCard(CardType.DDNS, true, CardSize.SMALL, 8)
            )
            
            PresetLayout.MINIMAL -> listOf(
                DashboardCard(CardType.ROUTER_STATUS, true, CardSize.MEDIUM, 0),
                DashboardCard(CardType.NETWORK_SPEED, true, CardSize.MEDIUM, 1),
                DashboardCard(CardType.QUICK_ACTIONS, true, CardSize.MEDIUM, 2)
            )
        }
    }
    
    companion object {
        @Volatile
        private var instance: DashboardConfig? = null
        
        fun getInstance(context: Context): DashboardConfig {
            return instance ?: synchronized(this) {
                instance ?: DashboardConfig(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 仪表板卡片配置
 */
data class DashboardCard(
    val id: CardType,
    val visible: Boolean,
    val size: CardSize,
    val order: Int
)

/**
 * 卡片类型
 */
enum class CardType {
    ROUTER_STATUS,
    NETWORK_SPEED,
    QUICK_ACTIONS,
    SYSTEM_STATUS,
    ONLINE_DEVICES,
    WIFI_STATUS,
    PLUGINS,
    FIREWALL,
    DDNS,
    PORT_STATUS;
    val displayName: String
        get() = when (this) {
            ROUTER_STATUS -> Loc.s(R.string.enum_004)
            NETWORK_SPEED -> Loc.s(R.string.enum_005)
            QUICK_ACTIONS -> Loc.s(R.string.enum_006)
            SYSTEM_STATUS -> Loc.s(R.string.enum_007)
            ONLINE_DEVICES -> Loc.s(R.string.enum_008)
            WIFI_STATUS -> Loc.s(R.string.enum_009)
            PLUGINS -> Loc.s(R.string.enum_010)
            FIREWALL -> Loc.s(R.string.enum_011)
            DDNS -> "DDNS"
            PORT_STATUS -> Loc.s(R.string.enum_012)
        }
}

/**
 * 卡片尺寸
 */
enum class CardSize {
    SMALL,      // 小卡片（1/2宽度）
    MEDIUM,     // 中等卡片（全宽）
    LARGE       // 大卡片（全宽+大高度）
}

/**
 * 预设布局
 */
enum class PresetLayout {
    DEFAULT,
    COMPACT,
    DETAILED,
    MINIMAL;
    val displayName: String
        get() = when (this) {
            DEFAULT -> Loc.s(R.string.enum_013)
            COMPACT -> Loc.s(R.string.enum_014)
            DETAILED -> Loc.s(R.string.enum_015)
            MINIMAL -> Loc.s(R.string.enum_016)
        }
}
