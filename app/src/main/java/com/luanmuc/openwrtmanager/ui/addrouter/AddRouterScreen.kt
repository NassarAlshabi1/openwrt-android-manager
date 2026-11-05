package com.luanmuc.openwrtmanager.ui.addrouter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.ui.components.MiCard
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiFeatureIcon
import com.luanmuc.openwrtmanager.ui.components.MiPrimaryButton
import com.luanmuc.openwrtmanager.ui.components.MiLinearProgress
import com.luanmuc.openwrtmanager.ui.components.MiTextField
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 添加路由器页面 - 小米路由器风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRouterScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    routerId: String? = null,
    viewModel: AddRouterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }
    
    // 编辑模式：加载路由器信息
    LaunchedEffect(routerId) {
        if (routerId != null) {
            viewModel.loadRouter(routerId)
        }
    }
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = if (uiState.isEditMode) Loc.s(R.string.s0243) else stringResource(R.string.add_router_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244),
                            tint = MiTheme.TextPrimary
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 头部图标和说明
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MiFeatureIcon(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    gradient = MiColors.GradientBlue,
                    size = 72.dp,
                    iconSize = 36.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Loc.s(R.string.s0245),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Loc.s(R.string.s0246),
                    fontSize = 14.sp,
                    color = MiTheme.TextTertiary
                )
            }
            
            // 设备名称（可选）
            MiTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = stringResource(R.string.add_router_name),
                placeholder = stringResource(R.string.add_router_name_hint),
                supportingText = Loc.s(R.string.s0247),
                leadingIcon = {
                    Icon(
                        Icons.Default.Label,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            
            // 路由器地址
            MiTextField(
                value = uiState.address,
                onValueChange = { viewModel.onAddressChange(it) },
                label = stringResource(R.string.add_router_address),
                placeholder = stringResource(R.string.add_router_address_hint),
                isError = uiState.error != null && uiState.address.isBlank(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                leadingIcon = {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            
            // 用户名
            MiTextField(
                value = uiState.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = stringResource(R.string.add_router_username),
                placeholder = stringResource(R.string.add_router_username_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            
            // 密码（可选，支持无密码登录）
            MiTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = stringResource(R.string.add_router_password),
                placeholder = stringResource(R.string.add_router_password_hint),
                supportingText = Loc.s(R.string.s0248),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MiTheme.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) Loc.s(R.string.s0249) else Loc.s(R.string.s0250),
                            tint = MiTheme.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
            
            // 错误提示
            uiState.error?.let { error ->
                MiCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MiColors.Error.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MiColors.Error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            fontSize = 13.sp,
                            color = MiColors.Error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 预加载进度
            if (uiState.isPreloading) {
                MiCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MiColors.Primary.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MiColors.Primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Loc.s(R.string.s0251),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiTheme.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.preloadCurrentItem,
                                    fontSize = 13.sp,
                                    color = MiTheme.TextSecondary
                                )
                            }
                            Text(
                                text = "${(uiState.preloadProgress * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiColors.Primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        MiLinearProgress(
                            progress = uiState.preloadProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Loc.s(R.string.s0252, uiState.preloadCurrent, uiState.preloadTotal),
                            fontSize = 12.sp,
                            color = MiTheme.TextTertiary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // 连接按钮
                MiPrimaryButton(
                    text = if (uiState.isConnecting) "" else stringResource(R.string.add_router_connect),
                    onClick = { viewModel.connectAndSave(onSuccess) },
                    enabled = !uiState.isConnecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    leadingIcon = {
                        if (uiState.isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
            
            // 安全说明
            MiCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MiColors.Primary.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MiColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = Loc.s(R.string.s0253),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiTheme.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Loc.s(R.string.s0254),
                            fontSize = 12.sp,
                            color = MiTheme.TextTertiary
                        )
                    }
                }
            }
        }
    }
}
