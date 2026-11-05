package com.luanmuc.openwrtmanager.ui.storage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.data.model.RouterStatus
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.data.repository.RouterRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.DebugMode
import com.luanmuc.openwrtmanager.util.EncryptionUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

data class StorageUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasRouter: Boolean = false,
    val isOfflineMode: Boolean = false,
    val isFromCache: Boolean = false,
    val storageTotal: Long = 0,
    val storageUsed: Long = 0,
    val storageFree: Long = 0,
    val memoryTotal: Long = 0,
    val memoryUsed: Long = 0,
    val memoryFree: Long = 0,
    val memoryCached: Long = 0,
    val memoryBuffered: Long = 0,
    val uptime: Long = 0,
    val loadAverage: List<Float> = listOf(0f, 0f, 0f),
    val cpuUsage: Float = 0f,
    val temperature: Float? = null,
    val mountPoints: List<MountPointInfo> = emptyList()
)

data class MountPointInfo(
    val mountPoint: String,
    val device: String,
    val filesystem: String,
    val total: Long,
    val used: Long,
    val free: Long,
    val usedPercent: Float
)

class StorageViewModel(application: Application) : BaseViewModel(application) {
    private val luciRepository = LuciRepository.getInstance(application)
    private val routerRepository = RouterRepository.getInstance(application)
    private val cacheRepository = CacheRepository.getInstance(application)

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        initNetworkMonitor()
        observeRouters()
    }

    override fun refreshData() {
        loadStorageInfo()
    }

    private fun observeRouters() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                routerRepository.routers,
                DebugMode.isDebugModeFlow
            ) { routers, isDebugMode ->
                Pair(routers, isDebugMode)
            }.collect { (routers, isDebugMode) ->
                _uiState.value = _uiState.value.copy(hasRouter = routers.isNotEmpty())

                if (isDebugMode && _uiState.value.isFromCache) {
                    _uiState.value = _uiState.value.copy(
                        storageTotal = 0,
                        storageUsed = 0,
                        storageFree = 0,
                        isFromCache = false
                    )
                }

                val shouldLoad = if (isDebugMode) {
                    _uiState.value.storageTotal == 0L
                } else {
                    routers.isNotEmpty() && _uiState.value.storageTotal == 0L
                }
                if (shouldLoad) {
                    loadFromCache()
                    loadStorageInfo()
                }
            }
        }
    }

    private fun loadFromCache() {
        viewModelScope.launch {
            try {
                if (DebugMode.isDebugMode) return@launch

                val activeRouter = getActiveRouter() ?: return@launch

                val cachedStatus = cacheRepository.getCacheEvenExpired(
                    CacheRepository.KEY_ROUTER_STATUS,
                    activeRouter.id,
                    RouterStatus::class.java
                )

                if (cachedStatus != null) {
                    _uiState.value = _uiState.value.copy(
                        storageTotal = cachedStatus.storageTotal,
                        storageUsed = cachedStatus.storageUsed,
                        storageFree = cachedStatus.storageFree,
                        memoryTotal = cachedStatus.memoryTotal,
                        memoryUsed = cachedStatus.memoryUsed,
                        memoryFree = cachedStatus.memoryFree,
                        memoryCached = cachedStatus.memoryCached,
                        memoryBuffered = cachedStatus.memoryBuffered,
                        uptime = cachedStatus.uptime,
                        loadAverage = cachedStatus.loadAverage,
                        cpuUsage = cachedStatus.cpuUsage,
                        temperature = cachedStatus.temperature,
                        isFromCache = true,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun getActiveRouter(): com.luanmuc.openwrtmanager.data.model.Router? {
        val routers = routerRepository.getRoutersList()
        val activeId = routerRepository.getActiveRouterId()
        return if (activeId != null) {
            routers.find { it.id == activeId } ?: routers.firstOrNull()
        } else {
            routers.firstOrNull()
        }
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (DebugMode.isDebugMode) {
                    DebugMode.simulateDelay(800)
                    val fakeStatus = DebugMode.getFakeRouterStatus()
                    val fakeMounts = DebugMode.getFakeMountPoints()
                    _uiState.value = _uiState.value.copy(
                        storageTotal = fakeStatus.storageTotal,
                        storageUsed = fakeStatus.storageUsed,
                        storageFree = fakeStatus.storageFree,
                        memoryTotal = fakeStatus.memoryTotal,
                        memoryUsed = fakeStatus.memoryUsed,
                        memoryFree = fakeStatus.memoryFree,
                        memoryCached = fakeStatus.memoryCached,
                        memoryBuffered = fakeStatus.memoryBuffered,
                        uptime = fakeStatus.uptime,
                        loadAverage = fakeStatus.loadAverage,
                        cpuUsage = fakeStatus.cpuUsage,
                        temperature = fakeStatus.temperature,
                        mountPoints = fakeMounts,
                        isLoading = false,
                        isFromCache = false,
                        isOfflineMode = false,
                        error = null
                    )
                    return@launch
                }

                val activeRouter = getActiveRouter() ?: return@launch
                val password = EncryptionUtil.decrypt(activeRouter.encryptedPassword)
                if (!luciRepository.isLoggedIn()) {
                    luciRepository.login(activeRouter.address, activeRouter.username, password)
                }

                val status = luciRepository.getRouterStatus()
                val mountPoints = try {
                    luciRepository.getMountPoints()
                } catch (e: Exception) {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    storageTotal = status.storageTotal,
                    storageUsed = status.storageUsed,
                    storageFree = status.storageFree,
                    memoryTotal = status.memoryTotal,
                    memoryUsed = status.memoryUsed,
                    memoryFree = status.memoryFree,
                    memoryCached = status.memoryCached,
                    memoryBuffered = status.memoryBuffered,
                    uptime = status.uptime,
                    loadAverage = status.loadAverage,
                    cpuUsage = status.cpuUsage,
                    temperature = status.temperature,
                    mountPoints = mountPoints,
                    isLoading = false,
                    isFromCache = false,
                    isOfflineMode = false,
                    error = null
                )

                cacheRepository.saveCache(
                    CacheRepository.KEY_ROUTER_STATUS,
                    activeRouter.id,
                    "RouterStatus",
                    status
                )
            } catch (e: Exception) {
                handleLoadError(e.message ?: Loc.s(R.string.s0740))
            }
        }
    }

    private fun handleLoadError(errorMsg: String) {
        viewModelScope.launch {
            if (_uiState.value.storageTotal > 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOfflineMode = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
