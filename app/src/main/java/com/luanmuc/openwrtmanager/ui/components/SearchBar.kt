package com.luanmuc.openwrtmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 搜索栏组件
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = Loc.s(R.string.s0325),
    onSearch: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MiTheme.CardBackground,
                shape = RoundedCornerShape(MiDimens.inputRadius)
            )
            .padding(horizontal = MiDimens.horizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MiDimens.itemSpacing)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MiTheme.TextTertiary
        )
        
        Text(
            text = query.ifEmpty { placeholder },
            style = MaterialTheme.typography.bodyMedium,
            color = if (query.isEmpty()) MiTheme.TextTertiary else MiTheme.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        if (query.isNotEmpty() && onClear != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    tint = MiTheme.TextTertiary
                )
            }
        }
    }
}

/**
 * 可输入的搜索框组件
 */
@Composable
fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = Loc.s(R.string.s0325),
    onSearch: (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = MiTheme.TextTertiary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MiTheme.TextTertiary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary
                    )
                }
            }
        },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke()
            }
        ),
        shape = RoundedCornerShape(MiDimens.inputRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MiTheme.Primary,
            unfocusedBorderColor = MiTheme.Divider,
            focusedContainerColor = MiTheme.CardBackground,
            unfocusedContainerColor = MiTheme.CardBackground
        )
    )
}

/**
 * 搜索结果统计组件
 */
@Composable
fun SearchResultCount(
    count: Int,
    modifier: Modifier = Modifier,
    keyword: String = ""
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MiDimens.horizontalPadding, vertical = MiDimens.verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (keyword.isNotEmpty()) {
                Loc.s(R.string.s0326, count, keyword)
            } else {
                Loc.s(R.string.s0327, count)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MiTheme.TextSecondary
        )
    }
}

/**
 * 搜索过滤标签组件
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    val backgroundColor = if (selected) MiTheme.Primary else MiTheme.CardBackground
    val contentColor = if (selected) Color.White else MiTheme.TextSecondary
    
    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor
        )
        
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 高亮搜索关键词
 */
@Composable
fun HighlightText(
    text: String,
    keyword: String,
    modifier: Modifier = Modifier,
    highlightColor: Color = MiTheme.Primary
) {
    if (keyword.isEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            color = MiTheme.TextPrimary
        )
        return
    }
    
    val lowerText = text.lowercase()
    val lowerKeyword = keyword.lowercase()
    val index = lowerText.indexOf(lowerKeyword)
    
    if (index == -1) {
        Text(
            text = text,
            modifier = modifier,
            color = MiTheme.TextPrimary
        )
        return
    }
    
    Row(modifier = modifier) {
        if (index > 0) {
            Text(
                text = text.substring(0, index),
                color = MiTheme.TextPrimary
            )
        }
        
        Text(
            text = text.substring(index, index + keyword.length),
            color = highlightColor
        )
        
        if (index + keyword.length < text.length) {
            Text(
                text = text.substring(index + keyword.length),
                color = MiTheme.TextPrimary
            )
        }
    }
}