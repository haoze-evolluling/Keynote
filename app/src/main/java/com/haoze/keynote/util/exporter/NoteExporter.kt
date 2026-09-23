package com.haoze.keynote.util.exporter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.util.ExportHelper
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NoteExporter {

    enum class NoteExportFormat { MARKDOWN, TXT, PDF }

    suspend fun exportNotes(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        tagIds: List<Long>? = null,
        format: NoteExportFormat = NoteExportFormat.MARKDOWN
    ): Int {
        val db = NoteDatabase.getDatabase(context)
        val repository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(context))

        val notes: List<NoteWithTags> = when {
            startDate != null && endDate != null && !tagIds.isNullOrEmpty() ->
                repository.getActiveNotesByDateRangeAndTags(startDate, endDate, tagIds).first()
            startDate != null && endDate != null ->
                repository.getActiveNotesByDateRange(startDate, endDate).first()
            else ->
                repository.getAllActiveNotesWithTags().first()
        }

        if (notes.isEmpty()) throw Exception("没有符合条件的笔记")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        notes.forEach { noteWithTags ->
            val safeTitle = noteWithTags.note.title
                .ifBlank { "无标题" }
                .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val timestamp = dateFormat.format(Date(noteWithTags.note.createdAt))

            when (format) {
                NoteExportFormat.MARKDOWN -> {
                    val fileName = "$timestamp+$safeTitle.md"
                    val content = buildMarkdown(noteWithTags)
                    ExportHelper.writeToDownloads(context, fileName, "text/markdown", content.toByteArray(Charsets.UTF_8), "Notes")
                }
                NoteExportFormat.TXT -> {
                    val fileName = "$timestamp+$safeTitle.txt"
                    val content = buildTxt(noteWithTags)
                    ExportHelper.writeToDownloads(context, fileName, "text/plain", content.toByteArray(Charsets.UTF_8), "Notes")
                }
                NoteExportFormat.PDF -> {
                    val fileName = "$timestamp+$safeTitle.pdf"
                    val content = generatePdf(noteWithTags)
                    ExportHelper.writeToDownloads(context, fileName, "application/pdf", content, "Notes")
                }
            }
        }
        return notes.size
    }

    private fun buildMarkdown(noteWithTags: NoteWithTags): String = buildString {
        appendLine("# ${noteWithTags.note.title}")
        appendLine()
        if (noteWithTags.tags.isNotEmpty()) {
            appendLine("标签: ${noteWithTags.tags.joinToString(" ") { "#${it.name}" }}")
            appendLine()
        }
        appendLine(noteWithTags.note.content)
    }

    private fun buildTxt(noteWithTags: NoteWithTags): String = buildString {
        appendLine(noteWithTags.note.title)
        appendLine()
        if (noteWithTags.tags.isNotEmpty()) {
            appendLine("标签: ${noteWithTags.tags.joinToString(", ") { it.name }}")
            appendLine()
        }
        appendLine(noteWithTags.note.content)
    }

    private fun generatePdf(noteWithTags: NoteWithTags): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            textSize = 18f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val tagPaint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
            color = android.graphics.Color.GRAY
        }

        var y = 40f
        val margin = 40f
        val maxWidth = 595f - margin * 2

        canvas.drawText(noteWithTags.note.title, margin, y, titlePaint)
        y += 30f

        if (noteWithTags.tags.isNotEmpty()) {
            val tagText = "标签: ${noteWithTags.tags.joinToString(", ") { it.name }}"
            canvas.drawText(tagText, margin, y, tagPaint)
            y += 20f
        }
        y += 10f

        val lines = noteWithTags.note.content.split("\n")
        for (line in lines) {
            if (y > 800f) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                page = document.startPage(newPageInfo)
                canvas = page.canvas
                y = 40f
            }
            if (line.isBlank()) {
                y += 14f
                continue
            }
            val chars = line.toList()
            var currentLine = ""
            for (ch in chars) {
                val testLine = currentLine + ch
                if (paint.measureText(testLine) > maxWidth) {
                    canvas.drawText(currentLine, margin, y, paint)
                    y += 16f
                    currentLine = ch.toString()
                    if (y > 800f) {
                        document.finishPage(page)
                        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                        page = document.startPage(newPageInfo)
                        canvas = page.canvas
                        y = 40f
                    }
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine.isNotBlank()) {
                canvas.drawText(currentLine, margin, y, paint)
                y += 16f
            }
        }

        document.finishPage(page)
        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }
}
