package com.luanmuc.openwrtmanager.ui.remote

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.util.DebounceUtils
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 远程管理配置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteManageScreen(
    onBack: () -> Unit = {},
    viewModel: RemoteManageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val remoteEnabled = uiState.remoteEnabled
    val remoteAddress = uiState.remoteAddress
    val remotePort = uiState.remotePort
    val remoteProtocol = uiState.remoteProtocol
    val autoSwitchRemote = uiState.autoSwitchRemote
    val isRemoteMode = uiState.isRemoteMode
    val onRemoteEnabledChange: (Boolean) -> Unit = { viewModel.onRemoteEnabledChange(it) }
    val onRemoteAddressChange: (String) -> Unit = { viewModel.onRemoteAddressChange(it) }
    val onRemotePortChange: (Int) -> Unit = { viewModel.onRemotePortChange(it) }
    val onRemoteProtocolChange: (String) -> Unit = { viewModel.onRemoteProtocolChange(it) }
    val onAutoSwitchRemoteChange: (Boolean) -> Unit = { viewModel.onAutoSwitchRemoteChange(it) }
    val onForceRemoteMode: (Boolean) -> Unit = { viewModel.onForceRemoteMode(it) }
    val onTestConnection: () -> Unit = { viewModel.testRemoteConnection() }
    var showProtocolDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0631),
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
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.HelpOutline,
                            contentDescription = Loc.s(R.string.s0680),
                            tint = MiTheme.TextPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // 远程模式状态卡片
            RemoteStatusCard(
                isRemoteMode = isRemoteMode,
                remoteEnabled = remoteEnabled,
                onForceRemoteMode = onForceRemoteMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 远程管理开关
            RemoteSettingCard(
                title = Loc.s(R.string.s0681),
                description = Loc.s(R.string.s0682),
                icon = Icons.Filled.Wifi,
                gradient = MiColors.GradientBlue
            ) {
                Switch(
                    checked = remoteEnabled,
                    onCheckedChange = onRemoteEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MiTheme.Primary,
                        checkedTrackColor = MiTheme.Primary.copy(alpha = 0.3f)
                    )
                )
            }

            if (remoteEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                // 远程地址配置
                RemoteConfigCard(
                    remoteAddress = remoteAddress,
                    remotePort = remotePort,
                    remoteProtocol = remoteProtocol,
                    onRemoteAddressChange = onRemoteAddressChange,
                    onRemotePortChange = onRemotePortChange,
                    onProtocolClick = { showProtocolDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 自动切换开关
                RemoteSettingCard(
                    title = Loc.s(R.string.s0683),
                    description = Loc.s(R.string.s0684),
                    icon = Icons.Filled.SwapHoriz,
                    gradient = MiColors.GradientGreen
                ) {
                    Switch(
                        checked = autoSwitchRemote,
                        onCheckedChange = onAutoSwitchRemoteChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MiTheme.Primary,
                            checkedTrackColor = MiTheme.Primary.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 测试连接按钮
                Button(
                    onClick = {
                        if (DebounceUtils.canClick()) {
                            onTestConnection()
                            Toast.makeText(context, Loc.s(R.string.s0685), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MiTheme.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Loc.s(R.string.s0686), fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 方案说明
            SectionTitle(title = Loc.s(R.string.s0687))

            Spacer(modifier = Modifier.height(8.dp))

            // DDNS方案卡片
            SolutionCard(
                title = Loc.s(R.string.s0688),
                description = Loc.s(R.string.s0689),
                icon = Icons.Filled.Dns,
                gradient = MiColors.GradientBlue,
                isRecommended = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 内网穿透方案卡片
            SolutionCard(
                title = Loc.s(R.string.s0690),
                description = Loc.s(R.string.s0691),
                icon = Icons.Filled.VpnLock,
                gradient = MiColors.GradientPurple,
                isRecommended = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // VPN方案卡片
            SolutionCard(
                title = Loc.s(R.string.s0692),
                description = Loc.s(R.string.s0693),
                icon = Icons.Filled.Security,
                gradient = MiColors.GradientGreen,
                isRecommended = false
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 路由器配置指引
            SectionTitle(title = Loc.s(R.string.s0694))

            Spacer(modifier = Modifier.height(8.dp))

            GuideStepCard(
                step = 1,
                title = Loc.s(R.string.s0695),
                description = Loc.s(R.string.s0696)
            )

            GuideStepCard(
                step = 2,
                title = Loc.s(R.string.s0697),
                description = Loc.s(R.string.s0698)
            )

            GuideStepCard(
                step = 3,
                title = Loc.s(R.string.s0699),
                description = Loc.s(R.string.s0700)
            )

            GuideStepCard(
                step = 4,
                title = Loc.s(R.string.s0701),
                description = Loc.s(R.string.s0702)
            )
        }
    }

    // 协议选择对话框
    if (showProtocolDialog) {
        AlertDialog(
            onDismissRequest = { showProtocolDialog = false },
            title = { Text(Loc.s(R.string.s0703)) },
            text = {
                Column {
                    listOf("https", "http").forEach { protocol ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRemoteProtocolChange(protocol)
                                    showProtocolDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = remoteProtocol == protocol,
                                onClick = {
                                    onRemoteProtocolChange(protocol)
                                    showProtocolDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(protocol.uppercase())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProtocolDialog = false }) {
                    Text(Loc.s(R.string.s0276))
                }
            },
            containerColor = MiTheme.CardBackground,
            titleContentColor = MiTheme.TextPrimary,
            textContentColor = MiTheme.TextSecondary
        )
    }

    // 帮助对话框
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(Loc.s(R.string.s0704)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Loc.s(R.string.s0705))
                    Text(Loc.s(R.string.s0706))
                    Text(Loc.s(R.string.s0707))
                    Text(Loc.s(R.string.s0708))
                    Text(Loc.s(R.string.s0709))
                    Text(Loc.s(R.string.s0710))
                    Text(Loc.s(R.string.s0711))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(Loc.s(R.string.s0712))
                }
            },
            containerColor = MiTheme.CardBackground,
            titleContentColor = MiTheme.TextPrimary,
            textContentColor = MiTheme.TextSecondary
        )
    }
}

@Composable
private fun RemoteStatusCard(
    isRemoteMode: Boolean,
    remoteEnabled: Boolean,
    onForceRemoteMode: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRemoteMode) MiTheme.Success.copy(alpha = 0.1f) else MiTheme.CardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isRemoteMode) MiTheme.Success.copy(alpha = 0.2f)
                            else MiTheme.TextTertiary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRemoteMode) Icons.Filled.CheckCircle else Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = if (isRemoteMode) MiTheme.Success else MiTheme.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRemoteMode) Loc.s(R.string.s0713) else Loc.s(R.string.s0714),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiTheme.TextPrimary
                    )
                    Text(
                        text = if (isRemoteMode) Loc.s(R.string.s0715) else Loc.s(R.string.s0716),
                        fontSize = 13.sp,
                        color = MiTheme.TextSecondary
                    )
                }
            }

            if (remoteEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MiTheme.Divider)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Loc.s(R.string.s0717),
                        fontSize = 14.sp,
                        color = MiTheme.TextSecondary
                    )
                    Switch(
                        checked = isRemoteMode,
                        onCheckedChange = onForceRemoteMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MiTheme.Primary,
                            checkedTrackColor = MiTheme.Primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteSettingCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MiTheme.TextSecondary
                )
            }
            content()
        }
    }
}

@Composable
private fun RemoteConfigCard(
    remoteAddress: String,
    remotePort: Int,
    remoteProtocol: String,
    onRemoteAddressChange: (String) -> Unit,
    onRemotePortChange: (Int) -> Unit,
    onProtocolClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = Loc.s(R.string.s0718),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiTheme.TextPrimary
            )

            // 协议选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s(R.string.s0719),
                    fontSize = 14.sp,
                    color = MiTheme.TextSecondary,
                    modifier = Modifier.width(60.dp)
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onProtocolClick),
                    shape = RoundedCornerShape(10.dp),
                    color = MiTheme.Background
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = remoteProtocol.uppercase(),
                            fontSize = 14.sp,
                            color = MiTheme.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MiTheme.TextSecondary
                        )
                    }
                }
            }

            // 远程地址
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s(R.string.s0720),
                    fontSize = 14.sp,
                    color = MiTheme.TextSecondary,
                    modifier = Modifier.width(60.dp)
                )
                OutlinedTextField(
                    value = remoteAddress,
                    onValueChange = onRemoteAddressChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("example.com", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiTheme.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
            }

            // 端口
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s(R.string.s0721),
                    fontSize = 14.sp,
                    color = MiTheme.TextSecondary,
                    modifier = Modifier.width(60.dp)
                )
                OutlinedTextField(
                    value = remotePort.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { onRemotePortChange(it) }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("443", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiTheme.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
            }

            // 访问地址预览
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MiTheme.Background
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${remoteProtocol}://${remoteAddress.ifEmpty { "example.com" }}:${remotePort}",
                        fontSize = 13.sp,
                        color = MiTheme.TextSecondary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MiTheme.TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SolutionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    isRecommended: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiTheme.TextPrimary
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MiTheme.Success.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = Loc.s(R.string.s0209),
                                fontSize = 10.sp,
                                color = MiTheme.Success,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MiTheme.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GuideStepCard(
    step: Int,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MiTheme.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiTheme.Primary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MiTheme.TextSecondary
                )
            }
        }
    }
}
