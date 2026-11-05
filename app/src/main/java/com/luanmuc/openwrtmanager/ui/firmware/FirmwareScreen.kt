package com.luanmuc.openwrtmanager.ui.firmware
import com.luanmuc.openwrtmanager.util.DebounceUtils

import com.luanmuc.openwrtmanager.R

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luanmuc.openwrtmanager.ui.components.*
import com.luanmuc.openwrtmanager.ui.components.MiButtonType
import com.luanmuc.openwrtmanager.data.model.FirmwareUpgradeState
import com.luanmuc.openwrtmanager.util.Loc
import com.luanmuc.openwrtmanager.util.OpenWrtVersions

/**
 * 固件升级页面
 */
@Composable
fun FirmwareScreen(
    onBack: () -> Unit = {},
    viewModel: FirmwareViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 文件选择器
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = it.lastPathSegment ?: "firmware.bin"
            android.widget.Toast.makeText(context, Loc.s(R.string.s0419, fileName), android.widget.Toast.LENGTH_SHORT).show()
            viewModel.uploadLocalFirmware(it, fileName)
        }
    }
    
    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0420),
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
                    IconButton(onClick = { viewModel.checkLatestVersion() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = Loc.s(R.string.s0421),
                            tint = MiTheme.Primary
                        )
                    }
                }
            )
        },
        containerColor = MiTheme.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前固件信息
            CurrentFirmwareCard(
                version = uiState.firmwareInfo.currentVersion,
                buildTime = uiState.firmwareInfo.currentBuildTime,
                model = uiState.firmwareInfo.deviceModel,
                architecture = uiState.firmwareInfo.architecture
            )

            // 最新版本信息
            uiState.latestRelease?.let { release ->
                LatestVersionCard(
                    release = release,
                    isChecking = uiState.isChecking,
                    onDownloadClick = { if (DebounceUtils.canClick()) viewModel.downloadFirmware() }
                )
            } ?: if (uiState.isChecking) {
                CheckingCard()
            } else {
                NoUpdateCard(
                    onCheckClick = { viewModel.checkLatestVersion() }
                )
            }

            // 下载进度
            if (uiState.upgradeState == FirmwareUpgradeState.DOWNLOADING ||
                uiState.upgradeState == FirmwareUpgradeState.VERIFYING
            ) {
                DownloadProgressCard(
                    progress = uiState.downloadProgress,
                    speed = uiState.downloadSpeed,
                    isVerifying = uiState.upgradeState == FirmwareUpgradeState.VERIFYING
                )
            }

            // 刷写进度
            if (uiState.upgradeState == FirmwareUpgradeState.FLASHING ||
                uiState.upgradeState == FirmwareUpgradeState.REBOOTING ||
                uiState.upgradeState == FirmwareUpgradeState.SUCCESS
            ) {
                FlashProgressCard(
                    progress = uiState.flashProgress,
                    state = uiState.upgradeState
                )
            }

            // 升级选项
            UpgradeOptionsCard(
                keepConfig = uiState.keepConfig,
                onKeepConfigChange = { viewModel.toggleKeepConfig(it) }
            )

            // 本地固件升级
            LocalUpgradeCard(
                onSelectFile = { fileLauncher.launch("*/*") }
            )

            // 升级配置
            UpgradeConfigCard(
                repoUrl = uiState.config.repoUrl,
                onConfigChange = { }
            )
        }
    }

    // 确认对话框
    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = {
                Text(stringResource(R.string.common_confirm_upgrade))
            },
            text = {
                Text(stringResource(R.string.dialog_firmware_upgrade_confirm))
            },
            confirmButton = {
                TextButton(onClick = { if (DebounceUtils.canClick()) viewModel.confirmFlash() }) {
                    Text(stringResource(R.string.common_confirm_upgrade), color = MiTheme.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmDialog() }) {
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
 * 当前固件信息卡片
 */
@Composable
private fun CurrentFirmwareCard(
    version: String,
    buildTime: String,
    model: String,
    architecture: String
) {
    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0422),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoItem(label = Loc.s(R.string.s0073), value = version.ifEmpty { Loc.s(R.string.s0050) })
            InfoItem(label = Loc.s(R.string.s0423), value = model.ifEmpty { Loc.s(R.string.s0050) })
            InfoItem(label = Loc.s(R.string.s0424), value = architecture.ifEmpty { Loc.s(R.string.s0050) })
            // OpenWrt 23.05.0 - 23.05.6 support status
            val versionSupported = androidx.compose.runtime.remember(version) {
                OpenWrtVersions.isSupported(version)
            }
            InfoItem(
                label = Loc.s(R.string.version_support_status),
                value = if (version.isEmpty()) Loc.s(R.string.s0050)
                    else if (versionSupported) Loc.s(R.string.version_supported_range, OpenWrtVersions.supportedRangeDescription())
                    else Loc.s(R.string.version_unsupported_range, OpenWrtVersions.supportedRangeDescription())
            )
        }
    }
}

/**
 * 最新版本信息卡片
 */
@Composable
private fun LatestVersionCard(
    release: com.luanmuc.openwrtmanager.data.model.FirmwareRelease,
    isChecking: Boolean,
    onDownloadClick: () -> Unit
) {
    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.NewReleases,
                    contentDescription = null,
                    tint = MiTheme.Success,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0425),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.Success
                )
                Spacer(modifier = Modifier.weight(1f))
                MiTag(
                    text = "NEW",
                    backgroundColor = MiTheme.Success.copy(alpha = 0.1f),
                    textColor = MiTheme.Success
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoItem(label = Loc.s(R.string.s0426), value = release.version)
            InfoItem(label = Loc.s(R.string.s0427), value = release.releaseDate)
            InfoItem(label = Loc.s(R.string.s0428), value = formatFileSize(release.size))

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = Loc.s(R.string.s0429),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MiTheme.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = release.changelog,
                fontSize = 13.sp,
                color = MiTheme.TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            MiButton(
                text = Loc.s(R.string.s0430),
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking
            )
        }
    }
}

/**
 * 检测中卡片
 */
@Composable
private fun CheckingCard() {
    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MiTheme.Primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Loc.s(R.string.s0431),
                fontSize = 14.sp,
                color = MiTheme.TextSecondary
            )
        }
    }
}

/**
 * 无更新卡片
 */
@Composable
private fun NoUpdateCard(
    onCheckClick: () -> Unit
) {
    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MiTheme.Success,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Loc.s(R.string.s0432),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MiTheme.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Loc.s(R.string.s0433),
                fontSize = 13.sp,
                color = MiTheme.TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            MiButton(
                text = Loc.s(R.string.s0421),
                onClick = onCheckClick,
                type = MiButtonType.Secondary
            )
        }
    }
}

/**
 * 下载进度卡片
 */
@Composable
private fun DownloadProgressCard(
    progress: Int,
    speed: Long,
    isVerifying: Boolean
) {
    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isVerifying) Loc.s(R.string.s0434) else Loc.s(R.string.s0435),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MiLinearProgress(
                progress = if (isVerifying) 100f else progress.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${if (isVerifying) Loc.s(R.string.s0436) else "$progress%"}",
                    fontSize = 13.sp,
                    color = MiTheme.TextSecondary
                )
                if (!isVerifying) {
                    Text(
                        text = "${formatSpeed(speed)}/s",
                        fontSize = 13.sp,
                        color = MiTheme.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * 刷写进度卡片
 */
@Composable
private fun FlashProgressCard(
    progress: Int,
    state: FirmwareUpgradeState
) {
    val statusText = when (state) {
        FirmwareUpgradeState.FLASHING -> Loc.s(R.string.s0437)
        FirmwareUpgradeState.REBOOTING -> Loc.s(R.string.s0438)
        FirmwareUpgradeState.SUCCESS -> Loc.s(R.string.s0439)
        else -> ""
    }

    val statusColor = when (state) {
        FirmwareUpgradeState.SUCCESS -> MiTheme.Success
        else -> MiTheme.Primary
    }

    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state == FirmwareUpgradeState.SUCCESS) Icons.Filled.CheckCircle else Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state == FirmwareUpgradeState.FLASHING) {
                MiLinearProgress(
                    progress = progress.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$progress%",
                    fontSize = 13.sp,
                    color = MiTheme.TextSecondary
                )
            } else if (state == FirmwareUpgradeState.REBOOTING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MiTheme.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Loc.s(R.string.s0440),
                    fontSize = 13.sp,
                    color = MiTheme.TextSecondary
                )
            } else if (state == FirmwareUpgradeState.SUCCESS) {
                Text(
                    text = Loc.s(R.string.s0441),
                    fontSize = 14.sp,
                    color = MiTheme.Success
                )
            }
        }
    }
}

/**
 * 升级选项卡片
 */
@Composable
private fun UpgradeOptionsCard(
    keepConfig: Boolean,
    onKeepConfigChange: (Boolean) -> Unit
) {
    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = Loc.s(R.string.s0442),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MiTheme.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Loc.s(R.string.s0443),
                        fontSize = 14.sp,
                        color = MiTheme.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Loc.s(R.string.s0444),
                        fontSize = 12.sp,
                        color = MiTheme.TextSecondary
                    )
                }
                MiSwitch(
                    checked = keepConfig,
                    onCheckedChange = onKeepConfigChange
                )
            }
        }
    }
}

/**
 * 本地固件升级卡片
 */
@Composable
private fun LocalUpgradeCard(
    onSelectFile: () -> Unit
) {
    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.FileUpload,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0445),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Loc.s(R.string.s0446),
                fontSize = 13.sp,
                color = MiTheme.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            MiButton(
                text = Loc.s(R.string.s0447),
                onClick = onSelectFile,
                type = MiButtonType.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 升级配置卡片
 */
@Composable
private fun UpgradeConfigCard(
    repoUrl: String,
    onConfigChange: () -> Unit
) {
    MiCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Loc.s(R.string.s0448),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoItem(
                label = Loc.s(R.string.s0449),
                value = repoUrl.ifEmpty { Loc.s(R.string.s0450) }
            )
        }
    }
}

/**
 * 信息项
 */
@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MiTheme.TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MiTheme.TextPrimary
        )
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${String.format("%.2f", size / (1024.0 * 1024.0))} MB"
        else -> "${String.format("%.2f", size / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

/**
 * 格式化下载速度
 */
private fun formatSpeed(speed: Long): String {
    return when {
        speed < 1024 -> "$speed B"
        speed < 1024 * 1024 -> "${speed / 1024} KB"
        else -> "${String.format("%.2f", speed / (1024.0 * 1024.0))} MB"
    }
}
