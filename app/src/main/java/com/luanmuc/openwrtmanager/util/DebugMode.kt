package com.luanmuc.openwrtmanager.util

import com.luanmuc.openwrtmanager.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 调试模式（演示模式）工具类
 * 所有调试相关代码集中在此文件，后期删除时直接删除此文件即可
 *
 * 使用方法：在"我的"页面连续点击版本号5次开启/关闭
 * 或者在添加路由器页面选择"添加演示路由器"
 */
object DebugMode {
    // 调试模式开关（StateFlow，可监听变化）
    private val _isDebugModeFlow = MutableStateFlow(false)
    val isDebugModeFlow: StateFlow<Boolean> = _isDebugModeFlow.asStateFlow()
    
    // 调试模式开关（便捷访问）
    var isDebugMode: Boolean
        get() = _isDebugModeFlow.value
        private set(value) {
            _isDebugModeFlow.value = value
        }

    // 版本号点击计数（用于隐藏入口）
    var versionClickCount = 0

    // 演示模式下的可变状态
    private val fakeState = FakeState()

    /**
     * 切换调试模式
     */
    fun toggle() {
        isDebugMode = !isDebugMode
        versionClickCount = 0
        if (isDebugMode) {
            fakeState.reset()
        }
    }

    /**
     * 开启演示模式（用于演示路由器）
     */
    fun enableDemoMode() {
        isDebugMode = true
        fakeState.reset()
    }

    /**
     * 关闭演示模式（切换到真实路由器时调用）
     */
    fun disableDemoMode() {
        isDebugMode = false
    }

    /**
     * 点击版本号，连续点击5次切换调试模式
     * @return true表示触发了切换
     */
    fun onVersionClick(): Boolean {
        versionClickCount++
        if (versionClickCount >= 5) {
            toggle()
            return true
        }
        return false
    }

    // ==================== 模拟延迟 ====================

    /**
     * 模拟延迟
     */
    suspend fun simulateDelay(delayMs: Long = 500) {
        delay(delayMs)
    }

    // ==================== 路由器状态 ====================

    /**
     * 生成假的路由器状态
     */
    fun getFakeRouterStatus(): RouterStatus {
        return RouterStatus(
            hostname = "OpenWrt-Router",
            model = "Generic x86/64",
            firmware = "23.05.0",
            kernel = "5.15.120",
            uptime = 3 * 24 * 3600 + 12 * 3600 + 45 * 60,
            cpuUsage = fakeState.cpuUsage,
            memoryTotal = 512 * 1024 * 1024L,
            memoryUsed = (512 * 1024 * 1024L * (0.3f + fakeState.memoryUsageDelta)).toLong(),
            memoryFree = (512 * 1024 * 1024L * 0.4f).toLong(),
            memoryCached = (512 * 1024 * 1024L * 0.2f).toLong(),
            memoryBuffered = (512 * 1024 * 1024L * 0.1f).toLong(),
            storageTotal = 128 * 1024 * 1024L,
            storageUsed = 45 * 1024 * 1024L,
            storageFree = 83 * 1024 * 1024L,
            loadAverage = listOf(0.23f, 0.18f, 0.15f),
            temperature = 45.5f,
            onlineDevices = 12,
            wanConnected = true,
            wanIp = "192.168.1.100",
            wanUptime = 2 * 24 * 3600 + 8 * 3600
        )
    }

    /**
     * 生成假的挂载点列表
     */
    fun getFakeMountPoints(): List<com.luanmuc.openwrtmanager.ui.storage.MountPointInfo> {
        return listOf(
            com.luanmuc.openwrtmanager.ui.storage.MountPointInfo(
                mountPoint = "/",
                device = "/dev/root",
                filesystem = "squashfs",
                total = 128 * 1024 * 1024L,
                used = 45 * 1024 * 1024L,
                free = 83 * 1024 * 1024L,
                usedPercent = 35.2f
            ),
            com.luanmuc.openwrtmanager.ui.storage.MountPointInfo(
                mountPoint = "/tmp",
                device = "tmpfs",
                filesystem = "tmpfs",
                total = 256 * 1024 * 1024L,
                used = 12 * 1024 * 1024L,
                free = 244 * 1024 * 1024L,
                usedPercent = 4.7f
            ),
            com.luanmuc.openwrtmanager.ui.storage.MountPointInfo(
                mountPoint = "/overlay",
                device = "/dev/mmcblk0p2",
                filesystem = "ext4",
                total = 64 * 1024 * 1024L,
                used = 28 * 1024 * 1024L,
                free = 36 * 1024 * 1024L,
                usedPercent = 43.8f
            )
        )
    }

    /**
     * 生成假的系统信息
     */
    fun getFakeSystemInfo(): SystemInfo {
        return SystemInfo(
            hostname = "OpenWrt-Router",
            model = "Generic x86/64",
            release = "23.05.0",
            kernel = "5.15.120",
            uptime = 3 * 24 * 3600 + 12 * 3600 + 45 * 60,
            load = listOf(0.23f, 0.18f, 0.15f),
            memory = MemoryInfo(
                total = 512 * 1024 * 1024L,
                free = (512 * 1024 * 1024L * 0.4f).toLong(),
                cached = (512 * 1024 * 1024L * 0.2f).toLong(),
                buffered = (512 * 1024 * 1024L * 0.1f).toLong(),
                available = (512 * 1024 * 1024L * 0.5f).toLong()
            ),
            root = StorageInfo(
                total = 128 * 1024 * 1024L,
                free = 83 * 1024 * 1024L,
                used = 45 * 1024 * 1024L
            ),
            swap = SwapInfo(
                total = 0,
                free = 0
            ),
            cpu = CpuInfo(
                model = "Intel(R) Celeron(R) J4125",
                cores = 4,
                frequency = "2.0 GHz"
            )
        )
    }

    // ==================== 网络状态 ====================

    /**
     * 生成假的WAN状态
     */
    fun getFakeWanStatus(): NetworkInterface {
        return NetworkInterface(
            name = "wan",
            device = "eth0",
            proto = "dhcp",
            ipaddr = "192.168.1.100",
            netmask = "255.255.255.0",
            gateway = "192.168.1.1",
            dns = listOf("114.114.114.114", "8.8.8.8"),
            uptime = 2 * 24 * 3600 + 8 * 3600,
            rxBytes = fakeState.wanRxBytes,
            txBytes = fakeState.wanTxBytes,
            rxPackets = 1234567,
            txPackets = 987654,
            isUp = true,
            isConnected = true
        )
    }

    /**
     * 生成假的LAN状态
     */
    fun getFakeLanStatus(): NetworkInterface {
        return NetworkInterface(
            name = "lan",
            device = "br-lan",
            proto = "static",
            ipaddr = "192.168.1.1",
            netmask = "255.255.255.0",
            gateway = "",
            dns = emptyList(),
            uptime = 3 * 24 * 3600 + 12 * 3600,
            rxBytes = 2345678901L,
            txBytes = 1234567890L,
            rxPackets = 3456789,
            txPackets = 2345678,
            isUp = true,
            isConnected = true
        )
    }

    // ==================== WiFi状态 ====================

    /**
     * 生成假的WiFi接口列表
     */
    fun getFakeWifiInterfaces(): List<WifiInterface> {
        return listOf(
            WifiInterface(
                name = "wlan0",
                device = "radio0",
                ssid = "OpenWrt-2.4G",
                encryption = "psk2",
                channel = 6,
                htmode = "HT20",
                txpower = 20,
                isUp = true,
                isGuest = false,
                band = "2.4g",
                clients = 8
            ),
            WifiInterface(
                name = "wlan1",
                device = "radio1",
                ssid = "OpenWrt-5G",
                encryption = "psk2",
                channel = 149,
                htmode = "VHT80",
                txpower = 23,
                isUp = true,
                isGuest = false,
                band = "5g",
                clients = 4
            ),
            WifiInterface(
                name = "wlan0-1",
                device = "radio0",
                ssid = "OpenWrt-Guest",
                encryption = "none",
                channel = 6,
                htmode = "HT20",
                txpower = 20,
                isUp = true,
                isGuest = true,
                band = "2.4g",
                clients = 2
            )
        )
    }

    // ==================== 在线设备 ====================

    /**
     * 生成假的在线设备列表
     */
    fun getFakeOnlineDevices(): List<DeviceInfo> {
        return fakeState.devices
    }

    // ==================== 插件管理 ====================

    /**
     * 生成假的已安装插件列表
     */
    fun getFakeInstalledPackages(): List<PackageInfo> {
        return fakeState.installedPackages
    }

    /**
     * 生成假的可用插件列表
     */
    fun getFakeAvailablePackages(): List<PackageInfo> {
        return fakeState.availablePackages
    }

    /**
     * 生成假的软件源列表
     */
    fun getFakeRepos(): List<RepoInfo> {
        return listOf(
            RepoInfo(
                name = "openwrt_core",
                url = "https://downloads.openwrt.org/releases/23.05.0/targets/x86/64/packages",
                enabled = true,
                priority = 0,
                type = "src/gz"
            ),
            RepoInfo(
                name = "openwrt_base",
                url = "https://downloads.openwrt.org/releases/23.05.0/packages/x86_64/base",
                enabled = true,
                priority = 0,
                type = "src/gz"
            ),
            RepoInfo(
                name = "openwrt_luci",
                url = "https://downloads.openwrt.org/releases/23.05.0/packages/x86_64/luci",
                enabled = true,
                priority = 0,
                type = "src/gz"
            ),
            RepoInfo(
                name = "openwrt_packages",
                url = "https://downloads.openwrt.org/releases/23.05.0/packages/x86_64/packages",
                enabled = true,
                priority = 0,
                type = "src/gz"
            ),
            RepoInfo(
                name = "openwrt_routing",
                url = "https://downloads.openwrt.org/releases/23.05.0/packages/x86_64/routing",
                enabled = true,
                priority = 0,
                type = "src/gz"
            ),
            RepoInfo(
                name = "openwrt_telephony",
                url = "https://downloads.openwrt.org/releases/23.05.0/packages/x86_64/telephony",
                enabled = true,
                priority = 0,
                type = "src/gz"
            ),
            RepoInfo(
                name = "kenzok8",
                url = "https://op.dllkids.xyz/packages/x86_64",
                enabled = false,
                priority = 10,
                type = "src/gz"
            )
        )
    }

    /**
     * 模拟安装插件
     */
    suspend fun simulateInstallPackage(packageName: String): Boolean {
        simulateDelay(1500)
        val pkg = fakeState.availablePackages.find { it.name == packageName }
        if (pkg != null) {
            fakeState.availablePackages = fakeState.availablePackages.filter { it.name != packageName }
            fakeState.installedPackages = fakeState.installedPackages + pkg.copy(installed = true)
            return true
        }
        return false
    }

    /**
     * 模拟卸载插件
     */
    suspend fun simulateRemovePackage(packageName: String): Boolean {
        simulateDelay(1000)
        val pkg = fakeState.installedPackages.find { it.name == packageName }
        if (pkg != null) {
            fakeState.installedPackages = fakeState.installedPackages.filter { it.name != packageName }
            fakeState.availablePackages = fakeState.availablePackages + pkg.copy(installed = false)
            return true
        }
        return false
    }

    // ==================== 防火墙 ====================

    /**
     * 生成假的端口转发规则
     */
    fun getFakePortForwards(): List<PortForwardRule> {
        return fakeState.portForwards
    }

    /**
     * 模拟添加端口转发规则
     */
    suspend fun simulateAddPortForward(rule: PortForwardRule): Boolean {
        simulateDelay(800)
        fakeState.portForwards = fakeState.portForwards + rule
        return true
    }

    /**
     * 模拟删除端口转发规则
     */
    suspend fun simulateDeletePortForward(ruleName: String): Boolean {
        simulateDelay(600)
        fakeState.portForwards = fakeState.portForwards.filter { it.name != ruleName }
        return true
    }

    /**
     * 模拟切换端口转发规则状态
     */
    suspend fun simulateTogglePortForward(ruleName: String): Boolean {
        simulateDelay(400)
        fakeState.portForwards = fakeState.portForwards.map {
            if (it.name == ruleName) it.copy(enabled = !it.enabled) else it
        }
        return true
    }

    // ==================== DDNS ====================

    /**
     * 生成假的DDNS配置
     */
    fun getFakeDdnsConfigs(): List<DdnsConfig> {
        return fakeState.ddnsConfigs
    }

    /**
     * 模拟添加DDNS配置
     */
    suspend fun simulateAddDdnsConfig(config: DdnsConfig): Boolean {
        simulateDelay(800)
        fakeState.ddnsConfigs = fakeState.ddnsConfigs + config
        return true
    }

    /**
     * 模拟删除DDNS配置
     */
    suspend fun simulateDeleteDdnsConfig(configName: String): Boolean {
        simulateDelay(600)
        fakeState.ddnsConfigs = fakeState.ddnsConfigs.filter { it.name != configName }
        return true
    }

    // ==================== 系统日志和进程 ====================

    /**
     * 生成假的系统日志
     */
    fun getFakeSystemLog(): List<LogEntry> {
        return listOf(
            LogEntry(time = "Aug  9 10:00:01", level = "info", facility = "system", message = "System started"),
            LogEntry(time = "Aug  9 10:00:02", level = "info", facility = "kernel", message = "Loading kernel modules..."),
            LogEntry(time = "Aug  9 10:00:03", level = "info", facility = "kernel", message = "eth0: link up, 1000Mbps full-duplex"),
            LogEntry(time = "Aug  9 10:00:05", level = "info", facility = "network", message = "wan: DHCP lease obtained, IP 192.168.1.100"),
            LogEntry(time = "Aug  9 10:00:10", level = "info", facility = "dns", message = "dnsmasq: started, version 2.89"),
            LogEntry(time = "Aug  9 10:00:15", level = "info", facility = "firewall", message = "firewall: started"),
            LogEntry(time = "Aug  9 10:01:23", level = "info", facility = "wifi", message = "wlan0: AP-STA-CONNECTED AA:BB:CC:DD:EE:01"),
            LogEntry(time = "Aug  9 10:02:45", level = "warn", facility = "wifi", message = "wlan0: AP-STA-DISCONNECTED AA:BB:CC:DD:EE:02"),
            LogEntry(time = "Aug  9 10:05:30", level = "info", facility = "wifi", message = "wlan0: AP-STA-CONNECTED AA:BB:CC:DD:EE:02"),
            LogEntry(time = "Aug  9 10:10:00", level = "info", facility = "ntp", message = "ntpd: time synchronized with 114.114.114.114"),
            LogEntry(time = "Aug  9 10:15:22", level = "info", facility = "wifi", message = "wlan1: AP-STA-CONNECTED AA:BB:CC:DD:EE:04"),
            LogEntry(time = "Aug  9 10:20:15", level = "warn", facility = "dns", message = "dnsmasq: possible DNS-rebind attack detected"),
            LogEntry(time = "Aug  9 10:30:00", level = "info", facility = "cron", message = "cron: running scheduled tasks"),
            LogEntry(time = "Aug  9 10:45:33", level = "info", facility = "wifi", message = "wlan0: AP-STA-CONNECTED AA:BB:CC:DD:EE:07"),
            LogEntry(time = "Aug  9 11:00:00", level = "info", facility = "system", message = "System uptime: 3 days, 12 hours, 45 minutes")
        )
    }

    /**
     * 生成假的进程列表
     */
    fun getFakeProcessList(): List<ProcessInfo> {
        return listOf(
            ProcessInfo(pid = 1, name = "init", cpu = 0.0f, memory = 0.5f, vsz = 1024L, user = "root", command = "/sbin/init"),
            ProcessInfo(pid = 2, name = "kthreadd", cpu = 0.0f, memory = 0.0f, vsz = 0L, user = "root", command = "[kthreadd]"),
            ProcessInfo(pid = 123, name = "netifd", cpu = 0.2f, memory = 1.2f, vsz = 2048L, user = "root", command = "/sbin/netifd"),
            ProcessInfo(pid = 234, name = "uhttpd", cpu = 0.1f, memory = 0.8f, vsz = 1536L, user = "root", command = "/usr/sbin/uhttpd"),
            ProcessInfo(pid = 345, name = "dnsmasq", cpu = 0.3f, memory = 1.5f, vsz = 3072L, user = "dnsmasq", command = "/usr/sbin/dnsmasq"),
            ProcessInfo(pid = 456, name = "hostapd", cpu = 0.5f, memory = 2.1f, vsz = 4096L, user = "root", command = "/usr/sbin/hostapd"),
            ProcessInfo(pid = 567, name = "wpa_supplicant", cpu = 0.1f, memory = 0.9f, vsz = 1792L, user = "root", command = "/usr/sbin/wpa_supplicant"),
            ProcessInfo(pid = 678, name = "crond", cpu = 0.0f, memory = 0.3f, vsz = 512L, user = "root", command = "/usr/sbin/crond"),
            ProcessInfo(pid = 789, name = "syslogd", cpu = 0.0f, memory = 0.4f, vsz = 768L, user = "root", command = "/sbin/syslogd"),
            ProcessInfo(pid = 890, name = "klogd", cpu = 0.0f, memory = 0.2f, vsz = 384L, user = "root", command = "/sbin/klogd"),
            ProcessInfo(pid = 1024, name = "dropbear", cpu = 0.0f, memory = 0.6f, vsz = 1152L, user = "root", command = "/usr/sbin/dropbear"),
            ProcessInfo(pid = 2048, name = "ubusd", cpu = 0.1f, memory = 0.7f, vsz = 1280L, user = "root", command = "/sbin/ubusd"),
            ProcessInfo(pid = 3072, name = "rpcd", cpu = 0.2f, memory = 1.0f, vsz = 2048L, user = "root", command = "/sbin/rpcd"),
            ProcessInfo(pid = 4096, name = "odhcpd", cpu = 0.1f, memory = 0.8f, vsz = 1536L, user = "root", command = "/usr/sbin/odhcpd"),
            ProcessInfo(pid = 5120, name = "firewall", cpu = 0.0f, memory = 0.5f, vsz = 1024L, user = "root", command = "/sbin/fw3")
        )
    }

    // ==================== DHCP静态租约 ====================

    /**
     * 生成假的DHCP静态租约
     */
    fun getFakeDhcpStaticLeases(): List<DhcpStaticLease> {
        return listOf(
            DhcpStaticLease(mac = "AA:BB:CC:DD:EE:01", ip = "192.168.1.10", hostname = "iPhone-15-Pro"),
            DhcpStaticLease(mac = "AA:BB:CC:DD:EE:02", ip = "192.168.1.11", hostname = "MacBook-Pro"),
            DhcpStaticLease(mac = "AA:BB:CC:DD:EE:05", ip = "192.168.1.14", hostname = "Desktop-PC"),
            DhcpStaticLease(mac = "AA:BB:CC:DD:EE:10", ip = "192.168.1.19", hostname = "Security-Camera"),
            DhcpStaticLease(mac = "AA:BB:CC:DD:EE:12", ip = "192.168.1.21", hostname = "NAS-Server")
        )
    }

    // ==================== 定时任务 ====================

    /**
     * 生成假的定时任务
     */
    fun getFakeCronJobs(): List<CronJob> {
        return listOf(
            CronJob(id = 1, minute = "0", hour = "3", dayOfMonth = "*", month = "*", dayOfWeek = "*", command = "/usr/sbin/ntpd -q -p 114.114.114.114", enabled = true),
            CronJob(id = 2, minute = "*/30", hour = "*", dayOfMonth = "*", month = "*", dayOfWeek = "*", command = "/usr/bin/ddns_update.sh", enabled = true),
            CronJob(id = 3, minute = "0", hour = "4", dayOfMonth = "*", month = "*", dayOfWeek = "0", command = "/sbin/reboot", enabled = false),
            CronJob(id = 4, minute = "0", hour = "0", dayOfMonth = "1", month = "*", dayOfWeek = "*", command = "/usr/bin/backup.sh", enabled = true)
        )
    }

    // ==================== 流量和CPU历史数据 ====================

    /**
     * 生成假的流量历史数据
     */
    fun getFakeTrafficHistory(points: Int = 30): List<TrafficDataPoint> {
        val now = System.currentTimeMillis()
        val baseRx = 1567890123L
        val baseTx = 987654321L
        return (0 until points).map { i ->
            val time = now - (points - i) * 60000L
            val rx = baseRx + i * 1024 * 1024L + Random.nextLong(0, 512 * 1024L)
            val tx = baseTx + i * 512 * 1024L + Random.nextLong(0, 256 * 1024L)
            TrafficDataPoint(time = time, rx = rx, tx = tx)
        }
    }

    /**
     * 生成假的CPU历史数据
     */
    fun getFakeCpuHistory(points: Int = 30): List<CpuDataPoint> {
        val now = System.currentTimeMillis()
        return (0 until points).map { i ->
            val time = now - (points - i) * 60000L
            val usage = 15f + Random.nextFloat() * 30f
            CpuDataPoint(time = time, usage = usage)
        }
    }

    // ==================== 智能诊断 ====================

    /**
     * 生成假的诊断结果
     */
    fun getFakeDiagnosticResult(): NetworkDiagnostic.DiagnosticResult {
        return NetworkDiagnostic.DiagnosticResult(
            isHealthy = true,
            issues = listOf(
                NetworkDiagnostic.DiagnosticIssue(
                    type = NetworkDiagnostic.IssueType.WIFI_SIGNAL,
                    severity = NetworkDiagnostic.Severity.WARNING,
                    description = Loc.s(R.string.s0871),
                    suggestion = Loc.s(R.string.s0872)
                ),
                NetworkDiagnostic.DiagnosticIssue(
                    type = NetworkDiagnostic.IssueType.CONNECTION,
                    severity = NetworkDiagnostic.Severity.INFO,
                    description = Loc.s(R.string.s0873),
                    suggestion = Loc.s(R.string.s0874)
                ),
                NetworkDiagnostic.DiagnosticIssue(
                    type = NetworkDiagnostic.IssueType.CPU_USAGE,
                    severity = NetworkDiagnostic.Severity.INFO,
                    description = Loc.s(R.string.s0875),
                    suggestion = ""
                )
            ),
            suggestions = listOf(
                Loc.s(R.string.s0876),
                Loc.s(R.string.s0877)
            )
        )
    }

    // ==================== 系统操作模拟 ====================

    /**
     * 模拟重启路由器
     */
    suspend fun simulateReboot(): Boolean {
        simulateDelay(3000)
        return true
    }

    /**
     * 模拟恢复出厂设置
     */
    suspend fun simulateFactoryReset(): Boolean {
        simulateDelay(5000)
        fakeState.reset()
        return true
    }

    // ==================== 实时数据更新 ====================

    /**
     * 更新实时数据（每次调用都会变化）
     */
    fun updateRealtimeData() {
        // CPU使用率在15-45%之间波动
        fakeState.cpuUsage = 15f + Random.nextFloat() * 30f

        // 内存使用率小幅波动
        fakeState.memoryUsageDelta = Random.nextFloat() * 0.1f - 0.05f

        // WAN流量增加
        fakeState.wanRxBytes += Random.nextLong(1024, 65536)
        fakeState.wanTxBytes += Random.nextLong(512, 32768)

        // 设备流量变化
        fakeState.devices = fakeState.devices.map { device ->
            device.copy(
                rxBytes = device.rxBytes + Random.nextLong(1024, 16384),
                txBytes = device.txBytes + Random.nextLong(512, 8192)
            )
        }
    }

    // ==================== 演示路由器 ====================

    /**
     * 获取演示路由器信息
     */
    fun getDemoRouter(): Router {
        return Router(
            id = "demo-router",
            name = Loc.s(R.string.s0878),
            address = "192.168.1.1",
            username = "root",
            encryptedPassword = "",
            isConnected = true,
            lastConnected = System.currentTimeMillis()
        )
    }

    /**
     * 检查是否是演示路由器
     */
    fun isDemoRouter(routerId: String): Boolean {
        return routerId == "demo-router"
    }

    // ==================== 内部状态类 ====================

    private class FakeState {
        var cpuUsage: Float = 23.5f
        var memoryUsageDelta: Float = 0f
        var wanRxBytes: Long = 1567890123L
        var wanTxBytes: Long = 987654321L

        var devices: List<DeviceInfo> = emptyList()
        var installedPackages: List<PackageInfo> = emptyList()
        var availablePackages: List<PackageInfo> = emptyList()
        var portForwards: List<PortForwardRule> = emptyList()
        var ddnsConfigs: List<DdnsConfig> = emptyList()

        init {
            reset()
        }

        fun reset() {
            cpuUsage = 23.5f
            memoryUsageDelta = 0f
            wanRxBytes = 1567890123L
            wanTxBytes = 987654321L

            devices = listOf(
                DeviceInfo(
                    ip = "192.168.1.10",
                    mac = "AA:BB:CC:DD:EE:01",
                    hostname = "iPhone-15-Pro",
                    vendor = "Apple Inc.",
                    interfaceName = "wlan0",
                    connectedTime = 86400,
                    rxBytes = 567890123L,
                    txBytes = 234567890L,
                    signal = -45,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.11",
                    mac = "AA:BB:CC:DD:EE:02",
                    hostname = "MacBook-Pro",
                    vendor = "Apple Inc.",
                    interfaceName = "wlan0",
                    connectedTime = 72000,
                    rxBytes = 1234567890L,
                    txBytes = 987654321L,
                    signal = -52,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.12",
                    mac = "AA:BB:CC:DD:EE:03",
                    hostname = "Mi-14-Ultra",
                    vendor = "Xiaomi Communications Co., Ltd.",
                    interfaceName = "wlan0",
                    connectedTime = 54000,
                    rxBytes = 345678901L,
                    txBytes = 123456789L,
                    signal = -58,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.13",
                    mac = "AA:BB:CC:DD:EE:04",
                    hostname = "iPad-Pro",
                    vendor = "Apple Inc.",
                    interfaceName = "wlan1",
                    connectedTime = 36000,
                    rxBytes = 789012345L,
                    txBytes = 456789012L,
                    signal = -62,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.14",
                    mac = "AA:BB:CC:DD:EE:05",
                    hostname = "Desktop-PC",
                    vendor = "Intel Corporate",
                    interfaceName = "eth1",
                    connectedTime = 259200,
                    rxBytes = 2345678901L,
                    txBytes = 1234567890L,
                    signal = 0,
                    isOnline = true,
                    isBlocked = false,
                    note = Loc.s(R.string.s0879)
                ),
                DeviceInfo(
                    ip = "192.168.1.15",
                    mac = "AA:BB:CC:DD:EE:06",
                    hostname = "Smart-TV",
                    vendor = "LG Electronics",
                    interfaceName = "wlan0",
                    connectedTime = 172800,
                    rxBytes = 1567890123L,
                    txBytes = 123456789L,
                    signal = -68,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.16",
                    mac = "AA:BB:CC:DD:EE:07",
                    hostname = "Switch-Lite",
                    vendor = "Nintendo Co., Ltd.",
                    interfaceName = "wlan0",
                    connectedTime = 43200,
                    rxBytes = 234567890L,
                    txBytes = 56789012L,
                    signal = -72,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.17",
                    mac = "AA:BB:CC:DD:EE:08",
                    hostname = "Smart-Speaker",
                    vendor = "Amazon.com Inc.",
                    interfaceName = "wlan0",
                    connectedTime = 604800,
                    rxBytes = 12345678L,
                    txBytes = 3456789L,
                    signal = -75,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.18",
                    mac = "AA:BB:CC:DD:EE:09",
                    hostname = "Smart-Bulb",
                    vendor = "Philips Lighting",
                    interfaceName = "wlan0",
                    connectedTime = 864000,
                    rxBytes = 1234567L,
                    txBytes = 234567L,
                    signal = -80,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.19",
                    mac = "AA:BB:CC:DD:EE:10",
                    hostname = "Security-Camera",
                    vendor = "Hangzhou Hikvision Digital Technology",
                    interfaceName = "eth2",
                    connectedTime = 1209600,
                    rxBytes = 3456789012L,
                    txBytes = 123456789L,
                    signal = 0,
                    isOnline = true,
                    isBlocked = false,
                    note = Loc.s(R.string.s0880)
                ),
                DeviceInfo(
                    ip = "192.168.1.20",
                    mac = "AA:BB:CC:DD:EE:11",
                    hostname = "Printer",
                    vendor = "HP Inc.",
                    interfaceName = "eth3",
                    connectedTime = 2592000,
                    rxBytes = 12345678L,
                    txBytes = 5678901L,
                    signal = 0,
                    isOnline = true,
                    isBlocked = false,
                    note = ""
                ),
                DeviceInfo(
                    ip = "192.168.1.21",
                    mac = "AA:BB:CC:DD:EE:12",
                    hostname = "NAS-Server",
                    vendor = "Synology Inc.",
                    interfaceName = "eth1",
                    connectedTime = 5184000,
                    rxBytes = 5678901234L,
                    txBytes = 3456789012L,
                    signal = 0,
                    isOnline = true,
                    isBlocked = false,
                    note = Loc.s(R.string.s0881)
                )
            )

            installedPackages = listOf(
                PackageInfo(
                    name = "luci-i18n-base-zh-cn",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0882),
                    size = 262144L,
                    installed = true,
                    category = "system",
                    depends = listOf("luci-base")
                ),
                PackageInfo(
                    name = "luci-app-opkg",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0883),
                    size = 65536L,
                    installed = true,
                    category = "system",
                    depends = listOf("luci-base", "opkg")
                ),
                PackageInfo(
                    name = "luci-app-firewall",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0884),
                    size = 131072L,
                    installed = true,
                    category = "network",
                    depends = listOf("luci-base", "firewall4")
                ),
                PackageInfo(
                    name = "luci-app-ddns",
                    version = "2.4.0",
                    description = Loc.s(R.string.s0885),
                    size = 98304L,
                    installed = true,
                    category = "network",
                    depends = listOf("luci-base", "ddns-scripts")
                ),
                PackageInfo(
                    name = "luci-app-upnp",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0886),
                    size = 49152L,
                    installed = true,
                    category = "network",
                    depends = listOf("luci-base", "miniupnpd")
                ),
                PackageInfo(
                    name = "luci-app-wol",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0887),
                    size = 32768L,
                    installed = true,
                    category = "network",
                    depends = listOf("luci-base", "etherwake")
                ),
                PackageInfo(
                    name = "luci-app-sqm",
                    version = "1.6.0",
                    description = Loc.s(R.string.s0888),
                    size = 81920L,
                    installed = true,
                    category = "network",
                    depends = listOf("luci-base", "sqm-scripts")
                ),
                PackageInfo(
                    name = "luci-app-statistics",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0889),
                    size = 163840L,
                    installed = true,
                    category = "admin",
                    depends = listOf("luci-base", "collectd")
                )
            )

            availablePackages = listOf(
                PackageInfo(
                    name = "luci-app-aria2",
                    version = "1.36.0",
                    description = Loc.s(R.string.s0890),
                    size = 131072L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "aria2")
                ),
                PackageInfo(
                    name = "luci-app-transmission",
                    version = "4.0.0",
                    description = Loc.s(R.string.s0891),
                    size = 163840L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "transmission-daemon")
                ),
                PackageInfo(
                    name = "luci-app-samba4",
                    version = "4.18.0",
                    description = Loc.s(R.string.s0892),
                    size = 98304L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "samba4-server")
                ),
                PackageInfo(
                    name = "luci-app-minidlna",
                    version = "1.3.0",
                    description = Loc.s(R.string.s0893),
                    size = 65536L,
                    installed = false,
                    category = "multimedia",
                    depends = listOf("luci-base", "minidlna")
                ),
                PackageInfo(
                    name = "luci-app-openvpn",
                    version = "2.5.0",
                    description = Loc.s(R.string.s0894),
                    size = 196608L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "openvpn-openssl")
                ),
                PackageInfo(
                    name = "luci-app-wireguard",
                    version = "1.0.0",
                    description = Loc.s(R.string.s0895),
                    size = 81920L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "wireguard-tools")
                ),
                PackageInfo(
                    name = "luci-app-mwan3",
                    version = "2.11.0",
                    description = Loc.s(R.string.s0896),
                    size = 147456L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "mwan3")
                ),
                PackageInfo(
                    name = "luci-app-snmpd",
                    version = "5.9.0",
                    description = Loc.s(R.string.s0897),
                    size = 73728L,
                    installed = false,
                    category = "admin",
                    depends = listOf("luci-base", "snmpd")
                ),
                PackageInfo(
                    name = "luci-app-nlbwmon",
                    version = "2023.09.12",
                    description = Loc.s(R.string.s0898),
                    size = 57344L,
                    installed = false,
                    category = "admin",
                    depends = listOf("luci-base", "nlbwmon")
                ),
                PackageInfo(
                    name = "luci-app-ntpc",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0899),
                    size = 40960L,
                    installed = false,
                    category = "system",
                    depends = listOf("luci-base", "ntpclient")
                ),
                PackageInfo(
                    name = "luci-app-uhttpd",
                    version = "2023.06.25",
                    description = Loc.s(R.string.s0900),
                    size = 49152L,
                    installed = false,
                    category = "system",
                    depends = listOf("luci-base", "uhttpd")
                ),
                PackageInfo(
                    name = "luci-app-acl",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0901),
                    size = 65536L,
                    installed = false,
                    category = "system",
                    depends = listOf("luci-base")
                ),
                PackageInfo(
                    name = "luci-app-adblock",
                    version = "4.1.5",
                    description = Loc.s(R.string.s0902),
                    size = 114688L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "adblock")
                ),
                PackageInfo(
                    name = "luci-app-ttyd",
                    version = "1.7.3",
                    description = Loc.s(R.string.s0903),
                    size = 45056L,
                    installed = false,
                    category = "utilities",
                    depends = listOf("luci-base", "ttyd")
                ),
                PackageInfo(
                    name = "luci-app-hd-idle",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0904),
                    size = 36864L,
                    installed = false,
                    category = "system",
                    depends = listOf("luci-base", "hd-idle")
                ),
                PackageInfo(
                    name = "luci-app-vsftpd",
                    version = "23.05.0",
                    description = Loc.s(R.string.s0905),
                    size = 53248L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "vsftpd")
                ),
                PackageInfo(
                    name = "luci-app-udpxy",
                    version = "2023.02.06",
                    description = Loc.s(R.string.s0906),
                    size = 40960L,
                    installed = false,
                    category = "multimedia",
                    depends = listOf("luci-base", "udpxy")
                ),
                PackageInfo(
                    name = "luci-app-frps",
                    version = "0.52.0",
                    description = Loc.s(R.string.s0907),
                    size = 90112L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "frps")
                ),
                PackageInfo(
                    name = "luci-app-frpc",
                    version = "0.52.0",
                    description = Loc.s(R.string.s0908),
                    size = 86016L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "frpc")
                ),
                PackageInfo(
                    name = "luci-app-zerotier",
                    version = "1.12.2",
                    description = Loc.s(R.string.s0909),
                    size = 73728L,
                    installed = false,
                    category = "network",
                    depends = listOf("luci-base", "zerotier")
                )
            )

            portForwards = listOf(
                PortForwardRule(name = "HTTP_Server", proto = "tcp", src = "wan", srcPort = "8080", dest = "lan", destIp = "192.168.1.10", destPort = "80", enabled = true),
                PortForwardRule(name = "SSH_Access", proto = "tcp", src = "wan", srcPort = "2222", dest = "lan", destIp = "192.168.1.14", destPort = "22", enabled = true),
                PortForwardRule(name = "RDP_Desktop", proto = "tcp", src = "wan", srcPort = "3389", dest = "lan", destIp = "192.168.1.15", destPort = "3389", enabled = false)
            )

            ddnsConfigs = listOf(
                DdnsConfig(name = "myddns", service = "no-ip.com", domain = "myhome.ddns.net", username = "user@example.com", password = "", interfaceName = "wan", enabled = true, status = Loc.s(R.string.s0502)),
                DdnsConfig(name = "aliyun", service = "aliyun.com", domain = "home.example.com", username = "access_key", password = "", interfaceName = "wan", enabled = false, status = Loc.s(R.string.s0334))
            )
        }
    }

    /**
     * 获取假的系统信息
     */
    fun getFakeFullSystemInfo(): FullSystemInfo {
        return FullSystemInfo(
            hostname = "OpenWrt-Router",
            model = "Generic x86/64",
            firmwareVersion = "23.05.0",
            kernelVersion = "5.15.120",
            architecture = "x86_64",
            packageManager = PackageManagerType.OPKG,
            boardName = "x86_64",
            release = "23.05.0",
            distribution = "OpenWrt",
            revision = "r23497-6637af95aa",
            target = "x86/64",
            description = "OpenWrt 23.05.0 r23497-6637af95aa",
            title = "OpenWrt 23.05.0"
        )
    }

    /**
     * 模拟切换包管理器模式（用于演示）
     */
    fun simulatePackageManagerMode(mode: PackageManagerType) {
        // 这个方法用于演示模式下切换包管理器显示
        // 实际实现可以根据需要修改
    }

    /**
     * 模拟架构验证
     */
    fun simulateArchitectureValidation(packageName: String, architecture: String): Boolean {
        // 演示模式下默认验证通过
        return true
    }

    /**
     * 获取假的固件信息
     */
    fun getFakeFirmwareInfo(): com.luanmuc.openwrtmanager.data.model.FirmwareInfo {
        return com.luanmuc.openwrtmanager.data.model.FirmwareInfo(
            currentVersion = "23.05.0",
            currentBuildTime = "2024-01-15 10:30:00",
            deviceModel = "Generic x86/64",
            architecture = "x86_64",
            kernelVersion = "5.15.120",
            boardName = "x86_64"
        )
    }

    /**
     * 获取假的最新固件版本
     */
    fun getFakeLatestFirmware(): com.luanmuc.openwrtmanager.data.model.FirmwareRelease {
        return com.luanmuc.openwrtmanager.data.model.FirmwareRelease(
            version = "23.05.2",
            releaseDate = "2024-03-20",
            size = 1024 * 1024 * 16, // 16MB
            md5 = "abc123def456",
            sha256 = "sha256hash123456",
            downloadUrl = "https://example.com/firmware.bin",
            changelog = Loc.s(R.string.s0910),
            isNewer = true
        )
    }

    /**
     * 获取假的网口状态
     */
    fun getFakePortStatus(): List<com.luanmuc.openwrtmanager.data.model.PortStatus> {
        return listOf(
            com.luanmuc.openwrtmanager.data.model.PortStatus(
                name = "eth0",
                displayName = "WAN",
                type = com.luanmuc.openwrtmanager.data.model.PortType.WAN,
                isConnected = true,
                speed = 1000,
                duplex = "Full",
                rxBytes = 1572864000,
                txBytes = 1048576000,
                rxPackets = 1250000,
                txPackets = 980000,
                macAddress = "00:11:22:33:44:55"
            ),
            com.luanmuc.openwrtmanager.data.model.PortStatus(
                name = "eth1",
                displayName = "LAN1",
                type = com.luanmuc.openwrtmanager.data.model.PortType.LAN,
                isConnected = true,
                speed = 1000,
                duplex = "Full",
                rxBytes = 524288000,
                txBytes = 786432000,
                rxPackets = 450000,
                txPackets = 620000,
                macAddress = "00:11:22:33:44:56"
            ),
            com.luanmuc.openwrtmanager.data.model.PortStatus(
                name = "eth2",
                displayName = "LAN2",
                type = com.luanmuc.openwrtmanager.data.model.PortType.LAN,
                isConnected = true,
                speed = 100,
                duplex = "Full",
                rxBytes = 104857600,
                txBytes = 52428800,
                rxPackets = 120000,
                txPackets = 80000,
                macAddress = "00:11:22:33:44:57"
            ),
            com.luanmuc.openwrtmanager.data.model.PortStatus(
                name = "eth3",
                displayName = "LAN3",
                type = com.luanmuc.openwrtmanager.data.model.PortType.LAN,
                isConnected = false,
                speed = 0,
                duplex = "Unknown",
                rxBytes = 0,
                txBytes = 0,
                rxPackets = 0,
                txPackets = 0,
                macAddress = "00:11:22:33:44:58"
            )
        )
    }

    /**
     * 获取假的设备能力
     */
    fun getFakeDeviceCapabilities(): com.luanmuc.openwrtmanager.data.model.DeviceCapabilities {
        return com.luanmuc.openwrtmanager.data.model.DeviceCapabilities(
            hasWifi = simulateWifiEnabled,
            hasUsb = true,
            hasSfp = false,
            wifiInterfaceCount = if (simulateWifiEnabled) 2 else 0,
            lanPortCount = 3,
            wanPortCount = 1,
            totalPortCount = if (simulateWifiEnabled) 6 else 4,
            packageManager = com.luanmuc.openwrtmanager.data.model.PackageManagerType.OPKG,
            architecture = "x86_64"
        )
    }

    // 演示模式WiFi开关
    private var simulateWifiEnabled = false
    
    /**
     * 设置演示模式是否模拟有WiFi
     */
    fun setSimulateWifiEnabled(enabled: Boolean) {
        simulateWifiEnabled = enabled
    }
    
    /**
     * 获取演示模式是否模拟有WiFi
     */
    fun isSimulateWifiEnabled(): Boolean {
        return simulateWifiEnabled
    }
    
    /**
     * 切换演示模式WiFi开关
     */
    fun toggleSimulateWifi() {
        simulateWifiEnabled = !simulateWifiEnabled
    }
}
