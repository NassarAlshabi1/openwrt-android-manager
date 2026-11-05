package com.luanmuc.openwrtmanager.ui.terminal

import com.luanmuc.openwrtmanager.R

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import kotlinx.coroutines.launch
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 终端页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    viewModel: TerminalViewModel = viewModel()
) {
    val terminalConfig by viewModel.terminalConfig.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val currentInput by viewModel.currentInput.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val showQuickCommands by viewModel.showQuickCommands.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val quickCommands by viewModel.quickCommands.collectAsState()
    val commandHistory by viewModel.commandHistory.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terminal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = Loc.s(R.string.s0244))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearTerminal() }) {
                        Icon(Icons.Default.Clear, contentDescription = Loc.s(R.string.s0560))
                    }
                    IconButton(onClick = { viewModel.toggleQuickCommands() }) {
                        Icon(Icons.Default.Terminal, contentDescription = Loc.s(R.string.s0765))
                    }
                    IconButton(onClick = { viewModel.toggleSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = Loc.s(R.string.s0766))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 快捷命令面板
            if (showQuickCommands) {
                QuickCommandsPanel(
                    quickCommands = quickCommands,
                    onCommandSelected = { viewModel.executeQuickCommand(it) },
                    onDismiss = { viewModel.toggleQuickCommands() }
                )
            }
            
            // 设置面板
            if (showSettings) {
                SettingsPanel(
                    config = terminalConfig,
                    onConfigChanged = { viewModel.updateTerminalConfig(it) },
                    onDismiss = { viewModel.toggleSettings() }
                )
            }
            
            // 终端输出区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color.Black)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    item {
                        Text(
                            text = terminalOutput,
                            color = androidx.compose.ui.graphics.Color(0xFF00FF00),
                            fontFamily = FontFamily.Monospace,
                            fontSize = terminalConfig.fontSize.sp,
                            lineHeight = (terminalConfig.fontSize + 4).sp
                        )
                    }
                    
                    if (isExecuting) {
                        item {
                            Text(
                                text = "...",
                                color = androidx.compose.ui.graphics.Color(0xFF00FF00),
                                fontFamily = FontFamily.Monospace,
                                fontSize = terminalConfig.fontSize.sp
                            )
                        }
                    }
                }
            }
            
            // 输入区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(MiDimens.cardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MiTheme.CardBackground
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "root@OpenWrt:~#",
                        color = MiTheme.Primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    TextField(
                        value = currentInput,
                        onValueChange = { viewModel.setCurrentInput(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isExecuting,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MiTheme.Background,
                            unfocusedContainerColor = MiTheme.Background,
                            focusedIndicatorColor = MiTheme.Primary,
                            unfocusedIndicatorColor = MiTheme.Divider
                        ),
                        shape = RoundedCornerShape(MiDimens.inputRadius)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { 
                            viewModel.executeCommand()
                            coroutineScope.launch {
                                listState.animateScrollToItem(Int.MAX_VALUE)
                            }
                        },
                        enabled = currentInput.isNotBlank() && !isExecuting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MiTheme.Primary
                        ),
                        shape = RoundedCornerShape(MiDimens.buttonRadius)
                    ) {
                        Text(stringResource(R.string.common_execute))
                    }
                }
            }
        }
    }
}

/**
 * 快捷命令面板
 */
@Composable
private fun QuickCommandsPanel(
    quickCommands: List<com.luanmuc.openwrtmanager.data.model.QuickCommand>,
    onCommandSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiDimens.horizontalPadding, vertical = 8.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = Loc.s(R.string.s0765),
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Loc.s(R.string.s0765),
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 快捷命令列表
            quickCommands.take(6).forEach { command ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MiDimens.smallRadius))
                        .clickable { onCommandSelected(command.command) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = command.name,
                            color = MiTheme.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = command.command,
                            color = MiTheme.TextTertiary,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * 设置面板
 */
@Composable
private fun SettingsPanel(
    config: com.luanmuc.openwrtmanager.data.model.TerminalConfig,
    onConfigChanged: (com.luanmuc.openwrtmanager.data.model.TerminalConfig) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiDimens.horizontalPadding, vertical = 8.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = Loc.s(R.string.s0767),
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Loc.s(R.string.s0767),
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 字体大小
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s(R.string.s0768),
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${config.fontSize} sp",
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = config.fontSize.toFloat(),
                onValueChange = { onConfigChanged(config.copy(fontSize = it.toInt())) },
                valueRange = 10f..24f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = MiTheme.Primary,
                    activeTrackColor = MiTheme.Primary
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 主题选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s(R.string.s0769),
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = config.theme.displayName,
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
