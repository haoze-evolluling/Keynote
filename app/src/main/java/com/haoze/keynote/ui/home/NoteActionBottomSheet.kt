package com.haoze.keynote.ui.home

import androidx.compose.runtime.Composable
import com.haoze.keynote.ui.common.ActionRow
import com.haoze.keynote.ui.common.ActionMenuDialog
import com.haoze.keynote.ui.components.ModalMenuGroup
import com.haoze.keynote.ui.components.ModalMenuSectionGap
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
        ModalMenuGroup(
            items = buildList {
                add { ActionRow(painterResource(R.drawable.ic_edit), "编辑笔记", onEdit) }
                add { ActionRow(painterResource(R.drawable.ic_info), "查看详情", onViewDetails, showsChevron = true) }
            }
        )
        ModalMenuSectionGap()
        ModalMenuGroup(
            items = buildList {
                add { ActionRow(painterResource(R.drawable.ic_label), "添加标签", onAddTag, showsChevron = true) }
                add { ActionRow(painterResource(R.drawable.ic_label), "管理标签", onManageTags, showsChevron = true) }
            }
        )
        ModalMenuSectionGap()
        ModalMenuGroup(
            items = buildList {
                add {
                    ActionRow(
                        painterResource(R.drawable.ic_auto_awesome), "AI 摘要", onAiSummary,
                        isLoading = isSummarizing, loadingLabel = "正在摘要..."
                    )
                }
                add {
                    ActionRow(
                        painterResource(R.drawable.ic_auto_awesome), "AI 生成标题", onAiGenerateTitle,
                        isLoading = isGeneratingTitle, loadingLabel = "生成中..."
                    )
                }
                add {
                    ActionRow(
                        painterResource(R.drawable.ic_label), "AI 标签", onAiTag,
                        isLoading = isAiTagLoading, loadingLabel = "生成中..."
                    )
                }
            }
        )
        ModalMenuSectionGap()
        ModalMenuGroup(
            items = buildList {
                add { ActionRow(painterResource(R.drawable.ic_share), "分享笔记", onShare) }
                add { ActionRow(painterResource(R.drawable.ic_content_copy), "复制内容", onCopyContent) }
            }
        )
        ModalMenuSectionGap()
        ModalMenuGroup(
            items = buildList {
                add { ActionRow(painterResource(R.drawable.ic_delete), "删除笔记", onDelete, isDestructive = true) }
            }
        )
    }
}
