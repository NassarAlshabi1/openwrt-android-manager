package com.luanmuc.openwrtmanager.ui.advanced

import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

data class AdvancedUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRebooting: Boolean = false,
    val isShuttingDown: Boolean = false,
    val actionSuccess: Boolean = false
)

class AdvancedViewModel(application: Application) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow(AdvancedUiState())
    val uiState: StateFlow<AdvancedUiState> = _uiState.asStateFlow()

    private val repository = LuciRepository.getInstance(getApplication())

    fun reboot() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isRebooting = true, error = null)
            try {
                // 调试模式：模拟重启

                repository.reboot()
                _uiState.value = _uiState.value.copy(isLoading = false, isRebooting = false, actionSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, isRebooting = false, error = e.message ?: Loc.s(R.string.s0278))
            }
        }
    }

    fun shutdown() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isShuttingDown = true, error = null)
            try {
                // 调试模式：模拟关机

                repository.shutdown()
                _uiState.value = _uiState.value.copy(isLoading = false, isShuttingDown = false, actionSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, isShuttingDown = false, error = e.message ?: Loc.s(R.string.s0279))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(actionSuccess = false)
    }
    
    override fun refreshData() {
        // 高级功能页面不需要自动刷新
    }
}
