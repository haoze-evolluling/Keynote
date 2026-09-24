package com.haoze.keynote.ui.toolbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.haoze.keynote.R
import com.haoze.keynote.data.db.entity.KnowledgeVaultEntity
import com.haoze.keynote.ui.components.AppAlertDialog as AlertDialog
import com.haoze.keynote.ui.components.AppConfirmDialog
import com.haoze.keynote.ui.components.AppDialogButton
import com.haoze.keynote.ui.components.IconCircle
import com.haoze.keynote.ui.components.MiniBadge
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.home.SearchBar
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.theme.SpacingTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeVaultScreen(
    onBack: () -> Unit = {}
) {
    val viewModel: KnowledgeVaultViewModel = koinViewModel()
    val colors = LocalAppColors.current
    val knowledgeItems by viewModel.knowledgeItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("全部") }
    var showAddEditDialog by remember { mutableStateOf<KnowledgeVaultEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<KnowledgeVaultEntity?>(null) }
    var inputMap by remember { mutableStateOf(mapOf<String, String>()) }

    val filterOptions = listOf("全部", "阅读清单", "摘录卡片", "灵感收件箱", "来源档案")

    val reading = knowledgeItems.filter { it.category == "阅读清单" }
    val quotes = knowledgeItems.filter { it.category == "摘录卡片" }
    val ideas = knowledgeItems.filter { it.category == "灵感收件箱" }
    val sources = knowledgeItems.filter { it.category == "来源档案" }

    val filteredItems = remember(knowledgeItems, searchQuery, selectedFilter) {
        knowledgeItems.filter { item ->
            val matchSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.note.contains(searchQuery, ignoreCase = true) ||
                    item.source.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            val matchFilter = selectedFilter == "全部" || item.category == selectedFilter
            matchSearch && matchFilter
        }
    }

    SettingsScaffold(
        title = "资料库",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddEditDialog = KnowledgeVaultEntity(
                        title = "",
                        source = "",
                        category = if (selectedFilter != "全部") selectedFilter else "阅读清单",
                        note = ""
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "添加资料")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. 顶部搜索栏：对齐 HomeScreen 规范
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChanged = { searchQuery = it },
                    placeholder = "搜索资料库内容..."
                )
            }

            // 搜索状态下直接显示搜索结果
            if (searchQuery.isNotBlank()) {
                item {
                    SettingsGroupTitle("搜索结果 (${filteredItems.size})")
                    SettingsGroup {
                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "未找到匹配的资料",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        } else {
                            filteredItems.forEachIndexed { index, item ->
                                VaultItemRow(
                                    item = item,
                                    showCategoryBadge = true,
                                    onEdit = { showAddEditDialog = item },
                                    onDelete = { itemToDelete = item }
                                )
                                if (index < filteredItems.lastIndex) {
                                    SettingsDivider()
                                }
                            }
                        }
                    }
                }
            } else {
                // 2. 资料概览统计卡片网格：2x2 排列，对齐 HabitSummary / BillStats 风格
                item {
                    SettingsGroupTitle("资料概览")
                    VaultOverviewSection(
                        readingCount = reading.size,
                        quotesCount = quotes.size,
                        ideasCount = ideas.size,
                        sourcesCount = sources.size,
                        selectedFilter = selectedFilter,
                        onSelectFilter = { category ->
                            selectedFilter = if (selectedFilter == category) "全部" else category
                        }
                    )
                }

                // 3. 分类筛选平铺芯片
                item {
                    SettingsGroupTitle("分类筛选")
                    VaultFilterRow(
                        filterOptions = filterOptions,
                        selectedFilter = selectedFilter,
                        onSelectFilter = { selectedFilter = it }
                    )
                }

                // 4. 各分类独立卡片模块
                val categoriesToShow = if (selectedFilter == "全部") {
                    listOf(
                        Triple("阅读清单", R.drawable.ic_link, reading),
                        Triple("摘录卡片", R.drawable.ic_chat_bubble_outline, quotes),
                        Triple("灵感收件箱", R.drawable.ic_lightbulb, ideas),
                        Triple("来源档案", R.drawable.ic_archive, sources)
                    )
                } else {
                    val single = when (selectedFilter) {
                        "阅读清单" -> Triple("阅读清单", R.drawable.ic_link, reading)
                        "摘录卡片" -> Triple("摘录卡片", R.drawable.ic_chat_bubble_outline, quotes)
                        "灵感收件箱" -> Triple("灵感收件箱", R.drawable.ic_lightbulb, ideas)
                        "来源档案" -> Triple("来源档案", R.drawable.ic_archive, sources)
                        else -> null
                    }
                    listOfNotNull(single)
                }

                categoriesToShow.forEach { (catName, _, catItems) ->
                    item {
                        SettingsGroupTitle("$catName (${catItems.size})")
                        SettingsGroup {
                            if (catItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "暂无条目，可在下方快速添加",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            } else {
                                catItems.forEachIndexed { index, item ->
                                    VaultItemRow(
                                        item = item,
                                        showCategoryBadge = false,
                                        onEdit = { showAddEditDialog = item },
                                        onDelete = { itemToDelete = item }
                                    )
                                    if (index < catItems.lastIndex) {
                                        SettingsDivider()
                                    }
                                }
                            }

                            // 卡片底部胶囊内嵌快速输入框
                            SettingsDivider()
                            VaultQuickInputRow(
                                category = catName,
                                value = inputMap[catName] ?: "",
                                onValueChange = { inputMap = inputMap + (catName to it) },
                                onAdd = { text ->
                                    viewModel.saveKnowledgeItem(
                                        KnowledgeVaultEntity(
                                            title = text.take(24),
                                            source = if (catName == "阅读清单" || catName == "来源档案") text else "",
                                            category = catName,
                                            note = text
                                        )
                                    )
                                    inputMap = inputMap + (catName to "")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 全功能新建 / 编辑资料弹窗
    if (showAddEditDialog != null) {
        KnowledgeVaultItemDialog(
            initialItem = showAddEditDialog!!,
            onDismiss = { showAddEditDialog = null },
            onConfirm = { updatedItem ->
                viewModel.saveKnowledgeItem(updatedItem)
                showAddEditDialog = null
            }
        )
    }

    // 删除确认弹窗
    if (itemToDelete != null) {
        val item = itemToDelete!!
        AppConfirmDialog(
            onDismissRequest = { itemToDelete = null },
            title = "删除资料",
            message = "确定要删除「${item.title.ifBlank { item.note }.take(20)}」吗？",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteKnowledgeItem(item)
                itemToDelete = null
            }
        )
    }
}

/**
 * 资料概览：2x2 排列的统计小卡片网格
 */
@Composable
private fun VaultOverviewSection(
    readingCount: Int,
    quotesCount: Int,
    ideasCount: Int,
    sourcesCount: Int,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VaultStatCard(
                title = "阅读清单",
                count = readingCount,
                icon = painterResource(R.drawable.ic_link),
                accentColor = colors.primary,
                isSelected = selectedFilter == "阅读清单",
                onClick = { onSelectFilter("阅读清单") },
                modifier = Modifier.weight(1f)
            )
            VaultStatCard(
                title = "摘录卡片",
                count = quotesCount,
                icon = painterResource(R.drawable.ic_chat_bubble_outline),
                accentColor = colors.tertiary,
                isSelected = selectedFilter == "摘录卡片",
                onClick = { onSelectFilter("摘录卡片") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VaultStatCard(
                title = "灵感收件箱",
                count = ideasCount,
                icon = painterResource(R.drawable.ic_lightbulb),
                accentColor = colors.secondary,
                isSelected = selectedFilter == "灵感收件箱",
                onClick = { onSelectFilter("灵感收件箱") },
                modifier = Modifier.weight(1f)
            )
            VaultStatCard(
                title = "来源档案",
                count = sourcesCount,
                icon = painterResource(R.drawable.ic_archive),
                accentColor = colors.outline,
                isSelected = selectedFilter == "来源档案",
                onClick = { onSelectFilter("来源档案") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 概览统计单卡：对齐 Bluke 设计语言（12dp 圆角、IconCircle、20sp ExtraBold 数值、无边框阴影）
 */
@Composable
private fun VaultStatCard(
    title: String,
    count: Int,
    icon: Painter,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(SpacingTokens.statusPillRadius),
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isSelected) BorderStroke(1.5.dp, colors.primary) else null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircle(
                icon = icon,
                containerColor = accentColor.copy(alpha = 0.15f),
                iconTint = accentColor,
                size = 36.dp
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = colors.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 分类筛选平铺芯片行
 */
@Composable
private fun VaultFilterRow(
    filterOptions: List<String>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filterOptions.forEach { option ->
            FilterChip(
                selected = selectedFilter == option,
                onClick = { onSelectFilter(option) },
                label = { Text(option) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * 资料卡片内单条数据：对齐 NoteCard / TodoCard 内边距与视觉层级
 */
@Composable
private fun VaultItemRow(
    item: KnowledgeVaultEntity,
    showCategoryBadge: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.isPinned) {
            Icon(
                painter = painterResource(R.drawable.ic_push_pin),
                contentDescription = "置顶",
                tint = colors.primary,
                modifier = Modifier
                    .size(SpacingTokens.iconSmall)
                    .padding(end = 4.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showCategoryBadge) {
                MiniBadge(text = item.category)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = item.title.ifBlank { item.note },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.note.isNotBlank() && item.note != item.title) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.source.isNotBlank() && item.source != item.note && item.source != item.title) {
                Text(
                    text = "来源: ${item.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_edit),
                contentDescription = "编辑",
                modifier = Modifier.size(18.dp),
                tint = colors.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_delete),
                contentDescription = "删除",
                modifier = Modifier.size(18.dp),
                tint = colors.onSurfaceVariant
            )
        }
    }
}

/**
 * 卡片底部胶囊形内嵌快速输入框：消除突兀边框，对齐 Bluke 容器规范
 */
@Composable
private fun VaultQuickInputRow(
    category: String,
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val placeholderText = when (category) {
        "阅读清单" -> "添加文章链接或书名..."
        "摘录卡片" -> "记录高价值摘录..."
        "灵感收件箱" -> "记录一闪而过的灵感..."
        "来源档案" -> "记录作者、来源或档案..."
        else -> "添加新条目..."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(SpacingTokens.pillRadius),
            placeholder = {
                Text(
                    placeholderText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(
                        onClick = { onAdd(value.trim()) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "添加",
                            tint = colors.primary
                        )
                    }
                }
            }
        )
    }
}

/**
 * 全功能添加 / 编辑资料对话框：多字段输入与置顶开关
 */
@Composable
private fun KnowledgeVaultItemDialog(
    initialItem: KnowledgeVaultEntity,
    onDismiss: () -> Unit,
    onConfirm: (KnowledgeVaultEntity) -> Unit
) {
    val colors = LocalAppColors.current
    val isEdit = initialItem.id != 0L
    var title by remember { mutableStateOf(initialItem.title) }
    var note by remember { mutableStateOf(initialItem.note) }
    var source by remember { mutableStateOf(initialItem.source) }
    var category by remember { mutableStateOf(initialItem.category.ifBlank { "阅读清单" }) }
    var isPinned by remember { mutableStateOf(initialItem.isPinned) }

    val categories = listOf("阅读清单", "摘录卡片", "灵感收件箱", "来源档案")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑资料" else "添加资料") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "分类",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            shape = ModalTokens.innerShape
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("内容 / 摘录 / 笔记") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    shape = ModalTokens.innerShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )

                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("来源 / 链接 / 作者（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "置顶该条目",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it }
                    )
                }
            }
        },
        confirmButton = {
            AppDialogButton(
                label = "保存",
                onClick = {
                    val effectiveTitle = title.ifBlank { note.take(24) }
                    val effectiveNote = note.ifBlank { title }
                    if (effectiveTitle.isNotBlank() || effectiveNote.isNotBlank()) {
                        onConfirm(
                            initialItem.copy(
                                title = effectiveTitle,
                                note = effectiveNote,
                                source = source,
                                category = category,
                                isPinned = isPinned
                            )
                        )
                    }
                }
            )
        },
        dismissButton = {
            AppDialogButton(label = "取消", onClick = onDismiss)
        }
    )
}
