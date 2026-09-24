package com.haoze.keynote.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * 向厂商拉取 OpenAI 兼容的 `GET {base}/models`，得到该 Key 真实可用的模型 ID。
 * 厂商管理页的模型候选完全来自这里，代码内不预置型号，以免随厂商上下线而过期。
 * 失败时抛 [AiApiException]，由调用方提示用户。
 */
object AiModelFetcher {

    private const val CONNECT_TIMEOUT_MILLIS = 15_000
    private const val READ_TIMEOUT_MILLIS = 15_000
    private const val ERROR_BODY_LIMIT = 500

    suspend fun fetchModelIds(baseUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val url = try {
            normalizeBaseUrl(baseUrl) + "/models"
        } catch (e: IllegalArgumentException) {
            throw AiApiException(e.message ?: "厂商基础地址不合法")
        }
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            if (apiKey.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = CONNECT_TIMEOUT_MILLIS
            conn.readTimeout = READ_TIMEOUT_MILLIS

            val statusCode = conn.responseCode
            val body = (if (statusCode in 200..299) conn.inputStream else conn.errorStream)
                ?.use { stream ->
                    BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                }
                .orEmpty()

            if (statusCode !in 200..299) {
                throw AiApiException("HTTP $statusCode: ${body.take(ERROR_BODY_LIMIT).ifBlank { "服务端未返回错误详情" }}")
            }
            parseModelIds(body).ifEmpty { throw AiApiException("接口未返回任何模型，请手动填写模型名称") }
        } catch (e: AiApiException) {
            throw e
        } catch (e: Exception) {
            throw AiApiException("拉取模型列表失败：${e.message ?: e.javaClass.simpleName}")
        } finally {
            conn.disconnect()
        }
    }

    /** 兼容两种返回体：OpenAI 的 `data[].id`，以及部分厂商把列表直接放在根数组的情况。 */
    private fun parseModelIds(response: String): List<String> {
        val ids = mutableListOf<String>()
        val root = JSONObject(response)
        val array = root.optJSONArray("data") ?: root.optJSONArray("models") ?: root.optJSONArray("result")
        if (array != null) {
            for (i in 0 until array.length()) {
                val item = array.opt(i)
                val id = when {
                    item is JSONObject -> item.optString("id").ifBlank { item.optString("model_id") }
                    else -> item?.toString().orEmpty()
                }
                if (id.isNotBlank() && id !in ids) ids.add(id)
            }
        }
        return ids
    }
}
