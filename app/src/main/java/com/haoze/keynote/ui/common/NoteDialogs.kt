package com.haoze.keynote.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.NoteWithTags
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@Composable
fun NoteDeleteConfirmDialog(
    show: Boolean,
    noteId: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    if (show && noteId != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("删除笔记") },
            text = { Text("确定要删除这篇笔记吗？删除后可在回收站中恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(noteId)
                    onDismiss()
                }) {
                    Text("删除", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
fun NoteDetailsDialog(
    note: NoteWithTags?,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    if (note != null) {
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("笔记详情") },
            text = {
                DialogContent(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("标题", style = ModalTokens.labelTextStyle, color = colors.outline)
                    Text(note.note.title.ifBlank { "无标题" }, style = ModalTokens.bodyTextStyle, fontWeight = FontWeight.SemiBold)
                    Text("创建时间", style = ModalTokens.labelTextStyle, color = colors.outline)
                    Text(dateFormat.format(Date(note.note.createdAt)), style = ModalTokens.bodyTextStyle)
                    Text("更新时间", style = ModalTokens.labelTextStyle, color = colors.outline)
                    Text(dateFormat.format(Date(note.note.updatedAt)), style = ModalTokens.bodyTextStyle)
                    if (note.tags.isNotEmpty()) {
                        Text("标签", style = ModalTokens.labelTextStyle, color = colors.outline)
                        Text(note.tags.joinToString(", ") { "#${it.name}" }, style = ModalTokens.bodyTextStyle)
                    }
                    if (note.note.content.isNotBlank()) {
                        Text("内容预览", style = ModalTokens.labelTextStyle, color = colors.outline)
                        Text(
                            note.note.content.take(100) + if (note.note.content.length > 100) "..." else "",
                            style = ModalTokens.bodyTextStyle,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("关闭") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
fun NoteAddTagDialog(
    show: Boolean,
    noteId: Long?,
    onAddTag: (noteId: Long, tagName: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (show && noteId != null) {
        var tagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("添加标签") },
            text = {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddTag(noteId, tagName)
                        onDismiss()
                    },
                    enabled = tagName.isNotBlank()
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
fun NoteManageTagsDialog(
    note: NoteWithTags?,
    onRemoveTag: (noteId: Long, tagId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    if (note != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("管理标签") },
            text = {
                if (note.tags.isEmpty()) {
                    Text("暂无标签", color = colors.outline)
                } else {
                    DialogContent(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        note.tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text("#${tag.name}") },
                                trailingIcon = {
                                    IconButton(onClick = { onRemoveTag(note.note.id, tag.id) }) {
                                        Icon(
                                            painterResource(R.drawable.ic_clear),
                                            contentDescription = "移除",
                                            modifier = Modifier.size(16.dp),
                                            tint = colors.error
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("关闭") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }
}
