package com.luanmuc.openwrtmanager.ui.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.model.BackupRecord
import com.luanmuc.openwrtmanager.data.model.BackupType
import com.luanmuc.openwrtmanager.data.model.RestoreProgress
import com.luanmuc.openwrtmanager.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 配置备份恢复ViewModel
 */
class BackupViewModel(application: Application) : BaseViewModel(application) {
    
    private val backupRepository = BackupRepository.getInstance(application)
    
    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 备份列表
    private val _backupList = MutableStateFlow<List<BackupRecord>>(emptyList())
    val backupList: StateFlow<List<BackupRecord>> = _backupList.asStateFlow()
    
    // 是否正在创建备份
    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()
    
    // 恢复进度
    private val _restoreProgress = MutableStateFlow<RestoreProgress?>(null)
    val restoreProgress: StateFlow<RestoreProgress?> = _restoreProgress.asStateFlow()
    
    // 操作结果
    private val _operationResult = MutableStateFlow<String?>(null)
    val operationResult: StateFlow<String?> = _operationResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    
    init {
        initNetworkMonitor()
        loadBackupList()
    }
    
    fun loadBackupList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = backupRepository.getBackupList()
                _backupList.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 创建备份
     */
    fun createBackup(
        name: String,
        description: String = "",
        backupType: BackupType = BackupType.FULL
    ) {
        viewModelScope.launch {
            _isCreating.value = true
            try {
                val record = backupRepository.createBackup(name, description, backupType)
                _operationResult.value = Loc.s(R.string.s0292)
                loadBackupList()
            } catch (e: Exception) {
                _operationResult.value = Loc.s(R.string.s0293, e.message)
            } finally {
                _isCreating.value = false
            }
        }
    }
    
    /**
     * 恢复备份
     */
    fun restoreBackup(backupId: String) {
        viewModelScope.launch {
            try {
                // 模拟分步恢复
                val progress = backupRepository.restoreBackup(backupId)
                _restoreProgress.value = progress
                _operationResult.value = if (progress.isCompleted) Loc.s(R.string.s0294) else Loc.s(R.string.s0295)
            } catch (e: Exception) {
                _operationResult.value = Loc.s(R.string.s0296, e.message)
            }
        }
    }
    
    /**
     * 删除备份
     */
    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            try {
                val success = backupRepository.deleteBackup(backupId)
                _operationResult.value = if (success) Loc.s(R.string.s0297) else Loc.s(R.string.s0298)
                loadBackupList()
            } catch (e: Exception) {
                _operationResult.value = Loc.s(R.string.s0299, e.message)
            }
        }
    }
    
    /**
     * 清除恢复进度
     */
    fun clearRestoreProgress() {
        _restoreProgress.value = null
    }
    
    /**
     * 清除操作结果
     */
    fun clearError() { _error.value = null }
    fun clearSuccess() { _success.value = null }

    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(date)
    }

    override fun refreshData() {
        // 基类统一刷新接口
    }
}
