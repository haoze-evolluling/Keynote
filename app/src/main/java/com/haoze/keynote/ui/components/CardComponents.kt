package com.haoze.keynote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haoze.keynote.ui.theme.SpacingTokens

/**
 * Bluke 设计语言共享组件。
 *
 * 规范要点：
 * - 卡片统一 [MaterialTheme.colorScheme.surfaceVariant].copy(alpha = 0.5f)，无阴影无边框
 * - 分组列表：条目间 2dp 细线镂空间隙，首尾条目外角 28dp、中间条目 4dp
 * - 列表独立卡片：28dp 圆角
 * - 状态 pill：12dp 圆角；小徽章：6dp 圆角
 * - 条目图标：40dp 圆形容器 + 20dp 图标
 * - 区块标题：bodyMedium + Bold + primary 色
 */

/** 区块标题，对齐 Bluke 的 "PAIRED DEVICES" 风格 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (trailing != null) {
            trailing()
        }
    }
}

/** 40dp 圆形图标容器，对齐 Bluke DeviceRow 的条目图标 */
@Composable
fun IconCircle(
    icon: Painter,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = SpacingTokens.iconCircleSize
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(SpacingTokens.iconCircleInner)
        )
    }
}

@Composable
fun IconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = SpacingTokens.iconCircleSize
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(SpacingTokens.iconCircleInner)
        )
    }
}

/** 状态 pill：12dp 圆角 + 圆点指示灯 + 加粗文字，对齐 Bluke 连接状态块 */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(shape = RoundedCornerShape(SpacingTokens.statusPillRadius), color = containerColor) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/** 小徽章：6dp 圆角 + 加粗小字，对齐 Bluke 的标签样式 */
@Composable
fun MiniBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(SpacingTokens.tagRadius))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * Bluke 式分组卡片组：条目纵向堆叠，条目间 2dp 细线镂空间隙，
 * 首条目顶部圆角 28dp、末条目底部圆角 28dp，中间条目 4dp。
 * 每个条目由 [itemContent] 独立渲染，index/total 用于计算圆角。
 */
@Composable
fun BlukeCardGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    contentPaddingHorizontal: Dp = 16.dp,
    itemCount: Int,
    itemContent: @Composable ColumnScope.(index: Int) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.padding(
                    start = contentPaddingHorizontal + 16.dp,
                    end = contentPaddingHorizontal,
                    top = 24.dp,
                    bottom = 8.dp
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.cardGap)
        ) {
            repeat(itemCount) { index ->
                itemContent(index)
            }
        }
    }
}

/**
 * 分组内的单个条目容器：自动按位置取外角 28dp / 内角 4dp，
 * 背景为 Bluke 标准卡片色（surfaceVariant @ 50%）。
 */
@Composable
fun GroupedItemSurface(
    index: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val topRadius = if (index == 0) SpacingTokens.groupedOuterRadius else SpacingTokens.groupedInnerRadius
    val bottomRadius = if (index == totalCount - 1) SpacingTokens.groupedOuterRadius else SpacingTokens.groupedInnerRadius
    val shape = RoundedCornerShape(
        topStart = topRadius, topEnd = topRadius,
        bottomStart = bottomRadius, bottomEnd = bottomRadius
    )
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor
        ) {
            content()
        }
    }
}
