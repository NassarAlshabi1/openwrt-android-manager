package com.luanmuc.openwrtmanager.ui.widget_config
import com.luanmuc.openwrtmanager.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 桌面小部件配置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    onBack: () -> Unit,
    viewModel: WidgetConfigViewModel = viewModel()
) {
    val routerStatusWidgetEnabled by viewModel.routerStatusWidgetEnabled.collectAsState()
    val networkSpeedWidgetEnabled by viewModel.networkSpeedWidgetEnabled.collectAsState()
    val updateInterval by viewModel.updateInterval.collectAsState()
    val showRouterName by viewModel.showRouterName.collectAsState()
    val showOnlineStatus by viewModel.showOnlineStatus.collectAsState()
    val showDeviceCount by viewModel.showDeviceCount.collectAsState()
    val showSpeed by viewModel.showSpeed.collectAsState()
    val widgetTheme by viewModel.widgetTheme.collectAsState()
    val widgetOpacity by viewModel.widgetOpacity.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setting_widget)) },
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
            // 小部件列表
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                SectionTitle(title = Loc.s(R.string.s0776), icon = Icons.Default.Widgets)
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                // 路由器状态小部件
                WidgetSwitchItem(
                    title = Loc.s(R.string.s0131),
                    description = Loc.s(R.string.s0777),
                    icon = Icons.Default.Devices,
                    checked = routerStatusWidgetEnabled,
                    onCheckedChange = { viewModel.setRouterStatusWidgetEnabled(it) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 网速小部件
                WidgetSwitchItem(
                    title = Loc.s(R.string.s0778),
                    description = Loc.s(R.string.s0779),
                    icon = Icons.Default.NetworkCheck,
                    checked = networkSpeedWidgetEnabled,
                    onCheckedChange = { viewModel.setNetworkSpeedWidgetEnabled(it) }
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 更新设置
            item {
                SectionTitle(title = Loc.s(R.string.s0780), icon = Icons.Default.Widgets)
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
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
                            text = Loc.s(R.string.s0781),
                            color = MiTheme.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Loc.s(R.string.s0782),
                            color = MiTheme.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 更新间隔选项
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.updateIntervalOptions.forEach { interval ->
                                FilterChip(
                                    selected = updateInterval == interval,
                                    onClick = { viewModel.setUpdateInterval(interval) },
                                    label = {
                                        Text(
                                            text = viewModel.formatUpdateInterval(interval),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MiTheme.Primary,
                                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                    )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 显示内容设置
            item {
                SectionTitle(title = Loc.s(R.string.s0783), icon = Icons.Default.Widgets)
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
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
                        SwitchSettingItem(
                            title = Loc.s(R.string.s0784),
                            checked = showRouterName,
                            onCheckedChange = { viewModel.setShowRouterName(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MiTheme.Divider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SwitchSettingItem(
                            title = Loc.s(R.string.s0785),
                            checked = showOnlineStatus,
                            onCheckedChange = { viewModel.setShowOnlineStatus(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MiTheme.Divider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SwitchSettingItem(
                            title = Loc.s(R.string.s0786),
                            checked = showDeviceCount,
                            onCheckedChange = { viewModel.setShowDeviceCount(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MiTheme.Divider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SwitchSettingItem(
                            title = Loc.s(R.string.s0787),
                            checked = showSpeed,
                            onCheckedChange = { viewModel.setShowSpeed(it) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 外观设置
            item {
                SectionTitle(title = Loc.s(R.string.s0788), icon = Icons.Default.Widgets)
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
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
                            text = Loc.s(R.string.s0789),
                            color = MiTheme.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 主题选项
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.themeOptions.forEach { (value, label) ->
                                FilterChip(
                                    selected = widgetTheme == value,
                                    onClick = { viewModel.setWidgetTheme(value) },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MiTheme.Primary,
                                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = Loc.s(R.string.s0790),
                            color = MiTheme.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$widgetOpacity%",
                            color = MiTheme.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = widgetOpacity.toFloat(),
                            onValueChange = { viewModel.setWidgetOpacity(it.toInt()) },
                            valueRange = 50f..100f,
                            steps = 49,
                            colors = SliderDefaults.colors(
                                thumbColor = MiTheme.Primary,
                                activeTrackColor = MiTheme.Primary
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 使用说明
            item {
                SectionTitle(title = Loc.s(R.string.s0791), icon = Icons.Default.Widgets)
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
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
                            text = Loc.s(R.string.s0792),
                            color = MiTheme.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = Loc.s(R.string.s0793),
                            color = MiTheme.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = Loc.s(R.string.s0794),
                            color = MiTheme.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = Loc.s(R.string.s0795),
                            color = MiTheme.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp
                        )
                    }
                }
                
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
 * 小部件开关项
 */
@Composable
private fun WidgetSwitchItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
                    .clip(RoundedCornerShape(MiDimens.iconRadius))
                    .background(MiTheme.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 开关
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MiTheme.Primary,
                    checkedTrackColor = MiTheme.Primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * 开关设置项
 */
@Composable
private fun SwitchSettingItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MiTheme.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MiTheme.Primary,
                checkedTrackColor = MiTheme.Primary.copy(alpha = 0.5f)
            )
        )
    }
}
