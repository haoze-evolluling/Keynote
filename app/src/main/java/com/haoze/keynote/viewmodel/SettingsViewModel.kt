package com.haoze.keynote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.remote.AiApiManager
import com.haoze.keynote.data.remote.AiProvider
import com.haoze.keynote.util.AppConstants
import com.haoze.keynote.util.KeyObfuscator
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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
                .map { raw ->
                    try {
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
                .collect {
                    _providers.value = it
                }
        }
    }

    fun getActiveProvider(): AiProvider? {
        val id = activeProviderId.value
        return providers.value.find { it.id == id }
    }

    fun selectProvider(id: String) {
        viewModelScope.launch { preferencesManager.saveActiveProviderId(id) }
    }

    fun updateProvider(updated: AiProvider) {
        val list = _providers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            list[idx] = updated
            _providers.value = list
            viewModelScope.launch {
                apiManager.saveProviders(list)
            }
        }
    }

    fun addCustomProvider(name: String, baseUrl: String, modelName: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val list = _providers.value.toMutableList()
                val id = "custom_${System.currentTimeMillis()}"
                val newProvider = AiProvider(id, name, baseUrl, apiKey = apiKey, modelName = modelName)
                list.add(newProvider)
                apiManager.saveProviders(list)
                _providers.value = list
                preferencesManager.saveActiveProviderId(id)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteCustomProvider(id: String) {
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

    fun setNoteFontSize(sp: Int) {
        viewModelScope.launch { preferencesManager.saveNoteFontSize(sp) }
    }

    fun setDarkMode(preference: DarkModePreference) {
        viewModelScope.launch { preferencesManager.saveDarkModePreference(preference.toInt()) }
    }

    fun sealZidaipass(plain: String): String = KeyObfuscator.seal(plain)
    fun openZidaipass(sealed: String): String = KeyObfuscator.open(sealed)
}
