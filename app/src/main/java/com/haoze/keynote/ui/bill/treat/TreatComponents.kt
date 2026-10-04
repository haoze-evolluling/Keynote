package com.haoze.keynote.ui.bill.treat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haoze.keynote.R
import com.haoze.keynote.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TreatBalanceHeaderCard(
    summary: TreatLedgerSummary,
    onSwitchLedger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val animatedRatioA by animateFloatAsState(targetValue = summary.ratioA, label = "ratioA")
    val animatedRatioB by animateFloatAsState(targetValue = summary.ratioB, label = "ratioB")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶栏：标题与账本切换入口
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "消费平衡看板",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    onClick = onSwitchLedger,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary.copy(alpha = 0.12f),
                    contentColor = colors.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = summary.ledger.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down),
                            contentDescription = "切换账本",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 双方金额与占比展示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // A 方信息
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = summary.ledger.personAName,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "¥${TreatBalanceCalculator.formatMoney(summary.totalA)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        text = "占比 ${(summary.ratioA * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                // 中间差额标签
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (summary.isBalanced) colors.priorityLow.copy(alpha = 0.15f) else colors.priorityMedium.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (summary.isBalanced) "完全持平" else "差额 ¥${TreatBalanceCalculator.formatMoney(summary.absDiff)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.isBalanced) colors.priorityLow else colors.priorityMedium
                    )
                }

                // B 方信息
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = summary.ledger.personBName,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "¥${TreatBalanceCalculator.formatMoney(summary.totalB)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.tertiary
                    )
                    Text(
                        text = "占比 ${(summary.ratioB * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            // 双方比例双色平衡条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.surfaceVariant)
            ) {
                if (summary.totalAmount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(animatedRatioA.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(colors.primary)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .weight(animatedRatioB.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(colors.tertiary)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surfaceVariant)
                    )
                }
            }

            // 状态概述文本
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        summary.isBalanced -> "✨ 双方累计支出保持完全均衡"
                        summary.payerMore == 0 -> "【${summary.ledger.personAName}】多付了 ¥${TreatBalanceCalculator.formatMoney(summary.absDiff)}"
                        else -> "【${summary.ledger.personBName}】多付了 ¥${TreatBalanceCalculator.formatMoney(summary.absDiff)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                Text(
                    text = "总计 ¥${TreatBalanceCalculator.formatMoney(summary.totalAmount)} · ${summary.recordCount}笔",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TreatSuggestionCard(
    summary: TreatLedgerSummary,
    onSettleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.primary.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_auto_awesome),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "智能轮流请客建议",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                if (!summary.isBalanced && summary.recordCount > 0) {
                    TextButton(
                        onClick = onSettleClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "平账结清",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary
                        )
                    }
                }
            }

            Text(
                text = summary.suggestionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            if (summary.suggestedAmountMin > 0) {
                val amountText = if (summary.suggestedAmountMax > summary.suggestedAmountMin + 0.01) {
                    "约 ¥${TreatBalanceCalculator.formatMoney(summary.suggestedAmountMin)} ~ ¥${TreatBalanceCalculator.formatMoney(summary.suggestedAmountMax)}"
                } else {
                    "约 ¥${TreatBalanceCalculator.formatMoney(summary.suggestedAmountMin)}"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "建议消费金额：$amountText",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
            }

            Text(
                text = summary.suggestionDetail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TreatRecordItemCard(
    item: TreatRecordWithBalance,
    personAName: String,
    personBName: String,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val record = item.record

    val (payerLabel, payerColor, avatarBg) = when (record.payer) {
        0 -> Triple(personAName, colors.primary, colors.primary.copy(alpha = 0.15f))
        1 -> Triple(personBName, colors.tertiary, colors.tertiary.copy(alpha = 0.15f))
        else -> Triple("双方分担", colors.secondary, colors.secondary.copy(alpha = 0.15f))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 头像徽章
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = payerLabel.take(1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = payerColor
            )
        }

        // 主体信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "由【$payerLabel】请客",
                    style = MaterialTheme.typography.bodySmall,
                    color = payerColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            // 记后差额微标签
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = colors.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "记后状态：${item.balanceDescription}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // 金额
        Text(
            text = "¥${TreatBalanceCalculator.formatMoney(record.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
    }
}
