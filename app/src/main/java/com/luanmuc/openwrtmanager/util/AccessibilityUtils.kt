package com.luanmuc.openwrtmanager.util

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 可访问性工具类
 * 提供可访问性相关的工具函数
 */
object AccessibilityUtils {
    
    /**
     * 检查是否启用了触摸探索（TalkBack等）
     */
    fun isTouchExplorationEnabled(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            am.isTouchExplorationEnabled
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查是否启用了任何可访问性服务
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            am.isEnabled
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查是否启用了高对比度文字
     */
    fun isHighTextContrastEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "high_text_contrast_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查是否启用了放大手势
     */
    fun isMagnificationEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "accessibility_display_magnification_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取动画缩放比例
     */
    fun getAnimationScale(context: Context): Float {
        return try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
    }
    
    /**
     * 检查是否禁用了动画
     */
    fun isAnimationDisabled(context: Context): Boolean {
        return getAnimationScale(context) == 0f
    }
    
    /**
     * 获取字体缩放比例
     */
    fun getFontScale(context: Context): Float {
        return try {
            Settings.System.getFloat(
                context.contentResolver,
                Settings.System.FONT_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
    }
    
    /**
     * 检查是否使用了大字体
     */
    fun isLargeFont(context: Context): Boolean {
        return getFontScale(context) > 1.0f
    }
    
    /**
     * 获取显示密度缩放比例
     */
    fun getDensityScale(context: Context): Float {
        return context.resources.configuration.densityDpi / 160f
    }
    
    /**
     * 检查是否启用了颜色校正
     */
    fun isColorCorrectionEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "accessibility_display_daltonizer_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取颜色校正模式
     */
    fun getColorCorrectionMode(context: Context): String {
        return try {
            val mode = Settings.Secure.getInt(
                context.contentResolver,
                "accessibility_display_daltonizer",
                -1
            )
            when (mode) {
                0 -> Loc.s(R.string.s0824)
                1 -> Loc.s(R.string.s0825)
                2 -> Loc.s(R.string.s0826)
                11 -> Loc.s(R.string.s0827)
                12 -> Loc.s(R.string.s0828)
                13 -> Loc.s(R.string.s0829)
                else -> Loc.s(R.string.s0050)
            }
        } catch (e: Exception) {
            Loc.s(R.string.s0050)
        }
    }
    
    /**
     * 检查是否启用了颜色反转
     */
    fun isColorInversionEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "accessibility_display_inversion_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查是否启用了字幕
     */
    fun isCaptioningEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "accessibility_captioning_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取推荐的最小触摸目标大小（dp）
     */
    fun getRecommendedMinTouchTargetSize(): Int {
        // Material Design推荐的最小触摸目标大小为48dp
        return 48
    }
    
    /**
     * 检查触摸目标大小是否符合可访问性标准
     */
    fun isTouchTargetAccessible(sizeDp: Int): Boolean {
        return sizeDp >= getRecommendedMinTouchTargetSize()
    }
    
    /**
     * 生成内容描述
     */
    fun generateContentDescription(vararg parts: String): String {
        return parts.filter { it.isNotEmpty() }.joinToString("，")
    }
    
    /**
     * 检查是否应该启用可访问性功能
     * 当检测到用户使用可访问性服务时，应该提供更好的可访问性支持
     */
    fun shouldEnhanceAccessibility(context: Context): Boolean {
        return isTouchExplorationEnabled(context) || 
               isHighTextContrastEnabled(context) ||
               isLargeFont(context)
    }
    
    /**
     * 获取可访问性状态摘要
     */
    fun getAccessibilityStatus(context: Context): AccessibilityStatus {
        return AccessibilityStatus(
            isTouchExplorationEnabled = isTouchExplorationEnabled(context),
            isAccessibilityEnabled = isAccessibilityEnabled(context),
            isHighTextContrastEnabled = isHighTextContrastEnabled(context),
            isMagnificationEnabled = isMagnificationEnabled(context),
            isAnimationDisabled = isAnimationDisabled(context),
            isLargeFont = isLargeFont(context),
            fontScale = getFontScale(context),
            animationScale = getAnimationScale(context),
            isColorCorrectionEnabled = isColorCorrectionEnabled(context),
            isColorInversionEnabled = isColorInversionEnabled(context),
            isCaptioningEnabled = isCaptioningEnabled(context)
        )
    }
    
    /**
     * 可访问性状态数据类
     */
    data class AccessibilityStatus(
        val isTouchExplorationEnabled: Boolean,
        val isAccessibilityEnabled: Boolean,
        val isHighTextContrastEnabled: Boolean,
        val isMagnificationEnabled: Boolean,
        val isAnimationDisabled: Boolean,
        val isLargeFont: Boolean,
        val fontScale: Float,
        val animationScale: Float,
        val isColorCorrectionEnabled: Boolean,
        val isColorInversionEnabled: Boolean,
        val isCaptioningEnabled: Boolean
    ) {
        /**
         * 是否需要增强可访问性
         */
        val needsEnhancement: Boolean
            get() = isTouchExplorationEnabled || isHighTextContrastEnabled || isLargeFont
        
        /**
         * 状态摘要
         */
        val summary: String
            get() {
                val features = mutableListOf<String>()
                if (isTouchExplorationEnabled) features.add(Loc.s(R.string.s0830))
                if (isHighTextContrastEnabled) features.add(Loc.s(R.string.s0831))
                if (isMagnificationEnabled) features.add(Loc.s(R.string.s0832))
                if (isLargeFont) features.add(Loc.s(R.string.s0833, fontScale))
                if (isAnimationDisabled) features.add(Loc.s(R.string.s0834))
                if (isColorCorrectionEnabled) features.add(Loc.s(R.string.s0835))
                if (isColorInversionEnabled) features.add(Loc.s(R.string.s0836))
                if (isCaptioningEnabled) features.add(Loc.s(R.string.s0837))
                
                return if (features.isEmpty()) {
                    Loc.s(R.string.s0838)
                } else {
                    Loc.s(R.string.s0839, features.joinToString("、"))
                }
            }
    }
}