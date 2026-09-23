package com.haoze.keynote.data.remote

import com.haoze.keynote.util.KeyObfuscator
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val modelName: String = "deepseek-v4-flash"
)

class AiApiManager(private val preferencesManager: PreferencesManager) {

    suspend fun getActiveProvider(): AiProvider? {
        val activeId = preferencesManager.activeProviderId.first()
        if (activeId.isBlank()) return null
        val providers = getProviders()
        return providers.find { it.id == activeId } ?: providers.firstOrNull()
    }

    suspend fun getProviders(): List<AiProvider> {
        val raw = preferencesManager.providersJson.first()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AiProvider>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AiProvider(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        baseUrl = obj.optString("baseUrl", ""),
                        apiKey = obj.optString("apiKey", ""),
                        modelName = obj.optString("modelName", "deepseek-v4-flash")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveProviders(providers: List<AiProvider>) {
        val arr = JSONArray()
        providers.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("baseUrl", p.baseUrl)
                put("apiKey", p.apiKey)
                put("modelName", p.modelName)
            })
        }
        preferencesManager.saveProvidersJson(arr.toString())
    }

    suspend fun resolveApiKey(provider: AiProvider?): String {
        if (provider == null) return ""
        val key = provider.apiKey
        return if (key.isBlank()) "" else KeyObfuscator.open(key)
    }

    suspend fun createApi(): DeepSeekApi {
        val provider = getActiveProvider()
            ?: throw IllegalStateException("请先在设置中配置 AI 厂商")
        val url = provider.baseUrl.trimEnd('/')
        if (url.isBlank()) throw IllegalStateException("厂商基础地址未配置")
        return DeepSeekApi.create("$url/")
    }
}
