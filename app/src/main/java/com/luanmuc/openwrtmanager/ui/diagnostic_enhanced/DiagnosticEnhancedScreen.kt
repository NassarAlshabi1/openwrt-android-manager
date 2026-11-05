package com.luanmuc.openwrtmanager.ui.diagnostic_enhanced

import com.luanmuc.openwrtmanager.R

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.data.model.DiagnosticCategory
import com.luanmuc.openwrtmanager.data.model.DiagnosticStatus
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 智能诊断增强页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticEnhancedScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticEnhancedViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    // 错误提示
    LaunchedEffect(error) {
        error?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    val isScanning by viewModel.isScanning.collectAsState()
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostic_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = Loc.s(R.string.s0244))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MiTheme.CardBackground,
                    titleContentColor = MiTheme.TextPrimary
                )
            )
        },
        containerColor = MiTheme.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(MiDimens.horizontalPadding)
        ) {
            // 体检评分卡片
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                ScoreCard(
                    score = diagnosticResult?.overallScore ?: 0,
                    status = diagnosticResult?.overallStatus ?: DiagnosticStatus.GOOD,
                    isScanning = isScanning,
                    onStartScan = { viewModel.startFullDiagnostic() },
                    viewModel = viewModel
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            // 诊断项分类
            diagnosticResult?.let { result ->
                item {
                    CategoryTabs(
                        categories = DiagnosticCategory.values().toList(),
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.setSelectedCategory(it) }
                    )
                    
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                }
                
                // 诊断项列表
                val filteredItems = if (selectedCategory != null) {
                    result.items.filter { it.category == selectedCategory }
                } else {
                    result.items
                }
                
                items(filteredItems) { item ->
                    DiagnosticItemCard(
                        item = item,
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 优化建议
            item {
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                
                SectionTitle(
                    title = Loc.s(R.string.s0378),
                    icon = Icons.Default.AutoFixHigh
                )
                
                Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MiTheme.Primary)
                    }
                }
            } else if (suggestions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Loc.s(R.string.s0399),
                            color = MiTheme.TextTertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(suggestions.take(5)) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onAutoFix = { viewModel.autoFix(suggestion.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 评分卡片
 */
@Composable
private fun ScoreCard(
    score: Int,
    status: DiagnosticStatus,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    viewModel: DiagnosticEnhancedViewModel
) {
    val statusColor = viewModel.getStatusColor(status)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiDimens.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 评分圆环
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = statusColor,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Text(
                            text = "$score",
                            color = statusColor,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Loc.s(R.string.s0400),
                            color = MiTheme.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isScanning) Loc.s(R.string.s0401) else status.displayName,
                color = MiTheme.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onStartScan,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MiTheme.Primary
                ),
                shape = RoundedCornerShape(MiDimens.buttonRadius)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) Loc.s(R.string.s0402) else Loc.s(R.string.s0403))
            }
        }
    }
}

/**
 * 分类标签
 */
@Composable
private fun CategoryTabs(
    categories: List<DiagnosticCategory>,
    selectedCategory: DiagnosticCategory?,
    onCategorySelected: (DiagnosticCategory?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 全部
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(MiDimens.smallRadius))
                    .background(
                        if (selectedCategory == null) MiTheme.Primary else MiTheme.CardBackground
                    )
                    .clickable { onCategorySelected(null) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Loc.s(R.string.s0404),
                    color = if (selectedCategory == null) androidx.compose.ui.graphics.Color.White else MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (selectedCategory == null) FontWeight.Medium else FontWeight.Normal
                )
            }
            
            // 各分类（只显示前3个，避免太挤）
            categories.take(3).forEach { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(MiDimens.smallRadius))
                        .background(
                            if (isSelected) MiTheme.Primary else MiTheme.CardBackground
                        )
                        .clickable { onCategorySelected(category) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.displayName,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else MiTheme.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 诊断项卡片
 */
@Composable
private fun DiagnosticItemCard(
    item: com.luanmuc.openwrtmanager.data.model.DiagnosticItem,
    viewModel: DiagnosticEnhancedViewModel
) {
    val statusColor = viewModel.getStatusColor(item.status)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiDimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态图标
            Icon(
                if (item.status == DiagnosticStatus.GOOD) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = item.status.displayName,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 诊断项信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = MiTheme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.message,
                    color = MiTheme.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 状态标签
            Text(
                text = item.status.displayName,
                color = statusColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 分区标题
 */
@Composable
private fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MiTheme.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = MiTheme.TextSecondary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 建议卡片
 */
@Composable
private fun SuggestionCard(
    suggestion: com.luanmuc.openwrtmanager.data.model.DiagnosticSuggestion,
    onAutoFix: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MiTheme.CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiDimens.cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分类标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MiTheme.Primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = suggestion.category.displayName,
                        color = MiTheme.Primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 优先级
                Text(
                    text = Loc.s(R.string.s0405, suggestion.priority),
                    color = MiTheme.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = suggestion.title,
                color = MiTheme.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = suggestion.description,
                color = MiTheme.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            
            if (suggestion.canAutoFix) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onAutoFix,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MiTheme.Primary
                        )
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.common_one_click_fix))
                    }
                }
            }
        }
    }
}
