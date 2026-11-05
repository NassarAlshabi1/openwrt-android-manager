package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import com.luanmuc.openwrtmanager.data.model.DiagnosticCategory
import com.luanmuc.openwrtmanager.data.model.DiagnosticItem
import com.luanmuc.openwrtmanager.data.model.DiagnosticSeverity
import com.luanmuc.openwrtmanager.data.model.DiagnosticStatus
import com.luanmuc.openwrtmanager.data.model.DiagnosticSuggestion
import com.luanmuc.openwrtmanager.data.model.FullDiagnosticResult
import com.luanmuc.openwrtmanager.data.model.NetworkQuality
import com.luanmuc.openwrtmanager.data.model.NetworkQualityResult
import com.luanmuc.openwrtmanager.util.DebugMode
import com.luanmuc.openwrtmanager.util.OpenWrtVersions
import kotlinx.coroutines.delay
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 智能诊断Repository
 * 负责全面体检、网络质量检测、优化建议等功能
 */
class DiagnosticRepository private constructor(private val context: Context) {
    private val luciRepository = LuciRepository.getInstance()
    
    /**
     * 执行全面体检
     */
    suspend fun runFullDiagnostic(): FullDiagnosticResult {
        if (DebugMode.isDebugMode) {
            // 模拟检测过程
            delay(2000)
            return getFakeDiagnosticResult()
        }
        
        // 真实模式：执行各项检测
        val items = mutableListOf<DiagnosticItem>()
        val suggestions = mutableListOf<DiagnosticSuggestion>()

        try {
            // 系统检测：获取系统信息
            val systemInfo = luciRepository.getSystemInfo()
            val uptime = (systemInfo["uptime"] as? Number)?.toLong() ?: 0L
            items.add(DiagnosticItem(
                id = "system",
                name = Loc.s(R.string.s0040),
                status = if (uptime > 0) DiagnosticStatus.GOOD else DiagnosticStatus.WARNING,
                message = Loc.s(R.string.s0041),
                detail = Loc.s(R.string.s0042, uptime / 86400)
            ))

            // 网络检测：检查WAN连接
            val wanStatus = luciRepository.getWanStatus()
            items.add(DiagnosticItem(
                id = "network",
                name = Loc.s(R.string.s0043),
                status = if (wanStatus?.isUp == true) DiagnosticStatus.GOOD else DiagnosticStatus.ERROR,
                message = if (wanStatus?.isUp == true) Loc.s(R.string.s0044) else Loc.s(R.string.s0045),
                detail = wanStatus?.ipaddr ?: Loc.s(R.string.s0046)
            ))

            // 存储检测：执行df命令
            val dfOutput = luciRepository.executeCommand("df -h /")
            val storageOk = dfOutput?.contains("/") == true
            items.add(DiagnosticItem(
                id = "storage",
                name = Loc.s(R.string.s0047),
                status = if (storageOk) DiagnosticStatus.GOOD else DiagnosticStatus.WARNING,
                message = if (storageOk) Loc.s(R.string.s0048) else Loc.s(R.string.s0049),
                detail = dfOutput?.trim() ?: Loc.s(R.string.s0050)
            ))

            // 固件版本检测：支持 OpenWrt 23.05.0 - 23.05.6（兼容 23.5.x 写法）
            try {
                val boardInfo = luciRepository.getBoardInfo()
                @Suppress("UNCHECKED_CAST")
                val releaseInfo = boardInfo["release"] as? Map<String, Any>
                val fwVersion = releaseInfo?.get("version")?.toString() ?: ""
                if (fwVersion.isNotBlank()) {
                    val supported = OpenWrtVersions.isSupported(fwVersion)
                    val upToDate = OpenWrtVersions.compareWithLatest(fwVersion) >= 0
                    items.add(DiagnosticItem(
                        id = "firmware_version",
                        name = Loc.s(R.string.s0073),
                        status = when {
                            !supported -> DiagnosticStatus.WARNING
                            upToDate -> DiagnosticStatus.GOOD
                            else -> DiagnosticStatus.WARNING
                        },
                        message = when {
                            !supported -> Loc.s(R.string.version_unsupported_range, OpenWrtVersions.supportedRangeDescription())
                            upToDate -> Loc.s(R.string.version_up_to_date)
                            else -> Loc.s(R.string.version_update_available, OpenWrtVersions.latestSupported())
                        },
                        detail = Loc.s(R.string.version_detail_format, OpenWrtVersions.normalize(fwVersion), OpenWrtVersions.supportedRangeDescription()),
                        category = DiagnosticCategory.SYSTEM,
                        severity = if (supported) DiagnosticSeverity.LOW else DiagnosticSeverity.MEDIUM
                    ))
                }
            } catch (e: Exception) {
                // 固件版本检测失败不影响整体诊断
            }

            // 计算总分
            val goodCount = items.count { it.status == DiagnosticStatus.GOOD }
            val score = (goodCount * 100 / items.size.coerceAtLeast(1))

            return FullDiagnosticResult(
                overallScore = score,
                overallStatus = if (score >= 80) DiagnosticStatus.GOOD else if (score >= 60) DiagnosticStatus.WARNING else DiagnosticStatus.ERROR,
                items = items,
                suggestions = suggestions,
                completedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            return FullDiagnosticResult(
                overallScore = 0,
                overallStatus = DiagnosticStatus.ERROR,
                items = items,
                suggestions = suggestions,
                completedAt = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * 检测网络质量
     */
    suspend fun checkNetworkQuality(): NetworkQualityResult {
        if (DebugMode.isDebugMode) {
            delay(1500)
            return getFakeNetworkQualityResult()
        }
        
        // 真实模式：执行网络质量检测（ping测试）
        return try {
            val pingOutput = luciRepository.executeCommand("ping -c 3 -W 2 8.8.8.8")
            val latency = if (pingOutput?.contains("time=") == true) {
                val match = Regex("time=(\\d+)").find(pingOutput)
                match?.groupValues?.get(1)?.toFloatOrNull() ?: 50f
            } else 100f
            NetworkQualityResult(
                latency = latency.toInt(),
                jitter = (latency * 0.1).toInt(),
                packetLoss = if (pingOutput?.contains("0% packet loss") == true) 0f else 50f,
                downloadSpeed = 0,
                uploadSpeed = 0,
                overallScore = if (latency < 50) 90 else if (latency < 100) 70 else 50,
                quality = if (latency < 50) NetworkQuality.EXCELLENT else if (latency < 100) NetworkQuality.GOOD else NetworkQuality.POOR
            )
        } catch (e: Exception) {
            NetworkQualityResult(
                latency = 0, jitter = 0, packetLoss = 100f,
                downloadSpeed = 0, uploadSpeed = 0,
                overallScore = 0, quality = NetworkQuality.POOR
            )
        }
    }
    
    /**
     * 获取优化建议
     */
    suspend fun getOptimizationSuggestions(): List<DiagnosticSuggestion> {
        if (DebugMode.isDebugMode) {
            return getFakeSuggestions()
        }
        
        return emptyList()
    }
    
    /**
     * 自动修复问题
     */
    suspend fun autoFix(suggestionId: String): Boolean {
        if (DebugMode.isDebugMode) {
            delay(1000)
            return true
        }
        
        return false
    }
    
    /**
     * 假数据：全面体检结果
     */
    private fun getFakeDiagnosticResult(): FullDiagnosticResult {
        val items = listOf(
            DiagnosticItem(
                id = "system_uptime",
                name = Loc.s(R.string.s0051),
                category = DiagnosticCategory.SYSTEM,
                status = DiagnosticStatus.GOOD,
                message = Loc.s(R.string.s0052),
                detail = Loc.s(R.string.s0053),
                severity = DiagnosticSeverity.LOW
            ),
            DiagnosticItem(
                id = "system_load",
                name = Loc.s(R.string.s0054),
                category = DiagnosticCategory.SYSTEM,
                status = DiagnosticStatus.GOOD,
                message = Loc.s(R.string.s0055),
                detail = Loc.s(R.string.s0056),
                severity = DiagnosticSeverity.LOW
            ),
            DiagnosticItem(
                id = "memory_usage",
                name = Loc.s(R.string.s0057),
                category = DiagnosticCategory.PERFORMANCE,
                status = DiagnosticStatus.WARNING,
                message = Loc.s(R.string.s0058),
                detail = Loc.s(R.string.s0059),
                severity = DiagnosticSeverity.MEDIUM
            ),
            DiagnosticItem(
                id = "storage_usage",
                name = Loc.s(R.string.s0047),
                category = DiagnosticCategory.STORAGE,
                status = DiagnosticStatus.GOOD,
                message = Loc.s(R.string.s0060),
                detail = Loc.s(R.string.s0061),
                severity = DiagnosticSeverity.LOW
            ),
            DiagnosticItem(
                id = "wan_connection",
                name = Loc.s(R.string.s0062),
                category = DiagnosticCategory.NETWORK,
                status = DiagnosticStatus.GOOD,
                message = Loc.s(R.string.s0044),
                detail = Loc.s(R.string.s0063),
                severity = DiagnosticSeverity.LOW
            ),
            DiagnosticItem(
                id = "dns_resolution",
                name = Loc.s(R.string.s0064),
                category = DiagnosticCategory.NETWORK,
                status = DiagnosticStatus.GOOD,
                message = Loc.s(R.string.s0065),
                detail = Loc.s(R.string.s0066),
                severity = DiagnosticSeverity.LOW
            ),
            DiagnosticItem(
                id = "wifi_signal",
                name = Loc.s(R.string.s0067),
                category = DiagnosticCategory.WIFI,
                status = DiagnosticStatus.WARNING,
                message = Loc.s(R.string.s0068),
                detail = Loc.s(R.string.s0069),
                severity = DiagnosticSeverity.MEDIUM
            ),
            DiagnosticItem(
                id = "firewall_status",
                name = Loc.s(R.string.s0070),
                category = DiagnosticCategory.SECURITY,
                status = DiagnosticStatus.GOOD,
                message = Loc.s(R.string.s0071),
                detail = Loc.s(R.string.s0072),
                severity = DiagnosticSeverity.LOW
            ),
            DiagnosticItem(
                id = "firmware_version",
                name = Loc.s(R.string.s0073),
                category = DiagnosticCategory.SYSTEM,
                status = DiagnosticStatus.WARNING,
                message = Loc.s(R.string.s0074),
                detail = Loc.s(R.string.s0075),
                severity = DiagnosticSeverity.MEDIUM
            ),
            DiagnosticItem(
                id = "online_devices",
                name = Loc.s(R.string.s0076),
                category = DiagnosticCategory.NETWORK,
                status = DiagnosticStatus.GOOD,
                message = Loc.s(R.string.s0077),
                detail = Loc.s(R.string.s0078),
                severity = DiagnosticSeverity.LOW
            )
        )
        
        val suggestions = listOf(
            DiagnosticSuggestion(
                id = "update_firmware",
                title = Loc.s(R.string.s0079),
                description = Loc.s(R.string.s0080),
                category = DiagnosticCategory.SYSTEM,
                priority = 1,
                canAutoFix = false,
                fixSteps = listOf(
                    Loc.s(R.string.s0081),
                    Loc.s(R.string.s0082),
                    Loc.s(R.string.s0083),
                    Loc.s(R.string.s0084)
                )
            ),
            DiagnosticSuggestion(
                id = "optimize_memory",
                title = Loc.s(R.string.s0085),
                description = Loc.s(R.string.s0086),
                category = DiagnosticCategory.PERFORMANCE,
                priority = 2,
                canAutoFix = true,
                fixSteps = listOf(
                    Loc.s(R.string.s0087),
                    Loc.s(R.string.s0088),
                    Loc.s(R.string.s0089)
                )
            ),
            DiagnosticSuggestion(
                id = "wifi_position",
                title = Loc.s(R.string.s0090),
                description = Loc.s(R.string.s0091),
                category = DiagnosticCategory.WIFI,
                priority = 3,
                canAutoFix = false,
                fixSteps = listOf(
                    Loc.s(R.string.s0092),
                    Loc.s(R.string.s0093),
                    Loc.s(R.string.s0094),
                    Loc.s(R.string.s0095)
                )
            ),
            DiagnosticSuggestion(
                id = "change_password",
                title = Loc.s(R.string.s0096),
                description = Loc.s(R.string.s0097),
                category = DiagnosticCategory.SECURITY,
                priority = 4,
                canAutoFix = false,
                fixSteps = listOf(
                    Loc.s(R.string.s0098),
                    Loc.s(R.string.s0099),
                    Loc.s(R.string.s0100)
                )
            )
        )
        
        // 计算总分
        var totalScore = 100
        for (item in items) {
            when (item.status) {
                DiagnosticStatus.WARNING -> totalScore -= 5
                DiagnosticStatus.ERROR -> totalScore -= 15
                else -> {}
            }
        }
        totalScore = totalScore.coerceIn(0, 100)
        
        val overallStatus = when {
            totalScore >= 90 -> DiagnosticStatus.GOOD
            totalScore >= 70 -> DiagnosticStatus.WARNING
            else -> DiagnosticStatus.ERROR
        }
        
        return FullDiagnosticResult(
            overallScore = totalScore,
            overallStatus = overallStatus,
            items = items,
            suggestions = suggestions,
            completedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 假数据：网络质量检测结果
     */
    private fun getFakeNetworkQualityResult(): NetworkQualityResult {
        return NetworkQualityResult(
            latency = 18,
            jitter = 3,
            packetLoss = 0.1f,
            downloadSpeed = 95 * 1024,  // 95MB/s
            uploadSpeed = 40 * 1024,    // 40MB/s
            overallScore = 92,
            quality = NetworkQuality.EXCELLENT
        )
    }
    
    /**
     * 假数据：优化建议
     */
    private fun getFakeSuggestions(): List<DiagnosticSuggestion> {
        return getFakeDiagnosticResult().suggestions
    }
    
    companion object {
        @Volatile
        private var instance: DiagnosticRepository? = null
        
        fun getInstance(context: Context): DiagnosticRepository {
            return instance ?: synchronized(this) {
                instance ?: DiagnosticRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
