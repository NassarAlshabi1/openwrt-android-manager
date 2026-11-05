package com.luanmuc.openwrtmanager

import android.app.Application
import android.content.Context
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.util.Loc
import com.luanmuc.openwrtmanager.util.NetworkMonitor
import com.luanmuc.openwrtmanager.util.AppNotificationManager
import com.luanmuc.openwrtmanager.util.LogUtils

/**
 * Application 类
 */
class OpenWrtApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化本地化（必须在其他组件之前，支持英语/阿拉伯语）
        Loc.init(this)
        // 初始化日志工具（release版本禁用调试日志）
        LogUtils.isDebugEnabled = BuildConfig.DEBUG
        // 初始化全局异常处理
        initGlobalExceptionHandler()
        // 初始化仓库
        RouterRepository.getInstance(this)
        // 初始化LuCI仓库，加载持久化的session
        LuciRepository.getInstance(this)
        // 初始化网络状态监控
        NetworkMonitor.init(this)
        // 初始化通知管理器
        AppNotificationManager.init(this)
    }

    /**
     * 初始化全局异常处理器
     * 确保APP不会因为未捕获的异常而闪退
     */
    private fun initGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                LogUtils.e("OpenWrtApp", "未捕获的异常: ${throwable.message}", throwable)
                // 记录异常日志，不让APP崩溃
                // 注意：某些严重异常可能无法恢复，但我们尽量尝试保持APP运行
            } catch (e: Exception) {
                LogUtils.e("OpenWrtApp", "异常处理失败", e)
            }
            // 不调用默认处理器，避免APP闪退
            // 对于非致命异常，APP会继续运行
            // 对于致命异常，可能会出现ANR或其他问题，但至少不会直接闪退
        }
    }

    companion object {
        /**
         * 获取Application实例
         */
        fun get(context: Context): OpenWrtApp {
            return context.applicationContext as? OpenWrtApp ?: error("Application class is not OpenWrtApp")
        }
    }
}
