package com.luanmuc.openwrtmanager.ui.diagnostic

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiPrimaryButton
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.util.NetworkDiagnostic
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 智能诊断页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0368),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244)
                        )
                    }
                }
            )
        },
        containerColor = MiTheme.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 诊断状态卡片
            MiCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isRunning) {
                        // 诊断中
                        CircularProgressIndicator(
                            color = MiColors.Primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Loc.s(R.string.s0369),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.currentStep,
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                    } else if (uiState.result != null) {
                        // 诊断结果
                        uiState.result?.let { result ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    if (result.isHealthy) {
                                        MiColors.Success.copy(alpha = 0.1f)
                                    } else {
                                        MiColors.Error.copy(alpha = 0.1f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (result.isHealthy) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Error
                                },
                                contentDescription = null,
                                tint = if (result.isHealthy) {
                                    MiColors.Success
                                } else {
                                    MiColors.Error
                                },
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (result.isHealthy) Loc.s(R.string.s0370) else Loc.s(R.string.s0371),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Loc.s(R.string.s0372, result.issues.size),
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                        } // end let
                    } else {
                        // 未开始诊断
                        MiFeatureIcon(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            gradient = MiColors.GradientBlue,
                            size = 80.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Loc.s(R.string.s0373),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiTheme.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Loc.s(R.string.s0374),
                            fontSize = 14.sp,
                            color = MiTheme.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 开始诊断按钮
            if (!uiState.isRunning) {
                MiPrimaryButton(
                    text = if (uiState.result != null) Loc.s(R.string.s0375) else Loc.s(R.string.s0376),
                    onClick = { viewModel.runDiagnostic() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 诊断结果详情
            uiState.result?.let { result ->
                Spacer(modifier = Modifier.height(24.dp))

                // 检测项列表
                Text(
                    text = Loc.s(R.string.s0377),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                result.issues.forEach { issue ->
                    DiagnosticIssueItem(issue = issue)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 修复建议
                if (result.suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = Loc.s(R.string.s0378),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiTheme.TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MiCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            result.suggestions.forEachIndexed { index, suggestion ->
                                Row(
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${index + 1}. ",
                                        fontSize = 14.sp,
                                        color = MiColors.Primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = suggestion,
                                        fontSize = 14.sp,
                                        color = MiTheme.TextSecondary
                                    )
                                }
                                if (index < result.suggestions.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 诊断问题项
 */
@Composable
fun DiagnosticIssueItem(issue: NetworkDiagnostic.DiagnosticIssue) {
    val (icon, color) = when (issue.severity) {
        NetworkDiagnostic.Severity.CRITICAL -> Icons.Default.Error to MiColors.Error
        NetworkDiagnostic.Severity.ERROR -> Icons.Default.Error to MiColors.Error
        NetworkDiagnostic.Severity.WARNING -> Icons.Default.Warning to MiColors.Warning
        NetworkDiagnostic.Severity.INFO -> Icons.Default.Info to MiColors.Info
    }

    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = issue.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = issue.suggestion,
                    fontSize = 12.sp,
                    color = MiTheme.TextSecondary
                )
            }
        }
    }
}
