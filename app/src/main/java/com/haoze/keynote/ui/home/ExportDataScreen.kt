package com.haoze.keynote.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.BillDatabase
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.CategoryEntity
import com.haoze.keynote.data.db.entity.TagEntity
import com.haoze.keynote.data.remote.AiProvider
import com.haoze.keynote.ui.navigation.LocalOpenMainNav
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.theme.SpacingTokens
import com.haoze.keynote.util.exporter.NoteExporter
import com.haoze.keynote.util.exporter.NoteExporter.NoteExportFormat
import com.haoze.keynote.util.exporter.BillExporter
import com.haoze.keynote.util.exporter.ScheduleExporter
import com.haoze.keynote.util.exporter.ScheduleExporter.ScheduleExportFormat
import com.haoze.keynote.util.exporter.TodoExporter
import com.haoze.keynote.util.exporter.AaSplitExporter
import com.haoze.keynote.util.exporter.AiProviderExporter
import com.haoze.keynote.util.PreferencesManager

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsInfoText
import com.haoze.keynote.ui.components.SettingsLoadingContent
import com.haoze.keynote.ui.components.SettingsNavigationItem
import com.haoze.keynote.ui.components.SettingsScaffold


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

    var showNoteSheet by remember { mutableStateOf(false) }
    var showBillSheet by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var showTodoSheet by remember { mutableStateOf(false) }
    var showAaSplitSheet by remember { mutableStateOf(false) }
    var showAiProviderSheet by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = "导出数据",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SettingsInfoText("选择需要导出的数据类型，文件将保存到 Downloads/KeyNote/。")

            SettingsGroupTitle("笔记与知识库")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "笔记",
                    subtitle = "导出为 Markdown / TXT / PDF",
                    leadingIcon = painterResource(R.drawable.ic_edit),
                    enabled = !isExporting,
                    onClick = { showNoteSheet = true }
                )
            }

            SettingsGroupTitle("财务与账单")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "账单",
                    subtitle = "导出为 CSV",
                    leadingIcon = painterResource(R.drawable.ic_account_balance),
                    enabled = !isExporting,
                    onClick = { showBillSheet = true }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "AA 计算",
                    subtitle = "导出为 CSV",
                    leadingIcon = painterResource(R.drawable.ic_people),
                    enabled = !isExporting,
                    onClick = { showAaSplitSheet = true }
                )
            }

            SettingsGroupTitle("日程与待办")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "日程",
                    subtitle = "导出为 iCal / CSV",
                    leadingIcon = painterResource(R.drawable.ic_calendar_month),
                    enabled = !isExporting,
                    onClick = { showScheduleSheet = true }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "待办事项",
                    subtitle = "导出为 CSV",
                    leadingIcon = painterResource(R.drawable.ic_check_box),
                    enabled = !isExporting,
                    onClick = { showTodoSheet = true }
                )
            }

            SettingsGroupTitle("AI 配置")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "AI 厂商设置",
                    subtitle = "导出厂商名称、地址和 API Key",
                    leadingIcon = painterResource(R.drawable.ic_psychology),
                    enabled = !isExporting,
                    onClick = { showAiProviderSheet = true }
                )
            }

            if (isExporting) {
                SettingsLoadingContent(modifier = Modifier.height(88.dp))
            }
        }
    }

    if (showNoteSheet) {
        NoteExportSheet(context = context, onDismiss = { showNoteSheet = false }, onExport = { startDate, endDate, tagIds, format ->
            showNoteSheet = false
            scope.launch {
                isExporting = true
                try {
                    val count = NoteExporter.exportNotes(context, startDate, endDate, tagIds, format)
                    snackbarHostState.showSnackbar("成功导出 ${count} 篇笔记")
                } catch (e: Exception) { snackbarHostState.showSnackbar("导出失败: ${e.message}") } finally { isExporting = false }
            }
        })
    }

    if (showBillSheet) {
        BillExportSheet(context = context, onDismiss = { showBillSheet = false }, onExport = { startDate, endDate, categoryIds ->
            showBillSheet = false
            scope.launch {
                isExporting = true
                try {
                    val count = BillExporter.exportBills(context, startDate, endDate, categoryIds)
                    snackbarHostState.showSnackbar("成功导出 ${count} 条账单")
                } catch (e: Exception) { snackbarHostState.showSnackbar("导出失败: ${e.message}") } finally { isExporting = false }
            }
        })
    }

    if (showScheduleSheet) {
        ScheduleExportSheet(context = context, onDismiss = { showScheduleSheet = false }, onExport = { startDate, endDate, format ->
            showScheduleSheet = false
            scope.launch {
                isExporting = true
                try {
                    val count = ScheduleExporter.exportSchedules(context, startDate, endDate, format)
                    snackbarHostState.showSnackbar("成功导出 ${count} 个日程")
                } catch (e: Exception) { snackbarHostState.showSnackbar("导出失败: ${e.message}") } finally { isExporting = false }
            }
        })
    }

    if (showTodoSheet) {
        TodoExportSheet(onDismiss = { showTodoSheet = false }, onExport = { startDate, endDate ->
            showTodoSheet = false
            scope.launch {
                isExporting = true
                try {
                    val count = TodoExporter.exportTodos(context, startDate, endDate)
                    snackbarHostState.showSnackbar("成功导出 ${count} 条待办")
                } catch (e: Exception) { snackbarHostState.showSnackbar("导出失败: ${e.message}") } finally { isExporting = false }
            }
        })
    }

    if (showAaSplitSheet) {
        AaSplitExportSheet(onDismiss = { showAaSplitSheet = false }, onExport = { startDate, endDate ->
            showAaSplitSheet = false
            scope.launch {
                isExporting = true
                try {
                    val db = com.haoze.keynote.data.db.BillDatabase.getDatabase(context)
                    val count = AaSplitExporter.exportAaSplits(context, db.aaSplitDao(), startDate, endDate)
                    snackbarHostState.showSnackbar("成功导出 ${count} 条AA计算")
                } catch (e: Exception) { snackbarHostState.showSnackbar("导出失败: ${e.message}") } finally { isExporting = false }
            }
        })
    }

    if (showAiProviderSheet) {
        AiProviderExportSheet(
            context = context,
            onDismiss = { showAiProviderSheet = false },
            onExport = { selectedIds ->
                showAiProviderSheet = false
                scope.launch {
                    isExporting = true
                    try {
                        val count = AiProviderExporter.exportProviders(context, selectedIds)
                        snackbarHostState.showSnackbar("成功导出 ${count} 个厂商配置")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("导出失败: ${e.message}")
                    } finally {
                        isExporting = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiProviderExportSheet(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onExport: (Set<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedProviderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showWarning by remember { mutableStateOf(true) }
    val providers = remember { mutableStateListOf<AiProvider>() }

    LaunchedEffect(Unit) {
        val preferencesManager = PreferencesManager(context)
        val rawJson = preferencesManager.providersJson.first()
        try {
            val type = object : TypeToken<List<AiProvider>>() {}.type
            val parsed = Gson().fromJson<List<AiProvider>>(rawJson, type) ?: emptyList()
            providers.clear()
            providers.addAll(parsed)
        } catch (_: Exception) {
            providers.clear()
        }
        if (providers.isNotEmpty()) {
            selectedProviderIds = providers.map { it.id }.toSet()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        DialogContent(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("导出 AI 厂商配置", style = ModalTokens.titleTextStyle)

            if (showWarning) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            tint = LocalAppColors.current.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "导出文件包含明文 API Key，请妥善保管，避免泄露。",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAppColors.current.onErrorContainer
                        )
                    }
                }
                TextButton(onClick = { showWarning = false }, modifier = Modifier.align(Alignment.End)) {
                    Text("已知晓")
                }
            }

            if (providers.isEmpty()) {
                Text("暂未配置任何 AI 厂商", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("选择要导出的厂商", style = ModalTokens.titleTextStyle)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    providers.forEach { provider ->
                        val isSelected = provider.id in selectedProviderIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedProviderIds = if (isSelected) {
                                    selectedProviderIds - provider.id
                                } else {
                                    selectedProviderIds + provider.id
                                }
                            },
                            label = { Text(provider.name) },
                            leadingIcon = if (isSelected) {
                                { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            Button(
                onClick = { onExport(selectedProviderIds) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(SpacingTokens.pillRadius),
                enabled = providers.isNotEmpty() && selectedProviderIds.isNotEmpty()
            ) {
                Text("确认导出")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(
    title: String,
    onDismiss: () -> Unit,
    filterContent: @Composable ColumnScope.() -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "确认导出"
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        DialogContent(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = ModalTokens.titleTextStyle)
            filterContent()
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SpacingTokens.pillRadius)) { Text(confirmLabel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteExportSheet(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onExport: (startDate: Long?, endDate: Long?, tagIds: List<Long>?, format: NoteExportFormat) -> Unit
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedFormat by remember { mutableStateOf(NoteExportFormat.MARKDOWN) }
    val tags = remember { mutableStateListOf<TagEntity>() }

    LaunchedEffect(Unit) {
        val db = NoteDatabase.getDatabase(context)
        tags.clear(); tags.addAll(db.tagDao().getActiveTags().first())
    }

    ExportSheet(
        title = "导出笔记",
        onDismiss = onDismiss,
        filterContent = {
            DateRangeSelector(startDate = startDate, endDate = endDate, onStartChange = { startDate = it }, onEndChange = { endDate = it })
            if (tags.isNotEmpty()) {
                Text("标签筛选", style = ModalTokens.titleTextStyle)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in selectedTagIds,
                            onClick = { selectedTagIds = if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }
            Text("导出格式", style = ModalTokens.titleTextStyle)
            NoteExportFormat.entries.forEach { format ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedFormat == format, onClick = { selectedFormat = format })
                    Text(
                        when (format) {
                            NoteExportFormat.MARKDOWN -> "Markdown"
                            NoteExportFormat.TXT -> "纯文本 (TXT)"
                            NoteExportFormat.PDF -> "PDF"
                        }
                    )
                }
            }
        },
        onConfirm = { onExport(startDate, endDate, if (selectedTagIds.isEmpty()) null else selectedTagIds.toList(), selectedFormat) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillExportSheet(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onExport: (startDate: Long?, endDate: Long?, categoryIds: List<Long>?) -> Unit
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryIds by remember { mutableStateOf(setOf<Long>()) }
    val categories = remember { mutableStateListOf<CategoryEntity>() }

    LaunchedEffect(Unit) {
        val db = BillDatabase.getDatabase(context)
        categories.clear(); categories.addAll(db.categoryDao().getAllCategories().first())
    }

    ExportSheet(
        title = "导出账单",
        onDismiss = onDismiss,
        filterContent = {
            DateRangeSelector(startDate = startDate, endDate = endDate, onStartChange = { startDate = it }, onEndChange = { endDate = it })
            if (categories.isNotEmpty()) {
                Text("分类筛选", style = ModalTokens.titleTextStyle)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category.id in selectedCategoryIds,
                            onClick = { selectedCategoryIds = if (category.id in selectedCategoryIds) selectedCategoryIds - category.id else selectedCategoryIds + category.id },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
        },
        onConfirm = { onExport(startDate, endDate, if (selectedCategoryIds.isEmpty()) null else selectedCategoryIds.toList()) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleExportSheet(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onExport: (startDate: Long?, endDate: Long?, format: ScheduleExportFormat) -> Unit
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedFormat by remember { mutableStateOf(ScheduleExportFormat.ICS) }

    ExportSheet(
        title = "导出日程",
        onDismiss = onDismiss,
        filterContent = {
            DateRangeSelector(startDate = startDate, endDate = endDate, onStartChange = { startDate = it }, onEndChange = { endDate = it })
            Text("导出格式", style = ModalTokens.titleTextStyle)
            ScheduleExportFormat.entries.forEach { format ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedFormat == format, onClick = { selectedFormat = format })
                    Text(
                        when (format) {
                            ScheduleExportFormat.ICS -> "iCal (.ics)"
                            ScheduleExportFormat.CSV -> "CSV"
                        }
                    )
                }
            }
        },
        onConfirm = { onExport(startDate, endDate, selectedFormat) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoExportSheet(
    onDismiss: () -> Unit,
    onExport: (startDate: Long?, endDate: Long?) -> Unit
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    ExportSheet(
        title = "导出待办事项",
        onDismiss = onDismiss,
        filterContent = {
            DateRangeSelector(startDate = startDate, endDate = endDate, onStartChange = { startDate = it }, onEndChange = { endDate = it })
        },
        onConfirm = { onExport(startDate, endDate) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AaSplitExportSheet(
    onDismiss: () -> Unit,
    onExport: (startDate: Long?, endDate: Long?) -> Unit
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    ExportSheet(
        title = "导出AA计算",
        onDismiss = onDismiss,
        filterContent = {
            DateRangeSelector(startDate = startDate, endDate = endDate, onStartChange = { startDate = it }, onEndChange = { endDate = it })
        },
        onConfirm = { onExport(startDate, endDate) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSelector(
    startDate: Long?,
    endDate: Long?,
    onStartChange: (Long?) -> Unit,
    onEndChange: (Long?) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Text("日期范围", style = ModalTokens.titleTextStyle)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
            Text(if (startDate != null) dateFormat.format(Date(startDate)) else "开始日期", maxLines = 1)
        }
        OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
            Text(if (endDate != null) dateFormat.format(Date(endDate)) else "结束日期", maxLines = 1)
        }
    }
    if (startDate != null || endDate != null) {
        TextButton(onClick = { onStartChange(null); onEndChange(null) }) { Text("清除日期") }
    }

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(onDismissRequest = { showStartPicker = false },
            confirmButton = { TextButton(onClick = { onStartChange(datePickerState.selectedDateMillis); showStartPicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(onDismissRequest = { showEndPicker = false },
            confirmButton = { TextButton(onClick = { onEndChange(datePickerState.selectedDateMillis); showEndPicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }
}
