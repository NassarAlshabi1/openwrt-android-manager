package com.luanmuc.openwrtmanager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 编辑模式工具栏
 */
@Composable
fun EditModeToolbar(
    onDone: () -> Unit,
    onReset: () -> Unit,
    onPreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiDimens.cardRadius))
            .background(MiColors.Primary.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MiColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Loc.s(R.string.s0475),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MiColors.Primary
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onPreset) {
                Text(
                    text = Loc.s(R.string.s0476),
                    fontSize = 13.sp,
                    color = MiColors.Primary
                )
            }
            TextButton(onClick = onReset) {
                Text(
                    text = Loc.s(R.string.s0306),
                    fontSize = 13.sp,
                    color = MiColors.TextSecondary
                )
            }
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MiColors.Primary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = Loc.s(R.string.s0024),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 仪表板卡片包装器
 * 支持编辑模式下的拖拽、删除、大小调整
 */
@Composable
fun DashboardCardWrapper(
    card: DashboardCard,
    isEditMode: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSizeChange: (CardSize) -> Unit,
    onDragStart: (() -> Unit)? = null,
    onDragEnd: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showSizeMenu by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxWidth()) {
        // 卡片内容
        content()
        
        // 编辑模式覆盖层
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(MiDimens.cardRadius))
                    .background(MiColors.Primary.copy(alpha = if (isDragging) 0.15f else 0.05f))
                    .border(
                        width = if (isDragging) 2.dp else 1.dp,
                        color = MiColors.Primary.copy(alpha = if (isDragging) 0.8f else 0.3f),
                        shape = RoundedCornerShape(MiDimens.cardRadius)
                    )
            )
            
            // 左上角删除按钮
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiColors.Error)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = Loc.s(R.string.s0477),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // 左下角拖拽手柄
            IconButton(
                onClick = { showSizeMenu = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDragging) MiColors.Primary else MiColors.Background)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = Loc.s(R.string.s0478),
                    tint = if (isDragging) Color.White else MiColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // 上下移动按钮
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiColors.Background)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = Loc.s(R.string.s0479),
                        tint = MiColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Divider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = MiColors.TextTertiary.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = Loc.s(R.string.s0480),
                        tint = MiColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // 大小调整菜单
            if (showSizeMenu) {
                DropdownMenu(
                    expanded = showSizeMenu,
                    onDismissRequest = { showSizeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Loc.s(R.string.s0481)) },
                        onClick = {
                            onSizeChange(CardSize.SMALL)
                            showSizeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(Loc.s(R.string.s0482)) },
                        onClick = {
                            onSizeChange(CardSize.MEDIUM)
                            showSizeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(Loc.s(R.string.s0483)) },
                        onClick = {
                            onSizeChange(CardSize.LARGE)
                            showSizeMenu = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 添加卡片按钮
 */
@Composable
fun AddCardButton(
    hiddenCards: List<DashboardCard>,
    onAddCard: (CardType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showMenu = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MiColors.Primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Loc.s(R.string.s0484),
                fontSize = 14.sp
            )
        }
        
        if (showMenu && hiddenCards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(MiDimens.cardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MiColors.Background
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    hiddenCards.forEach { card ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddCard(card.id)
                                    showMenu = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCardIcon(card.id),
                                contentDescription = null,
                                tint = MiColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = card.id.displayName,
                                fontSize = 14.sp,
                                color = MiColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = Loc.s(R.string.s0328),
                                tint = MiColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (card.id != hiddenCards.last().id) {
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MiColors.TextTertiary.copy(alpha = 0.2f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 根据卡片类型获取图标
 */
fun getCardIcon(cardType: CardType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (cardType) {
        CardType.ROUTER_STATUS -> Icons.Default.Router
        CardType.NETWORK_SPEED -> Icons.Default.Speed
        CardType.QUICK_ACTIONS -> Icons.Default.GridView
        CardType.SYSTEM_STATUS -> Icons.Default.Memory
        CardType.ONLINE_DEVICES -> Icons.Default.Devices
        CardType.WIFI_STATUS -> Icons.Default.Wifi
        CardType.PLUGINS -> Icons.Default.Extension
        CardType.FIREWALL -> Icons.Default.Security
        CardType.DDNS -> Icons.Default.Dns
        CardType.PORT_STATUS -> Icons.Default.Settings
    }
}

/**
 * 预设布局选择对话框
 */
@Composable
fun PresetLayoutDialog(
    onDismiss: () -> Unit,
    onSelect: (PresetLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Loc.s(R.string.s0485),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PresetLayout.values().forEach { layout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(layout)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (layout) {
                                PresetLayout.DEFAULT -> Icons.Default.ViewAgenda
                                PresetLayout.COMPACT -> Icons.Default.DensityMedium
                                PresetLayout.DETAILED -> Icons.Default.ViewList
                                PresetLayout.MINIMAL -> Icons.Default.HorizontalRule
                            },
                            contentDescription = null,
                            tint = MiColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = layout.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = getLayoutDescription(layout),
                                fontSize = 12.sp,
                                color = MiColors.TextTertiary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.s(R.string.s0276))
            }
        },
        modifier = modifier
    )
}

/**
 * 获取布局描述
 */
fun getLayoutDescription(layout: PresetLayout): String {
    return when (layout) {
        PresetLayout.DEFAULT -> Loc.s(R.string.s0486)
        PresetLayout.COMPACT -> Loc.s(R.string.s0487)
        PresetLayout.DETAILED -> Loc.s(R.string.s0488)
        PresetLayout.MINIMAL -> Loc.s(R.string.s0489)
    }
}
