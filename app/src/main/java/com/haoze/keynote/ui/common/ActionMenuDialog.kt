package com.haoze.keynote.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haoze.keynote.ui.components.AppAlertDialog
import com.haoze.keynote.ui.components.AppDialogButton

/**
 * 选项列表弹窗：全应用「长按条目 / 更多操作」菜单的统一外壳，对齐谛听「应用列表操作」对话框。
 * 圆角、模态底色、80% 高度上限与正文滚动都委托给 AppAlertDialog；
 * 条目由调用方按语义分进若干 [com.haoze.keynote.ui.components.ModalMenuGroup]，
 * 组间用 [com.haoze.keynote.ui.components.ModalMenuSectionGap] 隔开，底部只留一个右对齐的退出键。
 */
@Composable
fun ActionMenuDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissLabel: String = "取消",
    content: @Composable ColumnScope.() -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        },
        confirmButton = {
            AppDialogButton(label = dismissLabel, onClick = onDismiss)
        }
    )
}
