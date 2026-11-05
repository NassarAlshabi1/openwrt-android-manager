package com.luanmuc.openwrtmanager.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiEmptyState
import com.luanmuc.openwrtmanager.ui.components.MiListItem
import com.luanmuc.openwrtmanager.ui.components.MiLoadingState
import com.luanmuc.openwrtmanager.ui.components.MiTag
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.OfflineBanner
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

@Composable
fun StorageScreen(
    onBack: () -> Unit = {},
    viewModel: StorageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val errorMessage = uiState.error
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0267),
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
                    IconButton(onClick = { viewModel.loadStorageInfo() }) {
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
            OfflineBanner(isOffline = !viewModel.isNetworkAvailable)

            if (uiState.isLoading && uiState.storageTotal == 0L) {
                MiLoadingState(text = Loc.s(R.string.s0725))
            } else if (uiState.error != null && uiState.storageTotal == 0L) {
                MiEmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MiTheme.TextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    text = uiState.error ?: Loc.s(R.string.s0329)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Flash存储卡片
                    item {
                        MiCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MiColors.Warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Loc.s(R.string.s0726),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiTheme.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    val usedPercent = if (uiState.storageTotal > 0) {
                                        (uiState.storageUsed * 100f / uiState.storageTotal).toInt()
                                    } else 0
                                    MiTag(
                                        text = "${usedPercent}%",
                                        backgroundColor = getUsageColor(usedPercent).copy(alpha = 0.1f),
                                        textColor = getUsageColor(usedPercent)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // 进度条
                                val flashUsedPercent = if (uiState.storageTotal > 0) {
                                    (uiState.storageUsed * 100f / uiState.storageTotal).toInt()
                                } else 0
                                StorageProgressBar(
                                    used = uiState.storageUsed,
                                    total = uiState.storageTotal,
                                    color = getUsageColor(flashUsedPercent)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StorageStatItem(
                                        label = Loc.s(R.string.s0727),
                                        value = formatBytes(uiState.storageTotal)
                                    )
                                    StorageStatItem(
                                        label = Loc.s(R.string.s0728),
                                        value = formatBytes(uiState.storageUsed)
                                    )
                                    StorageStatItem(
                                        label = Loc.s(R.string.s0585),
                                        value = formatBytes(uiState.storageFree)
                                    )
                                }
                            }
                        }
                    }

                    // 内存卡片
                    item {
                        MiCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = MiColors.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Loc.s(R.string.s0729),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiTheme.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    val memPercent = if (uiState.memoryTotal > 0) {
                                        (uiState.memoryUsed * 100f / uiState.memoryTotal).toInt()
                                    } else 0
                                    MiTag(
                                        text = "${memPercent}%",
                                        backgroundColor = MiColors.Primary.copy(alpha = 0.1f),
                                        textColor = MiColors.Primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                StorageProgressBar(
                                    used = uiState.memoryUsed,
                                    total = uiState.memoryTotal,
                                    color = MiColors.Primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StorageStatItem(
                                        label = Loc.s(R.string.s0727),
                                        value = formatBytes(uiState.memoryTotal)
                                    )
                                    StorageStatItem(
                                        label = Loc.s(R.string.s0728),
                                        value = formatBytes(uiState.memoryUsed)
                                    )
                                    StorageStatItem(
                                        label = Loc.s(R.string.s0585),
                                        value = formatBytes(uiState.memoryFree)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = Loc.s(R.string.s0730, formatBytes(uiState.memoryCached)),
                                        fontSize = 12.sp,
                                        color = MiTheme.TextTertiary
                                    )
                                    Text(
                                        text = Loc.s(R.string.s0731, formatBytes(uiState.memoryBuffered)),
                                        fontSize = 12.sp,
                                        color = MiTheme.TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    // 系统状态卡片
                    item {
                        MiCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = Loc.s(R.string.s0040),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiTheme.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                InfoItem(label = Loc.s(R.string.s0516), value = formatUptime(uiState.uptime))
                                InfoItem(label = Loc.s(R.string.s0513), value = "${uiState.cpuUsage.toInt()}%")
                                InfoItem(
                                    label = Loc.s(R.string.s0732),
                                    value = "${uiState.loadAverage.getOrElse(0) { 0f }} / ${uiState.loadAverage.getOrElse(1) { 0f }} / ${uiState.loadAverage.getOrElse(2) { 0f }}"
                                )
                                if (uiState.temperature != null) {
                                    InfoItem(label = Loc.s(R.string.s0733), value = "${uiState.temperature}°C")
                                }
                            }
                        }
                    }

                    // 挂载点列表
                    if (uiState.mountPoints.isNotEmpty()) {
                        item {
                            Text(
                                text = Loc.s(R.string.s0734),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTheme.TextSecondary,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                        items(uiState.mountPoints, key = { mount: MountPointInfo -> mount.mountPoint }) { mount: MountPointInfo ->
                            MiCard {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mount.mountPoint,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MiTheme.TextPrimary
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        MiTag(
                                            text = "${mount.usedPercent.toInt()}%",
                                            backgroundColor = getUsageColor(mount.usedPercent.toInt()).copy(alpha = 0.1f),
                                            textColor = getUsageColor(mount.usedPercent.toInt())
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${mount.device} (${mount.filesystem})",
                                        fontSize = 12.sp,
                                        color = MiTheme.TextTertiary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    StorageProgressBar(
                                        used = mount.used,
                                        total = mount.total,
                                        color = getUsageColor(mount.usedPercent.toInt())
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = Loc.s(R.string.s0735, formatBytes(mount.total)),
                                            fontSize = 12.sp,
                                            color = MiTheme.TextTertiary
                                        )
                                        Text(
                                            text = Loc.s(R.string.s0736, formatBytes(mount.used)),
                                            fontSize = 12.sp,
                                            color = MiTheme.TextTertiary
                                        )
                                        Text(
                                            text = Loc.s(R.string.s0737, formatBytes(mount.free)),
                                            fontSize = 12.sp,
                                            color = MiTheme.TextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageProgressBar(
    used: Long,
    total: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    val percent = if (total > 0) (used * 1f / total).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MiTheme.Divider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

@Composable
private fun StorageStatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MiTheme.TextTertiary
        )
    }
}

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

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> Loc.s(R.string.s0738, days, hours, minutes)
        hours > 0 -> Loc.s(R.string.s0739, hours, minutes)
        else -> Loc.s(R.string.s0523, minutes)
    }
}

private fun getUsageColor(percent: Int): Color {
    return when {
        percent >= 90 -> MiColors.Error
        percent >= 70 -> MiColors.Warning
        else -> MiColors.Success
    }
}
