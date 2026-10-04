package com.haoze.keynote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.remote.AiApiManager
import com.haoze.keynote.data.remote.AiModelFetcher
import com.haoze.keynote.data.remote.AiProvider
import com.haoze.keynote.data.remote.AiProviderPreset
import com.haoze.keynote.data.remote.AiProviderPresets
import com.haoze.keynote.data.remote.decodeAiProviders
import com.haoze.keynote.util.AppConstants
import com.haoze.keynote.util.KeyObfuscator
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.haoze.keynote.ui.theme.DarkModePreference
import com.haoze.keynote.ui.theme.toDarkModePreference
import com.haoze.keynote.ui.theme.toInt

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val apiManager = AiApiManager(preferencesManager)

    val activeProviderId: StateFlow<String> = preferencesManager.activeProviderId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), "")

    private val _providers = MutableStateFlow<List<AiProvider>>(emptyList())
    val providers: StateFlow<List<AiProvider>> = _providers.asStateFlow()

    val noteFontSize: StateFlow<Int> = preferencesManager.noteFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), AppConstants.DEFAULT_FONT_SIZE)

    val darkModePreference: StateFlow<DarkModePreference> = preferencesManager.darkModePreference
        .map { it.toDarkModePreference() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), DarkModePreference.SYSTEM)

    init {
        viewModelScope.launch {
            preferencesManager.providersJson
                .map(::decodeAiProviders)
                .collect {
                    _providers.value = it
                }
        }
    }

    /** 厂商目录，供「预设厂商」分区渲染；顺序即展示顺序。 */
    val providerPresets: List<AiProviderPreset> = AiProviderPresets.all

    fun getActiveProvider(): AiProvider? {
        val id = activeProviderId.value
        return providers.value.find { it.id == id }
    }

    fun selectProvider(id: String) {
        viewModelScope.launch { preferencesManager.saveActiveProviderId(id) }
    }

    fun updateProvider(
        id: String,
        name: String,
        baseUrl: String,
        modelName: String,
        presetId: String,
        plainKey: String
    ) {
        val list = _providers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = list[idx]
        list[idx] = current.copy(
            name = name,
            baseUrl = baseUrl,
            modelName = modelName,
            presetId = presetId,
            // 留空表示沿用原密钥
            apiKey = if (plainKey.isBlank()) current.apiKey else KeyObfuscator.seal(plainKey)
        )
        _providers.value = list
        viewModelScope.launch { apiManager.saveProviders(list) }
    }

    /** 新建一条厂商配置并立即设为当前服务；[presetId] 为空即手填的自定义厂商。 */
    fun addProvider(
        name: String,
        baseUrl: String,
        modelName: String,
        presetId: String,
        plainKey: String
    ) {
        viewModelScope.launch {
            val list = _providers.value.toMutableList()
            val id = "custom_${System.currentTimeMillis()}"
            list.add(
                AiProvider(
                    id = id,
                    name = name,
                    baseUrl = baseUrl,
                    apiKey = if (plainKey.isBlank()) "" else KeyObfuscator.seal(plainKey),
                    modelName = modelName,
                    presetId = presetId
                )
            )
            apiManager.saveProviders(list)
            _providers.value = list
            preferencesManager.saveActiveProviderId(id)
        }
    }

    fun deleteProvider(id: String) {
        val list = _providers.value.toMutableList()
        list.removeAll { it.id == id }
        _providers.value = list
        viewModelScope.launch {
            apiManager.saveProviders(list)
            if (activeProviderId.value == id) {
                preferencesManager.saveActiveProviderId("")
            }
        }
    }

    /** 编辑框回显已存密钥。 */
    fun revealApiKey(provider: AiProvider): String =
        if (provider.apiKey.isBlank()) "" else KeyObfuscator.open(provider.apiKey)

    /**
     * 用编辑框里当前的地址与明文 Key 请求 `GET {base}/models`，无需先保存配置即可刷新模型候选。
     */
    fun fetchModels(
        baseUrl: String,
        plainKey: String,
        onResult: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (plainKey.isBlank()) {
            onError("请先填写该厂商的 API Key")
            return
        }
        viewModelScope.launch {
            try {
                onResult(AiModelFetcher.fetchModelIds(baseUrl, plainKey.trim()))
            } catch (e: Exception) {
                onError(e.message ?: "拉取模型列表失败")
            }
        }
    }

    fun setNoteFontSize(sp: Int) {
        viewModelScope.launch { preferencesManager.saveNoteFontSize(sp) }
    }

    fun setDarkMode(preference: DarkModePreference) {
        viewModelScope.launch { preferencesManager.saveDarkModePreference(preference.toInt()) }
    }
}
