package com.haoze.keynote.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.SpacingTokens

import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    // Bluke 卡片式预览容器：无描边、surfaceVariant@50%、28dp 圆角
    val containerModifier = modifier
        .background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            RoundedCornerShape(SpacingTokens.listCardRadius)
        )
    if (content.isBlank()) {
        Text(
            "暂无内容",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            modifier = containerModifier
                .padding(SpacingTokens.screenPadding)
        )
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = containerModifier
            .padding(SpacingTokens.screenPadding)
            .verticalScroll(scrollState)
    ) {
        MarkdownText(
            markdown = content,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
