package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.ChunkEnvelope
import com.example.crypto.CryptoManager
import com.example.crypto.EncryptedPayload
import com.example.crypto.P2PTransferTicket
import com.example.crypto.QrChunkProgress
import com.example.data.AppDatabase
import com.example.data.TeamKey
import com.example.data.TeamKeyRepository
import com.example.data.TransferMode
import com.example.data.TransferRecord
import com.example.data.TransferRepository
import com.example.data.TransferStatus
import com.example.p2p.LocalTransferClient
import com.example.p2p.LocalTransferServer
import com.example.p2p.NetworkUtils
import com.example.qr.QrBitmapDecoder
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class SendPreparationState(
    val fileName: String,
    val mimeType: String,
    val originalSize: Long,
    val rawBytes: ByteArray,
    val mode: TransferMode = TransferMode.QR_STREAM,
    val teamKey: TeamKey? = null,
    val customPassphrase: String = "",
    val encryptedPayload: EncryptedPayload? = null,
    val qrChunks: List<String> = emptyList(),
    val isPreparing: Boolean = false,
    val p2pTicket: P2PTransferTicket? = null
)

class CipherViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val transferRepository = TransferRepository(database.transferDao())
    val teamKeyRepository = TeamKeyRepository(database.teamKeyDao())

    val transfers: StateFlow<List<TransferRecord>> = transferRepository.allTransfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTransfers: StateFlow<List<TransferRecord>> = transferRepository.favoriteTransfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamKeys: StateFlow<List<TeamKey>> = teamKeyRepository.allTeamKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTeamKey = MutableStateFlow<TeamKey?>(null)
    val activeTeamKey: StateFlow<TeamKey?> = _activeTeamKey.asStateFlow()

    // Send State
    private val _sendState = MutableStateFlow<SendPreparationState?>(null)
    val sendState: StateFlow<SendPreparationState?> = _sendState.asStateFlow()

    // Animated QR Stream player state
    private val _currentChunkIndex = MutableStateFlow(0)
    val currentChunkIndex: StateFlow<Int> = _currentChunkIndex.asStateFlow()

    private val _isStreamPlaying = MutableStateFlow(true)
    val isStreamPlaying: StateFlow<Boolean> = _isStreamPlaying.asStateFlow()

    private val _streamFps = MutableStateFlow(3) // 1 to 10 FPS
    val streamFps: StateFlow<Int> = _streamFps.asStateFlow()

    private var streamLoopJob: Job? = null

    // P2P Local Server
    val p2pServer = LocalTransferServer()
    val p2pServerStatus = p2pServer.serverState
    val p2pServerProgress = p2pServer.transferProgress

    // Receive Scanner State
    private val _scanProgress = MutableStateFlow<QrChunkProgress?>(null)
    val scanProgress: StateFlow<QrChunkProgress?> = _scanProgress.asStateFlow()

    private val _scannedP2PTicket = MutableStateFlow<P2PTransferTicket?>(null)
    val scannedP2PTicket: StateFlow<P2PTransferTicket?> = _scannedP2PTicket.asStateFlow()

    private val _p2pDownloadProgress = MutableStateFlow<Float>(0f)
    val p2pDownloadProgress: StateFlow<Float> = _p2pDownloadProgress.asStateFlow()

    private val _isDownloadingP2P = MutableStateFlow(false)
    val isDownloadingP2P: StateFlow<Boolean> = _isDownloadingP2P.asStateFlow()

    // Active record for inspection / viewer
    private val _inspectedRecord = MutableStateFlow<TransferRecord?>(null)
    val inspectedRecord: StateFlow<TransferRecord?> = _inspectedRecord.asStateFlow()

    // Notification toast events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            teamKeyRepository.allTeamKeys.collect { keys ->
                if (_activeTeamKey.value == null && keys.isNotEmpty()) {
                    _activeTeamKey.value = keys.firstOrNull { it.isDefault } ?: keys.first()
                }
            }
        }
        startStreamLoop()
    }

    fun setActiveTeamKey(teamKey: TeamKey) {
        _activeTeamKey.value = teamKey
    }

    fun setStreamFps(fps: Int) {
        _streamFps.value = fps.coerceIn(1, 10)
        startStreamLoop()
    }

    fun toggleStreamPlaying() {
        _isStreamPlaying.value = !_isStreamPlaying.value
        if (_isStreamPlaying.value) {
            startStreamLoop()
        } else {
            streamLoopJob?.cancel()
        }
    }

    fun selectChunkIndex(index: Int) {
        _currentChunkIndex.value = index
    }

    private fun startStreamLoop() {
        streamLoopJob?.cancel()
        streamLoopJob = viewModelScope.launch {
            while (isActive) {
                val chunks = _sendState.value?.qrChunks
                if (chunks != null && chunks.isNotEmpty() && _isStreamPlaying.value) {
                    val delayMs = (1000L / _streamFps.value).coerceAtLeast(100L)
                    delay(delayMs)
                    _currentChunkIndex.value = (_currentChunkIndex.value + 1) % chunks.size
                } else {
                    delay(300)
                }
            }
        }
    }

    fun prepareFileForSending(
        context: Context,
        uri: Uri,
        mode: TransferMode,
        customPassphrase: String = ""
    ) {
        viewModelScope.launch {
            _sendState.value = SendPreparationState(
                fileName = "Reading file...",
                mimeType = "",
                originalSize = 0,
                rawBytes = ByteArray(0),
                mode = mode,
                isPreparing = true
            )

            val meta = withContext(Dispatchers.IO) {
                FileUtils.readUri(context, uri)
            }

            if (meta == null) {
                _sendState.value = null
                _toastEvent.emit("Failed to read file from storage")
                return@launch
            }

            processPayloadForSending(meta.fileName, meta.mimeType, meta.bytes, mode, customPassphrase)
        }
    }

    fun prepareSecretTextForSending(
        title: String,
        secretText: String,
        mode: TransferMode,
        customPassphrase: String = ""
    ) {
        val fileName = if (title.isBlank()) "secret_note.txt" else "${title.trim()}.txt"
        val bytes = secretText.toByteArray(Charsets.UTF_8)
        processPayloadForSending(fileName, "text/plain", bytes, mode, customPassphrase)
    }

    private fun processPayloadForSending(
        fileName: String,
        mimeType: String,
        rawBytes: ByteArray,
        mode: TransferMode,
        customPassphrase: String
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val key = if (customPassphrase.isNotBlank()) {
                customPassphrase
            } else {
                _activeTeamKey.value?.passphraseOrKey ?: CryptoManager.generateEphemeralKey()
            }

            val encrypted = CryptoManager.encryptData(rawBytes, key)
            val transferId = UUID.randomUUID().toString().substring(0, 8)
            val teamName = _activeTeamKey.value?.teamName ?: "Secure E2E"

            val targetChunkSize = if (rawBytes.size > 20000) 580 else 420
            val chunks = CryptoManager.createQrChunks(
                encryptedBytes = encrypted.envelopeBytes,
                fileName = fileName,
                mimeType = mimeType,
                originalSize = rawBytes.size.toLong(),
                originalSha256 = encrypted.sha256Original,
                transferId = transferId,
                targetChunkSizeBytes = targetChunkSize
            )

            val ip = NetworkUtils.getLocalIpAddress()
            val p2pTicket = P2PTransferTicket(
                transferId = transferId,
                hostIp = ip,
                port = 8989,
                fileName = fileName,
                mimeType = mimeType,
                originalSize = rawBytes.size.toLong(),
                encryptedSize = encrypted.envelopeBytes.size.toLong(),
                sha256 = encrypted.sha256Original,
                encryptionKeyBase64 = key,
                teamName = teamName
            )

            val safetyNum = CryptoManager.generateSafetyNumber(encrypted.sha256Original, key)

            val state = SendPreparationState(
                fileName = fileName,
                mimeType = mimeType,
                originalSize = rawBytes.size.toLong(),
                rawBytes = rawBytes,
                mode = mode,
                teamKey = _activeTeamKey.value,
                customPassphrase = customPassphrase,
                encryptedPayload = encrypted,
                qrChunks = chunks,
                isPreparing = false,
                p2pTicket = p2pTicket
            )

            _sendState.value = state
            _currentChunkIndex.value = 0

            // If P2P mode, automatically start local server
            if (mode == TransferMode.P2P_DIRECT) {
                p2pServer.startServer(
                    port = 8989,
                    fileName = fileName,
                    mimeType = mimeType,
                    encryptedPayload = encrypted.envelopeBytes,
                    transferId = transferId
                )
            }

            // Save sent transfer to Room
            val record = TransferRecord(
                transferId = transferId,
                fileName = fileName,
                mimeType = mimeType,
                originalSize = rawBytes.size.toLong(),
                encryptedSize = encrypted.envelopeBytes.size.toLong(),
                isReceived = false,
                transferMode = mode,
                teamMemberName = "Me (Sender)",
                teamName = teamName,
                sha256Checksum = encrypted.sha256Original,
                safetyNumber = safetyNum,
                decryptedTextPreview = if (mimeType.startsWith("text/")) String(rawBytes, Charsets.UTF_8).take(200) else null
            )
            transferRepository.insert(record)
        }
    }

    fun switchSendMode(newMode: TransferMode) {
        val current = _sendState.value ?: return
        if (current.mode == newMode) return

        if (newMode == TransferMode.P2P_DIRECT && current.encryptedPayload != null && current.p2pTicket != null) {
            p2pServer.startServer(
                port = 8989,
                fileName = current.fileName,
                mimeType = current.mimeType,
                encryptedPayload = current.encryptedPayload.envelopeBytes,
                transferId = current.p2pTicket.transferId
            )
        } else if (current.mode == TransferMode.P2P_DIRECT) {
            p2pServer.stopServer()
        }

        _sendState.value = current.copy(mode = newMode)
    }

    fun clearSendState() {
        p2pServer.stopServer()
        _sendState.value = null
    }

    /**
     * Handles incoming QR text from live camera or gallery image
     */
    fun handleScannedQr(rawText: String, context: Context) {
        viewModelScope.launch {
            // Case 1: P2P Ticket QR
            val p2pTicket = CryptoManager.parseP2PTicketQr(rawText)
            if (p2pTicket != null) {
                _scannedP2PTicket.value = p2pTicket
                _toastEvent.emit("P2P File Transfer Detected: ${p2pTicket.fileName}")
                return@launch
            }

            // Case 2: QR Stream Chunk
            val chunk = CryptoManager.parseQrChunk(rawText)
            if (chunk != null) {
                processScannedChunk(chunk, context)
                return@launch
            }

            // Case 3: Raw team key or secret payload
            if (rawText.startsWith("CIPHER_KEY:")) {
                importTeamKeyFromQr(rawText)
                return@launch
            }

            _toastEvent.emit("Unrecognized QR Code format")
        }
    }

    private suspend fun processScannedChunk(chunk: ChunkEnvelope, context: Context) {
        var currentProg = _scanProgress.value
        if (currentProg == null || currentProg.transferId != chunk.transferId) {
            currentProg = QrChunkProgress(
                transferId = chunk.transferId,
                fileName = chunk.fileName,
                mimeType = chunk.mimeType,
                originalSize = chunk.originalSize,
                originalSha256 = chunk.originalSha256,
                totalChunks = chunk.total
            )
        }

        val decodedChunkBytes = android.util.Base64.decode(chunk.payloadBase64, android.util.Base64.DEFAULT)
        val computedChunkSha = CryptoManager.computeSha256(decodedChunkBytes)
        if (computedChunkSha != chunk.chunkSha256) {
            _toastEvent.emit("Corrupted QR chunk received (Checksum mismatch)")
            return
        }

        currentProg.receivedChunks[chunk.index] = decodedChunkBytes
        _scanProgress.value = currentProg.copy(receivedChunks = HashMap(currentProg.receivedChunks))

        if (currentProg.isComplete) {
            val assembledEnvelope = CryptoManager.assembleChunks(currentProg.receivedChunks, currentProg.totalChunks)
            if (assembledEnvelope != null) {
                completeQrStreamTransfer(currentProg, assembledEnvelope, context)
            }
        }
    }

    private suspend fun completeQrStreamTransfer(
        progress: QrChunkProgress,
        assembledEnvelope: ByteArray,
        context: Context
    ) {
        withContext(Dispatchers.IO) {
            // Try decrypting with active team key or other team keys
            val keyList = mutableListOf<String>()
            _activeTeamKey.value?.let { keyList.add(it.passphraseOrKey) }
            val allKeys = teamKeyRepository.getDefaultTeamKey()
            allKeys?.let { if (!keyList.contains(it.passphraseOrKey)) keyList.add(it.passphraseOrKey) }

            var decryptedBytes: ByteArray? = null
            var matchedKey: String? = null

            for (k in keyList) {
                try {
                    decryptedBytes = CryptoManager.decryptData(assembledEnvelope, k)
                    matchedKey = k
                    break
                } catch (_: Exception) {}
            }

            if (decryptedBytes != null) {
                val computedOriginalSha = CryptoManager.computeSha256(decryptedBytes)
                if (computedOriginalSha != progress.originalSha256) {
                    _toastEvent.emit("Warning: Decrypted data hash mismatch!")
                }

                val savedFile = FileUtils.saveBytesToInternalStorage(context, progress.fileName, decryptedBytes)
                val safetyNum = CryptoManager.generateSafetyNumber(progress.originalSha256, matchedKey ?: "")
                val textPreview = if (progress.mimeType.startsWith("text/")) String(decryptedBytes, Charsets.UTF_8).take(200) else null

                val record = TransferRecord(
                    transferId = progress.transferId,
                    fileName = progress.fileName,
                    mimeType = progress.mimeType,
                    originalSize = progress.originalSize,
                    encryptedSize = assembledEnvelope.size.toLong(),
                    isReceived = true,
                    transferMode = TransferMode.QR_STREAM,
                    teamMemberName = "Team Peer",
                    teamName = _activeTeamKey.value?.teamName ?: "Team Vault",
                    status = TransferStatus.COMPLETED,
                    sha256Checksum = progress.originalSha256,
                    safetyNumber = safetyNum,
                    localFilePath = savedFile.absolutePath,
                    decryptedTextPreview = textPreview
                )

                val id = transferRepository.insert(record)
                val savedRecord = record.copy(id = id)
                _inspectedRecord.value = savedRecord
                _scanProgress.value = null
                _toastEvent.emit("Successfully decrypted & saved ${progress.fileName}!")
            } else {
                _toastEvent.emit("Chunks received! Please verify Team Passphrase to decrypt.")
            }
        }
    }

    fun downloadAndDecryptP2PTicket(ticket: P2PTransferTicket, context: Context) {
        viewModelScope.launch {
            _isDownloadingP2P.value = true
            _p2pDownloadProgress.value = 0f

            val result = LocalTransferClient.downloadEncryptedPayload(ticket.hostIp, ticket.port) { bytesRead, total, frac ->
                _p2pDownloadProgress.value = frac
            }

            _isDownloadingP2P.value = false

            result.onSuccess { encryptedEnvelope ->
                withContext(Dispatchers.IO) {
                    try {
                        val decrypted = CryptoManager.decryptData(encryptedEnvelope, ticket.encryptionKeyBase64)
                        val computedSha = CryptoManager.computeSha256(decrypted)
                        if (computedSha != ticket.sha256) {
                            _toastEvent.emit("Integrity warning: SHA-256 checksum mismatch!")
                        }

                        val savedFile = FileUtils.saveBytesToInternalStorage(context, ticket.fileName, decrypted)
                        val safetyNum = CryptoManager.generateSafetyNumber(ticket.sha256, ticket.encryptionKeyBase64)
                        val textPreview = if (ticket.mimeType.startsWith("text/")) String(decrypted, Charsets.UTF_8).take(200) else null

                        val record = TransferRecord(
                            transferId = ticket.transferId,
                            fileName = ticket.fileName,
                            mimeType = ticket.mimeType,
                            originalSize = ticket.originalSize,
                            encryptedSize = ticket.encryptedSize,
                            isReceived = true,
                            transferMode = TransferMode.P2P_DIRECT,
                            teamMemberName = "Team Peer (LAN)",
                            teamName = ticket.teamName ?: "Direct P2P",
                            status = TransferStatus.COMPLETED,
                            sha256Checksum = ticket.sha256,
                            safetyNumber = safetyNum,
                            localFilePath = savedFile.absolutePath,
                            decryptedTextPreview = textPreview
                        )

                        val id = transferRepository.insert(record)
                        val savedRecord = record.copy(id = id)
                        _scannedP2PTicket.value = null
                        _inspectedRecord.value = savedRecord
                        _toastEvent.emit("P2P File received & verified: ${ticket.fileName}")
                    } catch (e: Exception) {
                        _toastEvent.emit("Failed to decrypt P2P payload: ${e.localizedMessage}")
                    }
                }
            }.onFailure { err ->
                _toastEvent.emit("Failed to download from ${ticket.hostIp}: ${err.localizedMessage}")
            }
        }
    }

    fun dismissP2PTicket() {
        _scannedP2PTicket.value = null
    }

    fun resetScanProgress() {
        _scanProgress.value = null
    }

    fun scanFromGalleryBitmap(bitmap: Bitmap, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            val decoded = QrBitmapDecoder.decodeFromBitmap(bitmap)
            if (decoded != null) {
                withContext(Dispatchers.Main) {
                    handleScannedQr(decoded, context)
                }
            } else {
                _toastEvent.emit("No QR code found in selected image")
            }
        }
    }

    fun inspectRecord(record: TransferRecord?) {
        _inspectedRecord.value = record
    }

    fun saveToDownloads(record: TransferRecord, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val filePath = record.localFilePath
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    val bytes = file.readBytes()
                    val success = FileUtils.saveBytesToPublicDownloads(context, record.fileName, bytes)
                    if (success) {
                        _toastEvent.emit("Saved ${record.fileName} to Downloads folder")
                    } else {
                        _toastEvent.emit("Failed to save to Downloads")
                    }
                    return@launch
                }
            }

            if (record.decryptedTextPreview != null) {
                val success = FileUtils.saveBytesToPublicDownloads(
                    context,
                    record.fileName,
                    record.decryptedTextPreview.toByteArray(Charsets.UTF_8)
                )
                if (success) {
                    _toastEvent.emit("Saved ${record.fileName} to Downloads folder")
                } else {
                    _toastEvent.emit("Failed to save to Downloads")
                }
            }
        }
    }

    fun shareRecord(record: TransferRecord, context: Context) {
        val path = record.localFilePath
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                FileUtils.shareFile(context, file, record.mimeType)
                return
            }
        }

        if (record.decryptedTextPreview != null) {
            FileUtils.shareText(context, record.decryptedTextPreview, record.fileName)
        }
    }

    fun toggleFavorite(record: TransferRecord) {
        viewModelScope.launch {
            transferRepository.toggleFavorite(record.id, record.isFavorite)
        }
    }

    fun deleteRecord(record: TransferRecord) {
        viewModelScope.launch {
            record.localFilePath?.let { File(it).delete() }
            transferRepository.delete(record)
            if (_inspectedRecord.value?.id == record.id) {
                _inspectedRecord.value = null
            }
            _toastEvent.emit("Deleted ${record.fileName}")
        }
    }

    fun addTeamKey(teamName: String, passphraseOrKey: String, colorHex: Long = 0xFF10B981) {
        viewModelScope.launch {
            val finalKey = if (passphraseOrKey.isBlank()) CryptoManager.generateEphemeralKey() else passphraseOrKey.trim()
            val safetyNum = CryptoManager.generateSafetyNumber(teamName, finalKey)
            val teamKey = TeamKey(
                teamName = teamName.ifBlank { "Team Vault" },
                passphraseOrKey = finalKey,
                safetyNumber = safetyNum,
                colorHex = colorHex,
                memberCount = 1,
                isDefault = false
            )
            teamKeyRepository.insert(teamKey)
            _toastEvent.emit("Team key created: ${teamKey.teamName}")
        }
    }

    fun deleteTeamKey(teamKey: TeamKey) {
        viewModelScope.launch {
            teamKeyRepository.delete(teamKey)
            if (_activeTeamKey.value?.id == teamKey.id) {
                _activeTeamKey.value = teamKeys.value.firstOrNull { it.id != teamKey.id }
            }
            _toastEvent.emit("Removed team key")
        }
    }

    fun setDefaultTeamKey(id: Long) {
        viewModelScope.launch {
            teamKeyRepository.setDefault(id)
            _activeTeamKey.value = teamKeys.value.firstOrNull { it.id == id }
            _toastEvent.emit("Default team key updated")
        }
    }

    private fun importTeamKeyFromQr(rawText: String) {
        viewModelScope.launch {
            try {
                val keyPayload = rawText.removePrefix("CIPHER_KEY:")
                val parts = keyPayload.split("||")
                val teamName = parts.getOrNull(0) ?: "Imported Team"
                val secretKey = parts.getOrNull(1) ?: ""
                if (secretKey.isNotBlank()) {
                    addTeamKey(teamName, secretKey, 0xFF06B6D4)
                    _toastEvent.emit("Imported team key: $teamName")
                }
            } catch (_: Exception) {
                _toastEvent.emit("Failed to parse team key QR")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        p2pServer.stopServer()
        streamLoopJob?.cancel()
    }
}
