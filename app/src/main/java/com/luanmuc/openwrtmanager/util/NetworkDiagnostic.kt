package com.luanmuc.openwrtmanager.util

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 网络诊断工具
 * 智能检测网络问题并给出修复建议
 */
object NetworkDiagnostic {

    /**
     * 诊断结果
     */
    data class DiagnosticResult(
        val isHealthy: Boolean,
        val issues: List<DiagnosticIssue>,
        val suggestions: List<String>
    )

    /**
     * 诊断问题
     */
    data class DiagnosticIssue(
        val type: IssueType,
        val severity: Severity,
        val description: String,
        val suggestion: String
    )

    /**
     * 问题类型
     */
    enum class IssueType {
        CONNECTION,
        DNS,
        LATENCY,
        PACKET_LOSS,
        WIFI_SIGNAL,
        CPU_USAGE,
        MEMORY_USAGE,
        STORAGE_USAGE,
        UPTIME
    }

    /**
     * 严重程度
     */
    enum class Severity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * 运行完整诊断
     */
    suspend fun runFullDiagnostic(
        routerStatus: com.luanmuc.openwrtmanager.data.model.RouterStatus?
    ): DiagnosticResult {
        val issues = mutableListOf<DiagnosticIssue>()
        val suggestions = mutableListOf<String>()

        if (routerStatus == null) {
            issues.add(
                DiagnosticIssue(
                    type = IssueType.CONNECTION,
                    severity = Severity.CRITICAL,
                    description = Loc.s(R.string.s0915),
                    suggestion = Loc.s(R.string.s0916)
                )
            )
            return DiagnosticResult(false, issues, suggestions)
        }

        // 检查CPU使用率
        if (routerStatus.cpuUsage > 90f) {
            issues.add(
                DiagnosticIssue(
                    type = IssueType.CPU_USAGE,
                    severity = Severity.WARNING,
                    description = Loc.s(R.string.s0917, String.format("%.1f%%", routerStatus.cpuUsage)),
                    suggestion = Loc.s(R.string.s0918)
                )
            )
            suggestions.add(Loc.s(R.string.s0919))
        }

        // 检查内存使用率
        val memoryUsage = if (routerStatus.memoryTotal > 0) {
            (routerStatus.memoryUsed.toFloat() / routerStatus.memoryTotal.toFloat()) * 100
        } else 0f

        if (memoryUsage > 90f) {
            issues.add(
                DiagnosticIssue(
                    type = IssueType.MEMORY_USAGE,
                    severity = Severity.WARNING,
                    description = Loc.s(R.string.s0920, String.format("%.1f%%", memoryUsage)),
                    suggestion = Loc.s(R.string.s0921)
                )
            )
            suggestions.add(Loc.s(R.string.s0922))
        }

        // 检查存储使用率
        val storageUsage = if (routerStatus.storageTotal > 0) {
            (routerStatus.storageUsed.toFloat() / routerStatus.storageTotal.toFloat()) * 100
        } else 0f

        if (storageUsage > 90f) {
            issues.add(
                DiagnosticIssue(
                    type = IssueType.STORAGE_USAGE,
                    severity = Severity.WARNING,
                    description = Loc.s(R.string.s0923, String.format("%.1f%%", storageUsage)),
                    suggestion = Loc.s(R.string.s0924)
                )
            )
            suggestions.add(Loc.s(R.string.s0925))
        }

        // 检查运行时间
        if (routerStatus.uptime > 30 * 24 * 60 * 60) { // 30天
            issues.add(
                DiagnosticIssue(
                    type = IssueType.UPTIME,
                    severity = Severity.INFO,
                    description = Loc.s(R.string.s0926),
                    suggestion = Loc.s(R.string.s0927)
                )
            )
            suggestions.add(Loc.s(R.string.s0928))
        }

        // 检查WAN连接
        if (!routerStatus.wanConnected) {
            issues.add(
                DiagnosticIssue(
                    type = IssueType.CONNECTION,
                    severity = Severity.ERROR,
                    description = Loc.s(R.string.s0929),
                    suggestion = Loc.s(R.string.s0930)
                )
            )
            suggestions.add(Loc.s(R.string.s0931))
        }

        val isHealthy = issues.none { it.severity == Severity.ERROR || it.severity == Severity.CRITICAL }

        if (isHealthy && issues.isEmpty()) {
            suggestions.add(Loc.s(R.string.s0932))
        }

        return DiagnosticResult(isHealthy, issues, suggestions)
    }

    /**
     * 快速诊断（只检查关键指标）
     */
    fun quickDiagnostic(
        cpuUsage: Float,
        memoryUsage: Float,
        wanConnected: Boolean
    ): Boolean {
        if (!wanConnected) return false
        if (cpuUsage > 95f) return false
        if (memoryUsage > 95f) return false
        return true
    }
}
