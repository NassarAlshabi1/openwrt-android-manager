package com.luanmuc.openwrtmanager.ui.wifi
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
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
 * WiFi设置页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScreen(
    onBack: () -> Unit = {},
    viewModel: WifiViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (uiState.has5g) {
        listOf("2.4G", "5G", Loc.s(R.string.s0464))
    } else {
        listOf("2.4G", Loc.s(R.string.s0464))
    }
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0799),
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
                    IconButton(onClick = { viewModel.loadWifiConfig() }) {
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
                    when {
                        selectedTab == 0 -> WifiSettingsCard(
                            title = "2.4G WiFi",
                            gradient = MiColors.GradientOrange,
                            config = uiState.wifi2g,
                            onConfigChange = { viewModel.updateWifi2g(it) },
                            onSave = { if (DebounceUtils.canClick()) viewModel.saveWifiConfig("2g") },
                            isSaving = uiState.isSaving
                        )
                        selectedTab == 1 && uiState.has5g -> WifiSettingsCard(
                            title = "5G WiFi",
                            gradient = MiColors.GradientBlue,
                            config = uiState.wifi5g,
                            onConfigChange = { viewModel.updateWifi5g(it) },
                            onSave = { if (DebounceUtils.canClick()) viewModel.saveWifiConfig("5g") },
                            isSaving = uiState.isSaving
                        )
                        else -> WifiSettingsCard(
                            title = Loc.s(R.string.s0464),
                            gradient = MiColors.GradientGreen,
                            config = uiState.guestWifi,
                            onConfigChange = { viewModel.updateGuestWifi(it) },
                            onSave = { if (DebounceUtils.canClick()) viewModel.saveWifiConfig("guest") },
                            isSaving = uiState.isSaving
                        )
                    }
                    
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
fun WifiSettingsCard(
    title: String,
    gradient: androidx.compose.ui.graphics.Brush,
    config: WifiViewModel.WifiConfig,
    onConfigChange: (WifiViewModel.WifiConfig) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean
) {
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
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    gradient = gradient,
                    size = 40.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiTheme.TextPrimary
                    )
                    Text(
                        text = if (config.enabled) Loc.s(R.string.s0463) else Loc.s(R.string.s0465),
                        fontSize = 12.sp,
                        color = if (config.enabled) MiColors.Success else MiTheme.TextTertiary
                    )
                }
                MiSwitch(
                    checked = config.enabled,
                    onCheckedChange = { onConfigChange(config.copy(enabled = it)) }
                )
            }
            
            if (config.enabled) {
                MiTextField(
                    value = config.ssid,
                    onValueChange = { onConfigChange(config.copy(ssid = it)) },
                    label = Loc.s(R.string.s0800),
                    placeholder = Loc.s(R.string.s0801)
                )
                
                MiTextField(
                    value = config.password,
                    onValueChange = { onConfigChange(config.copy(password = it)) },
                    label = Loc.s(R.string.s0802),
                    placeholder = Loc.s(R.string.s0803)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiTextField(
                        value = config.channel,
                        onValueChange = { onConfigChange(config.copy(channel = it)) },
                        label = Loc.s(R.string.s0804),
                        placeholder = "auto",
                        modifier = Modifier.weight(1f)
                    )
                    MiTextField(
                        value = config.txpower,
                        onValueChange = { onConfigChange(config.copy(txpower = it)) },
                        label = Loc.s(R.string.s0805),
                        placeholder = "20",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                MiPrimaryButton(
                    text = Loc.s(R.string.s0806),
                    onClick = onSave,
                    enabled = !isSaving,
                    leadingIcon = {
                        if (isSaving) {
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
            }
        }
    }
}
