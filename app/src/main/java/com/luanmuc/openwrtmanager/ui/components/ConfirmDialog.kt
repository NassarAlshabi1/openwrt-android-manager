package com.luanmuc.openwrtmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 确认对话框组件
 * 用于危险操作的二次确认
 */
@Composable
fun ConfirmDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String = Loc.s(R.string.s0300),
    cancelText: String = Loc.s(R.string.s0276),
    icon: ImageVector? = null,
    isDangerous: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDangerous) MiTheme.Error else MiTheme.Primary
                    )
                }
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MiTheme.TextPrimary
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTheme.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDangerous) MiTheme.Error else MiTheme.Primary
                    )
                ) {
                    Text(text = confirmText)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MiTheme.TextSecondary
                    )
                ) {
                    Text(text = cancelText)
                }
            },
            containerColor = MiTheme.CardBackground,
            shape = RoundedCornerShape(MiDimens.cardRadius)
        )
    }
}

/**
 * 删除确认对话框
 */
@Composable
fun DeleteConfirmDialog(
    show: Boolean,
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        show = show,
        title = Loc.s(R.string.s0301),
        message = Loc.s(R.string.s0302, itemName),
        confirmText = Loc.s(R.string.s0303),
        cancelText = Loc.s(R.string.s0276),
        isDangerous = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * 重置确认对话框
 */
@Composable
fun ResetConfirmDialog(
    show: Boolean,
    title: String = Loc.s(R.string.s0304),
    message: String = Loc.s(R.string.s0305),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        show = show,
        title = title,
        message = message,
        confirmText = Loc.s(R.string.s0306),
        cancelText = Loc.s(R.string.s0276),
        isDangerous = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * 退出确认对话框
 */
@Composable
fun ExitConfirmDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        show = show,
        title = Loc.s(R.string.s0307),
        message = Loc.s(R.string.s0308),
        confirmText = Loc.s(R.string.s0309),
        cancelText = Loc.s(R.string.s0276),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * 信息对话框
 */
@Composable
fun InfoDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String = Loc.s(R.string.s0310),
    icon: ImageVector? = null,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MiTheme.Primary
                    )
                }
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MiTheme.TextPrimary
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTheme.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MiTheme.Primary
                    )
                ) {
                    Text(text = confirmText)
                }
            },
            containerColor = MiTheme.CardBackground,
            shape = RoundedCornerShape(MiDimens.cardRadius)
        )
    }
}

/**
 * 成功对话框
 */
@Composable
fun SuccessDialog(
    show: Boolean,
    title: String = Loc.s(R.string.s0260),
    message: String = "",
    onDismiss: () -> Unit
) {
    InfoDialog(
        show = show,
        title = title,
        message = message,
        icon = Icons.Default.CheckCircle,
        onDismiss = onDismiss
    )
}

/**
 * 错误对话框
 */
@Composable
fun ErrorDialog(
    show: Boolean,
    title: String = Loc.s(R.string.s0311),
    message: String = "",
    onDismiss: () -> Unit
) {
    InfoDialog(
        show = show,
        title = title,
        message = message,
        icon = Icons.Default.Error,
        onDismiss = onDismiss
    )
}

/**
 * 输入对话框
 */
@Composable
fun InputDialog(
    show: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    confirmText: String = Loc.s(R.string.s0300),
    cancelText: String = Loc.s(R.string.s0276),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MiTheme.TextPrimary
                )
            },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        placeholder = {
                            Text(
                                text = placeholder,
                                color = MiTheme.TextTertiary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MiTheme.Primary,
                            unfocusedBorderColor = MiTheme.Divider
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MiTheme.Primary
                    )
                ) {
                    Text(text = confirmText)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MiTheme.TextSecondary
                    )
                ) {
                    Text(text = cancelText)
                }
            },
            containerColor = MiTheme.CardBackground,
            shape = RoundedCornerShape(MiDimens.cardRadius)
        )
    }
}