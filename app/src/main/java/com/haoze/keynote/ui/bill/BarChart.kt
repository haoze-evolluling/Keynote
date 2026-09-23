package com.haoze.keynote.ui.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.DailySpending
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.SpacingTokens

@Composable
fun BarChart(
    data: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        return
    }

    val maxVal = data.maxOf { it.total }.coerceAtLeast(1.0)
    val gridColor = colors.outlineVariant
    val textColor = colors.onSurfaceVariant
    val barColor = colors.primary

    val barWidthDp = SpacingTokens.chartBarWidth
    val barSpacingDp = SpacingTokens.chartBarSpacing
    val paddingLeftDp = SpacingTokens.chartPaddingLeft
    val paddingRightDp = SpacingTokens.chartPaddingRight

    val totalBarWidthDp = (barWidthDp + barSpacingDp) * data.size - barSpacingDp
    val chartMinWidthDp = (paddingLeftDp + totalBarWidthDp + paddingRightDp).coerceAtLeast(SpacingTokens.chartMinWidth)

    val scrollState = rememberScrollState()

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartMinWidthDp)
                    .height(SpacingTokens.chartHeight)
            ) {
                val paddingLeft = paddingLeftDp.toPx()
                val paddingRight = paddingRightDp.toPx()
                val barWidthPx = barWidthDp.toPx()
                val barSpacingPx = barSpacingDp.toPx()
                val paddingBottom = SpacingTokens.chartPaddingBottom.toPx()
                val paddingTop = SpacingTokens.chartPaddingTop.toPx()
                val chartHeight = size.height - paddingTop - paddingBottom

                for (i in 0..4) {
                    val y = paddingTop + chartHeight * i / 4
                    drawLine(
                        gridColor,
                        Offset(paddingLeft, y),
                        Offset(size.width - paddingRight, y),
                        strokeWidth = SpacingTokens.chartGridStrokeWidth.toPx()
                    )
                }

                data.forEachIndexed { index, item ->
                    val barHeight = (item.total / maxVal * chartHeight).toFloat()
                    val x = paddingLeft + index * (barWidthPx + barSpacingPx)
                    val y = paddingTop + chartHeight - barHeight

                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidthPx, barHeight)
                    )

                    if (data.size <= 15) {
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = textColor.toArgb()
                                textSize = 10.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            canvas.nativeCanvas.drawText(
                                "¥${String.format("%.0f", item.total)}",
                                x + barWidthPx / 2,
                                y - SpacingTokens.tinySpacing.toPx(),
                                paint
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingTokens.tinySpacing))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .width(chartMinWidthDp)
                    .padding(start = paddingLeftDp, end = paddingRightDp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { item ->
                    Text(
                        item.day.takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            }
        }
    }
}
