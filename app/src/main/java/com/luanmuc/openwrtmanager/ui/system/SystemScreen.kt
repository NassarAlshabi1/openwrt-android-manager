package com.luanmuc.openwrtmanager.ui.system
import com.luanmuc.openwrtmanager.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.data.model.LogEntry
import com.luanmuc.openwrtmanager.data.model.ProcessInfo
import com.luanmuc.openwrtmanager.data.model.FullSystemInfo
import com.luanmuc.openwrtmanager.data.model.RepoPreset
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiButton
import com.luanmuc.openwrtmanager.ui.components.MiListItem
import com.luanmuc.openwrtmanager.ui.components.MiTag
import com.luanmuc.openwrtmanager.ui.components.MiEmptyState
import com.luanmuc.openwrtmanager.ui.components.MiLoadingState
import androidx.compose.foundation.lazy.items
import com.luanmuc.openwrtmanager.ui.components.MiButton
import com.luanmuc.openwrtmanager.ui.components.MiButtonType
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.LineChart
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiEmptyState
import com.luanmuc.openwrtmanager.ui.components.MiErrorState
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiLinearProgress
import com.luanmuc.openwrtmanager.ui.components.MiLoadingState
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.OfflineBanner
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 系统管理页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemScreen(
    onBack: () -> Unit = {},
    onNavigateToFirmware: () -> Unit = {},
    viewModel: SystemViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf(Loc.s(R.string.s0130), Loc.s(R.string.s0011), Loc.s(R.string.s0269))
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0518),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244),
                            tint = MiTheme.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            when (uiState.selectedTab) {
                                0 -> viewModel.loadSystemInfo()
                                1 -> viewModel.loadLogs()
                                2 -> viewModel.loadProcesses()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = Loc.s(R.string.s0322),
                            tint = MiTheme.TextSecondary
                        )
                    }
                }
            )
        },
        containerColor = MiTheme.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
                        // 离线提示条
            OfflineBanner(isOffline = !viewModel.isNetworkAvailable)
            
            // 系统状态概览
            SystemStatusOverview(
                cpuUsage = uiState.cpuUsage,
                memoryUsage = uiState.memoryUsage,
                cpuHistory = uiState.cpuHistory,
                memoryHistory = uiState.memoryHistory
            )
            
            // Tab栏
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MiTheme.Background,
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MiTheme.Divider)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = {
                            Text(
                                title,
                                fontSize = 15.sp,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        selectedContentColor = MiColors.Primary,
                        unselectedContentColor = MiTheme.TextTertiary
                    )
                }
            }
            
            if (uiState.selectedTab == 0) {
                SystemInfoContent(
                    systemInfo = uiState.systemInfo,
                    isLoading = uiState.isLoadingSystemInfo,
                    installedPackagesCount = uiState.installedPackagesCount,
                    availablePackagesCount = uiState.availablePackagesCount,
                    reposCount = uiState.reposCount,
                    presetRepos = uiState.presetRepos,
                    isSwitchingRepo = uiState.isSwitchingRepo,
                    onRefresh = { viewModel.loadSystemInfo() },
                    onSwitchRepo = { viewModel.switchRepo(it) },
                    onAutoConfigure = { viewModel.autoConfigureOfficialRepos() },
                    onNavigateToFirmware = onNavigateToFirmware
                )
            } else if (uiState.selectedTab == 1) {
                LogsContent(
                    logs = uiState.logs,
                    isLoading = uiState.isLoadingLogs,
                    error = uiState.error,
                    onRetry = { viewModel.loadLogs() }
                )
            } else {
                ProcessesContent(
                    processes = uiState.processes,
                    isLoading = uiState.isLoadingProcesses,
                    error = uiState.error,
                    onRetry = { viewModel.loadProcesses() },
                    onKillProcess = { viewModel.killProcess(it) }
                )
            }
        }
    }
}

@Composable
fun LogsContent(
    logs: List<LogEntry>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    if (isLoading) {
        MiLoadingState()
    } else if (error != null && logs.isEmpty()) {
        MiErrorState(message = error, onRetry = onRetry)
    } else {
        MiCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            backgroundColor = MiTheme.TextPrimary
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = "${log.time} ${log.message}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MiTheme.TextSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 系统信息内容
 */
@Composable
private fun SystemInfoContent(
    systemInfo: FullSystemInfo,
    isLoading: Boolean,
    installedPackagesCount: Int,
    availablePackagesCount: Int,
    reposCount: Int,
    presetRepos: List<RepoPreset>,
    isSwitchingRepo: Boolean,
    onRefresh: () -> Unit,
    onSwitchRepo: (RepoPreset) -> Unit,
    onAutoConfigure: () -> Unit,
    onNavigateToFirmware: () -> Unit
) {
    if (isLoading) {
        MiLoadingState(text = Loc.s(R.string.s0741))
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 基本信息卡片
            item {
                MiCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Loc.s(R.string.s0742),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiTheme.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        InfoItem(label = Loc.s(R.string.s0423), value = systemInfo.model)
                        InfoItem(label = Loc.s(R.string.s0743), value = systemInfo.hostname)
                        InfoItem(label = Loc.s(R.string.s0073), value = systemInfo.firmwareVersion)
                        InfoItem(
                            label = Loc.s(R.string.version_support_status),
                            value = if (systemInfo.firmwareVersion.isEmpty()) Loc.s(R.string.s0050)
                                else if (com.luanmuc.openwrtmanager.util.OpenWrtVersions.isSupported(systemInfo.firmwareVersion))
                                    Loc.s(R.string.version_supported_range, com.luanmuc.openwrtmanager.util.OpenWrtVersions.supportedRangeDescription())
                                else Loc.s(R.string.version_unsupported_range, com.luanmuc.openwrtmanager.util.OpenWrtVersions.supportedRangeDescription())
                        )
                        InfoItem(label = Loc.s(R.string.s0744), value = systemInfo.distribution)
                        InfoItem(label = Loc.s(R.string.s0745), value = systemInfo.kernelVersion)
                        InfoItem(label = Loc.s(R.string.s0424), value = systemInfo.architecture)
                        InfoItem(label = Loc.s(R.string.s0746), value = systemInfo.target.ifEmpty { Loc.s(R.string.s0050) })
                    }
                }
            }

            // 包管理器信息卡片
            item {
                MiCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Loc.s(R.string.s0747),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTheme.TextPrimary
                            )
                            MiTag(
                                text = systemInfo.packageManager.displayName,
                                backgroundColor = MiTheme.Primary.copy(alpha = 0.1f),
                                textColor = MiTheme.Primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        InfoItem(label = Loc.s(R.string.s0748), value = systemInfo.packageManager.displayName)
                        InfoItem(label = Loc.s(R.string.s0749), value = Loc.s(R.string.s0750, reposCount))
                        InfoItem(label = Loc.s(R.string.s0134), value = Loc.s(R.string.s0750, installedPackagesCount))
                        InfoItem(label = Loc.s(R.string.s0135), value = Loc.s(R.string.s0750, availablePackagesCount))
                    }
                }
            }

            // 软件源切换卡片
            item {
                MiCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Loc.s(R.string.s0751),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiTheme.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Loc.s(R.string.s0752),
                            fontSize = 13.sp,
                            color = MiTheme.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 自动配置按钮
                        MiButton(
                            text = Loc.s(R.string.s0753),
                            onClick = onAutoConfigure,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSwitchingRepo
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 预设源列表
                        presetRepos.forEach { preset ->
                            RepoPresetItem(
                                preset = preset,
                                isLoading = isSwitchingRepo,
                                onClick = { onSwitchRepo(preset) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 架构验证提示
            item {
                MiCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = MiColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Loc.s(R.string.s0754),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiTheme.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Loc.s(R.string.s0755),
                            fontSize = 13.sp,
                            color = MiTheme.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            // 固件升级入口卡片
            item {
                MiCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MiTheme.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Loc.s(R.string.s0420),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiTheme.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Loc.s(R.string.s0756),
                                    fontSize = 13.sp,
                                    color = MiTheme.TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MiTheme.TextTertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        MiButton(
                            text = Loc.s(R.string.s0757),
                            onClick = onNavigateToFirmware,
                            modifier = Modifier.fillMaxWidth(),
                            type = MiButtonType.Secondary
                        )
                    }
                }
            }

        }
    }
}

/**
 * 信息项
 */
@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MiTheme.TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiTheme.TextPrimary
        )
    }
}

/**
 * 软件源预设项
 */
@Composable
private fun RepoPresetItem(
    preset: RepoPreset,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MiColors.Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MiColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MiTheme.TextPrimary
            )
            Text(
                text = preset.description,
                fontSize = 12.sp,
                color = MiTheme.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MiColors.Primary
            )
        } else {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = MiTheme.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ProcessesContent(
    processes: List<ProcessInfo>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onKillProcess: (Int) -> Unit
) {
    var showKillDialog by remember { mutableStateOf<ProcessInfo?>(null) }
    
    if (isLoading) {
        MiLoadingState()
    } else if (error != null && processes.isEmpty()) {
        MiErrorState(message = error, onRetry = onRetry)
    } else if (processes.isEmpty()) {
        MiEmptyState(
            icon = {
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = null,
                    tint = MiTheme.TextTertiary,
                    modifier = Modifier.size(40.dp)
                )
            },
            text = Loc.s(R.string.s0758)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(processes, key = { it.pid }) { process ->
                ProcessCard(
                    process = process,
                    onKill = { showKillDialog = process }
                )
            }
        }
    }
    
    showKillDialog?.let { process ->
        AlertDialog(
            onDismissRequest = { showKillDialog = null },
            icon = {
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = null,
                    tint = MiColors.Error
                )
            },
            title = {
                Text(
                    Loc.s(R.string.s0759),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.dialog_kill_process_confirm, process.name, process.pid))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onKillProcess(process.pid)
                        showKillDialog = null
                    }
                ) {
                    Text(
                        Loc.s(R.string.s0760),
                        color = MiColors.Error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillDialog = null }) {
                    Text(
                        Loc.s(R.string.s0276),
                        color = MiTheme.TextTertiary
                    )
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun ProcessCard(
    process: ProcessInfo,
    onKill: () -> Unit
) {
    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiFeatureIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                gradient = MiColors.GradientBlue,
                size = 40.dp,
                iconSize = 20.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = process.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Loc.s(R.string.s0761, process.pid, process.user),
                    fontSize = 12.sp,
                    color = MiTheme.TextTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiLinearProgress(
                        progress = process.cpu / 100f,
                        color = MiColors.Primary,
                        modifier = Modifier.weight(1f),
                        height = 6.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${process.cpu}%",
                        fontSize = 11.sp,
                        color = MiColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiLinearProgress(
                        progress = process.memory / 100f,
                        color = MiColors.Purple,
                        modifier = Modifier.weight(1f),
                        height = 6.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${process.memory}%",
                        fontSize = 11.sp,
                        color = MiColors.Purple,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            MiButton(
                text = Loc.s(R.string.s0760),
                onClick = onKill,
                type = MiButtonType.Secondary,
                modifier = Modifier.width(60.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}


/**
 * 系统状态概览 - CPU和内存使用率
 */
@Composable
fun SystemStatusOverview(
    cpuUsage: Float,
    memoryUsage: Float,
    cpuHistory: List<Float>,
    memoryHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CPU使用率
            MiCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = null,
                            tint = MiColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CPU",
                            fontSize = 13.sp,
                            color = MiTheme.TextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${cpuUsage.toInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LineChart(
                        data = cpuHistory,
                        color = MiColors.Primary,
                        showGrid = false,
                        
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    )
                }
            }
            
            // 内存使用率
            MiCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MiColors.Success,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Loc.s(R.string.s0729),
                            fontSize = 13.sp,
                            color = MiTheme.TextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${memoryUsage.toInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LineChart(
                        data = memoryHistory,
                        color = MiColors.Success,
                        showGrid = false,
                        
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    )
                }
            }
        }
    }
}

/**
 * 系统信息内容
 */
