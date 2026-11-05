package com.luanmuc.openwrtmanager.ui.ddns
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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.data.model.DdnsConfig
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiEmptyState
import com.luanmuc.openwrtmanager.ui.components.MiErrorState
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiLoadingState
import com.luanmuc.openwrtmanager.ui.components.MiTag
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.ui.components.PluginDependencyCard
import com.luanmuc.openwrtmanager.ui.components.OfflineBanner
import com.luanmuc.openwrtmanager.util.Loc

/**
 * DDNS页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DdnsScreen(
    onBack: () -> Unit = {},
    viewModel: DdnsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<DdnsConfig?>(null) }
    var showDeleteDialog by remember { mutableStateOf<DdnsConfig?>(null) }
    var editingConfig by remember { mutableStateOf(DdnsConfig()) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.success) {
        uiState.success?.let { success ->
            android.widget.Toast.makeText(context, success, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            MiTopAppBar(
                title = "DDNS",
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
                    IconButton(onClick = { viewModel.loadDdnsConfig() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = Loc.s(R.string.s0322),
                            tint = MiTheme.TextSecondary
                        )
                    }
                    IconButton(onClick = {
                        editingConfig = DdnsConfig()
                        showAddDialog = true
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = Loc.s(R.string.s0328),
                            tint = MiColors.Primary
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
            OfflineBanner(isOffline = !viewModel.isNetworkAvailable)

            // 插件依赖提示
            uiState.pluginStatus?.let { status ->
                if (!status.isInstalled) {
                    PluginDependencyCard(
                        status = status.copy(
                            isInstalling = uiState.isInstallingPlugin,
                            installProgress = uiState.installProgress,
                            installMessage = uiState.installMessage
                        ),
                        onInstall = {
                            viewModel.installPlugin { success ->
                                if (success) {
                                    viewModel.checkPluginDependency()
                                }
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                MiLoadingState()
            } else if (uiState.error != null && uiState.ddnsConfigs.isEmpty()) {
                MiErrorState(
                    message = uiState.error ?: Loc.s(R.string.s0329),
                    onRetry = { viewModel.loadDdnsConfig() }
                )
            } else if (uiState.ddnsConfigs.isEmpty()) {
                MiEmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MiTheme.TextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    text = Loc.s(R.string.s0330)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.ddnsConfigs, key = { it.name }) { config ->
                        DdnsCard(
                            config = config,
                            onEdit = {
                                editingConfig = config
                                showEditDialog = config
                            },
                            onDelete = { showDeleteDialog = config }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        DdnsEditDialog(
            title = Loc.s(R.string.s0331),
            config = editingConfig,
            onConfigChange = { editingConfig = it },
            onConfirm = {
                if (DebounceUtils.canClick()) {
                    viewModel.addDdns(it)
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showEditDialog != null) {
        DdnsEditDialog(
            title = Loc.s(R.string.s0332),
            config = editingConfig,
            onConfigChange = { editingConfig = it },
            onConfirm = {
                val editName = showEditDialog?.name ?: return@DdnsEditDialog
                if (DebounceUtils.canClick()) {
                    viewModel.updateDdns(editName, it)
                    showEditDialog = null
                }
            },
            onDismiss = { showEditDialog = null }
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MiColors.Error)
            },
            title = { Text(stringResource(R.string.ddns_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.dialog_delete_ddns_confirm, showDeleteDialog?.name ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog?.name?.let { deleteName ->
                        if (DebounceUtils.canClick()) {
                            viewModel.deleteDdns(deleteName)
                            showDeleteDialog = null
                        }
                    }
                }) {
                    Text(stringResource(R.string.common_delete), color = MiColors.Error, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.common_cancel), color = MiTheme.TextTertiary)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun DdnsCard(
    config: DdnsConfig,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
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
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                gradient = MiColors.GradientCyan,
                size = 40.dp,
                iconSize = 20.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = config.domain,
                    fontSize = 12.sp,
                    color = MiTheme.TextTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiTag(
                        text = config.service,
                        backgroundColor = MiColors.Primary.copy(alpha = 0.1f),
                        textColor = MiColors.Primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (config.enabled) {
                        MiTag(
                            text = Loc.s(R.string.s0333),
                            backgroundColor = MiColors.Success.copy(alpha = 0.1f),
                            textColor = MiColors.Success
                        )
                    } else {
                        MiTag(
                            text = Loc.s(R.string.s0334),
                            backgroundColor = MiTheme.TextTertiary.copy(alpha = 0.1f),
                            textColor = MiTheme.TextTertiary
                        )
                    }
                }
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = Loc.s(R.string.s0335),
                        tint = MiColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Loc.s(R.string.s0303),
                        tint = MiColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DdnsEditDialog(
    title: String,
    config: DdnsConfig,
    onConfigChange: (DdnsConfig) -> Unit,
    onConfirm: (DdnsConfig) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = config.service,
                    onValueChange = { onConfigChange(config.copy(service = it)) },
                    label = { Text(stringResource(R.string.form_provider)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.domain,
                    onValueChange = { onConfigChange(config.copy(domain = it)) },
                    label = { Text(stringResource(R.string.form_domain)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.username,
                    onValueChange = { onConfigChange(config.copy(username = it)) },
                    label = { Text(stringResource(R.string.form_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.password,
                    onValueChange = { onConfigChange(config.copy(password = it)) },
                    label = { Text(stringResource(R.string.form_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.interfaceName,
                    onValueChange = { onConfigChange(config.copy(interfaceName = it)) },
                    label = { Text(stringResource(R.string.form_network_interface)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(config) }) {
                Text(stringResource(R.string.common_confirm), color = MiColors.Primary, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = MiTheme.TextTertiary)
            }
        },
        containerColor = Color.White
    )
}
