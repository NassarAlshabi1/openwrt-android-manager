package com.luanmuc.openwrtmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luanmuc.openwrtmanager.data.model.PluginInstallStatus
import com.luanmuc.openwrtmanager.util.DebugMode
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 插件依赖提示卡片
 * 当功能需要特定插件而未安装时显示，提供一键安装功能
 */
@Composable
fun PluginDependencyCard(
    status: PluginInstallStatus,
    onInstall: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (status.isInstalled) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF8F00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Loc.s(R.string.s0317, status.dependency.featureName),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5D4037)
                    )
                    Text(
                        text = status.dependency.packageName,
                        fontSize = 12.sp,
                        color = Color(0xFF8D6E63)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = status.dependency.description,
                fontSize = 13.sp,
                color = Color(0xFF6D4C41),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (status.isInstalling) {
                Column {
                    LinearProgressIndicator(
                        progress = { status.installProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFF8F00)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status.installMessage.ifEmpty { Loc.s(R.string.s0318) },
                        fontSize = 12.sp,
                        color = Color(0xFF8D6E63)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onInstall?.invoke()
                        },
                        enabled = !status.isInstalling,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF8F00)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Loc.s(R.string.s0319))
                    }
                }
            }


        }
    }
}

/**
 * 插件依赖检测页面
 * 统一检测和管理所有功能的插件依赖
 */
@Composable
fun PluginDependencyScreen(
    pluginStatuses: List<PluginInstallStatus>,
    onInstall: (PluginInstallStatus, (Int, String) -> Unit) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var installingIndex by remember { mutableStateOf(-1) }
    var progressMap by remember { mutableStateOf(mapOf<Int, Int>()) }
    var messageMap by remember { mutableStateOf(mapOf<Int, String>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部状态栏
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = Color(0xFF1677FF),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Loc.s(R.string.s0320),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = Loc.s(R.string.s0321, pluginStatuses.count { it.isInstalled }, pluginStatuses.size),
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = Loc.s(R.string.s0322))
                }
            }
        }

        // 插件列表
        pluginStatuses.forEachIndexed { index, status ->
            val isInstalling = installingIndex == index
            val progress = progressMap[index] ?: 0
            val message = messageMap[index] ?: ""

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (status.isInstalled) Icons.Default.CheckCircle 
                                          else Icons.Default.Extension,
                            contentDescription = null,
                            tint = if (status.isInstalled) Color(0xFF4CAF50) 
                                   else Color(0xFFFF9800),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.dependency.featureName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = status.dependency.packageName,
                                fontSize = 11.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        if (status.isInstalled) {
                            Text(
                                text = Loc.s(R.string.s0323),
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        } else if (!isInstalling) {
                            TextButton(
                                onClick = {
                                    installingIndex = index
                                    onInstall(status) { p, m ->
                                        progressMap = progressMap + (index to p)
                                        messageMap = messageMap + (index to m)
                                        if (p == 100 || p == 0) {
                                            installingIndex = -1
                                        }
                                    }
                                }
                            ) {
                                Text(Loc.s(R.string.s0324))
                            }
                        }
                    }

                    if (isInstalling) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (message.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message,
                                fontSize = 11.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = status.dependency.description,
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
