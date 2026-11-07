package com.luanmuc.openwrtmanager.ui.diagnostic_commands

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.data.repository.AiDiagnosticRepository
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import com.luanmuc.openwrtmanager.util.IwParser
import com.luanmuc.openwrtmanager.util.Loc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Execution state of a single diagnostic command. */
enum class DiagSectionStatus { IDLE, RUNNING, DONE, FAILED }

/**
 * One diagnostic command shown in the Diagnostic Commands screen.
 *
 * @param id            stable identifier ("hostname", "mesh_neighbors:phy0-mesh0", …)
 * @param title         localized display title
 * @param reportHeader  English header used in the exported report (matches the
 *                      `===== SECTION =====` layout of the reference script)
 * @param command       the command exactly as shown to (and runnable by) the user
 */
data class DiagSection(
    val id: String,
    val title: String,
    val reportHeader: String,
    val command: String,
    val output: String = "",
    val status: DiagSectionStatus = DiagSectionStatus.IDLE
)

/**
 * ViewModel for the raw diagnostic-commands screen.
 *
 * Runs the reference diagnostic script on the router, section by section:
 *
 * ```
 * echo "===== HOSTNAME =====";           hostname
 * echo "===== MESH NEIGHBORS =====";     iw dev phy0-mesh0 station dump
 * echo "===== MESH PATHS =====";         iw dev phy0-mesh0 mpath dump
 * echo "===== WIRELESS INTERFACES ====="; iw dev
 * echo "===== ROUTES =====";             ip route
 * ```
 *
 * Mesh sections are auto-discovered from `iw dev`, so every 802.11s mesh
 * interface on the router is covered (falls back to `phy0-mesh0`).
 * The collected report can be copied, shared, or analyzed with Gemini AI.
 */
class DiagnosticCommandsViewModel(application: Application) : BaseViewModel(application) {

    private val luciRepository = LuciRepository.getInstance()
    private val aiRepository = AiDiagnosticRepository.getInstance(application)

    // ==================== State ====================

    private val _sections = MutableStateFlow<List<DiagSection>>(defaultSections())
    val sections: StateFlow<List<DiagSection>> = _sections.asStateFlow()

    private val _isRunningAll = MutableStateFlow(false)
    val isRunningAll: StateFlow<Boolean> = _isRunningAll.asStateFlow()

    private val _aiAnswer = MutableStateFlow<String?>(null)
    val aiAnswer: StateFlow<String?> = _aiAnswer.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        initNetworkMonitor()
    }

    val hasAnyResult: Boolean
        get() = _sections.value.any {
            it.status == DiagSectionStatus.DONE || it.status == DiagSectionStatus.FAILED
        }

    // ==================== Public actions ====================

    /**
     * Run every section sequentially:
     * hostname → iw dev (discover mesh) → mesh dumps per interface → ip route.
     */
    fun runAll() {
        if (_isRunningAll.value) return
        viewModelScope.launch {
            _isRunningAll.value = true
            try {
                // Reset previous results
                _sections.value = defaultSections()

                runSectionInternal("hostname")

                val iwOut = runSectionInternal("wireless")
                val meshIfaces = IwParser.meshInterfacesOrDefault(iwOut)
                rebuildMeshSections(meshIfaces)
                meshIfaces.forEach { iface ->
                    runSectionInternal("mesh_neighbors:$iface")
                    runSectionInternal("mesh_paths:$iface")
                }

                runSectionInternal("routes")
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.diagcmd_err_run, e.message ?: "")
            } finally {
                _isRunningAll.value = false
            }
        }
    }

    /** Re-run a single section. */
    fun runSection(id: String) {
        if (_isRunningAll.value) return
        if (_sections.value.none { it.id == id }) return
        viewModelScope.launch {
            try {
                runSectionInternal(id)
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.diagcmd_err_run, e.message ?: "")
            }
        }
    }

    /**
     * Build the full report using the reference `===== SECTION =====`
     * layout (English headers, matching the original shell script).
     */
    fun buildReport(): String = _sections.value
        .filter { it.status == DiagSectionStatus.DONE || it.status == DiagSectionStatus.FAILED }
        .joinToString("\n\n") { section ->
            val body = section.output.ifBlank { "(${Loc.s(R.string.diagcmd_no_output)})" }
            "===== ${section.reportHeader} =====\n$body"
        }

    /**
     * Send the collected report to Gemini for analysis and fix suggestions.
     */
    fun analyzeWithAi() {
        if (_isAiAnalyzing.value) return
        if (!hasAnyResult) {
            _error.value = Loc.s(R.string.diagcmd_ai_no_data)
            return
        }
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            try {
                val answer = aiRepository.runAiDiagnosisWithSnapshot(
                    userVisibleRequest = Loc.s(R.string.diagcmd_ai_request),
                    snapshot = buildReport()
                )
                _aiAnswer.value = answer
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.ai_err_request, e.message ?: "")
            } finally {
                _isAiAnalyzing.value = false
            }
        }
    }

    fun clearAiAnswer() {
        _aiAnswer.value = null
    }

    fun clearError() {
        _error.value = null
    }

    override fun refreshData() {
        // Base refresh hook: commands run only on explicit user action.
    }

    // ==================== Internals ====================

    /**
     * Executes one section and returns its output (or null on failure).
     * stderr is merged into stdout so error text (e.g. "No such device")
     * stays visible, exactly like a real terminal.
     */
    private suspend fun runSectionInternal(id: String): String? {
        val section = _sections.value.firstOrNull { it.id == id } ?: return null
        updateSection(id, section.output, DiagSectionStatus.RUNNING)

        val output = withContext(Dispatchers.IO) {
            try {
                luciRepository.executeCommand("${section.command} 2>&1")
            } catch (e: Exception) {
                null
            }
        }

        val status = if (output != null) DiagSectionStatus.DONE else DiagSectionStatus.FAILED
        val body = output?.trim().orEmpty()
        updateSection(id, body, status)
        return if (output != null) body else null
    }

    private fun updateSection(id: String, output: String, status: DiagSectionStatus) {
        _sections.value = _sections.value.map { section ->
            if (section.id == id) section.copy(output = output, status = status) else section
        }
    }

    /**
     * Rebuild the section list so there is one neighbors + paths pair per
     * discovered mesh interface, placed between HOSTNAME and WIRELESS
     * INTERFACES (the order of the reference script).
     */
    private fun rebuildMeshSections(ifaces: List<String>) {
        val current = _sections.value
        val nonMesh = current.filter { !it.id.startsWith("mesh_") }
        val hostnameIdx = nonMesh.indexOfFirst { it.id == "hostname" }

        val meshSections = ifaces.flatMap { iface ->
            listOf(
                DiagSection(
                    id = "mesh_neighbors:$iface",
                    title = Loc.s(R.string.diagcmd_section_mesh_neighbors, iface),
                    reportHeader = "MESH NEIGHBORS ($iface)",
                    command = "iw dev $iface station dump"
                ),
                DiagSection(
                    id = "mesh_paths:$iface",
                    title = Loc.s(R.string.diagcmd_section_mesh_paths, iface),
                    reportHeader = "MESH PATHS ($iface)",
                    command = "iw dev $iface mpath dump"
                )
            )
        }

        val head = if (hostnameIdx >= 0) nonMesh.subList(0, hostnameIdx + 1) else emptyList()
        val tail = if (hostnameIdx >= 0) nonMesh.subList(hostnameIdx + 1, nonMesh.size) else nonMesh
        _sections.value = (head + meshSections + tail)
    }

    private fun defaultSections(): List<DiagSection> {
        val iface = IwParser.DEFAULT_MESH_INTERFACE
        return listOf(
            DiagSection(
                id = "hostname",
                title = Loc.s(R.string.diagcmd_section_hostname),
                reportHeader = "HOSTNAME",
                command = "hostname"
            ),
            DiagSection(
                id = "mesh_neighbors:$iface",
                title = Loc.s(R.string.diagcmd_section_mesh_neighbors, iface),
                reportHeader = "MESH NEIGHBORS ($iface)",
                command = "iw dev $iface station dump"
            ),
            DiagSection(
                id = "mesh_paths:$iface",
                title = Loc.s(R.string.diagcmd_section_mesh_paths, iface),
                reportHeader = "MESH PATHS ($iface)",
                command = "iw dev $iface mpath dump"
            ),
            DiagSection(
                id = "wireless",
                title = Loc.s(R.string.diagcmd_section_wireless),
                reportHeader = "WIRELESS INTERFACES",
                command = "iw dev"
            ),
            DiagSection(
                id = "routes",
                title = Loc.s(R.string.diagcmd_section_routes),
                reportHeader = "ROUTES",
                command = "ip route"
            )
        )
    }
}
