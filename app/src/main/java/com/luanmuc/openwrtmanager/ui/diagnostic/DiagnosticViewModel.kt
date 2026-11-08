package com.luanmuc.openwrtmanager.ui.diagnostic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.model.RouterStatus
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import com.luanmuc.openwrtmanager.util.NetworkDiagnostic
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 智能诊断 ViewModel
 */
class DiagnosticViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    data class DiagnosticUiState(
        val isRunning: Boolean = false,
        val result: NetworkDiagnostic.DiagnosticResult? = null,
        val error: String? = null,
        val hasRouter: Boolean = false,
        val currentStep: String = ""
    )

    init {
        initNetworkMonitor()
        observeRouters()
    }

    private fun observeRouters() {
        viewModelScope.launch {
            routerRepository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())
            }
        }
    }

    fun runDiagnostic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRunning = true,
                error = null,
                result = null,
                currentStep = Loc.s(R.string.s0379)
            )
            try {
                // 调试模式：使用假数据

                val activeRouter = getActiveRouter()
                if (activeRouter != null) {
                    val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                    if (!luciRepository.isLoggedIn()) {
                        luciRepository.login(activeRouter.address, activeRouter.username, password)
                    }

                    // 获取系统信息
                    _uiState.value = _uiState.value.copy(currentStep = Loc.s(R.string.s0396))
                    val routerStatus = luciRepository.getRouterStatus()

                    // 运行诊断
                    _uiState.value = _uiState.value.copy(currentStep = Loc.s(R.string.s0397))
                    val result = NetworkDiagnostic.runFullDiagnostic(routerStatus)

                    _uiState.value = _uiState.value.copy(
                        isRunning = false,
                        result = result,
                        currentStep = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    error = e.message ?: Loc.s(R.string.s0398),
                    currentStep = ""
                )
            }
        }
    }

    private suspend fun getActiveRouter(): Router? {
        val routers = routerRepository.getRoutersList()
        val activeId = routerRepository.getActiveRouterId()
        return if (activeId != null) {
            routers.find { it.id == activeId } ?: routers.firstOrNull()
        } else {
            routers.firstOrNull()
        }
    }
    
    override fun refreshData() {
        // 诊断页面不需要自动刷新
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
