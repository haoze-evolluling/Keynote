package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.NoteDao
import com.haoze.keynote.data.db.dao.TagDao
import com.haoze.keynote.data.db.entity.*
import com.haoze.keynote.data.remote.AiApiManager
import com.haoze.keynote.data.remote.DeepSeekRequest
import com.haoze.keynote.data.remote.Message
import com.haoze.keynote.util.AppConstants
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class NoteRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val preferencesManager: PreferencesManager
) {
    private val apiManager = AiApiManager(preferencesManager)

    fun getAllActiveNotesWithTags(): Flow<List<NoteWithTags>> = noteDao.getAllActiveNotesWithTags()

    fun getAllDeletedNotes(): Flow<List<NoteWithTags>> = noteDao.getAllDeletedNotes()

    fun getNoteWithTagsById(noteId: Long): Flow<NoteWithTags?> = noteDao.getNoteWithTagsById(noteId)

    fun searchNotesWithTags(query: String): Flow<List<NoteWithTags>> =
        noteDao.searchNotesWithTags(query)

    fun getActiveNotesByDateRange(start: Long, end: Long): Flow<List<NoteWithTags>> =
        noteDao.getActiveNotesByDateRange(start, end)

    fun getActiveNotesByDateRangeAndTags(start: Long, end: Long, tagIds: List<Long>): Flow<List<NoteWithTags>> =
        noteDao.getActiveNotesByDateRangeAndTags(start, end, tagIds)

    suspend fun createNote(title: String, content: String): Long {
        val note = NoteEntity(title = title, content = content)
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun softDeleteNote(note: NoteEntity) {
        val noteWithTags = noteDao.getNoteWithTagsById(note.id).first()
        noteDao.softDeleteNote(note.id)
        noteWithTags?.tags?.forEach { tag ->
            if (tagDao.getTagUsageCount(tag.id) == 0) {
                tagDao.deleteTag(tag)
            }
        }
    }

    suspend fun restoreNote(note: NoteEntity) {
        noteDao.restoreNote(note.id)
    }

    suspend fun permanentlyDeleteNote(note: NoteEntity) {
        val noteWithTags = noteDao.getNoteWithTagsById(note.id).first()
        noteDao.permanentlyDeleteNote(note.id)
        noteWithTags?.tags?.forEach { tag ->
            if (tagDao.getTagUsageCount(tag.id) == 0) {
                tagDao.deleteTag(tag)
            }
        }
    }

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getActiveTags(): Flow<List<TagEntity>> = tagDao.getActiveTags()

    suspend fun addTagToNote(noteId: Long, tagName: String) {
        val trimmedName = tagName.trim().removePrefix("#")
        if (trimmedName.isBlank()) return

        var tag = tagDao.getTagByName(trimmedName)
        if (tag == null) {
            val tagId = tagDao.insertTag(TagEntity(name = trimmedName))
            tag = TagEntity(id = tagId, name = trimmedName)
        }
        noteDao.insertCrossRef(NoteTagCrossRef(noteId = noteId, tagId = tag.id))
    }

    suspend fun removeTagFromNote(noteId: Long, tagId: Long) {
        noteDao.deleteTagFromNote(noteId, tagId)
        if (tagDao.getTagUsageCount(tagId) == 0) {
            tagDao.getTagById(tagId)?.let { tagDao.deleteTag(it) }
        }
    }

    suspend fun generateTagsForNote(noteId: Long): Result<List<String>> {
        val noteWithTags = noteDao.getNoteWithTagsById(noteId).first()
        val note = noteWithTags?.note ?: return Result.failure(Exception("Note not found"))
        val provider = apiManager.getActiveProvider()
        val apiKey = apiManager.resolveApiKey(provider)

        if (provider == null || apiKey.isBlank()) {
            return Result.failure(Exception("API Key 未配置，请在设置中配置"))
        }

        return try {
            val api = apiManager.createApi()
            val request = DeepSeekRequest(
                model = provider.modelName,
                messages = listOf(
                    Message(
                        role = "system",
                        content = "分析以下笔记内容，生成 1-3 个中文标签。内容简短时可用单个标签概括。直接返回标签，用逗号分隔，不要加引号、反引号或任何额外文字。"
                    ),
                    Message(role = "user", content = note.content)
                )
            )
            val response = api.generateTags(auth = "Bearer $apiKey", request = request)
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            // Sanitize: strip markdown backticks/formatting, trim whitespace
            val cleaned = content
                .replace(Regex("```[a-zA-Z]*\\n?|```|`"), "")
                .trim()
            val tags = cleaned.split(Regex("[，,、\\s]+"))
                .map { it.trim().removePrefix("#").removePrefix(".").removeSuffix(".") }
                .filter { it.isNotBlank() && !it.matches(Regex("^[\\d.\\-*]+$")) }
                .take(AppConstants.AI_TAG_MAX_COUNT)

            // Save generated tags to the note
            for (tagName in tags) {
                addTagToNote(noteId, tagName)
            }

            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTitle(noteId: Long): Result<String> {
        val noteWithTags = noteDao.getNoteWithTagsById(noteId).first()
        val note = noteWithTags?.note ?: return Result.failure(Exception("笔记不存在"))
        val provider = apiManager.getActiveProvider()
        val apiKey = apiManager.resolveApiKey(provider)

        if (provider == null || apiKey.isBlank()) {
            return Result.failure(Exception("API Key 未配置，请在设置中配置"))
        }

        return try {
            val api = apiManager.createApi()
            val request = DeepSeekRequest(
                model = provider.modelName,
                messages = listOf(
                    Message(role = "system", content = "根据以下笔记内容生成一个简洁的中文标题（不超过15个字）。如果内容较短或只有关键词，合理概括即可。直接返回标题文字，不要加引号、反引号、括号或任何额外格式。"),
                    Message(role = "user", content = note.content)
                ),
                maxTokens = 150
            )
            val response = api.generateTags(auth = "Bearer $apiKey", request = request)
            val rawTitle = response.choices.firstOrNull()?.message?.content
                ?.trim() ?: return Result.failure(Exception("AI 返回为空"))
            // Sanitize: strip markdown backticks/formatting
            val title = rawTitle
                .replace(Regex("```[a-zA-Z]*\\n?|```|`"), "")
                .trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
            if (title.isBlank()) return Result.failure(Exception("生成的标题为空"))

            noteDao.updateNote(note.copy(title = title, updatedAt = System.currentTimeMillis()))

            Result.success(title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun summarizeNote(noteId: Long): Result<String> {
        val noteWithTags = noteDao.getNoteWithTagsById(noteId).first()
        val note = noteWithTags?.note ?: return Result.failure(Exception("笔记不存在"))
        val provider = apiManager.getActiveProvider()
        val apiKey = apiManager.resolveApiKey(provider)

        if (provider == null || apiKey.isBlank()) {
            return Result.failure(Exception("API Key 未配置，请在设置中配置"))
        }

        return try {
            val api = apiManager.createApi()
            val request = DeepSeekRequest(
                model = provider.modelName,
                messages = listOf(
                    Message(role = "system", content = "用 1-3 句话概括以下笔记的核心要点。内容简短时一两句即可，不要强行凑字数。直接返回概括文字，不要加任何格式标记。"),
                    Message(role = "user", content = "标题: ${note.title}\n\n内容: ${note.content}")
                ),
                maxTokens = 300
            )
            val response = api.generateTags(auth = "Bearer $apiKey", request = request)
            val summary = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("AI 返回为空"))
            val trimmed = summary.trim()

            Result.success(trimmed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun polishNote(content: String): Result<String> {
        val provider = apiManager.getActiveProvider()
        val apiKey = apiManager.resolveApiKey(provider)

        if (provider == null || apiKey.isBlank()) {
            return Result.failure(Exception("API Key 未配置，请在设置中配置"))
        }

        return try {
            val api = apiManager.createApi()
            val maxTokens = maxOf((content.length * 1.5).toInt(), 500)
            val request = DeepSeekRequest(
                model = provider.modelName,
                messages = listOf(
                    Message(role = "system", content = "请在不改变原意的前提下，润色以下文本的表达和语言，使其更流畅、专业。保持原文的结构和格式。只返回润色后的文本，不要添加任何解释。"),
                    Message(role = "user", content = content)
                ),
                maxTokens = maxTokens
            )
            val response = api.generateTags(auth = "Bearer $apiKey", request = request)
            val polished = response.choices.firstOrNull()?.message?.content?.trim()
                ?: return Result.failure(Exception("AI 返回为空"))
            Result.success(polished)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNoteSummary(noteId: Long, summary: String?) {
        val noteWithTags = noteDao.getNoteWithTagsById(noteId).first()
        noteWithTags?.let {
            noteDao.updateNote(it.note.copy(summary = summary, updatedAt = System.currentTimeMillis()))
        }
    }
}
