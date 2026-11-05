package com.luanmuc.openwrtmanager.ui.firewall
import com.luanmuc.openwrtmanager.util.DebounceUtils
import com.luanmuc.openwrtmanager.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.data.model.PortForwardRule
import com.luanmuc.openwrtmanager.ui.components.MiButton
import com.luanmuc.openwrtmanager.ui.components.MiButtonType
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiEmptyState
import com.luanmuc.openwrtmanager.ui.components.MiErrorState
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiLoadingState
import com.luanmuc.openwrtmanager.ui.components.MiTag
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.OfflineBanner
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 防火墙页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(
    onBack: () -> Unit = {},
    viewModel: FirewallViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<PortForwardRule?>(null) }
    var editingRule by remember { mutableStateOf(PortForwardRule()) }
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0409),
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
                    IconButton(onClick = { viewModel.loadFirewallConfig() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = Loc.s(R.string.s0322),
                            tint = MiTheme.TextSecondary
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = Loc.s(R.string.s0328),
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
            
            if (uiState.isLoading) {
                MiLoadingState()
            } else if (uiState.error != null && uiState.portForwards.isEmpty()) {
                MiErrorState(
                    message = uiState.error ?: Loc.s(R.string.s0329),
                    onRetry = { viewModel.loadFirewallConfig() }
                )
            } else if (uiState.portForwards.isEmpty()) {
                MiEmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MiTheme.TextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    text = Loc.s(R.string.s0410)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.portForwards, key = { it.name }) { rule ->
                        PortForwardCard(
                            rule = rule,
                            onDelete = { if (DebounceUtils.canClick()) viewModel.deletePortForward(it) },
                            onEdit = { rule ->
                                editingRule = rule
                                showEditDialog = rule
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddPortForwardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, proto, srcPort, destIp, destPort ->
                if (DebounceUtils.canClick()) {
                    viewModel.addPortForward(
                        PortForwardRule(
                            name = name,
                            proto = proto,
                            srcPort = srcPort,
                            destIp = destIp,
                            destPort = destPort
                        )
                    )
                    showAddDialog = false
                }
            }
        )
    }

    if (showEditDialog != null) {
        EditPortForwardDialog(
            initialRule = editingRule,
            onDismiss = { showEditDialog = null },
            onConfirm = { rule ->
                showEditDialog?.name?.let { editName ->
                    viewModel.editPortForward(editName, rule)
                    showEditDialog = null
                }
            }
        )
    }
}

@Composable
fun PortForwardCard(
    rule: PortForwardRule,
    onDelete: (String) -> Unit = {},
    onEdit: (PortForwardRule) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiFeatureIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                gradient = MiColors.GradientRed,
                size = 40.dp,
                iconSize = 20.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiTag(
                        text = rule.proto.uppercase(),
                        backgroundColor = MiColors.Primary.copy(alpha = 0.1f),
                        textColor = MiColors.Primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${rule.srcPort} → ${rule.destIp}:${rule.destPort}",
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary
                    )
                }
            }
            IconButton(onClick = { onEdit(rule) }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = Loc.s(R.string.s0335),
                    tint = MiColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = Loc.s(R.string.s0303),
                    tint = MiColors.Error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MiColors.Error
                )
            },
            title = {
                Text(
                    Loc.s(R.string.s0411),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.dialog_delete_port_forward_confirm, rule.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(rule.name)
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        Loc.s(R.string.s0303),
                        color = MiColors.Error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
fun AddPortForwardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var proto by rememberSaveable { mutableStateOf("tcp") }
    var srcPort by rememberSaveable { mutableStateOf("") }
    var destIp by rememberSaveable { mutableStateOf("") }
    var destPort by rememberSaveable { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                Loc.s(R.string.s0412),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.firewall_rule_name), fontSize = 14.sp, color = MiTheme.TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiColors.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
                Text(stringResource(R.string.firewall_protocol), fontSize = 14.sp, color = MiTheme.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("tcp", "udp", "tcpudp").forEach { p ->
                        androidx.compose.material3.FilterChip(
                            selected = proto == p,
                            onClick = { proto = p },
                            label = { Text(p.uppercase(), fontSize = 12.sp) },
                            colors = if (proto == p) {
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
                Text(stringResource(R.string.firewall_external_port), fontSize = 14.sp, color = MiTheme.TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = srcPort,
                    onValueChange = { srcPort = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiColors.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
                Text(stringResource(R.string.firewall_internal_ip), fontSize = 14.sp, color = MiTheme.TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = destIp,
                    onValueChange = { destIp = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiColors.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
                Text(stringResource(R.string.firewall_internal_port), fontSize = 14.sp, color = MiTheme.TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = destPort,
                    onValueChange = { destPort = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiColors.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotEmpty() && srcPort.isNotEmpty() && destIp.isNotEmpty() && destPort.isNotEmpty()) {
                        onConfirm(name, proto, srcPort, destIp, destPort)
                    }
                }
            ) {
                Text(
                    Loc.s(R.string.s0328),
                    color = MiColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    Loc.s(R.string.s0276),
                    color = MiTheme.TextTertiary
                )
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun EditPortForwardDialog(
    initialRule: PortForwardRule,
    onDismiss: () -> Unit,
    onConfirm: (PortForwardRule) -> Unit
) {
    var name by remember { mutableStateOf(initialRule.name) }
    var proto by remember { mutableStateOf(initialRule.proto) }
    var srcPort by remember { mutableStateOf(initialRule.srcPort) }
    var destIp by remember { mutableStateOf(initialRule.destIp) }
    var destPort by remember { mutableStateOf(initialRule.destPort) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                Loc.s(R.string.s0413),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.firewall_rule_name), fontSize = 14.sp, color = MiTheme.TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiColors.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
                Text(stringResource(R.string.firewall_protocol), fontSize = 14.sp, color = MiTheme.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("tcp", "udp", "tcpudp").forEach { p ->
                        androidx.compose.material3.FilterChip(
                            selected = proto == p,
                            onClick = { proto = p },
                            label = { Text(p.uppercase(), fontSize = 12.sp) },
                            colors = if (proto == p) {
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
                Text(stringResource(R.string.firewall_external_port), fontSize = 14.sp, color = MiTheme.TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = srcPort,
                    onValueChange = { srcPort = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiColors.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
                Text(stringResource(R.string.firewall_internal_ip), fontSize = 14.sp, color = MiTheme.TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = destIp,
                    onValueChange = { destIp = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiColors.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
                Text(stringResource(R.string.firewall_internal_port), fontSize = 14.sp, color = MiTheme.TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = destPort,
                    onValueChange = { destPort = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiColors.Primary,
                        unfocusedBorderColor = MiTheme.Divider
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotEmpty() && srcPort.isNotEmpty() && destIp.isNotEmpty() && destPort.isNotEmpty()) {
                        onConfirm(
                            PortForwardRule(
                                name = name,
                                proto = proto,
                                srcPort = srcPort,
                                destIp = destIp,
                                destPort = destPort
                            )
                        )
                    }
                }
            ) {
                Text(
                    Loc.s(R.string.s0414),
                    color = MiColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    Loc.s(R.string.s0276),
                    color = MiTheme.TextTertiary
                )
            }
        },
        containerColor = Color.White
    )
}
