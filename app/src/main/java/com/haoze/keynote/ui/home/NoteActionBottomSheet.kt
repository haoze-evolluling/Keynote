package com.haoze.keynote.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.haoze.keynote.ui.common.ActionRow
import com.haoze.keynote.ui.common.ActionMenuDialog
import com.haoze.keynote.ui.theme.ModalTokens
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@Composable
fun NoteActionBottomSheet(
    noteTitle: String,
    noteContent: String = "",
    isAiTagLoading: Boolean = false,
    isSummarizing: Boolean = false,
    isGeneratingTitle: Boolean = false,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onAiSummary: () -> Unit,
    onCopyContent: () -> Unit,
    onViewDetails: () -> Unit,
    onAiTag: () -> Unit,
    onAiGenerateTitle: () -> Unit,
    onAddTag: () -> Unit,
    onManageTags: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionMenuDialog(title = "笔记操作", onDismiss = onDismiss) {
                ActionRow(painterResource(R.drawable.ic_edit), "编辑笔记", onEdit)
                ActionRow(painterResource(R.drawable.ic_info), "查看详情", onViewDetails)

                HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))

                ActionRow(painterResource(R.drawable.ic_label), "添加标签", onAddTag)
                ActionRow(painterResource(R.drawable.ic_label), "管理标签", onManageTags)

                HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))

                ActionRow(painterResource(R.drawable.ic_auto_awesome), "AI 摘要", onAiSummary, isLoading = isSummarizing, loadingLabel = "正在摘要...")
                ActionRow(painterResource(R.drawable.ic_auto_awesome), "AI 生成标题", onAiGenerateTitle, isLoading = isGeneratingTitle, loadingLabel = "生成中...")
                ActionRow(painterResource(R.drawable.ic_label), "AI 标签", onAiTag, isLoading = isAiTagLoading, loadingLabel = "生成中...")

                HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))

                ActionRow(painterResource(R.drawable.ic_share), "分享笔记", onShare)
                ActionRow(painterResource(R.drawable.ic_content_copy), "复制内容", onCopyContent)

                HorizontalDivider(modifier = Modifier.padding(vertical = ModalTokens.menuDividerPaddingVertical))

                ActionRow(painterResource(R.drawable.ic_delete), "删除笔记", onDelete, isDestructive = true)
    }
}
