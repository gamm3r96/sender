package com.example.p2p

import android.content.Context
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class LocalTransferServer {
    private var server: HttpServer? = null
    private var executor = Executors.newCachedThreadPool()

    private val _serverState = MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
    val serverState: StateFlow<ServerStatus> = _serverState.asStateFlow()

    private val _transferProgress = MutableStateFlow<Float>(0f)
    val transferProgress: StateFlow<Float> = _transferProgress.asStateFlow()

    private val _transferSpeedBytesPerSec = MutableStateFlow<Long>(0L)
    val transferSpeedBytesPerSec: StateFlow<Long> = _transferSpeedBytesPerSec.asStateFlow()

    private val _currentChunkIndex = MutableStateFlow<Int>(0)
    val currentChunkIndex: StateFlow<Int> = _currentChunkIndex.asStateFlow()

    private val _totalChunks = MutableStateFlow<Int>(1)
    val totalChunks: StateFlow<Int> = _totalChunks.asStateFlow()

    private val _bytesTransferred = MutableStateFlow<Long>(0L)
    val bytesTransferred: StateFlow<Long> = _bytesTransferred.asStateFlow()

    private val _totalBytesToTransfer = MutableStateFlow<Long>(0L)
    val totalBytesToTransfer: StateFlow<Long> = _totalBytesToTransfer.asStateFlow()

    sealed interface ServerStatus {
        data object Stopped : ServerStatus
        data class Running(
            val hostIp: String,
            val port: Int,
            val fileName: String,
            val fileSize: Long,
            val isReceiverMode: Boolean = false,
            val webPortalUrl: String = "http://$hostIp:$port/"
        ) : ServerStatus
        data class ClientConnected(
            val clientIp: String,
            val action: String = "Downloading"
        ) : ServerStatus
        data class Completed(
            val bytesTransferred: Long,
            val message: String = "Transfer completed successfully"
        ) : ServerStatus
        data class Error(val message: String) : ServerStatus
    }

    /**
     * Starts Sender Server (Streaming file/secret over Wi-Fi / Hotspot)
     */
    fun startServer(
        port: Int = 8989,
        fileName: String,
        mimeType: String,
        encryptedPayload: ByteArray,
        transferId: String,
        sha256: String = "",
        passphrasePrompt: String = "",
        onComplete: (() -> Unit)? = null
    ): Int {
        stopServer()

        return try {
            val address = InetSocketAddress(port)
            server = HttpServer.create(address, 0)
            server?.executor = executor

            // 1. Binary Download Endpoint
            server?.createContext("/download", object : HttpHandler {
                override fun handle(exchange: HttpExchange) {
                    if (!"GET".equals(exchange.requestMethod, ignoreCase = true)) {
                        exchange.sendResponseHeaders(405, -1)
                        exchange.close()
                        return
                    }

                    val clientIp = exchange.remoteAddress?.address?.hostAddress ?: "Unknown"
                    _serverState.value = ServerStatus.ClientConnected(clientIp, "Downloading $fileName")

                    exchange.responseHeaders.set("Content-Type", "application/octet-stream")
                    exchange.responseHeaders.set("Content-Disposition", "attachment; filename=\"$fileName.enc\"")
                    exchange.responseHeaders.set("X-Transfer-Id", transferId)
                    exchange.responseHeaders.set("X-File-Name", fileName)
                    exchange.responseHeaders.set("X-Mime-Type", mimeType)
                    exchange.responseHeaders.set("X-Sha256", sha256)
                    exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")

                    val totalBytes = encryptedPayload.size.toLong()
                    val bufferSize = 32 * 1024
                    val calculatedTotalChunks = ((encryptedPayload.size + bufferSize - 1) / bufferSize).coerceAtLeast(1)
                    _totalChunks.value = calculatedTotalChunks
                    _totalBytesToTransfer.value = totalBytes
                    _bytesTransferred.value = 0L
                    _currentChunkIndex.value = 0

                    exchange.sendResponseHeaders(200, totalBytes)

                    val outputStream: OutputStream = exchange.responseBody
                    var bytesSent = 0L
                    var offset = 0
                    var chunkIdx = 0
                    var lastSpeedCalcTime = System.currentTimeMillis()
                    var bytesSinceLastCalc = 0L

                    while (offset < encryptedPayload.size) {
                        val length = (encryptedPayload.size - offset).coerceAtMost(bufferSize)
                        _currentChunkIndex.value = chunkIdx
                        outputStream.write(encryptedPayload, offset, length)
                        offset += length
                        bytesSent += length
                        bytesSinceLastCalc += length
                        _bytesTransferred.value = bytesSent
                        chunkIdx++

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastSpeedCalcTime
                        if (elapsed >= 400) {
                            val speed = (bytesSinceLastCalc * 1000L) / elapsed.coerceAtLeast(1)
                            _transferSpeedBytesPerSec.value = speed
                            lastSpeedCalcTime = now
                            bytesSinceLastCalc = 0L
                        }

                        _transferProgress.value = bytesSent.toFloat() / totalBytes
                    }

                    _currentChunkIndex.value = calculatedTotalChunks - 1

                    outputStream.flush()
                    outputStream.close()
                    exchange.close()

                    _transferSpeedBytesPerSec.value = 0L
                    _serverState.value = ServerStatus.Completed(bytesSent, "Sent $fileName (${formatSize(bytesSent)})")
                    onComplete?.invoke()
                }
            })

            // 2. Metadata JSON Endpoint
            server?.createContext("/info", object : HttpHandler {
                override fun handle(exchange: HttpExchange) {
                    val json = """
                        {
                            "status": "ready",
                            "transferId": "$transferId",
                            "fileName": "$fileName",
                            "mimeType": "$mimeType",
                            "fileSize": ${encryptedPayload.size},
                            "sha256": "$sha256",
                            "isEncrypted": true
                        }
                    """.trimIndent()
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                    exchange.responseBody.close()
                    exchange.close()
                }
            })

            // 3. Sleek Web Browser Download Portal (for browsers on laptops/phones on the same Wi-Fi/Hotspot)
            val webPortalHtml = generateWebPortalHtml(
                fileName = fileName,
                fileSize = encryptedPayload.size.toLong(),
                mimeType = mimeType,
                sha256 = sha256,
                transferId = transferId
            )

            server?.createContext("/", object : HttpHandler {
                override fun handle(exchange: HttpExchange) {
                    val path = exchange.requestURI.path
                    if (path == "/" || path == "/share" || path == "/index.html") {
                        val bytes = webPortalHtml.toByteArray(Charsets.UTF_8)
                        exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.write(bytes)
                        exchange.responseBody.close()
                        exchange.close()
                    } else if (path == "/download") {
                        // handled by /download context
                    } else {
                        exchange.sendResponseHeaders(404, -1)
                        exchange.close()
                    }
                }
            })

            server?.start()
            val actualPort = server?.address?.port ?: port
            val ip = NetworkUtils.getLocalIpAddress()
            _serverState.value = ServerStatus.Running(
                hostIp = ip,
                port = actualPort,
                fileName = fileName,
                fileSize = encryptedPayload.size.toLong(),
                isReceiverMode = false
            )
            actualPort
        } catch (e: Exception) {
            _serverState.value = ServerStatus.Error(e.localizedMessage ?: "Failed to start Wi-Fi / Hotspot server")
            -1
        }
    }

    /**
     * Starts Receiver Server Mode (Acts as a Wi-Fi / Hotspot drop receiver on this device)
     */
    fun startReceiverServer(
        port: Int = 8990,
        onFileReceived: (fileName: String, mimeType: String, data: ByteArray, clientIp: String) -> Unit
    ): Int {
        stopServer()

        return try {
            val address = InetSocketAddress(port)
            server = HttpServer.create(address, 0)
            server?.executor = executor

            server?.createContext("/upload", object : HttpHandler {
                override fun handle(exchange: HttpExchange) {
                    if (!"POST".equals(exchange.requestMethod, ignoreCase = true)) {
                        exchange.sendResponseHeaders(405, -1)
                        exchange.close()
                        return
                    }

                    val clientIp = exchange.remoteAddress?.address?.hostAddress ?: "Unknown"
                    _serverState.value = ServerStatus.ClientConnected(clientIp, "Receiving file from $clientIp")

                    val fileName = exchange.requestHeaders.getFirst("X-File-Name") ?: "received_file.bin"
                    val mimeType = exchange.requestHeaders.getFirst("X-Mime-Type") ?: "application/octet-stream"

                    val inputStream = exchange.requestBody
                    val buffer = ByteArray(32 * 1024)
                    val outputStream = ByteArrayOutputStream()
                    var bytesReadTotal = 0L
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesReadTotal += read
                    }

                    val fileBytes = outputStream.toByteArray()
                    exchange.sendResponseHeaders(200, 2)
                    exchange.responseBody.write("OK".toByteArray())
                    exchange.responseBody.close()
                    exchange.close()

                    _serverState.value = ServerStatus.Completed(bytesReadTotal, "Received $fileName (${formatSize(bytesReadTotal)})")
                    onFileReceived(fileName, mimeType, fileBytes, clientIp)
                }
            })

            // Web Upload Portal (so any computer/phone on Wi-Fi can drop files to this device)
            val dropPortalHtml = generateWebDropPortalHtml(port)
            server?.createContext("/", object : HttpHandler {
                override fun handle(exchange: HttpExchange) {
                    val bytes = dropPortalHtml.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                    exchange.responseBody.close()
                    exchange.close()
                }
            })

            server?.start()
            val actualPort = server?.address?.port ?: port
            val ip = NetworkUtils.getLocalIpAddress()
            _serverState.value = ServerStatus.Running(
                hostIp = ip,
                port = actualPort,
                fileName = "Receiver File Drop Active",
                fileSize = 0L,
                isReceiverMode = true
            )
            actualPort
        } catch (e: Exception) {
            _serverState.value = ServerStatus.Error(e.localizedMessage ?: "Failed to start Wi-Fi receiver server")
            -1
        }
    }

    fun stopServer() {
        try {
            server?.stop(0)
            server = null
            _serverState.value = ServerStatus.Stopped
            _transferProgress.value = 0f
            _transferSpeedBytesPerSec.value = 0L
        } catch (_: Exception) {}
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun generateWebPortalHtml(
        fileName: String,
        fileSize: Long,
        mimeType: String,
        sha256: String,
        transferId: String
    ): String {
        val formattedSize = formatSize(fileSize)
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sender — Wi-Fi / Hotspot Direct Transfer</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            background: #0B0F19;
            color: #E2E8F0;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 20px;
        }
        .card {
            background: #131B2E;
            border: 1px solid #1E293B;
            border-radius: 16px;
            padding: 28px;
            max-width: 480px;
            width: 100%;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
            text-align: center;
        }
        .badge {
            display: inline-block;
            background: rgba(16, 185, 129, 0.15);
            color: #10B981;
            font-weight: 700;
            font-size: 11px;
            padding: 4px 10px;
            border-radius: 20px;
            letter-spacing: 0.5px;
            text-transform: uppercase;
            margin-bottom: 16px;
            border: 1px solid rgba(16, 185, 129, 0.3);
        }
        h1 { font-size: 22px; font-weight: 800; margin-bottom: 8px; color: #FFFFFF; }
        p.subtitle { color: #94A3B8; font-size: 13px; margin-bottom: 24px; }
        .file-box {
            background: #0F172A;
            border: 1px solid #334155;
            border-radius: 12px;
            padding: 16px;
            margin-bottom: 24px;
            text-align: left;
        }
        .file-name { font-weight: 700; font-size: 16px; color: #38BDF8; word-break: break-all; }
        .file-meta { font-size: 12px; color: #64748B; margin-top: 4px; }
        .sha-box { font-family: monospace; font-size: 10px; color: #10B981; word-break: break-all; margin-top: 8px; background: #090D16; padding: 6px; border-radius: 6px; }
        .btn {
            display: block;
            width: 100%;
            background: #10B981;
            color: #064E3B;
            font-weight: 800;
            font-size: 15px;
            padding: 14px;
            border-radius: 10px;
            text-decoration: none;
            border: none;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .btn:hover { background: #34D399; transform: translateY(-1px); }
        .footer { margin-top: 24px; font-size: 11px; color: #475569; }
    </style>
</head>
<body>
    <div class="card">
        <div class="badge">📡 Air-Gapped Wi-Fi / Hotspot Link</div>
        <h1>Sender Direct Transfer</h1>
        <p class="subtitle">Direct peer-to-peer download over local Wi-Fi or Hotspot.</p>
        <div class="file-box">
            <div class="file-name">📦 $fileName</div>
            <div class="file-meta">Size: $formattedSize • MIME: $mimeType</div>
            <div class="sha-box">SHA-256: ${sha256.take(32)}...</div>
        </div>
        <a href="/download" class="btn" download="$fileName">⬇️ Download Encrypted File ($formattedSize)</a>
        <div class="footer">
            Zero-Cloud Transfer • Authenticated AES-256-GCM • Developed by Elvis Gatwara (@gamm3r96)
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun generateWebDropPortalHtml(port: Int): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sender — Wi-Fi Drop Receiver</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #0B0F19;
            color: #E2E8F0;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 20px;
        }
        .card {
            background: #131B2E;
            border: 1px solid #1E293B;
            border-radius: 16px;
            padding: 28px;
            max-width: 480px;
            width: 100%;
            text-align: center;
        }
        .badge {
            display: inline-block;
            background: rgba(56, 189, 248, 0.15);
            color: #38BDF8;
            font-weight: 700;
            font-size: 11px;
            padding: 4px 10px;
            border-radius: 20px;
            text-transform: uppercase;
            margin-bottom: 16px;
        }
        h1 { font-size: 22px; font-weight: 800; margin-bottom: 8px; color: #FFFFFF; }
        p.subtitle { color: #94A3B8; font-size: 13px; margin-bottom: 24px; }
        .drop-zone {
            border: 2px dashed #38BDF8;
            background: #0F172A;
            border-radius: 12px;
            padding: 32px 16px;
            cursor: pointer;
            margin-bottom: 20px;
        }
        .btn {
            background: #38BDF8;
            color: #0C4A6E;
            font-weight: 800;
            font-size: 15px;
            padding: 12px 24px;
            border-radius: 8px;
            border: none;
            cursor: pointer;
            margin-top: 12px;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="badge">📥 Receiver Portal Active</div>
        <h1>Sender Wi-Fi File Drop</h1>
        <p class="subtitle">Upload files directly to the Android receiver over Wi-Fi / Hotspot.</p>
        <div class="drop-zone" onclick="document.getElementById('fileInput').click()">
            <p style="font-size: 28px; margin-bottom: 8px;">📂</p>
            <p style="font-weight: bold; color: #E2E8F0;">Select file to send to device</p>
            <input type="file" id="fileInput" style="display:none" onchange="uploadFile()">
        </div>
        <p id="statusText" style="font-size: 12px; color: #94A3B8;"></p>
    </div>
    <script>
        function uploadFile() {
            const input = document.getElementById('fileInput');
            if (!input.files || input.files.length === 0) return;
            const file = input.files[0];
            const status = document.getElementById('statusText');
            status.innerText = 'Uploading ' + file.name + '...';
            
            fetch('/upload', {
                method: 'POST',
                headers: {
                    'X-File-Name': file.name,
                    'X-Mime-Type': file.type || 'application/octet-stream'
                },
                body: file
            }).then(res => {
                if (res.ok) {
                    status.innerText = '✓ File transferred successfully to Sender device!';
                    status.style.color = '#10B981';
                } else {
                    status.innerText = 'Upload failed: ' + res.statusText;
                    status.style.color = '#EF4444';
                }
            }).catch(err => {
                status.innerText = 'Error: ' + err.message;
                status.style.color = '#EF4444';
            });
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
