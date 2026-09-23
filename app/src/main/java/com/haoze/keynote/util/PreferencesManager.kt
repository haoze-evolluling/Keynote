package com.haoze.keynote.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

class PreferencesManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "keynote_settings"
        private const val KEY_ACTIVE_PROVIDER_ID = "active_provider_id"
        private const val KEY_PROVIDERS_JSON = "providers_json"
        private const val KEY_NOTE_FONT_SIZE = "note_font_size"
        private const val KEY_DARK_MODE_PREFERENCE = "dark_mode_preference"
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    val activeProviderId: kotlinx.coroutines.flow.Flow<String> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(prefs.getString(KEY_ACTIVE_PROVIDER_ID, "") ?: "")
                kotlinx.coroutines.delay(500)
            }
        }.distinctUntilChanged()

    val providersJson: kotlinx.coroutines.flow.Flow<String> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(prefs.getString(KEY_PROVIDERS_JSON, "[]") ?: "[]")
                kotlinx.coroutines.delay(500)
            }
        }.distinctUntilChanged()

    val noteFontSize: kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(prefs.getInt(KEY_NOTE_FONT_SIZE, 16))
                kotlinx.coroutines.delay(500)
            }
        }.distinctUntilChanged()

    val darkModePreference: kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(prefs.getInt(KEY_DARK_MODE_PREFERENCE, 0))
                kotlinx.coroutines.delay(500)
            }
        }.distinctUntilChanged()

    suspend fun saveActiveProviderId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_PROVIDER_ID, id).apply()
    }

    suspend fun saveProvidersJson(json: String) {
        prefs.edit().putString(KEY_PROVIDERS_JSON, json).apply()
    }

    suspend fun saveNoteFontSize(sp: Int) {
        prefs.edit().putInt(KEY_NOTE_FONT_SIZE, sp).apply()
    }

    suspend fun saveDarkModePreference(mode: Int) {
        prefs.edit().putInt(KEY_DARK_MODE_PREFERENCE, mode).apply()
    }

    fun markdownModeForNote(noteId: Long): kotlinx.coroutines.flow.Flow<Boolean> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(prefs.getBoolean("preview_mode_$noteId", false))
                kotlinx.coroutines.delay(500)
            }
        }.distinctUntilChanged()

    suspend fun saveMarkdownMode(noteId: Long, enabled: Boolean) {
        prefs.edit().putBoolean("preview_mode_$noteId", enabled).apply()
    }
}
