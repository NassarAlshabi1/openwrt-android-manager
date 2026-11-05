package com.luanmuc.openwrtmanager.ui.devicemanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.model.DeviceGroup
import com.luanmuc.openwrtmanager.data.model.DeviceHistory
import com.luanmuc.openwrtmanager.data.model.DeviceNote
import com.luanmuc.openwrtmanager.data.repository.DeviceManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 设备管理增强ViewModel
 */
class DeviceManagerViewModel(application: Application) : BaseViewModel(application) {
    
    private val deviceManagerRepository = DeviceManagerRepository.getInstance(application)
    
    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 设备分组列表
    private val _deviceGroups = MutableStateFlow<List<DeviceGroup>>(emptyList())
    val deviceGroups: StateFlow<List<DeviceGroup>> = _deviceGroups.asStateFlow()
    
    // 设备上下线历史
    private val _deviceHistory = MutableStateFlow<List<DeviceHistory>>(emptyList())
    val deviceHistory: StateFlow<List<DeviceHistory>> = _deviceHistory.asStateFlow()
    
    // 操作结果
    private val _operationResult = MutableStateFlow<String?>(null)
    val operationResult: StateFlow<String?> = _operationResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        initNetworkMonitor()
        loadDeviceManagerData()
    }
    
    fun loadDeviceManagerData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // 加载设备分组
                val groups = deviceManagerRepository.getDeviceGroups()
                _deviceGroups.value = groups
                
                // 加载设备历史
                val history = deviceManagerRepository.getDeviceHistory(limit = 50)
                _deviceHistory.value = history
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 拉黑设备
     */
    fun blockDevice(mac: String) {
        viewModelScope.launch {
            try {
                val success = deviceManagerRepository.blockDevice(mac)
                _operationResult.value = if (success) Loc.s(R.string.s0345) else Loc.s(R.string.s0346)
                loadDeviceManagerData()
            } catch (e: Exception) {
                _operationResult.value = Loc.s(R.string.s0347, e.message)
            }
        }
    }
    
    /**
     * 解除拉黑
     */
    fun unblockDevice(mac: String) {
        viewModelScope.launch {
            try {
                val success = deviceManagerRepository.unblockDevice(mac)
                _operationResult.value = if (success) Loc.s(R.string.s0348) else Loc.s(R.string.s0349)
                loadDeviceManagerData()
            } catch (e: Exception) {
                _operationResult.value = Loc.s(R.string.s0347, e.message)
            }
        }
    }
    
    /**
     * 清除操作结果
     */
    fun clearError() { _error.value = null }

    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(date)
    }

    override fun refreshData() {
        // 基类统一刷新接口
    }
}
