package com.haoze.keynote.ui.schedule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.ui.common.ActionRow
import com.haoze.keynote.util.toDayStartMillis
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.common.ActionMenuDialog
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsScaffold
import androidx.compose.ui.res.painterResource
import org.koin.compose.viewmodel.koinViewModel
import com.haoze.keynote.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    onBack: () -> Unit = {},
    viewModel: ScheduleViewModel = koinViewModel()
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val schedules by viewModel.schedules.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val aiGeneratedContent by viewModel.aiGeneratedContent.collectAsState()
    val isGeneratingNote by viewModel.isGeneratingNote.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showActionDialogForSchedule by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ScheduleEntity?>(null) }
    var showEditDialogForSchedule by remember { mutableStateOf<ScheduleEntity?>(null) }
    var showLinkNoteDialog by remember { mutableStateOf<Long?>(null) }
    var pendingAiScheduleId by remember { mutableStateOf<Long?>(null) }

    val groupedSchedules = remember(schedules) {
        schedules.groupBy { schedule ->
            val cal = Calendar.getInstance().apply { timeInMillis = schedule.date }
            "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
        }.toSortedMap(Comparator.reverseOrder())
    }

    SettingsScaffold(
        title = "日程",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "新建日程")
            }
        }
    ) { innerPadding ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(R.drawable.ic_calendar_month),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("暂无日程", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedSchedules.forEach { (month, monthSchedules) ->
                    item {
                        SettingsGroupTitle(month)
                        SettingsGroup {
                            monthSchedules.forEachIndexed { index, schedule ->
                                val linkedNote = notes.find { it.note.id == schedule.noteId }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = { showActionDialogForSchedule = schedule.id }
                                        )
                                        .padding(horizontal = 20.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(schedule.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(4.dp))
                                            Text(dateFormat.format(Date(schedule.date)), style = MaterialTheme.typography.bodySmall, color = colors.outline)
                                            if (schedule.location != null) {
                                                Spacer(Modifier.height(2.dp))
                                                Text("📍 ${schedule.location}", style = MaterialTheme.typography.bodySmall, color = colors.outline)
                                            }
                                            if (linkedNote != null) {
                                                Spacer(Modifier.height(2.dp))
                                                Text("关联: ${linkedNote.note.title.ifBlank { "无标题" }}", style = MaterialTheme.typography.bodySmall, color = colors.primary)
                                            }
                                        }
                                    }
                                }
                                if (index < monthSchedules.lastIndex) {
                                    SettingsDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActionDialogForSchedule != null) {
        val currentSchedule = schedules.find { it.id == showActionDialogForSchedule }
        if (currentSchedule != null) {
            val hasLink = currentSchedule.noteId != null
            ActionMenuDialog(
                title = currentSchedule.title,
                onDismiss = { showActionDialogForSchedule = null }
            ) {
                        ActionRow(icon = painterResource(R.drawable.ic_edit), label = "编辑日程", onClick = {
                            showEditDialogForSchedule = currentSchedule
                            showActionDialogForSchedule = null
                        })
                        if (hasLink) {
                            ActionRow(icon = painterResource(R.drawable.ic_link_off), label = "取消关联笔记", onClick = {
                                viewModel.unlinkNote(currentSchedule.id)
                                showActionDialogForSchedule = null
                            })
                        } else {
                            ActionRow(icon = painterResource(R.drawable.ic_link), label = "关联笔记", onClick = {
                                showLinkNoteDialog = currentSchedule.id
                                showActionDialogForSchedule = null
                            })
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))
                        ActionRow(icon = painterResource(R.drawable.ic_auto_awesome), label = if (isGeneratingNote) "生成中..." else "AI 生成笔记", onClick = {
                            pendingAiScheduleId = currentSchedule.id
                            viewModel.aiGenerateNote(currentSchedule.id)
                            showActionDialogForSchedule = null
                        })
                        HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))
                        ActionRow(icon = painterResource(R.drawable.ic_delete), label = "删除日程", isDestructive = true, onClick = {
                            showDeleteConfirm = currentSchedule
                            showActionDialogForSchedule = null
                        })
            }
        }
    }

    showDeleteConfirm?.let { schedule ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除日程") },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
            text = { Text("确定要删除「${schedule.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSchedule(schedule)
                    showDeleteConfirm = null
                }) { Text("删除", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            },
        )
    }

    if (showCreateDialog) {
        ScheduleDialog(
            title = "新建日程",
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, date, endDate, location, description, noteId, reminderEnabled, reminderMinutesBefore ->
                viewModel.createSchedule(
                    title, date, endDate, location, description, noteId,
                    reminderEnabled, reminderMinutesBefore
                )
                showCreateDialog = false
            },
            notes = notes,
            notificationPermissionGranted = notificationPermissionGranted,
            onRequestNotificationPermission = requestNotificationPermission
        )
    }

    showEditDialogForSchedule?.let { schedule ->
        ScheduleDialog(
            title = "编辑日程",
            initialTitle = schedule.title,
            initialDate = schedule.date,
            initialEndDate = schedule.endDate,
            initialLocation = schedule.location,
            initialDescription = schedule.description,
            initialNoteId = schedule.noteId,
            initialReminderEnabled = schedule.reminderEnabled,
            initialReminderMinutesBefore = schedule.reminderMinutesBefore,
            onDismiss = { showEditDialogForSchedule = null },
            onConfirm = { title, date, endDate, location, description, noteId, reminderEnabled, reminderMinutesBefore ->
                viewModel.updateSchedule(
                    schedule.copy(
                        title = title,
                        date = date,
                        endDate = endDate,
                        location = location,
                        description = description,
                        noteId = noteId,
                        reminderEnabled = reminderEnabled,
                        reminderMinutesBefore = reminderMinutesBefore
                    )
                )
                showEditDialogForSchedule = null
            },
            notes = notes,
            notificationPermissionGranted = notificationPermissionGranted,
            onRequestNotificationPermission = requestNotificationPermission
        )
    }

    showLinkNoteDialog?.let { scheduleId ->
        AlertDialog(
            onDismissRequest = { showLinkNoteDialog = null },
            title = { Text("选择关联笔记") },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
            text = {
                if (notes.isEmpty()) {
                    Text("暂无笔记", color = colors.outline)
                } else {
                    DialogContent {
                        notes.forEach { noteWithTags ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = noteWithTags.note.title.ifBlank { "无标题" },
                                    style = ModalTokens.bodyTextStyle,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    viewModel.linkNote(scheduleId, noteWithTags.note.id)
                                    showLinkNoteDialog = null
                                }) { Text("选择") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkNoteDialog = null }) { Text("取消") }
            },
        )
    }

    aiGeneratedContent?.let { content ->
        var editedContent by remember(content) { mutableStateOf(content) }
        AlertDialog(
            onDismissRequest = {
                viewModel.discardAiGeneratedNote()
                pendingAiScheduleId = null
            },
            title = { Text("AI 生成笔记预览") },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
            text = {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp),
                    maxLines = Int.MAX_VALUE
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingAiScheduleId?.let { scheduleId ->
                        viewModel.saveAiGeneratedNote(scheduleId, editedContent)
                    }
                    pendingAiScheduleId = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardAiGeneratedNote()
                    pendingAiScheduleId = null
                }) { Text("取消") }
            },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ScheduleDialog(
    title: String,
    initialTitle: String = "",
    initialDate: Long = System.currentTimeMillis(),
    initialEndDate: Long? = null,
    initialLocation: String? = null,
    initialDescription: String? = null,
    initialNoteId: Long? = null,
    initialReminderEnabled: Boolean = false,
    initialReminderMinutesBefore: Int = 15,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        date: Long,
        endDate: Long?,
        location: String?,
        description: String?,
        noteId: Long?,
        reminderEnabled: Boolean,
        reminderMinutesBefore: Int
    ) -> Unit,
    notes: List<NoteWithTags>,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val colors = LocalAppColors.current
    var editTitle by remember { mutableStateOf(initialTitle) }
    var editDate by remember { mutableStateOf(initialDate) }
    var editEndDate by remember { mutableStateOf(initialEndDate) }
    var editLocation by remember { mutableStateOf(initialLocation ?: "") }
    var editDescription by remember { mutableStateOf(initialDescription ?: "") }
    var editNoteId by remember { mutableStateOf(initialNoteId) }
    var editReminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    var editReminderMinutesBefore by remember { mutableStateOf(initialReminderMinutesBefore) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var pendingTime by remember { mutableStateOf(false) }
    var pendingEndTime by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val reminderOptions = remember {
        listOf(
            0 to "准时",
            5 to "提前5分钟",
            15 to "提前15分钟",
            30 to "提前30分钟"
        )
    }

    LaunchedEffect(pendingTime) {
        if (pendingTime) {
            kotlinx.coroutines.delay(350)
            showTimePicker = true
            pendingTime = false
        }
    }

    LaunchedEffect(pendingEndTime) {
        if (pendingEndTime) {
            kotlinx.coroutines.delay(350)
            showEndTimePicker = true
            pendingEndTime = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            DialogContent(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("日程标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 开始时间
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateFormat.format(Date(editDate)),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("开始时间") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = colors.onSurface,
                            disabledBorderColor = colors.outline,
                            disabledLabelColor = colors.onSurfaceVariant,
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 48.dp)
                            .clickable { showDatePicker = true }
                    )
                }
                // 结束时间（可选）
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (editEndDate != null) dateFormat.format(Date(editEndDate!!)) else "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("结束时间（可选）") },
                        placeholder = { Text("点击选择结束时间") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = colors.onSurface,
                            disabledBorderColor = colors.outline,
                            disabledLabelColor = colors.onSurfaceVariant,
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 48.dp)
                            .clickable { showEndDatePicker = true }
                    )
                }
                // 地点
                OutlinedTextField(
                    value = editLocation,
                    onValueChange = { editLocation = it },
                    label = { Text("地点（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 任务事项
                OutlinedTextField(
                    value = editDescription,
                    onValueChange = { editDescription = it },
                    label = { Text("任务事项（可选）") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("提醒我", style = ModalTokens.bodyTextStyle, modifier = Modifier.weight(1f))
                    Switch(
                        checked = editReminderEnabled,
                        onCheckedChange = { checked ->
                            editReminderEnabled = checked
                            if (checked && !notificationPermissionGranted) {
                                onRequestNotificationPermission()
                            }
                        }
                    )
                }
                if (editReminderEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        reminderOptions.forEach { (minutesBefore, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editReminderMinutesBefore = minutesBefore }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = editReminderMinutesBefore == minutesBefore,
                                    onClick = { editReminderMinutesBefore = minutesBefore }
                                )
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (!notificationPermissionGranted) {
                    TextButton(onClick = onRequestNotificationPermission) {
                        Text("授权通知以接收日程提醒")
                    }
                    Text(
                        "未授权通知时，日程仍会保存，但不会弹出系统提醒。",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.outline
                    )
                }
                val linkedNote = notes.find { it.note.id == editNoteId }
                if (linkedNote != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("关联: ${linkedNote.note.title.ifBlank { "无标题" }}", style = ModalTokens.bodyTextStyle, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editNoteId = null }) { Text("取消关联") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editTitle.isNotBlank()) onConfirm(
                        editTitle, editDate, editEndDate,
                        editLocation.ifBlank { null },
                        editDescription.ifBlank { null },
                        editNoteId,
                        editReminderEnabled,
                        editReminderMinutesBefore
                    )
                },
                enabled = editTitle.isNotBlank()
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
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            editDate = it.toDayStartMillis()
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

    if (showTimePicker) {
        val cal = remember { Calendar.getInstance().apply { timeInMillis = editDate } }
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
                    editDate = Calendar.getInstance().apply {
                        timeInMillis = editDate
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        )
    }

    if (showEndDatePicker) {
        val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = editEndDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let {
                        editEndDate = it.toDayStartMillis()
                        showEndDatePicker = false
                        pendingEndTime = true
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = endDatePickerState) }
    }

    if (showEndTimePicker) {
        val cal = remember { Calendar.getInstance().apply { timeInMillis = editEndDate ?: System.currentTimeMillis() } }
        val endTimePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("选择结束时间") },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
            text = { TimePicker(state = endTimePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    editEndDate = Calendar.getInstance().apply {
                        timeInMillis = editEndDate ?: System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                        set(Calendar.MINUTE, endTimePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    showEndTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("取消") }
            },
        )
    }
}
