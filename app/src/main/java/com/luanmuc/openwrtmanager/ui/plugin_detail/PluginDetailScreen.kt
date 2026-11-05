package com.luanmuc.openwrtmanager.ui.plugin_detail
import com.luanmuc.openwrtmanager.util.DebounceUtils
import com.luanmuc.openwrtmanager.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 插件详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    packageName: String,
    onBack: () -> Unit,
    viewModel: PluginDetailViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val pluginDetail by viewModel.pluginDetail.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val isInstalling by viewModel.isInstalling.collectAsState()
    val error by viewModel.error.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    
    // 首次加载
    androidx.compose.runtime.LaunchedEffect(packageName) {
        viewModel.loadPluginDetail(packageName)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pluginDetail?.displayName ?: Loc.s(R.string.s0569)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = Loc.s(R.string.s0244))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MiTheme.CardBackground,
                    titleContentColor = MiTheme.TextPrimary
                )
            )
        },
        containerColor = MiTheme.Background,
        bottomBar = {
            pluginDetail?.let { plugin ->
                BottomActionBar(
                    isInstalled = plugin.isInstalled,
                    isUpdateAvailable = plugin.isUpdateAvailable,
                    isInstalling = isInstalling,
                    onInstall = { if (DebounceUtils.canClick()) viewModel.installPlugin() },
                    onUninstall = { if (DebounceUtils.canClick()) viewModel.uninstallPlugin() },
                    onUpdate = { if (DebounceUtils.canClick()) viewModel.updatePlugin() }
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MiTheme.Primary)
            }
        } else if (pluginDetail != null) {
            pluginDetail?.let { plugin ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(MiDimens.horizontalPadding)
            ) {
                // 错误提示
                if (error != null) {
                    item {
                        Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MiTheme.Error.copy(alpha = 0.1f), MiTheme.shapes.smallShape)
                                .padding(MiDimens.cardPadding)
                        ) {
                            Text(
                                text = error ?: "",
                                color = MiTheme.Error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                    }
                }
                
                // 操作结果提示
                if (operationResult != null) {
                    item {
                        Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MiTheme.Success.copy(alpha = 0.1f), MiTheme.shapes.smallShape)
                                .padding(MiDimens.cardPadding)
                        ) {
                            Text(
                                text = operationResult ?: "",
                                color = MiTheme.Success,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                    }
                }
                
                // 插件基本信息
                item {
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                    PluginHeaderCard(plugin = plugin, viewModel = viewModel)
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                }
                
                // 插件信息
                item {
                    PluginInfoCard(plugin = plugin, viewModel = viewModel)
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                }
                
                // 插件描述
                item {
                    SectionTitle(title = Loc.s(R.string.s0570), icon = Icons.Default.Extension)
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                    PluginDescriptionCard(plugin = plugin)
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                }
                
                // 用户评价
                item {
                    SectionTitle(title = Loc.s(R.string.s0571), icon = Icons.Default.Star)
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                }
                
                if (reviews.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Loc.s(R.string.s0572),
                                color = MiTheme.TextTertiary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(reviews.take(5)) { review ->
                        ReviewItemCard(review = review, viewModel = viewModel)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            } // end let
        }
    }
}

/**
 * 分区标题
 */
@Composable
private fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MiTheme.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = MiTheme.TextSecondary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 插件头部卡片
 */
@Composable
private fun PluginHeaderCard(
    plugin: com.luanmuc.openwrtmanager.data.model.PluginDetail,
    viewModel: PluginDetailViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiDimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 插件图标
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(MiDimens.iconRadius))
                    .background(MiTheme.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Extension,
                    contentDescription = plugin.displayName,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 插件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.displayName,
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plugin.category,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MiTheme.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${plugin.rating}",
                        color = MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Loc.s(R.string.s0573, plugin.downloadCount),
                        color = MiTheme.TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * 插件信息卡片
 */
@Composable
private fun PluginInfoCard(
    plugin: com.luanmuc.openwrtmanager.data.model.PluginDetail,
    viewModel: PluginDetailViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiDimens.cardPadding)
        ) {
            InfoRow(label = Loc.s(R.string.s0574), value = plugin.version)
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = Loc.s(R.string.s0575), value = viewModel.formatFileSize(plugin.size))
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = Loc.s(R.string.s0576), value = plugin.author)
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = Loc.s(R.string.s0577), value = viewModel.formatTime(plugin.lastUpdated))
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = Loc.s(R.string.s0578), value = plugin.license)
        }
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MiTheme.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = MiTheme.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 插件描述卡片
 */
@Composable
private fun PluginDescriptionCard(
    plugin: com.luanmuc.openwrtmanager.data.model.PluginDetail
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiDimens.cardPadding)
        ) {
            Text(
                text = plugin.description,
                color = MiTheme.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (plugin.longDescription.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = plugin.longDescription,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (plugin.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plugin.tags.take(5).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MiTheme.Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = tag,
                                color = MiTheme.Primary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 评价项卡片
 */
@Composable
private fun ReviewItemCard(
    review: com.luanmuc.openwrtmanager.data.model.PluginReview,
    viewModel: PluginDetailViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiDimens.cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 用户头像
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MiTheme.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.userName.first().toString(),
                        color = MiTheme.Primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // 用户名和评分
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        color = MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < review.rating.toInt()) MiTheme.Warning else MiTheme.Divider,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = viewModel.formatTime(review.date),
                            color = MiTheme.TextTertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = review.comment,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 底部操作栏
 */
@Composable
private fun BottomActionBar(
    isInstalled: Boolean,
    isUpdateAvailable: Boolean,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onUpdate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MiTheme.CardBackground,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiDimens.horizontalPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isUpdateAvailable -> {
                    // 更新按钮
                    Button(
                        onClick = onUpdate,
                        enabled = !isInstalling,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MiTheme.Primary
                        ),
                        shape = RoundedCornerShape(MiDimens.buttonRadius)
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.status_updating))
                        } else {
                            Text(stringResource(R.string.common_update))
                        }
                    }
                    
                    // 卸载按钮
                    OutlinedButton(
                        onClick = onUninstall,
                        enabled = !isInstalling,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MiTheme.Error
                        ),
                        shape = RoundedCornerShape(MiDimens.buttonRadius)
                    ) {
                        Text(stringResource(R.string.common_uninstall))
                    }
                }
                isInstalled -> {
                    // 已安装状态
                    OutlinedButton(
                        onClick = onUninstall,
                        enabled = !isInstalling,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MiTheme.Error
                        ),
                        shape = RoundedCornerShape(MiDimens.buttonRadius)
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(
                                color = MiTheme.Error,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.status_uninstalling))
                        } else {
                            Text(stringResource(R.string.common_uninstall))
                        }
                    }
                }
                else -> {
                    // 安装按钮
                    Button(
                        onClick = onInstall,
                        enabled = !isInstalling,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MiTheme.Primary
                        ),
                        shape = RoundedCornerShape(MiDimens.buttonRadius)
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.status_installing))
                        } else {
                            Text(stringResource(R.string.common_install))
                        }
                    }
                }
            }
        }
    }
}
