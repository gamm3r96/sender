package com.example.p2p

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    sealed interface ServerStatus {
        data object Stopped : ServerStatus
        data class Running(val hostIp: String, val port: Int, val fileName: String, val fileSize: Long) : ServerStatus
        data class ClientConnected(val clientIp: String) : ServerStatus
        data class Completed(val bytesSent: Long) : ServerStatus
        data class Error(val message: String) : ServerStatus
    }

    fun startServer(
        port: Int = 8989,
        fileName: String,
        mimeType: String,
        encryptedPayload: ByteArray,
        transferId: String,
        onComplete: (() -> Unit)? = null
    ): Int {
        stopServer()

        return try {
            val address = InetSocketAddress(port)
            server = HttpServer.create(address, 0)
            server?.executor = executor

            server?.createContext("/download", object : HttpHandler {
                override fun handle(exchange: HttpExchange) {
                    if (!"GET".equals(exchange.requestMethod, ignoreCase = true)) {
                        exchange.sendResponseHeaders(405, -1)
                        exchange.close()
                        return
                    }

                    val clientIp = exchange.remoteAddress?.address?.hostAddress ?: "Unknown"
                    _serverState.value = ServerStatus.ClientConnected(clientIp)

                    exchange.responseHeaders.set("Content-Type", "application/octet-stream")
                    exchange.responseHeaders.set("Content-Disposition", "attachment; filename=\"$fileName.enc\"")
                    exchange.responseHeaders.set("X-Transfer-Id", transferId)
                    exchange.responseHeaders.set("X-File-Name", fileName)
                    exchange.responseHeaders.set("X-Mime-Type", mimeType)
                    exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")

                    val totalBytes = encryptedPayload.size.toLong()
                    exchange.sendResponseHeaders(200, totalBytes)

                    val outputStream: OutputStream = exchange.responseBody
                    val bufferSize = 16 * 1024
                    var bytesSent = 0L
                    var offset = 0

                    while (offset < encryptedPayload.size) {
                        val length = (encryptedPayload.size - offset).coerceAtMost(bufferSize)
                        outputStream.write(encryptedPayload, offset, length)
                        offset += length
                        bytesSent += length
                        _transferProgress.value = bytesSent.toFloat() / totalBytes
                    }

                    outputStream.flush()
                    outputStream.close()
                    exchange.close()

                    _serverState.value = ServerStatus.Completed(bytesSent)
                    onComplete?.invoke()
                }
            })

            server?.start()
            val actualPort = server?.address?.port ?: port
            val ip = NetworkUtils.getLocalIpAddress()
            _serverState.value = ServerStatus.Running(ip, actualPort, fileName, encryptedPayload.size.toLong())
            actualPort
        } catch (e: Exception) {
            _serverState.value = ServerStatus.Error(e.localizedMessage ?: "Failed to start local server")
            -1
        }
    }

    fun stopServer() {
        try {
            server?.stop(0)
            server = null
            _serverState.value = ServerStatus.Stopped
            _transferProgress.value = 0f
        } catch (_: Exception) {}
    }
}
