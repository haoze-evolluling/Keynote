package com.haoze.keynote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.NoteEntity
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.TagEntity
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.util.AppConstants
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray

class EditNoteViewModel(
    private val preferencesManager: PreferencesManager,
    private val repository: NoteRepository
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())
    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()

    private val _isGeneratingTags = MutableStateFlow(false)
    val isGeneratingTags: StateFlow<Boolean> = _isGeneratingTags.asStateFlow()

    private val _isGeneratingTitle = MutableStateFlow(false)
    val isGeneratingTitle: StateFlow<Boolean> = _isGeneratingTitle.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var noteId: Long = -1L
    private var originalCreatedAt: Long = System.currentTimeMillis()
    private var loadNoteJob: Job? = null

    private val _summaries = MutableStateFlow<List<String>>(emptyList())
    val summaries: StateFlow<List<String>> = _summaries.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _isPolishing = MutableStateFlow(false)
    val isPolishing: StateFlow<Boolean> = _isPolishing.asStateFlow()

    private val _polishedText = MutableStateFlow<String?>(null)
    val polishedText: StateFlow<String?> = _polishedText.asStateFlow()

    private val _isPreview = MutableStateFlow(false)
    val isPreview: StateFlow<Boolean> = _isPreview.asStateFlow()

    val noteFontSize: StateFlow<Int> = preferencesManager.noteFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), AppConstants.DEFAULT_FONT_SIZE)

    private var previewJob: Job? = null

    data class UndoEntry(val title: String, val content: String)

    private val undoStack = mutableListOf<UndoEntry>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private var undoDebounceJob: Job? = null
    private val undoDebounceMillis = AppConstants.UNDO_DEBOUNCE_MILLIS

    init {
    }

    fun loadNote(id: Long) {
        noteId = id
        loadNoteJob?.cancel()
        loadNoteJob = viewModelScope.launch {
            var isFirstEmission = true
            repository.getNoteWithTagsById(id).collect { noteWithTags ->
                if (noteWithTags != null) {
                    if (isFirstEmission) {
                        _title.value = noteWithTags.note.title
                        _content.value = noteWithTags.note.content
                        _summaries.value = parseSummaries(noteWithTags.note.summary)
                        originalCreatedAt = noteWithTags.note.createdAt
                        isFirstEmission = false
                    }
                    _tags.value = noteWithTags.tags
                }
            }
        }
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            preferencesManager.markdownModeForNote(id).collect { mode ->
                _isPreview.value = mode
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _title.value = newTitle
        scheduleUndoSnapshot()
    }
    fun onContentChanged(newContent: String) {
        _content.value = newContent
        scheduleUndoSnapshot()
    }

    private fun scheduleUndoSnapshot() {
        undoDebounceJob?.cancel()
        undoDebounceJob = viewModelScope.launch {
            delay(undoDebounceMillis)
            pushSnapshot()
        }
    }

    private fun pushSnapshot() {
        undoStack.add(UndoEntry(_title.value, _content.value))
        if (undoStack.size > AppConstants.UNDO_STACK_MAX_SIZE) {
            undoStack.removeFirst()
        }
        _canUndo.value = undoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val entry = undoStack.removeLast()
            _title.value = entry.title
            _content.value = entry.content
            _canUndo.value = undoStack.isNotEmpty()
        }
    }

    private fun clearUndoStack() {
        undoStack.clear()
        _canUndo.value = false
    }

    fun saveNoteWithFeedback() {
        viewModelScope.launch {
            saveNoteToDb()
            _snackbarMessage.emit("已保存")
        }
    }

    fun saveOrDeleteIfEmpty(onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            if (_title.value.isBlank() && _content.value.isBlank()) {
                repository.permanentlyDeleteNote(
                    NoteEntity(
                        id = noteId,
                        title = "",
                        content = "",
                        createdAt = originalCreatedAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                saveNoteToDb()
            }
            onComplete(noteId)
        }
    }

    private suspend fun saveNoteToDb() {
        repository.updateNote(
            NoteEntity(
                id = noteId,
                title = _title.value,
                content = _content.value,
                summary = summariesToJson(_summaries.value),
                createdAt = originalCreatedAt,
                updatedAt = System.currentTimeMillis()
            )
        )
        clearUndoStack()
    }

    fun deleteNote() {
        viewModelScope.launch {
            val noteWithTags = repository.getNoteWithTagsById(noteId).first()
            noteWithTags?.let { repository.softDeleteNote(it.note) }
        }
    }

    fun addTag(tagName: String) {
        viewModelScope.launch {
            repository.addTagToNote(noteId, tagName)
        }
    }

    fun removeTag(tagId: Long) {
        viewModelScope.launch {
            repository.removeTagFromNote(noteId, tagId)
        }
    }

    fun generateTags() {
        if (_content.value.isBlank()) {
            viewModelScope.launch {
                _snackbarMessage.emit("请先输入笔记内容")
            }
            return
        }
        _isGeneratingTags.value = true
        viewModelScope.launch {
            try {
                saveNoteToDb()
                val result = repository.generateTagsForNote(noteId)
                result.onSuccess { tags ->
                    _snackbarMessage.emit("生成了标签: ${tags.joinToString(", ")}")
                }.onFailure { error ->
                    _snackbarMessage.emit(error.message ?: "生成失败")
                }
            } finally {
                _isGeneratingTags.value = false
            }
        }
    }

    fun generateTitleFromContent() {
        if (_content.value.isBlank()) {
            viewModelScope.launch {
                _snackbarMessage.emit("正文内容为空，无法生成标题")
            }
            return
        }
        _isGeneratingTitle.value = true
        viewModelScope.launch {
            try {
                saveNoteToDb()
                val result = repository.generateTitle(noteId)
                result.onSuccess { title ->
                    _title.value = title
                    _snackbarMessage.emit("标题已生成")
                }.onFailure { error ->
                    _snackbarMessage.emit(error.message ?: "标题生成失败")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("生成失败: ${e.message}")
            } finally {
                _isGeneratingTitle.value = false
            }
        }
    }

    fun summarizeNote() {
        if (_content.value.isBlank()) {
            viewModelScope.launch {
                _snackbarMessage.emit("笔记内容为空")
            }
            return
        }
        _isSummarizing.value = true
        viewModelScope.launch {
            try {
                saveNoteToDb()
                val result = repository.summarizeNote(noteId)
                result.onSuccess { text ->
                    _summaries.value = _summaries.value + text
                    repository.updateNoteSummary(noteId, summariesToJson(_summaries.value))
                }.onFailure { error ->
                    _snackbarMessage.emit(error.message ?: "摘要生成失败")
                }
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun removeSummary(index: Int) {
        _summaries.value = _summaries.value.toMutableList().apply { removeAt(index) }
    }

    fun updateSummary(index: Int, newSummary: String) {
        _summaries.value = _summaries.value.toMutableList().apply { set(index, newSummary) }
    }

    fun polishNote() {
        if (_content.value.isBlank()) {
            viewModelScope.launch {
                _snackbarMessage.emit("笔记内容为空")
            }
            return
        }
        _isPolishing.value = true
        viewModelScope.launch {
            try {
                val result = repository.polishNote(_content.value)
                result.onSuccess { polished ->
                    _polishedText.value = polished
                }.onFailure { error ->
                    _snackbarMessage.emit(error.message ?: "润色失败")
                }
            } finally {
                _isPolishing.value = false
            }
        }
    }

    fun applyPolishedText() {
        val polished = _polishedText.value ?: return
        pushSnapshot()
        _content.value = polished
        _polishedText.value = null
    }

    fun dismissPolishedText() {
        _polishedText.value = null
    }

    fun togglePreview() {
        viewModelScope.launch {
            val newValue = !_isPreview.value
            _isPreview.value = newValue
            preferencesManager.saveMarkdownMode(noteId, newValue)
        }
    }

    private fun parseSummaries(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            listOf(json)
        }
    }

    private fun summariesToJson(summaries: List<String>): String? {
        if (summaries.isEmpty()) return null
        val arr = JSONArray()
        summaries.forEach { arr.put(it) }
        return arr.toString()
    }
}
