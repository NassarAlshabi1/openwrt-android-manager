package com.luanmuc.openwrtmanager.ui.network
import com.luanmuc.openwrtmanager.util.DebounceUtils

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiLoadingState
import com.luanmuc.openwrtmanager.ui.components.MiPrimaryButton
import com.luanmuc.openwrtmanager.ui.components.MiSwitch
import com.luanmuc.openwrtmanager.ui.components.MiTextField
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.OfflineBanner
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 网络设置页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onBack: () -> Unit = {},
    viewModel: NetworkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(Loc.s(R.string.s0525), Loc.s(R.string.s0526), Loc.s(R.string.s0527))
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0512),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244),
                            tint = MiTheme.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadNetworkConfig() }) {
                        Icon(
                            Icons.Default.Refresh,
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
                        // 离线提示条
            OfflineBanner(isOffline = !viewModel.isNetworkAvailable)
            
            // Tab 切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedTab == index) {
                            androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MiColors.Primary,
                                selectedLabelColor = Color.White
                            )
                        } else {
                            androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = MiTheme.TextTertiary
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            
            if (uiState.isLoading) {
                MiLoadingState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> LanSettings(viewModel = viewModel)
                        1 -> WanSettings(viewModel = viewModel)
                        2 -> DhcpSettings(viewModel = viewModel)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 保存按钮
                    MiPrimaryButton(
                        text = Loc.s(R.string.s0528),
                        onClick = {
                            when (selectedTab) {
                                0 -> if (DebounceUtils.canClick()) viewModel.saveLanConfig()
                                1 -> if (DebounceUtils.canClick()) viewModel.saveWanConfig()
                                2 -> if (DebounceUtils.canClick()) viewModel.saveLanConfig()
                            }
                        },
                        enabled = !uiState.isSaving,
                        leadingIcon = {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                    
                    // 成功提示
                    uiState.success?.let {
                        MiCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MiColors.Success.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = it,
                                color = MiColors.Success,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    // 错误提示
                    uiState.error?.let {
                        MiCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MiColors.Error.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = it,
                                color = MiColors.Error,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun LanSettings(viewModel: NetworkViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiFeatureIcon(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    gradient = MiColors.GradientBlue,
                    size = 40.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0529),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
            }
            
            MiTextField(
                value = uiState.lanIp,
                onValueChange = { viewModel.updateLanIp(it) },
                label = Loc.s(R.string.s0530),
                placeholder = "192.168.1.1"
            )
            
            MiTextField(
                value = uiState.lanNetmask,
                onValueChange = { viewModel.updateLanNetmask(it) },
                label = Loc.s(R.string.s0531),
                placeholder = "255.255.255.0"
            )
        }
    }
}

@Composable
fun WanSettings(viewModel: NetworkViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiFeatureIcon(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    gradient = MiColors.GradientGreen,
                    size = 40.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0532),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
            }
            
            // 协议选择
            Text(
                text = Loc.s(R.string.s0533),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MiTheme.TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("dhcp" to "DHCP", "static" to Loc.s(R.string.s0534), "pppoe" to "PPPoE").forEach { (proto, label) ->
                    FilterChip(
                        selected = uiState.wanProto == proto,
                        onClick = { viewModel.updateWanProto(proto) },
                        label = {
                            Text(label, fontSize = 13.sp)
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (uiState.wanProto == proto) {
                            androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MiColors.Primary,
                                selectedLabelColor = Color.White
                            )
                        } else {
                            androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                containerColor = MiTheme.CardBackground,
                                labelColor = MiTheme.TextTertiary
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            
            if (uiState.wanProto == "static") {
                MiTextField(
                    value = uiState.wanIp,
                    onValueChange = { viewModel.updateWanIp(it) },
                    label = Loc.s(R.string.s0530),
                    placeholder = "192.168.1.100"
                )
                MiTextField(
                    value = uiState.wanNetmask,
                    onValueChange = { viewModel.updateWanNetmask(it) },
                    label = Loc.s(R.string.s0531),
                    placeholder = "255.255.255.0"
                )
                MiTextField(
                    value = uiState.wanGateway,
                    onValueChange = { viewModel.updateWanGateway(it) },
                    label = Loc.s(R.string.s0535),
                    placeholder = "192.168.1.1"
                )
                MiTextField(
                    value = uiState.wanDns,
                    onValueChange = { viewModel.updateWanDns(it) },
                    label = Loc.s(R.string.s0536),
                    placeholder = "8.8.8.8"
                )
            }
            
            if (uiState.wanProto == "pppoe") {
                MiTextField(
                    value = uiState.wanUsername,
                    onValueChange = { viewModel.updateWanUsername(it) },
                    label = Loc.s(R.string.s0537),
                    placeholder = Loc.s(R.string.s0538)
                )
                MiTextField(
                    value = uiState.wanPassword,
                    onValueChange = { viewModel.updateWanPassword(it) },
                    label = Loc.s(R.string.s0539),
                    placeholder = Loc.s(R.string.s0540)
                )
            }
        }
    }
}

@Composable
fun DhcpSettings(viewModel: NetworkViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiFeatureIcon(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    gradient = MiColors.GradientOrange,
                    size = 40.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Loc.s(R.string.s0541),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiTheme.TextPrimary
                    )
                    Text(
                        text = Loc.s(R.string.s0542),
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary
                    )
                }
                MiSwitch(
                    checked = uiState.lanDhcpEnabled,
                    onCheckedChange = { viewModel.updateLanDhcpEnabled(it) }
                )
            }
            
            if (uiState.lanDhcpEnabled) {
                MiTextField(
                    value = uiState.lanDhcpStart,
                    onValueChange = { viewModel.updateLanDhcpStart(it) },
                    label = Loc.s(R.string.s0543),
                    placeholder = "100"
                )
                MiTextField(
                    value = uiState.lanDhcpLimit,
                    onValueChange = { viewModel.updateLanDhcpLimit(it) },
                    label = Loc.s(R.string.s0544),
                    placeholder = "150"
                )
                MiTextField(
                    value = uiState.lanDhcpLease,
                    onValueChange = { viewModel.updateLanDhcpLease(it) },
                    label = Loc.s(R.string.s0545),
                    placeholder = "12h"
                )
            }
        }
    }
}
