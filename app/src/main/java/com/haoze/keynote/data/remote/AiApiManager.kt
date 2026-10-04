package com.haoze.keynote.data.remote

import com.haoze.keynote.util.KeyObfuscator
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * 一条已配置的 AI 服务厂商。[presetId] 非空表示它由 [AiProviderPresets] 里的预设创建，编辑时据此
 * 回填该预设的接口地址；手填地址的自定义厂商 presetId 为空。[modelName] 一律来自在线拉取或手填。
 */
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val modelName: String = "",
    val presetId: String = ""
)

/** 厂商列表与 SharedPreferences 里 providers_json 的唯一编解码入口。 */
fun decodeAiProviders(raw: String): List<AiProvider> = try {
    val arr = JSONArray(raw)
    (0 until arr.length()).mapNotNull { i ->
        val obj = arr.optJSONObject(i) ?: return@mapNotNull null
        AiProvider(
            id = obj.optString("id"),
            name = obj.optString("name"),
            baseUrl = obj.optString("baseUrl"),
            apiKey = obj.optString("apiKey"),
            modelName = obj.optString("modelName"),
            presetId = obj.optString("presetId")
        )
    }
} catch (_: Exception) {
    emptyList()
}

fun encodeAiProviders(providers: List<AiProvider>): String {
    val arr = JSONArray()
    providers.forEach { p ->
        arr.put(JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("baseUrl", p.baseUrl)
            put("apiKey", p.apiKey)
            put("modelName", p.modelName)
            put("presetId", p.presetId)
        })
    }
    return arr.toString()
}

class AiApiManager(private val preferencesManager: PreferencesManager) {

    suspend fun getActiveProvider(): AiProvider? {
        val activeId = preferencesManager.activeProviderId.first()
        if (activeId.isBlank()) return null
        val providers = getProviders()
        return providers.find { it.id == activeId } ?: providers.firstOrNull()
    }

    suspend fun getProviders(): List<AiProvider> =
        decodeAiProviders(preferencesManager.providersJson.first())

    suspend fun saveProviders(providers: List<AiProvider>) {
        preferencesManager.saveProvidersJson(encodeAiProviders(providers))
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
