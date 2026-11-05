package com.luanmuc.openwrtmanager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luanmuc.openwrtmanager.data.model.DeviceInfo
import com.luanmuc.openwrtmanager.data.model.PortStatus
import com.luanmuc.openwrtmanager.data.model.RouterStatus
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 在线设备卡片
 */
@Composable
fun OnlineDevicesCard(
    uiState: HomeViewModel.HomeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiColors.Success.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = MiColors.Success,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Loc.s(R.string.s0076),
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                        Text(
                            text = Loc.s(R.string.s0460, uiState.onlineDevices.size),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MiTheme.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 设备列表预览（前3个）
            uiState.onlineDevices.take(3).forEach { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = device.hostname.ifEmpty { device.ip },
                        fontSize = 13.sp,
                        color = MiTheme.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = device.ip,
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary
                    )
                }
            }
            
            if (uiState.onlineDevices.size > 3) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Loc.s(R.string.s0461, uiState.onlineDevices.size - 3),
                    fontSize = 12.sp,
                    color = MiTheme.TextTertiary
                )
            }
        }
    }
}

/**
 * WiFi状态卡片
 */
@Composable
fun WifiStatusCard(
    uiState: HomeViewModel.HomeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiColors.Cyan.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MiColors.Cyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Loc.s(R.string.s0462),
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                        Text(
                            text = "2.4G + 5G",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MiTheme.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "2.4G",
                        fontSize = 12.sp,
                        color = MiTheme.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Loc.s(R.string.s0463),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiColors.Success
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "5G",
                        fontSize = 12.sp,
                        color = MiTheme.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Loc.s(R.string.s0463),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiColors.Success
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.s(R.string.s0464),
                        fontSize = 12.sp,
                        color = MiTheme.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Loc.s(R.string.s0465),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiTheme.TextTertiary
                    )
                }
            }
        }
    }
}

/**
 * 网口状态卡片
 */
@Composable
fun PortStatusCard(
    ports: List<PortStatus>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MiColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Loc.s(R.string.s0466),
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                        Text(
                            text = Loc.s(R.string.s0467, ports.count { it.isConnected }, ports.size),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MiTheme.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 网口列表
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ports.take(4).forEach { port ->
                    PortItem(port = port)
                }
                if (ports.size > 4) {
                    Text(
                        text = Loc.s(R.string.s0468, ports.size - 4),
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单个网口项
 */
@Composable
private fun PortItem(
    port: PortStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 状态指示灯
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (port.isConnected) MiColors.Success else MiTheme.TextTertiary
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        
        // 网口名称
        Text(
            text = port.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiTheme.TextPrimary,
            modifier = Modifier.width(60.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 连接状态/速率
        Text(
            text = if (port.isConnected) {
                if (port.speed > 0) "${port.speed}M" else Loc.s(R.string.s0469)
            } else {
                Loc.s(R.string.s0470)
            },
            fontSize = 13.sp,
            color = if (port.isConnected) MiColors.Success else MiTheme.TextTertiary,
            modifier = Modifier.weight(1f)
        )
        
        // 流量
        if (port.isConnected && (port.rxBytes > 0 || port.txBytes > 0)) {
            Text(
                text = formatPortBytes(port.rxBytes + port.txBytes),
                fontSize = 12.sp,
                color = MiTheme.TextSecondary
            )
        }
    }
}

/**
 * 格式化网口流量
 */
private fun formatPortBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.1fGB", bytes / 1024f / 1024f / 1024f)
        bytes >= 1024 * 1024 -> String.format("%.1fMB", bytes / 1024f / 1024f)
        bytes >= 1024 -> String.format("%.1fKB", bytes / 1024f)
        else -> "${bytes}B"
    }
}

/**
 * 插件卡片
 */
@Composable
fun PluginsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiColors.Purple.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = MiColors.Purple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Loc.s(R.string.s0471),
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                        Text(
                            text = Loc.s(R.string.s0472),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MiTheme.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 防火墙卡片
 */
@Composable
fun FirewallCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiColors.Warning.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MiColors.Warning,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Loc.s(R.string.s0409),
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                        Text(
                            text = Loc.s(R.string.s0473),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MiTheme.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * DDNS卡片
 */
@Composable
fun DdnsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiColors.Orange.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MiColors.Orange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DDNS",
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                        Text(
                            text = Loc.s(R.string.s0474),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MiTheme.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
