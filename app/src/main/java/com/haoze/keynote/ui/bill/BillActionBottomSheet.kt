package com.haoze.keynote.ui.bill

import androidx.compose.runtime.Composable
import com.haoze.keynote.ui.common.ActionRow
import com.haoze.keynote.ui.common.ActionMenuDialog
import com.haoze.keynote.ui.components.ModalMenuGroup
import com.haoze.keynote.ui.components.ModalMenuSectionGap
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@Composable
fun BillActionBottomSheet(
    billItem: String,
    billAmount: Double,
    onEdit: () -> Unit,
    onViewDetails: () -> Unit,
    onCopyItem: () -> Unit,
    onCopyAmount: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionMenuDialog(title = "账单操作", onDismiss = onDismiss) {
        ModalMenuGroup(
            items = buildList {
                add { ActionRow(painterResource(R.drawable.ic_edit), "编辑账单", onEdit) }
                add { ActionRow(painterResource(R.drawable.ic_info), "查看详情", onViewDetails, showsChevron = true) }
            }
        )
        ModalMenuSectionGap()
        ModalMenuGroup(
            items = buildList {
                add { ActionRow(painterResource(R.drawable.ic_content_copy), "复制项目名称", onCopyItem) }
                add {
                    ActionRow(
                        painterResource(R.drawable.ic_content_copy),
                        "复制金额（¥${"%.2f".format(billAmount)}）",
                        onCopyAmount
                    )
                }
            }
        )
        ModalMenuSectionGap()
        ModalMenuGroup(
            items = buildList {
                add { ActionRow(painterResource(R.drawable.ic_delete), "删除账单", onDelete, isDestructive = true) }
            }
        )
    }
}
