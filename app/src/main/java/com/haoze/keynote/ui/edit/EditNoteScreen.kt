package com.haoze.keynote.ui.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.haoze.keynote.viewmodel.EditNoteViewModel
import kotlinx.coroutines.launch
import com.haoze.keynote.ui.common.ActionRow
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.theme.SpacingTokens
import com.haoze.keynote.ui.common.ActionMenuDialog
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditNoteScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditNoteViewModel = koinViewModel()
) {
    val colors = LocalAppColors.current
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val isGenerating by viewModel.isGeneratingTags.collectAsState()
    val isGeneratingTitle by viewModel.isGeneratingTitle.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val summaries by viewModel.summaries.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()
    val isPolishing by viewModel.isPolishing.collectAsState()
    val isPreview by viewModel.isPreview.collectAsState()
    val noteFontSize by viewModel.noteFontSize.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val polishedText by viewModel.polishedText.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasHandledExit by remember { mutableStateOf(false) }

    fun handleExit() {
        if (hasHandledExit) return
        hasHandledExit = true
        viewModel.saveOrDeleteIfEmpty { onNavigateBack() }
    }

    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }
    LaunchedEffect(Unit) { viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(isSummarizing) { if (!isSummarizing && showMenu) showMenu = false }
    LaunchedEffect(isGenerating) { if (!isGenerating && showMenu) showMenu = false }
    LaunchedEffect(isPolishing) { if (!isPolishing && showMenu) showMenu = false }

    BackHandler { handleExit() }
    DisposableEffect(Unit) {
        onDispose {
            if (!hasHandledExit) {
                hasHandledExit = true
                viewModel.saveOrDeleteIfEmpty {}
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("编辑笔记") },
                    navigationIcon = {
                        IconButton(onClick = { handleExit() }) {
                            Icon(painterResource(R.drawable.ic_arrow_back_mirrored), contentDescription = "返回")
                        }
                    },
                    actions = {
                        EditNoteTopAppBarActions(
                            canUndo = canUndo,
                            isPreview = isPreview,
                            isSummarizing = isSummarizing,
                            isGenerating = isGenerating,
                            isPolishing = isPolishing,
                            showMenu = showMenu,
                            colors = colors,
                            title = title,
                            content = content,
                            context = context,
                            onUndo = { viewModel.undo() },
                            onSave = { viewModel.saveNoteWithFeedback() },
                            onTogglePreview = { viewModel.togglePreview() },
                            onShare = { showMenu = false },
                            onSummarize = { viewModel.summarizeNote() },
                            onGenerateTags = { viewModel.generateTags() },
                            onPolish = { viewModel.polishNote() },
                            onDelete = { showDeleteDialog = true; showMenu = false },
                            onMenuToggle = { showMenu = !showMenu }
                        )
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        if (isPreview && content.isNotBlank()) {
            EditNotePreviewContent(
                title = title,
                content = content,
                tags = tags,
                colors = colors,
                innerPadding = innerPadding,
                onRemoveTag = { viewModel.removeTag(it) }
            )
        } else {
            EditNoteEditContent(
                title = title,
                content = content,
                tags = tags,
                summaries = summaries,
                noteFontSize = noteFontSize,
                isGeneratingTitle = isGeneratingTitle,
                colors = colors,
                innerPadding = innerPadding,
                context = context,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                onTitleChanged = { viewModel.onTitleChanged(it) },
                onContentChanged = { viewModel.onContentChanged(it) },
                onGenerateTitle = { viewModel.generateTitleFromContent() },
                onRemoveSummary = { viewModel.removeSummary(it) },
                onUpdateSummary = { index, text -> viewModel.updateSummary(index, text) },
                onRemoveTag = { viewModel.removeTag(it) },
                onAddTag = { viewModel.addTag(it) }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除笔记") },
            text = { Text("确定要删除这篇笔记吗？删除后可在回收站中恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    hasHandledExit = true; viewModel.deleteNote(); showDeleteDialog = false; onNavigateBack()
                }) { Text("删除", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }

    if (polishedText != null) {
        PolishedTextDialog(
            polishedText = polishedText!!,
            colors = colors,
            onDismiss = { viewModel.dismissPolishedText() },
            onApply = { viewModel.applyPolishedText() }
        )
    }
}

@Composable
private fun EditNoteTopAppBarActions(
    canUndo: Boolean,
    isPreview: Boolean,
    isSummarizing: Boolean,
    isGenerating: Boolean,
    isPolishing: Boolean,
    showMenu: Boolean,
    colors: com.haoze.keynote.ui.theme.AppColors,
    title: String,
    content: String,
    context: Context,
    onUndo: () -> Unit,
    onSave: () -> Unit,
    onTogglePreview: () -> Unit,
    onShare: () -> Unit,
    onSummarize: () -> Unit,
    onGenerateTags: () -> Unit,
    onPolish: () -> Unit,
    onDelete: () -> Unit,
    onMenuToggle: () -> Unit
) {
    IconButton(onClick = onTogglePreview) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = if (isPreview) painterResource(R.drawable.ic_edit) else painterResource(R.drawable.ic_visibility),
                contentDescription = if (isPreview) "编辑" else "预览",
                modifier = Modifier.size(18.dp)
            )
            Text(if (isPreview) "编辑" else "预览", style = MaterialTheme.typography.labelSmall)
        }
    }
    IconButton(onClick = onMenuToggle) {
        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "更多")
    }

    if (showMenu) {
        EditNoteMoreDialog(
            canUndo = canUndo,
            isPreview = isPreview,
            isSummarizing = isSummarizing,
            isGenerating = isGenerating,
            isPolishing = isPolishing,
            colors = colors,
            title = title,
            content = content,
            context = context,
            onDismiss = onMenuToggle,
            onSave = onSave,
            onUndo = onUndo,
            onShare = {
                val text = buildString {
                    if (title.isNotBlank()) append(title).append("\n\n")
                    append(content)
                }
                context.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                    }, "分享笔记"))
            },
            onSummarize = onSummarize,
            onGenerateTags = onGenerateTags,
            onPolish = onPolish,
            onDelete = onDelete
        )
    }
}

@Composable
private fun EditNoteMoreDialog(
    canUndo: Boolean,
    isPreview: Boolean,
    isSummarizing: Boolean,
    isGenerating: Boolean,
    isPolishing: Boolean,
    colors: com.haoze.keynote.ui.theme.AppColors,
    title: String,
    content: String,
    context: Context,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onShare: () -> Unit,
    onSummarize: () -> Unit,
    onGenerateTags: () -> Unit,
    onPolish: () -> Unit,
    onDelete: () -> Unit
) {
    ActionMenuDialog(title = "更多操作", onDismiss = onDismiss) {
        // 编辑操作组
        ActionRow(
            icon = painterResource(R.drawable.ic_save),
            label = "保存",
            onClick = { onSave(); onDismiss() }
        )
        ActionRow(
            icon = painterResource(R.drawable.ic_undo),
            label = "撤回",
            onClick = { onUndo(); onDismiss() },
            enabled = canUndo
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))
        // 分享操作组
        ActionRow(
            icon = painterResource(R.drawable.ic_share),
            label = "分享",
            onClick = { onShare(); onDismiss() }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))
        // AI功能组
        ActionRow(
            icon = painterResource(R.drawable.ic_auto_awesome),
            label = "AI摘要",
            onClick = { onSummarize(); onDismiss() },
            enabled = !isSummarizing && !isPreview,
            isLoading = isSummarizing,
            loadingLabel = "摘要生成中..."
        )
        ActionRow(
            icon = painterResource(R.drawable.ic_label),
            label = "AI标签",
            onClick = { onGenerateTags(); onDismiss() },
            enabled = !isGenerating && !isPreview,
            isLoading = isGenerating,
            loadingLabel = "标签生成中..."
        )
        ActionRow(
            icon = painterResource(R.drawable.ic_auto_awesome),
            label = "AI润色",
            onClick = { onPolish(); onDismiss() },
            enabled = !isPolishing && !isPreview,
            isLoading = isPolishing,
            loadingLabel = "润色中..."
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))
        // 危险操作组
        ActionRow(
            icon = painterResource(R.drawable.ic_delete),
            label = "删除",
            onClick = { onDelete(); onDismiss() },
            isDestructive = true
        )
    }
}

@Composable
private fun EditNotePreviewContent(
    title: String,
    content: String,
    tags: List<com.haoze.keynote.data.db.entity.TagEntity>,
    colors: com.haoze.keynote.ui.theme.AppColors,
    innerPadding: PaddingValues,
    onRemoveTag: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {},
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                readOnly = true
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        MarkdownPreview(content = content, modifier = Modifier.weight(1f).fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        if (tags.isNotEmpty()) {
            Text("标签", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            TagChipRow(tags = tags, onRemoveTag = {}, showRemove = false)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun EditNoteEditContent(
    title: String,
    content: String,
    tags: List<com.haoze.keynote.data.db.entity.TagEntity>,
    summaries: List<String>,
    noteFontSize: Int,
    isGeneratingTitle: Boolean,
    colors: com.haoze.keynote.ui.theme.AppColors,
    innerPadding: PaddingValues,
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onGenerateTitle: () -> Unit,
    onRemoveSummary: (Int) -> Unit,
    onUpdateSummary: (Int, String) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onAddTag: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imeNestedScroll()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChanged,
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    if (title.isBlank() && content.isNotBlank()) {
                        if (isGeneratingTitle) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = onGenerateTitle) {
                                Icon(painterResource(R.drawable.ic_auto_awesome), contentDescription = "AI生成标题")
                            }
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = content,
            onValueChange = onContentChanged,
            label = { Text("正文") },
            textStyle = TextStyle(fontSize = noteFontSize.sp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
            maxLines = Int.MAX_VALUE
        )
        Spacer(modifier = Modifier.height(16.dp))

        summaries.forEachIndexed { index, summaryText ->
            SummaryCard(
                index = index,
                summaryText = summaryText,
                colors = colors,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                context = context,
                onRemove = { onRemoveSummary(index) },
                onUpdate = { text -> onUpdateSummary(index, text) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("标签", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        TagChipRow(tags = tags, onRemoveTag = onRemoveTag)

        var newTag by remember { mutableStateOf("") }
        OutlinedTextField(
            value = newTag,
            onValueChange = { newTag = it },
            label = { Text("添加标签") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            trailingIcon = {
                IconButton(onClick = {
                    if (newTag.isNotBlank()) { onAddTag(newTag.trim().removePrefix("#")); newTag = "" }
                }) { Icon(painterResource(R.drawable.ic_add), contentDescription = "添加标签") }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (newTag.isNotBlank()) { onAddTag(newTag.trim().removePrefix("#")); newTag = "" }
                }
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SummaryCard(
    index: Int,
    summaryText: String,
    colors: com.haoze.keynote.ui.theme.AppColors,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    onRemove: () -> Unit,
    onUpdate: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).combinedClickable(
            onClick = {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("summary", summaryText))
                coroutineScope.launch { snackbarHostState.showSnackbar("已复制到剪贴板") }
            },
            onLongClick = { showEditDialog = true }
        ),
        shape = RoundedCornerShape(SpacingTokens.statusPillRadius),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                val circledNumbers = listOf("\u2460", "\u2461", "\u2462", "\u2463", "\u2464", "\u2465", "\u2466", "\u2467", "\u2468", "\u2469")
                val label = if (index < 10) "AI \u6458\u8981 ${circledNumbers[index]}" else "AI \u6458\u8981 ${index + 1}"
                Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = summaryText, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "关闭摘要", modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showEditDialog) {
        var editedText by remember { mutableStateOf(summaryText) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑摘要") },
            text = { OutlinedTextField(value = editedText, onValueChange = { editedText = it }, modifier = Modifier.fillMaxWidth(), maxLines = 5) },
            confirmButton = { TextButton(onClick = { onUpdate(editedText); showEditDialog = false }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
private fun PolishedTextDialog(
    polishedText: String,
    colors: com.haoze.keynote.ui.theme.AppColors,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI润色结果") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.6f)
                    .verticalScroll(rememberScrollState())
            ) { Text(polishedText) }
        },
        confirmButton = { TextButton(onClick = onApply) { Text("替换") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        textContentColor = colors.onSurface,
        shape = RoundedCornerShape(28.dp),
    )
}
