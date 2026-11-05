package com.luanmuc.openwrtmanager.ui.diagnostic_enhanced

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.model.DiagnosticCategory
import com.luanmuc.openwrtmanager.data.model.DiagnosticStatus
import com.luanmuc.openwrtmanager.data.model.DiagnosticSuggestion
import com.luanmuc.openwrtmanager.data.model.FullDiagnosticResult
import com.luanmuc.openwrtmanager.data.repository.DiagnosticRepository
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 智能诊断增强ViewModel
 */
class DiagnosticEnhancedViewModel(application: Application) : BaseViewModel(application) {
    
    private val diagnosticRepository = DiagnosticRepository.getInstance(application)
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 是否正在体检
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    // 全面体检结果
    private val _diagnosticResult = MutableStateFlow<FullDiagnosticResult?>(null)
    val diagnosticResult: StateFlow<FullDiagnosticResult?> = _diagnosticResult.asStateFlow()
    
    // 优化建议
    private val _suggestions = MutableStateFlow<List<DiagnosticSuggestion>>(emptyList())
    val suggestions: StateFlow<List<DiagnosticSuggestion>> = _suggestions.asStateFlow()
    
    // 当前选中的分类
    private val _selectedCategory = MutableStateFlow<DiagnosticCategory?>(null)
    val selectedCategory: StateFlow<DiagnosticCategory?> = _selectedCategory.asStateFlow()
    
    // 操作结果
    private val _operationResult = MutableStateFlow<String?>(null)
    val operationResult: StateFlow<String?> = _operationResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        initNetworkMonitor()
        loadLastResult()
    }
    
    private fun loadLastResult() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 加载优化建议
                val suggestions = diagnosticRepository.getOptimizationSuggestions()
                _suggestions.value = suggestions
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 开始全面体检
     */
    fun startFullDiagnostic() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = diagnosticRepository.runFullDiagnostic()
                _diagnosticResult.value = result
                
                // 更新建议
                _suggestions.value = result.suggestions
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    /**
     * 一键修复
     */
    fun autoFix(suggestionId: String) {
        viewModelScope.launch {
            try {
                val success = diagnosticRepository.autoFix(suggestionId)
                _operationResult.value = if (success) Loc.s(R.string.s0406) else Loc.s(R.string.s0407)
                // 重新体检
                startFullDiagnostic()
            } catch (e: Exception) {
                _operationResult.value = Loc.s(R.string.s0408, e.message)
            }
        }
    }
    
    /**
     * 设置选中的分类
     */
    fun setSelectedCategory(category: DiagnosticCategory?) {
        _selectedCategory.value = category
    }
    
    /**
     * 清除操作结果
     */
    fun clearError() { _error.value = null }

    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    /**
     * 获取状态对应的颜色
     */
    fun getStatusColor(status: DiagnosticStatus): androidx.compose.ui.graphics.Color {
        return when (status) {
            DiagnosticStatus.GOOD -> MiTheme.Success
            DiagnosticStatus.WARNING -> MiTheme.Warning
            DiagnosticStatus.ERROR -> MiTheme.Error
            DiagnosticStatus.CHECKING -> MiTheme.Primary
        }
    }

    override fun refreshData() {
        // 基类统一刷新接口
    }
}
