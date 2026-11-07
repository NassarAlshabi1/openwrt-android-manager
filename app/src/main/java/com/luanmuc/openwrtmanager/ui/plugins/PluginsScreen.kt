package com.luanmuc.openwrtmanager.ui.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.net.Uri
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.data.model.PackageInfo
import com.luanmuc.openwrtmanager.data.model.RecommendedPlugin
import com.luanmuc.openwrtmanager.ui.components.MiButton
import com.luanmuc.openwrtmanager.ui.components.MiButtonType
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiEmptyState
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiLinearProgress
import com.luanmuc.openwrtmanager.ui.components.MiTag
import com.luanmuc.openwrtmanager.ui.components.MiListItem
import com.luanmuc.openwrtmanager.ui.components.MiTextField
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.OfflineBanner
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 插件页 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    viewModel: PluginsViewModel = viewModel(),
    onPluginClick: (PackageInfo) -> Unit = {},
    onNavigateToRepos: () -> Unit = {},
    onNavigateToOfflineInstall: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(Loc.s(R.string.s0323), Loc.s(R.string.s0585), Loc.s(R.string.s0404))
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val fileName = getFileName(context, selectedUri)
                val fileData = readFileData(context, selectedUri)
                if (fileData != null) {
                    viewModel.installIpk(fileName, fileData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            MiTopAppBar(
                title = stringResource(R.string.plugins_title),
                actions = {
                    IconButton(onClick = { viewModel.updateRepo() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = Loc.s(R.string.s0586),
                            tint = if (uiState.isUpdatingRepo) MiTheme.Primary else MiTheme.TextSecondary
                        )
                    }
                    IconButton(onClick = { showSortMenu = !showSortMenu }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = Loc.s(R.string.s0357),
                            tint = MiTheme.TextSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.loadPackages() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = Loc.s(R.string.s0322),
                            tint = MiTheme.TextSecondary
                        )
                    }
                    // 更多操作菜单
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = Loc.s(R.string.s0587),
                                tint = MiTheme.TextSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.systemInfo.packageManager == com.luanmuc.openwrtmanager.data.model.PackageManagerType.APK) Loc.s(R.string.s0588) else Loc.s(R.string.s0589),
                                        color = MiTheme.TextPrimary,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    filePickerLauncher.launch("*/*")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.InstallMobile,
                                        contentDescription = null,
                                        tint = MiTheme.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        Loc.s(R.string.offline_title),
                                        color = MiTheme.TextPrimary,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateToOfflineInstall()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = MiTheme.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        Loc.s(R.string.s0590),
                                        color = MiTheme.TextPrimary,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.updateRepo()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = MiTheme.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        containerColor = MiTheme.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 离线提示条
            item {
                OfflineBanner(isOffline = !viewModel.isNetworkAvailable)
            }

            // 搜索框
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MiTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = Loc.s(R.string.s0591),
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

            // 分类筛选栏
            item {
                CategoryFilterBar(
                    selectedCategory = uiState.selectedCategory,
                    categories = viewModel.getAvailableCategories(),
                    onCategorySelected = { viewModel.setCategory(it) }
                )
            }

            // 更新软件源进度条
            if (uiState.isUpdatingRepo) {
                item {
                    MiLinearProgress(
                        progress = 0.5f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    Text(
                        text = Loc.s(R.string.s0141),
                        fontSize = 12.sp,
                        color = MiTheme.TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // 推荐插件区域
            item {
                RecommendedPluginsSection(
                    recommendedPlugins = viewModel.getRecommendedPlugins(),
                    installedPackages = uiState.installedPackages,
                    onInstallClick = { viewModel.installPackage(it) },
                    onDetailClick = { /* 跳转到详情 */ }
                )
            }

            // 快捷操作区域（只保留软件源管理）
            item {
                QuickActionsSection(
                    onManageRepos = { onNavigateToRepos() },
                    packageManager = uiState.systemInfo.packageManager
                )
            }

            // Tab栏
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
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
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium
                                )
                            },
                            selectedContentColor = MiColors.Primary,
                            unselectedContentColor = MiTheme.TextTertiary
                        )
                    }
                }
            }

            // 排序方式选择
            if (showSortMenu) {
                item {
                    MiCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                Loc.s(R.string.s0358),
                                modifier = Modifier.padding(16.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTheme.TextPrimary
                            )
                            PluginsViewModel.SortType.values().forEach { sortType ->
                                MiListItem(
                                    title = sortType.displayName,
                                    onClick = {
                                        viewModel.setSortType(sortType)
                                        showSortMenu = false
                                    },
                                    trailing = {
                                        if (uiState.sortType == sortType) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MiColors.Primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MiColors.Primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            Loc.s(R.string.s0312),
                            color = MiTheme.TextTertiary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                val packages = when (selectedTab) {
                    0 -> viewModel.getFilteredInstalled()
                    1 -> viewModel.getFilteredAvailable()
                    2 -> (viewModel.getFilteredInstalled() + viewModel.getFilteredAvailable()).distinctBy { it.name }
                    else -> emptyList()
                }

                if (packages.isEmpty()) {
                    item {
                        MiEmptyState(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = MiTheme.TextTertiary,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            text = when (selectedTab) {
                                0 -> Loc.s(R.string.s0592)
                                1 -> Loc.s(R.string.s0593)
                                else -> Loc.s(R.string.s0594)
                            }
                        )
                    }
                } else {
                    items(packages, key = { it.name }) { plugin ->
                        val isInstalled = uiState.installedPackages.any { it.name == plugin.name }
                        PluginCard(
                            plugin = plugin,
                            isInstalled = isInstalled,
                            isLoading = uiState.actionLoading == plugin.name,
                            installProgress = uiState.installProgress[plugin.name] ?: 0,
                            onInstall = { viewModel.installPackage(plugin.name) },
                            onRemove = { viewModel.removePackage(plugin.name) },
                            onClick = {
                                viewModel.showPackageDetail(plugin)
                                scope.launch { sheetState.show() }
                            }
                        )
                    }
                }
            }
        }

        // 插件详情页
        if (uiState.showDetail && uiState.selectedPackage != null) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = {
                    viewModel.hidePackageDetail()
                },
                
            ) {
                uiState.selectedPackage?.let { pkg ->
                    PluginDetailSheet(
                        plugin = pkg,
                        isInstalled = uiState.installedPackages.any { it.name == pkg.name },
                        isLoading = uiState.actionLoading == pkg.name,
                        onInstall = { viewModel.installPackage(pkg.name) },
                        onRemove = { viewModel.removePackage(pkg.name) },
                        onOpen = {
                            onPluginClick(pkg)
                            scope.launch { sheetState.hide() }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 分类筛选栏
 */
@Composable
fun CategoryFilterBar(
    selectedCategory: PluginsViewModel.PluginCategory,
    categories: List<PluginsViewModel.PluginCategory>,
    onCategorySelected: (PluginsViewModel.PluginCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MiColors.Primary else MiTheme.CardBackground
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = category.displayName,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else MiTheme.TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun PluginCard(
    plugin: PackageInfo,
    isInstalled: Boolean,
    isLoading: Boolean,
    installProgress: Int = 0,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    MiCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiFeatureIcon(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    gradient = if (isInstalled) MiColors.GradientBlue else MiColors.GradientOrange,
                    size = 44.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.description.ifEmpty { plugin.name },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiTheme.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plugin.name + if (plugin.version.isNotEmpty()) " v${plugin.version}" else "",
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary
                    )
                    if (plugin.category.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Loc.s(R.string.s0595, plugin.category),
                            fontSize = 11.sp,
                            color = MiTheme.TextTertiary
                        )
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MiColors.Primary,
                        strokeWidth = 2.dp
                    )
                } else if (isInstalled) {
                    MiButton(
                        text = Loc.s(R.string.s0596),
                        onClick = onRemove,
                        type = MiButtonType.Secondary,
                        modifier = Modifier.width(70.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                } else {
                    MiButton(
                        text = Loc.s(R.string.s0324),
                        onClick = onInstall,
                        type = MiButtonType.Primary,
                        modifier = Modifier.width(70.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // 安装进度条
            if (isLoading && installProgress > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                MiLinearProgress(
                    progress = installProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Loc.s(R.string.s0597, installProgress),
                    fontSize = 11.sp,
                    color = MiTheme.TextTertiary
                )
            }

            // 大小显示
            if (plugin.size > 0 && !isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Loc.s(R.string.s0598, formatSize(plugin.size)),
                    fontSize = 11.sp,
                    color = MiTheme.TextTertiary
                )
            }
        }
    }
}

/**
 * 插件详情页
 */
@Composable
fun PluginDetailSheet(
    plugin: PackageInfo,
    isInstalled: Boolean,
    isLoading: Boolean,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // 标题行
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiFeatureIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                },
                gradient = if (isInstalled) MiColors.GradientBlue else MiColors.GradientOrange,
                size = 56.dp,
                iconSize = 28.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.description.ifEmpty { plugin.name },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plugin.name,
                    fontSize = 13.sp,
                    color = MiTheme.TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 基本信息
        MiCard {
            Column {
                DetailItem(Loc.s(R.string.s0574), if (plugin.version.isNotEmpty()) plugin.version else Loc.s(R.string.s0050))
                DetailItem(Loc.s(R.string.s0575), if (plugin.size > 0) formatSize(plugin.size) else Loc.s(R.string.s0050))
                DetailItem(Loc.s(R.string.s0599), if (plugin.category.isNotEmpty()) plugin.category else Loc.s(R.string.s0600))
                DetailItem(Loc.s(R.string.s0601), if (isInstalled) Loc.s(R.string.s0323) else Loc.s(R.string.s0602))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 插件描述
        if (plugin.description.isNotEmpty()) {
            Text(
                text = Loc.s(R.string.s0570),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiTheme.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiCard {
                Text(
                    text = plugin.description,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 14.sp,
                    color = MiTheme.TextSecondary,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 依赖信息
        if (plugin.depends.isNotEmpty()) {
            Text(
                text = Loc.s(R.string.s0603),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiTheme.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    plugin.depends.forEach { dep ->
                        Text(
                            text = "• $dep",
                            fontSize = 13.sp,
                            color = MiTheme.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 操作按钮
        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MiColors.Primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Loc.s(R.string.s0604),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                color = MiTheme.TextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else if (isInstalled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiButton(
                    text = Loc.s(R.string.s0605),
                    onClick = onOpen,
                    type = MiButtonType.Primary,
                    modifier = Modifier.weight(1f)
                )
                MiButton(
                    text = Loc.s(R.string.s0596),
                    onClick = onRemove,
                    type = MiButtonType.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            MiButton(
                text = Loc.s(R.string.s0606),
                onClick = onInstall,
                type = MiButtonType.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * 详情项
 */
@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MiTheme.TextSecondary,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MiTheme.TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1048576 -> String.format("%.1f MB", bytes / 1048576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

/**
 * 从Uri获取文件名
 */
private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = it.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.let { path ->
            val cut = path.lastIndexOf('/')
            if (cut != -1) path.substring(cut + 1) else path
        }
    }
    return result ?: "unknown.ipk"
}

/**
 * 从Uri读取文件数据
 */
private fun readFileData(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 推荐插件区域
 */
@Composable
private fun RecommendedPluginsSection(
    recommendedPlugins: List<RecommendedPlugin>,
    installedPackages: List<PackageInfo>,
    onInstallClick: (String) -> Unit,
    onDetailClick: (RecommendedPlugin) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = Loc.s(R.string.s0607),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiTheme.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recommendedPlugins) { plugin ->
                val isInstalled = installedPackages.any { it.name == plugin.name }
                MiCard(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onDetailClick(plugin) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = plugin.icon,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = plugin.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiTheme.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = plugin.description,
                            fontSize = 12.sp,
                            color = MiTheme.TextTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (isInstalled) {
                            MiTag(
                                text = Loc.s(R.string.s0323),
                                backgroundColor = MiTheme.Success.copy(alpha = 0.1f),
                                textColor = MiTheme.Success
                            )
                        } else {
                            MiButton(
                                text = Loc.s(R.string.s0324),
                                type = MiButtonType.Primary,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onInstallClick(plugin.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 快捷操作区域
 */
@Composable
private fun QuickActionsSection(
    onManageRepos: () -> Unit,
    packageManager: com.luanmuc.openwrtmanager.data.model.PackageManagerType = com.luanmuc.openwrtmanager.data.model.PackageManagerType.OPKG
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiCard(
            modifier = Modifier
                .weight(1f)
                .clickable { onManageRepos() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Extension,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Loc.s(R.string.s0608),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
            }
        }
        
        MiCard(
            modifier = Modifier
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Loc.s(R.string.s0590),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
            }
        }
    }
}
