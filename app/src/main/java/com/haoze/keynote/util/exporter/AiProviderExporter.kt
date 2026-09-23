package com.haoze.keynote.util.exporter

import android.content.Context
import com.haoze.keynote.data.remote.AiProvider
import com.haoze.keynote.util.ExportHelper
import com.haoze.keynote.util.KeyObfuscator
import com.haoze.keynote.util.PreferencesManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AiProviderExporter {

    suspend fun exportProviders(
        context: Context,
        selectedIds: Set<String>
    ): Int {
        val preferencesManager = PreferencesManager(context)
        val rawJson = preferencesManager.providersJson.first()
        val type = object : TypeToken<List<AiProvider>>() {}.type
        val allProviders: List<AiProvider> = try {
            Gson().fromJson(rawJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val filtered = if (selectedIds.isEmpty()) {
            allProviders
        } else {
            allProviders.filter { it.id in selectedIds }
        }

        if (filtered.isEmpty()) throw Exception("没有符合条件的厂商配置")

        val exportList = filtered.map { provider ->
            val decryptedKey = if (provider.apiKey.isNotBlank()) {
                KeyObfuscator.open(provider.apiKey)
            } else {
                ""
            }
            mapOf(
                "id" to provider.id,
                "name" to provider.name,
                "baseUrl" to provider.baseUrl,
                "modelName" to provider.modelName,
                "apiKey" to decryptedKey
            )
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "providers_$timestamp.json"
        val jsonContent = Gson().toJson(exportList)

        ExportHelper.writeToDownloads(
            context, fileName, "application/json",
            jsonContent.toByteArray(Charsets.UTF_8), "AIProviders"
        )
        return filtered.size
    }
}
