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

    fun openFile(context: Context, record: com.example.data.TransferRecord) {
        if (record.localFilePath != null) {
            val file = File(record.localFilePath)
            if (file.exists()) {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, record.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                    return
                } catch (_: Exception) {}
            }
        }
        reshareTransfer(context, record)
    }

    fun formatTransferDetailsForClipboard(record: com.example.data.TransferRecord): String {
        val dateStr = DateFormatter.formatFullDateTime(record.timestamp)
        return buildString {
            appendLine("═══ CIPHER TRANSFER AUDIT RECORD ═══")
            appendLine("File Name: ${record.fileName}")
            appendLine("Transfer ID: ${record.transferId}")
            appendLine("Status: ${record.status.displayName}")
            appendLine("Direction: ${if (record.isReceived) "Received from ${record.teamMemberName}" else "Sent to ${record.teamName}"}")
            appendLine("Team: ${record.teamName}")
            appendLine("Transfer Mode: ${record.transferMode.name}")
            appendLine("Original Size: ${formatBytes(record.originalSize)}")
            appendLine("Encrypted Size: ${formatBytes(record.encryptedSize)}")
            appendLine("Timestamp: $dateStr")
            appendLine("SHA-256 Checksum: ${record.sha256Checksum}")
            if (record.safetyNumber.isNotEmpty()) {
                appendLine("Safety Number: ${record.safetyNumber}")
            }
            if (record.localFilePath != null) {
                appendLine("Local Vault Path: ${record.localFilePath}")
            }
            if (!record.decryptedTextPreview.isNullOrEmpty()) {
                appendLine("─── Decrypted Content Preview ───")
                appendLine(record.decryptedTextPreview)
            }
            appendLine("════════════════════════════════════")
        }.trimEnd()
    }

    fun reshareTransfer(context: Context, record: com.example.data.TransferRecord) {
        if (record.localFilePath != null) {
            val file = File(record.localFilePath)
            if (file.exists()) {
                shareFile(context, file, record.mimeType)
                return
            }
        }
        val textToShare = if (!record.decryptedTextPreview.isNullOrEmpty()) {
            record.decryptedTextPreview
        } else {
            formatTransferDetailsForClipboard(record)
        }
        shareText(context, textToShare, record.fileName)
    }

    /**
     * Exports transfer history records to a CSV file and launches system share intent.
     */
    fun exportHistoryToCsv(context: Context, records: List<com.example.data.TransferRecord>): Boolean {
        return try {
            val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
            val timestamp = System.currentTimeMillis()
            val file = File(exportDir, "cipherqr_history_$timestamp.csv")

            val csvContent = buildString {
                appendLine("TransferId,FileName,Direction,Status,TeamName,TeamMember,TransferMode,OriginalSizeBytes,EncryptedSizeBytes,Timestamp,FormattedDate,Sha256Checksum,SafetyNumber")
                for (r in records) {
                    val safeId = r.transferId.replace("\"", "\"\"")
                    val safeFileName = r.fileName.replace("\"", "\"\"")
                    val direction = if (r.isReceived) "RECEIVED" else "SENT"
                    val status = r.status.name
                    val safeTeam = r.teamName.replace("\"", "\"\"")
                    val safeMember = r.teamMemberName.replace("\"", "\"\"")
                    val mode = r.transferMode.name
                    val origSize = r.originalSize
                    val encSize = r.encryptedSize
                    val time = r.timestamp
                    val formattedDate = DateFormatter.formatFullDateTime(r.timestamp).replace("\"", "\"\"")
                    val sha = r.sha256Checksum.replace("\"", "\"\"")
                    val safety = r.safetyNumber.replace("\"", "\"\"")

                    appendLine("\"$safeId\",\"$safeFileName\",$direction,$status,\"$safeTeam\",\"$safeMember\",$mode,$origSize,$encSize,$time,\"$formattedDate\",\"$sha\",\"$safety\"")
                }
            }

            FileOutputStream(file).use { it.write(csvContent.toByteArray(Charsets.UTF_8)) }
            shareFile(context, file, "text/csv")
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Exports transfer history records to a JSON file and launches system share intent.
     */
    fun exportHistoryToJson(context: Context, records: List<com.example.data.TransferRecord>): Boolean {
        return try {
            val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
            val timestamp = System.currentTimeMillis()
            val file = File(exportDir, "cipherqr_history_$timestamp.json")

            val jsonArray = org.json.JSONArray()
            for (r in records) {
                val obj = org.json.JSONObject().apply {
                    put("transferId", r.transferId)
                    put("fileName", r.fileName)
                    put("isReceived", r.isReceived)
                    put("status", r.status.name)
                    put("teamName", r.teamName)
                    put("teamMemberName", r.teamMemberName)
                    put("transferMode", r.transferMode.name)
                    put("originalSizeBytes", r.originalSize)
                    put("encryptedSizeBytes", r.encryptedSize)
                    put("timestamp", r.timestamp)
                    put("formattedDate", DateFormatter.formatFullDateTime(r.timestamp))
                    put("sha256Checksum", r.sha256Checksum)
                    put("safetyNumber", r.safetyNumber)
                    if (r.decryptedTextPreview != null) {
                        put("decryptedTextPreview", r.decryptedTextPreview)
                    }
                }
                jsonArray.put(obj)
            }

            val root = org.json.JSONObject().apply {
                put("app", "Sender")
                put("exportTimestamp", timestamp)
                put("exportDate", DateFormatter.formatFullDateTime(timestamp))
                put("totalRecords", records.size)
                put("records", jsonArray)
            }

            FileOutputStream(file).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            shareFile(context, file, "application/json")
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts and shares the installed Sender APK package for direct offline sideloading.
     */
    fun shareAppApk(context: Context): Boolean {
        return try {
            val appInfo = context.applicationInfo
            val sourceApk = File(appInfo.sourceDir)
            if (!sourceApk.exists()) return false

            val exportDir = File(context.cacheDir, "apk_share").apply { if (!exists()) mkdirs() }
            val destApk = File(exportDir, "Sender.apk")

            sourceApk.inputStream().use { input ->
                FileOutputStream(destApk).use { output ->
                    input.copyTo(output)
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destApk)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Sender — Encrypted Air-Gapped Transfer Android App (APK)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🔒 Sender — Zero-Trust Optical Air-Gapped Encrypted File & Secret Transfer App (APK)\n\n" +
                    "Developer: Elvis Gatwara\nPortfolio: https://elvis-gatwara.vercel.app\nEmail: elvisgatwara@gmail.com"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Sender APK"))
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Calculates the total storage consumed by decrypted vault files, exports, and shared APK caches.
     */
    fun getVaultCacheSize(context: Context): Long {
        var size = 0L
        try {
            val vaultDir = File(context.filesDir, "vault")
            if (vaultDir.exists()) {
                vaultDir.listFiles()?.forEach { size += it.length() }
            }
            val exportDir = File(context.filesDir, "exports")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { size += it.length() }
            }
            val cacheApkDir = File(context.cacheDir, "apk_share")
            if (cacheApkDir.exists()) {
                cacheApkDir.listFiles()?.forEach { size += it.length() }
            }
        } catch (_: Exception) {}
        return size
    }

    /**
     * Clears temporary exported files and shared APK cache.
     */
    fun clearTemporaryCache(context: Context): Boolean {
        return try {
            val exportDir = File(context.filesDir, "exports")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { it.delete() }
            }
            val cacheApkDir = File(context.cacheDir, "apk_share")
            if (cacheApkDir.exists()) {
                cacheApkDir.listFiles()?.forEach { it.delete() }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
