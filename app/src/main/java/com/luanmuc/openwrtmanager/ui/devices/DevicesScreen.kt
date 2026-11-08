package com.luanmuc.openwrtmanager.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiPrimaryButton
import com.luanmuc.openwrtmanager.ui.components.MiTag
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 设备页 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onAddRouter: () -> Unit,
    viewModel: DevicesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<Router?>(null) }
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = stringResource(R.string.devices_title)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRouter,
                containerColor = MiColors.Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.devices_add))
            }
        },
        containerColor = MiTheme.Background
    ) { padding ->
        if (uiState.routers.isEmpty() && !uiState.isLoading) {
            EmptyDevicesView(
                onAddRouter = onAddRouter,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = Loc.s(R.string.s0350),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiTheme.TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(
                    items = uiState.routers,
                    key = { router -> router.id }
                ) { router ->
                    RouterItem(
                        router = router,
                        isActive = router.id == uiState.activeRouterId,
                        onSelect = { viewModel.setActiveRouter(router.id) },
                        onDelete = { showDeleteDialog = router }
                    )
                }
            }
        }
    }
    
    // 删除确认对话框
    showDeleteDialog?.let { router ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MiColors.Error
                )
            },
            title = { Text(stringResource(R.string.dialog_delete_device)) },
            text = {
                Text(stringResource(R.string.dialog_delete_router_confirm, router.name.ifEmpty { router.address }))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRouter(router.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MiColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.common_cancel), color = MiTheme.TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun EmptyDevicesView(
    onAddRouter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
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
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = Loc.s(R.string.s0351),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MiTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = Loc.s(R.string.s0352),
            fontSize = 14.sp,
            color = MiTheme.TextTertiary
        )
        Spacer(modifier = Modifier.height(32.dp))
        MiPrimaryButton(
            text = stringResource(R.string.devices_add),
            onClick = onAddRouter
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = Loc.s(R.string.s0354),
            fontSize = 12.sp,
            color = MiTheme.TextTertiary
        )
    }
}

@Composable
fun RouterItem(
    router: Router,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) MiColors.Primary.copy(alpha = 0.08f) else Color.White)
            .clickable(onClick = onSelect)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiFeatureIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                gradient = if (isActive) MiColors.GradientBlue else Brush.linearGradient(listOf(MiTheme.TextTertiary, MiTheme.TextSecondary)),
                size = 48.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = router.name.ifEmpty { Loc.s(R.string.s0355) },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${router.username}@${router.address}",
                    fontSize = 13.sp,
                    color = MiTheme.TextTertiary
                )
            }
            if (isActive) {
                MiTag(
                    text = Loc.s(R.string.s0356),
                    backgroundColor = MiColors.Primary,
                    textColor = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = Loc.s(R.string.s0335),
                    tint = MiTheme.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = Loc.s(R.string.s0303),
                    tint = MiColors.Error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
