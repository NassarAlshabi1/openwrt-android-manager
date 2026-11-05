package com.luanmuc.openwrtmanager.ui.devicemanager

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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
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
import com.luanmuc.openwrtmanager.data.model.DeviceEventType
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 设备管理增强页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerScreen(
    onBack: () -> Unit,
    viewModel: DeviceManagerViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    // 错误提示
    LaunchedEffect(error) {
        error?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    val deviceGroups by viewModel.deviceGroups.collectAsState()
    val deviceHistory by viewModel.deviceHistory.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.device_manager_title)) },
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
        containerColor = MiTheme.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(MiDimens.horizontalPadding)
        ) {
            // 设备分组
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                SectionTitle(
                    title = Loc.s(R.string.s0341),
                    icon = Icons.Default.Group
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
            } else {
                items(deviceGroups) { group ->
                    DeviceGroupItem(group = group)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 设备上下线历史
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                SectionTitle(
                    title = Loc.s(R.string.s0342),
                    icon = Icons.Default.History
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
            } else if (deviceHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Loc.s(R.string.s0343),
                            color = MiTheme.TextTertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(deviceHistory.take(20)) { history ->
                    DeviceHistoryItem(
                        history = history,
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
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
 * 设备分组项
 */
@Composable
private fun DeviceGroupItem(
    group: com.luanmuc.openwrtmanager.data.model.DeviceGroup
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
            // 分组图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MiTheme.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = group.name,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 分组信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Loc.s(R.string.s0344, group.deviceCount),
                    color = MiTheme.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 设备历史项
 */
@Composable
private fun DeviceHistoryItem(
    history: com.luanmuc.openwrtmanager.data.model.DeviceHistory,
    viewModel: DeviceManagerViewModel
) {
    val eventColor = when (history.eventType) {
        DeviceEventType.ONLINE -> MiTheme.Success
        DeviceEventType.OFFLINE -> MiTheme.Error
        DeviceEventType.BLOCKED -> MiTheme.Error
        DeviceEventType.UNBLOCKED -> MiTheme.Success
        DeviceEventType.SPEED_LIMITED -> MiTheme.Warning
        DeviceEventType.SPEED_UNLIMITED -> MiTheme.Success
    }
    
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
            // 事件指示器
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(eventColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 设备信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.hostname.ifEmpty { history.ip },
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = history.eventType.displayName,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 时间
            Text(
                text = viewModel.formatTime(history.timestamp),
                color = MiTheme.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
