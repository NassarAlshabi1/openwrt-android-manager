package com.luanmuc.openwrtmanager.ui.wifi_enhanced
import com.luanmuc.openwrtmanager.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
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
import com.luanmuc.openwrtmanager.data.model.WifiBand
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc

/**
 * WiFi增强页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiEnhancedScreen(
    onBack: () -> Unit,
    viewModel: WifiEnhancedViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val wifiSchedules by viewModel.wifiSchedules.collectAsState()
    val guestConfig by viewModel.guestConfig.collectAsState()
    val channelInfo by viewModel.channelInfo.collectAsState()
    val isScanningChannels by viewModel.isScanningChannels.collectAsState()
    val selectedBand by viewModel.selectedBand.collectAsState()
    val txPower by viewModel.txPower.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setting_wifi_enhanced)) },
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
            // WiFi定时开关
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                SectionTitle(
                    title = Loc.s(R.string.s0808),
                    icon = Icons.Default.Schedule
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
                items(wifiSchedules) { schedule ->
                    ScheduleItemCard(
                        schedule = schedule,
                        onToggle = { enabled -> viewModel.toggleSchedule(schedule.id, enabled) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 访客网络
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                SectionTitle(
                    title = Loc.s(R.string.s0464),
                    icon = Icons.Default.Smartphone
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MiTheme.Primary)
                    }
                } else if (guestConfig != null) {
                    guestConfig?.let { config ->
                    GuestNetworkCard(
                        config = config,
                        onToggle = { enabled -> viewModel.toggleGuestNetwork(enabled) }
                    )
                    }
                }
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 信道分析
            item {
                SectionTitle(
                    title = Loc.s(R.string.s0809),
                    icon = Icons.Default.SignalWifi4Bar
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                // 频段选择
                BandSelector(
                    selectedBand = selectedBand,
                    onBandSelected = { viewModel.setSelectedBand(it) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 扫描按钮
                Button(
                    onClick = { viewModel.scanChannels() },
                    enabled = !isScanningChannels,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MiTheme.Primary
                    ),
                    shape = RoundedCornerShape(MiDimens.buttonRadius)
                ) {
                    if (isScanningChannels) {
                        CircularProgressIndicator(
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.status_scanning))
                    } else {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.common_scan_channel))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (channelInfo.isNotEmpty()) {
                items(channelInfo.take(5)) { channel ->
                    ChannelItemCard(channel = channel)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 发射功率调节
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                SectionTitle(
                    title = Loc.s(R.string.s0810),
                    icon = Icons.Default.SignalWifi4Bar
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                TxPowerCard(
                    power = txPower,
                    onPowerChanged = { viewModel.setTxPower(it) }
                )
                
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
 * 定时项卡片
 */
@Composable
private fun ScheduleItemCard(
    schedule: com.luanmuc.openwrtmanager.data.model.WifiSchedule,
    onToggle: (Boolean) -> Unit
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (schedule.enabled) MiTheme.Primary.copy(alpha = 0.1f)
                        else MiTheme.Divider
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = schedule.name,
                    tint = if (schedule.enabled) MiTheme.Primary else MiTheme.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 定时信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.name,
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime} · ${schedule.action.displayName}",
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 开关
            Switch(
                checked = schedule.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MiTheme.Primary,
                    checkedTrackColor = MiTheme.Primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * 访客网络卡片
 */
@Composable
private fun GuestNetworkCard(
    config: com.luanmuc.openwrtmanager.data.model.GuestNetworkConfig,
    onToggle: (Boolean) -> Unit
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
                // 图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (config.enabled) MiTheme.Primary.copy(alpha = 0.1f)
                            else MiTheme.Divider
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Smartphone,
                        contentDescription = Loc.s(R.string.s0464),
                        tint = if (config.enabled) MiTheme.Primary else MiTheme.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Loc.s(R.string.s0464),
                        color = MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SSID: ${config.ssid}",
                        color = MiTheme.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // 开关
                Switch(
                    checked = config.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MiTheme.Primary,
                        checkedTrackColor = MiTheme.Primary.copy(alpha = 0.5f)
                    )
                )
            }
            
            if (config.enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MiTheme.Divider)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Loc.s(R.string.s0811),
                        color = MiTheme.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = Loc.s(R.string.s0812, config.maxClients),
                        color = MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Loc.s(R.string.s0813),
                        color = MiTheme.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (config.isolated) Loc.s(R.string.s0463) else Loc.s(R.string.s0465),
                        color = if (config.isolated) MiTheme.Success else MiTheme.TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 频段选择器
 */
@Composable
private fun BandSelector(
    selectedBand: WifiBand,
    onBandSelected: (WifiBand) -> Unit
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
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WifiBand.values().forEach { band ->
                val isSelected = band == selectedBand
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(MiDimens.smallRadius))
                        .background(
                            if (isSelected) MiTheme.Primary else MiTheme.CardBackground
                        )
                        .clickable { onBandSelected(band) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = band.displayName,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 信道项卡片
 */
@Composable
private fun ChannelItemCard(
    channel: com.luanmuc.openwrtmanager.data.model.WifiChannelInfo
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
            // 信道号
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(MiDimens.iconRadius))
                    .background(MiTheme.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${channel.channel}",
                    color = MiTheme.Primary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信道信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Loc.s(R.string.s0814, channel.channel),
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Loc.s(R.string.s0815, channel.frequency, channel.utilization),
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 活跃站点数
            Text(
                text = Loc.s(R.string.s0812, channel.activeStations),
                color = MiTheme.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 发射功率卡片
 */
@Composable
private fun TxPowerCard(
    power: Int,
    onPowerChanged: (Int) -> Unit
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
                // 图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MiTheme.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SignalWifi4Bar,
                        contentDescription = Loc.s(R.string.s0810),
                        tint = MiTheme.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Loc.s(R.string.s0810),
                        color = MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$power dBm",
                        color = MiTheme.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 滑块
            Slider(
                value = power.toFloat(),
                onValueChange = { onPowerChanged(it.toInt()) },
                valueRange = 0f..30f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = MiTheme.Primary,
                    activeTrackColor = MiTheme.Primary
                )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0 dBm",
                    color = MiTheme.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "30 dBm",
                    color = MiTheme.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
