package com.haoze.keynote.util.exporter

import android.content.Context
import com.haoze.keynote.data.db.BillDatabase
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BillExporter {

    suspend fun exportBills(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        categoryIds: List<Long>? = null
    ): Int {
        val db = BillDatabase.getDatabase(context)
        val repository = BillRepository(db.billDao(), db.categoryDao())

        val bills: List<BillEntity> = when {
            startDate != null && endDate != null && !categoryIds.isNullOrEmpty() ->
                repository.getBillsByDateRangeAndCategory(startDate, endDate, categoryIds)
            startDate != null && endDate != null ->
                repository.getBillsByDateRange(startDate, endDate)
            else ->
                repository.getAllBillsList()
        }

        if (bills.isEmpty()) throw Exception("没有符合条件的账单")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvContent = buildString {
            appendLine("消费项目,金额,时间")
            bills.forEach { bill ->
                val item = bill.item.replace("\"", "\"\"")
                appendLine("\"${item}\",${bill.amount},${dateFormat.format(Date(bill.date))}")
            }
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "$dateStr+账单导出.csv"
        ExportHelper.writeToDownloads(context, fileName, "text/csv", csvContent.toByteArray(Charsets.UTF_8), "Bills")
        return bills.size
    }
}
