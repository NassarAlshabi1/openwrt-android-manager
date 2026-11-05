package com.luanmuc.openwrtmanager.ui.backup
import com.luanmuc.openwrtmanager.util.DebounceUtils

import com.luanmuc.openwrtmanager.R

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.data.model.BackupType
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 配置备份恢复页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val backupList by viewModel.backupList.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val restoreProgress by viewModel.restoreProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    // 错误提示
    LaunchedEffect(error) {
        error?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    viewModel.createBackup(
                        name = Loc.s(R.string.s0280),
                        description = Loc.s(R.string.s0281),
                        backupType = BackupType.FULL
                    )
                },
                containerColor = MiTheme.Primary
            ) {
                Icon(Icons.Default.Backup, contentDescription = Loc.s(R.string.s0282))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(MiDimens.horizontalPadding)
        ) {
            // 备份统计卡片
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                BackupStatsCard(
                    count = backupList.size,
                    isCreating = isCreating
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 恢复进度
            restoreProgress?.let { progress ->
                item {
                    RestoreProgressCard(
                        progress = progress,
                        onDismiss = { viewModel.clearRestoreProgress() }
                    )
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                }
            }
            
            // 备份列表标题
            item {
                SectionTitle(
                    title = Loc.s(R.string.s0283),
                    icon = Icons.Default.Backup
                )
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MiTheme.Primary)
                    }
                }
            } else if (backupList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Loc.s(R.string.s0284),
                            color = MiTheme.TextTertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(backupList) { backup ->
                    BackupItemCard(
                        backup = backup,
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * 备份统计卡片
 */
@Composable
private fun BackupStatsCard(
    count: Int,
    isCreating: Boolean
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
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MiTheme.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        color = MiTheme.Primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Backup,
                        contentDescription = Loc.s(R.string.s0285),
                        tint = MiTheme.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 统计信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Loc.s(R.string.s0286, count),
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCreating) Loc.s(R.string.s0287) else Loc.s(R.string.s0288),
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 恢复进度卡片
 */
@Composable
private fun RestoreProgressCard(
    progress: com.luanmuc.openwrtmanager.data.model.RestoreProgress,
    onDismiss: () -> Unit
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
                Icon(
                    Icons.Default.Restore,
                    contentDescription = Loc.s(R.string.s0289),
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0290),
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = { progress.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MiTheme.Primary,
                trackColor = MiTheme.Divider
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = progress.currentStepName,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${progress.percentage}%",
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (progress.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.common_complete))
                }
            }
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
 * 备份项卡片
 */
@Composable
private fun BackupItemCard(
    backup: com.luanmuc.openwrtmanager.data.model.BackupRecord,
    viewModel: BackupViewModel
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
                // 备份类型图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(MiDimens.iconRadius))
                        .background(MiTheme.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Backup,
                        contentDescription = backup.backupType.displayName,
                        tint = MiTheme.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 备份信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = backup.name,
                        color = MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = backup.backupType.displayName,
                        color = MiTheme.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // 大小
                Text(
                    text = viewModel.formatFileSize(backup.size),
                    color = MiTheme.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 详细信息
            Text(
                text = Loc.s(R.string.s0291, viewModel.formatTime(backup.createdAt)),
                color = MiTheme.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
            
            if (backup.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = backup.description,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { if (DebounceUtils.canClick()) viewModel.restoreBackup(backup.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MiTheme.Primary
                    ),
                    shape = RoundedCornerShape(MiDimens.buttonRadius)
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_restore))
                }
                
                OutlinedButton(
                    onClick = { if (DebounceUtils.canClick()) viewModel.deleteBackup(backup.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MiTheme.Error
                    ),
                    shape = RoundedCornerShape(MiDimens.buttonRadius)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }
}
