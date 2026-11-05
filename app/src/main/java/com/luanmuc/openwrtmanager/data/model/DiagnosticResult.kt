package com.luanmuc.openwrtmanager.data.model

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 全面体检结果
 */
data class FullDiagnosticResult(
    val overallScore: Int = 100,
    val overallStatus: DiagnosticStatus = DiagnosticStatus.GOOD,
    val items: List<DiagnosticItem> = emptyList(),
    val suggestions: List<DiagnosticSuggestion> = emptyList(),
    val completedAt: Long = 0
)

/**
 * 诊断项
 */
data class DiagnosticItem(
    val id: String = "",
    val name: String = "",
    val category: DiagnosticCategory = DiagnosticCategory.SYSTEM,
    val status: DiagnosticStatus = DiagnosticStatus.GOOD,
    val message: String = "",
    val detail: String = "",
    val severity: DiagnosticSeverity = DiagnosticSeverity.LOW
)

/**
 * 诊断建议
 */
data class DiagnosticSuggestion(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: DiagnosticCategory = DiagnosticCategory.SYSTEM,
    val priority: Int = 0,
    val canAutoFix: Boolean = false,
    val fixSteps: List<String> = emptyList()
)

/**
 * 诊断状态
 */
enum class DiagnosticStatus(val color: String) {
    GOOD("#00B578"),
    WARNING("#FF7D00"),
    ERROR("#F53F3F"),
    CHECKING("#1677FF");
    val displayName: String
        get() = when (this) {
            GOOD -> Loc.s(R.string.enum_031)
            WARNING -> Loc.s(R.string.enum_032)
            ERROR -> Loc.s(R.string.enum_033)
            CHECKING -> Loc.s(R.string.enum_034)
        }
}

/**
 * 诊断分类
 */
enum class DiagnosticCategory {
    SYSTEM,
    NETWORK,
    WIFI,
    SECURITY,
    PERFORMANCE,
    STORAGE;
    val displayName: String
        get() = when (this) {
            SYSTEM -> Loc.s(R.string.enum_021)
            NETWORK -> Loc.s(R.string.enum_022)
            WIFI -> "WiFi"
            SECURITY -> Loc.s(R.string.enum_035)
            PERFORMANCE -> Loc.s(R.string.enum_036)
            STORAGE -> Loc.s(R.string.enum_037)
        }
}

/**
 * 诊断严重程度
 */
enum class DiagnosticSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;
    val displayName: String
        get() = when (this) {
            LOW -> Loc.s(R.string.enum_038)
            MEDIUM -> Loc.s(R.string.enum_039)
            HIGH -> Loc.s(R.string.enum_040)
            CRITICAL -> Loc.s(R.string.enum_041)
        }
}

/**
 * 网络质量检测结果
 */
data class NetworkQualityResult(
    val latency: Int = 0,  // 延迟，ms
    val jitter: Int = 0,  // 抖动，ms
    val packetLoss: Float = 0f,  // 丢包率，%
    val downloadSpeed: Long = 0,  // 下载速度，KB/s
    val uploadSpeed: Long = 0,  // 上传速度，KB/s
    val overallScore: Int = 100,
    val quality: NetworkQuality = NetworkQuality.EXCELLENT
)

/**
 * 网络质量等级
 */
enum class NetworkQuality(val minScore: Int) {
    EXCELLENT(90),
    GOOD(70),
    FAIR(50),
    POOR(30),
    BAD(0);
    val displayName: String
        get() = when (this) {
            EXCELLENT -> Loc.s(R.string.enum_042)
            GOOD -> Loc.s(R.string.enum_031)
            FAIR -> Loc.s(R.string.enum_043)
            POOR -> Loc.s(R.string.enum_044)
            BAD -> Loc.s(R.string.enum_045)
        }
}
