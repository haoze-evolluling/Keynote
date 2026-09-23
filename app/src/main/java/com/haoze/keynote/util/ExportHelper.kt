package com.haoze.keynote.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    fun writeToDownloads(context: Context, fileName: String, mimeType: String, content: ByteArray, subFolder: String = "") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        } else {
            val targetDir = if (subFolder.isNotEmpty()) {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KeyNote/$subFolder")
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KeyNote")
            }
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, fileName)
            FileOutputStream(targetFile).use { it.write(content) }
        }
    }
}
