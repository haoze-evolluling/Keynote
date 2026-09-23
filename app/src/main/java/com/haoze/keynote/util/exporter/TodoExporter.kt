package com.haoze.keynote.util.exporter

import android.content.Context
import com.haoze.keynote.data.db.TodoDatabase
import com.haoze.keynote.data.db.entity.TodoEntity
import com.haoze.keynote.data.repository.TodoRepository
import com.haoze.keynote.util.ExportHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TodoExporter {

    suspend fun exportTodos(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null
    ): Int {
        val db = TodoDatabase.getDatabase(context)
        val repository = TodoRepository(db.todoDao())

        val todos: List<TodoEntity> = when {
            startDate != null && endDate != null ->
                repository.getTodosByDateRange(startDate, endDate).first()
            else ->
                repository.getAllActiveTodos().first()
        }

        if (todos.isEmpty()) throw Exception("没有符合条件的待办")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dueDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvContent = buildString {
            appendLine("标题,完成状态,优先级,截止日期,备注")
            todos.forEach { todo ->
                val title = todo.title.replace("\"", "\"\"")
                val completed = if (todo.isCompleted) "已完成" else "未完成"
                val priority = when (todo.priority) {
                    2 -> "高"
                    1 -> "中"
                    else -> "低"
                }
                val dueDate = todo.dueDate?.let { dueDateFormat.format(Date(it)) } ?: ""
                val notes = (todo.notes ?: "").replace("\"", "\"\"")
                appendLine("\"$title\",$completed,$priority,\"$dueDate\",\"$notes\"")
            }
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "$dateStr+待办导出.csv"
        ExportHelper.writeToDownloads(context, fileName, "text/csv", csvContent.toByteArray(Charsets.UTF_8), "Todos")
        return todos.size
    }
}
