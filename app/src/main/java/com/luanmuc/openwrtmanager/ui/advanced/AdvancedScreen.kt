package com.luanmuc.openwrtmanager.ui.advanced

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.DebounceUtils

import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiDivider
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiListItem
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.OfflineBanner
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 高级功能页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(
    onBack: () -> Unit = {},
    onNavigateToSystem: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToDiagnostic: () -> Unit = {},
    onOpenWebView: (String, String) -> Unit = { _, _ -> },
    viewModel: AdvancedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRebootDialog by remember { mutableStateOf(false) }
    var showShutdownDialog by remember { mutableStateOf(false) }
    
    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    // 成功提示
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            android.widget.Toast.makeText(context, Loc.s(R.string.s0260), android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0261),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244),
                            tint = MiTheme.TextPrimary
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
                        // 离线提示条
            OfflineBanner(isOffline = !viewModel.isNetworkAvailable)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 系统操作
            SectionTitle(title = Loc.s(R.string.s0262))
            MiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    MiListItem(
                        title = Loc.s(R.string.s0233),
                        subtitle = Loc.s(R.string.s0263),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MiColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = { showRebootDialog = true }
                    )
                    MiDivider(indent = 60.dp)
                    MiListItem(
                        title = Loc.s(R.string.s0264),
                        subtitle = Loc.s(R.string.s0265),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = MiColors.Error,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = { showShutdownDialog = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 系统信息
            SectionTitle(title = Loc.s(R.string.s0130))
            MiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    MiListItem(
                        title = Loc.s(R.string.s0130),
                        subtitle = Loc.s(R.string.s0266),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MiColors.Success,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = onNavigateToSystem
                    )
                    MiDivider(indent = 60.dp)
                    MiListItem(
                        title = Loc.s(R.string.s0267),
                        subtitle = Loc.s(R.string.s0268),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MiColors.Warning,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = onNavigateToStorage
                    )
                    MiDivider(indent = 60.dp)
                    MiListItem(
                        title = Loc.s(R.string.s0269),
                        subtitle = Loc.s(R.string.s0270),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = MiColors.Purple,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = {
                            onOpenWebView("/cgi-bin/luci/admin/status/processes", Loc.s(R.string.s0269))
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 诊断工具
            SectionTitle(title = Loc.s(R.string.s0271))
            MiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    MiListItem(
                        title = Loc.s(R.string.s0272),
                        subtitle = "Ping、Traceroute",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MiColors.Cyan,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = onNavigateToDiagnostic
                    )
                    MiDivider(indent = 60.dp)
                    MiListItem(
                        title = Loc.s(R.string.s0273),
                        subtitle = Loc.s(R.string.s0274),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MiColors.Orange,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = {
                            onOpenWebView("/cgi-bin/luci/admin/services/ttyd", Loc.s(R.string.s0273))
                        }
                    )
                }
            }
        }
    }
    
    // 重启确认对话框
    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            icon = {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MiColors.Primary
                )
            },
            title = {
                Text(
                    Loc.s(R.string.s0233),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.advanced_reboot_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (DebounceUtils.canClick()) {
                            viewModel.reboot()
                            showRebootDialog = false
                        }
                    }
                ) {
                    Text(
                        Loc.s(R.string.s0275),
                        color = MiColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text(
                        Loc.s(R.string.s0276),
                        color = MiTheme.TextTertiary
                    )
                }
            },
            containerColor = Color.White
        )
    }
    
    // 关机确认对话框
    if (showShutdownDialog) {
        AlertDialog(
            onDismissRequest = { showShutdownDialog = false },
            icon = {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = MiColors.Error
                )
            },
            title = {
                Text(
                    Loc.s(R.string.s0264),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.advanced_shutdown_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (DebounceUtils.canClick()) {
                            viewModel.shutdown()
                            showShutdownDialog = false
                        }
                    }
                ) {
                    Text(
                        Loc.s(R.string.s0277),
                        color = MiColors.Error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showShutdownDialog = false }) {
                    Text(
                        Loc.s(R.string.s0276),
                        color = MiTheme.TextTertiary
                    )
                }
            },
            containerColor = Color.White
        )
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
