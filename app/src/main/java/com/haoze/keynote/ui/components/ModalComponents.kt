package com.haoze.keynote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.haoze.keynote.R
import com.haoze.keynote.ui.theme.ModalTokens

/**
 * 全应用对话框基元，对齐「谛听」的设计语言：28dp 外壳圆角、降饱和的模态底色、内容区不超过
 * 屏高 80% 且正文自带滚动。页面只需给出 title/text/按钮，不再各自传 shape 与 containerColor。
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = ModalTokens.dialogShape,
    containerColor: Color = ModalTokens.containerColor,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = ModalTokens.onContainer,
    textContentColor: Color = ModalTokens.onContainer,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
    scrollable: Boolean = true
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * ModalTokens.maxHeightFraction
    val scrollableText: (@Composable () -> Unit)? = text?.let { content ->
        if (scrollable) {
            { Box(modifier = Modifier.verticalScroll(rememberScrollState())) { content() } }
        } else {
            content
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = Modifier.heightIn(max = maxHeight).then(modifier),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = scrollableText,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}

/** 对话框底部动作：谛听式统一用 TextButton，破坏性动作着 error 色。 */
@Composable
fun AppDialogButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(label)
    }
}

/** 确认 / 提示框：标题 + 正文 + 取消与确认，覆盖全应用最常见的模态场景。 */
@Composable
fun AppConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    cancelLabel: String? = "取消",
    destructive: Boolean = false,
    confirmEnabled: Boolean = true
) {
    val textSlot: (@Composable () -> Unit)? = message?.let { msg ->
        { Text(msg, style = ModalTokens.bodyTextStyle) }
    }
    val dismissSlot: (@Composable () -> Unit)? = cancelLabel?.let { label ->
        { AppDialogButton(label, onDismissRequest) }
    }

    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(title) },
        text = textSlot,
        confirmButton = {
            AppDialogButton(confirmLabel, onConfirm, destructive = destructive, enabled = confirmEnabled)
        },
        dismissButton = dismissSlot
    )
}

/** 对话框内的信息块：12dp 圆角 + 40% 淡底，用于提示、预览等次级内容。 */
@Composable
fun ModalInfoCard(
    modifier: Modifier = Modifier,
    color: Color = ModalTokens.innerCardColor,
    border: BorderStroke? = null,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = color,
        shape = ModalTokens.innerShape,
        border = border,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

/**
 * 谛听式菜单条目组：每行是一块独立的实底圆角卡片，行间留 2dp 缝隙，
 * 首行只圆上角、末行只圆下角、中间行四角近乎直角，使整组读作一张被切分的卡片。
 * 条目数决定末行圆角，因此这里收 List<composable> 而不是开放 ColumnScope。
 */
@Composable
fun ModalMenuGroup(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val rowColor = ModalTokens.menuRowColor
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ModalTokens.menuItemSpacing)
    ) {
        items.forEachIndexed { index, item ->
            ModalMenuCard(index = index, itemCount = items.size, containerColor = rowColor, content = item)
        }
    }
}

/** 菜单分组之间的 12dp 留白。 */
@Composable
fun ModalMenuSectionGap() {
    Spacer(modifier = Modifier.height(ModalTokens.menuSectionSpacing))
}

/** 单条菜单卡片：按在组内的位置决定上下圆角。 */
@Composable
private fun ModalMenuCard(
    index: Int,
    itemCount: Int,
    containerColor: Color,
    content: @Composable () -> Unit
) {
    val outer = ModalTokens.menuOuterRadius
    val inner = ModalTokens.menuInnerRadius
    val top = if (index == 0) outer else inner
    val bottom = if (index == itemCount - 1) outer else inner
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape),
        shape = shape,
        color = containerColor,
        content = content
    )
}

/** 底部抽屉标题栏：圆形 primaryContainer 图标徽标 + 标题/副标题 + 关闭键 + 一条发丝分割线。 */
@Composable
fun ModalSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconRes: Int? = null,
    onClose: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Surface(color = colors.primaryContainer, shape = CircleShape, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_close), "关闭", tint = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * 全应用底部抽屉基元：仅顶部两角 28dp 圆角、比对话框更浅一档的抽屉底色、6dp 色调高度，
 * 并自带谛听式标题栏与内容滚动；content 直接写正文即可。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconRes: Int? = null,
    showCloseButton: Boolean = true,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * ModalTokens.maxHeightFraction

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = ModalTokens.sheetShape,
        containerColor = ModalTokens.sheetContainerColor,
        tonalElevation = 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModalSheetHeader(
                title = title,
                subtitle = subtitle,
                iconRes = iconRes,
                onClose = if (showCloseButton) onDismissRequest else null
            )
            content()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** 日期选择对话框：与 AppAlertDialog 同档圆角与底色。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        shape = ModalTokens.dialogShape,
        colors = DatePickerDefaults.colors(containerColor = ModalTokens.containerColor),
        content = content
    )
}

/** 时间选择对话框：时钟面板不做滚动，避免与表盘手势抢事件。 */
@Composable
fun AppTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "选择时间",
    confirmLabel: String = "确定",
    cancelLabel: String = "取消"
) {
    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(title) },
        text = content,
        confirmButton = { AppDialogButton(confirmLabel, onConfirm) },
        dismissButton = { AppDialogButton(cancelLabel, onDismissRequest) },
        scrollable = false
    )
}
