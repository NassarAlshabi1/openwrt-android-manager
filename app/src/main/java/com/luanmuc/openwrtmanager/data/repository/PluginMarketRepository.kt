package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import com.luanmuc.openwrtmanager.data.model.HotPlugin
import com.luanmuc.openwrtmanager.data.model.PluginCategory
import com.luanmuc.openwrtmanager.data.model.PluginDetail
import com.luanmuc.openwrtmanager.data.model.PluginReview
import com.luanmuc.openwrtmanager.util.DebugMode
import kotlinx.coroutines.delay
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 插件市场Repository
 * 负责插件市场的推荐、排行、分类、详情等功能
 */
class PluginMarketRepository private constructor(private val context: Context) {
    
    /**
     * 获取推荐插件
     */
    suspend fun getRecommendedPlugins(): List<HotPlugin> {
        if (DebugMode.isDebugMode) {
            delay(500)
            return getFakeRecommendedPlugins()
        }
        return emptyList()
    }
    
    /**
     * 获取热门插件排行
     */
    suspend fun getHotPlugins(category: String? = null, limit: Int = 20): List<HotPlugin> {
        if (DebugMode.isDebugMode) {
            delay(500)
            val plugins = getFakeHotPlugins()
            return if (category != null) {
                plugins.filter { it.category == category }.take(limit)
            } else {
                plugins.take(limit)
            }
        }
        return emptyList()
    }
    
    /**
     * 获取新上线插件
     */
    suspend fun getNewPlugins(limit: Int = 10): List<HotPlugin> {
        if (DebugMode.isDebugMode) {
            delay(500)
            return getFakeHotPlugins().filter { it.isNew }.take(limit)
        }
        return emptyList()
    }
    
    /**
     * 获取插件分类（更细致的分类）
     */
    suspend fun getPluginCategories(): List<PluginCategory> {
        if (DebugMode.isDebugMode) {
            return getFakeCategories()
        }
        return emptyList()
    }
    
    /**
     * 获取插件详情（增强版）
     */
    suspend fun getPluginDetail(packageName: String): PluginDetail? {
        if (DebugMode.isDebugMode) {
            delay(500)
            return getFakePluginDetail(packageName)
        }
        return null
    }
    
    /**
     * 获取插件评论
     */
    suspend fun getPluginReviews(packageName: String, limit: Int = 20): List<PluginReview> {
        if (DebugMode.isDebugMode) {
            delay(500)
            return getFakeReviews()
        }
        return emptyList()
    }
    
    /**
     * 搜索插件
     */
    suspend fun searchPlugins(query: String): List<HotPlugin> {
        if (DebugMode.isDebugMode) {
            delay(500)
            return getFakeHotPlugins().filter { 
                it.name.contains(query, true) || 
                it.displayName.contains(query, true) ||
                it.description.contains(query, true)
            }
        }
        return emptyList()
    }
    
    /**
     * 假数据：推荐插件
     */
    private fun getFakeRecommendedPlugins(): List<HotPlugin> {
        return listOf(
            HotPlugin(
                name = "luci-app-aria2",
                displayName = Loc.s(R.string.s0148),
                description = Loc.s(R.string.s0149),
                icon = "⬇️",
                category = Loc.s(R.string.s0150),
                downloadCount = 15000,
                rating = 4.8f,
                isNew = false,
                isHot = true
            ),
            HotPlugin(
                name = "luci-app-transmission",
                displayName = Loc.s(R.string.s0151),
                description = Loc.s(R.string.s0152),
                icon = "🌊",
                category = Loc.s(R.string.s0150),
                downloadCount = 12000,
                rating = 4.6f,
                isNew = false,
                isHot = true
            ),
            HotPlugin(
                name = "luci-app-samba4",
                displayName = Loc.s(R.string.s0153),
                description = Loc.s(R.string.s0154),
                icon = "📁",
                category = Loc.s(R.string.s0155),
                downloadCount = 10000,
                rating = 4.5f,
                isNew = false,
                isHot = true
            ),
            HotPlugin(
                name = "luci-app-wireguard",
                displayName = "WireGuard VPN",
                description = Loc.s(R.string.s0156),
                icon = "🔒",
                category = "VPN",
                downloadCount = 9000,
                rating = 4.9f,
                isNew = true,
                isHot = true
            ),
            HotPlugin(
                name = "luci-app-adblock",
                displayName = Loc.s(R.string.s0157),
                description = Loc.s(R.string.s0158),
                icon = "🚫",
                category = Loc.s(R.string.s0159),
                downloadCount = 8500,
                rating = 4.4f,
                isNew = false,
                isHot = true
            )
        )
    }
    
    /**
     * 假数据：热门插件
     */
    private fun getFakeHotPlugins(): List<HotPlugin> {
        return getFakeRecommendedPlugins() + listOf(
            HotPlugin(
                name = "luci-app-openvpn",
                displayName = "OpenVPN",
                description = Loc.s(R.string.s0160),
                icon = "🔐",
                category = "VPN",
                downloadCount = 7500,
                rating = 4.3f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-minidlna",
                displayName = Loc.s(R.string.s0161),
                description = Loc.s(R.string.s0162),
                icon = "🎬",
                category = Loc.s(R.string.s0163),
                downloadCount = 7000,
                rating = 4.2f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-sqm",
                displayName = Loc.s(R.string.s0164),
                description = Loc.s(R.string.s0165),
                icon = "⚡",
                category = Loc.s(R.string.s0166),
                downloadCount = 6500,
                rating = 4.7f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-ttyd",
                displayName = Loc.s(R.string.s0167),
                description = Loc.s(R.string.s0168),
                icon = "💻",
                category = Loc.s(R.string.s0169),
                downloadCount = 6000,
                rating = 4.5f,
                isNew = true,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-nlbwmon",
                displayName = Loc.s(R.string.s0170),
                description = Loc.s(R.string.s0171),
                icon = "📊",
                category = Loc.s(R.string.s0172),
                downloadCount = 5500,
                rating = 4.4f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-upnp",
                displayName = "UPnP",
                description = Loc.s(R.string.s0173),
                icon = "🔌",
                category = Loc.s(R.string.s0174),
                downloadCount = 5000,
                rating = 4.1f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-wol",
                displayName = Loc.s(R.string.s0175),
                description = Loc.s(R.string.s0176),
                icon = "🌙",
                category = Loc.s(R.string.s0169),
                downloadCount = 4500,
                rating = 4.3f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-ddns",
                displayName = Loc.s(R.string.s0177),
                description = Loc.s(R.string.s0178),
                icon = "🌐",
                category = Loc.s(R.string.s0174),
                downloadCount = 8000,
                rating = 4.2f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-statistics",
                displayName = Loc.s(R.string.s0179),
                description = Loc.s(R.string.s0180),
                icon = "📈",
                category = Loc.s(R.string.s0172),
                downloadCount = 4000,
                rating = 4.3f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-frps",
                displayName = Loc.s(R.string.s0181),
                description = Loc.s(R.string.s0182),
                icon = "🚀",
                category = Loc.s(R.string.s0174),
                downloadCount = 3500,
                rating = 4.6f,
                isNew = true,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-frpc",
                displayName = Loc.s(R.string.s0183),
                description = Loc.s(R.string.s0184),
                icon = "🛸",
                category = Loc.s(R.string.s0174),
                downloadCount = 3000,
                rating = 4.5f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-zerotier",
                displayName = "ZeroTier",
                description = Loc.s(R.string.s0185),
                icon = "🌍",
                category = "VPN",
                downloadCount = 2500,
                rating = 4.4f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-vsftpd",
                displayName = Loc.s(R.string.s0186),
                description = Loc.s(R.string.s0187),
                icon = "📤",
                category = Loc.s(R.string.s0155),
                downloadCount = 2000,
                rating = 4.0f,
                isNew = false,
                isHot = false
            ),
            HotPlugin(
                name = "luci-app-udpxy",
                displayName = Loc.s(R.string.s0188),
                description = Loc.s(R.string.s0189),
                icon = "📺",
                category = Loc.s(R.string.s0163),
                downloadCount = 1500,
                rating = 4.1f,
                isNew = false,
                isHot = false
            )
        )
    }
    
    /**
     * 假数据：插件分类
     */
    private fun getFakeCategories(): List<PluginCategory> {
        return listOf(
            PluginCategory(
                id = "download",
                name = Loc.s(R.string.s0150),
                icon = "⬇️",
                description = Loc.s(R.string.s0190),
                pluginCount = 5
            ),
            PluginCategory(
                id = "vpn",
                name = Loc.s(R.string.s0191),
                icon = "🔒",
                description = Loc.s(R.string.s0192),
                pluginCount = 8
            ),
            PluginCategory(
                id = "fileshare",
                name = Loc.s(R.string.s0155),
                icon = "📁",
                description = Loc.s(R.string.s0193),
                pluginCount = 4
            ),
            PluginCategory(
                id = "media",
                name = Loc.s(R.string.s0163),
                icon = "🎬",
                description = Loc.s(R.string.s0194),
                pluginCount = 5
            ),
            PluginCategory(
                id = "network",
                name = Loc.s(R.string.s0195),
                icon = "🌐",
                description = Loc.s(R.string.s0196),
                pluginCount = 10
            ),
            PluginCategory(
                id = "security",
                name = Loc.s(R.string.s0159),
                icon = "🛡️",
                description = Loc.s(R.string.s0197),
                pluginCount = 6
            ),
            PluginCategory(
                id = "monitor",
                name = Loc.s(R.string.s0198),
                icon = "📊",
                description = Loc.s(R.string.s0199),
                pluginCount = 7
            ),
            PluginCategory(
                id = "optimize",
                name = Loc.s(R.string.s0166),
                icon = "⚡",
                description = Loc.s(R.string.s0200),
                pluginCount = 3
            ),
            PluginCategory(
                id = "tools",
                name = Loc.s(R.string.s0201),
                icon = "🔧",
                description = Loc.s(R.string.s0202),
                pluginCount = 8
            ),
            PluginCategory(
                id = "system",
                name = Loc.s(R.string.s0203),
                icon = "⚙️",
                description = Loc.s(R.string.s0204),
                pluginCount = 6
            )
        )
    }
    
    /**
     * 假数据：插件详情
     */
    private fun getFakePluginDetail(packageName: String): PluginDetail {
        return PluginDetail(
            name = packageName,
            displayName = Loc.s(R.string.s0205),
            description = Loc.s(R.string.s0206),
            longDescription = """
                这是一个功能强大的OpenWrt插件，可以帮助你更好地管理路由器。
                
                主要功能：
                • 功能一：详细说明
                • 功能二：详细说明
                • 功能三：详细说明
                • 功能四：详细说明
                
                安装说明：
                1. 点击安装按钮
                2. 等待安装完成
                3. 在菜单中找到并打开
                
                注意事项：
                • 安装前请确保有足够的存储空间
                • 部分功能需要特定的硬件支持
                • 如有问题请查看官方文档
            """.trimIndent(),
            icon = "📦",
            category = Loc.s(R.string.s0169),
            version = "1.0.0",
            size = 1024 * 1024,
            author = Loc.s(R.string.s0207),
            homepage = "https://openwrt.org",
            license = "GPL-2.0",
            screenshots = listOf(),
            tags = listOf(Loc.s(R.string.s0169), Loc.s(R.string.s0208), Loc.s(R.string.s0209)),
            dependencies = listOf("libc", "libuci"),
            downloadCount = 10000,
            rating = 4.5f,
            ratingCount = 100,
            lastUpdated = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
            isInstalled = false,
            isUpdateAvailable = false,
            latestVersion = "1.0.0"
        )
    }
    
    /**
     * 假数据：评论
     */
    private fun getFakeReviews(): List<PluginReview> {
        return listOf(
            PluginReview(
                id = "1",
                userName = Loc.s(R.string.s0210),
                rating = 5f,
                comment = Loc.s(R.string.s0211),
                date = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000,
                helpfulCount = 15
            ),
            PluginReview(
                id = "2",
                userName = Loc.s(R.string.s0212),
                rating = 4f,
                comment = Loc.s(R.string.s0213),
                date = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000,
                helpfulCount = 8
            ),
            PluginReview(
                id = "3",
                userName = Loc.s(R.string.s0214),
                rating = 5f,
                comment = Loc.s(R.string.s0215),
                date = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
                helpfulCount = 12
            ),
            PluginReview(
                id = "4",
                userName = Loc.s(R.string.s0216),
                rating = 3f,
                comment = Loc.s(R.string.s0217),
                date = System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000,
                helpfulCount = 3
            ),
            PluginReview(
                id = "5",
                userName = Loc.s(R.string.s0218),
                rating = 5f,
                comment = Loc.s(R.string.s0219),
                date = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000,
                helpfulCount = 20
            )
        )
    }
    
    companion object {
        @Volatile
        private var instance: PluginMarketRepository? = null
        
        fun getInstance(context: Context): PluginMarketRepository {
            return instance ?: synchronized(this) {
                instance ?: PluginMarketRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
