package com.luanmuc.openwrtmanager.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 主题管理器
 * 支持浅色、深色、跟随系统三种模式
 * 
 * 特性：
 * - 我的页面切换入口
 * - 所有页面适配深色
 * - 跟随系统
 * - 立即生效
 */
class ThemeManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    
    /**
     * 当前主题模式
     */
    val currentMode: ThemeMode
        get() = _themeMode.value
    
    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        saveThemeMode(mode)
        _themeMode.value = mode
        // 同步到MiTheme
        MiTheme.isDarkMode = isDarkMode()
    }
    
    /**
     * 切换主题模式（浅色 -> 深色 -> 跟随系统 -> 浅色）
     */
    fun toggleThemeMode() {
        val nextMode = when (_themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        setThemeMode(nextMode)
    }
    
    /**
     * 从SharedPreferences加载主题模式
     */
    private fun loadThemeMode(): ThemeMode {
        val modeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(modeName ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }
    
    /**
     * 保存主题模式到SharedPreferences
     */
    private fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }
    
    /**
     * 更新系统主题变化（跟随系统时调用）
     */
    fun updateSystemTheme() {
        if (_themeMode.value == ThemeMode.SYSTEM) {
            MiTheme.isDarkMode = isDarkMode()
        }
    }
    
    /**
     * 判断当前是否是深色模式
     */
    fun isDarkMode(): Boolean {
        return when (_themeMode.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> {
                // 跟随系统：检查系统是否是深色模式
                val nightModeFlags = context.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
    
    init {
        // 初始化时同步MiTheme状态
        MiTheme.isDarkMode = isDarkMode()
    }
    
    companion object {
        @Volatile
        private var instance: ThemeManager? = null
        
        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 主题模式枚举
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;
    val displayName: String
        get() = when (this) {
            LIGHT -> Loc.s(R.string.enum_001)
            DARK -> Loc.s(R.string.enum_002)
            SYSTEM -> Loc.s(R.string.enum_003)
        }
}
