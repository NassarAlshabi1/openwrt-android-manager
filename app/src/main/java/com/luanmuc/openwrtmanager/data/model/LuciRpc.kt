package com.luanmuc.openwrtmanager.data.model

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * LuCI ubus RPC 请求封装
 * 基于 OpenWrt rpcd ubus JSON-RPC 2.0 协议
 */
data class LuciRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String = "call",
    val params: List<Any>
) {
    companion object {
        /**
         * 创建登录请求
         */
        fun loginRequest(username: String, password: String): LuciRpcRequest {
            return LuciRpcRequest(
                id = 1,
                params = listOf(
                    "00000000000000000000000000000000",
                    "session",
                    "login",
                    mapOf(
                        "username" to username,
                        "password" to password
                    )
                )
            )
        }

        /**
         * 创建通用调用请求
         */
        fun callRequest(
            sessionId: String,
            obj: String,
            method: String,
            params: Map<String, Any> = emptyMap()
        ): LuciRpcRequest {
            return LuciRpcRequest(
                id = (Math.random() * 10000).toInt(),
                params = listOf(sessionId, obj, method, params)
            )
        }
    }
}

/**
 * LuCI ubus RPC 响应封装
 * result 是一个数组：[statusCode, data]
 * statusCode 0 表示成功
 */
data class LuciRpcResponse<T>(
    val jsonrpc: String?,
    val id: Int,
    val result: T?,
    val error: LuciRpcError?
)

data class LuciRpcError(
    val code: Int,
    val message: String
)

/**
 * 登录响应数据
 */
data class LoginResult(
    val ubus_rpc_session: String,
    val expires: Int?,
    val timeout: Int?,
    val username: String?,
    val section: String?
)

/**
 * 系统信息
 */
data class SystemInfo(
    val hostname: String? = null,
    val model: String? = null,
    val release: String? = null,
    val kernel: String? = null,
    val uptime: Long? = null,
    val load: List<Float>? = null,
    val memory: MemoryInfo? = null,
    val root: StorageInfo? = null,
    val swap: SwapInfo? = null,
    val cpu: CpuInfo? = null
)

data class MemoryInfo(
    val total: Long? = null,
    val free: Long? = null,
    val cached: Long? = null,
    val buffered: Long? = null,
    val shared: Long? = null,
    val available: Long? = null
)

data class StorageInfo(
    val total: Long? = null,
    val free: Long? = null,
    val used: Long? = null
)

data class SwapInfo(
    val total: Long? = null,
    val free: Long? = null
)

data class CpuInfo(
    val model: String? = null,
    val cores: Int? = null,
    val frequency: String? = null
)

/**
 * 在线设备
 */
data class DeviceInfo(
    val ip: String = "",
    val mac: String = "",
    val hostname: String = "",
    val vendor: String = "",
    val interfaceName: String = "",
    val connectedTime: Long = 0,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val signal: Int = 0,
    val isOnline: Boolean = true,
    val isBlocked: Boolean = false,
    val note: String = ""
)

/**
 * 网络接口信息
 */
data class NetworkInterface(
    val name: String = "",
    val device: String = "",
    val proto: String = "",
    val ipaddr: String = "",
    val netmask: String = "",
    val gateway: String = "",
    val dns: List<String> = emptyList(),
    val uptime: Long = 0,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxPackets: Long = 0,
    val txPackets: Long = 0,
    val isUp: Boolean = false,
    val isConnected: Boolean = false
)

/**
 * WiFi 接口信息
 */
data class WifiInterface(
    val name: String = "",
    val device: String = "",
    val ssid: String = "",
    val encryption: String = "",
    val channel: Int = 0,
    val htmode: String = "",
    val txpower: Int = 0,
    val isUp: Boolean = false,
    val isGuest: Boolean = false,
    val band: String = "2.4g",
    val clients: Int = 0
)

/**
 * 插件信息
 */
data class PackageInfo(
    val name: String = "",
    val version: String = "",
    val description: String = "",
    val size: Long = 0,
    val installed: Boolean = false,
    val category: String = "",
    val depends: List<String> = emptyList(),
    val architecture: String = ""
)

/**
 * 软件源信息
 */
data class RepoInfo(
    val name: String = "",
    val url: String = "",
    val enabled: Boolean = true,
    val priority: Int = 0,
    val type: String = "src/gz",
    val repoType: RepoType = RepoType.CUSTOM
)

/**
 * 软件源类型
 */
enum class RepoType {
    OFFICIAL_MIRROR,
    THIRD_PARTY,
    CUSTOM;
    val displayName: String
        get() = when (this) {
            OFFICIAL_MIRROR -> Loc.s(R.string.enum_072)
            THIRD_PARTY -> Loc.s(R.string.enum_073)
            CUSTOM -> Loc.s(R.string.enum_074)
        }
}

/**
 * 推荐插件信息
 */
data class RecommendedPlugin(
    val name: String = "",
    val displayName: String = "",
    val description: String = "",
    val category: String = "",
    val icon: String = ""
)

/**
 * 端口转发规则
 */
data class PortForwardRule(
    val name: String = "",
    val proto: String = "tcp",
    val src: String = "wan",
    val srcPort: String = "",
    val dest: String = "lan",
    val destIp: String = "",
    val destPort: String = "",
    val enabled: Boolean = true
)

/**
 * DDNS 配置
 */
data class DdnsConfig(
    val name: String = "",
    val service: String = "",
    val domain: String = "",
    val username: String = "",
    val password: String = "",
    val interfaceName: String = "wan",
    val enabled: Boolean = true,
    val status: String = ""
)

/**
 * 进程信息
 */
data class ProcessInfo(
    val pid: Int = 0,
    val name: String = "",
    val cpu: Float = 0f,
    val memory: Float = 0f,
    val vsz: Long = 0,
    val rss: Long = 0,
    val user: String = "",
    val command: String = ""
)

/**
 * 系统日志条目
 */
data class LogEntry(
    val time: String = "",
    val level: String = "",
    val facility: String = "",
    val message: String = ""
)

/**
 * DHCP 静态租约
 */
data class DhcpStaticLease(
    val mac: String = "",
    val ip: String = "",
    val hostname: String = ""
)

/**
 * 定时任务
 */
data class CronJob(
    val id: Int = 0,
    val minute: String = "*",
    val hour: String = "*",
    val dayOfMonth: String = "*",
    val month: String = "*",
    val dayOfWeek: String = "*",
    val command: String = "",
    val enabled: Boolean = true
)

/**
 * 流量统计数据点
 */
data class TrafficDataPoint(
    val time: Long = 0,
    val rx: Long = 0,
    val tx: Long = 0
)

/**
 * CPU 使用率数据点
 */
data class CpuDataPoint(
    val time: Long = 0,
    val usage: Float = 0f
)


/**
 * 系统信息
 */
data class FullSystemInfo(
    val hostname: String = "",
    val model: String = "",
    val firmwareVersion: String = "",
    val kernelVersion: String = "",
    val architecture: String = "",
    val packageManager: PackageManagerType = PackageManagerType.OPKG,
    val boardName: String = "",
    val release: String = "",
    val distribution: String = "",
    val revision: String = "",
    val target: String = "",
    val description: String = "",
    val title: String = ""
)

/**
 * 包管理器类型
 */
enum class PackageManagerType {
    OPKG,
    APK,
    UNKNOWN;
    val displayName: String
        get() = when (this) {
            OPKG -> "OPKG"
            APK -> "APK"
            UNKNOWN -> Loc.s(R.string.enum_075)
        }
}

/**
 * 架构类型
 */
enum class ArchitectureType(val opkgArch: String, val apkArch: String) {
    X86_64("x86_64", "x86_64"),
    ARMV7("arm_cortex-a7_neon-vfpv4", "armv7"),
    AARCH64("aarch64_cortex-a53", "aarch64"),
    MIPSEL("mipsel_24kc", "mipsel"),
    MIPS("mips_24kc", "mips"),
    UNKNOWN("all", "all");
    val displayName: String
        get() = when (this) {
            X86_64 -> "x86_64"
            ARMV7 -> "armv7"
            AARCH64 -> "aarch64"
            MIPSEL -> "mipsel"
            MIPS -> "mips"
            UNKNOWN -> Loc.s(R.string.enum_075)
        }
}

/**
 * 软件源预设
 */
data class RepoPreset(
    val id: String,
    val name: String,
    val description: String,
    val baseUrl: String,
    val type: RepoPresetType,
    val supportedArchitectures: List<ArchitectureType> = listOf()
)

/**
 * 软件源预设类型
 */
enum class RepoPresetType {
    OFFICIAL,
    TSINGHUA,
    USTC,
    ALIYUN,
    KENZOK8,
    LIENOL,
    IMMORTALWRT,
    CUSTOM;
    val displayName: String
        get() = when (this) {
            OFFICIAL -> Loc.s(R.string.enum_076)
            TSINGHUA -> Loc.s(R.string.enum_077)
            USTC -> Loc.s(R.string.enum_078)
            ALIYUN -> Loc.s(R.string.enum_079)
            KENZOK8 -> Loc.s(R.string.enum_080)
            LIENOL -> Loc.s(R.string.enum_081)
            IMMORTALWRT -> Loc.s(R.string.enum_082)
            CUSTOM -> Loc.s(R.string.enum_083)
        }
}


/**
 * 固件信息
 */
data class FirmwareInfo(
    val currentVersion: String = "",
    val currentBuildTime: String = "",
    val deviceModel: String = "",
    val architecture: String = "",
    val kernelVersion: String = "",
    val boardName: String = ""
)

/**
 * 固件版本信息
 */
data class FirmwareRelease(
    val version: String = "",
    val releaseDate: String = "",
    val size: Long = 0,
    val md5: String = "",
    val sha256: String = "",
    val downloadUrl: String = "",
    val changelog: String = "",
    val isNewer: Boolean = false
)

/**
 * 固件升级状态
 */
enum class FirmwareUpgradeState {
    IDLE,
    CHECKING,
    DOWNLOADING,
    VERIFYING,
    FLASHING,
    REBOOTING,
    SUCCESS,
    FAILED
}

/**
 * 固件升级配置
 */
data class FirmwareUpgradeConfig(
    val repoUrl: String = "",
    val customServerUrl: String = "",
    val autoCheck: Boolean = true,
    val keepConfig: Boolean = true,
    val autoReboot: Boolean = true
)


/**
 * 网口状态
 */
data class PortStatus(
    val name: String = "",
    val displayName: String = "",
    val type: PortType = PortType.LAN,
    val isConnected: Boolean = false,
    val speed: Int = 0, // 速率 Mbps
    val duplex: String = "", // 全双工/半双工
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxPackets: Long = 0,
    val txPackets: Long = 0,
    val macAddress: String = ""
)

/**
 * 网口类型
 */
enum class PortType {
    WAN,
    LAN,
    WIFI,
    SFP,
    USB,
    UNKNOWN;
    val displayName: String
        get() = when (this) {
            WAN -> "WAN"
            LAN -> "LAN"
            WIFI -> "WiFi"
            SFP -> "SFP"
            USB -> "USB"
            UNKNOWN -> Loc.s(R.string.enum_075)
        }
}

/**
 * 设备能力检测
 */
data class DeviceCapabilities(
    val hasWifi: Boolean = false,
    val hasUsb: Boolean = false,
    val hasSfp: Boolean = false,
    val wifiInterfaceCount: Int = 0,
    val lanPortCount: Int = 0,
    val wanPortCount: Int = 0,
    val totalPortCount: Int = 0,
    val packageManager: PackageManagerType = PackageManagerType.UNKNOWN,
    val architecture: String = "",
    // 功能可用性检测
    val hasTc: Boolean = false,           // 流量限速(tc命令)
    val hasIwinfo: Boolean = false,       // WiFi信息(iwinfo)
    val hasDdnsScripts: Boolean = false,  // DDNS(ddns-scripts)
    val hasLogd: Boolean = false,         // 系统日志(logread)
    val hasDnsmasq: Boolean = false       // DHCP/DNS(dnsmasq)
)
