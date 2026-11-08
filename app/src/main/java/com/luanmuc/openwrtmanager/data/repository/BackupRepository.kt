package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.luanmuc.openwrtmanager.data.model.BackupRecord
import com.luanmuc.openwrtmanager.data.model.BackupType
import com.luanmuc.openwrtmanager.data.model.RestoreProgress
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 配置备份Repository
 * 负责配置备份、恢复、管理等功能
 */
class BackupRepository private constructor(private val context: Context) {
    private val luciRepository = LuciRepository.getInstance()
    
    private val prefs: SharedPreferences = context.getSharedPreferences("backup_manager", Context.MODE_PRIVATE)
    private val backupDir: File = File(context.filesDir, "backups")
    
    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }
    
    /**
     * 获取备份列表
     */
    fun getBackupList(): List<BackupRecord> {
        
        val json = prefs.getString("backup_list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                BackupRecord(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", ""),
                    description = obj.optString("description", ""),
                    createdAt = obj.optLong("createdAt", 0),
                    size = obj.optLong("size", 0),
                    routerName = obj.optString("routerName", ""),
                    firmwareVersion = obj.optString("firmwareVersion", ""),
                    backupType = BackupType.valueOf(obj.optString("backupType", "FULL")),
                    filePath = obj.optString("filePath", ""),
                    isEncrypted = obj.optBoolean("isEncrypted", false)
                )
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 创建备份
     */
    suspend fun createBackup(
        name: String,
        description: String = "",
        type: BackupType = BackupType.FULL
    ): BackupRecord? {
        
        // 真实模式：调用路由器备份接口
        return try {
            val backupData = luciRepository.exportBackup()
            if (backupData != null) {
                val record = BackupRecord(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    createdAt = System.currentTimeMillis(),
                    size = backupData.length.toLong(),
                    routerName = luciRepository.getCurrentAddress(),
                    firmwareVersion = "",
                    backupType = type,
                    filePath = "",
                    isEncrypted = false
                )
                saveBackupRecord(record)
                record
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 恢复配置
     */
    suspend fun restoreBackup(backupId: String): RestoreProgress {
        
        // 真实模式：恢复配置（需要上传备份文件并执行sysupgrade -r）
        return RestoreProgress(
            currentStep = 1,
            totalSteps = 3,
            currentStepName = Loc.s(R.string.s0025),
            percentage = 33,
            isCompleted = false,
            error = Loc.s(R.string.s0026)
        )
    }
    
    /**
     * 删除备份
     */
    fun deleteBackup(backupId: String): Boolean {
        val list = getBackupList().toMutableList()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val record = iterator.next()
            if (record.id == backupId) {
                // 删除文件
                if (record.filePath.isNotEmpty()) {
                    try {
                        File(record.filePath).delete()
                    } catch (e: Exception) {
                        // 忽略
                    }
                }
                iterator.remove()
                break
            }
        }
        saveBackupList(list)
        return true
    }
    
    /**
     * 导出备份
     */
    fun exportBackup(backupId: String): File? {
        val record = getBackupList().firstOrNull { it.id == backupId } ?: return null
        if (record.filePath.isEmpty()) return null
        return File(record.filePath)
    }
    
    /**
     * 保存备份记录
     */
    private fun saveBackupRecord(record: BackupRecord) {
        val list = getBackupList().toMutableList()
        list.add(0, record)
        // 最多保留20个备份
        val limited = list.take(20)
        saveBackupList(limited)
    }
    
    /**
     * 保存备份列表
     */
    private fun saveBackupList(list: List<BackupRecord>) {
        val arr = JSONArray()
        for (record in list) {
            arr.put(JSONObject().apply {
                put("id", record.id)
                put("name", record.name)
                put("description", record.description)
                put("createdAt", record.createdAt)
                put("size", record.size)
                put("routerName", record.routerName)
                put("firmwareVersion", record.firmwareVersion)
                put("backupType", record.backupType.name)
                put("filePath", record.filePath)
                put("isEncrypted", record.isEncrypted)
            })
        }
        prefs.edit().putString("backup_list", arr.toString()).apply()
    }
    
    /**
     * 假数据：备份列表
     */
    private fun getFakeBackupList(): List<BackupRecord> {
        val now = System.currentTimeMillis()
        return listOf(
            BackupRecord(
                id = "backup_001",
                name = Loc.s(R.string.s0027),
                description = Loc.s(R.string.s0028),
                createdAt = now - 30 * 24 * 60 * 60 * 1000,
                size = 512 * 1024,
                routerName = "OpenWrt-Router",
                firmwareVersion = "23.05.0",
                backupType = BackupType.FULL,
                filePath = "",
                isEncrypted = false
            ),
            BackupRecord(
                id = "backup_002",
                name = Loc.s(R.string.s0029),
                description = Loc.s(R.string.s0030),
                createdAt = now - 15 * 24 * 60 * 60 * 1000,
                size = 640 * 1024,
                routerName = "OpenWrt-Router",
                firmwareVersion = "23.05.0",
                backupType = BackupType.FULL,
                filePath = "",
                isEncrypted = false
            ),
            BackupRecord(
                id = "backup_003",
                name = Loc.s(R.string.s0031),
                description = Loc.s(R.string.s0032),
                createdAt = now - 7 * 24 * 60 * 60 * 1000,
                size = 680 * 1024,
                routerName = "OpenWrt-Router",
                firmwareVersion = "23.05.0",
                backupType = BackupType.NETWORK,
                filePath = "",
                isEncrypted = false
            ),
            BackupRecord(
                id = "backup_004",
                name = Loc.s(R.string.s0033),
                description = Loc.s(R.string.s0034),
                createdAt = now - 3 * 24 * 60 * 60 * 1000,
                size = 580 * 1024,
                routerName = "OpenWrt-Router",
                firmwareVersion = "23.05.0",
                backupType = BackupType.WIFI,
                filePath = "",
                isEncrypted = false
            ),
            BackupRecord(
                id = "backup_005",
                name = Loc.s(R.string.s0035),
                description = Loc.s(R.string.s0036),
                createdAt = now - 1 * 24 * 60 * 60 * 1000,
                size = 720 * 1024,
                routerName = "OpenWrt-Router",
                firmwareVersion = "23.05.0",
                backupType = BackupType.FULL,
                filePath = "",
                isEncrypted = false
            )
        )
    }
    
    companion object {
        @Volatile
        private var instance: BackupRepository? = null
        
        fun getInstance(context: Context): BackupRepository {
            return instance ?: synchronized(this) {
                instance ?: BackupRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
