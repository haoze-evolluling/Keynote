package com.haoze.keynote.data.remote

import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class DeepSeekRequest(
    val model: String = "deepseek-v4-flash",
    val messages: List<Message>,
    val temperature: Double = 0.3,
    val maxTokens: Int = 150
)

data class Message(
    val role: String,
    val content: String
)

data class DeepSeekResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

class AiApiException(message: String) : Exception(message)

private fun resolveChatCompletionsUrl(configuredUrl: String): String {
    val normalized = configuredUrl.trim().trimEnd('/')
    require(normalized.startsWith("https://") || normalized.startsWith("http://")) {
        "请求地址必须以 http:// 或 https:// 开头"
    }
    return if (normalized.endsWith("/chat/completions", ignoreCase = true)) {
        normalized
    } else {
        "$normalized/chat/completions"
    }
}

interface DeepSeekApi {
    suspend fun generateTags(auth: String, request: DeepSeekRequest): DeepSeekResponse

    companion object {
        fun create(baseUrl: String): DeepSeekApi = object : DeepSeekApi {
            override suspend fun generateTags(auth: String, request: DeepSeekRequest): DeepSeekResponse =
                withContext(Dispatchers.IO) {
                    val url = URL(resolveChatCompletionsUrl(baseUrl))
                    val conn = url.openConnection() as HttpURLConnection
                    try {
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Authorization", auth)
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        conn.setRequestProperty("Accept", "application/json")
                        conn.doOutput = true
                        conn.connectTimeout = 30_000
                        conn.readTimeout = 30_000

                        val body = JSONObject().apply {
                            put("model", request.model)
                            put("temperature", request.temperature)
                            put("max_tokens", request.maxTokens)
                            put("messages", JSONArray().apply {
                                request.messages.forEach { msg ->
                                    put(JSONObject().apply {
                                        put("role", msg.role)
                                        put("content", msg.content)
                                    })
                                }
                            })
                        }.toString()

                        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }

                        val statusCode = conn.responseCode
                        val response = (if (statusCode in 200..299) conn.inputStream else conn.errorStream)
                            ?.use { stream ->
                                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                            }
                            .orEmpty()

                        if (statusCode !in 200..299) {
                            val details = response.take(2_000).ifBlank { "服务端未返回错误详情" }
                            throw AiApiException("HTTP $statusCode: $details")
                        }

                        val json = JSONObject(response)
                        val choicesArray = json.getJSONArray("choices")
                        val choices = (0 until choicesArray.length()).map { i ->
                            val choiceObj = choicesArray.getJSONObject(i)
                            val messageObj = choiceObj.getJSONObject("message")
                            Choice(
                                message = Message(
                                    role = messageObj.getString("role"),
                                    content = messageObj.getString("content")
                                )
                            )
                        }

                        DeepSeekResponse(choices = choices)
                    } finally {
                        conn.disconnect()
                    }
                }
        }
    }
}
