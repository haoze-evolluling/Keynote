@file:OptIn(ExperimentalMaterial3Api::class)

package com.haoze.keynote.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.haoze.keynote.data.db.entity.HabitEntity
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.theme.SpacingTokens
import com.haoze.keynote.util.toDayStartMillis
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

private val habitColors = listOf(
    0xFF4CAF50,
    0xFF2196F3,
    0xFFFF9800,
    0xFFE91E63,
    0xFF7E57C2,
    0xFF009688
)

@Composable
fun HabitScreen(
    onBack: () -> Unit = {},
    viewModel: HabitViewModel = koinViewModel()
) {
    val colors = LocalAppColors.current
    val progressItems by viewModel.progressItems.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    val editingHabit by viewModel.editingHabit.collectAsState()

    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }

    SettingsScaffold(
        title = "习惯打卡",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateEditor() },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "添加习惯")
            }
        }
    ) { padding ->
        if (progressItems.isEmpty()) {
            HabitEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onCreate = { viewModel.openCreateEditor() }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    SettingsGroupTitle("习惯概览")
                    HabitSummary(progressItems)
                }
                item {
                    SettingsGroupTitle("全部习惯 (${progressItems.size})")
                    SettingsGroup {
                        progressItems.forEachIndexed { index, item ->
                            HabitCard(
                                item = item,
                                onToggle = { viewModel.toggleTodayCheckIn(item.habit.id) },
                                onEdit = { viewModel.openEditEditor(item.habit) },
                                onDelete = { habitToDelete = item.habit }
                            )
                            if (index < progressItems.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        HabitEditorDialog(
            habit = editingHabit,
            onDismiss = { viewModel.dismissEditor() },
            onSave = { title, description, targetDays, color ->
                viewModel.saveHabit(title, description, targetDays, color)
            }
        )
    }

    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("删除习惯") },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
            text = { Text("确定要删除「${habit.title}」吗？删除后可在回收站中恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.softDeleteHabit(habit.id)
                    habitToDelete = null
                }) { Text("删除", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun HabitEmptyState(
    modifier: Modifier,
    onCreate: () -> Unit
) {
    val colors = LocalAppColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = colors.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "还没有习惯",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            Text(
                "从一个每天都想坚持的小动作开始",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onCreate,
                shape = RoundedCornerShape(SpacingTokens.pillRadius)
            ) { Text("创建第一个习惯") }
        }
    }
}

@Composable
private fun HabitSummary(items: List<HabitProgressItem>) {
    val checkedToday = items.count { it.checkedToday }
    val bestStreak = items.maxOfOrNull { it.currentStreak } ?: 0
    val averageCompletion = items.map { it.weeklyCompletionPercent }.average().takeIf { !it.isNaN() } ?: 0.0

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HabitStatCard("今日", "$checkedToday/${items.size}", Modifier.weight(1f))
        HabitStatCard("最长连续", "${bestStreak}天", Modifier.weight(1f))
        HabitStatCard("周目标", "${averageCompletion.toInt()}%", Modifier.weight(1f))
    }
}

@Composable
private fun HabitStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    // Bluke 风格统计块：无描边、状态块圆角、标准卡片色
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(SpacingTokens.statusPillRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(
                value,
                // 三卡并排空间有限，按规范以 22sp ExtraBold 靠拢 28sp 数值层级
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}

@Composable
private fun HabitCard(
    item: HabitProgressItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    val accent = Color(item.habit.color)
    val todayStart = remember { System.currentTimeMillis().toDayStartMillis() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = if (item.checkedToday) painterResource(R.drawable.ic_check_circle) else painterResource(R.drawable.ic_radio_button_unchecked),
                contentDescription = if (item.checkedToday) "取消今日打卡" else "今日打卡",
                tint = if (item.checkedToday) accent else colors.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.habit.description.isNotBlank()) {
                    Text(
                        item.habit.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = "编辑习惯")
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "删除习惯")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "连续 ${item.currentStreak} 天",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "周目标 ${item.weeklyCompletionPercent}%",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (6 downTo 0).forEach { offset ->
                val day = todayStart - offset * DayMillis
                val checked = day in item.recentCheckInDays
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (checked) accent else colors.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (checked) accent else colors.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun HabitEditorDialog(
    habit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long) -> Unit
) {
    val colors = LocalAppColors.current
    var title by remember(habit) { mutableStateOf(habit?.title ?: "") }
    var description by remember(habit) { mutableStateOf(habit?.description ?: "") }
    var targetDays by remember(habit) { mutableIntStateOf(habit?.targetDaysPerWeek ?: 7) }
    var selectedColor by remember(habit) { mutableLongStateOf(habit?.color ?: habitColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (habit == null) "新建习惯" else "编辑习惯") },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        textContentColor = colors.onSurface,
        shape = RoundedCornerShape(28.dp),
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
                    label = { Text("习惯名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("备注") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    maxLines = 3
                )
                Text("每周目标：$targetDays 天", style = ModalTokens.labelTextStyle)
                Slider(
                    value = targetDays.toFloat(),
                    onValueChange = { targetDays = it.toInt().coerceIn(1, 7) },
                    valueRange = 1f..7f,
                    steps = 5
                )
                Text("颜色", style = ModalTokens.labelTextStyle)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    habitColors.chunked(3).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowColors.forEach { colorValue ->
                                val selected = selectedColor == colorValue
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { selectedColor = colorValue },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorValue))
                                            .border(
                                                width = if (selected) 3.dp else 1.dp,
                                                color = if (selected) colors.onSurface else colors.outlineVariant,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, description, targetDays, selectedColor) },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
