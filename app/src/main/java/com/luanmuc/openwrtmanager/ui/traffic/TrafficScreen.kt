package com.luanmuc.openwrtmanager.ui.traffic

import com.luanmuc.openwrtmanager.R

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
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
import com.luanmuc.openwrtmanager.data.model.TrafficPeriod
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 流量统计页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficScreen(
    onBack: () -> Unit,
    viewModel: TrafficViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val trafficStats by viewModel.trafficStats.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val deviceRanking by viewModel.deviceRanking.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.traffic_title)) },
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
            // 周期切换
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { viewModel.setSelectedPeriod(it) }
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 流量概览卡片
            item {
                TrafficOverviewCard(
                    stats = trafficStats,
                    period = selectedPeriod,
                    viewModel = viewModel
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 上下行流量详情
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MiDimens.itemSpacing)
                ) {
                    // 下载流量
                    TrafficDetailCard(
                        title = Loc.s(R.string.s0505),
                        icon = Icons.Default.Download,
                        total = when (selectedPeriod) {
                            TrafficPeriod.TODAY -> trafficStats.todayRx
                            TrafficPeriod.WEEK -> trafficStats.weekRx
                            TrafficPeriod.MONTH -> trafficStats.monthRx
                            TrafficPeriod.YEAR -> trafficStats.totalRx
                        },
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 上传流量
                    TrafficDetailCard(
                        title = Loc.s(R.string.s0506),
                        icon = Icons.Default.Upload,
                        total = when (selectedPeriod) {
                            TrafficPeriod.TODAY -> trafficStats.todayTx
                            TrafficPeriod.WEEK -> trafficStats.weekTx
                            TrafficPeriod.MONTH -> trafficStats.monthTx
                            TrafficPeriod.YEAR -> trafficStats.totalTx
                        },
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 设备流量排行标题
            item {
                Text(
                    text = Loc.s(R.string.s0772),
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // 设备流量排行列表
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MiTheme.Primary)
                    }
                }
            } else if (deviceRanking.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Loc.s(R.string.s0773),
                            color = MiTheme.TextTertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(deviceRanking.take(10)) { device ->
                    DeviceTrafficItem(
                        device = device,
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
 * 周期选择器
 */
@Composable
private fun PeriodSelector(
    selectedPeriod: TrafficPeriod,
    onPeriodSelected: (TrafficPeriod) -> Unit
) {
    val periods = TrafficPeriod.values().take(3) // 只显示今日、本周、本月
    
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
                .padding(4.dp)
        ) {
            periods.forEach { period ->
                val isSelected = period == selectedPeriod
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(MiDimens.smallRadius))
                        .background(
                            if (isSelected) MiTheme.Primary else MiTheme.CardBackground
                        )
                        .clickable { onPeriodSelected(period) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.displayName,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 流量概览卡片
 */
@Composable
private fun TrafficOverviewCard(
    stats: com.luanmuc.openwrtmanager.data.model.TrafficStats,
    period: TrafficPeriod,
    viewModel: TrafficViewModel
) {
    val total = when (period) {
        TrafficPeriod.TODAY -> stats.todayRx + stats.todayTx
        TrafficPeriod.WEEK -> stats.weekRx + stats.weekTx
        TrafficPeriod.MONTH -> stats.monthRx + stats.monthTx
        TrafficPeriod.YEAR -> stats.totalRx + stats.totalTx
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.Primary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiDimens.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Loc.s(R.string.s0774, period.displayName),
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewModel.formatBytes(total),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.s(R.string.s0505),
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.formatBytes(
                            when (period) {
                                TrafficPeriod.TODAY -> stats.todayRx
                                TrafficPeriod.WEEK -> stats.weekRx
                                TrafficPeriod.MONTH -> stats.monthRx
                                TrafficPeriod.YEAR -> stats.totalRx
                            }
                        ),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.s(R.string.s0506),
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.formatBytes(
                            when (period) {
                                TrafficPeriod.TODAY -> stats.todayTx
                                TrafficPeriod.WEEK -> stats.weekTx
                                TrafficPeriod.MONTH -> stats.monthTx
                                TrafficPeriod.YEAR -> stats.totalTx
                            }
                        ),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 流量详情卡片
 */
@Composable
private fun TrafficDetailCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    total: Long,
    viewModel: TrafficViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                    icon,
                    contentDescription = title,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = viewModel.formatBytes(total),
                color = MiTheme.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 设备流量项
 */
@Composable
private fun DeviceTrafficItem(
    device: com.luanmuc.openwrtmanager.data.model.DeviceTraffic,
    viewModel: TrafficViewModel
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
            // 设备名称
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.hostname.ifEmpty { device.ip },
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.ip,
                    color = MiTheme.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 流量数据
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "↓ ${viewModel.formatBytes(device.rxBytes)}",
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "↑ ${viewModel.formatBytes(device.txBytes)}",
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
