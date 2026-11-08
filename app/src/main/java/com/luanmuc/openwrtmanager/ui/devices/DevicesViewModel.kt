package com.luanmuc.openwrtmanager.ui.devices

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository

/**
 * 设备页 ViewModel
 */
class DevicesViewModel(application: Application) : BaseViewModel(application) {
    private val repository = RouterRepository.getInstance(application)

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    data class DevicesUiState(
        val routers: List<Router> = emptyList(),
        val activeRouterId: String? = null,
        val isLoading: Boolean = true
    )

    init {
        initNetworkMonitor()
        loadRouters()
    }

    private fun loadRouters() {
        viewModelScope.launch {
            repository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(
                    routers = routers,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            repository.activeRouterId.collect { id ->
                _uiState.value = _uiState.value.copy(activeRouterId = id)
            }
        }
    }

    fun setActiveRouter(routerId: String) {
        viewModelScope.launch {
            // 切换路由器时清除旧路由器的缓存
            val oldRouterId = _uiState.value.activeRouterId
            if (oldRouterId != null && oldRouterId != routerId) {
                try {
                    CacheRepository.getInstance(getApplication()).clearRouterCache(oldRouterId)
                } catch (_: Exception) {}
            }
            repository.setActiveRouter(routerId)
            // 切换路由器时，更新LuciRepository地址并重新登录
            try {
                val router = repository.getRouterById(routerId)
                if (router != null) {
                    val luci = LuciRepository.getInstance(getApplication())
                    luci.logout()
                    luci.login(router.address, router.username, router.encryptedPassword)
                }
            } catch (e: Exception) {
                // 登录失败不影响切换，后续操作会自动重试
            }
        }
    }

    fun deleteRouter(routerId: String) {
        viewModelScope.launch {
            repository.removeRouter(routerId)
            if (_uiState.value.activeRouterId == routerId) {
                repository.setActiveRouter(null)
            }
        }
    }

    override fun refreshData() {
        loadRouters()
    }
}
