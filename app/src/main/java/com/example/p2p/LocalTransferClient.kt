package com.example.p2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

data class DiscoveredPeer(
    val ip: String,
    val port: Int,
    val fileName: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = "",
    val transferId: String = "",
    val sha256: String = "",
    val isSender: Boolean = true,
    val encrypted: Boolean = true
) {
    val hostIp: String get() = ip
    val size: Long get() = fileSize
}

object LocalTransferClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val fastProbeClient = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Scans the local Wi-Fi / Hotspot subnet concurrently to discover active Sender hosts.
     */
    suspend fun discoverPeers(
        subnetPrefix: String,
        port: Int = 8989
    ): List<DiscoveredPeer> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<DiscoveredPeer>()

        coroutineScope {
            // Scan 1..254 concurrently in batches
            val deferreds = (1..254).map { hostNum ->
                async {
                    val ip = "$subnetPrefix$hostNum"
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(ip, port), 250)
                        socket.close()

                        // Socket is open, query /info
                        val infoReq = Request.Builder()
                            .url("http://$ip:$port/info")
                            .build()
                        val res = fastProbeClient.newCall(infoReq).execute()
                        if (res.isSuccessful) {
                            val bodyStr = res.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            DiscoveredPeer(
                                ip = ip,
                                port = port,
                                fileName = json.optString("fileName", "Shared File"),
                                fileSize = json.optLong("fileSize", 0L),
                                mimeType = json.optString("mimeType", "application/octet-stream"),
                                transferId = json.optString("transferId", ""),
                                sha256 = json.optString("sha256", ""),
                                isSender = true
                            )
                        } else {
                            DiscoveredPeer(ip = ip, port = port)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            deferreds.awaitAll().filterNotNullTo(discovered)
        }

        discovered
    }

    suspend fun fetchTransferMetadata(
        hostIp: String,
        port: Int = 8989
    ): Result<DiscoveredPeer> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://$hostIp:$port/info")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
            val jsonStr = response.body?.string() ?: "{}"
            val json = JSONObject(jsonStr)
            val peer = DiscoveredPeer(
                ip = hostIp,
                port = port,
                fileName = json.optString("fileName", "Shared File"),
                fileSize = json.optLong("fileSize", 0L),
                mimeType = json.optString("mimeType", "application/octet-stream"),
                transferId = json.optString("transferId", ""),
                sha256 = json.optString("sha256", "")
            )
            Result.success(peer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadEncryptedPayload(
        hostIp: String,
        port: Int,
        onProgress: (bytesRead: Long, totalBytes: Long, progressFraction: Float, speedBytesPerSec: Long, currentChunkIndex: Int, totalChunks: Int) -> Unit
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

            val bufferSize = 32 * 1024
            val buffer = ByteArray(bufferSize)
            val totalCalculatedChunks = if (contentLength > 0) {
                ((contentLength + bufferSize - 1) / bufferSize).toInt().coerceAtLeast(1)
            } else 1

            var bytesReadTotal = 0L
            var read: Int
            var chunkIndex = 0
            var lastSpeedTime = System.currentTimeMillis()
            var bytesSinceLastSpeed = 0L
            var currentSpeed = 0L

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesReadTotal += read
                bytesSinceLastSpeed += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastSpeedTime
                if (elapsed >= 400) {
                    currentSpeed = (bytesSinceLastSpeed * 1000L) / elapsed.coerceAtLeast(1)
                    lastSpeedTime = now
                    bytesSinceLastSpeed = 0L
                }

                val currentChunk = (bytesReadTotal / bufferSize).toInt().coerceIn(0, totalCalculatedChunks - 1)
                val fraction = if (contentLength > 0) bytesReadTotal.toFloat() / contentLength else 0f
                onProgress(bytesReadTotal, contentLength, fraction, currentSpeed, currentChunk, totalCalculatedChunks)
                chunkIndex++
            }

            outputStream.flush()
            val resultBytes = outputStream.toByteArray()
            Result.success(resultBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPayloadToReceiver(
        receiverIp: String,
        port: Int = 8990,
        fileName: String,
        mimeType: String,
        data: ByteArray
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$receiverIp:$port/upload"
            val body = data.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-File-Name", fileName)
                .addHeader("X-Mime-Type", mimeType)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Receiver rejected upload: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
