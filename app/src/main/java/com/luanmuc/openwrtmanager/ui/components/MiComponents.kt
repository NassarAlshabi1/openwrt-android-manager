package com.luanmuc.openwrtmanager.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

// ========== 小米风格颜色 ==========
object MiColors {
    val Primary = Color(0xFF1677FF)
    val PrimaryDark = Color(0xFF0958D9)
    val PrimaryLight = Color(0xFF4096FF)
    val Background = Color(0xFFF5F7FA)
    val CardBackground = Color.White
    val TextPrimary = Color(0xFF1D2129)
    val TextSecondary = Color(0xFF4E5969)
    val TextTertiary = Color(0xFF86909C)
    val Divider = Color(0xFFE5E6EB)
    val Success = Color(0xFF00B578)
    val Warning = Color(0xFFFF7D00)
    val Error = Color(0xFFF53F3F)
    val Purple = Color(0xFF722ED1)
    val Cyan = Color(0xFF13C2C2)
    val Orange = Color(0xFFFF7D00)
    val Info = Color(0xFF1677FF)
    
    // 功能图标渐变色
    val GradientBlue = Brush.linearGradient(listOf(Color(0xFF1677FF), Color(0xFF4096FF)))
    val GradientGreen = Brush.linearGradient(listOf(Color(0xFF00B578), Color(0xFF36CFC9)))
    val GradientOrange = Brush.linearGradient(listOf(Color(0xFFFF7D00), Color(0xFFFFA940)))
    val GradientPurple = Brush.linearGradient(listOf(Color(0xFF722ED1), Color(0xFF9254DE)))
    val GradientRed = Brush.linearGradient(listOf(Color(0xFFF53F3F), Color(0xFFFF7875)))
    val GradientCyan = Brush.linearGradient(listOf(Color(0xFF13C2C2), Color(0xFF36CFC9)))
    val GradientGold = Brush.linearGradient(listOf(Color(0xFFFAAD14), Color(0xFFFFC53D)))
}

// ========== 深色模式颜色 ==========
object MiColorsDark {
    val Primary = Color(0xFF3385FF)
    val PrimaryDark = Color(0xFF1677FF)
    val PrimaryLight = Color(0xFF5CADFF)
    val Background = Color(0xFF17171A)
    val CardBackground = Color(0xFF232324)
    val TextPrimary = Color(0xFFF2F3F5)
    val TextSecondary = Color(0xFFC9CDD4)
    val TextTertiary = Color(0xFF86909C)
    val Divider = Color(0xFF3A3A3C)
    val Success = Color(0xFF00B578)
    val Warning = Color(0xFFFF7D00)
    val Error = Color(0xFFF53F3F)
    val Purple = Color(0xFF9254DE)
    val Cyan = Color(0xFF36CFC9)
    val Orange = Color(0xFFFFA940)
    val Info = Color(0xFF3385FF)
    
    // 功能图标渐变色
    val GradientBlue = Brush.linearGradient(listOf(Color(0xFF1677FF), Color(0xFF4096FF)))
    val GradientGreen = Brush.linearGradient(listOf(Color(0xFF00B578), Color(0xFF36CFC9)))
    val GradientOrange = Brush.linearGradient(listOf(Color(0xFFFF7D00), Color(0xFFFFA940)))
    val GradientPurple = Brush.linearGradient(listOf(Color(0xFF722ED1), Color(0xFF9254DE)))
    val GradientRed = Brush.linearGradient(listOf(Color(0xFFF53F3F), Color(0xFFFF7875)))
    val GradientCyan = Brush.linearGradient(listOf(Color(0xFF13C2C2), Color(0xFF36CFC9)))
    val GradientGold = Brush.linearGradient(listOf(Color(0xFFFAAD14), Color(0xFFFFC53D)))
}

// ========== 主题状态 ==========
object MiTheme {
    private val _isDarkMode = mutableStateOf(false)
    var isDarkMode: Boolean
        get() = _isDarkMode.value
        set(value) { _isDarkMode.value = value }
    
    // 根据当前主题返回对应的颜色
    val colors: MiColorsTheme
        get() = if (_isDarkMode.value) DarkColors else LightColors
    
    // 统一的形状
    val shapes: MiShapes
        get() = DefaultShapes
    
    // 便捷属性
    val Primary: Color get() = if (_isDarkMode.value) MiColorsDark.Primary else MiColors.Primary
    val Background: Color get() = if (_isDarkMode.value) MiColorsDark.Background else MiColors.Background
    val CardBackground: Color get() = if (_isDarkMode.value) MiColorsDark.CardBackground else MiColors.CardBackground
    val TextPrimary: Color get() = if (_isDarkMode.value) MiColorsDark.TextPrimary else MiColors.TextPrimary
    val TextSecondary: Color get() = if (_isDarkMode.value) MiColorsDark.TextSecondary else MiColors.TextSecondary
    val TextTertiary: Color get() = if (_isDarkMode.value) MiColorsDark.TextTertiary else MiColors.TextTertiary
    val Divider: Color get() = if (_isDarkMode.value) MiColorsDark.Divider else MiColors.Divider
    val Success: Color get() = if (_isDarkMode.value) MiColorsDark.Success else MiColors.Success
    val Warning: Color get() = if (_isDarkMode.value) MiColorsDark.Warning else MiColors.Warning
    val Error: Color get() = if (_isDarkMode.value) MiColorsDark.Error else MiColors.Error
    val Purple: Color get() = if (_isDarkMode.value) MiColorsDark.Purple else MiColors.Purple
    val Cyan: Color get() = if (_isDarkMode.value) MiColorsDark.Cyan else MiColors.Cyan
    val Orange: Color get() = if (_isDarkMode.value) MiColorsDark.Orange else MiColors.Orange
}

// ========== 小米风格形状 ==========
data class MiShapes(
    val cardShape: Shape,
    val buttonShape: Shape,
    val inputShape: Shape,
    val iconShape: Shape,
    val smallShape: Shape
)

private val DefaultShapes = MiShapes(
    cardShape = RoundedCornerShape(MiDimens.cardRadius),
    buttonShape = RoundedCornerShape(MiDimens.buttonRadius),
    inputShape = RoundedCornerShape(MiDimens.inputRadius),
    iconShape = RoundedCornerShape(MiDimens.iconRadius),
    smallShape = RoundedCornerShape(MiDimens.smallRadius)
)

// 主题颜色接口
data class MiColorsTheme(
    val Primary: Color,
    val Background: Color,
    val CardBackground: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextTertiary: Color,
    val Divider: Color,
    val Success: Color,
    val Warning: Color,
    val Error: Color,
    val Purple: Color,
    val Cyan: Color,
    val Orange: Color
)

private val LightColors = MiColorsTheme(
    Primary = MiColors.Primary,
    Background = MiColors.Background,
    CardBackground = MiColors.CardBackground,
    TextPrimary = MiColors.TextPrimary,
    TextSecondary = MiColors.TextSecondary,
    TextTertiary = MiColors.TextTertiary,
    Divider = MiColors.Divider,
    Success = MiColors.Success,
    Warning = MiColors.Warning,
    Error = MiColors.Error,
    Purple = MiColors.Purple,
    Cyan = MiColors.Cyan,
    Orange = MiColors.Orange
)

private val DarkColors = MiColorsTheme(
    Primary = MiColorsDark.Primary,
    Background = MiColorsDark.Background,
    CardBackground = MiColorsDark.CardBackground,
    TextPrimary = MiColorsDark.TextPrimary,
    TextSecondary = MiColorsDark.TextSecondary,
    TextTertiary = MiColorsDark.TextTertiary,
    Divider = MiColorsDark.Divider,
    Success = MiColorsDark.Success,
    Warning = MiColorsDark.Warning,
    Error = MiColorsDark.Error,
    Purple = MiColorsDark.Purple,
    Cyan = MiColorsDark.Cyan,
    Orange = MiColorsDark.Orange
)

// ========== 小米风格尺寸 ==========
object MiDimens {
    val cardRadius = 16.dp
    val buttonRadius = 12.dp
    val inputRadius = 12.dp
    val iconRadius = 12.dp
    val smallRadius = 8.dp
    
    val horizontalPadding = 16.dp
    val verticalPadding = 12.dp
    val cardPadding = 16.dp
    val itemSpacing = 12.dp
    
    val iconSizeSmall = 20.dp
    val iconSizeMedium = 24.dp
    val iconSizeLarge = 32.dp
    val iconSizeXLarge = 48.dp
}

// ========== 小米风格卡片 ==========
@Composable
fun MiCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MiDimens.cardRadius),
    backgroundColor: Color = MiTheme.CardBackground,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}

// ========== 按钮类型枚举 ==========
enum class MiButtonType {
    Primary,
    Secondary,
    Text
}

// ========== 小米风格通用按钮 ==========
@Composable
fun MiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: MiButtonType = MiButtonType.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
) {
    when (type) {
        MiButtonType.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(MiDimens.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MiTheme.Primary,
                    contentColor = Color.White,
                    disabledContainerColor = MiTheme.Primary.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon
                )
            }
        }
        MiButtonType.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(MiDimens.buttonRadius),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MiTheme.TextSecondary,
                    disabledContentColor = MiTheme.TextTertiary
                ),
                border = BorderStroke(1.dp, MiColors.Divider),
                contentPadding = contentPadding
            ) {
                ButtonContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon
                )
            }
        }
        MiButtonType.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MiColors.Primary,
                    disabledContentColor = MiTheme.TextTertiary
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon
                )
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: @Composable (() -> Unit)?
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MiColors.Primary,
            strokeWidth = 2.dp
        )
    } else {
        leadingIcon?.let {
            it()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ========== 小米风格主按钮 ==========
@Composable
fun MiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(MiDimens.buttonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MiTheme.Primary,
            contentColor = Color.White,
            disabledContainerColor = MiTheme.Primary.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ========== 小米风格次要按钮 ==========
@Composable
fun MiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(MiDimens.buttonRadius),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MiColors.Primary
        ),
        border = BorderStroke(1.dp, MiColors.Primary)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ========== 小米风格文字按钮 ==========
@Composable
fun MiTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MiColors.Primary
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = color)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ========== 小米风格输入框 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    supportingText: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MiColors.TextSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = MiColors.TextTertiary,
                    fontSize = 15.sp
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(MiDimens.inputRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MiColors.Primary,
                unfocusedBorderColor = MiColors.Divider,
                errorBorderColor = MiColors.Error,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF7F8FA),
                errorContainerColor = Color(0xFFFFF1F0),
                cursorColor = MiColors.Primary,
                focusedTextColor = MiColors.TextPrimary,
                unfocusedTextColor = MiColors.TextPrimary
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
        )
        if (supportingText.isNotEmpty()) {
            Text(
                text = supportingText,
                fontSize = 12.sp,
                color = if (isError) MiColors.Error else MiColors.TextTertiary,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}

// ========== 小米风格开关 ==========
@Composable
fun MiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MiColors.Primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFE5E6EB),
            checkedBorderColor = MiColors.Primary,
            uncheckedBorderColor = Color(0xFFE5E6EB)
        )
    )
}

// ========== 小米风格列表项 ==========
@Composable
fun MiListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showArrow: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                it()
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MiColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MiColors.TextTertiary
                )
            }
        }
        trailing?.invoke()
        if (showArrow && trailing == null && onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = MiColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ========== 小米风格分割线 ==========
@Composable
fun MiDivider(
    modifier: Modifier = Modifier,
    indent: Dp = 0.dp
) {
    Divider(
        modifier = modifier.padding(start = indent),
        color = MiColors.Divider,
        thickness = 0.5.dp
    )
}

// ========== 小米风格功能图标 ==========
@Composable
fun MiFeatureIcon(
    icon: @Composable () -> Unit,
    gradient: Brush,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(MiDimens.iconRadius))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

// ========== 小米风格标签 ==========
@Composable
fun MiTag(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MiTheme.Primary.copy(alpha = 0.1f),
    textColor: Color = MiColors.Primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ========== 小米风格进度条 ==========
@Composable
fun MiLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MiTheme.Primary,
    trackColor: Color = Color(0xFFF2F3F5),
    height: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

// ========== 小米风格空状态 ==========
@Composable
fun MiEmptyState(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MiColors.TextTertiary
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF2F3F5)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MiColors.TextTertiary
        )
    }
}

// ========== 小米风格加载状态 ==========
@Composable
fun MiLoadingState(
    text: String = Loc.s(R.string.s0312),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MiColors.Primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MiColors.TextTertiary
        )
    }
}

// ========== 小米风格错误状态 ==========
@Composable
fun MiErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MiTheme.Error.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MiColors.Error,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MiColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        MiTextButton(text = Loc.s(R.string.s0313), onClick = onRetry)
    }
}

// ========== 小米风格顶部AppBar ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiColors.TextPrimary
            )
        },
        modifier = modifier,
        actions = actions,
        navigationIcon = navigationIcon,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MiColors.Background,
            titleContentColor = MiColors.TextPrimary,
            actionIconContentColor = MiColors.TextSecondary
        )
    )
}

// ========== 小米风格底部导航 ==========
@Composable
fun MiBottomNavigation(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    items: List<MiNavItem>,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (selectedIndex == index) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MiColors.Primary,
                    selectedTextColor = MiColors.Primary,
                    unselectedIconColor = MiColors.TextTertiary,
                    unselectedTextColor = MiColors.TextTertiary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class MiNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
