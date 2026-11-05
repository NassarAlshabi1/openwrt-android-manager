package com.luanmuc.openwrtmanager.data.model

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 插件依赖信息
 */
data class PluginDependency(
    val featureName: String,       // 功能名称
    val packageName: String,       // 插件包名
    val description: String,       // 插件说明
    val isRequired: Boolean = true, // 是否必需
    val installCommand: String = "", // 安装命令（默认opkg install）
    val category: DependencyCategory = DependencyCategory.OTHER
)

/**
 * 插件分类
 */
enum class DependencyCategory {
    NETWORK,      // 网络相关
    WIFI,         // WiFi相关
    SYSTEM,       // 系统相关
    STORAGE,      // 存储相关
    DIAGNOSTIC,   // 诊断相关
    OTHER         // 其他
}

/**
 * 插件安装状态
 */
data class PluginInstallStatus(
    val dependency: PluginDependency,
    val isInstalled: Boolean,
    val isInstalling: Boolean = false,
    val installProgress: Int = 0,
    val installMessage: String = "",
    val installError: String = ""
)

/**
 * 插件依赖配置
 * 定义每个功能需要的插件
 */
object PluginDependencies {
    // DDNS功能
    val DDNS = PluginDependency(
        featureName = Loc.s(R.string.s0003),
        packageName = "ddns-scripts",
        description = Loc.s(R.string.s0004),
        category = DependencyCategory.NETWORK
    )

    // 设备限速功能
    val TRAFFIC_CONTROL = PluginDependency(
        featureName = Loc.s(R.string.s0005),
        packageName = "ip-tiny",
        description = Loc.s(R.string.s0006),
        category = DependencyCategory.NETWORK
    )

    // 挂载点管理
    val BLOCK_MOUNT = PluginDependency(
        featureName = Loc.s(R.string.s0007),
        packageName = "block-mount",
        description = Loc.s(R.string.s0008),
        category = DependencyCategory.STORAGE
    )

    // WiFi信息
    val IWINFO = PluginDependency(
        featureName = Loc.s(R.string.s0009),
        packageName = "rpcd-mod-iwinfo",
        description = Loc.s(R.string.s0010),
        category = DependencyCategory.WIFI
    )

    // 系统日志
    val LOGD = PluginDependency(
        featureName = Loc.s(R.string.s0011),
        packageName = "logd",
        description = Loc.s(R.string.s0012),
        category = DependencyCategory.SYSTEM
    )

    // 诊断工具-traceroute
    val TRACEROUTE = PluginDependency(
        featureName = Loc.s(R.string.s0013),
        packageName = "traceroute",
        description = Loc.s(R.string.s0014),
        isRequired = false,
        category = DependencyCategory.DIAGNOSTIC
    )

    // 所有必需插件列表
    val ALL_REQUIRED = listOf(
        DDNS,
        TRAFFIC_CONTROL,
        BLOCK_MOUNT,
        IWINFO,
        LOGD
    )

    // 所有插件列表（含可选）
    val ALL = listOf(
        DDNS,
        TRAFFIC_CONTROL,
        BLOCK_MOUNT,
        IWINFO,
        LOGD,
        TRACEROUTE
    )
}
