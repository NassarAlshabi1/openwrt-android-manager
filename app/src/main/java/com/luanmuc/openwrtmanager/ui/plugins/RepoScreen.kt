package com.luanmuc.openwrtmanager.ui.plugins

import com.luanmuc.openwrtmanager.R

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.data.model.RepoInfo
import com.luanmuc.openwrtmanager.ui.components.MiButton
import com.luanmuc.openwrtmanager.ui.components.MiButtonType
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiEmptyState
import com.luanmuc.openwrtmanager.ui.components.MiListItem
import com.luanmuc.openwrtmanager.ui.components.MiSwitch
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.util.DebugMode
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 软件源管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoScreen(
    onBack: () -> Unit,
    viewModel: PluginsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var repoName by remember { mutableStateOf("") }
    var repoUrl by remember { mutableStateOf("") }

    // 加载软件源列表
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadRepos()
    }

    Scaffold(
        topBar = {
            MiTopAppBar(
                title = Loc.s(R.string.s0608),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244),
                            tint = MiTheme.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = Loc.s(R.string.s0328),
                            tint = MiTheme.Primary
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
            if (uiState.repos.isEmpty()) {
                MiEmptyState(
                    icon = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MiTheme.TextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    text = Loc.s(R.string.s0618)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 按类型分组
                    val officialRepos = uiState.repos.filter { it.repoType == com.luanmuc.openwrtmanager.data.model.RepoType.OFFICIAL_MIRROR }
                    val thirdPartyRepos = uiState.repos.filter { it.repoType == com.luanmuc.openwrtmanager.data.model.RepoType.THIRD_PARTY }
                    val customRepos = uiState.repos.filter { it.repoType == com.luanmuc.openwrtmanager.data.model.RepoType.CUSTOM }
                    
                    // 官方镜像源
                    if (officialRepos.isNotEmpty()) {
                        item {
                            Text(
                                text = Loc.s(R.string.s0619),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiTheme.TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(officialRepos) { repo ->
                            RepoItem(
                                repo = repo,
                                onToggle = { enabled ->
                                    viewModel.toggleRepo(repo.name, enabled)
                                },
                                onDelete = {
                                    viewModel.removeRepo(repo.name)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    
                    // 第三方插件源
                    if (thirdPartyRepos.isNotEmpty()) {
                        item {
                            Text(
                                text = Loc.s(R.string.s0620),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiTheme.TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(thirdPartyRepos) { repo ->
                            RepoItem(
                                repo = repo,
                                onToggle = { enabled ->
                                    viewModel.toggleRepo(repo.name, enabled)
                                },
                                onDelete = {
                                    viewModel.removeRepo(repo.name)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    
                    // 自定义源
                    if (customRepos.isNotEmpty()) {
                        item {
                            Text(
                                text = Loc.s(R.string.s0621),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiTheme.TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(customRepos) { repo ->
                            RepoItem(
                                repo = repo,
                                onToggle = { enabled ->
                                    viewModel.toggleRepo(repo.name, enabled)
                                },
                                onDelete = {
                                    viewModel.removeRepo(repo.name)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加软件源对话框
    if (showAddDialog) {
        AlertDialog(
            containerColor = MiTheme.CardBackground,
            titleContentColor = MiTheme.TextPrimary,
            textContentColor = MiTheme.TextSecondary,
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = Loc.s(R.string.s0622),
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = repoName,
                        onValueChange = { repoName = it },
                        label = { Text(stringResource(R.string.form_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        label = { Text(stringResource(R.string.form_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (repoName.isNotEmpty() && repoUrl.isNotEmpty()) {
                            viewModel.addRepo(repoName, repoUrl)
                            repoName = ""
                            repoUrl = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_add), color = MiTheme.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = MiTheme.TextSecondary)
                }
            }
        )
    }
}

/**
 * 软件源列表项
 */
@Composable
private fun RepoItem(
    repo: RepoInfo,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    MiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = repo.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiTheme.TextPrimary
                    )
                    Text(
                        text = repo.url,
                        fontSize = 12.sp,
                        color = MiTheme.TextTertiary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Loc.s(R.string.s0303),
                        tint = MiTheme.Error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s(R.string.s0623),
                    fontSize = 14.sp,
                    color = MiTheme.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                MiSwitch(
                    checked = repo.enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}
