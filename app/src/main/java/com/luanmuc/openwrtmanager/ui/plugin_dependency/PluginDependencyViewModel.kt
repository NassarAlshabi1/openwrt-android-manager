package com.luanmuc.openwrtmanager.ui.plugin_dependency

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.model.PluginDependencies
import com.luanmuc.openwrtmanager.data.model.PluginInstallStatus
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

class PluginDependencyViewModel(application: Application) : BaseViewModel(application) {

    data class UiState(
        val pluginStatuses: List<PluginInstallStatus> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val installingIndex: Int = -1,
        val installProgress: Int = 0,
        val installMessage: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        initNetworkMonitor()
        checkDependencies()
    }

    override fun refreshData() {
        checkDependencies()
    }

    fun checkDependencies() {

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val luci = LuciRepository.getInstance(getApplication())
                val statuses = luci.checkAllPluginDependencies()
                _uiState.value = _uiState.value.copy(
                    pluginStatuses = statuses,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: Loc.s(R.string.s0568),
                    isLoading = false
                )
            }
        }
    }

    fun installPlugin(index: Int) {
        val status = _uiState.value.pluginStatuses.getOrNull(index) ?: return
        if (status.isInstalled) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(installingIndex = index, installProgress = 0, installMessage = "")
                val luci = LuciRepository.getInstance(getApplication())
                val success = luci.installPluginDependency(status.dependency) { progress, message ->
                    _uiState.value = _uiState.value.copy(installProgress = progress, installMessage = message)
                }
                if (success) {
                    val newStatus = luci.checkPluginDependency(status.dependency)
                    val newStatuses = _uiState.value.pluginStatuses.toMutableList()
                    newStatuses[index] = newStatus
                    _uiState.value = _uiState.value.copy(pluginStatuses = newStatuses)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(installMessage = Loc.s(R.string.s0147, e.message))
            } finally {
                _uiState.value = _uiState.value.copy(installingIndex = -1)
            }
        }
    }
}
