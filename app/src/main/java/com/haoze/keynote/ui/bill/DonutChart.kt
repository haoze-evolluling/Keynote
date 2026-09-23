package com.haoze.keynote.ui.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.CategorySpending
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.SpacingTokens

@Composable
fun DonutChart(
    data: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val total = data.sumOf { it.total }
    if (total <= 0.0 || data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        return
    }

    val chartColors = colors.chartColors

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(SpacingTokens.donutChartSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = SpacingTokens.donutChartStrokeWidth.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f
                data.forEachIndexed { index, item ->
                    val sweep = (item.total / total * 360).toFloat()
                    val color = chartColors[index % chartColors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "¥${String.format("%.0f", total)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "总计",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(SpacingTokens.screenPadding))

        data.forEachIndexed { index, item ->
            val color = chartColors[index % chartColors.size]
            val percent = if (total > 0) item.total / total * 100 else 0.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.screenPadding, vertical = SpacingTokens.tinySpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(SpacingTokens.contentSpacing)) {
                    drawCircle(color = color)
                }
                Spacer(modifier = Modifier.width(SpacingTokens.smallSpacing))
                Text(
                    item.categoryName ?: "未分类",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "¥${String.format("%.2f", item.total)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${String.format("%.1f", percent)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.outline
                )
            }
        }
    }
}
