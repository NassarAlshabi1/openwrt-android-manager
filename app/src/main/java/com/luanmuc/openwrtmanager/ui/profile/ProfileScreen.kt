package com.luanmuc.openwrtmanager.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.luanmuc.openwrtmanager.util.Loc
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.theme.ThemeManager
import com.luanmuc.openwrtmanager.ui.theme.ThemeMode
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiListItem
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.MiSwitch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.luanmuc.openwrtmanager.data.repository.CacheRepository
import com.luanmuc.openwrtmanager.data.repository.SettingsRepository
import com.luanmuc.openwrtmanager.data.repository.Language
import com.luanmuc.openwrtmanager.data.repository.TrafficUnit
import com.luanmuc.openwrtmanager.data.repository.RefreshInterval
import com.luanmuc.openwrtmanager.data.repository.ConnectionTimeout
import com.luanmuc.openwrtmanager.data.repository.RetryCount

/**
 * 我的页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSystem: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToWifi: () -> Unit = {},
    onNavigateToFirewall: () -> Unit = {},
    onNavigateToDdns: () -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToTraffic: () -> Unit = {},
    onNavigateToDeviceManager: () -> Unit = {},
    onNavigateToDiagnosticEnhanced: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToWifiEnhanced: () -> Unit = {},
    onNavigateToTerminal: () -> Unit = {},
    onNavigateToWidgetConfig: () -> Unit = {},
    onNavigateToPlugins: () -> Unit = {},
    onNavigateToRemoteManage: () -> Unit = {},
    onNavigateToPluginDependency: () -> Unit = {},
    onNavigateToAiDiagnostic: () -> Unit = {},
    onNavigateToDiagnosticCommands: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = stringResource(R.string.profile_title)
            )
        },
        containerColor = MiTheme.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            UserInfoCard()
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 功能工具
            SectionTitle(title = Loc.s(R.string.s0624))
            ToolsGrid(
                onNavigateToSystem = onNavigateToSystem,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToWifi = onNavigateToWifi,
                onNavigateToFirewall = onNavigateToFirewall,
                onNavigateToDdns = onNavigateToDdns,
                onNavigateToAdvanced = onNavigateToAdvanced,
                onNavigateToNotification = onNavigateToNotification,
                onNavigateToTraffic = onNavigateToTraffic,
                onNavigateToDeviceManager = onNavigateToDeviceManager,
                onNavigateToDiagnosticEnhanced = onNavigateToDiagnosticEnhanced,
                onNavigateToBackup = onNavigateToBackup,
                onNavigateToWifiEnhanced = onNavigateToWifiEnhanced,
                onNavigateToTerminal = onNavigateToTerminal,
                onNavigateToWidgetConfig = onNavigateToWidgetConfig,
                onNavigateToPlugins = onNavigateToPlugins,
                onNavigateToRemoteManage = onNavigateToRemoteManage,
                onNavigateToPluginDependency = onNavigateToPluginDependency,
                onNavigateToAiDiagnostic = onNavigateToAiDiagnostic,
                onNavigateToDiagnosticCommands = onNavigateToDiagnosticCommands
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 设置列表
            SectionTitle(title = stringResource(R.string.profile_settings))
            SettingsList()
            
            Spacer(modifier = Modifier.height(20.dp))
            
            AboutCard()
        }
        
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MiTheme.TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ToolsGrid(
    onNavigateToSystem: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToWifi: () -> Unit,
    onNavigateToFirewall: () -> Unit,
    onNavigateToDdns: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToTraffic: () -> Unit,
    onNavigateToDeviceManager: () -> Unit,
    onNavigateToDiagnosticEnhanced: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToWifiEnhanced: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToWidgetConfig: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToRemoteManage: () -> Unit,
    onNavigateToPluginDependency: () -> Unit,
    onNavigateToAiDiagnostic: () -> Unit,
    onNavigateToDiagnosticCommands: () -> Unit
) {
    MiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolItem(
                    icon = Icons.Default.BugReport,
                    title = Loc.s(R.string.s0518),
                    gradient = MiColors.GradientBlue,
                    onClick = onNavigateToSystem
                )
                ToolItem(
                    icon = Icons.Default.SignalCellularAlt,
                    title = Loc.s(R.string.s0512),
                    gradient = MiColors.GradientGreen,
                    onClick = onNavigateToNetwork
                )
                ToolItem(
                    icon = Icons.Default.Wifi,
                    title = Loc.s(R.string.s0511),
                    gradient = MiColors.GradientOrange,
                    onClick = onNavigateToWifi
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolItem(
                    icon = Icons.Default.Security,
                    title = Loc.s(R.string.s0409),
                    gradient = MiColors.GradientRed,
                    onClick = onNavigateToFirewall
                )
                ToolItem(
                    icon = Icons.Default.Dns,
                    title = "DDNS",
                    gradient = MiColors.GradientCyan,
                    onClick = onNavigateToDdns
                )
                ToolItem(
                    icon = Icons.Default.Memory,
                    title = Loc.s(R.string.s0261),
                    gradient = MiColors.GradientPurple,
                    onClick = onNavigateToAdvanced
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolItem(
                    icon = Icons.Default.AutoAwesome,
                    title = Loc.s(R.string.ai_tool_title),
                    gradient = MiColors.GradientPurple,
                    onClick = onNavigateToAiDiagnostic
                )
                ToolItem(
                    icon = Icons.Default.BugReport,
                    title = Loc.s(R.string.s0368),
                    gradient = MiColors.GradientBlue,
                    onClick = onNavigateToDiagnosticEnhanced
                )
                ToolItem(
                    icon = Icons.Default.Speed,
                    title = Loc.s(R.string.s0179),
                    gradient = MiColors.GradientGreen,
                    onClick = onNavigateToTraffic
                )
                ToolItem(
                    icon = Icons.Default.Devices,
                    title = Loc.s(R.string.s0510),
                    gradient = MiColors.GradientOrange,
                    onClick = onNavigateToDeviceManager
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolItem(
                    icon = Icons.Default.Settings,
                    title = Loc.s(R.string.s0625),
                    gradient = MiColors.GradientCyan,
                    onClick = onNavigateToBackup
                )
                ToolItem(
                    icon = Icons.Default.Wifi,
                    title = Loc.s(R.string.s0626),
                    gradient = MiColors.GradientGreen,
                    onClick = onNavigateToWifiEnhanced
                )
                ToolItem(
                    icon = Icons.Default.Code,
                    title = Loc.s(R.string.s0627),
                    gradient = MiColors.GradientPurple,
                    onClick = onNavigateToTerminal
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolItem(
                    icon = Icons.Default.Notifications,
                    title = Loc.s(R.string.s0628),
                    gradient = MiColors.GradientOrange,
                    onClick = onNavigateToNotification
                )
                ToolItem(
                    icon = Icons.Default.Apps,
                    title = Loc.s(R.string.s0629),
                    gradient = MiColors.GradientBlue,
                    onClick = onNavigateToWidgetConfig
                )
                ToolItem(
                    icon = Icons.Default.Extension,
                    title = Loc.s(R.string.s0630),
                    gradient = MiColors.GradientRed,
                    onClick = onNavigateToPlugins
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolItem(
                    icon = Icons.Default.Wifi,
                    title = Loc.s(R.string.s0631),
                    gradient = MiColors.GradientBlue,
                    onClick = onNavigateToRemoteManage
                )
                ToolItem(
                    icon = Icons.Default.Extension,
                    title = Loc.s(R.string.s0632),
                    gradient = MiColors.GradientGreen,
                    onClick = onNavigateToPluginDependency
                )
                ToolItem(
                    icon = Icons.Default.Code,
                    title = Loc.s(R.string.diagcmd_title),
                    gradient = MiColors.GradientPurple,
                    onClick = onNavigateToDiagnosticCommands
                )
            }
        }
    }
}

@Composable
fun ToolItem(
    icon: ImageVector,
    title: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        MiFeatureIcon(
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            gradient = gradient,
            size = 52.dp,
            iconSize = 26.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            color = MiTheme.TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun UserInfoCard() {
    MiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiFeatureIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                },
                gradient = MiColors.GradientBlue,
                size = 56.dp,
                iconSize = 28.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Loc.s(R.string.s0633),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Loc.s(R.string.s0634),
                    fontSize = 13.sp,
                    color = MiTheme.TextTertiary
                )
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

@Composable
fun SettingsList() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = SettingsRepository.getInstance(context)
    
    // 状态
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRefreshIntervalDialog by remember { mutableStateOf(false) }
    var showConnectionTimeoutDialog by remember { mutableStateOf(false) }
    var showRetryCountDialog by remember { mutableStateOf(false) }
    var showTrafficUnitDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(Loc.s(R.string.s0635)) }
    
    // 监听设置变化（简化版，实际应该用Flow）
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var language by remember { mutableStateOf(settings.language) }
    var autoRefreshEnabled by remember { mutableStateOf(settings.autoRefreshEnabled) }
    var refreshInterval by remember { mutableStateOf(settings.refreshInterval) }
    var connectionTimeout by remember { mutableStateOf(settings.connectionTimeout) }
    var retryCount by remember { mutableStateOf(settings.retryCount) }
    var httpsAutoDetect by remember { mutableStateOf(settings.httpsAutoDetect) }
    var pushNotificationEnabled by remember { mutableStateOf(settings.pushNotificationEnabled) }
    var offlineAlertEnabled by remember { mutableStateOf(settings.offlineAlertEnabled) }
    var trafficUnit by remember { mutableStateOf(settings.trafficUnit) }
    var statusBarEnabled by remember { mutableStateOf(settings.statusBarEnabled) }
    
    // 计算缓存大小
    LaunchedEffect(Unit) {
        try {
            val cacheStats = CacheRepository.getInstance(context).getCacheStats()
            cacheSize = formatSize(cacheStats.totalSizeBytes)
        } catch (e: Exception) {
            cacheSize = "0 B"
        }
    }
    
    Column {
        // 通用设置
        Text(
            text = Loc.s(R.string.s0636),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiTheme.TextSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        
        MiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                // 主题设置
                MiListItem(
                    title = Loc.s(R.string.s0637),
                    subtitle = themeMode.displayName,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MiTheme.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = { showThemeDialog = true }
                )
                MiDivider(indent = 60.dp)
                
                // 语言设置
                MiListItem(
                    title = Loc.s(R.string.s0638),
                    subtitle = language.displayName,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MiTheme.Purple,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = { showLanguageDialog = true }
                )
                MiDivider(indent = 60.dp)
                
                // 清除缓存
                MiListItem(
                    title = Loc.s(R.string.s0639),
                    subtitle = cacheSize,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MiTheme.Error,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = { showClearCacheDialog = true }
                )
            }
        }
        
        // 刷新设置
        Text(
            text = Loc.s(R.string.s0322),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiTheme.TextSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        
        MiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                // 自动刷新开关
                MiListItem(
                    title = Loc.s(R.string.s0640),
                    subtitle = if (autoRefreshEnabled) Loc.s(R.string.s0463) else Loc.s(R.string.s0465),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MiTheme.Success,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailing = {
                        MiSwitch(
                            checked = autoRefreshEnabled,
                            onCheckedChange = { 
                                autoRefreshEnabled = it
                                settings.autoRefreshEnabled = it
                                Toast.makeText(context, if (it) Loc.s(R.string.s0641) else Loc.s(R.string.s0642), Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onClick = { 
                        autoRefreshEnabled = !autoRefreshEnabled
                        settings.autoRefreshEnabled = autoRefreshEnabled
                    }
                )
                MiDivider(indent = 60.dp)
                
                // 刷新间隔
                MiListItem(
                    title = Loc.s(R.string.s0643),
                    subtitle = Loc.s(R.string.s0644, refreshInterval),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MiTheme.Warning,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = { if (autoRefreshEnabled) showRefreshIntervalDialog = true }
                )
            }
        }
        
        // 连接设置
        Text(
            text = Loc.s(R.string.s0645),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiTheme.TextSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        
        MiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                // 连接超时
                MiListItem(
                    title = Loc.s(R.string.s0646),
                    subtitle = Loc.s(R.string.s0644, connectionTimeout),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MiTheme.Cyan,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = { showConnectionTimeoutDialog = true }
                )
                MiDivider(indent = 60.dp)
                
                // 重试次数
                MiListItem(
                    title = Loc.s(R.string.s0647),
                    subtitle = Loc.s(R.string.s0648, retryCount),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = MiTheme.Orange,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = { showRetryCountDialog = true }
                )
                MiDivider(indent = 60.dp)
                
                // HTTPS自动检测
                MiListItem(
                    title = Loc.s(R.string.s0649),
                    subtitle = if (httpsAutoDetect) Loc.s(R.string.s0463) else Loc.s(R.string.s0465),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MiTheme.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailing = {
                        MiSwitch(
                            checked = httpsAutoDetect,
                            onCheckedChange = { 
                                httpsAutoDetect = it
                                settings.httpsAutoDetect = it
                                Toast.makeText(context, if (it) Loc.s(R.string.s0650) else Loc.s(R.string.s0651), Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onClick = { 
                        httpsAutoDetect = !httpsAutoDetect
                        settings.httpsAutoDetect = httpsAutoDetect
                    }
                )
            }
        }
        
        // 通知设置
        Text(
            text = Loc.s(R.string.s0652),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiTheme.TextSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        
        MiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                // 推送通知
                MiListItem(
                    title = Loc.s(R.string.s0653),
                    subtitle = if (pushNotificationEnabled) Loc.s(R.string.s0463) else Loc.s(R.string.s0465),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MiTheme.Warning,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailing = {
                        MiSwitch(
                            checked = pushNotificationEnabled,
                            onCheckedChange = { 
                                pushNotificationEnabled = it
                                settings.pushNotificationEnabled = it
                                Toast.makeText(context, if (it) Loc.s(R.string.s0654) else Loc.s(R.string.s0655), Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onClick = { 
                        pushNotificationEnabled = !pushNotificationEnabled
                        settings.pushNotificationEnabled = pushNotificationEnabled
                    }
                )
                MiDivider(indent = 60.dp)
                
                // 离线提醒
                MiListItem(
                    title = Loc.s(R.string.s0656),
                    subtitle = if (offlineAlertEnabled) Loc.s(R.string.s0463) else Loc.s(R.string.s0465),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MiTheme.Error,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailing = {
                        MiSwitch(
                            checked = offlineAlertEnabled,
                            onCheckedChange = { 
                                offlineAlertEnabled = it
                                settings.offlineAlertEnabled = it
                                Toast.makeText(context, if (it) Loc.s(R.string.s0657) else Loc.s(R.string.s0658), Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onClick = { 
                        offlineAlertEnabled = !offlineAlertEnabled
                        settings.offlineAlertEnabled = offlineAlertEnabled
                    }
                )
            }
        }
        
        // 其他设置
        Text(
            text = Loc.s(R.string.s0600),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiTheme.TextSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        
        MiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                // 流量单位
                MiListItem(
                    title = Loc.s(R.string.s0659),
                    subtitle = trafficUnit.displayName,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MiTheme.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = { showTrafficUnitDialog = true }
                )
                MiDivider(indent = 60.dp)
                
                // 状态栏显示
                MiListItem(
                    title = Loc.s(R.string.s0660),
                    subtitle = if (statusBarEnabled) Loc.s(R.string.s0463) else Loc.s(R.string.s0465),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MiTheme.Success,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailing = {
                        MiSwitch(
                            checked = statusBarEnabled,
                            onCheckedChange = { 
                                statusBarEnabled = it
                                settings.statusBarEnabled = it
                                Toast.makeText(context, if (it) Loc.s(R.string.s0661) else Loc.s(R.string.s0662), Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onClick = { 
                        statusBarEnabled = !statusBarEnabled
                        settings.statusBarEnabled = statusBarEnabled
                    }
                )
            }
        }
        
        // 间距
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // 主题选择对话框
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.setting_theme)) },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    themeMode = mode
                                    settings.themeMode = mode
                                    ThemeManager.getInstance(context).setThemeMode(mode)
                                    showThemeDialog = false
                                    Toast.makeText(context, Loc.s(R.string.s0663, mode.displayName), Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    themeMode = mode
                                    settings.themeMode = mode
                                    ThemeManager.getInstance(context).setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
    
    // 语言选择对话框
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.setting_language)) },
            text = {
                Column {
                    Language.values().forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    language = lang
                                    settings.language = lang
                                    showLanguageDialog = false
                                    Loc.refresh()
                                    Toast.makeText(context, Loc.s(R.string.s0664, lang.displayName), Toast.LENGTH_SHORT).show()
                                    (context as? android.app.Activity)?.recreate()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == lang,
                                onClick = {
                                    language = lang
                                    settings.language = lang
                                    showLanguageDialog = false
                                    Loc.refresh()
                                    (context as? android.app.Activity)?.recreate()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
    
    // 刷新间隔选择对话框
    if (showRefreshIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshIntervalDialog = false },
            title = { Text(stringResource(R.string.form_refresh_interval)) },
            text = {
                Column {
                    RefreshInterval.values().forEach { interval ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    refreshInterval = interval.seconds
                                    settings.refreshInterval = interval.seconds
                                    showRefreshIntervalDialog = false
                                    Toast.makeText(context, Loc.s(R.string.s0665, interval.displayName), Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = refreshInterval == interval.seconds,
                                onClick = {
                                    refreshInterval = interval.seconds
                                    settings.refreshInterval = interval.seconds
                                    showRefreshIntervalDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(interval.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRefreshIntervalDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
    
    // 连接超时选择对话框
    if (showConnectionTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionTimeoutDialog = false },
            title = { Text(stringResource(R.string.form_connection_timeout)) },
            text = {
                Column {
                    ConnectionTimeout.values().forEach { timeout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    connectionTimeout = timeout.seconds
                                    settings.connectionTimeout = timeout.seconds
                                    showConnectionTimeoutDialog = false
                                    Toast.makeText(context, Loc.s(R.string.s0666, timeout.displayName), Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = connectionTimeout == timeout.seconds,
                                onClick = {
                                    connectionTimeout = timeout.seconds
                                    settings.connectionTimeout = timeout.seconds
                                    showConnectionTimeoutDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(timeout.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConnectionTimeoutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
    
    // 重试次数选择对话框
    if (showRetryCountDialog) {
        AlertDialog(
            onDismissRequest = { showRetryCountDialog = false },
            title = { Text(stringResource(R.string.form_retry_count)) },
            text = {
                Column {
                    RetryCount.values().forEach { count ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    retryCount = count.count
                                    settings.retryCount = count.count
                                    showRetryCountDialog = false
                                    Toast.makeText(context, Loc.s(R.string.s0667, count.displayName), Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = retryCount == count.count,
                                onClick = {
                                    retryCount = count.count
                                    settings.retryCount = count.count
                                    showRetryCountDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(count.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetryCountDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
    
    // 流量单位选择对话框
    if (showTrafficUnitDialog) {
        AlertDialog(
            onDismissRequest = { showTrafficUnitDialog = false },
            title = { Text(stringResource(R.string.form_traffic_unit)) },
            text = {
                Column {
                    TrafficUnit.values().forEach { unit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    trafficUnit = unit
                                    settings.trafficUnit = unit
                                    showTrafficUnitDialog = false
                                    Toast.makeText(context, Loc.s(R.string.s0668, unit.displayName), Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = trafficUnit == unit,
                                onClick = {
                                    trafficUnit = unit
                                    settings.trafficUnit = unit
                                    showTrafficUnitDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(unit.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrafficUnitDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
    
    // 清除缓存确认对话框
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.common_clear_cache)) },
            text = { Text(stringResource(R.string.dialog_clear_cache_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 清除缓存
                        scope.launch(Dispatchers.IO) {
                            try {
                                CacheRepository.getInstance(context).clearAllCache()
                                withContext(Dispatchers.Main) {
                                    cacheSize = "0 B"
                                    Toast.makeText(context, Loc.s(R.string.s0669), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, Loc.s(R.string.s0670, e.message), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        showClearCacheDialog = false
                    }
                ) {
                    Text(stringResource(R.string.common_confirm), color = MiTheme.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
fun AboutCard() {
    
    MiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MiFeatureIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                gradient = MiColors.GradientBlue,
                size = 48.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Loc.s(R.string.s0490),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiTheme.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Loc.s(R.string.s0671),
                fontSize = 13.sp,
                color = MiTheme.TextTertiary
            )
        }
    }
}

@Composable
private fun MiDivider(indent: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(0.5.dp)
            .background(MiTheme.Divider)
    )
}



/**
 * 主题选择对话框
 */
@Composable
fun ThemeSelectionDialog(
    showDialog: Boolean,
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onModeSelected: (ThemeMode) -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = Loc.s(R.string.s0637),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    ThemeOptionItem(
                        title = Loc.s(R.string.s0674),
                        description = Loc.s(R.string.s0675),
                        isSelected = currentMode == ThemeMode.LIGHT,
                        onClick = {
                            onModeSelected(ThemeMode.LIGHT)
                            onDismiss()
                        }
                    )
                    ThemeOptionItem(
                        title = Loc.s(R.string.s0676),
                        description = Loc.s(R.string.s0677),
                        isSelected = currentMode == ThemeMode.DARK,
                        onClick = {
                            onModeSelected(ThemeMode.DARK)
                            onDismiss()
                        }
                    )
                    ThemeOptionItem(
                        title = Loc.s(R.string.s0678),
                        description = Loc.s(R.string.s0679),
                        isSelected = currentMode == ThemeMode.SYSTEM,
                        onClick = {
                            onModeSelected(ThemeMode.SYSTEM)
                            onDismiss()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            containerColor = MiTheme.CardBackground,
            titleContentColor = MiTheme.TextPrimary,
            textContentColor = MiTheme.TextSecondary
        )
    }
}

/**
 * 主题选项
 */
@Composable
fun ThemeOptionItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MiTheme.Primary
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiTheme.TextPrimary
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = MiTheme.TextSecondary
            )
        }
    }
}

/**
 * 格式化文件大小
 */
private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
