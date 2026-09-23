package com.haoze.keynote.ui.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.data.remote.AiApiManager
import com.haoze.keynote.data.remote.DeepSeekRequest
import com.haoze.keynote.data.remote.Message
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.data.repository.ScheduleRepository
import com.haoze.keynote.util.AlarmScheduler
import com.haoze.keynote.util.AppConstants
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val noteRepository: NoteRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val apiManager = AiApiManager(preferencesManager)

    val schedules: StateFlow<List<ScheduleEntity>>
    val notes: StateFlow<List<NoteWithTags>>

    private val _aiGeneratedContent = MutableStateFlow<String?>(null)
    val aiGeneratedContent: StateFlow<String?> = _aiGeneratedContent.asStateFlow()

    private val _isGeneratingNote = MutableStateFlow(false)
    val isGeneratingNote: StateFlow<Boolean> = _isGeneratingNote.asStateFlow()

    init {
        schedules = scheduleRepository.getAllSchedules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())
        notes = noteRepository.getAllActiveNotesWithTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())
    }

    fun createSchedule(
        title: String,
        date: Long,
        endDate: Long? = null,
        location: String? = null,
        description: String? = null,
        noteId: Long? = null,
        reminderEnabled: Boolean = false,
        reminderMinutesBefore: Int = 15
    ) {
        viewModelScope.launch {
            val id = scheduleRepository.insertSchedule(
                title = title, date = date, endDate = endDate, location = location,
                description = description, noteId = noteId,
                reminderEnabled = reminderEnabled, reminderMinutesBefore = reminderMinutesBefore
            )
            if (reminderEnabled) {
                AlarmScheduler.scheduleAlarm(
                    context, id, date, reminderMinutesBefore
                )
            }
        }
    }

    fun updateSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(context, schedule.id)
            scheduleRepository.updateSchedule(schedule)
            if (schedule.reminderEnabled) {
                AlarmScheduler.scheduleAlarm(
                    context, schedule.id, schedule.date, schedule.reminderMinutesBefore
                )
            }
        }
    }

    fun deleteSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(context, schedule.id)
            scheduleRepository.softDelete(schedule)
        }
    }

    fun linkNote(scheduleId: Long, noteId: Long) {
        viewModelScope.launch {
            val schedule = scheduleRepository.getScheduleById(scheduleId) ?: return@launch
            scheduleRepository.updateSchedule(schedule.copy(noteId = noteId))
        }
    }

    fun unlinkNote(scheduleId: Long) {
        viewModelScope.launch {
            val schedule = scheduleRepository.getScheduleById(scheduleId) ?: return@launch
            scheduleRepository.updateSchedule(schedule.copy(noteId = null))
        }
    }

    fun aiGenerateNote(scheduleId: Long) {
        viewModelScope.launch {
            try {
                val schedule = scheduleRepository.getScheduleById(scheduleId) ?: return@launch
                val provider = apiManager.getActiveProvider()
                val apiKey = apiManager.resolveApiKey(provider)

                if (provider == null || apiKey.isBlank()) return@launch

                _isGeneratingNote.value = true
                val api = apiManager.createApi()
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                val startDateStr = dateFormat.format(java.util.Date(schedule.date))
                val endDateStr = schedule.endDate?.let { dateFormat.format(java.util.Date(it)) }
                val locationStr = schedule.location ?: "未指定"
                val descriptionStr = schedule.description ?: "无"

                val prompt = """
你是一个笔记助手。请根据以下日程信息生成一篇 markdown 格式的笔记。

日程信息：
- 标题：${schedule.title}
- 开始时间：$startDateStr
- 结束时间：${endDateStr ?: "未指定"}
- 地点：$locationStr
- 任务事项：$descriptionStr

要求：
1. 使用 markdown 语法
2. 包含以下章节：## 基本信息、## 任务事项、## 备注
3. 基本信息中列出时间、地点
4. 任务事项中根据描述列出具体待办
5. 备注部分留空供用户填写
6. 语言简洁明了
""".trimIndent()

                val request = DeepSeekRequest(
                    model = provider.modelName,
                    messages = listOf(
                        Message(role = "system", content = prompt)
                    ),
                    maxTokens = 500
                )
                val response = api.generateTags(auth = "Bearer $apiKey", request = request)
                val content = response.choices.firstOrNull()?.message?.content?.trim() ?: "日程「${schedule.title}」自动生成的笔记"
                _aiGeneratedContent.value = content
            } catch (_: Exception) {
                _aiGeneratedContent.value = null
            } finally {
                _isGeneratingNote.value = false
            }
        }
    }

    fun saveAiGeneratedNote(scheduleId: Long, content: String) {
        viewModelScope.launch {
            val schedule = scheduleRepository.getScheduleById(scheduleId) ?: return@launch
            val noteId = noteRepository.createNote(schedule.title, content)
            scheduleRepository.updateSchedule(schedule.copy(noteId = noteId))
            _aiGeneratedContent.value = null
        }
    }

    fun discardAiGeneratedNote() {
        _aiGeneratedContent.value = null
    }
}