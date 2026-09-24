package com.haoze.keynote.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haoze.keynote.ui.components.AppAlertDialog
import com.haoze.keynote.ui.components.AppDialogButton

/**
 * 选项列表弹窗：全应用「长按条目 / 更多操作」菜单的统一外壳。
 * 圆角、模态底色、80% 高度上限与正文滚动都委托给 AppAlertDialog，
 * 这里只负责标题、条目容器与关闭键，调用方继续只写 ActionRow。
 */
@Composable
fun ActionMenuDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        },
        confirmButton = {
            AppDialogButton(label = "关闭", onClick = onDismiss)
        }
    )
}
