package com.luanmuc.openwrtmanager.ui.ai_diagnostic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.ui.components.MiDimens
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.util.Loc
import kotlinx.coroutines.launch

/**
 * AI (Gemini) diagnostic screen — chat-style.
 *
 * - API key setup card when no key is saved
 * - "Smart diagnosis" button: collects router data and asks Gemini
 * - Free-form questions with optional router data attachment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDiagnosticScreen(
    onBack: () -> Unit,
    viewModel: AiDiagnosticViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val isCollecting by viewModel.isCollecting.collectAsState()
    val error by viewModel.error.collectAsState()
    val attachRouterData by viewModel.attachRouterData.collectAsState()

    var questionText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Show errors as toasts
    LaunchedEffect(error) {
        error?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Auto-scroll to the newest message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val apiKeyValue by viewModel.apiKey.collectAsState()
    val configuredNow = remember(apiKeyValue) { apiKeyValue.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_diagnostic_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244)
                        )
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = Loc.s(R.string.ai_clear_chat)
                            )
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==================== Messages list ====================
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = MiDimens.horizontalPadding,
                    vertical = MiDimens.itemSpacing
                )
            ) {
                if (!configuredNow) {
                    item { ApiKeySetupCard(viewModel) }
                }

                if (configuredNow && messages.isEmpty()) {
                    item { EmptyStateCard(onStart = { viewModel.startSmartDiagnosis() }) }
                }

                items(messages, key = { it.id }) { message ->
                    ChatBubble(message)
                    Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                }

                if (isCollecting) {
                    item {
                        StatusBubble(text = Loc.s(R.string.ai_collecting_data))
                        Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                    }
                }

                if (isThinking) {
                    item {
                        StatusBubble(text = Loc.s(R.string.ai_thinking))
                        Spacer(modifier = Modifier.height(MiDimens.itemSpacing))
                    }
                }
            }

            // ==================== Input bar ====================
            Surface(
                color = MiTheme.CardBackground,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MiDimens.horizontalPadding, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Smart diagnosis quick action
                        FilledTonalButton(
                            onClick = { viewModel.startSmartDiagnosis() },
                            enabled = configuredNow && !isThinking && !isCollecting,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Loc.s(R.string.ai_smart_diagnosis),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Toggle: attach router data to next question
                        FilterChip(
                            selected = attachRouterData,
                            onClick = { viewModel.toggleAttachRouterData() },
                            label = {
                                Icon(
                                    Icons.Default.Memory,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Loc.s(R.string.ai_attach_router_data),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = MiDimens.horizontalPadding,
                                end = MiDimens.horizontalPadding,
                                bottom = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(Loc.s(R.string.ai_input_hint), fontSize = 14.sp) },
                            maxLines = 4,
                            enabled = configuredNow && !isThinking && !isCollecting,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MiTheme.Primary,
                                unfocusedBorderColor = MiTheme.TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                viewModel.sendQuestion(questionText)
                                questionText = ""
                            },
                            enabled = configuredNow && questionText.isNotBlank() && !isThinking && !isCollecting,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = Loc.s(R.string.ai_send)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Components ====================

/**
 * API key setup card (shown when no key is configured).
 */
@Composable
private fun ApiKeySetupCard(viewModel: AiDiagnosticViewModel) {
    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MiTheme.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Loc.s(R.string.ai_setup_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MiTheme.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Loc.s(R.string.ai_setup_desc),
                fontSize = 13.sp,
                color = MiTheme.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("AIza...", fontSize = 14.sp) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                                    TextButton(onClick = { showKey = !showKey }) {
                        Text(
                            text = if (showKey) Loc.s(R.string.ai_hide) else Loc.s(R.string.ai_show),
                            fontSize = 12.sp
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.saveApiKey(keyInput) },
                enabled = keyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Loc.s(R.string.ai_save_key))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Loc.s(R.string.ai_setup_privacy),
                fontSize = 11.sp,
                color = MiTheme.TextSecondary
            )
        }
    }
}

/**
 * Empty state with a big "start diagnosis" action.
 */
@Composable
private fun EmptyStateCard(onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MiDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MiTheme.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = MiTheme.Primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Loc.s(R.string.ai_empty_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MiTheme.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Loc.s(R.string.ai_empty_desc),
                fontSize = 13.sp,
                color = MiTheme.TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Loc.s(R.string.ai_start_diagnosis))
            }
        }
    }
}

/**
 * A user / AI chat bubble.
 */
@Composable
private fun ChatBubble(message: AiChatMessage) {
    val isUser = message.isFromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MiTheme.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (isUser) MiTheme.Primary else MiTheme.CardBackground,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) androidx.compose.ui.graphics.Color.White else MiTheme.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Small bubble with a progress hint (collecting data / thinking).
 */
@Composable
private fun StatusBubble(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MiTheme.Primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MiTheme.CardBackground,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MiTheme.Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = MiTheme.TextSecondary
                )
            }
        }
    }
}
