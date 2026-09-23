package com.haoze.keynote.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore

object ExportHelper {

    fun writeToDownloads(context: Context, fileName: String, mimeType: String, content: ByteArray, subFolder: String = "") {
        val relativePath = if (subFolder.isNotEmpty()) {
            "${Environment.DIRECTORY_DOWNLOADS}/KeyNote/$subFolder"
        } else {
            "${Environment.DIRECTORY_DOWNLOADS}/KeyNote"
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("无法创建文件: $fileName")
        context.contentResolver.openOutputStream(uri)?.use { it.write(content) }
            ?: throw Exception("无法写入文件: $fileName")
    }
}
