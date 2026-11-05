package com.luanmuc.openwrtmanager.data.model

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 备份记录
 */
data class BackupRecord(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdAt: Long = 0,
    val size: Long = 0,
    val routerName: String = "",
    val firmwareVersion: String = "",
    val backupType: BackupType = BackupType.FULL,
    val filePath: String = "",
    val isEncrypted: Boolean = false
)

/**
 * 备份类型
 */
enum class BackupType {
    FULL,
    SYSTEM,
    NETWORK,
    FIREWALL,
    WIFI,
    CUSTOM;
    val displayName: String
        get() = when (this) {
            FULL -> Loc.s(R.string.enum_066)
            SYSTEM -> Loc.s(R.string.enum_067)
            NETWORK -> Loc.s(R.string.enum_068)
            FIREWALL -> Loc.s(R.string.enum_069)
            WIFI -> Loc.s(R.string.enum_070)
            CUSTOM -> Loc.s(R.string.enum_071)
        }
}

/**
 * 恢复进度
 */
data class RestoreProgress(
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val currentStepName: String = "",
    val percentage: Int = 0,
    val isCompleted: Boolean = false,
    val error: String? = null
)
