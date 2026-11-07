package com.luanmuc.openwrtmanager.ui.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.data.model.OfflinePackageEntry
import com.luanmuc.openwrtmanager.data.model.OfflinePhase
import com.luanmuc.openwrtmanager.data.repository.OfflinePackageRepository
import com.luanmuc.openwrtmanager.ui.components.MiButton
import com.luanmuc.openwrtmanager.ui.components.MiButtonType
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiEmptyState
import com.luanmuc.openwrtmanager.ui.components.MiLinearProgress
import com.luanmuc.openwrtmanager.ui.components.MiTag
import com.luanmuc.openwrtmanager.ui.components.MiTextField
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 离线安装页 —— 从官方源下载 .ipk 到手机并推送安装到路由器
 * （路由器无需联网）。界面支持阿拉伯语/英语与 RTL。
 */
@Composable
fun OfflinePackagesScreen(
    viewModel: OfflinePackagesViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var detailEntry by remember { mutableStateOf<OfflinePackageEntry?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }

    // 过滤后的可见列表（依赖 key 变化时重算）
    val visible = remember(
        uiState.searchQuery, uiState.feedFilter, uiState.hideInstalled,
        uiState.installedNames, uiState.indexCount, uiState.selected, uiState.downloadedNames
    ) { viewModel.getVisiblePackages() }

    // 错误提示
    LaunchedEffect(uiState.opError) {
        uiState.opError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            MiTopAppBar(
                title = stringResource(R.string.offline_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = Loc.s(R.string.s0001),
                            tint = MiTheme.TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadIndex() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = Loc.s(R.string.offline_refresh_index),
                            tint = MiTheme.TextSecondary
                        )
                    }
                    IconButton(onClick = { showAdvanced = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = Loc.s(R.string.offline_advanced),
                            tint = MiTheme.TextSecondary
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selected.isNotEmpty() && uiState.phase == OfflinePhase.IDLE) {
                SelectionBar(
                    selectedCount = uiState.selected.size,
                    depCount = (uiState.resolution?.toDownload?.size ?: 0) - uiState.selected.count { !uiState.installedNames.contains(it) },
                    totalSize = uiState.resolution?.totalBytes ?: 0,
                    allDownloaded = uiState.allDownloaded,
                    onDownloadAndInstall = { viewModel.downloadSelected(thenInstall = true) },
                    onDownloadOnly = { viewModel.downloadSelected(thenInstall = false) },
                    onInstall = { viewModel.installDownloaded() },
                    onClear = { viewModel.clearSelection() }
                )
            }
        },
        containerColor = MiTheme.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ---------- 路由器信息卡片 ----------
            item { RouterInfoCard(uiState, onRetry = { viewModel.start() }) }

            // ---------- 索引状态 ----------
            item { IndexStatusBar(uiState) }

            // ---------- 搜索与过滤 ----------
            if (uiState.indexCount > 0) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        MiTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = Loc.s(R.string.offline_search_hint),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MiTheme.TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
                item { FeedFilterRow(uiState, viewModel) }
            }

            // ---------- 包列表 ----------
            if (uiState.indexCount > 0 && uiState.phase != OfflinePhase.LOADING_INDEX) {
                if (visible.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp)) {
                            MiEmptyState(
                                icon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp)) },
                                text = Loc.s(R.string.offline_index_empty)
                            )
                        }
                    }
                } else {
                    items(visible, key = { "${it.feed}:${it.name}" }) { entry ->
                        PackageRow(
                            entry = entry,
                            installed = uiState.installedNames.contains(entry.name),
                            downloaded = uiState.downloadedNames.contains(entry.name),
                            selected = uiState.selected.contains(entry.name),
                            onToggle = { viewModel.toggleSelect(entry.name) },
                            onInfo = { detailEntry = entry }
                        )
                    }
                    item {
                        Text(
                            text = Loc.s(R.string.offline_select_hint),
                            fontSize = 12.sp,
                            color = MiTheme.TextTertiary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            // ---------- 检测中 ----------
            if (uiState.phase == OfflinePhase.DETECTING) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MiTheme.Primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = Loc.s(R.string.offline_detecting),
                                fontSize = 14.sp,
                                color = MiTheme.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------- 详情对话框 ----------
    detailEntry?.let { entry ->
        PackageDetailDialog(entry = entry, onDismiss = { detailEntry = null })
    }

    // ---------- 高级设置对话框 ----------
    if (showAdvanced) {
        AdvancedSettingsDialog(
            mirror = uiState.mirror,
            versionOverride = uiState.versionOverride,
            detectedVersion = uiState.router?.version ?: "",
            onSave = { m, v ->
                viewModel.saveAdvancedSettings(m, v)
                showAdvanced = false
            },
            onDismiss = { showAdvanced = false }
        )
    }

    // ---------- 进度 / 结果对话框 ----------
    if (uiState.phase == OfflinePhase.DOWNLOADING ||
        uiState.phase == OfflinePhase.UPLOADING ||
        uiState.phase == OfflinePhase.INSTALLING
    ) {
        OperationProgressDialog(uiState, onCancel = { viewModel.cancelOperation() })
    } else if (uiState.phase == OfflinePhase.DONE || uiState.phase == OfflinePhase.FAILED) {
        ResultDialog(uiState, onClose = { viewModel.dismissResult() })
    }
}

// =====================================================================
//  路由器信息卡片
// =====================================================================

@Composable
private fun RouterInfoCard(
    uiState: OfflinePackagesViewModel.OfflineUiState,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        MiCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.InstallMobile,
                        contentDescription = null,
                        tint = MiTheme.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Loc.s(R.string.offline_router_info),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiTheme.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val router = uiState.router
                when {
                    uiState.detectError != null -> {
                        Text(
                            text = uiState.detectError ?: "",
                            fontSize = 13.sp,
                            color = MiColors.Error
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        MiButton(
                            text = Loc.s(R.string.s0322),
                            onClick = onRetry,
                            type = MiButtonType.Secondary,
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    router != null -> {
                        InfoLine(Loc.s(R.string.offline_version_line, router.version.ifBlank { "?" }))
                        InfoLine(Loc.s(R.string.offline_arch_line, router.architecture.ifBlank { "?" }))
                        if (router.kernelRelease.isNotBlank()) {
                            InfoLine(Loc.s(R.string.offline_kernel_line, router.kernelRelease))
                        }
                        if (router.target.isNotBlank()) {
                            InfoLine(Loc.s(R.string.offline_target_line, router.target))
                        }
                        uiState.spaceInfo?.let { space ->
                            if (space.tmpFree >= 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = Loc.s(
                                        R.string.offline_space_line,
                                        OfflinePackageRepository.formatBytes(space.tmpFree),
                                        OfflinePackageRepository.formatBytes(space.overlayFree)
                                    ),
                                    fontSize = 12.sp,
                                    color = MiTheme.TextTertiary
                                )
                            }
                        }
                        if (!uiState.isSupportedVersion && router.version.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Loc.s(R.string.offline_unsupported_version, router.version),
                                fontSize = 12.sp,
                                color = Color(0xFFB25000),
                                lineHeight = 16.sp
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = Loc.s(R.string.offline_detecting),
                            fontSize = 13.sp,
                            color = MiTheme.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MiTheme.TextSecondary,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}

// =====================================================================
//  索引状态栏
// =====================================================================

@Composable
private fun IndexStatusBar(uiState: OfflinePackagesViewModel.OfflineUiState) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        when {
            uiState.indexLoading || uiState.phase == OfflinePhase.LOADING_INDEX -> {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MiTheme.Primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Loc.s(R.string.offline_index_progress, uiState.indexProgress, uiState.indexTotal),
                            fontSize = 13.sp,
                            color = MiTheme.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MiLinearProgress(
                        progress = if (uiState.indexTotal > 0)
                            uiState.indexProgress.toFloat() / uiState.indexTotal else 0f
                    )
                }
            }
            uiState.indexError != null -> {
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MiColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.indexError ?: "",
                            fontSize = 13.sp,
                            color = MiColors.Error,
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            uiState.indexCount > 0 -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MiTheme.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Loc.s(R.string.offline_index_count, uiState.indexCount),
                        fontSize = 13.sp,
                        color = MiTheme.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        if (uiState.kmodsUnavailable) {
            Text(
                text = Loc.s(R.string.offline_kmods_unavailable),
                fontSize = 12.sp,
                color = Color(0xFFB25000),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

// =====================================================================
//  Feed 过滤行
// =====================================================================

@Composable
private fun FeedFilterRow(
    uiState: OfflinePackagesViewModel.OfflineUiState,
    viewModel: OfflinePackagesViewModel
) {
    val feeds = remember(uiState.indexCount) { viewModel.getAvailableFeeds() }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = uiState.feedFilter == null,
                    onClick = { viewModel.setFeedFilter(null) },
                    label = { Text(Loc.s(R.string.offline_feed_all), fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MiTheme.Primary.copy(alpha = 0.12f),
                        selectedLabelColor = MiTheme.Primary
                    )
                )
            }
            items(feeds) { feed ->
                FilterChip(
                    selected = uiState.feedFilter == feed,
                    onClick = { viewModel.setFeedFilter(if (uiState.feedFilter == feed) null else feed) },
                    label = { Text(feed, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MiTheme.Primary.copy(alpha = 0.12f),
                        selectedLabelColor = MiTheme.Primary
                    )
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.toggleHideInstalled() }
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (uiState.hideInstalled) MiTheme.Primary else MiTheme.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (uiState.hideInstalled) Loc.s(R.string.offline_hide_installed)
                else Loc.s(R.string.offline_show_installed),
                fontSize = 12.sp,
                color = MiTheme.TextTertiary
            )
        }
    }
}

// =====================================================================
//  包条目
// =====================================================================

@Composable
private fun PackageRow(
    entry: OfflinePackageEntry,
    installed: Boolean,
    downloaded: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onInfo: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        MiCard(
            backgroundColor = if (selected) MiTheme.Primary.copy(alpha = 0.06f) else MiTheme.CardBackground
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MiTheme.Primary,
                        uncheckedColor = MiTheme.TextTertiary
                    )
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiTheme.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${entry.version} • ${OfflinePackageRepository.formatBytes(entry.size)}",
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.description.isNotBlank()) {
                        Text(
                            text = entry.description,
                            fontSize = 12.sp,
                            color = MiTheme.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (installed || downloaded || entry.feed.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (installed) {
                                MiTag(
                                    text = Loc.s(R.string.offline_installed_tag),
                                    backgroundColor = Color(0xFFE8F5E9),
                                    textColor = Color(0xFF2E7D32)
                                )
                            }
                            if (downloaded) {
                                MiTag(
                                    text = Loc.s(R.string.offline_downloaded_tag),
                                    backgroundColor = Color(0xFFE3F2FD),
                                    textColor = Color(0xFF1565C0)
                                )
                            }
                            if (entry.depends.isNotEmpty()) {
                                MiTag(text = Loc.s(R.string.offline_deps_tag, entry.depends.size))
                            }
                            MiTag(
                                text = entry.feed,
                                backgroundColor = MiTheme.TextTertiary.copy(alpha = 0.1f),
                                textColor = MiTheme.TextSecondary
                            )
                        }
                    }
                }
                IconButton(onClick = onInfo) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = Loc.s(R.string.offline_details_title),
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
//  底部选择栏
// =====================================================================

@Composable
private fun SelectionBar(
    selectedCount: Int,
    depCount: Int,
    totalSize: Long,
    allDownloaded: Boolean,
    onDownloadAndInstall: () -> Unit,
    onDownloadOnly: () -> Unit,
    onInstall: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        color = MiTheme.CardBackground,
        shadowElevation = 12.dp,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Loc.s(
                        R.string.offline_selected_summary,
                        selectedCount,
                        depCount.coerceAtLeast(0),
                        OfflinePackageRepository.formatBytes(totalSize)
                    ),
                    fontSize = 13.sp,
                    color = MiTheme.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = Loc.s(R.string.s0587),
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiButton(
                    text = Loc.s(R.string.offline_btn_download_install),
                    onClick = onDownloadAndInstall,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                )
                if (allDownloaded) {
                    MiButton(
                        text = Loc.s(R.string.offline_btn_install),
                        onClick = onInstall,
                        type = MiButtonType.Secondary,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    )
                } else {
                    MiButton(
                        text = Loc.s(R.string.offline_btn_download_only),
                        onClick = onDownloadOnly,
                        type = MiButtonType.Secondary,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
//  进度对话框
// =====================================================================

@Composable
private fun OperationProgressDialog(
    uiState: OfflinePackagesViewModel.OfflineUiState,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MiTheme.Primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (uiState.phase) {
                        OfflinePhase.DOWNLOADING -> Loc.s(R.string.offline_downloading_file, uiState.currentFile ?: "")
                        OfflinePhase.UPLOADING -> Loc.s(
                            R.string.offline_uploading,
                            OfflinePackageRepository.formatBytes(uiState.uploadedBytes),
                            OfflinePackageRepository.formatBytes(uiState.uploadTotal)
                        )
                        else -> Loc.s(R.string.offline_installing)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column {
                val progress = when (uiState.phase) {
                    OfflinePhase.DOWNLOADING ->
                        if (uiState.totalBytes > 0) uiState.downloadedBytes.toFloat() / uiState.totalBytes else 0f
                    OfflinePhase.UPLOADING ->
                        if (uiState.uploadTotal > 0) uiState.uploadedBytes.toFloat() / uiState.uploadTotal else 0f
                    else -> null
                }
                progress?.let {
                    MiLinearProgress(progress = it)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(it * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary
                    )
                }
                if (uiState.opLog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = Loc.s(R.string.offline_oplog_title),
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ConsoleBox(uiState.opLog)
                }
            }
        },
        confirmButton = {
            MiButton(
                text = Loc.s(R.string.offline_cancel_btn),
                onClick = onCancel,
                type = MiButtonType.Text
            )
        },
        containerColor = MiTheme.CardBackground
    )
}

// =====================================================================
//  结果对话框
// =====================================================================

@Composable
private fun ResultDialog(
    uiState: OfflinePackagesViewModel.OfflineUiState,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (uiState.phase == OfflinePhase.DONE) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (uiState.phase == OfflinePhase.DONE) Color(0xFF2E7D32) else MiColors.Error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (uiState.phase == OfflinePhase.DONE)
                        Loc.s(R.string.offline_install_success, uiState.opResult?.installedCount ?: 0)
                    else Loc.s(R.string.offline_install_failed, uiState.opError ?: ""),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column {
                val output = uiState.opResult?.output
                if (!output.isNullOrBlank()) {
                    ConsoleBox(output.lineSequence().toList())
                }
            }
        },
        confirmButton = {
            MiButton(
                text = Loc.s(R.string.offline_close),
                onClick = onClose,
                type = MiButtonType.Text
            )
        },
        containerColor = MiTheme.CardBackground
    )
}

// =====================================================================
//  控制台日志框
// =====================================================================

@Composable
private fun ConsoleBox(lines: List<String>) {
    val scrollState = rememberScrollState()
    LaunchedEffect(lines.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .verticalScroll(scrollState)
            .padding(10.dp)
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFD4D4D4),
                lineHeight = 15.sp
            )
        }
    }
}

// =====================================================================
//  详情对话框
// =====================================================================

@Composable
private fun PackageDetailDialog(entry: OfflinePackageEntry, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = entry.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow(Loc.s(R.string.offline_details_version), entry.version)
                DetailRow(Loc.s(R.string.offline_details_size), OfflinePackageRepository.formatBytes(entry.size))
                DetailRow(
                    Loc.s(R.string.offline_details_installed_size),
                    OfflinePackageRepository.formatBytes(entry.installedSize)
                )
                DetailRow(Loc.s(R.string.offline_details_section), entry.section.ifBlank { "-" })
                DetailRow(Loc.s(R.string.offline_details_arch), entry.architecture.ifBlank { "-" })
                DetailRow(Loc.s(R.string.offline_details_feed), entry.feed)
                HorizontalDivider(color = MiColors.Divider)
                DetailRow(
                    Loc.s(R.string.offline_details_deps),
                    entry.depends.joinToString(", ").ifBlank { Loc.s(R.string.offline_details_none) }
                )
                if (entry.provides.isNotEmpty()) {
                    DetailRow(Loc.s(R.string.offline_details_provides), entry.provides.joinToString(", "))
                }
                if (entry.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.description,
                        fontSize = 13.sp,
                        color = MiTheme.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            MiButton(
                text = Loc.s(R.string.offline_close),
                onClick = onDismiss,
                type = MiButtonType.Text
            )
        },
        containerColor = MiTheme.CardBackground
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MiTheme.TextTertiary,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MiTheme.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

// =====================================================================
//  高级设置对话框（镜像 / 版本覆盖）
// =====================================================================

@Composable
private fun AdvancedSettingsDialog(
    mirror: String,
    versionOverride: String,
    detectedVersion: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var mirrorInput by remember { mutableStateOf(mirror) }
    var versionInput by remember { mutableStateOf(versionOverride) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Loc.s(R.string.offline_advanced),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                MiTextField(
                    value = mirrorInput,
                    onValueChange = { mirrorInput = it },
                    label = Loc.s(R.string.offline_mirror_label),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                MiTextField(
                    value = versionInput,
                    onValueChange = { versionInput = it },
                    label = Loc.s(R.string.offline_version_label),
                    placeholder = detectedVersion.ifBlank { Loc.s(R.string.offline_version_hint) },
                    supportingText = Loc.s(R.string.offline_version_hint),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row {
                MiButton(
                    text = Loc.s(R.string.offline_close),
                    onClick = onDismiss,
                    type = MiButtonType.Text
                )
                MiButton(
                    text = Loc.s(R.string.offline_save),
                    onClick = { onSave(mirrorInput, versionInput) },
                    type = MiButtonType.Text
                )
            }
        },
        containerColor = MiTheme.CardBackground
    )
}
