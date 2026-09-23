package com.haoze.keynote.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.SpacingTokens

@Composable
fun ActionRow(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    loadingLabel: String = "",
    enabled: Boolean = true,
    rowHeight: Dp = 48.dp,
    horizontalPadding: Dp = 16.dp
) {
    val colors = LocalAppColors.current
    val tint = when {
        isDestructive -> colors.error
        !enabled -> colors.onSurface.copy(alpha = 0.38f)
        else -> colors.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpacingTokens.iconMedium),
                strokeWidth = SpacingTokens.borderWidthThick,
                color = tint
            )
        } else {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(SpacingTokens.iconMedium)
            )
        }
        Spacer(modifier = Modifier.width(SpacingTokens.contentSpacing))
        Text(
            text = if (isLoading) loadingLabel else label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint
        )
    }
}
