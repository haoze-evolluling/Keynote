package com.haoze.keynote.ui.trash

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsInfoText
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.SpacingTokens
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@Composable
fun TrashScreen(
    onBack: () -> Unit = {},
    viewModel: TrashViewModel = koinViewModel()
) {
    val colors = LocalAppColors.current
    val trashItems by viewModel.trashItems.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var pendingPermanentDelete by remember { mutableStateOf<TrashItem?>(null) }

    SettingsScaffold(
        title = "回收站",
        onBack = onBack
    ) { innerPadding ->
        if (trashItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "回收站为空",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    SettingsInfoText(
                        text = "回收站内容会保留 30 天；永久删除后无法恢复。"
                    )
                }
                item {
                    SettingsGroupTitle("已删除项目 (${trashItems.size})")
                    SettingsGroup {
                        trashItems.forEachIndexed { index, item ->
                            val remainingDays = maxOf(0, 30 - TimeUnit.MILLISECONDS.toDays(
                                System.currentTimeMillis() - item.deletedAt
                            ).toInt())

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    when (item) {
                                        is TrashItem.TrashNote -> NoteTrashContent(item, remainingDays, dateFormat)
                                        is TrashItem.TrashBill -> BillTrashContent(item, remainingDays, dateFormat)
                                        is TrashItem.TrashSchedule -> ScheduleTrashContent(item, remainingDays, dateFormat)
                                        is TrashItem.TrashTodo -> TodoTrashContent(item, remainingDays, dateFormat)
                                        is TrashItem.TrashHabit -> HabitTrashContent(item, remainingDays, dateFormat)
                                        is TrashItem.TrashAIChatConversation -> AIChatTrashContent(item, remainingDays, dateFormat)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TrashActionButtons(
                                        onRestore = { viewModel.restore(item) },
                                        onDelete = { pendingPermanentDelete = item }
                                    )
                                }
                            }
                            if (index < trashItems.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    pendingPermanentDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text("永久删除") },
            text = { Text("永久删除后无法恢复，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.permanentlyDelete(item)
                    pendingPermanentDelete = null
                }) { Text("永久删除", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentDelete = null }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
private fun AIChatTrashContent(
    item: TrashItem.TrashAIChatConversation,
    remainingDays: Int,
    dateFormat: SimpleDateFormat
) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_chat_bubble),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.data.title.ifBlank { "AI 对话" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = dateFormat.format(Date(item.data.updatedAt)),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant
    )
}

@Composable
private fun NoteTrashContent(item: TrashItem.TrashNote, remainingDays: Int, dateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_description),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.data.note.title.ifBlank { "无标题" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = dateFormat.format(Date(item.data.note.updatedAt)),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant
    )
}

@Composable
private fun BillTrashContent(item: TrashItem.TrashBill, remainingDays: Int, dateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_receipt),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "¥%.2f".format(item.data.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.data.item,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = dateFormat.format(Date(item.data.date)),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant
    )
}

@Composable
private fun ScheduleTrashContent(item: TrashItem.TrashSchedule, remainingDays: Int, dateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_event),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.data.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    val dateText = buildString {
        append(dateFormat.format(Date(item.data.date)))
        item.data.endDate?.let { append(" - ${dateFormat.format(Date(it))}") }
    }
    Text(
        text = dateText,
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant
    )
    item.data.location?.let { location ->
        if (location.isNotBlank()) {
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodoTrashContent(item: TrashItem.TrashTodo, remainingDays: Int, dateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.data.title.ifBlank { "无标题" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    item.data.dueDate?.let { dueDate ->
        Text(
            text = "截止日期: ${dateFormat.format(Date(dueDate))}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun HabitTrashContent(item: TrashItem.TrashHabit, remainingDays: Int, dateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.data.title.ifBlank { "无标题" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = dateFormat.format(Date(item.data.updatedAt)),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant
    )
}

@Composable
private fun RemainingDaysBadge(remainingDays: Int) {
    val colors = LocalAppColors.current
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = colors.errorContainer
    ) {
        Text(
            text = "${remainingDays}天",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onErrorContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TrashActionButtons(onRestore: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onRestore) {
            Icon(
                painterResource(R.drawable.ic_restore),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("恢复")
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onDelete) {
            Icon(
                painterResource(R.drawable.ic_delete_forever),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.error
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("永久删除", color = colors.error)
        }
    }
}
