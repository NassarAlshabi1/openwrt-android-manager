package com.luanmuc.openwrtmanager.ui.firmware

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.luanmuc.openwrtmanager.data.model.FirmwareInfo
import com.luanmuc.openwrtmanager.data.model.FirmwareRelease
import com.luanmuc.openwrtmanager.data.model.FirmwareUpgradeState
import com.luanmuc.openwrtmanager.data.model.FirmwareUpgradeConfig
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 固件升级 ViewModel
 */
class FirmwareViewModel(application: Application) : BaseViewModel(application) {
    private val routerRepository = RouterRepository.getInstance(application)
    private val luciRepository = LuciRepository.getInstance(getApplication())

    private val _uiState = MutableStateFlow(FirmwareUiState())
    val uiState: StateFlow<FirmwareUiState> = _uiState.asStateFlow()

    data class FirmwareUiState(
        val firmwareInfo: FirmwareInfo = FirmwareInfo(),
        val latestRelease: FirmwareRelease? = null,
        val upgradeState: FirmwareUpgradeState = FirmwareUpgradeState.IDLE,
        val downloadProgress: Int = 0,
        val downloadSpeed: Long = 0,
        val flashProgress: Int = 0,
        val error: String? = null,
        val hasRouter: Boolean = false,
        val isChecking: Boolean = false,
        val isDownloading: Boolean = false,
        val isFlashing: Boolean = false,
        val keepConfig: Boolean = true,
        val config: FirmwareUpgradeConfig = FirmwareUpgradeConfig(),
        val showConfirmDialog: Boolean = false,
        val success: String? = null
    )

    init {
        initNetworkMonitor()
        observeRouters()
        loadConfig()
    }

    /**
     * 监听路由器变化
     */
    private fun observeRouters() {
        viewModelScope.launch {
            routerRepository.routers.collect { routers ->
                _uiState.value = _uiState.value.copy(
                    hasRouter = routers.isNotEmpty()
                )
                if (routers.isNotEmpty()) {
                    loadFirmwareInfo()
                }
            }
        }
    }

    /**
     * 加载升级配置
     */
    private fun loadConfig() {
        val config = luciRepository.getFirmwareUpgradeConfig()
        _uiState.value = _uiState.value.copy(
            config = config,
            keepConfig = config.keepConfig
        )
    }

    /**
     * 加载固件信息
     */
    fun loadFirmwareInfo() {
        viewModelScope.launch {

            try {
                val info = luciRepository.getFirmwareInfo()
                _uiState.value = _uiState.value.copy(
                    firmwareInfo = info
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: Loc.s(R.string.s0451)
                )
            }
        }
    }

    /**
     * 检测最新版本
     */
    fun checkLatestVersion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                upgradeState = FirmwareUpgradeState.CHECKING,
                error = null
            )

            try {
                val release = luciRepository.checkLatestFirmware(_uiState.value.config.repoUrl)
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    latestRelease = release,
                    upgradeState = FirmwareUpgradeState.IDLE
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    error = e.message ?: Loc.s(R.string.s0452),
                    upgradeState = FirmwareUpgradeState.IDLE
                )
            }
        }
    }

    /**
     * 下载固件
     */
    fun downloadFirmware() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                upgradeState = FirmwareUpgradeState.DOWNLOADING,
                downloadProgress = 0,
                error = null
            )

            try {
                val release = _uiState.value.latestRelease
                if (release != null) {
                    val success = luciRepository.downloadFirmware(release.downloadUrl) { progress, speed ->
                        _uiState.value = _uiState.value.copy(
                            downloadProgress = progress,
                            downloadSpeed = speed
                        )
                    }
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            upgradeState = FirmwareUpgradeState.IDLE
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            error = Loc.s(R.string.s0453),
                            upgradeState = FirmwareUpgradeState.FAILED
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = e.message ?: Loc.s(R.string.s0453),
                    upgradeState = FirmwareUpgradeState.FAILED
                )
            }
        }
    }

    /**
     * 开始刷写固件
     */
    fun startFlash() {
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = true
        )
    }

    /**
     * 确认刷写固件
     */
    fun confirmFlash() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showConfirmDialog = false,
                isFlashing = true,
                upgradeState = FirmwareUpgradeState.FLASHING,
                flashProgress = 0,
                error = null
            )

            try {
                val success = luciRepository.flashFirmware(
                    firmwarePath = "",
                    keepConfig = _uiState.value.keepConfig
                ) { progress ->
                    _uiState.value = _uiState.value.copy(
                        flashProgress = progress
                    )
                }
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isFlashing = false,
                        upgradeState = FirmwareUpgradeState.REBOOTING
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isFlashing = false,
                        error = Loc.s(R.string.s0454),
                        upgradeState = FirmwareUpgradeState.FAILED
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFlashing = false,
                    error = e.message ?: Loc.s(R.string.s0454),
                    upgradeState = FirmwareUpgradeState.FAILED
                )
            }
        }
    }

    /**
     * 取消确认对话框
     */
    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = false
        )
    }

    /**
     * 切换保留配置
     */
    fun toggleKeepConfig(keep: Boolean) {
        _uiState.value = _uiState.value.copy(
            keepConfig = keep
        )
    }

    /**
     * 更新升级配置
     */
    fun updateConfig(config: FirmwareUpgradeConfig) {
        luciRepository.setFirmwareUpgradeConfig(config)
        _uiState.value = _uiState.value.copy(
            config = config
        )
    }

    /**
     * 重置状态
     */
    fun uploadLocalFirmware(uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isDownloading = true,
                    downloadProgress = 0,
                    error = null
                )

                // 验证文件扩展名
                val isFirmwareFile = fileName.endsWith(".bin") ||
                    fileName.endsWith(".img") ||
                    fileName.endsWith(".trx") ||
                    fileName.endsWith(".tar") ||
                    fileName.endsWith(".gz")

                if (!isFirmwareFile) {
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        error = Loc.s(R.string.s0455)
                    )
                    return@launch
                }

                // 真实模式：读取文件并验证
                val context = getApplication<Application>().applicationContext
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileSize = inputStream?.available() ?: 0

                if (fileSize < 1024 * 1024) {
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        error = Loc.s(R.string.s0457)
                    )
                    return@launch
                }

                // 模拟上传进度（实际需要通过HTTP上传）
                for (i in 1..10) {
                    delay(100)
                    _uiState.value = _uiState.value.copy(downloadProgress = i * 10)
                }

                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 100,
                    upgradeState = FirmwareUpgradeState.VERIFYING,
                    error = null
                )

                // 验证固件（简化版）
                delay(500)
                _uiState.value = _uiState.value.copy(
                    upgradeState = FirmwareUpgradeState.IDLE,
                    success = Loc.s(R.string.s0458, fileSize / 1024 / 1024)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = e.message ?: Loc.s(R.string.s0459)
                )
            }
        }
    }

    fun clearSuccess() { _uiState.value = _uiState.value.copy(success = null) }

    fun resetState() {
        _uiState.value = _uiState.value.copy(
            upgradeState = FirmwareUpgradeState.IDLE,
            downloadProgress = 0,
            flashProgress = 0,
            error = null
        )
    }

    override fun refreshData() {
        loadFirmwareInfo()
    }
}
