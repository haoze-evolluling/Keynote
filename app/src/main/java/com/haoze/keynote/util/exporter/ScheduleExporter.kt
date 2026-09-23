package com.haoze.keynote.util.exporter

import android.content.Context
import com.haoze.keynote.data.db.ScheduleDatabase
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.data.repository.ScheduleRepository
import com.haoze.keynote.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScheduleExporter {

    enum class ScheduleExportFormat { ICS, CSV }

    suspend fun exportSchedules(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        format: ScheduleExportFormat = ScheduleExportFormat.ICS
    ): Int {
        val db = ScheduleDatabase.getDatabase(context)
        val repository = ScheduleRepository(db.scheduleDao())

        val schedules: List<ScheduleEntity> = when {
            startDate != null && endDate != null ->
                repository.getSchedulesByDateRangeList(startDate, endDate)
            else ->
                repository.getAllSchedulesList()
        }

        if (schedules.isEmpty()) throw Exception("没有符合条件的日程")

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        when (format) {
            ScheduleExportFormat.ICS -> {
                val fileName = "$dateStr+日程导出.ics"
                val content = generateIcs(schedules)
                ExportHelper.writeToDownloads(context, fileName, "text/calendar", content.toByteArray(Charsets.UTF_8), "Schedules")
            }
            ScheduleExportFormat.CSV -> {
                val fileName = "$dateStr+日程导出.csv"
                val content = generateScheduleCsv(schedules)
                ExportHelper.writeToDownloads(context, fileName, "text/csv", content.toByteArray(Charsets.UTF_8), "Schedules")
            }
        }
        return schedules.size
    }

    private fun generateIcs(schedules: List<ScheduleEntity>): String = buildString {
        val dateTimeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//KeyNote//Export//CN")
        appendLine("CALSCALE:GREGORIAN")
        schedules.forEach { schedule ->
            appendLine("BEGIN:VEVENT")
            appendLine("DTSTART:${dateTimeFormat.format(Date(schedule.date))}")
            if (schedule.endDate != null) {
                appendLine("DTEND:${dateTimeFormat.format(Date(schedule.endDate))}")
            }
            appendLine("SUMMARY:${escapeIcs(schedule.title)}")
            if (!schedule.location.isNullOrBlank()) {
                appendLine("LOCATION:${escapeIcs(schedule.location)}")
            }
            if (!schedule.description.isNullOrBlank()) {
                appendLine("DESCRIPTION:${escapeIcs(schedule.description)}")
            }
            appendLine("UID:keynote-${schedule.id}@haoze")
            appendLine("DTSTAMP:${dateTimeFormat.format(Date())}")
            appendLine("END:VEVENT")
        }
        appendLine("END:VCALENDAR")
    }

    private fun escapeIcs(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }

    private fun generateScheduleCsv(schedules: List<ScheduleEntity>): String = buildString {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        appendLine("标题,开始时间,结束时间,地点,描述")
        schedules.forEach { schedule ->
            val title = schedule.title.replace("\"", "\"\"")
            val start = dateFormat.format(Date(schedule.date))
            val end = if (schedule.endDate != null) dateFormat.format(Date(schedule.endDate)) else ""
            val location = (schedule.location ?: "").replace("\"", "\"\"")
            val desc = (schedule.description ?: "").replace("\"", "\"\"")
            appendLine("\"$title\",\"$start\",\"$end\",\"$location\",\"$desc\"")
        }
    }
}
