package com.haoze.keynote.ui.bill.treat

import com.haoze.keynote.data.db.entity.TreatLedgerEntity
import com.haoze.keynote.data.db.entity.TreatRecordEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 账本消费平衡总结
 */
data class TreatLedgerSummary(
    val ledger: TreatLedgerEntity,
    val totalAmount: Double,
    val totalA: Double,
    val totalB: Double,
    val diff: Double,
    val absDiff: Double,
    val ratioA: Float,
    val ratioB: Float,
    val recordCount: Int,
    val averageAmount: Double,
    val payerMore: Int?,               // 0: A 多付, 1: B 多付, null: 平衡
    val suggestedNextPayer: Int?,      // 0: 建议 A 请, 1: 建议 B 请, null: 任一方
    val suggestedNextPayerName: String,
    val suggestedAmountMin: Double,
    val suggestedAmountMax: Double,
    val suggestionTitle: String,
    val suggestionDetail: String,
    val isBalanced: Boolean
)

/**
 * 带有历史动态累计平衡信息的记录展示项
 */
data class TreatRecordWithBalance(
    val record: TreatRecordEntity,
    val cumulativeTotalA: Double,
    val cumulativeTotalB: Double,
    val cumulativeDiff: Double,
    val balanceDescription: String
)

object TreatBalanceCalculator {

    /**
     * 计算账本整体统计与下一次请客建议
     */
    fun calculateSummary(
        ledger: TreatLedgerEntity,
        records: List<TreatRecordEntity>
    ): TreatLedgerSummary {
        val totalA = records.sumOf { it.amountA }
        val totalB = records.sumOf { it.amountB }
        val totalAmount = totalA + totalB
        val diff = totalA - totalB
        val absDiff = abs(diff)
        val recordCount = records.size
        val averageAmount = if (recordCount > 0) totalAmount / recordCount else 0.0

        val ratioA = when {
            totalAmount <= 0.0 -> 0.5f
            else -> (totalA / totalAmount).toFloat().coerceIn(0f, 1f)
        }
        val ratioB = when {
            totalAmount <= 0.0 -> 0.5f
            else -> (totalB / totalAmount).toFloat().coerceIn(0f, 1f)
        }

        val isBalanced = absDiff < 0.01

        val payerMore = when {
            isBalanced -> null
            diff > 0 -> 0
            else -> 1
        }

        // 下一次建议请客方及金额计算
        val lastPayer = records.maxByOrNull { it.date }?.payer
        val (suggestedNextPayer, suggestedNextPayerName, minAmt, maxAmt, title, detail) = when {
            recordCount == 0 -> {
                SuggestionResult(
                    payer = null,
                    payerName = "任一方",
                    minAmt = 0.0,
                    maxAmt = 0.0,
                    title = "暂无消费记录",
                    detail = "第一笔消费可由任一方请客，系统将自动开始记录双方支出并维持消费平衡。"
                )
            }
            isBalanced -> {
                // 平衡状态，若上次有付款人则建议轮换，否则任一方
                val nextPayer = when (lastPayer) {
                    0 -> 1
                    1 -> 0
                    else -> null
                }
                val nextName = when (nextPayer) {
                    0 -> ledger.personAName
                    1 -> ledger.personBName
                    else -> "任一方"
                }
                val recAmt = if (averageAmount > 0) averageAmount else 100.0
                SuggestionResult(
                    payer = nextPayer,
                    payerName = nextName,
                    minAmt = recAmt,
                    maxAmt = recAmt,
                    title = "双方消费当前已完全持平 ✨",
                    detail = "双方累计实际支出均为 ¥${formatMoney(totalA)}。建议下一次由【$nextName】请客约 ¥${formatMoney(recAmt)}，开启新一轮轮换。"
                )
            }
            diff > 0 -> {
                // A 多付了，建议 B 请客
                val gap = diff
                val isLargeGap = recordCount >= 3 && averageAmount > 0 && gap > 1.5 * averageAmount
                if (isLargeGap) {
                    val steps = max(2, (gap / averageAmount).roundToInt())
                    val stepAmt = gap / steps
                    SuggestionResult(
                        payer = 1,
                        payerName = ledger.personBName,
                        minAmt = stepAmt,
                        maxAmt = gap,
                        title = "下次建议由【${ledger.personBName}】请客",
                        detail = "【${ledger.personAName}】当前累计多支出 ¥${formatMoney(gap)}。建议【${ledger.personBName}】分 $steps 次请客（每次约 ¥${formatMoney(stepAmt)}）逐步缩小差距，或单次消费 ¥${formatMoney(gap)} 一步拉平。"
                    )
                } else {
                    SuggestionResult(
                        payer = 1,
                        payerName = ledger.personBName,
                        minAmt = gap,
                        maxAmt = gap,
                        title = "下次建议由【${ledger.personBName}】请客",
                        detail = "【${ledger.personAName}】当前累计多支出 ¥${formatMoney(gap)}。建议【${ledger.personBName}】下次请客消费约 ¥${formatMoney(gap)}，即可使双方累计支出重回平衡。"
                    )
                }
            }
            else -> {
                // B 多付了，建议 A 请客
                val gap = absDiff
                val isLargeGap = recordCount >= 3 && averageAmount > 0 && gap > 1.5 * averageAmount
                if (isLargeGap) {
                    val steps = max(2, (gap / averageAmount).roundToInt())
                    val stepAmt = gap / steps
                    SuggestionResult(
                        payer = 0,
                        payerName = ledger.personAName,
                        minAmt = stepAmt,
                        maxAmt = gap,
                        title = "下次建议由【${ledger.personAName}】请客",
                        detail = "【${ledger.personBName}】当前累计多支出 ¥${formatMoney(gap)}。建议【${ledger.personAName}】分 $steps 次请客（每次约 ¥${formatMoney(stepAmt)}）逐步缩小差距，或单次消费 ¥${formatMoney(gap)} 一步拉平。"
                    )
                } else {
                    SuggestionResult(
                        payer = 0,
                        payerName = ledger.personAName,
                        minAmt = gap,
                        maxAmt = gap,
                        title = "下次建议由【${ledger.personAName}】请客",
                        detail = "【${ledger.personBName}】当前累计多支出 ¥${formatMoney(gap)}。建议【${ledger.personAName}】下次请客消费约 ¥${formatMoney(gap)}，即可使双方累计支出重回平衡。"
                    )
                }
            }
        }

        return TreatLedgerSummary(
            ledger = ledger,
            totalAmount = totalAmount,
            totalA = totalA,
            totalB = totalB,
            diff = diff,
            absDiff = absDiff,
            ratioA = ratioA,
            ratioB = ratioB,
            recordCount = recordCount,
            averageAmount = averageAmount,
            payerMore = payerMore,
            suggestedNextPayer = suggestedNextPayer,
            suggestedNextPayerName = suggestedNextPayerName,
            suggestedAmountMin = minAmt,
            suggestedAmountMax = maxAmt,
            suggestionTitle = title,
            suggestionDetail = detail,
            isBalanced = isBalanced
        )
    }

    /**
     * 计算每条记录发生时的累计状态并按时间倒序排列返回
     */
    fun calculateRecordsWithBalance(
        ledger: TreatLedgerEntity,
        records: List<TreatRecordEntity>
    ): List<TreatRecordWithBalance> {
        val sortedAsc = records.sortedWith(compareBy({ it.date }, { it.id }))
        var curA = 0.0
        var curB = 0.0

        val resultAsc = sortedAsc.map { record ->
            curA += record.amountA
            curB += record.amountB
            val diff = curA - curB
            val desc = when {
                abs(diff) < 0.01 -> "双方已持平 ✨"
                diff > 0 -> "${ledger.personAName} 多请 ¥${formatMoney(diff)}"
                else -> "${ledger.personBName} 多请 ¥${formatMoney(-diff)}"
            }
            TreatRecordWithBalance(
                record = record,
                cumulativeTotalA = curA,
                cumulativeTotalB = curB,
                cumulativeDiff = diff,
                balanceDescription = desc
            )
        }

        return resultAsc.asReversed()
    }

    /**
     * 实时模拟新增或修改记账后对双方差额的影响
     */
    fun simulateBalanceImpact(
        currentTotalA: Double,
        currentTotalB: Double,
        deltaA: Double,
        deltaB: Double,
        personAName: String,
        personBName: String
    ): String {
        val newA = currentTotalA + deltaA
        val newB = currentTotalB + deltaB
        val newDiff = newA - newB
        return when {
            abs(newDiff) < 0.01 -> "保存后：双方累计支出将完全持平 ✨"
            newDiff > 0 -> "保存后：【$personAName】将累计多请 ¥${formatMoney(newDiff)}"
            else -> "保存后：【$personBName】将累计多请 ¥${formatMoney(-newDiff)}"
        }
    }

    fun formatMoney(amount: Double): String = String.format(java.util.Locale.CHINA, "%.2f", amount)

    private data class SuggestionResult(
        val payer: Int?,
        val payerName: String,
        val minAmt: Double,
        val maxAmt: Double,
        val title: String,
        val detail: String
    )
}
