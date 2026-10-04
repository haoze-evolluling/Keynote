package com.haoze.keynote.ui.bill.treat

import com.haoze.keynote.data.db.entity.TreatLedgerEntity
import com.haoze.keynote.data.db.entity.TreatRecordEntity
import org.junit.Assert.*
import org.junit.Test

class TreatBalanceCalculatorTest {

    private val ledger = TreatLedgerEntity(
        id = 1,
        name = "测试请客账本",
        personAName = "小明",
        personBName = "小红"
    )

    @Test
    fun testUserRequestScenario_A_Treats_200_Then_B_Treats_120() {
        // 场景：A 请客 200 元，之后 B 请客 120 元
        val record1 = TreatRecordEntity(
            id = 1,
            ledgerId = 1,
            title = "聚餐火锅",
            amount = 200.0,
            payer = 0,
            amountA = 200.0,
            amountB = 0.0,
            date = 1000L
        )
        val record2 = TreatRecordEntity(
            id = 2,
            ledgerId = 1,
            title = "看电影+奶茶",
            amount = 120.0,
            payer = 1,
            amountA = 0.0,
            amountB = 120.0,
            date = 2000L
        )

        val summary = TreatBalanceCalculator.calculateSummary(ledger, listOf(record1, record2))

        // 验证累计消费
        assertEquals(320.0, summary.totalAmount, 0.001)
        assertEquals(200.0, summary.totalA, 0.001)
        assertEquals(120.0, summary.totalB, 0.001)
        // 差额 80 元
        assertEquals(80.0, summary.diff, 0.001)
        assertEquals(80.0, summary.absDiff, 0.001)
        // A 多付了
        assertEquals(0, summary.payerMore)
        assertFalse(summary.isBalanced)

        // 建议下次由 B (小红) 请客
        assertEquals(1, summary.suggestedNextPayer)
        assertEquals("小红", summary.suggestedNextPayerName)
        // 建议消费 80 元以拉平差距
        assertEquals(80.0, summary.suggestedAmountMin, 0.001)
        assertEquals(80.0, summary.suggestedAmountMax, 0.001)

        // 验证每笔流水后的累计动态差额
        val recordsWithBal = TreatBalanceCalculator.calculateRecordsWithBalance(ledger, listOf(record1, record2))
        assertEquals(2, recordsWithBal.size)
        // 倒序：第一项为最新记录 record2
        assertEquals(2L, recordsWithBal[0].record.id)
        assertEquals(80.0, recordsWithBal[0].cumulativeDiff, 0.001)
        assertTrue(recordsWithBal[0].balanceDescription.contains("小明 多请 ¥80.00"))

        // 第二项为 record1
        assertEquals(1L, recordsWithBal[1].record.id)
        assertEquals(200.0, recordsWithBal[1].cumulativeDiff, 0.001)
        assertTrue(recordsWithBal[1].balanceDescription.contains("小明 多请 ¥200.00"))
    }

    @Test
    fun testSubsequentTreat_B_Treats_80_Achieves_Full_Balance() {
        // 接续上一场景：B 之后请客 80 元，双方达到完全平衡
        val records = listOf(
            TreatRecordEntity(1, 1, "火锅", 200.0, 0, 200.0, 0.0, 1000L),
            TreatRecordEntity(2, 1, "电影", 120.0, 1, 0.0, 120.0, 2000L),
            TreatRecordEntity(3, 1, "烤肉", 80.0, 1, 0.0, 80.0, 3000L)
        )

        val summary = TreatBalanceCalculator.calculateSummary(ledger, records)

        assertEquals(400.0, summary.totalAmount, 0.001)
        assertEquals(200.0, summary.totalA, 0.001)
        assertEquals(200.0, summary.totalB, 0.001)
        assertEquals(0.0, summary.absDiff, 0.001)
        assertTrue(summary.isBalanced)
        assertNull(summary.payerMore)

        // 上次付款人为 B (1)，建议轮流由 A (小明) 请客
        assertEquals(0, summary.suggestedNextPayer)
        assertEquals("小明", summary.suggestedNextPayerName)
    }

    @Test
    fun testRealtimeSimulation() {
        // 当前 A: 200, B: 120 (差额 80)
        // 若拟新增 B 付款 80，应提示完全持平
        val sim1 = TreatBalanceCalculator.simulateBalanceImpact(
            currentTotalA = 200.0,
            currentTotalB = 120.0,
            deltaA = 0.0,
            deltaB = 80.0,
            personAName = "小明",
            personBName = "小红"
        )
        assertTrue(sim1.contains("完全持平"))

        // 若拟新增 B 付款 50，应提示小明仍多付 30
        val sim2 = TreatBalanceCalculator.simulateBalanceImpact(
            currentTotalA = 200.0,
            currentTotalB = 120.0,
            deltaA = 0.0,
            deltaB = 50.0,
            personAName = "小明",
            personBName = "小红"
        )
        assertTrue(sim2.contains("小明") && sim2.contains("30.00"))
    }
}
