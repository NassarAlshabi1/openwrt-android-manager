package com.luanmuc.openwrtmanager.service
import com.luanmuc.openwrtmanager.data.repository.BackupRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.util.AppNotificationManager
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.util.LogUtils

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 定时任务服务
 * 定时获取数据、定时备份
 *
 * 任务内容（全部为真实操作）：
 * 1. 刷新路由器数据（保持会话并记录状态）
 * 2. 读取固件信息并保持缓存新鲜
 * 3. 自动备份（在设置开启时，通过路由器备份接口导出配置）
 */
class ScheduledTaskService : Service() {

    companion object {
        private const val TAG = "ScheduledTaskService"
        private const val TASK_INTERVAL = 30 * 60 * 1000L // 30分钟执行一次

        // 自动备份开关（备份设置页控制）
        private const val PREFS_NAME = "scheduled_task_prefs"
        private const val KEY_AUTO_BACKUP = "auto_backup_enabled"

        fun start(context: Context) {
            try {
                val intent = Intent(context, ScheduledTaskService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                LogUtils.e(TAG, "启动服务失败", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, ScheduledTaskService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                LogUtils.e(TAG, "停止服务失败", e)
            }
        }

        /**
         * 设置定时闹钟
         */
        fun setAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, ScheduledTaskReceiver::class.java).apply {
                    action = "com.luanmuc.openwrtmanager.SCHEDULED_TASK"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + TASK_INTERVAL,
                    TASK_INTERVAL,
                    pendingIntent
                )

                LogUtils.d(TAG, "定时闹钟已设置")
            } catch (e: Exception) {
                LogUtils.e(TAG, "设置定时闹钟失败", e)
            }
        }

        /**
         * 取消定时闹钟
         */
        fun cancelAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, ScheduledTaskReceiver::class.java).apply {
                    action = "com.luanmuc.openwrtmanager.SCHEDULED_TASK"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                alarmManager.cancel(pendingIntent)
                LogUtils.d(TAG, "定时闹钟已取消")
            } catch (e: Exception) {
                LogUtils.e(TAG, "取消定时闹钟失败", e)
            }
        }

        fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
        }

        fun isAutoBackupEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_BACKUP, false)
        }
    }

    private var taskJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        LogUtils.d(TAG, "服务创建")
        startTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.d(TAG, "服务启动")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "服务销毁")
        stopTasks()
    }

    /**
     * 开始定时任务
     */
    private fun startTasks() {
        stopTasks()

        taskJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    executeScheduledTasks()
                } catch (e: Exception) {
                    LogUtils.e(TAG, "执行定时任务失败", e)
                }
                delay(TASK_INTERVAL)
            }
        }
    }

    /**
     * 停止定时任务
     */
    private fun stopTasks() {
        taskJob?.cancel()
        taskJob = null
    }

    /**
     * 执行定时任务
     */
    private suspend fun executeScheduledTasks() {
        LogUtils.d(TAG, "执行定时任务")

        // 任务1：刷新数据（真实读取路由器状态）
        refreshData()

        // 任务2：读取固件信息（保持数据新鲜）
        checkFirmwareUpdate()

        // 任务3：自动备份（如果开启）
        if (isAutoBackupEnabled()) {
            performAutoBackup()
        }
    }

    /**
     * 刷新数据：登录活动路由器并读取系统状态，
     * 让缓存与会话保持新鲜。
     */
    private suspend fun refreshData() {
        try {
            LogUtils.d(TAG, "刷新数据")
            val routerRepository = RouterRepository.getInstance(this)
            val luciRepository = LuciRepository.getInstance(this)

            val routers = routerRepository.getRoutersList()
            val activeId = routerRepository.getActiveRouterId()
            val activeRouter = (routers.find { it.id == activeId } ?: routers.firstOrNull()) ?: return

            if (!luciRepository.isLoggedIn()) {
                val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                luciRepository.login(activeRouter.address, activeRouter.username, password)
            }

            val status = luciRepository.getRouterStatus()
            LogUtils.d(TAG, "数据刷新完成: uptime=${status.uptime}s")
        } catch (e: Exception) {
            LogUtils.e(TAG, "刷新数据失败", e)
        }
    }

    /**
     * 检查固件信息：读取路由器固件版本；
     * 与上次记录不同则发出固件变更通知。
     */
    private suspend fun checkFirmwareUpdate() {
        try {
            LogUtils.d(TAG, "检查固件信息")
            val luciRepository = LuciRepository.getInstance(this)
            if (!luciRepository.isLoggedIn()) return

            val info = luciRepository.getFirmwareInfo()
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastVersion = prefs.getString("last_firmware_version", null)
            if (lastVersion != null && lastVersion != info.currentVersion) {
                AppNotificationManager.showFirmwareUpdateNotification(info.currentVersion, info.deviceModel)
            }
            prefs.edit().putString("last_firmware_version", info.currentVersion).apply()
            LogUtils.d(TAG, "固件信息检查完成: ${info.currentVersion}")
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查固件信息失败", e)
        }
    }

    /**
     * 检查是否开启自动备份
     */
    private fun isAutoBackupEnabled(): Boolean {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_BACKUP, false)
    }

    /**
     * 执行自动备份：通过路由器备份接口导出配置并保存记录。
     */
    private suspend fun performAutoBackup() {
        try {
            LogUtils.d(TAG, "执行自动备份")
            val backupRepository = BackupRepository.getInstance(this)
            val record = backupRepository.createBackup(
                name = "auto_" + System.currentTimeMillis(),
                description = "auto backup"
            )
            if (record != null) {
                LogUtils.d(TAG, "自动备份完成: ${record.name}")
            } else {
                LogUtils.d(TAG, "自动备份失败（路由器不可达或导出失败）")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "执行自动备份失败", e)
        }
    }
}
