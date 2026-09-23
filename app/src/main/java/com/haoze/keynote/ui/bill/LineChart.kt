package com.haoze.keynote.ui.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.DailySpending
import com.haoze.keynote.data.db.entity.MonthlySpending
import com.haoze.keynote.data.db.entity.WeeklySpending
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.SpacingTokens

data class ChartPoint(val label: String, val value: Double)

@JvmName("monthlyToChartPoints")
fun List<MonthlySpending>.toChartPoints(): List<ChartPoint> =
    map { ChartPoint(it.month.takeLast(2) + "月", it.total) }

@JvmName("dailyToChartPoints")
fun List<DailySpending>.toChartPoints(): List<ChartPoint> =
    map { ChartPoint(it.day.takeLast(5), it.total) }

@JvmName("weeklyToChartPoints")
fun List<WeeklySpending>.toChartPoints(): List<ChartPoint> =
    map { ChartPoint(it.week.takeLast(2) + "周", it.total) }

@Composable
fun LineChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        return
    }

    val lineColor = colors.primary
    val gridColor = colors.outlineVariant
    val textColor = colors.onSurfaceVariant
    val surfaceColor = colors.surface

    val paddingLeftDp = SpacingTokens.chartPaddingLeft
    val paddingRightDp = SpacingTokens.chartPaddingRight
    val paddingTopDp = SpacingTokens.chartPaddingTop
    val paddingBottomDp = SpacingTokens.chartPaddingBottom
    val pointSpacingDp = SpacingTokens.chartPointSpacing

    val totalWidthDp = (paddingLeftDp + paddingRightDp + pointSpacingDp * data.size).coerceAtLeast(SpacingTokens.chartMinWidth)

    val scrollState = rememberScrollState()

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(totalWidthDp)
                    .height(SpacingTokens.chartHeight)
            ) {
                val paddingLeft = paddingLeftDp.toPx()
                val paddingRight = paddingRightDp.toPx()
                val paddingTop = paddingTopDp.toPx()
                val paddingBottom = paddingBottomDp.toPx()
                val chartHeight = size.height - paddingTop - paddingBottom

                val maxVal = data.maxOf { it.value }.coerceAtLeast(1.0)

                for (i in 0..4) {
                    val y = paddingTop + chartHeight * i / 4
                    drawLine(gridColor, Offset(paddingLeft, y), Offset(size.width - paddingRight, y), strokeWidth = SpacingTokens.chartGridStrokeWidth.toPx())
                }

                val path = Path()
                val points = data.mapIndexed { index, item ->
                    val x = paddingLeft + index * pointSpacingDp.toPx()
                    val y = paddingTop + chartHeight * (1 - item.value / maxVal).toFloat()
                    Offset(x, y)
                }
                path.moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { path.lineTo(it.x, it.y) }
                drawPath(path, lineColor, style = Stroke(width = SpacingTokens.chartStrokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                points.forEach { point ->
                    drawCircle(lineColor, radius = SpacingTokens.chartPointRadius.toPx(), center = point)
                    drawCircle(surfaceColor, radius = SpacingTokens.chartInnerPointRadius.toPx(), center = point)
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
                    .width(totalWidthDp)
                    .padding(start = paddingLeftDp, end = paddingRightDp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { item ->
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            }
        }
    }
}
