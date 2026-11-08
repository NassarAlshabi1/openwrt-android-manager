package com.luanmuc.openwrtmanager.ui.home
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import com.luanmuc.openwrtmanager.ui.components.DualLineChart
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.LineChart
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiLinearProgress
import com.luanmuc.openwrtmanager.ui.components.MiListItem
import com.luanmuc.openwrtmanager.ui.components.MiLoadingState
import com.luanmuc.openwrtmanager.ui.components.MiPrimaryButton
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.OfflineBanner
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 首页 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddRouter: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToWifi: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToSystem: () -> Unit,
    onNavigateToFirewall: () -> Unit,
    onNavigateToDdns: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    var showDeviceDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0490),
                navigationIcon = {
                    if (uiState.hasRouter) {
                        IconButton(onClick = { showDeviceDialog = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = Loc.s(R.string.s0491),
                                    tint = MiColors.Primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MiTheme.TextSecondary
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = Loc.s(R.string.s0322)
                        )
                    }
                }
            )
        },
        containerColor = MiTheme.Background
    ) { padding ->
        if (!uiState.hasRouter) {
            EmptyRouterView(
                onAddRouter = onAddRouter,
                modifier = Modifier.padding(padding)
            )
        } else {
            HomeContent(
                uiState = uiState,
                viewModel = viewModel,
                onNavigateToDevices = onNavigateToDevices,
                onNavigateToPlugins = onNavigateToPlugins,
                onNavigateToWifi = onNavigateToWifi,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToSystem = onNavigateToSystem,
                onNavigateToFirewall = onNavigateToFirewall,
                onNavigateToDdns = onNavigateToDdns,
                onNavigateToAdvanced = onNavigateToAdvanced,
                modifier = Modifier.padding(padding)
            )
        }
        
        // 设备选择对话框
        if (showDeviceDialog) {
            DeviceSelectorDialog(
                activeRouter = uiState.activeRouter,
                onDismiss = { showDeviceDialog = false },
                onAddRouter = {
                    showDeviceDialog = false
                    onAddRouter()
                },
                onViewDevices = {
                    showDeviceDialog = false
                    onNavigateToDevices()
                }
            )
        }
    }
}

/**
 * 设备选择对话框
 */
@Composable
fun DeviceSelectorDialog(
    activeRouter: com.luanmuc.openwrtmanager.data.model.Router?,
    onDismiss: () -> Unit,
    onAddRouter: () -> Unit,
    onViewDevices: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Loc.s(R.string.s0492),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // 当前路由器
                activeRouter?.let { router ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiColors.Primary.copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MiColors.GradientBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = router.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTheme.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = router.address,
                                fontSize = 12.sp,
                                color = MiTheme.TextTertiary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MiColors.Success)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 查看在线设备
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onViewDevices)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = MiColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Loc.s(R.string.s0076),
                        fontSize = 15.sp,
                        color = MiTheme.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // 添加路由器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddRouter)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MiColors.Warning,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Loc.s(R.string.s0493),
                        fontSize = 15.sp,
                        color = MiTheme.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}

/**
 * 空路由器视图
 */
@Composable
fun EmptyRouterView(
    onAddRouter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MiColors.GradientBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Router,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = Loc.s(R.string.s0494),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MiTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = Loc.s(R.string.s0495),
            fontSize = 14.sp,
            color = MiTheme.TextTertiary
        )
        Spacer(modifier = Modifier.height(32.dp))
        MiPrimaryButton(
            text = Loc.s(R.string.s0493),
            onClick = onAddRouter
        )
    }
}

/**
 * 首页内容 - 小米路由器风格
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    uiState: HomeViewModel.HomeUiState,
    viewModel: HomeViewModel,
    onNavigateToDevices: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToWifi: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToSystem: () -> Unit,
    onNavigateToFirewall: () -> Unit,
    onNavigateToDdns: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dashboardConfig = viewModel.dashboardConfig
    val cards by dashboardConfig.config.collectAsStateWithLifecycle()
    val isEditMode by dashboardConfig.isEditMode.collectAsStateWithLifecycle()
    var showPresetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 离线提示条
        OfflineBanner(isOffline = isOffline)
        
        // 长按进入编辑模式提示
        if (!isEditMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .combinedClickable(
                        onClick = { dashboardConfig.toggleEditMode() },
                        onLongClick = { dashboardConfig.toggleEditMode() }
                    ),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { dashboardConfig.toggleEditMode() }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Loc.s(R.string.s0496),
                        fontSize = 12.sp,
                        color = MiTheme.TextSecondary
                    )
                }
            }
        }
        
        // 编辑模式工具栏
        if (isEditMode) {
            EditModeToolbar(
                onDone = { dashboardConfig.exitEditMode() },
                onReset = { dashboardConfig.resetToDefault() },
                onPreset = { showPresetDialog = true }
            )
        }
        
        // 动态渲染卡片（根据DashboardConfig配置）
        // 根据设备能力过滤卡片
        val hasWifi = uiState.deviceCapabilities.hasWifi
        cards.filter { it.visible }.filter { card ->
            when (card.id) {
                CardType.WIFI_STATUS -> hasWifi // 没有WiFi就隐藏WiFi卡片
                else -> true
            }
        }.forEach { card ->
            DashboardCardWrapper(
                card = card,
                isEditMode = isEditMode,
                onRemove = { dashboardConfig.toggleCardVisibility(card.id) },
                onMoveUp = { 
                    val index = cards.indexOf(card)
                    if (index > 0) {
                        dashboardConfig.moveCard(index, index - 1)
                    }
                },
                onMoveDown = { 
                    val index = cards.indexOf(card)
                    if (index < cards.size - 1) {
                        dashboardConfig.moveCard(index, index + 1)
                    }
                },
                onSizeChange = { newSize ->
                    dashboardConfig.changeCardSize(card.id, newSize)
                }
            ) {
                // 根据卡片类型渲染对应内容
                when (card.id) {
                    CardType.ROUTER_STATUS -> {
                        RouterStatusCard(
                            uiState = uiState,
                            onRefresh = { viewModel.refresh() },
                            onViewDevices = onNavigateToDevices,
                            onViewWifi = onNavigateToWifi,
                            onViewSystem = onNavigateToSystem
                        )
                    }
                    CardType.NETWORK_SPEED -> {
                        NetworkSpeedCard(
                            uiState = uiState,
                            viewModel = viewModel,
                            onClick = onNavigateToNetwork
                        )
                    }
                    CardType.QUICK_ACTIONS -> {
                        QuickFunctionsGrid(
                            onNavigateToDevices = onNavigateToDevices,
                            onNavigateToWifi = onNavigateToWifi,
                            onNavigateToPlugins = onNavigateToPlugins,
                            onNavigateToNetwork = onNavigateToNetwork
                        )
                    }
                    CardType.SYSTEM_STATUS -> {
                        SystemStatusCard(
                            uiState = uiState,
                            onClick = onNavigateToSystem
                        )
                    }
                    CardType.ONLINE_DEVICES -> {
                        OnlineDevicesCard(
                            uiState = uiState,
                            onClick = onNavigateToDevices
                        )
                    }
                    CardType.WIFI_STATUS -> {
                        WifiStatusCard(
                            uiState = uiState,
                            onClick = onNavigateToWifi
                        )
                    }
                    CardType.PLUGINS -> {
                        PluginsCard(
                            onClick = onNavigateToPlugins
                        )
                    }
                    CardType.FIREWALL -> {
                        FirewallCard(
                            onClick = onNavigateToFirewall
                        )
                    }
                    CardType.DDNS -> {
                        DdnsCard(
                            onClick = onNavigateToDdns
                        )
                    }
                    CardType.PORT_STATUS -> {
                        PortStatusCard(
                            ports = uiState.portStatus,
                            onClick = onNavigateToNetwork
                        )
                    }
                }
            }
        }
        
        // 编辑模式下的添加卡片按钮
        if (isEditMode) {
            AddCardButton(
                hiddenCards = cards.filter { !it.visible },
                onAddCard = { cardType ->
                    dashboardConfig.toggleCardVisibility(cardType)
                }
            )
        }
        
        // 预设布局对话框
        if (showPresetDialog) {
            PresetLayoutDialog(
                onDismiss = { showPresetDialog = false },
                onSelect = { layout ->
                    dashboardConfig.applyPresetLayout(layout)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 路由器状态大卡片 - 小米风格渐变卡片
 */
@Composable
fun RouterStatusCard(
    uiState: HomeViewModel.HomeUiState,
    onRefresh: () -> Unit,
    onViewDevices: () -> Unit = {},
    onViewWifi: () -> Unit = {},
    onViewSystem: () -> Unit = {}
) {
    val status = uiState.routerStatus
    val isOnline = status != null
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isOnline) {
                        listOf(
                            MiTheme.Primary,
                            MiColors.PrimaryLight
                        )
                    } else {
                        listOf(
                            MiTheme.TextTertiary,
                            MiTheme.TextSecondary
                        )
                    }
                )
            )
            .padding(20.dp)
    ) {
        Column {
            // 顶部：路由器名称和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.activeRouter?.name ?: Loc.s(R.string.s0242),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOnline) MiTheme.Success
                                    else MiTheme.Error
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnline) Loc.s(R.string.s0499) else Loc.s(R.string.s0500),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 关键数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatusItem(
                    icon = Icons.Default.Devices,
                    label = Loc.s(R.string.s0076),
                    value = "${status?.onlineDevices ?: 0}",
                    unit = Loc.s(R.string.s0501),
                    onClick = onViewDevices
                )
                StatusItem(
                    icon = Icons.Default.Wifi,
                    label = "WiFi",
                    value = if (isOnline) Loc.s(R.string.s0502) else Loc.s(R.string.s0277),
                    unit = "",
                    onClick = onViewWifi
                )
                StatusItem(
                    icon = Icons.Default.Speed,
                    label = "CPU",
                    value = "${(status?.cpuUsage ?: 0f).toInt()}",
                    unit = "%",
                    onClick = onViewSystem
                )
            }
        }
        
        // 加载中遮罩
        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

/**
 * 状态数据项
 */
@Composable
fun StatusItem(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * 网速监控卡片
 */
@Composable
fun NetworkSpeedCard(
    uiState: HomeViewModel.HomeUiState,
    viewModel: HomeViewModel,
    onClick: () -> Unit = {}
) {
    val status = uiState.routerStatus
    val wan = uiState.wanStatus
    
    MiCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MiColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0132),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (status?.wanConnected == true) Loc.s(R.string.s0469) else Loc.s(R.string.s0470),
                    fontSize = 13.sp,
                    color = if (status?.wanConnected == true) MiColors.Success else MiColors.Error,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 实时速度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 下载速度
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MiColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Loc.s(R.string.s0503),
                            fontSize = 12.sp,
                            color = MiTheme.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatSpeed(uiState.downloadSpeed),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiColors.Primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "/s",
                            fontSize = 12.sp,
                            color = MiTheme.TextTertiary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                
                // 分割线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(MiTheme.Divider)
                )
                
                // 上传速度
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = MiColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Loc.s(R.string.s0504),
                            fontSize = 12.sp,
                            color = MiTheme.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatSpeed(uiState.uploadSpeed),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiColors.Success
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "/s",
                            fontSize = 12.sp,
                            color = MiTheme.TextTertiary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 流量曲线图
            if (uiState.trafficHistory.size > 1) {
                DualLineChart(
                    data1 = uiState.trafficHistory.map { it.rx / 1024f / 1024f },
                    data2 = uiState.trafficHistory.map { it.tx / 1024f / 1024f },
                    label1 = Loc.s(R.string.s0505),
                    label2 = Loc.s(R.string.s0506),
                    unit = "MB",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 总流量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 总下载
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.s(R.string.s0507),
                        fontSize = 11.sp,
                        color = MiTheme.TextTertiary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.formatBytes(wan?.rxBytes ?: 0),
                        fontSize = 13.sp,
                        color = MiTheme.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 总上传
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.s(R.string.s0508),
                        fontSize = 11.sp,
                        color = MiTheme.TextTertiary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.formatBytes(wan?.txBytes ?: 0),
                        fontSize = 13.sp,
                        color = MiTheme.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // WAN IP
            if (!wan?.ipaddr.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiTheme.Background)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WAN IP",
                            fontSize = 12.sp,
                            color = MiTheme.TextTertiary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = wan?.ipaddr ?: "",
                            fontSize = 13.sp,
                            color = MiTheme.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 常用功能网格
 */
@Composable
fun QuickFunctionsGrid(
    onNavigateToDevices: () -> Unit,
    onNavigateToWifi: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToNetwork: () -> Unit
) {
    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = Loc.s(R.string.s0509),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiTheme.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickFunctionItem(
                    icon = Icons.Default.Devices,
                    label = Loc.s(R.string.s0510),
                    gradient = MiColors.GradientBlue,
                    onClick = onNavigateToDevices
                )
                QuickFunctionItem(
                    icon = Icons.Default.Wifi,
                    label = Loc.s(R.string.s0511),
                    gradient = MiColors.GradientGreen,
                    onClick = onNavigateToWifi
                )
                QuickFunctionItem(
                    icon = Icons.Default.Extension,
                    label = Loc.s(R.string.s0471),
                    gradient = MiColors.GradientOrange,
                    onClick = onNavigateToPlugins
                )
                QuickFunctionItem(
                    icon = Icons.Default.Settings,
                    label = Loc.s(R.string.s0512),
                    gradient = MiColors.GradientPurple,
                    onClick = onNavigateToNetwork
                )
            }
        }
    }
}

/**
 * 快捷功能项
 */
@Composable
fun QuickFunctionItem(
    icon: ImageVector,
    label: String,
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
            text = label,
            fontSize = 13.sp,
            color = MiTheme.TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 系统状态卡片
 */
@Composable
fun SystemStatusCard(
    uiState: HomeViewModel.HomeUiState,
    onClick: () -> Unit = {}
) {
    val status = uiState.routerStatus
    
    MiCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiColors.GradientCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0040),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // CPU使用率
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s(R.string.s0513),
                    fontSize = 14.sp,
                    color = MiTheme.TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(status?.cpuUsage ?: 0f).toInt()}%",
                    fontSize = 14.sp,
                    color = MiTheme.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // CPU历史曲线图
            if (uiState.cpuHistory.isNotEmpty()) {
                LineChart(
                    data = uiState.cpuHistory.map { it.usage },
                    color = MiColors.Success,
                    unit = "%",
                    showGrid = false,
                    modifier = Modifier.height(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))

            // 内存使用
            StatusProgressItem(
                label = Loc.s(R.string.s0514),
                progress = if (status?.memoryTotal ?: 0 > 0) {
                    (status?.memoryUsed ?: 0).toFloat() / (status?.memoryTotal ?: 1).toFloat()
                } else 0f,
                used = status?.memoryUsed ?: 0,
                total = status?.memoryTotal ?: 0,
                color = MiColors.Primary
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // 存储使用
            StatusProgressItem(
                label = Loc.s(R.string.s0515),
                progress = if (status?.storageTotal ?: 0 > 0) {
                    (status?.storageUsed ?: 0).toFloat() / (status?.storageTotal ?: 1).toFloat()
                } else 0f,
                used = status?.storageUsed ?: 0,
                total = status?.storageTotal ?: 0,
                color = MiColors.Warning
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // 运行时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s(R.string.s0516),
                    fontSize = 14.sp,
                    color = MiTheme.TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatUptime(status?.uptime ?: 0),
                    fontSize = 14.sp,
                    color = MiTheme.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 状态进度项
 */
@Composable
fun StatusProgressItem(
    label: String,
    progress: Float,
    used: Long,
    total: Long,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MiTheme.TextSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${formatBytes(used)} / ${formatBytes(total)}",
                fontSize = 13.sp,
                color = MiTheme.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        MiLinearProgress(
            progress = progress,
            color = color
        )
    }
}

/**
 * 更多功能区域
 */
@Composable
fun MoreFunctionsSection(
    onNavigateToSystem: () -> Unit,
    onNavigateToAdvanced: () -> Unit
) {
    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = Loc.s(R.string.s0517),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
            }
            
            MiDivider()
            
            MiListItem(
                title = Loc.s(R.string.s0518),
                subtitle = Loc.s(R.string.s0519),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MiColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onNavigateToSystem
            )
            
            MiDivider(indent = 60.dp)
            
            MiListItem(
                title = Loc.s(R.string.s0261),
                subtitle = Loc.s(R.string.s0520),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MiColors.Warning,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onNavigateToAdvanced
            )
        }
    }
}

// ========== 工具函数 ==========

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1073741824 -> String.format("%.2f GB", bytes / 1073741824.0)
        bytes >= 1048576 -> String.format("%.2f MB", bytes / 1048576.0)
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1048576 -> String.format("%.2f MB", bytesPerSecond / 1048576.0)
        bytesPerSecond >= 1024 -> String.format("%.2f KB", bytesPerSecond / 1024.0)
        else -> "$bytesPerSecond B"
    }
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> Loc.s(R.string.s0521, days, hours)
        hours > 0 -> Loc.s(R.string.s0522, hours, minutes)
        else -> Loc.s(R.string.s0523, minutes)
    }
}

@Composable
private fun MiDivider(indent: Dp = 0.dp) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(0.5.dp)
            .background(MiTheme.Divider)
    )
}
