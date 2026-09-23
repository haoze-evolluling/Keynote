package com.haoze.keynote.ui.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.haoze.keynote.R
import com.haoze.keynote.data.db.entity.AIChatConversationEntity
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.LocalDarkModeManager
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.theme.SpacingTokens
import com.haoze.keynote.ui.common.ActionMenuDialog
import com.haoze.keynote.ui.common.ActionRow
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * AI 对话页（主界面首页）。
 * 输入框底部预留悬浮导航栏空间，避免底栏遮挡输入框。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    viewModel: AIChatViewModel = koinViewModel(),
    onCreateNote: (Long) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val darkModeManager = LocalDarkModeManager.current
    val isDarkMode = darkModeManager.isDarkMode()
    val messages by viewModel.messages.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val billMessages by viewModel.billMessages.collectAsState()
    val plannerMessages by viewModel.plannerMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCreatingBill by viewModel.isCreatingBill.collectAsState()
    val pendingBill by viewModel.pendingBill.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currentAssistant by viewModel.currentAssistant.collectAsState()
    val historyConversations by viewModel.historyConversations.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isCreatingNote by remember { mutableStateOf(false) }
    val assistantText = assistantUiText(currentAssistant)
    var showNewConversationConfirm by remember { mutableStateOf(false) }
    var showDeleteConversationConfirm by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var assistantMenuExpanded by remember { mutableStateOf(false) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
            if (lastVisibleItem >= messages.size - 2) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.createdNoteId.collect { noteId ->
            isCreatingNote = false
            if (noteId > 0) onCreateNote(noteId)
        }
    }

    // 呼吸感渐变动画：切换助手时先淡出再淡入，形成明显呼吸效果
    val glowAlphaAnim = remember { Animatable(1f) }

    LaunchedEffect(currentAssistant) {
        if (messages.isEmpty()) {
            // 第一阶段：缓慢淡出（消失）
            glowAlphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1200, easing = LinearEasing)
            )
            // 短暂停顿，增强节奏感
            delay(300)
            // 第二阶段：缓慢淡入（出现），形成呼吸感
            glowAlphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
            )
        }
    }
    val glowAlpha = glowAlphaAnim.value

    Box(modifier = Modifier.fillMaxSize()) {
        ChatBackground(
            messagesEmpty = messages.isEmpty(),
            glowAlpha = glowAlpha,
            isDarkMode = isDarkMode
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("AI 对话")
                                Box {
                                    val arrowRotation by animateFloatAsState(
                                        targetValue = if (assistantMenuExpanded) 180f else 0f,
                                        animationSpec = tween(200),
                                        label = "arrowRotation"
                                    )
                                    IconButton(onClick = { assistantMenuExpanded = true }) {
                                        Icon(
                                            painterResource(R.drawable.ic_arrow_drop_down),
                                            contentDescription = "切换助手",
                                            modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                                        )
                                    }
                                }
                            }
                            Text(
                                text = assistantText.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = colors.surface.copy(alpha = 0.85f),
                        navigationIconContentColor = colors.onSurface,
                        titleContentColor = colors.onSurface,
                        actionIconContentColor = colors.onSurface
                    ),
                    actions = {
                        IconButton(onClick = { actionsMenuExpanded = true }) {
                            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "更多操作")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                )

                if (assistantMenuExpanded) {
                    ActionMenuDialog(title = "切换助手", onDismiss = { assistantMenuExpanded = false }) {
                        listOf(AssistantType.CHAT, AssistantType.BILL, AssistantType.PLANNER).forEach { type ->
                            ActionRow(
                                icon = painterResource(if (type == currentAssistant) R.drawable.ic_check else R.drawable.ic_auto_awesome),
                                label = assistantUiText(type).menuLabel,
                                onClick = {
                                    viewModel.switchAssistant(type)
                                    assistantMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (actionsMenuExpanded) {
                    ActionMenuDialog(title = "更多操作", onDismiss = { actionsMenuExpanded = false }) {
                        ActionRow(painterResource(R.drawable.ic_history_outlined), "历史对话", {
                            actionsMenuExpanded = false
                            showHistoryDialog = true
                        })
                        if (messages.isNotEmpty()) {
                            ActionRow(painterResource(R.drawable.ic_edit_outlined), "保存对话", {
                                actionsMenuExpanded = false
                                isCreatingNote = true
                                viewModel.createNoteFromMessages()
                            }, enabled = !isCreatingNote)
                            ActionRow(painterResource(R.drawable.ic_delete_outlined), "删除对话", {
                                actionsMenuExpanded = false
                                showDeleteConversationConfirm = true
                            }, isDestructive = true)
                        }
                        ActionRow(painterResource(R.drawable.ic_refresh_outlined), "重新生成", {
                            actionsMenuExpanded = false
                            viewModel.regenerateLastResponse()
                        }, enabled = !isLoading && messages.any { it.role == "user" })
                        ActionRow(painterResource(R.drawable.ic_chat_outlined_mirrored), "新对话", {
                            actionsMenuExpanded = false
                            if (messages.isEmpty()) viewModel.clearMessages() else showNewConversationConfirm = true
                        })
                    }
                }
            }
        ) { innerPadding ->
            val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val isImeVisible = imeBottomDp > 0.dp

            // 悬浮底栏高度 64.dp，底边距 16.dp，共占用 navBottomDp + 80.dp 空间。
            // 预留 12.dp 悬浮呼吸空隙，底栏可见时避让距离为 80.dp + 12.dp = 92.dp。
            // 当软键盘弹出时，底栏淡出隐藏，输入框避让距离平滑降为 0.dp，贴合键盘。
            val bottomBarClearance by animateDpAsState(
                targetValue = if (isImeVisible) 0.dp else 92.dp,
                animationSpec = tween(durationMillis = 200),
                label = "bottomBarClearance"
            )
            val inputBottomPadding = maxOf(navBottomDp + bottomBarClearance, imeBottomDp + 10.dp)

            LaunchedEffect(isImeVisible) {
                if (isImeVisible && messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Crossfade(
                        targetState = currentAssistant,
                        animationSpec = tween(300)
                    ) { assistant ->
                        val displayMessages = when (assistant) {
                            AssistantType.CHAT -> chatMessages
                            AssistantType.BILL -> billMessages
                            AssistantType.PLANNER -> plannerMessages
                        }
                        if (displayMessages.isEmpty()) {
                            ChatWelcomeSection(
                                colors = colors,
                                assistant = assistant,
                                onQuickPrompt = { prompt ->
                                    inputText = ""
                                    viewModel.sendMessage(prompt)
                                }
                            )
                        } else {
                            ChatMessageList(displayMessages, isLoading, isCreatingBill, viewModel, colors, listState)
                        }
                    }
                }

                ChatInputBar(
                    inputText = inputText,
                    isLoading = isLoading,
                    placeholder = assistantText.inputPlaceholder,
                    colors = colors,
                    modifier = Modifier.padding(bottom = inputBottomPadding),
                    onInputChange = { inputText = it },
                    onSend = { viewModel.sendMessage(inputText); inputText = "" }
                )
            }

            if (pendingBill != null) {
                PendingBillDialog(
                    bill = pendingBill!!,
                    categories = categories,
                    colors = colors,
                    onDismiss = { viewModel.dismissPendingBill() },
                    onConfirm = { categoryId -> viewModel.confirmCreateBill(categoryId) }
                )
            }

            if (showNewConversationConfirm) {
                AlertDialog(
                    onDismissRequest = { showNewConversationConfirm = false },
                    title = { Text("开始新对话") },
                    containerColor = colors.surface,
                    textContentColor = colors.onSurface,
                    shape = RoundedCornerShape(28.dp),
                    text = { Text("当前对话已自动保存到历史记录。开始新对话后，可以从历史对话继续回来。") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.clearMessages()
                            showNewConversationConfirm = false
                        }) { Text("开始新对话") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewConversationConfirm = false }) { Text("取消") }
                    }
                )
            }

            if (showDeleteConversationConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConversationConfirm = false },
                    title = { Text("删除对话") },
                    containerColor = colors.surface,
                    textContentColor = colors.onSurface,
                    shape = RoundedCornerShape(28.dp),
                    text = { Text("删除后会进入回收站，可在回收站中恢复或永久删除。") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteCurrentConversation()
                            showDeleteConversationConfirm = false
                        }) { Text("移入回收站", color = colors.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConversationConfirm = false }) { Text("取消") }
                    }
                )
            }

            if (showHistoryDialog) {
                AIChatHistoryDialog(
                    conversations = historyConversations,
                    onDismiss = { showHistoryDialog = false },
                    onOpen = { conversation ->
                        viewModel.loadConversation(conversation)
                        showHistoryDialog = false
                    },
                    onDelete = { conversation -> viewModel.deleteConversation(conversation) }
                )
            }
        }
    }
}

@Composable
private fun AIChatHistoryDialog(
    conversations: List<AIChatConversationEntity>,
    onDismiss: () -> Unit,
    onOpen: (AIChatConversationEntity) -> Unit,
    onDelete: (AIChatConversationEntity) -> Unit
) {
    val colors = LocalAppColors.current
    var query by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val filtered = remember(conversations, query) {
        val keyword = query.trim()
        if (keyword.isBlank()) conversations else conversations.filter { it.title.contains(keyword, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("历史对话") },
        containerColor = colors.surface,
        textContentColor = colors.onSurface,
        shape = RoundedCornerShape(28.dp),
        text = {
            DialogContent(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索历史对话") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search_outlined), contentDescription = null) }
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = if (query.isBlank()) "暂无历史对话" else "没有匹配的历史对话",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { conversation ->
                            AIChatHistoryRow(
                                conversation = conversation,
                                dateFormat = dateFormat,
                                onOpen = { onOpen(conversation) },
                                onDelete = { onDelete(conversation) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun AIChatHistoryRow(
    conversation: AIChatConversationEntity,
    dateFormat: SimpleDateFormat,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    val assistantLabel = runCatching {
        assistantUiText(AssistantType.valueOf(conversation.assistantType)).menuLabel
    }.getOrDefault("AI 对话")

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(SpacingTokens.listCardRadius),
        color = colors.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_history_outlined), contentDescription = null, tint = colors.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title.ifBlank { "新对话" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$assistantLabel · ${dateFormat.format(Date(conversation.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete_outlined), contentDescription = "删除对话", tint = colors.error)
            }
        }
    }
}

@Composable
private fun ChatBackground(
    messagesEmpty: Boolean,
    glowAlpha: Float,
    isDarkMode: Boolean
) {
    // 呼吸律动：使光影呈现如生命般柔和的起伏微动
    val infiniteTransition = rememberInfiniteTransition(label = "GeminiGlowBreathing")
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathPhase"
    )

    // 对话中有消息时依然保留底部柔和的氛围光，空状态时呈现完整辉光
    val stateAlpha = if (messagesEmpty) 1.0f else 0.85f
    val currentGlowAlpha = (glowAlpha * stateAlpha).coerceIn(0f, 1f)

    val baseBgColor = if (isDarkMode) Color(0xFF000000) else Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBgColor)
            .drawWithCache {
                val longestSide = max(size.width, size.height)
                val breathScale = 0.96f + 0.08f * breathPhase
                val breathShiftY = 0.015f * (breathPhase - 0.5f)

                // 1. 垂直天幕大气渐变（精准复刻 Gemini 色谱）
                val verticalAtmosphere = if (isDarkMode) {
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.52f to Color.Transparent,
                            0.62f to Color(0xFF020409).copy(alpha = 0.70f),
                            0.72f to Color(0xFF060D22),
                            0.82f to Color(0xFF0C1638),
                            0.92f to Color(0xFF111E48),
                            1.00f to Color(0xFF142252)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                } else {
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.50f to Color.Transparent,
                            0.62f to Color(0xFFF1F7FD),
                            0.72f to Color(0xFFDEEFFD),
                            0.82f to Color(0xFFBEDFFE),
                            0.92f to Color(0xFFA2D3FE),
                            1.00f to Color(0xFF98CCFE)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                }

                // 2. 底部中央核心主光穹（Light Dome）
                val centerCenter = Offset(
                    x = size.width * 0.50f,
                    y = size.height * (1.02f + breathShiftY)
                )
                val centerRadius = longestSide * 0.76f * breathScale
                val centerDomeBrush = if (isDarkMode) {
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF1E4085).copy(alpha = 0.80f),
                            0.32f to Color(0xFF152F66).copy(alpha = 0.55f),
                            0.60f to Color(0xFF0A1738).copy(alpha = 0.28f),
                            1.00f to Color.Transparent
                        ),
                        center = centerCenter,
                        radius = centerRadius
                    )
                } else {
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF88BEF8).copy(alpha = 0.75f),
                            0.32f to Color(0xFFA8D3FC).copy(alpha = 0.50f),
                            0.62f to Color(0xFFD6EBFE).copy(alpha = 0.22f),
                            1.00f to Color.Transparent
                        ),
                        center = centerCenter,
                        radius = centerRadius
                    )
                }

                // 3. 底部两翼漫射光（模拟两边轻微向上的弧形光晕）
                val leftWingCenter = Offset(
                    x = 0f,
                    y = size.height * (0.96f + breathShiftY * 0.5f)
                )
                val leftWingRadius = longestSide * 0.54f * breathScale
                val leftWingBrush = if (isDarkMode) {
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF122452).copy(alpha = 0.50f),
                            0.55f to Color(0xFF071028).copy(alpha = 0.22f),
                            1.00f to Color.Transparent
                        ),
                        center = leftWingCenter,
                        radius = leftWingRadius
                    )
                } else {
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFFA4D1FC).copy(alpha = 0.42f),
                            0.55f to Color(0xFFCEE6FE).copy(alpha = 0.18f),
                            1.00f to Color.Transparent
                        ),
                        center = leftWingCenter,
                        radius = leftWingRadius
                    )
                }

                val rightWingCenter = Offset(
                    x = size.width,
                    y = size.height * (0.96f + breathShiftY * 0.5f)
                )
                val rightWingRadius = longestSide * 0.54f * breathScale
                val rightWingBrush = if (isDarkMode) {
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF122452).copy(alpha = 0.50f),
                            0.55f to Color(0xFF071028).copy(alpha = 0.22f),
                            1.00f to Color.Transparent
                        ),
                        center = rightWingCenter,
                        radius = rightWingRadius
                    )
                } else {
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFFA4D1FC).copy(alpha = 0.42f),
                            0.55f to Color(0xFFCEE6FE).copy(alpha = 0.18f),
                            1.00f to Color.Transparent
                        ),
                        center = rightWingCenter,
                        radius = rightWingRadius
                    )
                }

                onDrawBehind {
                    if (currentGlowAlpha > 0f) {
                        // 绘制垂直色谱大气
                        drawRect(brush = verticalAtmosphere, alpha = currentGlowAlpha)
                        // 绘制中央主光穹
                        drawCircle(
                            brush = centerDomeBrush,
                            radius = centerRadius,
                            center = centerCenter,
                            alpha = currentGlowAlpha
                        )
                        // 绘制左翼漫射
                        drawCircle(
                            brush = leftWingBrush,
                            radius = leftWingRadius,
                            center = leftWingCenter,
                            alpha = currentGlowAlpha
                        )
                        // 绘制右翼漫射
                        drawCircle(
                            brush = rightWingBrush,
                            radius = rightWingRadius,
                            center = rightWingCenter,
                            alpha = currentGlowAlpha
                        )
                    }
                }
            }
    )
}

@Composable
private fun ChatWelcomeSection(
    colors: com.haoze.keynote.ui.theme.AppColors,
    assistant: AssistantType,
    onQuickPrompt: (String) -> Unit
) {
    val text = assistantUiText(assistant)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "KeyNote Logo",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text.welcomeTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                text.quickPrompts.forEach { prompt ->
                    AssistChip(
                        onClick = { onQuickPrompt(prompt) },
                        label = {
                            Text(
                                prompt,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.ic_auto_awesome_outlined),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isCreatingBill: Boolean,
    viewModel: AIChatViewModel,
    colors: com.haoze.keynote.ui.theme.AppColors,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            ChatMessageBubble(message, isCreatingBill, colors, viewModel)
        }
        if (isLoading) {
            item { ChatLoadingIndicator(colors) }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isCreatingBill: Boolean,
    colors: com.haoze.keynote.ui.theme.AppColors,
    viewModel: AIChatViewModel
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isUser) {
            Avatar(painterResource(R.drawable.ic_psychology_outlined), colors.primaryContainer, colors.primary)
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUser) colors.primaryContainer else colors.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 0.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column {
                Text(text = message.content, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                if (!isUser) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = colors.outlineVariant.copy(alpha = 0.3f)
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.createNoteFromContent(message.content) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(painterResource(R.drawable.ic_note_add_outlined_mirrored), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("存为笔记", style = MaterialTheme.typography.labelMedium)
                        }
                        if (message.isBillRelated) {
                            TextButton(
                                onClick = { viewModel.prepareBillFromAI(message.billJson) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                enabled = !isCreatingBill
                            ) {
                                Icon(painterResource(R.drawable.ic_receipt_outlined), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("创建账单", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (message.isScheduleRelated) {
                            TextButton(
                                onClick = { viewModel.createScheduleFromAI(message.scheduleJson) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(painterResource(R.drawable.ic_calendar_month_outlined), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("创建日程", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (message.isTodoRelated) {
                            TextButton(
                                onClick = { viewModel.createTodoFromAI(message.todoJson) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(painterResource(R.drawable.ic_check_circle_outlined), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("创建待办", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Avatar(painterResource(R.drawable.ic_person_outlined), colors.primaryContainer, colors.primary)
        }
    }
}

@Composable
private fun ChatLoadingIndicator(colors: com.haoze.keynote.ui.theme.AppColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(painterResource(R.drawable.ic_psychology_outlined), colors.primaryContainer, colors.primary)
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("思考中...", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Avatar(
    icon: androidx.compose.ui.graphics.painter.Painter,
    containerColor: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    placeholder: String,
    colors: com.haoze.keynote.ui.theme.AppColors,
    modifier: Modifier = Modifier,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val darkModeManager = LocalDarkModeManager.current
    val isDarkMode = darkModeManager.isDarkMode()

    val containerBg = if (isDarkMode) {
        Color(0xFF1E1F24).copy(alpha = 0.94f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.96f)
    }
    val borderStroke = if (isDarkMode) {
        BorderStroke(1.dp, Color(0xFF2E313C).copy(alpha = 0.7f))
    } else {
        BorderStroke(1.dp, Color(0xFFD6E3F2).copy(alpha = 0.7f))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = containerBg,
        border = borderStroke,
        shadowElevation = if (isDarkMode) 2.dp else 4.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder, color = colors.onSurfaceVariant.copy(alpha = 0.75f)) },
                singleLine = true,
                enabled = !isLoading,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            FilledIconButton(
                onClick = onSend,
                enabled = !isLoading && inputText.isNotBlank(),
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isDarkMode) Color(0xFF265096) else colors.primary,
                    contentColor = Color.White,
                    disabledContainerColor = if (isDarkMode) Color(0xFF282B34) else colors.surfaceVariant.copy(alpha = 0.6f),
                    disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    painterResource(R.drawable.ic_keyboard_return_mirrored),
                    contentDescription = "发送",
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
private fun PendingBillDialog(
    bill: PendingBill,
    categories: List<com.haoze.keynote.data.db.entity.CategoryEntity>,
    colors: com.haoze.keynote.ui.theme.AppColors,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    var selectedCategoryId by remember(bill) {
        mutableStateOf(categories.find { it.name == bill.suggestedCategory }?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认创建账单") },
        containerColor = colors.surface,
        textContentColor = colors.onSurface,
        shape = RoundedCornerShape(28.dp),
        text = {
            DialogContent(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("消费项目", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                    Text(bill.item, style = ModalTokens.bodyTextStyle)
                    Text("金额", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                    Text("¥${String.format("%.2f", bill.amount)}", style = ModalTokens.bodyTextStyle)
                    Text("时间", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                    Text(
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(bill.date)),
                        style = ModalTokens.bodyTextStyle
                    )
                }
                Text("类别", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == category.id) null else category.id
                            },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedCategoryId) }) { Text("确认创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
