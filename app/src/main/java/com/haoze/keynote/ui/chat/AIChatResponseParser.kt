package com.haoze.keynote.ui.chat

internal enum class StructuredChatAction { BILL, SCHEDULE, TODO }

internal data class ParsedAiReply(
    val visibleContent: String,
    val action: StructuredChatAction? = null,
    val json: String? = null
)

internal fun parseAiReply(rawReply: String): ParsedAiReply {
    val tagMatch = Regex("\\[(BILL|SCHEDULE|TODO)]").find(rawReply)
        ?: return ParsedAiReply(normalizeVisibleContent(rawReply))

    val action = when (tagMatch.groupValues[1]) {
        "BILL" -> StructuredChatAction.BILL
        "SCHEDULE" -> StructuredChatAction.SCHEDULE
        else -> StructuredChatAction.TODO
    }

    val jsonStart = rawReply.indexOf('{', startIndex = tagMatch.range.last + 1)
    if (jsonStart < 0) {
        val visible = rawReply.removeRange(tagMatch.range.first, tagMatch.range.last + 1)
        return ParsedAiReply(normalizeVisibleContent(visible))
    }

    val jsonEnd = findJsonObjectEnd(rawReply, jsonStart)
    if (jsonEnd < 0) {
        val visible = rawReply.removeRange(tagMatch.range.first, tagMatch.range.last + 1)
        return ParsedAiReply(normalizeVisibleContent(visible))
    }

    var removeEnd = jsonEnd
    val tail = rawReply.substring(removeEnd)
    val fenceMatch = Regex("^\\s*```").find(tail)
    if (fenceMatch != null) {
        removeEnd += fenceMatch.range.last + 1
    }

    val visible = rawReply.removeRange(tagMatch.range.first, removeEnd)
    return ParsedAiReply(
        visibleContent = normalizeVisibleContent(visible),
        action = action,
        json = rawReply.substring(jsonStart, jsonEnd).trim()
    )
}

private fun findJsonObjectEnd(text: String, start: Int): Int {
    var depth = 0
    var inString = false
    var escaped = false

    for (index in start until text.length) {
        val char = text[index]
        if (inString) {
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> inString = false
            }
        } else {
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return index + 1
                }
            }
        }
    }

    return -1
}

private fun normalizeVisibleContent(text: String): String {
    return text
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
