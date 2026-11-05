package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.luanmuc.openwrtmanager.ui.theme.ThemeMode
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 设置仓库 - 管理所有APP设置
 * 使用SharedPreferences持久化存储
 */
class SettingsRepository private constructor() {

    private lateinit var prefs: SharedPreferences

    // 主题设置
    var themeMode: ThemeMode
        get() {
            val value = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            return try {
                ThemeMode.valueOf(value)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    // 语言设置
    var language: Language
        get() {
            val value = prefs.getString(KEY_LANGUAGE, Language.SYSTEM.name) ?: Language.SYSTEM.name
            return try {
                Language.valueOf(value)
            } catch (e: Exception) {
                Language.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value.name).apply()
        }

    // 自动刷新设置
    var autoRefreshEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_REFRESH_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_REFRESH_ENABLED, value).apply()
        }

    var refreshInterval: Int
        get() = prefs.getInt(KEY_REFRESH_INTERVAL, 30) // 默认30秒
        set(value) {
            prefs.edit().putInt(KEY_REFRESH_INTERVAL, value).apply()
        }

    // 连接设置
    var connectionTimeout: Int
        get() = prefs.getInt(KEY_CONNECTION_TIMEOUT, 10) // 默认10秒
        set(value) {
            prefs.edit().putInt(KEY_CONNECTION_TIMEOUT, value).apply()
        }

    var retryCount: Int
        get() = prefs.getInt(KEY_RETRY_COUNT, 3) // 默认3次
        set(value) {
            prefs.edit().putInt(KEY_RETRY_COUNT, value).apply()
        }

    var httpsAutoDetect: Boolean
        get() = prefs.getBoolean(KEY_HTTPS_AUTO_DETECT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HTTPS_AUTO_DETECT, value).apply()
        }

    // 通知设置
    var pushNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_PUSH_NOTIFICATION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PUSH_NOTIFICATION_ENABLED, value).apply()
        }

    var offlineAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_OFFLINE_ALERT_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_OFFLINE_ALERT_ENABLED, value).apply()
        }

    // 其他设置
    var defaultHomePage: String
        get() = prefs.getString(KEY_DEFAULT_HOME_PAGE, "home") ?: "home"
        set(value) {
            prefs.edit().putString(KEY_DEFAULT_HOME_PAGE, value).apply()
        }

    var trafficUnit: TrafficUnit
        get() {
            val value = prefs.getString(KEY_TRAFFIC_UNIT, TrafficUnit.AUTO.name) ?: TrafficUnit.AUTO.name
            return try {
                TrafficUnit.valueOf(value)
            } catch (e: Exception) {
                TrafficUnit.AUTO
            }
        }
        set(value) {
            prefs.edit().putString(KEY_TRAFFIC_UNIT, value.name).apply()
        }

    var statusBarEnabled: Boolean
        get() = prefs.getBoolean(KEY_STATUS_BAR_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_STATUS_BAR_ENABLED, value).apply()
        }

    // 预加载设置
    var autoPreloadEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PRELOAD_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_PRELOAD_ENABLED, value).apply()
        }

    var preloadSystemInfo: Boolean
        get() = prefs.getBoolean(KEY_PRELOAD_SYSTEM_INFO, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PRELOAD_SYSTEM_INFO, value).apply()
        }

    var preloadRouterStatus: Boolean
        get() = prefs.getBoolean(KEY_PRELOAD_ROUTER_STATUS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PRELOAD_ROUTER_STATUS, value).apply()
        }

    var preloadNetwork: Boolean
        get() = prefs.getBoolean(KEY_PRELOAD_NETWORK, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PRELOAD_NETWORK, value).apply()
        }

    var preloadPlugins: Boolean
        get() = prefs.getBoolean(KEY_PRELOAD_PLUGINS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PRELOAD_PLUGINS, value).apply()
        }

    var preloadDevices: Boolean
        get() = prefs.getBoolean(KEY_PRELOAD_DEVICES, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PRELOAD_DEVICES, value).apply()
        }

    var preloadFirewall: Boolean
        get() = prefs.getBoolean(KEY_PRELOAD_FIREWALL, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PRELOAD_FIREWALL, value).apply()
        }

    /**
     * 初始化
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 清除所有设置
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "settings_prefs"

        // Keys
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_AUTO_REFRESH_ENABLED = "auto_refresh_enabled"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval"
        private const val KEY_CONNECTION_TIMEOUT = "connection_timeout"
        private const val KEY_RETRY_COUNT = "retry_count"
        private const val KEY_HTTPS_AUTO_DETECT = "https_auto_detect"
        private const val KEY_PUSH_NOTIFICATION_ENABLED = "push_notification_enabled"
        private const val KEY_OFFLINE_ALERT_ENABLED = "offline_alert_enabled"
        private const val KEY_DEFAULT_HOME_PAGE = "default_home_page"
        private const val KEY_TRAFFIC_UNIT = "traffic_unit"
        private const val KEY_STATUS_BAR_ENABLED = "status_bar_enabled"
        private const val KEY_AUTO_PRELOAD_ENABLED = "auto_preload_enabled"
        private const val KEY_PRELOAD_SYSTEM_INFO = "preload_system_info"
        private const val KEY_PRELOAD_ROUTER_STATUS = "preload_router_status"
        private const val KEY_PRELOAD_NETWORK = "preload_network"
        private const val KEY_PRELOAD_PLUGINS = "preload_plugins"
        private const val KEY_PRELOAD_DEVICES = "preload_devices"
        private const val KEY_PRELOAD_FIREWALL = "preload_firewall"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context? = null): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository().also {
                    instance = it
                    context?.let { ctx -> it.init(ctx) }
                }
            }
        }
    }
}

/**
 * 语言枚举
 */
enum class Language {
    SYSTEM,
    ENGLISH,
    ARABIC;
    val displayName: String
        get() = when (this) {
            SYSTEM -> Loc.s(R.string.lang_system)
            ENGLISH -> Loc.s(R.string.lang_english)
            ARABIC -> Loc.s(R.string.lang_arabic)
        }
}

/**
 * 流量单位枚举
 */
enum class TrafficUnit {
    AUTO,
    KB,
    MB,
    GB;
    val displayName: String
        get() = when (this) {
            AUTO -> Loc.s(R.string.enum_086)
            KB -> "KB"
            MB -> "MB"
            GB -> "GB"
        }
}

/**
 * 刷新间隔选项
 */
enum class RefreshInterval(val seconds: Int) {
    SECONDS_5(5),
    SECONDS_10(10),
    SECONDS_30(30),
    MINUTES_1(60),
    MINUTES_5(300);
    val displayName: String
        get() = when (this) {
            SECONDS_5 -> Loc.s(R.string.enum_087)
            SECONDS_10 -> Loc.s(R.string.enum_088)
            SECONDS_30 -> Loc.s(R.string.enum_089)
            MINUTES_1 -> Loc.s(R.string.enum_090)
            MINUTES_5 -> Loc.s(R.string.enum_091)
        }
}

/**
 * 连接超时选项
 */
enum class ConnectionTimeout(val seconds: Int) {
    SECONDS_5(5),
    SECONDS_10(10),
    SECONDS_30(30),
    MINUTES_1(60);
    val displayName: String
        get() = when (this) {
            SECONDS_5 -> Loc.s(R.string.enum_087)
            SECONDS_10 -> Loc.s(R.string.enum_088)
            SECONDS_30 -> Loc.s(R.string.enum_089)
            MINUTES_1 -> Loc.s(R.string.enum_090)
        }
}

/**
 * 重试次数选项
 */
enum class RetryCount(val count: Int) {
    COUNT_1(1),
    COUNT_2(2),
    COUNT_3(3),
    COUNT_5(5);
    val displayName: String
        get() = when (this) {
            COUNT_1 -> Loc.s(R.string.enum_092)
            COUNT_2 -> Loc.s(R.string.enum_093)
            COUNT_3 -> Loc.s(R.string.enum_094)
            COUNT_5 -> Loc.s(R.string.enum_095)
        }
}
