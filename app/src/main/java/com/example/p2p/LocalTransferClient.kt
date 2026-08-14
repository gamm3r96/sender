package com.example.p2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object LocalTransferClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun downloadEncryptedPayload(
        hostIp: String,
        port: Int,
        onProgress: (bytesRead: Long, totalBytes: Long, progressFraction: Float) -> Unit
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$hostIp:$port/download"
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error: ${response.code} ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()
            val inputStream: InputStream = body.byteStream()
            val outputStream = ByteArrayOutputStream()

            val buffer = ByteArray(16 * 1024)
            var bytesReadTotal = 0L
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesReadTotal += read
                val fraction = if (contentLength > 0) bytesReadTotal.toFloat() / contentLength else 0f
                onProgress(bytesReadTotal, contentLength, fraction)
            }

            outputStream.flush()
            val resultBytes = outputStream.toByteArray()
            Result.success(resultBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
