package com.haoze.keynote.util.exporter

import android.content.Context
import com.haoze.keynote.data.db.dao.AaSplitDao
import com.haoze.keynote.data.db.entity.AaSplitEntity
import com.haoze.keynote.util.ExportHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AaSplitExporter {

    suspend fun exportAaSplits(
        context: Context,
        aaSplitDao: AaSplitDao,
        startDate: Long? = null,
        endDate: Long? = null
    ): Int {
        val aaSplits: List<AaSplitEntity> = when {
            startDate != null && endDate != null ->
                aaSplitDao.getAaSplitsByDateRange(startDate, endDate).first()
            else ->
                aaSplitDao.getAllAaSplits().first()
        }

        if (aaSplits.isEmpty()) throw Exception("没有符合条件的AA计算")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvContent = buildString {
            appendLine("标题,总金额,人数,人均金额,日期,备注")
            aaSplits.forEach { split ->
                val title = split.title.replace("\"", "\"\"")
                val note = (split.note ?: "").replace("\"", "\"\"")
                val date = dateFormat.format(Date(split.date))
                appendLine("\"$title\",${split.totalAmount},${split.personCount},${split.perPersonAmount},\"$date\",\"$note\"")
            }
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "$dateStr+AA计算导出.csv"
        ExportHelper.writeToDownloads(context, fileName, "text/csv", csvContent.toByteArray(Charsets.UTF_8), "AaSplits")
        return aaSplits.size
    }
}
