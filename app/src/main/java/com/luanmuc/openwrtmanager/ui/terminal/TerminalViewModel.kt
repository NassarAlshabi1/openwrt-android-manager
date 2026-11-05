package com.luanmuc.openwrtmanager.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.model.CommandHistory
import com.luanmuc.openwrtmanager.data.model.QuickCommand
import com.luanmuc.openwrtmanager.data.model.TerminalConfig
import com.luanmuc.openwrtmanager.data.repository.TerminalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 终端ViewModel
 */
class TerminalViewModel(application: Application) : BaseViewModel(application) {
    
    private val terminalRepository = TerminalRepository.getInstance(application)
    
    // 终端配置
    private val _terminalConfig = MutableStateFlow(TerminalConfig())
    val terminalConfig: StateFlow<TerminalConfig> = _terminalConfig.asStateFlow()
    
    // 命令历史
    private val _commandHistory = MutableStateFlow<List<CommandHistory>>(emptyList())
    val commandHistory: StateFlow<List<CommandHistory>> = _commandHistory.asStateFlow()
    
    // 快捷命令
    private val _quickCommands = MutableStateFlow<List<QuickCommand>>(emptyList())
    val quickCommands: StateFlow<List<QuickCommand>> = _quickCommands.asStateFlow()
    
    // 终端输出
    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()
    
    // 当前输入
    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()
    
    // 是否正在执行命令
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()
    
    // 是否显示快捷命令面板
    private val _showQuickCommands = MutableStateFlow(false)
    val showQuickCommands: StateFlow<Boolean> = _showQuickCommands.asStateFlow()
    
    // 是否显示设置面板
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()
    
    init {
        initNetworkMonitor()
        loadTerminalData()
    }
    
    private fun loadTerminalData() {
        viewModelScope.launch {
            try {
                // 加载终端配置
                val config = terminalRepository.getTerminalConfig()
                _terminalConfig.value = config
                
                // 加载命令历史
                val history = terminalRepository.getCommandHistory(50)
                _commandHistory.value = history
                
                // 加载快捷命令
                val quickCommands = terminalRepository.getQuickCommands()
                _quickCommands.value = quickCommands
                
                // 初始化终端输出
                _terminalOutput.value = Loc.s(R.string.s0770)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 设置当前输入
     */
    fun setCurrentInput(input: String) {
        _currentInput.value = input
    }
    
    /**
     * 执行命令
     */
    fun executeCommand() {
        val command = _currentInput.value.trim()
        if (command.isEmpty() || _isExecuting.value) return
        
        viewModelScope.launch {
            _isExecuting.value = true
            
            try {
                // 添加命令到输出
                _terminalOutput.value += "root@OpenWrt:~# $command\n"
                
                // 执行命令
                val result = terminalRepository.executeCommand(command)
                
                // 添加输出
                _terminalOutput.value += "${result.output}\n"
                
                // 清空输入
                _currentInput.value = ""
                
                // 刷新历史
                val history = terminalRepository.getCommandHistory(50)
                _commandHistory.value = history
                
            } catch (e: Exception) {
                _terminalOutput.value += Loc.s(R.string.s0771, e.message)
            } finally {
                _isExecuting.value = false
            }
        }
    }
    
    /**
     * 执行快捷命令
     */
    fun executeQuickCommand(command: String) {
        _currentInput.value = command
        executeCommand()
        _showQuickCommands.value = false
    }
    
    /**
     * 切换快捷命令面板
     */
    fun toggleQuickCommands() {
        _showQuickCommands.value = !_showQuickCommands.value
        if (_showQuickCommands.value) {
            _showSettings.value = false
        }
    }
    
    /**
     * 切换设置面板
     */
    fun toggleSettings() {
        _showSettings.value = !_showSettings.value
        if (_showSettings.value) {
            _showQuickCommands.value = false
        }
    }
    
    /**
     * 清空终端
     */
    fun clearTerminal() {
        _terminalOutput.value = ""
    }
    
    /**
     * 清空命令历史
     */
    fun clearCommandHistory() {
        viewModelScope.launch {
            try {
                terminalRepository.clearCommandHistory()
                _commandHistory.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 更新终端配置
     */
    fun updateTerminalConfig(config: TerminalConfig) {
        viewModelScope.launch {
            try {
                terminalRepository.saveTerminalConfig(config)
                _terminalConfig.value = config
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(date)
    }

    override fun refreshData() {
        // 基类统一刷新接口
    }
}
