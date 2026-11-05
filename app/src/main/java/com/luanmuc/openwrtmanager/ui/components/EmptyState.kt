package com.luanmuc.openwrtmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * 空状态组件
 * 用于列表为空、加载失败等场景
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String = "",
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MiDimens.horizontalPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MiTheme.TextTertiary
        )
        
        Spacer(modifier = Modifier.height(MiDimens.itemSpacing * 2))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MiTheme.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MiTheme.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        
        if (action != null) {
            Spacer(modifier = Modifier.height(MiDimens.itemSpacing * 2))
            action()
        }
    }
}

/**
 * 加载中状态组件
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = Loc.s(R.string.s0312)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MiDimens.horizontalPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = MiTheme.Primary,
            strokeWidth = 3.dp
        )
        
        Spacer(modifier = Modifier.height(MiDimens.itemSpacing * 2))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MiTheme.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 错误状态组件
 */
@Composable
fun ErrorState(
    icon: ImageVector,
    title: String,
    description: String = "",
    modifier: Modifier = Modifier,
    retryAction: (() -> Unit)? = null,
    retryText: String = Loc.s(R.string.s0313)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MiDimens.horizontalPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MiTheme.Error
        )
        
        Spacer(modifier = Modifier.height(MiDimens.itemSpacing * 2))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MiTheme.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MiTheme.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        
        if (retryAction != null) {
            Spacer(modifier = Modifier.height(MiDimens.itemSpacing * 2))
            
            androidx.compose.material3.Button(
                onClick = retryAction,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MiTheme.Primary
                )
            ) {
                Text(text = retryText)
            }
        }
    }
}

/**
 * 离线状态组件
 */
@Composable
fun OfflineState(
    modifier: Modifier = Modifier,
    message: String = Loc.s(R.string.s0314),
    description: String = Loc.s(R.string.s0315)
) {
    ErrorState(
        icon = Icons.Default.WifiOff,
        title = message,
        description = description,
        modifier = modifier
    )
}