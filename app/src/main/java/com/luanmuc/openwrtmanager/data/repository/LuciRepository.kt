package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.luanmuc.openwrtmanager.data.api.LuciApiService
import com.luanmuc.openwrtmanager.data.api.RetrofitClient
import com.luanmuc.openwrtmanager.data.model.*
import kotlinx.coroutines.*
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc
import com.luanmuc.openwrtmanager.util.OpenWrtVersions

/**
 * LuCI API 仓库
 * 封装所有与 OpenWrt LuCI ubus RPC 的交互
 * 支持 ImmortalWrt / OpenWrt 21.02+ 新版 LuCI
 * 
 * Session自动续期特性：
 * - 登录成功记录session过期时间
 * - 每次请求前检查，快过期自动刷新
 * - 过期了自动重新登录，用户无感知
 * - APP重启自动恢复session
 * - 前台定时自动续期
 */
class LuciRepository {
    private lateinit var context: Context
    private var authToken: String = ""
    private var currentAddress: String = ""
    private var currentUsername: String = ""
    private var currentPassword: String = ""
    
    // Session过期时间相关
    private var sessionExpireTime: Long = 0L  // session过期时间戳（毫秒）
    private var sessionExpiresIn: Long = TimeUnit.MINUTES.toMillis(30)  // session有效期，默认30分钟
    private val sessionRefreshThreshold = TimeUnit.MINUTES.toMillis(5)  // 快过期阈值：剩余5分钟时刷新
    
    // Session重试相关
    private val maxRefreshRetries = 3  // 最大重试次数
    private var currentRefreshRetryCount = 0  // 当前重试次数
    private val baseRetryDelay = 2000L  // 基础重试延迟（毫秒）
    
    // 续期失败状态
    private var isRefreshFailed = false  // 续期是否失败
    private var lastRefreshError: String? = null  // 最后一次续期错误
    
    // 回调
    var onSessionExpired: (() -> Unit)? = null  // session过期回调
    var onRefreshFailed: ((String) -> Unit)? = null  // 续期失败回调
    
    // 自动续期相关
    private var autoRefreshJob: Job? = null
    private var isAutoRefreshRunning = false
    
    // 持久化相关
    private var prefs: SharedPreferences? = null
    private val PREFS_NAME = "luci_session_prefs"
    private val KEY_AUTH_TOKEN = "auth_token"
    private val KEY_ADDRESS = "address"
    private val KEY_USERNAME = "username"
    private val KEY_PASSWORD = "password"
    private val KEY_EXPIRE_TIME = "expire_time"
    private val KEY_EXPIRES_IN = "expires_in"
    
    // 重试限制
    private val MAX_RELOGIN_RETRY = 3
    
    // 单例
    companion object {
        @Volatile
        private var instance: LuciRepository? = null
        
        fun getInstance(context: Context? = null): LuciRepository {
            return instance ?: synchronized(this) {
                instance ?: LuciRepository().also { 
                    instance = it
                    context?.let { ctx -> it.init(ctx) }
                }
            }
        }
    }
    
    /**
     * 初始化，加载持久化的session
     */
    fun init(context: Context) {
        this.context = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSessionFromPrefs()
        // 如果恢复的session有效，启动自动续期
        if (isSessionValid()) {
            startAutoRefresh()
        }
    }
    
    /**
     * 从SharedPreferences加载session
     */
    private fun loadSessionFromPrefs() {
        prefs?.let {
            authToken = it.getString(KEY_AUTH_TOKEN, "") ?: ""
            currentAddress = it.getString(KEY_ADDRESS, "") ?: ""
            currentUsername = it.getString(KEY_USERNAME, "") ?: ""
            currentPassword = it.getString(KEY_PASSWORD, "") ?: ""
            sessionExpireTime = it.getLong(KEY_EXPIRE_TIME, 0L)
            sessionExpiresIn = it.getLong(KEY_EXPIRES_IN, TimeUnit.MINUTES.toMillis(30))
        }
    }
    
    /**
     * 保存session到SharedPreferences
     */
    private fun saveSessionToPrefs() {
        prefs?.edit()?.apply {
            putString(KEY_AUTH_TOKEN, authToken)
            putString(KEY_ADDRESS, currentAddress)
            putString(KEY_USERNAME, currentUsername)
            putString(KEY_PASSWORD, currentPassword)
            putLong(KEY_EXPIRE_TIME, sessionExpireTime)
            putLong(KEY_EXPIRES_IN, sessionExpiresIn)
            apply()
        }
    }
    
    /**
     * 清除持久化的session
     */
    private fun clearSessionPrefs() {
        prefs?.edit()?.clear()?.apply()
    }
    
    /**
     * 检查session是否有效
     */
    fun isSessionValid(): Boolean {
        return authToken.isNotEmpty() && System.currentTimeMillis() < sessionExpireTime
    }
    
    /**
     * 检查session是否快过期
     */
    private fun isSessionExpiringSoon(): Boolean {
        val remainingTime = sessionExpireTime - System.currentTimeMillis()
        return authToken.isNotEmpty() && remainingTime in 0..sessionRefreshThreshold
    }
    
    /**
     * 检查并刷新session（如果快过期的话）
     */
    private suspend fun checkAndRefreshSession() {
        if (isSessionExpiringSoon()) {
            try {
                refreshSession()
            } catch (e: Exception) {
                // 刷新失败，忽略，等真正过期了再重新登录
            }
        }
    }
    
    /**
     * 刷新session（通过重新登录）
     * 支持指数退避重试
     */
    private suspend fun refreshSession() {
        if (currentUsername.isEmpty() || currentPassword.isEmpty()) {
            return
        }
        
        currentRefreshRetryCount = 0
        while (currentRefreshRetryCount < maxRefreshRetries) {
            try {
                login(currentAddress, currentUsername, currentPassword)
                // 刷新成功，重置重试计数
                currentRefreshRetryCount = 0
                isRefreshFailed = false
                lastRefreshError = null
                return
            } catch (e: Exception) {
                currentRefreshRetryCount++
                if (currentRefreshRetryCount < maxRefreshRetries) {
                    // 指数退避重试
                    val delay = baseRetryDelay * (1 shl (currentRefreshRetryCount - 1))
                    delay(delay)
                }
            }
        }
        
        // 所有重试都失败了
        isRefreshFailed = true
        lastRefreshError = Loc.s(R.string.s0101)
        onRefreshFailed?.invoke(lastRefreshError ?: Loc.s(R.string.s0102))
        // 清除无效session
        authToken = ""
        sessionExpireTime = 0L
        onSessionExpired?.invoke()
    }
    
    /**
     * 启动自动续期
     */
    fun startAutoRefresh() {
        if (isAutoRefreshRunning || autoRefreshJob?.isActive == true) {
            return
        }
        
        isAutoRefreshRunning = true
        autoRefreshJob = CoroutineScope(Dispatchers.IO).launch {
            while (isAutoRefreshRunning && isLoggedIn()) {
                try {
                    delay(TimeUnit.MINUTES.toMillis(10))  // 每10分钟检查一次
                    if (isSessionExpiringSoon()) {
                        refreshSession()
                    }
                } catch (e: Exception) {
                    // 忽略错误，继续循环
                }
            }
        }
    }
    
    /**
     * 停止自动续期
     */
    fun stopAutoRefresh() {
        isAutoRefreshRunning = false
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    // ========== 认证相关 ==========

    /**
     * 登录认证
     */
    // opkg是否可用（用于检测包管理器）
    private var opkgAvailable: Boolean = true
    private var opkgChecked: Boolean = false
    
    /**
     * 检测opkg是否可用
     */
    suspend fun checkLuciRpcAvailable(): Boolean {
        if (opkgChecked) return opkgAvailable
        
        try {
            val output = executeCommand("opkg --version")
            opkgAvailable = output != null && output.isNotEmpty()
        } catch (e: Exception) {
            opkgAvailable = false
        }
        
        opkgChecked = true
        return opkgAvailable
    }
    
    suspend fun login(address: String, username: String, password: String): String {
        currentAddress = normalizeAddress(address)
        currentUsername = username
        currentPassword = password

        val api = RetrofitClient.getApi(currentAddress)
        try {
            val request = LuciRpcRequest.loginRequest(username, password)
            val response = api.call(LuciApiService.UBUS_PATH, request)

            if (response.error != null) {
                throw LuciException(
                    code = response.error.code,
                    message = response.error.message
                )
            }

            val result = response.result
            if (result == null || result.size < 2) {
                throw LuciException(message = Loc.s(R.string.s0103))
            }

            val statusCode = (result[0] as? Number)?.toInt() ?: -1
            if (statusCode != 0) {
                when (statusCode) {
                    -32000, -32002 -> throw LuciException(
                        code = statusCode,
                        message = Loc.s(R.string.s0104),
                        type = ErrorType.AUTH_FAILED
                    )
                    else -> throw LuciException(
                        code = statusCode,
                        message = Loc.s(R.string.s0105, statusCode),
                        type = ErrorType.AUTH_FAILED
                    )
                }
            }

            val data = result[1] as? Map<*, *>
            authToken = data?.get("ubus_rpc_session")?.toString() ?: ""
            if (authToken.isEmpty()) {
                throw LuciException(message = Loc.s(R.string.s0106), type = ErrorType.AUTH_FAILED)
            }
            
            // 记录session过期时间
            // 尝试从响应中获取过期时间，如果没有则使用默认值
            val expires = data?.get("expires") as? Number
            if (expires != null) {
                sessionExpiresIn = TimeUnit.SECONDS.toMillis(expires.toLong())
            }
            sessionExpireTime = System.currentTimeMillis() + sessionExpiresIn
            
            // 保存到SharedPreferences
            saveSessionToPrefs()
            
            // 启动自动续期
            startAutoRefresh()
            
            return authToken
        } catch (e: Exception) {
            throw wrapException(e)
        }
    }

    /**
     * 调用 ubus 方法
     */
    private suspend fun callUbus(
        obj: String,
        method: String,
        params: Map<String, Any> = emptyMap(),
        retryCount: Int = 0
    ): Map<String, Any> {
        // 请求前检查session是否快过期，快过期则自动刷新
        checkAndRefreshSession()
        
        val api = RetrofitClient.getApi(currentAddress)
        try {
            val request = LuciRpcRequest.callRequest(authToken, obj, method, params)
            val response = api.call(LuciApiService.UBUS_PATH, request)

            if (response.error != null) {
                if ((response.error.code == -32002 || response.error.message?.contains("session", true) == true) && retryCount < MAX_RELOGIN_RETRY) {
                    reLogin()
                    return callUbus(obj, method, params, retryCount + 1)
                }
                throw LuciException(
                    code = response.error.code,
                    message = response.error.message
                )
            }

            val result = response.result
            if (result == null || result.size < 2) {
                throw LuciException(message = Loc.s(R.string.s0103))
            }

            val statusCode = (result[0] as? Number)?.toInt() ?: -1
            if (statusCode != 0) {
                if ((statusCode == -32002 || statusCode == -6) && retryCount < MAX_RELOGIN_RETRY) {
                    reLogin()
                    return callUbus(obj, method, params, retryCount + 1)
                }
                throw LuciException(
                    code = statusCode,
                    message = Loc.s(R.string.s0107, statusCode)
                )
            }

            @Suppress("UNCHECKED_CAST")
            return result[1] as? Map<String, Any> ?: emptyMap()
        } catch (e: Exception) {
            throw wrapException(e)
        }
    }

    /**
     * 重新登录
     */
    private suspend fun reLogin() {
        if (currentUsername.isNotEmpty()) {
            authToken = ""
            login(currentAddress, currentUsername, currentPassword)
        }
    }

    // ========== 系统信息 ==========

    /**
     * 获取系统信息
     */
    suspend fun getSystemInfo(): Map<String, Any> {
        return try {
            callUbus("system", "info")
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 获取板载信息
     */
    suspend fun getBoardInfo(): Map<String, Any> {
        return try {
            callUbus("system", "board")
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 获取路由器状态
     */
    suspend fun getRouterStatus(): RouterStatus {
        val sysInfo = getSystemInfo()
        val boardInfo = getBoardInfo()

        val hostname = boardInfo["hostname"]?.toString()
            ?: sysInfo["hostname"]?.toString()
            ?: "OpenWrt"
        val model = boardInfo["model"]?.toString()
            ?: sysInfo["model"]?.toString()
            ?: "Unknown"
        // release是Map，包含distribution、version、revision等字段
        val releaseMap = boardInfo["release"] as? Map<*, *>
        val release = if (releaseMap != null) {
            "${releaseMap["distribution"]} ${releaseMap["version"]}"
        } else {
            boardInfo["release"]?.toString() ?: "Unknown"
        }
        val kernel = boardInfo["kernel"]?.toString() ?: "Unknown"
        val uptime = (sysInfo["uptime"] as? Number)?.toLong() ?: 0L

        val loadList = sysInfo["load"] as? List<*>
        val loadAverage = loadList?.mapNotNull {
            (it as? Number)?.toFloat()
        } ?: listOf(0f, 0f, 0f)

        val memory = sysInfo["memory"] as? Map<*, *>
        val memoryTotal = (memory?.get("total") as? Number)?.toLong() ?: 0L
        val memoryFree = (memory?.get("free") as? Number)?.toLong() ?: 0L
        val memoryCached = (memory?.get("cached") as? Number)?.toLong() ?: 0L
        val memoryBuffered = (memory?.get("buffered") as? Number)?.toLong() ?: 0L
        val memoryUsed = memoryTotal - memoryFree - memoryCached - memoryBuffered

        // 存储信息通过df命令获取根分区
        var storageTotal = 0L
        var storageFree = 0L
        var storageUsed = 0L
        try {
            val dfOutput = executeCommand("df -k /")
            dfOutput?.lines()?.getOrNull(1)?.let { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 4) {
                    storageTotal = (parts[1].toLongOrNull() ?: 0L) * 1024
                    storageUsed = (parts[2].toLongOrNull() ?: 0L) * 1024
                    storageFree = (parts[3].toLongOrNull() ?: 0L) * 1024
                }
            }
        } catch (e: Exception) {
            // 忽略，使用默认值0
        }

        val cpuUsage = calculateCpuUsage(sysInfo)

        return RouterStatus(
            hostname = hostname,
            model = model,
            firmware = release,
            kernel = kernel,
            uptime = uptime,
            loadAverage = loadAverage,
            memoryTotal = memoryTotal,
            memoryFree = memoryFree,
            memoryUsed = memoryUsed,
            memoryCached = memoryCached,
            memoryBuffered = memoryBuffered,
            storageTotal = storageTotal,
            storageFree = storageFree,
            storageUsed = storageUsed,
            cpuUsage = cpuUsage
        )
    }

    /**
     * 重启路由器
     */
    /**
     * 获取挂载点信息
     */
    suspend fun getMountPoints(): List<com.luanmuc.openwrtmanager.ui.storage.MountPointInfo> {
        return try {
            val output = executeCommand("df -k") ?: return emptyList()
            val lines = output.lines()
            if (lines.size <= 1) return emptyList()
            
            val mounts = mutableListOf<com.luanmuc.openwrtmanager.ui.storage.MountPointInfo>()
            // df格式: Filesystem 1K-blocks Used Available Use% Mounted on
            lines.drop(1).mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 6) return@mapNotNull null
                
                val total = parts[1].toLongOrNull()?.times(1024) ?: 0L
                val used = parts[2].toLongOrNull()?.times(1024) ?: 0L
                val free = parts[3].toLongOrNull()?.times(1024) ?: 0L
                
                com.luanmuc.openwrtmanager.ui.storage.MountPointInfo(
                    mountPoint = parts[5],
                    device = parts[0],
                    filesystem = "",
                    total = total,
                    used = used,
                    free = free,
                    usedPercent = if (total > 0) (used * 100f / total) else 0f
                )
            }.let { mounts.addAll(it) }
            
            mounts
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun reboot(): Boolean {
        return try {
            callUbus("system", "reboot")
            true
        } catch (e: Exception) {
            true
        }
    }

    /**
     * 关机
     */
    suspend fun shutdown(): Boolean {
        return try {
            callUbus("system", "poweroff")
            true
        } catch (e: Exception) {
            true
        }
    }

    // ========== 网络接口 ==========

    /**
     * 获取网络接口列表
     */
    suspend fun getNetworkInterfaces(): List<NetworkInterface> {
        return try {
            val result = callUbus("network.interface", "dump")
            val interfaces = result["interface"] as? List<*>
            interfaces?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                parseNetworkInterface(map)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseNetworkInterface(map: Map<*, *>): NetworkInterface {
        val name = map["interface"]?.toString() ?: map["name"]?.toString() ?: ""
        val l3Device = map["l3_device"]?.toString() ?: ""
        val proto = map["proto"]?.toString() ?: ""
        val isUp = map["up"] as? Boolean ?: false
        val isConnected = map["connected"] as? Boolean ?: false
        val uptime = (map["uptime"] as? Number)?.toLong() ?: 0L

        val ipv4Addresses = map["ipv4-address"] as? List<*>
        val firstIpv4 = ipv4Addresses?.firstOrNull() as? Map<*, *>
        val ipaddr = firstIpv4?.get("address")?.toString() ?: ""
        val netmask = firstIpv4?.get("mask")?.toString() ?: ""

        val routes = map["route"] as? List<*>
        val gateway = routes?.firstOrNull {
            val r = it as? Map<*, *>
            r?.get("target") == "0.0.0.0"
        }?.let {
            (it as? Map<*, *>)?.get("nexthop")?.toString()
        } ?: ""

        val dnsServers = map["dns-server"] as? List<*>
        val dns = dnsServers?.mapNotNull { it?.toString() } ?: emptyList()

        val stats = map["stats"] as? Map<*, *>
        val rxBytes = (stats?.get("rx_bytes") as? Number)?.toLong() ?: 0L
        val txBytes = (stats?.get("tx_bytes") as? Number)?.toLong() ?: 0L
        val rxPackets = (stats?.get("rx_packets") as? Number)?.toLong() ?: 0L
        val txPackets = (stats?.get("tx_packets") as? Number)?.toLong() ?: 0L

        return NetworkInterface(
            name = name,
            device = l3Device,
            proto = proto,
            ipaddr = ipaddr,
            netmask = netmask,
            gateway = gateway,
            dns = dns,
            uptime = uptime,
            rxBytes = rxBytes,
            txBytes = txBytes,
            rxPackets = rxPackets,
            txPackets = txPackets,
            isUp = isUp,
            isConnected = isConnected
        )
    }

    /**
     * 获取WAN口状态
     */
    suspend fun getWanStatus(): NetworkInterface? {
        return getNetworkInterfaces().firstOrNull { it.name == "wan" }
    }

    /**
     * 重启网络
     */
    suspend fun restartNetwork(): Boolean {
        return try {
            callUbus("network", "restart")
            true
        } catch (e: Exception) {
            false
        }
    }

    // ========== WiFi 相关 ==========

    /**
     * 获取WiFi设备列表
     */
    suspend fun getWifiDevices(): List<WifiInterface> {
        return try {
            val result = callUbus("iwinfo", "devices")
            val devices = result["devices"] as? List<*>
            devices?.mapNotNull { name ->
                val deviceName = name?.toString() ?: return@mapNotNull null
                getWifiDeviceInfo(deviceName)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取WiFi设备信息
     */
    suspend fun getWifiDeviceInfo(device: String): WifiInterface {
        return try {
            val result = callUbus("iwinfo", "info", mapOf("device" to device))
            val ssid = result["ssid"]?.toString() ?: ""
            val channel = (result["channel"] as? Number)?.toInt() ?: 0
            val htmode = result["htmode"]?.toString() ?: ""
            val txpower = (result["txpower"] as? Number)?.toInt() ?: 0
            val encryption = result["encryption"]?.toString() ?: ""
            val isUp = result["up"] as? Boolean ?: false

            val band = if (channel > 14) "5g" else "2.4g"

            WifiInterface(
                name = device,
                device = device,
                ssid = ssid,
                encryption = encryption,
                channel = channel,
                htmode = htmode,
                txpower = txpower,
                isUp = isUp,
                band = band
            )
        } catch (e: Exception) {
            WifiInterface(name = device, device = device)
        }
    }

    /**
     * 获取WiFi关联设备数
     */
    suspend fun getWifiAssoclist(device: String): List<DeviceInfo> {
        return try {
            val result = callUbus("iwinfo", "assoclist", mapOf("device" to device))
            val results = result["results"] as? List<*>
            results?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                DeviceInfo(
                    mac = map["mac"]?.toString() ?: "",
                    signal = (map["signal"] as? Number)?.toInt() ?: 0,
                    rxBytes = (map["rx_bytes"] as? Number)?.toLong() ?: 0,
                    txBytes = (map["tx_bytes"] as? Number)?.toLong() ?: 0,
                    interfaceName = device
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 重启WiFi
     */
    suspend fun restartWifi(): Boolean {
        return try {
            callUbus("network.wireless", "restart")
            true
        } catch (e: Exception) {
            false
        }
    }

    // ========== DHCP 租约（在线设备） ==========

    /**
     * 获取DHCP租约（在线设备）
     */
    suspend fun getDhcpLeases(): List<DeviceInfo> {
        return try {
            val output = executeCommand("cat /tmp/dhcp.leases") ?: return emptyList()
            output.lines().mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                // /tmp/dhcp.leases格式: expires mac ip hostname client-id
                val parts = line.split(" ")
                if (parts.size < 3) return@mapNotNull null
                DeviceInfo(
                    ip = parts[2],
                    mac = parts[1],
                    hostname = if (parts.size >= 4 && parts[3] != "*") parts[3] else "",
                    vendor = "",
                    connectedTime = parts[0].toLongOrNull() ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取ARP表（在线设备）
     */
    suspend fun getArpTable(): List<DeviceInfo> {
        return try {
            val output = executeCommand("cat /proc/net/arp") ?: return emptyList()
            val lines = output.lines()
            if (lines.size <= 1) return emptyList()
            // /proc/net/arp格式: IP address HW type Flags HW address Mask Device
            lines.drop(1).mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 4) return@mapNotNull null
                // 只返回已解析的ARP条目（Flags为0x2）
                if (parts[2] != "0x2") return@mapNotNull null
                DeviceInfo(
                    ip = parts[0],
                    mac = parts[3],
                    interfaceName = if (parts.size >= 6) parts[5] else ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ========== 系统日志 ==========

    /**
     * 获取系统日志
     */
    suspend fun getSystemLog(): List<LogEntry> {
        return try {
            val output = executeCommand("logread") ?: return emptyList()
            output.lines().mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                // logread格式: Mon Jan 15 10:30:00 2024 daemon.info dnsmasq[1234]: message
                val parts = line.split(" ", limit = 7)
                LogEntry(
                    time = if (parts.size >= 4) "${parts[0]} ${parts[1]} ${parts[2]} ${parts[3]}" else line,
                    level = if (parts.size >= 5) parts[4].substringAfterLast(".").ifEmpty { "info" } else "info",
                    facility = if (parts.size >= 5) parts[4].substringBefore(".").ifEmpty { "daemon" } else "daemon",
                    message = if (parts.size >= 7) parts[6] else line
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ========== 进程管理 ==========

    /**
     * 获取进程列表
     */
    suspend fun getProcessList(): List<ProcessInfo> {
        return try {
            val output = executeCommand("ps") ?: return emptyList()
            val lines = output.lines()
            if (lines.size <= 1) return emptyList()
            // ps格式: PID  UID  VSZ  RSS  COMMAND
            lines.drop(1).mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val parts = line.trim().split(Regex("\\s+"), limit = 5)
                if (parts.size < 4) return@mapNotNull null
                ProcessInfo(
                    pid = parts[0].toIntOrNull() ?: 0,
                    name = parts.last().substringAfterLast("/").ifEmpty { parts.last() },
                    cpu = 0f,
                    memory = 0f,
                    vsz = parts[2].toLongOrNull() ?: 0L,
                    rss = parts[3].toLongOrNull() ?: 0L,
                    user = parts[1],
                    command = parts.last()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 结束进程
     */
    suspend fun killProcess(pid: Int): Boolean {
        return try {
            // 使用system signal方法（标准ubus），参数为pid和signum
            callUbus("system", "signal", mapOf("pid" to pid, "signum" to 9))
            true
        } catch (e: Exception) {
            // 备用方案：使用kill命令
            executeCommand("kill -9 $pid")
            true
        }
    }

    // ========== UCI 配置操作 ==========

    /**
     * 获取UCI配置
     */
    suspend fun getUciConfig(config: String, section: String? = null): Map<String, String> {
        return try {
            val params = mutableMapOf<String, Any>("config" to config)
            if (section != null) {
                params["section"] = section
            }
            val result = callUbus("uci", "get", params)
            // 如果指定了section，返回该section的values
            if (section != null) {
                val values = result["values"] as? Map<*, *>
                values?.mapKeys { it.key.toString() }?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
            } else {
                result.mapKeys { it.key.toString() }.mapValues { it.value?.toString() ?: "" }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 获取单个UCI配置值
     */
    suspend fun getUciConfigValue(config: String, section: String, option: String): String? {
        return try {
            val values = getUciConfig(config, section)
            values[option]
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置UCI配置
     */
    suspend fun setUciConfig(config: String, section: String, option: String, value: String): Boolean {
        return try {
            callUbus(
                "uci", "set",
                mapOf(
                    "config" to config,
                    "section" to section,
                    "option" to option,
                    "value" to value
                )
            )
            callUbus("uci", "commit", mapOf("config" to config))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 应用UCI配置
     */
    suspend fun commitUci(config: String): Boolean {
        return try {
            callUbus("uci", "commit", mapOf("config" to config))
            true
        } catch (e: Exception) {
            false
        }
    }

    // ========== 插件/包管理 ==========

    /**
     * 获取已安装包列表
     */
    suspend fun getInstalledPackages(): List<PackageInfo> {
        return try {
            val output = executeCommand("opkg list-installed") ?: return emptyList()
            output.lines().mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                // opkg list-installed格式: package-name - version
                val parts = line.split(" - ", limit = 2)
                if (parts.size < 2) return@mapNotNull null
                PackageInfo(
                    name = parts[0].trim(),
                    version = parts[1].trim(),
                    description = "",
                    size = 0L,
                    installed = true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取可用包列表
     */
    suspend fun getAvailablePackages(): List<PackageInfo> {
        return try {
            val output = executeCommand("opkg list") ?: return emptyList()
            output.lines().mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                // opkg list格式: package-name - version - description
                val parts = line.split(" - ", limit = 3)
                if (parts.size < 2) return@mapNotNull null
                PackageInfo(
                    name = parts[0].trim(),
                    version = parts[1].trim(),
                    description = if (parts.size >= 3) parts[2].trim() else "",
                    size = 0L,
                    installed = false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 安装包
     */
    suspend fun installPackage(name: String): Boolean {
        return try {
            val output = executeCommand("opkg install $name")
            output != null && !output.contains("error") && !output.contains("failed")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 卸载包
     */
    suspend fun removePackage(name: String): Boolean {
        return try {
            val output = executeCommand("opkg remove $name")
            output != null && !output.contains("error") && !output.contains("failed")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 更新软件源列表（opkg update）
     */
    suspend fun updatePackageLists(): Boolean {
        return try {
            val output = executeCommand("opkg update")
            output != null && !output.contains("error") && !output.contains("failed")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取软件源列表
     */
    suspend fun getPackageRepos(): List<RepoInfo> {
        return try {
            val result = callUbus("uci", "get", mapOf("config" to "opkg"))
            val values = result["values"] as? Map<*, *>
            val repos = mutableListOf<RepoInfo>()
            
            values?.forEach { (key, value) ->
                val map = value as? Map<*, *> ?: return@forEach
                val type = map[".type"]?.toString() ?: ""
                if (type == "feed") {
                    val name = key.toString()
                    repos.add(
                        RepoInfo(
                            name = name,
                            url = map["url"]?.toString() ?: "",
                            enabled = map["enabled"]?.toString()?.toBoolean() ?: true,
                            priority = (map["priority"] as? Number)?.toInt() ?: 0,
                            type = map[".type"]?.toString() ?: "src/gz",
                            repoType = getRepoType(name)
                        )
                    )
                }
            }
            repos
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加软件源
     */
    suspend fun addPackageRepo(name: String, url: String, enabled: Boolean = true): Boolean {
        return try {
            callUbus(
                "uci", "set", mapOf(
                    "config" to "opkg",
                    "section" to name,
                    "type" to "feed",
                    "values" to mapOf(
                        "url" to url,
                        "enabled" to if (enabled) "1" else "0"
                    )
                )
            )
            callUbus("uci", "commit", mapOf("config" to "opkg"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除软件源
     */
    suspend fun removePackageRepo(name: String): Boolean {
        return try {
            callUbus(
                "uci", "delete", mapOf(
                    "config" to "opkg",
                    "section" to name
                )
            )
            callUbus("uci", "commit", mapOf("config" to "opkg"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 启用/禁用软件源
     */
    suspend fun setRepoEnabled(name: String, enabled: Boolean): Boolean {
        return try {
            callUbus(
                "uci", "set", mapOf(
                    "config" to "opkg",
                    "section" to name,
                    "options" to mapOf(
                        "enabled" to if (enabled) "1" else "0"
                    )
                )
            )
            callUbus("uci", "commit", mapOf("config" to "opkg"))
            true
        } catch (e: Exception) {
            false
        }
    }

    // ========== 防火墙/端口转发 ==========

    /**
     * 获取端口转发规则
     */
    suspend fun getPortForwards(): List<PortForwardRule> {
        return try {
            val result = callUbus("uci", "get", mapOf("config" to "firewall"))
            val values = result["values"] as? Map<*, *>
            val rules = mutableListOf<PortForwardRule>()
            values?.forEach { (key, value) ->
                val section = value as? Map<*, *> ?: return@forEach
                val type = section[".type"]?.toString()
                if (type == "redirect") {
                    rules.add(
                        PortForwardRule(
                            name = section["name"]?.toString() ?: key.toString(),
                            proto = section["proto"]?.toString() ?: "tcp",
                            src = section["src"]?.toString() ?: "wan",
                            srcPort = section["src_dport"]?.toString() ?: "",
                            dest = section["dest"]?.toString() ?: "lan",
                            destIp = section["dest_ip"]?.toString() ?: "",
                            destPort = section["dest_port"]?.toString() ?: "",
                            enabled = section["enabled"]?.toString() != "0"
                        )
                    )
                }
            }
            rules
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加端口转发规则
     */
    suspend fun addPortForward(rule: PortForwardRule): Boolean {
        return try {
            val sectionName = "redirect_${System.currentTimeMillis()}"
            callUbus(
                "uci", "add", mapOf(
                    "config" to "firewall",
                    "type" to "redirect",
                    "name" to sectionName,
                    "values" to mapOf(
                        "name" to rule.name,
                        "proto" to rule.proto,
                        "src" to rule.src,
                        "src_dport" to rule.srcPort,
                        "dest" to rule.dest,
                        "dest_ip" to rule.destIp,
                        "dest_port" to rule.destPort,
                        "enabled" to if (rule.enabled) "1" else "0"
                    )
                )
            )
            callUbus("uci", "commit", mapOf("config" to "firewall"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除端口转发规则
     */
    suspend fun deletePortForward(ruleName: String): Boolean {
        return try {
            callUbus(
                "uci", "delete", mapOf(
                    "config" to "firewall",
                    "section" to ruleName
                )
            )
            callUbus("uci", "commit", mapOf("config" to "firewall"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 更新端口转发规则
     */
    suspend fun updatePortForward(ruleName: String, rule: PortForwardRule): Boolean {
        return try {
            setUciConfig("firewall", ruleName, "name", rule.name)
            setUciConfig("firewall", ruleName, "proto", rule.proto)
            setUciConfig("firewall", ruleName, "src", rule.src)
            setUciConfig("firewall", ruleName, "src_dport", rule.srcPort)
            setUciConfig("firewall", ruleName, "dest", rule.dest)
            setUciConfig("firewall", ruleName, "dest_ip", rule.destIp)
            setUciConfig("firewall", ruleName, "dest_port", rule.destPort)
            setUciConfig("firewall", ruleName, "enabled", if (rule.enabled) "1" else "0")
            commitUci("firewall")
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取DDNS配置列表
     */
    suspend fun getDdnsConfigs(): List<DdnsConfig> {
        return try {
            val result = callUbus("uci", "get", mapOf("config" to "ddns"))
            val values = result["values"] as? Map<*, *>
            val configs = mutableListOf<DdnsConfig>()
            values?.forEach { (key, value) ->
                val section = value as? Map<*, *> ?: return@forEach
                val type = section[".type"]?.toString()
                if (type == "service") {
                    configs.add(
                        DdnsConfig(
                            name = key.toString(),
                            service = section["service_name"]?.toString() ?: section["service"]?.toString() ?: "",
                            domain = section["domain"]?.toString() ?: "",
                            username = section["username"]?.toString() ?: "",
                            password = section["password"]?.toString() ?: "",
                            interfaceName = section["interface"]?.toString() ?: "wan",
                            enabled = section["enabled"]?.toString() != "0",
                            status = ""
                        )
                    )
                }
            }
            configs
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加DDNS配置
     */
    suspend fun addDdnsConfig(config: DdnsConfig): Boolean {
        return try {
            val sectionName = "ddns_${System.currentTimeMillis()}"
            callUbus(
                "uci", "add", mapOf(
                    "config" to "ddns",
                    "type" to "service",
                    "name" to sectionName,
                    "values" to mapOf(
                        "service_name" to config.service,
                        "domain" to config.domain,
                        "username" to config.username,
                        "password" to config.password,
                        "interface" to config.interfaceName,
                        "enabled" to if (config.enabled) "1" else "0"
                    )
                )
            )
            callUbus("uci", "commit", mapOf("config" to "ddns"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除DDNS配置
     */
    suspend fun deleteDdnsConfig(sectionName: String): Boolean {
        return try {
            callUbus(
                "uci", "delete", mapOf(
                    "config" to "ddns",
                    "section" to sectionName
                )
            )
            callUbus("uci", "commit", mapOf("config" to "ddns"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 更新DDNS配置
     */
    suspend fun updateDdnsConfig(sectionName: String, config: DdnsConfig): Boolean {
        return try {
            setUciConfig("ddns", sectionName, "service_name", config.service)
            setUciConfig("ddns", sectionName, "domain", config.domain)
            setUciConfig("ddns", sectionName, "username", config.username)
            setUciConfig("ddns", sectionName, "password", config.password)
            setUciConfig("ddns", sectionName, "interface", config.interfaceName)
            setUciConfig("ddns", sectionName, "enabled", if (config.enabled) "1" else "0")
            commitUci("ddns")
            true
        } catch (e: Exception) {
            false
        }
    }

    // ========== 工具方法 ==========

    private var lastCpuStats: LongArray? = null
    
    private suspend fun calculateCpuUsage(sysInfo: Map<String, Any>): Float {
        return try {
            val statOutput = executeCommand("cat /proc/stat | head -1") ?: return 0f
            // /proc/stat格式: cpu user nice system idle iowait irq softirq
            val parts = statOutput.trim().split(Regex("\\s+"))
            if (parts.size < 5) return 0f
            
            val user = parts[1].toLongOrNull() ?: 0L
            val nice = parts[2].toLongOrNull() ?: 0L
            val system = parts[3].toLongOrNull() ?: 0L
            val idle = parts[4].toLongOrNull() ?: 0L
            val iowait = if (parts.size > 5) parts[5].toLongOrNull() ?: 0L else 0L
            
            val total = user + nice + system + idle + iowait
            val idleTotal = idle + iowait
            
            val last = lastCpuStats
            lastCpuStats = longArrayOf(total, idleTotal)
            
            if (last != null && total > last[0]) {
                val totalDiff = total - last[0]
                val idleDiff = idleTotal - last[1]
                ((totalDiff - idleDiff) * 100f / totalDiff).coerceIn(0f, 100f)
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun normalizeAddress(address: String): String {
        var addr = address.trim()
        if (!addr.startsWith("http://") && !addr.startsWith("https://")) {
            addr = "http://$addr"
        }
        if (!addr.endsWith("/")) {
            addr = "$addr/"
        }
        return addr
    }

    private fun wrapException(e: Exception): Exception {
        return when (e) {
            is LuciException -> e
            is UnknownHostException -> LuciException(
                message = Loc.s(R.string.s0108),
                type = ErrorType.NETWORK_ERROR
            )
            is ConnectException -> LuciException(
                message = Loc.s(R.string.s0109),
                type = ErrorType.NETWORK_ERROR
            )
            is SocketTimeoutException -> LuciException(
                message = Loc.s(R.string.s0110),
                type = ErrorType.TIMEOUT
            )
            is retrofit2.HttpException -> {
                when (e.code()) {
                    404 -> LuciException(
                        message = Loc.s(R.string.s0111),
                        type = ErrorType.NOT_FOUND
                    )
                    403 -> LuciException(
                        message = Loc.s(R.string.s0112),
                        type = ErrorType.FORBIDDEN
                    )
                    401 -> LuciException(
                        message = Loc.s(R.string.s0113),
                        type = ErrorType.AUTH_FAILED
                    )
                    else -> LuciException(
                        message = Loc.s(R.string.s0114, e.code()),
                        type = ErrorType.HTTP_ERROR
                    )
                }
            }
            else -> LuciException(
                message = e.message ?: Loc.s(R.string.s0115),
                type = ErrorType.UNKNOWN
            )
        }
    }

    fun getCurrentAddress(): String = currentAddress
    fun getCurrentAuthToken(): String = authToken
    fun isLoggedIn(): Boolean = authToken.isNotEmpty()
    fun logout() {
        // 停止自动续期
        stopAutoRefresh()
        
        authToken = ""
        currentAddress = ""
        currentUsername = ""
        currentPassword = ""
        sessionExpireTime = 0L
        
        // 清除持久化的session
        clearSessionPrefs()
        
        RetrofitClient.reset()
    }
    
    /**
     * 获取session剩余有效时间（毫秒）
     */
    fun getSessionRemainingTime(): Long {
        val remaining = sessionExpireTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }
    
    /**
     * 获取session总有效期（毫秒）
     */
    fun getSessionExpiresIn(): Long = sessionExpiresIn

    // ==================== 系统信息检测 ====================

    /**
     * 获取完整的系统信息
     */
    suspend fun getFullSystemInfo(): FullSystemInfo {
        return try {
            val sysInfo = getSystemInfo()
            val boardInfo = getBoardInfo()
            val releaseInfo = boardInfo["release"] as? Map<*, *>

            val hostname = sysInfo["hostname"]?.toString()
                ?: boardInfo["hostname"]?.toString()
                ?: "OpenWrt"
            val model = boardInfo["model"]?.toString()
                ?: sysInfo["model"]?.toString()
                ?: "Unknown"
            val kernel = boardInfo["kernel"]?.toString() ?: "Unknown"
            val boardName = boardInfo["board_name"]?.toString() ?: ""

            // 解析release信息
            val distribution = releaseInfo?.get("distribution")?.toString() ?: "OpenWrt"
            val version = releaseInfo?.get("version")?.toString() ?: "Unknown"
            val revision = releaseInfo?.get("revision")?.toString() ?: ""
            val target = releaseInfo?.get("target")?.toString() ?: ""
            val description = releaseInfo?.get("description")?.toString() ?: ""
            val title = releaseInfo?.get("title")?.toString() ?: ""

            // 检测架构
            val architecture = detectArchitecture(boardInfo, sysInfo)

            // 检测包管理器
            val packageManager = detectPackageManager()

            FullSystemInfo(
                hostname = hostname,
                model = model,
                firmwareVersion = version,
                kernelVersion = kernel,
                architecture = architecture.displayName,
                packageManager = packageManager,
                boardName = boardName,
                release = version,
                distribution = distribution,
                revision = revision,
                target = target,
                description = description,
                title = title
            )
        } catch (e: Exception) {
            e.printStackTrace()
            FullSystemInfo()
        }
    }

    /**
     * 检测架构
     */
    private suspend fun detectArchitecture(boardInfo: Map<String, Any>, sysInfo: Map<String, Any>): ArchitectureType {
        return try {
            val boardName = boardInfo["board_name"]?.toString() ?: ""
            val model = boardInfo["model"]?.toString() ?: ""
            val system = sysInfo["system"]?.toString() ?: ""

            when {
                // x86_64
                boardName.contains("x86", ignoreCase = true) ||
                model.contains("x86", ignoreCase = true) ||
                system.contains("x86_64", ignoreCase = true) -> ArchitectureType.X86_64

                // aarch64
                boardName.contains("aarch64", ignoreCase = true) ||
                model.contains("aarch64", ignoreCase = true) ||
                system.contains("aarch64", ignoreCase = true) -> ArchitectureType.AARCH64

                // armv7
                boardName.contains("armv7", ignoreCase = true) ||
                model.contains("armv7", ignoreCase = true) ||
                system.contains("armv7", ignoreCase = true) ||
                boardName.contains("cortex-a7", ignoreCase = true) -> ArchitectureType.ARMV7

                // mipsel
                boardName.contains("mipsel", ignoreCase = true) ||
                system.contains("mipsel", ignoreCase = true) -> ArchitectureType.MIPSEL

                // mips
                boardName.contains("mips", ignoreCase = true) ||
                system.contains("mips", ignoreCase = true) -> ArchitectureType.MIPS

                else -> ArchitectureType.UNKNOWN
            }
        } catch (e: Exception) {
            ArchitectureType.UNKNOWN
        }
    }

    /**
     * 检测包管理器类型
     */
    suspend fun detectPackageManager(): PackageManagerType {
        return try {
            // 尝试执行opkg命令
            val result = try {
                val opkgOutput = executeCommand("opkg --version")
                if (opkgOutput != null && opkgOutput.isNotEmpty()) {
                    PackageManagerType.OPKG
                } else null
            } catch (e: Exception) {
                null
            }

            if (result != null) {
                return PackageManagerType.OPKG
            }

            // 尝试执行apk命令（通过shell）
            try {
                val apkResult = callUbus("file", "exec", mapOf(
                    "command" to "/bin/sh",
                    "params" to listOf("-c", "apk --version")
                ))
                if (apkResult.isNotEmpty()) {
                    return PackageManagerType.APK
                }
            } catch (e: Exception) {
                // 忽略
            }

            PackageManagerType.UNKNOWN
        } catch (e: Exception) {
            PackageManagerType.OPKG // 默认返回OPKG
        }
    }

    // ==================== 软件源自动配置 ====================

    /**
     * 获取预设软件源列表
     */
    fun getPresetRepos(systemInfo: FullSystemInfo): List<RepoPreset> {
        val arch = when (systemInfo.architecture) {
            "x86_64" -> ArchitectureType.X86_64
            "aarch64" -> ArchitectureType.AARCH64
            "armv7" -> ArchitectureType.ARMV7
            "mipsel" -> ArchitectureType.MIPSEL
            "mips" -> ArchitectureType.MIPS
            else -> ArchitectureType.UNKNOWN
        }

        // Normalize firmware version (supports 23.5.x / 23.05.x / "OpenWrt 23.05.x r…")
        val version = OpenWrtVersions.normalize(systemInfo.firmwareVersion)
        val isApk = systemInfo.packageManager == PackageManagerType.APK
        
        // 根据包管理器选择架构字符串
        val archStr = if (isApk) arch.apkArch else arch.opkgArch
        
        // 基础URL前缀（根据包管理器不同）
        val basePath = if (isApk) {
            "releases/$version/packages/$archStr"
        } else {
            "releases/$version/packages/$archStr"
        }

        return listOf(
            // 官方源
            RepoPreset(
                id = "official",
                name = Loc.s(R.string.s0116),
                description = Loc.s(R.string.s0117),
                baseUrl = "https://downloads.openwrt.org/$basePath",
                type = RepoPresetType.OFFICIAL,
                supportedArchitectures = ArchitectureType.values().toList()
            ),
            // 清华源
            RepoPreset(
                id = "tsinghua",
                name = Loc.s(R.string.s0118),
                description = Loc.s(R.string.s0119),
                baseUrl = "https://mirrors.tuna.tsinghua.edu.cn/openwrt/$basePath",
                type = RepoPresetType.TSINGHUA,
                supportedArchitectures = ArchitectureType.values().toList()
            ),
            // 中科大源
            RepoPreset(
                id = "ustc",
                name = Loc.s(R.string.s0120),
                description = Loc.s(R.string.s0121),
                baseUrl = "https://mirrors.ustc.edu.cn/openwrt/$basePath",
                type = RepoPresetType.USTC,
                supportedArchitectures = ArchitectureType.values().toList()
            ),
            // 阿里云源
            RepoPreset(
                id = "aliyun",
                name = Loc.s(R.string.s0122),
                description = Loc.s(R.string.s0123),
                baseUrl = "https://mirrors.aliyun.com/openwrt/$basePath",
                type = RepoPresetType.ALIYUN,
                supportedArchitectures = ArchitectureType.values().toList()
            ),
            // kenzok8源
            RepoPreset(
                id = "kenzok8",
                name = Loc.s(R.string.s0124),
                description = Loc.s(R.string.s0125),
                baseUrl = "https://op.dllkids.xyz/packages/$archStr",
                type = RepoPresetType.KENZOK8,
                supportedArchitectures = listOf(
                    ArchitectureType.X86_64,
                    ArchitectureType.AARCH64,
                    ArchitectureType.ARMV7
                )
            ),
            // lienol源
            RepoPreset(
                id = "lienol",
                name = Loc.s(R.string.s0126),
                description = Loc.s(R.string.s0127),
                baseUrl = "https://op.supes.top/packages/$archStr",
                type = RepoPresetType.LIENOL,
                supportedArchitectures = listOf(
                    ArchitectureType.X86_64,
                    ArchitectureType.AARCH64,
                    ArchitectureType.ARMV7,
                    ArchitectureType.MIPSEL
                )
            ),
            // ImmortalWrt源
            RepoPreset(
                id = "immortalwrt",
                name = Loc.s(R.string.s0128),
                description = Loc.s(R.string.s0129),
                baseUrl = "https://mirrors.tuna.tsinghua.edu.cn/immortalwrt/releases/$version/packages/$archStr",
                type = RepoPresetType.IMMORTALWRT,
                supportedArchitectures = listOf(
                    ArchitectureType.X86_64,
                    ArchitectureType.AARCH64,
                    ArchitectureType.ARMV7,
                    ArchitectureType.MIPSEL
                )
            )
        )
    }

    /**
     * 自动配置官方软件源
     */
    suspend fun autoConfigureOfficialRepos(systemInfo: FullSystemInfo): Boolean {
        return try {
            val arch = when (systemInfo.architecture) {
                "x86_64" -> ArchitectureType.X86_64
                "aarch64" -> ArchitectureType.AARCH64
                "armv7" -> ArchitectureType.ARMV7
                "mipsel" -> ArchitectureType.MIPSEL
                "mips" -> ArchitectureType.MIPS
                else -> return false
            }

            // Normalize firmware version (supports 23.5.x / 23.05.x)
            val version = OpenWrtVersions.normalize(systemInfo.firmwareVersion)
            if (version == "Unknown" || version.isBlank()) return false

            // 备份原有配置
            backupOpkgConfig()

            // 获取当前源列表
            val currentRepos = getPackageRepos()

            // 配置官方源（追加模式）
            val baseUrl = "https://downloads.openwrt.org/releases/$version/packages/${arch.opkgArch}"

            // 添加各个源（已存在则跳过）
            val repos = listOf(
                "base" to "$baseUrl/base",
                "luci" to "$baseUrl/luci",
                "packages" to "$baseUrl/packages",
                "routing" to "$baseUrl/routing",
                "telephony" to "$baseUrl/telephony"
            )

            for ((name, url) in repos) {
                if (currentRepos.none { it.name == name }) {
                    addPackageRepo(name, url, true)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 切换官方镜像源（追加模式，保留原有源）
     * 只切换官方镜像源的URL，不删除第三方源
     */
    suspend fun switchMirrorRepo(preset: RepoPreset, systemInfo: FullSystemInfo): Boolean {
        return try {
            // 备份原有配置
            backupOpkgConfig()

            // 获取当前源列表
            val currentRepos = getPackageRepos()
            
            // 官方源的名称列表
            val officialRepoNames = listOf("base", "luci", "packages", "routing", "telephony")
            
            // 检查官方源是否存在
            val hasOfficialRepos = currentRepos.any { it.name in officialRepoNames }
            
            val baseUrl = preset.baseUrl
            
            if (hasOfficialRepos) {
                // 如果已有官方源，更新它们的URL并启用
                for (repoName in officialRepoNames) {
                    val existingRepo = currentRepos.find { it.name == repoName }
                    val url = "$baseUrl/$repoName"
                    if (existingRepo != null) {
                        // 更新现有源的URL和状态
                        updatePackageRepo(repoName, url, true)
                    } else {
                        // 添加缺失的官方源
                        addPackageRepo(repoName, url, true)
                    }
                }
            } else {
                // 如果没有官方源，添加全套官方源
                val repos = listOf(
                    "base" to "$baseUrl/base",
                    "luci" to "$baseUrl/luci",
                    "packages" to "$baseUrl/packages",
                    "routing" to "$baseUrl/routing",
                    "telephony" to "$baseUrl/telephony"
                )
                for ((name, url) in repos) {
                    addPackageRepo(name, url, true)
                }
            }

            // 更新软件源
            updatePackageLists()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 添加第三方插件源（追加模式）
     */
    suspend fun addThirdPartyRepo(preset: RepoPreset, systemInfo: FullSystemInfo): Boolean {
        return try {
            // 备份原有配置
            backupOpkgConfig()

            // 获取当前源列表
            val currentRepos = getPackageRepos()
            
            // 检查是否已存在同名源
            if (currentRepos.any { it.name == preset.id }) {
                // 已存在，启用它
                setRepoEnabled(preset.id, true)
                return true
            }
            
            // 添加第三方源
            val arch = when (systemInfo.architecture) {
                "x86_64" -> ArchitectureType.X86_64
                "aarch64" -> ArchitectureType.AARCH64
                "armv7" -> ArchitectureType.ARMV7
                "mipsel" -> ArchitectureType.MIPSEL
                "mips" -> ArchitectureType.MIPS
                else -> return false
            }
            
            val url = "${preset.baseUrl}/${arch.opkgArch}"
            addPackageRepo(preset.id, url, true)

            // 更新软件源
            updatePackageLists()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 移除第三方插件源
     */
    suspend fun removeThirdPartyRepo(presetId: String): Boolean {
        return try {
            // 备份原有配置
            backupOpkgConfig()
            
            removePackageRepo(presetId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 更新软件源配置
     */
    private suspend fun updatePackageRepo(name: String, url: String, enabled: Boolean): Boolean {
        return try {
            // 先删除旧的
            removePackageRepo(name)
            // 再添加新的
            addPackageRepo(name, url, enabled)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 备份opkg配置
     */
    private suspend fun backupOpkgConfig(): Boolean {
        return try {
            callUbus("file", "exec", mapOf(
                "command" to "/bin/sh",
                "params" to listOf("-c", "cp /etc/opkg/customfeeds.conf /etc/opkg/customfeeds.conf.bak")
            ))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 恢复opkg配置
     */
    suspend fun restoreOpkgConfig(): Boolean {
        return try {
            callUbus("file", "exec", mapOf(
                "command" to "/bin/sh",
                "params" to listOf("-c", "cp /etc/opkg/customfeeds.conf.bak /etc/opkg/customfeeds.conf")
            ))
            updatePackageLists()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取源类型
     */
    fun getRepoType(repoName: String): RepoType {
        val officialNames = listOf("base", "luci", "packages", "routing", "telephony")
        val thirdPartyIds = listOf("kenzok8", "lienol", "immortalwrt")
        
        return when {
            repoName in officialNames -> RepoType.OFFICIAL_MIRROR
            repoName in thirdPartyIds -> RepoType.THIRD_PARTY
            else -> RepoType.CUSTOM
        }
    }

    // ==================== 架构验证 ====================

    /**
     * 验证插件架构是否匹配
     */
    fun validatePackageArchitecture(pkg: PackageInfo, systemInfo: FullSystemInfo): Boolean {
        return try {
            val arch = when (systemInfo.architecture) {
                "x86_64" -> ArchitectureType.X86_64
                "aarch64" -> ArchitectureType.AARCH64
                "armv7" -> ArchitectureType.ARMV7
                "mipsel" -> ArchitectureType.MIPSEL
                "mips" -> ArchitectureType.MIPS
                else -> return true // 未知架构时跳过验证
            }

            // 如果包信息中没有架构信息，默认通过
            if (pkg.architecture.isEmpty()) return true

            // 检查架构是否匹配
            pkg.architecture.contains(arch.opkgArch, ignoreCase = true) ||
            pkg.architecture.contains("all", ignoreCase = true) ||
            pkg.architecture.contains("noarch", ignoreCase = true)
        } catch (e: Exception) {
            true
        }
    }

    /**
     * 获取插件支持的架构列表
     */
    fun getPackageArchitectures(pkg: PackageInfo): List<String> {
        return if (pkg.architecture.isEmpty()) {
            listOf("all")
        } else {
            pkg.architecture.split(" ").filter { it.isNotEmpty() }
        }
    }

    // ==================== 固件升级 ====================

    /**
     * 获取当前固件信息
     */
    suspend fun getFirmwareInfo(): FirmwareInfo {
        return try {
            val sysInfo = callUbus("system", "info", emptyMap())
            val boardInfo = callUbus("system", "board", emptyMap())
            
            val infoMap = sysInfo["info"] as? Map<String, Any> ?: emptyMap()
            val boardMap = boardInfo["board"] as? Map<String, Any> ?: emptyMap()
            
            val hostname = infoMap["hostname"] as? String ?: ""
            val model = boardMap["model"] as? String ?: ""
            val release = infoMap["release"] as? String ?: ""
            val kernel = infoMap["kernel"] as? String ?: ""
            val boardName = boardMap["board_name"] as? String ?: ""
            val architecture = boardMap["system"] as? String ?: ""
            
            FirmwareInfo(
                currentVersion = release,
                currentBuildTime = "",
                deviceModel = model,
                architecture = architecture,
                kernelVersion = kernel,
                boardName = boardName
            )
        } catch (e: Exception) {
            FirmwareInfo()
        }
    }

    /**
     * 检测最新固件版本（GitHub Release）
     */
    suspend fun checkLatestFirmware(repoUrl: String): FirmwareRelease? {
        return try {
            // 从GitHub Release API获取最新版本
            // 这里简化处理，实际需要解析GitHub API返回
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 下载固件
     */
    suspend fun downloadFirmware(
        url: String,
        onProgress: (Int, Long) -> Unit
    ): Boolean {
        return try {
            // 下载固件文件
            // 这里简化处理，实际需要实现下载逻辑
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 刷写固件
     */
    suspend fun flashFirmware(
        firmwarePath: String,
        keepConfig: Boolean,
        onProgress: (Int) -> Unit
    ): Boolean {
        return try {
            onProgress(10)
            // 验证固件文件存在
            if (firmwarePath.isNotEmpty()) {
                val checkResult = callUbus(
                    "file", "exec", mapOf(
                        "command" to "/bin/sh",
                        "params" to listOf("-c", "test -f $firmwarePath && echo exists || echo missing")
                    )
                )
                val stdout = (checkResult as? Map<*, *>)?.get("stdout")?.toString() ?: ""
                if (!stdout.contains("exists")) {
                    return false
                }
            }
            onProgress(30)
            // 调用sysupgrade刷写固件
            val keepFlag = if (keepConfig) "" else "-n"
            val cmd = if (firmwarePath.isNotEmpty()) {
                "/sbin/sysupgrade $keepFlag $firmwarePath"
            } else {
                // 没有本地文件时，先下载再刷写（简化处理）
                "/sbin/sysupgrade $keepFlag /tmp/firmware.bin"
            }
            onProgress(50)
            callUbus(
                "file", "exec", mapOf(
                    "command" to "/bin/sh",
                    "params" to listOf("-c", cmd)
                )
            )
            onProgress(80)
            // sysupgrade会自动重启，这里返回成功
            onProgress(100)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取固件升级配置
     */
    fun getFirmwareUpgradeConfig(): FirmwareUpgradeConfig {
        val prefs = context.getSharedPreferences("firmware_prefs", Context.MODE_PRIVATE)
        return FirmwareUpgradeConfig(
            repoUrl = prefs.getString("repo_url", "") ?: "",
            customServerUrl = prefs.getString("custom_server_url", "") ?: "",
            autoCheck = prefs.getBoolean("auto_check", true),
            keepConfig = prefs.getBoolean("keep_config", true),
            autoReboot = prefs.getBoolean("auto_reboot", true)
        )
    }

    /**
     * 设置固件升级配置
     */
    fun setFirmwareUpgradeConfig(config: FirmwareUpgradeConfig) {
        val prefs = context.getSharedPreferences("firmware_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("repo_url", config.repoUrl)
            putString("custom_server_url", config.customServerUrl)
            putBoolean("auto_check", config.autoCheck)
            putBoolean("keep_config", config.keepConfig)
            putBoolean("auto_reboot", config.autoReboot)
            apply()
        }
    }

    // ==================== 预加载功能 ====================

    /**
     * 预加载进度
     */
    data class PreloadProgress(
        val current: Int,
        val total: Int,
        val currentItem: String,
        val percentage: Float
    )

    /**
     * 预加载项目
     */
    data class PreloadItem(
        val key: String,
        val name: String,
        val loader: suspend () -> Any?
    )

    /**
     * 预加载所有常用数据
     * @param onProgress 进度回调
     * @return 成功加载的项目数
     */
    suspend fun preloadAllData(
        routerId: String,
        onProgress: ((PreloadProgress) -> Unit)? = null
    ): Int {
        val cacheRepository = CacheRepository.getInstance(context)
        
        val preloadItems = listOf(
            PreloadItem(
                key = CacheRepository.KEY_SYSTEM_INFO,
                name = Loc.s(R.string.s0130),
                loader = { 
                    try { getFullSystemInfo() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_ROUTER_STATUS,
                name = Loc.s(R.string.s0131),
                loader = { 
                    try { getRouterStatus() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_WAN_STATUS,
                name = Loc.s(R.string.s0132),
                loader = { 
                    try { getWanStatus() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_NETWORK_INTERFACES,
                name = Loc.s(R.string.s0133),
                loader = { 
                    try { getNetworkInterfaces() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_INSTALLED_PACKAGES,
                name = Loc.s(R.string.s0134),
                loader = { 
                    try { getInstalledPackages() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_AVAILABLE_PACKAGES,
                name = Loc.s(R.string.s0135),
                loader = { 
                    try { getAvailablePackages() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_ONLINE_DEVICES,
                name = Loc.s(R.string.s0076),
                loader = { 
                    try { getDhcpLeases() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_WIFI_DEVICES,
                name = Loc.s(R.string.s0136),
                loader = { 
                    try { getWifiDevices() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_PORT_FORWARDS,
                name = Loc.s(R.string.s0137),
                loader = { 
                    try { getPortForwards() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_DDNS_CONFIGS,
                name = Loc.s(R.string.s0138),
                loader = { 
                    try { getDdnsConfigs() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = "package_repos",
                name = Loc.s(R.string.s0139),
                loader = { 
                    try { getPackageRepos() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_SYSTEM_LOG,
                name = Loc.s(R.string.s0011),
                loader = { 
                    try { getSystemLog() } catch (e: Exception) { null }
                }
            ),
            PreloadItem(
                key = CacheRepository.KEY_PROCESS_LIST,
                name = Loc.s(R.string.s0140),
                loader = { 
                    try { getProcessList() } catch (e: Exception) { null }
                }
            )
        )

        var successCount = 0
        val total = preloadItems.size

        for ((index, item) in preloadItems.withIndex()) {
            onProgress?.invoke(
                PreloadProgress(
                    current = index,
                    total = total,
                    currentItem = item.name,
                    percentage = index.toFloat() / total.toFloat()
                )
            )

            try {
                val data = item.loader()
                if (data != null) {
                    cacheRepository.saveCache(
                        key = item.key,
                        routerId = routerId,
                        type = item.key,
                        data = data
                    )
                    successCount++
                }
            } catch (e: Exception) {
                // 忽略单个项目的错误，继续加载其他项目
            }
        }

        // 最后更新进度为100%
        onProgress?.invoke(
            PreloadProgress(
                current = total,
                total = total,
                currentItem = Loc.s(R.string.s0024),
                percentage = 1f
            )
        )

        return successCount
    }

    // ==================== 网口状态 ====================

    /**
     * 获取网口状态列表
     */
    suspend fun getPortStatus(): List<PortStatus> {
        return try {
            val result = callUbus("network.device", "status")
            val ports = mutableListOf<PortStatus>()
            
            // 解析所有网络设备
            for ((key, value) in result) {
                val deviceMap = value as? Map<*, *> ?: continue
                val name = key
                
                // 跳过非物理接口
                if (name.startsWith("br-") || name.startsWith("eth0.") || name == "lo") {
                    continue
                }
                
                val isUp = deviceMap["up"] as? Boolean ?: false
                val speed = (deviceMap["speed"] as? Number)?.toInt() ?: 0
                val duplex = deviceMap["duplex"]?.toString() ?: ""
                val macAddress = deviceMap["macaddr"]?.toString() ?: ""
                
                val statistics = deviceMap["statistics"] as? Map<*, *>
                val rxBytes = (statistics?.get("rx_bytes") as? Number)?.toLong() ?: 0L
                val txBytes = (statistics?.get("tx_bytes") as? Number)?.toLong() ?: 0L
                val rxPackets = (statistics?.get("rx_packets") as? Number)?.toLong() ?: 0L
                val txPackets = (statistics?.get("tx_packets") as? Number)?.toLong() ?: 0L
                
                // 判断网口类型
                val type = when {
                    name == "eth0" || name == "wan" || name.contains("wan", true) -> PortType.WAN
                    name.startsWith("eth") && name != "eth0" -> PortType.LAN
                    name.startsWith("wlan") || name.startsWith("wifi") -> PortType.WIFI
                    name.startsWith("sfp") -> PortType.SFP
                    name.startsWith("usb") -> PortType.USB
                    else -> PortType.UNKNOWN
                }
                
                // 生成显示名称
                val displayName = when (type) {
                    PortType.WAN -> "WAN"
                    PortType.LAN -> {
                        val num = name.filter { it.isDigit() }.toIntOrNull() ?: 1
                        "LAN$num"
                    }
                    PortType.WIFI -> name.uppercase()
                    PortType.SFP -> "SFP"
                    PortType.USB -> name.uppercase()
                    PortType.UNKNOWN -> name
                }
                
                ports.add(
                    PortStatus(
                        name = name,
                        displayName = displayName,
                        type = type,
                        isConnected = isUp,
                        speed = speed,
                        duplex = duplex,
                        rxBytes = rxBytes,
                        txBytes = txBytes,
                        rxPackets = rxPackets,
                        txPackets = txPackets,
                        macAddress = macAddress
                    )
                )
            }
            
            // 按类型排序：WAN在前，然后LAN，然后其他
            ports.sortedWith(compareBy(
                { it.type.ordinal },
                { it.displayName }
            ))
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== 设备能力检测 ====================

    /**
     * 检测设备能力
     */
    suspend fun detectDeviceCapabilities(): DeviceCapabilities {
        return try {
            val wifiDevices = try {
                getWifiDevices()
            } catch (e: Exception) {
                emptyList()
            }
            
            val portStatus = try {
                getPortStatus()
            } catch (e: Exception) {
                emptyList()
            }
            
            val systemInfo = try {
                getFullSystemInfo()
            } catch (e: Exception) {
                FullSystemInfo()
            }
            
            val hasWifi = wifiDevices.isNotEmpty()
            val hasUsb = portStatus.any { it.type == PortType.USB }
            val hasSfp = portStatus.any { it.type == PortType.SFP }
            
            val wanCount = portStatus.count { it.type == PortType.WAN }
            val lanCount = portStatus.count { it.type == PortType.LAN }
            
            // 检测功能所需命令/插件
            val hasTc = isCommandAvailable("tc")
            val hasIwinfo = isCommandAvailable("iwinfo") || wifiDevices.isNotEmpty()
            val hasDdnsScripts = isPackageInstalled("ddns-scripts")
            val hasLogd = isCommandAvailable("logread")
            val hasDnsmasq = isCommandAvailable("dnsmasq") || isPackageInstalled("dnsmasq")
            
            DeviceCapabilities(
                hasWifi = hasWifi,
                hasUsb = hasUsb,
                hasSfp = hasSfp,
                wifiInterfaceCount = wifiDevices.size,
                lanPortCount = lanCount,
                wanPortCount = wanCount,
                totalPortCount = portStatus.size,
                packageManager = systemInfo.packageManager,
                architecture = systemInfo.architecture,
                hasTc = hasTc,
                hasIwinfo = hasIwinfo,
                hasDdnsScripts = hasDdnsScripts,
                hasLogd = hasLogd,
                hasDnsmasq = hasDnsmasq
            )
        } catch (e: Exception) {
            DeviceCapabilities()
        }
    }

    /**
     * 检查是否有WiFi接口
     */
    suspend fun hasWifiInterface(): Boolean {
        return try {
            val wifiDevices = getWifiDevices()
            wifiDevices.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 导出系统配置备份
     * 通过sysupgrade -b生成备份并返回base64编码内容
     */
    suspend fun exportBackup(): String? {
        return try {
            // 使用ubus调用file exec执行sysupgrade -b
            val result = callUbus(
                "file", "exec", mapOf(
                    "command" to "/sbin/sysupgrade",
                    "params" to listOf("-b", "/tmp/backup.tar.gz")
                )
            )
            // 读取生成的备份文件
            val fileResult = callUbus(
                "file", "read", mapOf(
                    "path" to "/tmp/backup.tar.gz"
                )
            )
            (fileResult as? Map<*, *>)?.get("data")?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 拉黑设备（通过防火墙添加拒绝规则）
     */
    suspend fun blockDevice(mac: String): Boolean {
        return try {
            val ruleName = "block_${mac.replace(":", "").lowercase()}"
            // 添加拒绝转发规则
            callUbus(
                "uci", "add", mapOf(
                    "config" to "firewall",
                    "type" to "rule",
                    "name" to ruleName,
                    "values" to mapOf(
                        "name" to "Block-$mac",
                        "src" to "lan",
                        "dest" to "wan",
                        "proto" to "all",
                        "src_mac" to mac,
                        "target" to "REJECT",
                        "enabled" to "1"
                    )
                )
            )
            commitUci("firewall")
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 解除设备拉黑
     */
    suspend fun unblockDevice(mac: String): Boolean {
        return try {
            val ruleName = "block_${mac.replace(":", "").lowercase()}"
            callUbus(
                "uci", "delete", mapOf(
                    "config" to "firewall",
                    "section" to ruleName
                )
            )
            commitUci("firewall")
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 执行系统命令（用于诊断）
     */
    suspend fun executeCommand(command: String): String? {
        return try {
            val result = callUbus(
                "file", "exec", mapOf(
                    "command" to "/bin/sh",
                    "params" to listOf("-c", command)
                )
            )
            (result as? Map<*, *>)?.get("stdout")?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检测命令是否可用
     */
    suspend fun isCommandAvailable(command: String): Boolean {
        return try {
            val output = executeCommand("which $command 2>/dev/null || command -v $command 2>/dev/null")
            !output.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 执行系统命令并返回完整结果（退出码 + stdout + stderr）
     * 用于离线安装等需要判断命令成败的场景
     */
    suspend fun execCommandFull(command: String): ExecResult {
        val result = callUbus(
            "file", "exec", mapOf(
                "command" to "/bin/sh",
                "params" to listOf("-c", command)
            )
        )
        return ExecResult(
            code = (result["code"] as? Number)?.toInt() ?: -1,
            stdout = result["stdout"]?.toString() ?: "",
            stderr = result["stderr"]?.toString() ?: ""
        )
    }

    /**
     * 向路由器写入文件（ubus file write，data 为 base64）
     * 支持分块追加写入，用于离线安装时上传 .ipk 文件
     *
     * @param path 远程目标路径，如 /tmp/offline-pkgs/xxx.ipk
     * @param dataBase64 base64 编码的数据块
     * @param append false 时创建/覆盖文件，true 时追加
     * @param mode 文件权限（十进制，如 420 = 0644），仅创建时生效
     */
    suspend fun writeRemoteFile(
        path: String,
        dataBase64: String,
        append: Boolean,
        mode: Int? = null
    ): Boolean {
        val params = mutableMapOf<String, Any>(
            "path" to path,
            "data" to dataBase64,
            "append" to append
        )
        mode?.let { params["mode"] = it }
        return try {
            callUbus("file", "write", params)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检测功能所需插件是否安装
     */
    suspend fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val output = executeCommand("opkg list-installed | grep -q '^$packageName ' && echo yes || echo no")
            output?.trim() == "yes"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检测插件依赖是否满足
     */
    suspend fun checkPluginDependency(dependency: PluginDependency): PluginInstallStatus {
        val isInstalled = if (dependency.packageName == "ip-tiny") {
            // ip-tiny提供tc命令，检测tc命令是否可用
            isCommandAvailable("tc") || isPackageInstalled("ip-tiny") || isPackageInstalled("tc")
        } else if (dependency.packageName == "logd") {
            isCommandAvailable("logread") || isPackageInstalled("logd")
        } else if (dependency.packageName == "rpcd-mod-iwinfo") {
            isCommandAvailable("iwinfo") || isPackageInstalled("rpcd-mod-iwinfo")
        } else {
            isPackageInstalled(dependency.packageName)
        }
        return PluginInstallStatus(
            dependency = dependency,
            isInstalled = isInstalled
        )
    }

    /**
     * 检测所有插件依赖
     */
    suspend fun checkAllPluginDependencies(): List<PluginInstallStatus> {
        return PluginDependencies.ALL.map { dependency ->
            checkPluginDependency(dependency)
        }
    }

    /**
     * 安装插件（带进度回调）
     */
    suspend fun installPlugin(
        packageName: String,
        onProgress: ((Int, String) -> Unit)? = null
    ): Boolean {
        return try {
            onProgress?.invoke(10, Loc.s(R.string.s0141))
            val updateResult = executeCommand("opkg update 2>&1")
            if (updateResult == null) {
                onProgress?.invoke(0, Loc.s(R.string.s0142))
                return false
            }
            
            onProgress?.invoke(50, Loc.s(R.string.s0143, packageName))
            val installResult = executeCommand("opkg install $packageName 2>&1")
            if (installResult == null) {
                onProgress?.invoke(0, Loc.s(R.string.s0144))
                return false
            }
            
            // 检查是否安装成功
            val success = installResult.contains("Installing") || 
                          installResult.contains("upgrading") ||
                          installResult.contains("already installed") ||
                          isPackageInstalled(packageName)
            
            if (success) {
                onProgress?.invoke(100, Loc.s(R.string.s0145))
            } else {
                onProgress?.invoke(0, Loc.s(R.string.s0146, installResult.take(100)))
            }
            success
        } catch (e: Exception) {
            onProgress?.invoke(0, Loc.s(R.string.s0147, e.message))
            false
        }
    }

    /**
     * 安装插件依赖
     */
    suspend fun installPluginDependency(
        dependency: PluginDependency,
        onProgress: ((Int, String) -> Unit)? = null
    ): Boolean {
        return installPlugin(dependency.packageName, onProgress)
    }
}


/**
 * LuCI 异常类
 */
class LuciException(
    val code: Int = -1,
    override val message: String,
    val type: ErrorType = ErrorType.UNKNOWN
) : Exception(message)


/**
 * 远程命令执行完整结果
 */
data class ExecResult(
    val code: Int,
    val stdout: String,
    val stderr: String
) {
    val combinedOutput: String
        get() = buildString {
            if (stdout.isNotBlank()) append(stdout.trim())
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append(stderr.trim())
            }
        }
}


/**
 * 错误类型枚举
 */
enum class ErrorType {
    NETWORK_ERROR,
    TIMEOUT,
    AUTH_FAILED,
    NOT_FOUND,
    FORBIDDEN,
    HTTP_ERROR,
    UNKNOWN
}
