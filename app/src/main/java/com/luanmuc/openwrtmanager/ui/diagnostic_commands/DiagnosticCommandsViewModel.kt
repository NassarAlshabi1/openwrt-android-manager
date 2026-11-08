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
 * One executed diagnostic command and its raw output.
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
 * ViewModel for the diagnostic-commands screen.
 *
 * The full catalog lives in [DiagCommandCatalog] (26 read-only commands in 5
 * categories). The user selects the commands to run — individually, per
 * category or via presets — and every command card shows an explanation of
 * what it collects and when it is useful.
 *
 * Execution is sequential; mesh neighbor/path sections are auto-discovered
 * from `iw dev` so every 802.11s mesh interface on the router is covered.
 * The collected report can be copied, shared, or analyzed with Gemini AI.
 */
class DiagnosticCommandsViewModel(application: Application) : BaseViewModel(application) {

    private val luciRepository = LuciRepository.getInstance()
    private val aiRepository = AiDiagnosticRepository.getInstance(application)

    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    // ==================== State ====================

    /** Currently selected command ids (persisted between app starts). */
    private val _selectedIds = MutableStateFlow(loadSelection())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    /** Executed sections with their output (empty until the user runs). */
    private val _sections = MutableStateFlow<List<DiagSection>>(emptyList())
    val sections: StateFlow<List<DiagSection>> = _sections.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

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

    // ==================== Selection ====================

    fun toggleCommand(id: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
        persistSelection()
    }

    /** Select or deselect every command of one category. */
    fun setCategorySelected(category: DiagCategory, selected: Boolean) {
        val ids = DiagCommandCatalog.byCategory[category].orEmpty().map { it.id }
        val current = _selectedIds.value
        _selectedIds.value = if (selected) current + ids else current - ids
        persistSelection()
    }

    /** True when the whole category is selected, null when partial. */
    fun categorySelectedState(category: DiagCategory): Boolean? {
        val ids = DiagCommandCatalog.byCategory[category].orEmpty().map { it.id }
        if (ids.isEmpty()) return false
        val count = ids.count { it in _selectedIds.value }
        return when (count) {
            0 -> false
            ids.size -> true
            else -> null
        }
    }

    fun selectAll() {
        _selectedIds.value = DiagCommandCatalog.commands.map { it.id }.toSet()
        persistSelection()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        persistSelection()
    }

    fun applyPreset(preset: DiagPreset) {
        _selectedIds.value = DiagCommandCatalog.presetSelection(preset)
        persistSelection()
    }

    fun isSelected(id: String): Boolean = id in _selectedIds.value

    private fun loadSelection(): Set<String> {
        val saved = prefs.getStringSet(KEY_SELECTION, null)
        // Ignore stale ids from older versions of the catalog
        return saved?.filter { DiagCommandCatalog.byId(it) != null }?.toSet()
            ?: DiagCommandCatalog.presetSelection(DiagPreset.STANDARD)
    }

    private fun persistSelection() {
        prefs.edit().putStringSet(KEY_SELECTION, _selectedIds.value).apply()
    }

    // ==================== Execution ====================

    /**
     * Run every selected command sequentially, in catalog order.
     * When the run reaches the mesh entry, mesh interfaces are discovered
     * from `iw dev` and one neighbors + paths pair is executed per
     * discovered interface.
     */
    fun runSelected() {
        if (_isRunning.value) return
        val selected = _selectedIds.value
        if (selected.isEmpty()) {
            _error.value = Loc.s(R.string.diagcmd_err_no_selection)
            return
        }
        viewModelScope.launch {
            _isRunning.value = true
            try {
                // (Re)build the section list from the current selection.
                // Dynamic mesh entry becomes two placeholders that are
                // replaced by real per-interface sections during the run.
                _sections.value = buildSections(selected)

                var meshExpanded = false
                var i = 0
                while (i < _sections.value.size) {
                    val section = _sections.value[i]
                    if (!meshExpanded && section.id.startsWith("mesh_")) {
                        expandMeshSections()
                        meshExpanded = true
                        continue // re-read the section now occupying index i
                    }
                    runSectionInternal(section.id)
                    i++
                }
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.diagcmd_err_run, e.message ?: "")
            } finally {
                _isRunning.value = false
            }
        }
    }

    /** Re-run a single section that already has a result. */
    fun runSection(id: String) {
        if (_isRunning.value) return
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
     * Build one section per selected command, in catalog order. The dynamic
     * mesh entry becomes a placeholder section that is replaced by real
     * per-interface sections after `iw dev` output is available.
     */
    private fun buildSections(selected: Set<String>): List<DiagSection> =
        DiagCommandCatalog.commands
            .filter { it.id in selected }
            .flatMap { def ->
                if (def.isMeshDynamic) {
                    // Placeholder with the fallback interface; replaced by
                    // insertMeshSections() as soon as `iw dev` returns.
                    val iface = IwParser.DEFAULT_MESH_INTERFACE
                    listOf(meshNeighborsSection(iface), meshPathsSection(iface))
                } else {
                    listOf(
                        DiagSection(
                            id = def.id,
                            title = Loc.s(def.titleRes),
                            reportHeader = def.reportHeader,
                            command = def.command
                        )
                    )
                }
            }

    private fun meshNeighborsSection(iface: String) = DiagSection(
        id = "mesh_neighbors:$iface",
        title = Loc.s(R.string.diagcmd_section_mesh_neighbors, iface),
        reportHeader = "MESH NEIGHBORS ($iface)",
        command = "iw dev $iface station dump"
    )

    private fun meshPathsSection(iface: String) = DiagSection(
        id = "mesh_paths:$iface",
        title = Loc.s(R.string.diagcmd_section_mesh_paths, iface),
        reportHeader = "MESH PATHS ($iface)",
        command = "iw dev $iface mpath dump"
    )

    /**
     * Replace the two mesh placeholder sections with one neighbors + paths
     * pair per discovered mesh interface, keeping their position in the
     * list. Discovery runs `iw dev` silently (it is also shown as its own
     * section when selected; running it twice is harmless).
     */
    private suspend fun expandMeshSections() {
        val current = _sections.value
        val firstMeshIdx = current.indexOfFirst { it.id.startsWith("mesh_") }
        if (firstMeshIdx < 0) return

        val iwOut = withContext(Dispatchers.IO) {
            try {
                luciRepository.executeCommand("iw dev 2>&1")
            } catch (e: Exception) {
                null
            }
        }
        val ifaces = IwParser.meshInterfacesOrDefault(iwOut)
        val mesh = ifaces.flatMap { listOf(meshNeighborsSection(it), meshPathsSection(it)) }

        val head = current.subList(0, firstMeshIdx)
        val tail = current.subList(minOf(firstMeshIdx + 2, current.size), current.size)
        _sections.value = head + mesh + tail
    }

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

    companion object {
        private const val PREFS_NAME = "diagcmd_prefs"
        private const val KEY_SELECTION = "selected_commands"
    }
}
