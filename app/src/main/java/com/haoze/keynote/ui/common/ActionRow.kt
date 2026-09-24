package com.haoze.keynote.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoze.keynote.R
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens

/**
 * 谛听式菜单条目：整行可点，左侧 24dp 图标着 primary（破坏性动作着 error），
 * 标题用 bodyLarge 与「设置」页条目同档，右侧按语义挂上选中对勾或下钻箭头。
 * 实底圆角背景由外层 [com.haoze.keynote.ui.components.ModalMenuGroup] 提供，本行只负责内容。
 */
@Composable
fun ActionRow(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    loadingLabel: String = "",
    enabled: Boolean = true,
    selected: Boolean = false,
    showsChevron: Boolean = false
) {
    val colors = LocalAppColors.current
    val disabledAlpha = if (enabled) 1f else 0.38f
    val iconTint = (if (isDestructive) colors.error else colors.primary).copy(alpha = disabledAlpha)
    val labelTint = (if (isDestructive) colors.error else colors.onSurface).copy(alpha = disabledAlpha)
    val supportingTint = colors.onSurfaceVariant.copy(alpha = disabledAlpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ModalTokens.menuRowMinHeight)
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .padding(
                horizontal = ModalTokens.menuRowHorizontalPadding,
                vertical = ModalTokens.menuRowVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ModalTokens.menuRowIconSize),
                strokeWidth = 2.dp,
                color = iconTint
            )
        } else {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(ModalTokens.menuRowIconSize)
            )
        }
        Spacer(modifier = Modifier.width(ModalTokens.menuRowElementSpacing))
        Text(
            text = if (isLoading && loadingLabel.isNotEmpty()) loadingLabel else label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelTint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(modifier = Modifier.width(ModalTokens.menuRowElementSpacing))
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = "已选中",
                tint = colors.primary,
                modifier = Modifier.size(ModalTokens.menuRowIconSize)
            )
        }
        if (showsChevron) {
            Spacer(modifier = Modifier.width(ModalTokens.menuRowElementSpacing))
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = null,
                tint = supportingTint,
                modifier = Modifier.size(ModalTokens.menuRowIconSize)
            )
        }
    }
}
