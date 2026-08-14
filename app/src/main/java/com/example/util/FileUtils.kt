package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    data class FileMeta(
        val fileName: String,
        val mimeType: String,
        val size: Long,
        val bytes: ByteArray
    )

    fun readUri(context: Context, uri: Uri): FileMeta? {
        return try {
            var fileName = "encrypted_file.bin"
            var size = 0L
            val contentResolver = context.contentResolver

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            FileMeta(
                fileName = fileName,
                mimeType = mimeType,
                size = if (size > 0) size else bytes.size.toLong(),
                bytes = bytes
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveBytesToInternalStorage(context: Context, fileName: String, data: ByteArray): File {
        val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
        val safeFileName = System.currentTimeMillis().toString() + "_" + fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val file = File(vaultDir, safeFileName)
        FileOutputStream(file).use { it.write(data) }
        return file
    }

    fun saveBytesToPublicDownloads(context: Context, fileName: String, data: ByteArray): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fileName)
            FileOutputStream(destFile).use { it.write(data) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.2f GB".format(gb)
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Decrypted File"))
        } catch (_: Exception) {}
    }

    fun shareText(context: Context, text: String, title: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Share Decrypted Content"))
        } catch (_: Exception) {}
    }
}
