@file:OptIn(ExperimentalMaterial3Api::class)

package com.haoze.keynote.ui.todo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.haoze.keynote.data.db.entity.TodoCategoryEntity
import com.haoze.keynote.data.db.entity.TodoEntity
import com.haoze.keynote.ui.common.ActionRow
import com.haoze.keynote.ui.common.ActionMenuDialog
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.theme.SpacingTokens
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.util.toDayStartMillis
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(
    onBack: () -> Unit = {},
    viewModel: TodoViewModel = koinViewModel()
) {
    val colors = LocalAppColors.current
    val todos by viewModel.todos.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val editingTodo by viewModel.editingTodo.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTodo by remember { mutableStateOf<TodoEntity?>(null) }

    val groupedTodos = remember(todos) { groupTodosByDate(todos) }

    SettingsScaffold(
        title = "待办事项",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "添加待办")
            }
        }
    ) { padding ->
        if (todos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "暂无待办事项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedTodos.forEach { (label, groupTodos) ->
                    item {
                        SettingsGroupTitle(label)
                        SettingsGroup {
                            groupTodos.forEachIndexed { index, todo ->
                                TodoCard(
                                    todo = todo,
                                    categories = categories,
                                    onToggle = { viewModel.toggleComplete(todo) },
                                    onLongClick = {
                                        selectedTodo = todo
                                        showBottomSheet = true
                                    }
                                )
                                if (index < groupTodos.lastIndex) {
                                    SettingsDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TodoDialog(
            todo = editingTodo,
            categories = categories,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { title, priority, dueDate, hasTime, categoryId, noteId, notes ->
                if (editingTodo != null) {
                    viewModel.updateTodo(
                        editingTodo!!.copy(
                            title = title, priority = priority, dueDate = dueDate,
                            hasTime = hasTime, categoryId = categoryId, noteId = noteId,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.createTodo(
                        title, priority, dueDate, hasTime, categoryId, noteId, notes
                    )
                }
            }
        )
    }

    if (showBottomSheet && selectedTodo != null) {
        TodoActionBottomSheet(
            todo = selectedTodo!!,
            onDismiss = { showBottomSheet = false; selectedTodo = null },
            onEdit = {
                showBottomSheet = false
                viewModel.openEditDialog(selectedTodo!!)
                selectedTodo = null
            },
            onDelete = {
                viewModel.deleteTodo(selectedTodo!!)
                showBottomSheet = false
                selectedTodo = null
            },
            onToggleComplete = {
                viewModel.toggleComplete(selectedTodo!!)
                showBottomSheet = false
                selectedTodo = null
            }
        )
    }
}

private fun groupTodosByDate(todos: List<TodoEntity>): List<Pair<String, List<TodoEntity>>> {
    val now = System.currentTimeMillis()
    val todayStart = now.toDayStartMillis()
    val tomorrowStart = todayStart + 86400000L
    val weekEnd = todayStart + 7 * 86400000L

    val overdue = mutableListOf<TodoEntity>()
    val today = mutableListOf<TodoEntity>()
    val tomorrow = mutableListOf<TodoEntity>()
    val thisWeek = mutableListOf<TodoEntity>()
    val later = mutableListOf<TodoEntity>()
    val completed = mutableListOf<TodoEntity>()

    todos.forEach { todo ->
        if (todo.isCompleted) {
            completed.add(todo)
        } else if (todo.dueDate == null) {
            later.add(todo)
        } else if (todo.dueDate < todayStart) {
            overdue.add(todo)
        } else if (todo.dueDate < tomorrowStart) {
            today.add(todo)
        } else if (todo.dueDate < tomorrowStart + 86400000L) {
            tomorrow.add(todo)
        } else if (todo.dueDate < weekEnd) {
            thisWeek.add(todo)
        } else {
            later.add(todo)
        }
    }

    val result = mutableListOf<Pair<String, List<TodoEntity>>>()
    if (overdue.isNotEmpty()) result.add("逾期" to overdue)
    if (today.isNotEmpty()) result.add("今天" to today)
    if (tomorrow.isNotEmpty()) result.add("明天" to tomorrow)
    if (thisWeek.isNotEmpty()) result.add("本周" to thisWeek)
    if (later.isNotEmpty()) result.add("以后" to later)
    if (completed.isNotEmpty()) result.add("已完成" to completed)
    return result
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoCard(
    todo: TodoEntity,
    categories: List<TodoCategoryEntity>,
    onToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val category = categories.find { it.id == todo.categoryId }
    val priorityColor = when (todo.priority) {
        2 -> colors.priorityHigh
        1 -> colors.priorityMedium
        else -> colors.priorityLow
    }
    val dateDf = remember { SimpleDateFormat("M月d日", Locale.CHINESE) }
    val dateTimeDf = remember { SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = { onToggle() }
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (!todo.isCompleted) FontWeight.Medium else FontWeight.Normal,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (todo.isCompleted) colors.onSurfaceVariant else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (todo.dueDate != null) {
                    Text(
                        text = if (todo.hasTime) dateTimeDf.format(Date(todo.dueDate))
                               else dateDf.format(Date(todo.dueDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (todo.dueDate < System.currentTimeMillis() && !todo.isCompleted) colors.error
                                else colors.onSurfaceVariant
                    )
                }
                if (category != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(category.color).copy(alpha = 0.2f)
                    ) {
                        Text(
                            category.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(category.color)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoActionBottomSheet(
    todo: TodoEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    ActionMenuDialog(title = "待办操作", onDismiss = onDismiss) {
            ActionRow(painterResource(R.drawable.ic_edit), "编辑", onEdit)
            ActionRow(
                painterResource(R.drawable.ic_done),
                if (todo.isCompleted) "标记未完成" else "标记完成",
                onToggleComplete
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))
            ActionRow(painterResource(R.drawable.ic_delete), "删除", onDelete, isDestructive = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoDialog(
    todo: TodoEntity?,
    categories: List<TodoCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Long?, Boolean, Long?, Long?, String?) -> Unit
) {
    val colors = LocalAppColors.current
    var title by remember { mutableStateOf(todo?.title ?: "") }
    var priority by remember { mutableIntStateOf(todo?.priority ?: 1) }
    var dueDate by remember { mutableStateOf(todo?.dueDate) }
    var hasTime by remember { mutableStateOf(todo?.hasTime ?: false) }
    var categoryId by remember { mutableStateOf(todo?.categoryId) }
    var noteId by remember { mutableStateOf(todo?.noteId) }
    var notesText by remember { mutableStateOf(todo?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingTime by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (todo != null) "编辑待办" else "新建待办") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("优先级", style = ModalTokens.labelTextStyle)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "低" to 0 to colors.priorityLow,
                        "中" to 1 to colors.priorityMedium,
                        "高" to 2 to colors.priorityHigh
                    ).forEach { (pair, color) ->
                        val (label, p) = pair
                        val isSelected = priority == p
                        Surface(
                            onClick = { priority = p },
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) color.copy(alpha = 0.15f) else colors.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) color else colors.outlineVariant
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = ModalTokens.bodyTextStyle,
                                    color = if (isSelected) color else colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val dateDf = remember { SimpleDateFormat("yyyy/M/d", Locale.CHINESE) }
                val dateTimeDf = remember { SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINESE) }
                Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    OutlinedTextField(
                        value = if (dueDate != null) {
                            if (hasTime) dateTimeDf.format(Date(dueDate!!))
                            else dateDf.format(Date(dueDate!!))
                        } else "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("截止日期") },
                        placeholder = { Text("点击选择") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = colors.onSurface,
                            disabledBorderColor = colors.outline,
                            disabledLabelColor = colors.onSurfaceVariant,
                            disabledPlaceholderColor = colors.onSurfaceVariant,
                        ),
                        trailingIcon = { Icon(painterResource(R.drawable.ic_date_range), contentDescription = "选择时间") }
                    )
                }

                if (categories.isNotEmpty()) {
                    val selectedCat = categories.find { it.id == categoryId }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCat?.name ?: "无分类",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("分类") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("无分类") },
                                onClick = { categoryId = null; categoryExpanded = false }
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { categoryId = cat.id; categoryExpanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("备注") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) onConfirm(
                        title, priority, dueDate, hasTime, categoryId, noteId,
                        notesText.ifBlank { null }
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        textContentColor = colors.onSurface,
        shape = RoundedCornerShape(28.dp),
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dueDate = it.toDayStartMillis()
                        showDatePicker = false
                        pendingTime = true
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(pendingTime) {
        if (pendingTime) {
            delay(350)
            showTimePicker = true
            pendingTime = false
        }
    }

    if (showTimePicker) {
        val cal = remember {
            Calendar.getInstance().apply { timeInMillis = dueDate ?: System.currentTimeMillis() }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = Calendar.getInstance().apply {
                        timeInMillis = dueDate ?: System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    hasTime = true
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        )
    }
}
