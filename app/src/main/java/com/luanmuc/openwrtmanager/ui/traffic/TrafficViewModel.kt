package com.luanmuc.openwrtmanager.ui.traffic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.model.DeviceTraffic
import com.luanmuc.openwrtmanager.data.model.TrafficPeriod
import com.luanmuc.openwrtmanager.data.model.TrafficStats
import com.luanmuc.openwrtmanager.data.repository.TrafficRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 流量统计ViewModel
 */
class TrafficViewModel(application: Application) : BaseViewModel(application) {
    
    private val trafficRepository = TrafficRepository.getInstance(application)
    private val routerRepository = RouterRepository.getInstance(application)
    
    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 流量统计数据
    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()
    
    // 当前选中的周期
    private val _selectedPeriod = MutableStateFlow(TrafficPeriod.TODAY)
    val selectedPeriod: StateFlow<TrafficPeriod> = _selectedPeriod.asStateFlow()
    
    // 设备流量排行
    private val _deviceRanking = MutableStateFlow<List<DeviceTraffic>>(emptyList())
    val deviceRanking: StateFlow<List<DeviceTraffic>> = _deviceRanking.asStateFlow()
    
    // 当前活动路由器ID
    private var routerId: String = ""
    
    init {
        initNetworkMonitor()
        loadRouterId()
        loadTrafficData()
    }

    private fun loadRouterId() {
        viewModelScope.launch {
            try {
                val routers = routerRepository.getRoutersList()
                val activeId = routerRepository.getActiveRouterId()
                val active = routers.find { it.id == activeId } ?: routers.firstOrNull()
                active?.let { routerId = it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun loadTrafficData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // 加载流量统计
                val stats = trafficRepository.getTrafficStats(routerId)
                _trafficStats.value = stats
                
                // 加载设备流量排行
                val ranking = trafficRepository.getDeviceTrafficRanking(routerId)
                _deviceRanking.value = ranking
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setSelectedPeriod(period: TrafficPeriod) {
        _selectedPeriod.value = period
    }
    
    /**
     * 格式化字节数为可读字符串
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    override fun refreshData() {
        // 基类统一刷新接口
    }
}
