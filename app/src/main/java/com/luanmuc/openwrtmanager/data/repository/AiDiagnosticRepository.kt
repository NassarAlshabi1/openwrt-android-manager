package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.data.api.GeminiClient
import com.luanmuc.openwrtmanager.data.model.GeminiContent
import com.luanmuc.openwrtmanager.data.model.GeminiGenerationConfig
import com.luanmuc.openwrtmanager.data.model.GeminiPart
import com.luanmuc.openwrtmanager.data.model.GeminiRequest
import com.luanmuc.openwrtmanager.util.Loc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * AI (Gemini) diagnostic repository.
 *
 * Two capabilities:
 * 1. [runAiDiagnosis] — collects a snapshot of the router (system, WAN,
 *    storage, wifi, DHCP clients, load, log tail, top processes) and asks
 *    Gemini to analyze it, in the app language (Arabic / English).
 * 2. [ask] — free-form follow-up chat; the snapshot is included as context
 *    so the user can ask questions like "why is wifi slow?".
 *
 * Data-collection failures never abort the diagnosis: each section carries
 * its own "unavailable" note so the model knows what is missing.
 */
class AiDiagnosticRepository private constructor(private val context: Context) {

    private val luciRepository = LuciRepository.getInstance()
    private val settingsRepository = SettingsRepository.getInstance(context)

    // Cap the snapshot sent to the model (tokens & privacy)
    private object Limits {
        const val LOG_LINES = 30
        const val PROCESSES = 12
        const val DHCP_CLIENTS = 20
        const val SNAPSHOT_CHARS = 9000
    }

    // ==================== Public API ====================

    /**
     * Collect a router snapshot and run a full AI diagnosis.
     * Returns the model answer text.
     */
    suspend fun runAiDiagnosis(): String = withContext(Dispatchers.IO) {
        val apiKey = requireKey()
        val model = settingsRepository.geminiModel
        val snapshot = collectRouterSnapshot()
        askDiagnosis(apiKey, model, snapshot)
    }

    /**
     * Full AI diagnosis using an already-collected snapshot
     * (so the UI can show collection progress separately).
     */
    suspend fun runAiDiagnosisWithSnapshot(userVisibleRequest: String, snapshot: String): String =
        withContext(Dispatchers.IO) {
            val apiKey = requireKey()
            val model = settingsRepository.geminiModel
            askDiagnosis(apiKey, model, snapshot)
        }

    private suspend fun askDiagnosis(apiKey: String, model: String, snapshot: String): String {
        val system = systemInstruction()
        val userPrompt = buildString {
            append(if (Loc.isRtl()) PROMPT_DIAGNOSE_AR else PROMPT_DIAGNOSE_EN)
            append("\n\n")
            append(snapshot)
        }

        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(userPrompt)))),
            systemInstruction = GeminiContent(role = "system", parts = listOf(GeminiPart(system))),
            generationConfig = GeminiGenerationConfig(temperature = 0.3)
        )
        return GeminiClient.ask(model, apiKey, request)
    }

    /**
     * Follow-up question with the router snapshot (and chat history) as context.
     */
    suspend fun ask(question: String, history: List<Pair<String, String>> = emptyList()): String =
        withContext(Dispatchers.IO) {
            askInternal(question, history, null)
        }

    /**
     * Follow-up question; optionally collects a fresh router snapshot first
     * and prepends it to the question.
     */
    suspend fun askWithOptionalSnapshot(
        question: String,
        collectIfRequested: Boolean,
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val snapshot = if (collectIfRequested) {
            runCatching { collectRouterSnapshot() }.getOrNull()
        } else null
        askInternal(question, history, snapshot)
    }

    private suspend fun askInternal(
        question: String,
        history: List<Pair<String, String>>,
        snapshot: String?
    ): String {
        val apiKey = requireKey()
        val model = settingsRepository.geminiModel

        val contents = mutableListOf<GeminiContent>()
        // Prior conversation (user/model)
        history.forEach { (role, text) ->
            if (text.isNotBlank()) {
                contents.add(GeminiContent(role = role, parts = listOf(GeminiPart(text))))
            }
        }

        val questionWithSnapshot = if (snapshot != null) {
            buildString {
                append(question)
                append("\n\n")
                append(if (Loc.isRtl()) PROMPT_ATTACH_AR else PROMPT_ATTACH_EN)
                append("\n\n")
                append(snapshot)
            }
        } else question

        contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(questionWithSnapshot))))

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(role = "system", parts = listOf(GeminiPart(systemInstruction()))),
            generationConfig = GeminiGenerationConfig(temperature = 0.4)
        )
        return GeminiClient.ask(model, apiKey, request)
    }

    /**
     * Collect a fresh snapshot (used when the user taps "attach router data"
     * before asking a question).
     */
    suspend fun freshSnapshotText(): String = withContext(Dispatchers.IO) {
        collectRouterSnapshot()
    }

    fun isConfigured(): Boolean = settingsRepository.isGeminiConfigured

    fun saveApiKey(key: String) {
        settingsRepository.geminiApiKey = key
    }

    fun savedApiKey(): String = settingsRepository.geminiApiKey

    fun saveModel(model: String) {
        settingsRepository.geminiModel = model
    }

    fun savedModel(): String = settingsRepository.geminiModel

    // ==================== Internals ====================

    private fun requireKey(): String {
        val key = settingsRepository.geminiApiKey
        if (key.isBlank()) throw GeminiClient.GeminiException(Loc.s(R.string.ai_err_no_key))
        return key
    }

    private fun systemInstruction(): String {
        val lang = if (Loc.isRtl()) "Arabic" else "English"
        return buildString {
            append(
                "You are an expert OpenWrt router support engineer inside an Android app. " +
                    "You diagnose router problems and give practical, safe advice. "
            )
            append("ALWAYS answer in $lang. ")
            append(
                "Structure the answer with short markdown sections: a one-line overall verdict, " +
                    "then findings, then concrete step-by-step fixes (commands where useful, " +
                    "LuCI web steps where useful). Never invent data that is not in the snapshot; " +
                    "if something is missing say so briefly. Keep it under 400 words."
            )
        }
    }

    /**
     * Gather router state from all available sources concurrently.
     * Every section degrades gracefully to "unavailable".
     */
    private suspend fun collectRouterSnapshot(): String = coroutineScope {
        val systemInfo = async { runCatching { luciRepository.getSystemInfo() }.getOrNull() }
        val boardInfo = async { runCatching { luciRepository.getBoardInfo() }.getOrNull() }
        val wanStatus = async { runCatching { luciRepository.getWanStatus() }.getOrNull() }
        val dfOutput = async { runCatching { luciRepository.executeCommand("df -h") }.getOrNull() }
        val memInfo = async { runCatching { luciRepository.executeCommand("free") }.getOrNull() }
        val wifiStatus = async { runCatching { luciRepository.executeCommand("iwinfo") }.getOrNull() }
        val dhcpLeases = async { runCatching { luciRepository.getDhcpLeases() }.getOrNull() }
        val sysLog = async { runCatching { luciRepository.getSystemLog() }.getOrNull() }
        val processes = async { runCatching { luciRepository.getProcessList() }.getOrNull() }
        val pingTest = async { runCatching { luciRepository.executeCommand("ping -c 4 -W 2 8.8.8.8") }.getOrNull() }
        val dnsTest = async { runCatching { luciRepository.executeCommand("nslookup openwrt.org") }.getOrNull() }

        val sb = StringBuilder()
        sb.append("=== ROUTER SNAPSHOT ===\n")

        // --- System ---
        sb.append("\n[System]\n")
        val sys = systemInfo.await()
        if (sys != null && sys.isNotEmpty()) {
            val uptime = (sys["uptime"] as? Number)?.toLong() ?: 0L
            sb.append("uptime_seconds: $uptime (${uptime / 86400}d ${(uptime % 86400) / 3600}h)\n")
            sys.forEach { (k, v) -> if (k != "uptime") sb.append("$k: $v\n") }
        } else {
            sb.append("unavailable\n")
        }

        // --- Board / firmware ---
        sb.append("\n[Board / Firmware]\n")
        val board = boardInfo.await()
        if (board != null && board.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val release = board["release"] as? Map<String, Any>
            val model = board["model"]?.toString().orEmpty()
            val target = board["target"]?.toString().orEmpty()
            sb.append("model: $model\n")
            sb.append("target: $target\n")
            release?.forEach { (k, v) -> sb.append("release.$k: $v\n") }
        } else {
            sb.append("unavailable\n")
        }

        // --- WAN ---
        sb.append("\n[WAN]\n")
        val wan = wanStatus.await()
        if (wan != null) {
            sb.append("up: ${wan.isUp}\n")
            sb.append("ip: ${wan.ipaddr}\n")
            sb.append("gateway: ${wan.gateway}\n")
            sb.append("dns: ${wan.dns?.joinToString(", ")}\n")
        } else {
            sb.append("unavailable\n")
        }

        // --- Storage ---
        sb.append("\n[Storage (df -h)]\n")
        sb.append((dfOutput.await()?.trim()?.take(1500)) ?: "unavailable\n")

        // --- Memory ---
        sb.append("\n\n[Memory (free)]\n")
        sb.append((memInfo.await()?.trim()?.take(800)) ?: "unavailable\n")

        // --- WiFi ---
        sb.append("\n\n[WiFi (iwinfo)]\n")
        sb.append((wifiStatus.await()?.trim()?.take(1500)) ?: "unavailable\n")

        // --- DHCP clients ---
        sb.append("\n\n[DHCP clients]\n")
        val leases = dhcpLeases.await()
        if (!leases.isNullOrEmpty()) {
            leases.take(Limits.DHCP_CLIENTS).forEach {
                sb.append("- ${it.hostname ?: "?"} ip=${it.ip} mac=${it.mac}\n")
            }
            if (leases.size > Limits.DHCP_CLIENTS) sb.append("... and ${leases.size - Limits.DHCP_CLIENTS} more\n")
        } else {
            sb.append("unavailable\n")
        }

        // --- Load & top processes ---
        sb.append("\n[Top processes (by memory)]\n")
        val procs = processes.await()
        if (!procs.isNullOrEmpty()) {
            procs.sortedByDescending { it.memory }
                .take(Limits.PROCESSES)
                .forEach { sb.append("- pid=${it.pid} cpu=${it.cpu} mem=${it.memory} ${it.name}\n") }
        } else {
            sb.append("unavailable\n")
        }

        // --- Connectivity tests ---
        sb.append("\n[Ping 8.8.8.8]\n")
        sb.append((pingTest.await()?.trim()?.take(600)) ?: "unavailable\n")
        sb.append("\n\n[DNS lookup openwrt.org]\n")
        sb.append((dnsTest.await()?.trim()?.take(400)) ?: "unavailable\n")

        // --- Log tail ---
        sb.append("\n\n[System log (last ${Limits.LOG_LINES} lines)]\n")
        val log = sysLog.await()
        if (!log.isNullOrEmpty()) {
            log.takeLast(Limits.LOG_LINES).forEach { entry ->
                sb.append("[${entry.level}] ${entry.facility}: ${entry.message}\n")
            }
        } else {
            sb.append("unavailable\n")
        }

        sb.append("\n=== END SNAPSHOT ===\n")

        val snapshot = sb.toString()
        if (snapshot.length > Limits.SNAPSHOT_CHARS) snapshot.take(Limits.SNAPSHOT_CHARS) else snapshot
    }

    companion object {
        // Prompts kept in both languages so the diagnosis direction matches the UI language
        private const val PROMPT_DIAGNOSE_EN =
            "Please analyze this OpenWrt router snapshot and provide a diagnosis. " +
                "Identify any problems (connectivity, performance, security, storage, firmware), " +
                "rate their severity, and give clear fix steps."

        private const val PROMPT_DIAGNOSE_AR =
            "حلّل لقطة حالة راوتر OpenWrt هذه وقدّم تشخيصاً. " +
                "حدّد أي مشاكل (الاتصال، الأداء، الأمان، التخزين، البرنامج الثابت)، " +
                "قِدر خطورتها، واعطِ خطوات إصلاح واضحة."

        private const val PROMPT_ATTACH_EN =
            "(Attached: current router state snapshot for context. " +
                "Use it silently; answer the user's question.)"

        private const val PROMPT_ATTACH_AR =
            "(مرفق: لقطة حالة الراوتر الحالية للسياق. " +
                "استخدمها بصمت؛ وأجب عن سؤال المستخدم.)"

        @Volatile
        private var instance: AiDiagnosticRepository? = null

        fun getInstance(context: Context): AiDiagnosticRepository {
            return instance ?: synchronized(this) {
                instance ?: AiDiagnosticRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
