package com.haoze.keynote.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.AIChatConversationEntity
import com.haoze.keynote.data.db.entity.AIChatMessageEntity
import com.haoze.keynote.data.db.entity.CategoryEntity
import com.haoze.keynote.data.repository.AIChatRepository
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.data.repository.ScheduleRepository
import com.haoze.keynote.data.repository.TodoRepository
import com.haoze.keynote.data.remote.AiApiManager
import com.haoze.keynote.data.remote.DeepSeekRequest
import com.haoze.keynote.data.remote.Message
import com.haoze.keynote.util.AppConstants
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

data class ChatMessage(
    val id: Long,
    val role: String,
    val content: String,
    val isBillRelated: Boolean = false,
    val billJson: String? = null,
    val isScheduleRelated: Boolean = false,
    val scheduleJson: String? = null,
    val isTodoRelated: Boolean = false,
    val todoJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PendingBill(
    val item: String,
    val amount: Double,
    val date: Long,
    val suggestedCategory: String?,
    val billJson: String
)

class AIChatViewModel(
    private val preferencesManager: PreferencesManager,
    private val noteRepository: NoteRepository,
    private val billRepository: BillRepository,
    private val todoRepository: TodoRepository,
    private val scheduleRepository: ScheduleRepository,
    private val aiChatRepository: AIChatRepository
) : ViewModel() {

    private var messageCounter = 0L

    private val apiManager = AiApiManager(preferencesManager)

    private val messageFlows = mutableMapOf<AssistantType, MutableStateFlow<List<ChatMessage>>>(
        AssistantType.CHAT to MutableStateFlow(emptyList()),
        AssistantType.BILL to MutableStateFlow(emptyList()),
        AssistantType.PLANNER to MutableStateFlow(emptyList())
    )

    private var activeFlow: MutableStateFlow<List<ChatMessage>> = messageFlows[AssistantType.CHAT]!!

    private val activeConversationIds = mutableMapOf<AssistantType, Long?>()

    private val _messages = MutableStateFlow(emptyList<ChatMessage>())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = messageFlows[AssistantType.CHAT]!!.asStateFlow()
    val billMessages: StateFlow<List<ChatMessage>> = messageFlows[AssistantType.BILL]!!.asStateFlow()
    val plannerMessages: StateFlow<List<ChatMessage>> = messageFlows[AssistantType.PLANNER]!!.asStateFlow()

    private val _currentAssistant = MutableStateFlow(AssistantType.CHAT)
    val currentAssistant: StateFlow<AssistantType> = _currentAssistant.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCreatingBill = MutableStateFlow(false)
    val isCreatingBill: StateFlow<Boolean> = _isCreatingBill.asStateFlow()

    private val _createdNoteId = MutableSharedFlow<Long>()
    val createdNoteId: SharedFlow<Long> = _createdNoteId.asSharedFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _pendingBill = MutableStateFlow<PendingBill?>(null)
    val pendingBill: StateFlow<PendingBill?> = _pendingBill.asStateFlow()

    private val _historyConversations = MutableStateFlow<List<AIChatConversationEntity>>(emptyList())
    val historyConversations: StateFlow<List<AIChatConversationEntity>> = _historyConversations.asStateFlow()

    init {
        viewModelScope.launch {
            billRepository.getAllCategories().collect { _categories.value = it }
        }
        viewModelScope.launch {
            aiChatRepository.getActiveConversations().collect { _historyConversations.value = it }
        }
    }

    fun switchAssistant(type: AssistantType) {
        _currentAssistant.value = type
        activeFlow = messageFlows[type]!!
        _messages.value = activeFlow.value
    }

    fun loadConversation(conversation: AIChatConversationEntity) {
        viewModelScope.launch {
            val type = runCatching { AssistantType.valueOf(conversation.assistantType) }
                .getOrDefault(AssistantType.CHAT)
            val loadedMessages = aiChatRepository.getMessages(conversation.id).map { it.toChatMessage() }
            messageFlows[type]!!.value = loadedMessages
            activeConversationIds[type] = conversation.id
            messageCounter = maxOf(messageCounter, (loadedMessages.maxOfOrNull { it.id } ?: 0L) + 1L)
            switchAssistant(type)
        }
    }

    private fun syncMessagesIfActive(flow: MutableStateFlow<List<ChatMessage>>) {
        if (flow === activeFlow) {
            _messages.value = flow.value
        }
    }

    private fun getSystemPrompt(type: AssistantType): String {
        val currentTime = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm", java.util.Locale.getDefault()
        ).format(java.util.Date())
        val categoryNames = _categories.value.joinToString("、") { it.name }

        return buildAssistantSystemPrompt(
            type = type,
            context = AssistantPromptContext(
                currentTime = currentTime,
                billCategoryNames = categoryNames
            )
        )
    }

    fun sendMessage(content: String) {
        submitMessage(content, appendUserMessage = true)
    }

    fun regenerateLastResponse() {
        if (_isLoading.value) return
        val type = _currentAssistant.value
        val flow = activeFlow
        val lastUserIndex = flow.value.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) return

        val lastUserContent = flow.value[lastUserIndex].content
        flow.value = flow.value.take(lastUserIndex + 1)
        _messages.value = flow.value
        activeConversationIds[type]?.let { conversationId ->
            viewModelScope.launch {
                aiChatRepository.replaceMessages(
                    conversationId,
                    flow.value.map { it.toEntity(conversationId) }
                )
            }
        }
        submitMessage(lastUserContent, appendUserMessage = false)
    }

    private fun submitMessage(content: String, appendUserMessage: Boolean) {
        if (content.isBlank() || _isLoading.value) return
        val currentType = _currentAssistant.value
        val flow = activeFlow
        val startingConversationId = activeConversationIds[currentType]
        var userMessage: ChatMessage? = null

        if (appendUserMessage) {
            val newUserMessage = ChatMessage(role = "user", content = content, id = messageCounter++)
            userMessage = newUserMessage
            flow.value = flow.value + newUserMessage
            syncMessagesIfActive(flow)
        }
        val requestMessages = flow.value
        val firstUserContent = requestMessages.firstOrNull { it.role == "user" }?.content
        _isLoading.value = true

        viewModelScope.launch {
            var requestConversationId: Long? = null
            try {
                val conversationId = ensureConversation(
                    type = currentType,
                    firstUserContent = firstUserContent,
                    startingConversationId = startingConversationId
                )
                requestConversationId = conversationId
                userMessage?.let { aiChatRepository.addMessage(conversationId, it.toEntity(conversationId)) }
                val provider = apiManager.getActiveProvider()
                val apiKey = apiManager.resolveApiKey(provider)

                if (provider == null || apiKey.isBlank()) {
                    appendAndPersistMessage(
                        currentType,
                        flow,
                        conversationId,
                        ChatMessage(
                            role = "assistant",
                            content = "请先在设置中配置 AI 厂商和 API Key",
                            id = messageCounter++
                        )
                    )
                    return@launch
                }

                val api = apiManager.createApi()
                val historyMessages = requestMessages
                    .filter { !it.content.startsWith("✅") && !it.content.startsWith("❌") }
                    .map { msg -> Message(role = msg.role, content = msg.content) }
                val systemPrompt = getSystemPrompt(currentType)
                val request = DeepSeekRequest(
                    model = provider.modelName,
                    messages = listOf(
                        Message(role = "system", content = systemPrompt)
                    ) + historyMessages,
                    maxTokens = 800
                )
                val response = api.generateTags(auth = "Bearer $apiKey", request = request)
                val rawReply = response.choices.firstOrNull()?.message?.content ?: "无响应"
                val parsedReply = parseAiReply(rawReply)
                val reply = parsedReply.visibleContent
                    .ifBlank { defaultVisibleReply(parsedReply.action) }

                appendAndPersistMessage(
                    currentType,
                    flow,
                    conversationId,
                    ChatMessage(
                        role = "assistant", content = reply, id = messageCounter++,
                        isBillRelated = parsedReply.action == StructuredChatAction.BILL,
                        billJson = parsedReply.json.takeIf { parsedReply.action == StructuredChatAction.BILL },
                        isScheduleRelated = parsedReply.action == StructuredChatAction.SCHEDULE,
                        scheduleJson = parsedReply.json.takeIf { parsedReply.action == StructuredChatAction.SCHEDULE },
                        isTodoRelated = parsedReply.action == StructuredChatAction.TODO,
                        todoJson = parsedReply.json.takeIf { parsedReply.action == StructuredChatAction.TODO }
                    )
                )
            } catch (e: Exception) {
                val conversationId = requestConversationId ?: ensureConversation(
                    type = currentType,
                    firstUserContent = firstUserContent,
                    startingConversationId = startingConversationId
                )
                appendAndPersistMessage(
                    currentType,
                    flow,
                    conversationId,
                    ChatMessage(
                        role = "assistant",
                        content = "请求失败: ${e.message ?: "未知错误"}",
                        id = messageCounter++
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun appendAndPersistMessage(
        type: AssistantType,
        flow: MutableStateFlow<List<ChatMessage>>,
        conversationId: Long? = null,
        message: ChatMessage
    ) {
        if (conversationId != null) {
            aiChatRepository.addMessage(conversationId, message.toEntity(conversationId))
            if (activeConversationIds[type] == conversationId) {
                flow.value = flow.value + message
                syncMessagesIfActive(flow)
            }
        } else {
            flow.value = flow.value + message
            syncMessagesIfActive(flow)
            persistMessage(type, flow, message)
        }
    }

    private suspend fun persistMessage(
        type: AssistantType,
        flow: MutableStateFlow<List<ChatMessage>>,
        message: ChatMessage
    ) {
        val conversationId = ensureConversation(type, flow.value.firstOrNull { it.role == "user" }?.content)
        aiChatRepository.addMessage(conversationId, message.toEntity(conversationId))
    }

    private suspend fun ensureConversation(
        type: AssistantType,
        firstUserContent: String?,
        startingConversationId: Long? = null
    ): Long {
        startingConversationId?.let { return it }
        activeConversationIds[type]?.let { return it }
        val title = firstUserContent?.toConversationTitle() ?: "新对话"
        val conversationId = aiChatRepository.createConversation(type.name, title)
        if (startingConversationId == null || activeConversationIds[type] == startingConversationId) {
            activeConversationIds[type] = conversationId
        }
        return conversationId
    }

    private fun String.toConversationTitle(): String {
        val singleLine = lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return singleLine.take(AppConstants.CONVERSATION_TITLE_MAX_LENGTH).ifBlank { "新对话" }
    }

    private fun ChatMessage.toEntity(conversationId: Long): AIChatMessageEntity {
        return AIChatMessageEntity(
            conversationId = conversationId,
            role = role,
            content = content,
            createdAt = createdAt,
            isBillRelated = isBillRelated,
            billJson = billJson,
            isScheduleRelated = isScheduleRelated,
            scheduleJson = scheduleJson,
            isTodoRelated = isTodoRelated,
            todoJson = todoJson
        )
    }

    private fun AIChatMessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            role = role,
            content = content,
            isBillRelated = isBillRelated,
            billJson = billJson,
            isScheduleRelated = isScheduleRelated,
            scheduleJson = scheduleJson,
            isTodoRelated = isTodoRelated,
            todoJson = todoJson,
            createdAt = createdAt
        )
    }

    private fun defaultVisibleReply(action: StructuredChatAction?): String {
        return when (action) {
            StructuredChatAction.BILL -> "已识别到账单信息，可以帮你创建账单。"
            StructuredChatAction.SCHEDULE -> "已识别到日程信息，可以帮你创建日程。"
            StructuredChatAction.TODO -> "已识别到待办事项，可以帮你创建待办。"
            null -> "无响应"
        }
    }

    fun createNoteFromContent(content: String) {
        viewModelScope.launch {
            try {
                val noteId = noteRepository.createNote("", content)
                preferencesManager.saveMarkdownMode(noteId, true)
                _createdNoteId.emit(noteId)
            } catch (_: Exception) {
                _createdNoteId.emit(-1L)
            }
        }
    }

    fun createNoteFromMessages() {
        viewModelScope.launch {
            try {
                val markdown = buildString {
                    activeFlow.value.forEach { message ->
                        if (message.role == "system") return@forEach
                        if (message.content.isBlank()) return@forEach
                        val label = if (message.role == "user") "**用户：**" else "**AI：**"
                        val displayContent = if (
                            message.isBillRelated || message.isScheduleRelated || message.isTodoRelated
                        ) {
                            message.content.lines()
                                .filter { line ->
                                    !line.trimStart().startsWith("[BILL]") &&
                                    !line.trimStart().startsWith("[SCHEDULE]") &&
                                    !line.trimStart().startsWith("[TODO]")
                                }
                                .joinToString("\n")
                                .trim()
                        } else {
                            message.content
                        }
                        if (displayContent.isBlank()) return@forEach
                        if (isNotEmpty()) append("\n\n")
                        append(label).append(" ").append(displayContent)
                    }
                }
                val noteId = noteRepository.createNote("", markdown)
                preferencesManager.saveMarkdownMode(noteId, true)
                _createdNoteId.emit(noteId)
            } catch (_: Exception) {
                _createdNoteId.emit(-1L)
            }
        }
    }

    fun prepareBillFromAI(billJson: String?) {
        if (billJson.isNullOrBlank()) return
        try {
            val jsonStr = billJson
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = org.json.JSONObject(jsonStr)
            val item = json.getString("item")
            val amount = json.getDouble("amount")
            val dateStr = json.optString("date", "")
            val date = if (dateStr.isNotBlank()) {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        .parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) { System.currentTimeMillis() }
            } else {
                System.currentTimeMillis()
            }
            val category = json.optString("category", "").ifBlank { null }
            _pendingBill.value = PendingBill(item, amount, date, category, billJson)
        } catch (_: Exception) {
            viewModelScope.launch {
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant",
                    content = "❌ 无法解析账单信息，请重试",
                    id = messageCounter++
                ))
            }
        }
    }

    fun confirmCreateBill(categoryId: Long?) {
        val bill = _pendingBill.value ?: return
        _pendingBill.value = null
        _isCreatingBill.value = true
        viewModelScope.launch {
            try {
                val jsonStr = bill.billJson
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val json = JSONObject(jsonStr)
                val item = json.getString("item")
                val amount = json.getDouble("amount")
                val date = parseDate(json.optString("date", ""))
                val categoryName = json.optString("category", "").ifBlank { null }
                val categoryId = resolveCategoryId(categoryName ?: bill.suggestedCategory, _categories.value)
                val id = billRepository.insertBill(item, amount, date, categoryId)
                val catName = categoryId?.let { cid -> _categories.value.find { it.id == cid }?.name } ?: "未分类"
                val message = "✅ 账单已创建：$item - ¥${String.format("%.2f", amount)}（$catName）"
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant", content = message, id = messageCounter++
                ))
            } catch (e: Exception) {
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant",
                    content = "❌ 账单创建失败：${e.message ?: "未知错误"}",
                    id = messageCounter++
                ))
            } finally {
                _isCreatingBill.value = false
            }
        }
    }

    fun dismissPendingBill() {
        _pendingBill.value = null
    }

    fun createScheduleFromAI(scheduleJson: String?) {
        viewModelScope.launch {
            if (scheduleJson.isNullOrBlank()) {
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant",
                    content = "❌ 日程创建失败：未找到日程信息",
                    id = messageCounter++
                ))
                return@launch
            }

            try {
                val json = JSONObject(scheduleJson)
                val title = json.getString("title")
                val date = parseDate(json.optString("date", ""))
                val endDate = json.optString("endDate", "").takeIf { it.isNotBlank() }?.let { parseDate(it) }
                val location = json.optString("location", "").ifBlank { null }
                val description = json.optString("description", "").ifBlank { null }
                scheduleRepository.insertSchedule(
                    title = title, date = date, endDate = endDate,
                    location = location, description = description,
                    reminderEnabled = false, reminderMinutesBefore = 0
                )
                val message = "✅ 日程已创建：$title。未设置提醒，可编辑日程开启提醒。"
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant", content = message, id = messageCounter++
                ))
            } catch (e: Exception) {
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant",
                    content = "❌ 日程创建失败：${e.message ?: "未知错误"}",
                    id = messageCounter++
                ))
            }
        }
    }

    fun createTodoFromAI(todoJson: String?) {
        viewModelScope.launch {
            if (todoJson.isNullOrBlank()) {
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant",
                    content = "❌ 待办创建失败：未找到待办信息",
                    id = messageCounter++
                ))
                return@launch
            }

            try {
                val json = JSONObject(todoJson)
                val title = json.getString("title")
                val priority = json.optInt("priority", 1).coerceIn(0, 2)
                val dueDate = parseDateOrNull(json.optString("dueDate", ""))
                val hasTime = json.optBoolean("hasTime", json.optString("dueDate", "").contains(":"))
                val notes = json.optString("notes", "").ifBlank { null }
                todoRepository.insertTodo(
                    title = title, priority = priority,
                    dueDate = dueDate, hasTime = hasTime && dueDate != null, notes = notes
                )
                val message = "✅ 待办已创建：$title"
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant", content = message, id = messageCounter++
                ))
            } catch (e: Exception) {
                appendAndPersistMessage(_currentAssistant.value, activeFlow, message = ChatMessage(
                    role = "assistant",
                    content = "❌ 待办创建失败：${e.message ?: "未知错误"}",
                    id = messageCounter++
                ))
            }
        }
    }

    fun clearMessages() {
        activeConversationIds[_currentAssistant.value] = null
        activeFlow.value = emptyList()
        _messages.value = emptyList()
    }

    fun deleteCurrentConversation() {
        val type = _currentAssistant.value
        val conversationId = activeConversationIds[type]
        clearMessages()
        if (conversationId != null) {
            viewModelScope.launch { aiChatRepository.softDeleteConversation(conversationId) }
        }
    }

    fun deleteConversation(conversation: AIChatConversationEntity) {
        viewModelScope.launch {
            aiChatRepository.softDeleteConversation(conversation.id)
            val type = runCatching { AssistantType.valueOf(conversation.assistantType) }.getOrNull()
            if (type != null && activeConversationIds[type] == conversation.id) {
                messageFlows[type]!!.value = emptyList()
                activeConversationIds[type] = null
                if (_currentAssistant.value == type) _messages.value = emptyList()
            }
        }
    }

    private suspend fun resolveCategoryId(categoryName: String?, categories: List<CategoryEntity>): Long? {
        if (categoryName.isNullOrBlank()) return null
        val existing = categories.find { it.name == categoryName }
        if (existing != null) return existing.id
        return billRepository.insertCategory(categoryName)
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(dateStr)?.time
                ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseDateOrNull(value: String): Long? {
        if (value.isBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }
}

sealed class AiActionResult {
    data class Success(val message: String, val id: Long) : AiActionResult()
    data class Error(val message: String) : AiActionResult()
}
