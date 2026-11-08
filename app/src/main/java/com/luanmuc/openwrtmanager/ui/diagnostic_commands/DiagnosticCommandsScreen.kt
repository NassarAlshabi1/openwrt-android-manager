package com.luanmuc.openwrtmanager.ui.diagnostic_commands

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc

/**
 * Diagnostic-commands screen.
 *
 * Shows the full catalog of read-only diagnostic commands grouped by
 * category. The user selects which commands to run — individually, per
 * category or via presets — and every command card explains what it
 * collects and when it is useful. Selected commands run on the router and
 * their raw output is shown per section; the report can be copied, shared,
 * or analyzed with Gemini AI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticCommandsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticCommandsViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val selectedIds by viewModel.selectedIds.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val aiAnswer by viewModel.aiAnswer.collectAsState()
    val isAiAnalyzing by viewModel.isAiAnalyzing.collectAsState()
    val error by viewModel.error.collectAsState()

    // Show errors as toasts
    LaunchedEffect(error) {
        error?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    fun copyReport() {
        val report = viewModel.buildReport()
        if (report.isBlank()) return
        clipboard.setText(androidx.compose.ui.text.AnnotatedString(report))
        android.widget.Toast.makeText(
            context, Loc.s(R.string.diagcmd_copied), android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun shareReport() {
        val report = viewModel.buildReport()
        if (report.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(
            Intent.createChooser(intent, Loc.s(R.string.diagcmd_share))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagcmd_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { copyReport() },
                        enabled = viewModel.hasAnyResult
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = Loc.s(R.string.diagcmd_copy)
                        )
                    }
                    IconButton(
                        onClick = { shareReport() },
                        enabled = viewModel.hasAnyResult
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = Loc.s(R.string.diagcmd_share)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MiTheme.CardBackground,
                    titleContentColor = MiTheme.TextPrimary
                )
            )
        },
        containerColor = MiTheme.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isRunning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MiTheme.Primary
                )
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = MiDimens.horizontalPadding,
                    vertical = MiDimens.itemSpacing
                ),
                verticalArrangement = Arrangement.spacedBy(MiDimens.itemSpacing)
            ) {
                // ==================== Intro: presets + run ====================
                item {
                    IntroCard(
                        selectedCount = selectedIds.size,
                        isRunning = isRunning,
                        onRunSelected = { viewModel.runSelected() },
                        onSelectAll = { viewModel.selectAll() },
                        onClearAll = { viewModel.clearSelection() },
                        onApplyPreset = { viewModel.applyPreset(it) }
                    )
                }

                // ==================== Command catalog by category ====================
                DiagCategory.entries.forEach { category ->
                    val catCommands = DiagCommandCatalog.byCategory[category].orEmpty()

                    item(key = "cat_${category.name}") {
                        CategoryCard(
                            category = category,
                            commands = catCommands,
                            selectedIds = selectedIds,
                            enabled = !isRunning,
                            onToggleCategory = { selected ->
                                viewModel.setCategorySelected(category, selected)
                            },
                            onToggleCommand = { id -> viewModel.toggleCommand(id) }
                        )
                    }
                }

                // ==================== Results ====================
                if (sections.isNotEmpty()) {
                    item(key = "results_header") {
                        ResultsHeader()
                    }
                    items(
                        count = sections.size,
                        key = { sections[it].id }
                    ) { index ->
                        SectionCard(
                            section = sections[index],
                            enabled = !isRunning,
                            onRun = { viewModel.runSection(sections[index].id) }
                        )
                    }
                }

                // ==================== AI analysis ====================
                item(key = "ai_card") {
                    AiAnalysisCard(
                        hasResult = viewModel.hasAnyResult,
                        isAnalyzing = isAiAnalyzing,
                        answer = aiAnswer,
                        onAnalyze = { viewModel.analyzeWithAi() },
                        onDismiss = { viewModel.clearAiAnswer() }
                    )
                }
            }
        }
    }
}

// ==================== Components ====================

/**
 * Header card: description, selection presets, select all / clear and the
 * primary "run selected" button.
 */
@Composable
private fun IntroCard(
    selectedCount: Int,
    isRunning: Boolean,
    onRunSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onApplyPreset: (DiagPreset) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Loc.s(R.string.diagcmd_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Loc.s(R.string.diagcmd_subtitle),
                fontSize = 13.sp,
                color = MiTheme.TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Presets
            Text(
                text = Loc.s(R.string.diagcmd_presets_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MiTheme.TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagPreset.entries.forEach { preset ->
                    AssistChip(
                        onClick = { onApplyPreset(preset) },
                        label = {
                            Text(
                                text = Loc.s(preset.labelRes),
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MiTheme.Primary.copy(alpha = 0.4f)
                        ),
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MiTheme.Primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSelectAll,
                    enabled = !isRunning,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Default.CheckBox,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Loc.s(R.string.diagcmd_select_all), fontSize = 12.sp)
                }
                TextButton(
                    onClick = onClearAll,
                    enabled = !isRunning && selectedCount > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Loc.s(R.string.diagcmd_clear_all), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onRunSelected,
                enabled = !isRunning && selectedCount > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MiTheme.Primary)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) Loc.s(R.string.diagcmd_running_all)
                    else Loc.s(R.string.diagcmd_run_selected, selectedCount),
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * One category: header with select-all-in-category toggle and the list of
 * selectable command cards.
 */
@Composable
private fun CategoryCard(
    category: DiagCategory,
    commands: List<DiagCommandDef>,
    selectedIds: Set<String>,
    enabled: Boolean,
    onToggleCategory: (Boolean) -> Unit,
    onToggleCommand: (String) -> Unit
) {
    val state = when {
        commands.isEmpty() -> false
        commands.all { it.id in selectedIds } -> true
        commands.none { it.id in selectedIds } -> false
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, role = Role.Checkbox) { onToggleCategory(state != true) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TriStateIcon(
                    state = state,
                    enabled = enabled,
                    onClick = { onToggleCategory(state != true) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Loc.s(category.titleRes),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${commands.count { it.id in selectedIds }}/${commands.size}",
                    fontSize = 12.sp,
                    color = MiTheme.TextSecondary
                )
            }

            // Commands
            commands.forEach { def ->
                CommandRow(
                    def = def,
                    selected = def.id in selectedIds,
                    enabled = enabled,
                    onToggle = { onToggleCommand(def.id) }
                )
            }
        }
    }
}

/**
 * Checkbox-style icon supporting the partial (indeterminate) state.
 */
@Composable
private fun TriStateIcon(
    state: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val (icon, tint) = when (state) {
        true -> Icons.Default.CheckBox to MiTheme.Primary
        false -> Icons.Default.CheckBoxOutlineBlank to MiTheme.TextSecondary
        null -> Icons.Default.IndeterminateCheckBox to MiTheme.Primary.copy(alpha = 0.6f)
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (enabled) tint else MiTheme.TextTertiary,
        modifier = Modifier
            .size(22.dp)
            .clickable(enabled = enabled, role = Role.Checkbox) { onClick() }
    )
}

/**
 * One selectable command: checkbox + title + the command itself (always
 * left-to-right monospace) + the explanation of what it collects and when
 * to select it.
 */
@Composable
private fun CommandRow(
    def: DiagCommandDef,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Checkbox) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Loc.s(def.titleRes),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) MiTheme.TextPrimary else MiTheme.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = def.command,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    textDirection = TextDirection.Ltr,
                    fontSize = 11.sp
                ),
                color = MiTheme.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MiTheme.Background,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Loc.s(def.descRes),
                fontSize = 12.sp,
                color = MiTheme.TextTertiary,
                lineHeight = 16.sp
            )
        }
    }
}

/** Divider-style header above the executed results. */
@Composable
private fun ResultsHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MiTheme.Primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = Loc.s(R.string.diagcmd_results_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiTheme.TextPrimary
        )
    }
}

/**
 * One executed diagnostic command and its raw output.
 */
@Composable
private fun SectionCard(
    section: DiagSection,
    enabled: Boolean,
    onRun: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title + status + re-run
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = section.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = section.status)
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRun,
                    enabled = enabled,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = Loc.s(R.string.diagcmd_rerun),
                        tint = MiTheme.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // The command, always left-to-right monospace
            Text(
                text = "$ ${section.command}",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    textDirection = TextDirection.Ltr,
                    fontSize = 12.sp
                ),
                color = MiTheme.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MiTheme.Background,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Output
            when (section.status) {
                DiagSectionStatus.IDLE -> Text(
                    text = Loc.s(R.string.diagcmd_status_idle),
                    fontSize = 12.sp,
                    color = MiTheme.TextSecondary,
                    fontStyle = FontStyle.Italic
                )
                DiagSectionStatus.RUNNING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MiTheme.Primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Loc.s(R.string.diagcmd_status_running),
                        fontSize = 12.sp,
                        color = MiTheme.TextSecondary
                    )
                }
                else -> SelectionContainer {
                    Text(
                        text = section.output.ifBlank { "(${Loc.s(R.string.diagcmd_no_output)})" },
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            textDirection = TextDirection.Ltr,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = if (section.status == DiagSectionStatus.FAILED) {
                            MiTheme.Error
                        } else {
                            MiTheme.TextPrimary
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

/**
 * Small colored status badge.
 */
@Composable
private fun StatusBadge(status: DiagSectionStatus) {
    val (textRes, color) = when (status) {
        DiagSectionStatus.IDLE -> R.string.diagcmd_status_idle to MiTheme.TextSecondary
        DiagSectionStatus.RUNNING -> R.string.diagcmd_status_running to MiTheme.Primary
        DiagSectionStatus.DONE -> R.string.diagcmd_status_done to MiTheme.Success
        DiagSectionStatus.FAILED -> R.string.diagcmd_status_failed to MiTheme.Error
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = Loc.s(textRes),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Gemini AI analysis card: send the collected report for analysis.
 */
@Composable
private fun AiAnalysisCard(
    hasResult: Boolean,
    isAnalyzing: Boolean,
    answer: String?,
    onAnalyze: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Loc.s(R.string.diagcmd_ai_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Loc.s(R.string.diagcmd_ai_desc),
                fontSize = 13.sp,
                color = MiTheme.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isAnalyzing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MiTheme.Primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Loc.s(R.string.diagcmd_ai_running),
                        fontSize = 13.sp,
                        color = MiTheme.TextSecondary
                    )
                }
            } else if (answer != null) {
                SelectionContainer {
                    Text(
                        text = answer,
                        fontSize = 13.sp,
                        color = MiTheme.TextPrimary,
                        lineHeight = 19.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    FilledTonalButton(
                        onClick = onAnalyze,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Loc.s(R.string.diagcmd_ai_again), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(Loc.s(R.string.offline_close), fontSize = 12.sp)
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onAnalyze,
                    enabled = hasResult,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Loc.s(R.string.diagcmd_ai_button), fontSize = 12.sp)
                }
            }
        }
    }
}
