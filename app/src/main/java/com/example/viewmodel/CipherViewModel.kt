package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.BiometricAuthManager
import com.example.auth.BiometricStatus
import com.example.crypto.ChunkEnvelope
import com.example.crypto.CryptoManager
import com.example.crypto.EncryptedPayload
import com.example.crypto.P2PTransferTicket
import com.example.crypto.QrChunkProgress
import com.example.data.AppDatabase
import com.example.data.QrColorScheme
import com.example.data.QrDensityPreset
import com.example.data.QrErrorCorrectionLevel
import com.example.data.QrModuleShape
import com.example.data.ScannerContrastBoostMode
import com.example.data.TeamKey
import com.example.data.TeamKeyRepository
import com.example.data.TransferMode
import com.example.data.TransferRecord
import com.example.data.TransferRepository
import com.example.data.TransferStatus
import com.example.p2p.DiscoveredPeer
import com.example.p2p.LocalTransferClient
import com.example.p2p.LocalTransferServer
import com.example.p2p.NetworkInfoState
import com.example.p2p.NetworkUtils
import com.example.qr.QrBitmapDecoder
import com.example.qr.QrCodeGenerator
import com.example.ui.theme.ThemeMode
import com.example.util.FileUtils
import com.example.util.HapticFeedbackHelper
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

data class PendingDecryptionState(
    val progress: QrChunkProgress,
    val assembledEnvelope: ByteArray
)

data class StreamTimeoutNotice(
    val fileName: String,
    val receivedCount: Int,
    val totalChunks: Int,
    val timeoutSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

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
    val p2pTicket: P2PTransferTicket? = null,
    val densityPreset: QrDensityPreset = QrDensityPreset.STANDARD,
    val sourceFilePath: String? = null,
    val rawTextPreview: String? = null
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

    private val _streamFps = MutableStateFlow(4) // 1 to 15 FPS
    val streamFps: StateFlow<Int> = _streamFps.asStateFlow()

    private val _densityPreset = MutableStateFlow(QrDensityPreset.STANDARD)
    val densityPreset: StateFlow<QrDensityPreset> = _densityPreset.asStateFlow()

    private val _loopCount = MutableStateFlow(1)
    val loopCount: StateFlow<Int> = _loopCount.asStateFlow()

    private var streamLoopJob: Job? = null

    // P2P / Wi-Fi / Hotspot Local Server
    val p2pServer = LocalTransferServer()
    val p2pServerStatus = p2pServer.serverState
    val p2pServerProgress = p2pServer.transferProgress
    val p2pServerSpeed = p2pServer.transferSpeedBytesPerSec

    // Network & Wi-Fi / Hotspot State
    private val _networkInfo = MutableStateFlow(NetworkUtils.getNetworkInfo(application))
    val networkInfo: StateFlow<NetworkInfoState> = _networkInfo.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _isScanningPeers = MutableStateFlow(false)
    val isScanningPeers: StateFlow<Boolean> = _isScanningPeers.asStateFlow()

    // Standalone Receiver File Drop Server
    val receiverServer = LocalTransferServer()
    val receiverServerStatus = receiverServer.serverState
    val receiverServerProgress = receiverServer.transferProgress

    // Receive Scanner State
    private val _scanProgress = MutableStateFlow<QrChunkProgress?>(null)
    val scanProgress: StateFlow<QrChunkProgress?> = _scanProgress.asStateFlow()

    private val _scannedP2PTicket = MutableStateFlow<P2PTransferTicket?>(null)
    val scannedP2PTicket: StateFlow<P2PTransferTicket?> = _scannedP2PTicket.asStateFlow()

    private val _p2pDownloadProgress = MutableStateFlow<Float>(0f)
    val p2pDownloadProgress: StateFlow<Float> = _p2pDownloadProgress.asStateFlow()

    private val _p2pDownloadSpeed = MutableStateFlow<Long>(0L)
    val p2pDownloadSpeed: StateFlow<Long> = _p2pDownloadSpeed.asStateFlow()

    private val _isDownloadingP2P = MutableStateFlow(false)
    val isDownloadingP2P: StateFlow<Boolean> = _isDownloadingP2P.asStateFlow()

    // Pending manual passphrase decryption modal
    private val _pendingDecryption = MutableStateFlow<PendingDecryptionState?>(null)
    val pendingDecryption: StateFlow<PendingDecryptionState?> = _pendingDecryption.asStateFlow()

    // Real-time frame capture pulse event (chunk index)
    private val _frameCaptureEvent = MutableSharedFlow<Int>()
    val frameCaptureEvent: SharedFlow<Int> = _frameCaptureEvent.asSharedFlow()

    // Active record for inspection / viewer
    private val _inspectedRecord = MutableStateFlow<TransferRecord?>(null)
    val inspectedRecord: StateFlow<TransferRecord?> = _inspectedRecord.asStateFlow()

    // Notification toast events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // QR Stream Inactivity Timeout Mechanism & Event State
    private val _streamTimeoutEvent = MutableSharedFlow<StreamTimeoutNotice>()
    val streamTimeoutEvent: SharedFlow<StreamTimeoutNotice> = _streamTimeoutEvent.asSharedFlow()

    private val _lastTimeoutNotice = MutableStateFlow<StreamTimeoutNotice?>(null)
    val lastTimeoutNotice: StateFlow<StreamTimeoutNotice?> = _lastTimeoutNotice.asStateFlow()

    private val _streamTimeoutSeconds = MutableStateFlow(
        application.getSharedPreferences("cipher_theme_prefs", Context.MODE_PRIVATE)
            .getInt("stream_timeout_sec", 15)
    )
    val streamTimeoutSeconds: StateFlow<Int> = _streamTimeoutSeconds.asStateFlow()

    private val _streamRemainingSeconds = MutableStateFlow<Int?>(null)
    val streamRemainingSeconds: StateFlow<Int?> = _streamRemainingSeconds.asStateFlow()

    private var streamTimeoutJob: Job? = null

    // Application Theme Mode Preference
    private val sharedPreferences = application.getSharedPreferences("cipher_theme_prefs", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(
                sharedPreferences.getString("app_theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            )
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // QR Code Optical Customization Preferences
    private val _qrColorScheme = MutableStateFlow(
        try {
            val savedName = sharedPreferences.getString("qr_color_scheme", QrColorScheme.HIGH_CONTRAST_MONO.name)
            QrColorScheme.valueOf(savedName ?: QrColorScheme.HIGH_CONTRAST_MONO.name)
        } catch (_: Exception) {
            QrColorScheme.HIGH_CONTRAST_MONO
        }
    )
    val qrColorScheme: StateFlow<QrColorScheme> = _qrColorScheme.asStateFlow()

    private val _qrErrorCorrectionLevel = MutableStateFlow(
        try {
            val savedName = sharedPreferences.getString("qr_ecc_level", QrErrorCorrectionLevel.LEVEL_M.name)
            QrErrorCorrectionLevel.valueOf(savedName ?: QrErrorCorrectionLevel.LEVEL_M.name)
        } catch (_: Exception) {
            QrErrorCorrectionLevel.LEVEL_M
        }
    )
    val qrErrorCorrectionLevel: StateFlow<QrErrorCorrectionLevel> = _qrErrorCorrectionLevel.asStateFlow()

    private val _qrModuleShape = MutableStateFlow(
        try {
            val savedName = sharedPreferences.getString("qr_module_shape", QrModuleShape.SQUARE.name)
            QrModuleShape.valueOf(savedName ?: QrModuleShape.SQUARE.name)
        } catch (_: Exception) {
            QrModuleShape.SQUARE
        }
    )
    val qrModuleShape: StateFlow<QrModuleShape> = _qrModuleShape.asStateFlow()

    private val _isQrInverted = MutableStateFlow(
        sharedPreferences.getBoolean("qr_inverted", false)
    )
    val isQrInverted: StateFlow<Boolean> = _isQrInverted.asStateFlow()

    private val _scannerContrastMode = MutableStateFlow(
        try {
            val savedName = sharedPreferences.getString("scanner_contrast_mode", ScannerContrastBoostMode.STANDARD.name)
            ScannerContrastBoostMode.valueOf(savedName ?: ScannerContrastBoostMode.STANDARD.name)
        } catch (_: Exception) {
            ScannerContrastBoostMode.STANDARD
        }
    )
    val scannerContrastMode: StateFlow<ScannerContrastBoostMode> = _scannerContrastMode.asStateFlow()

    private val _isScreenBrightnessBoostEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("qr_brightness_boost", true)
    )
    val isScreenBrightnessBoostEnabled: StateFlow<Boolean> = _isScreenBrightnessBoostEnabled.asStateFlow()

    fun setQrColorScheme(scheme: QrColorScheme) {
        _qrColorScheme.value = scheme
        sharedPreferences.edit().putString("qr_color_scheme", scheme.name).apply()
    }

    fun setQrErrorCorrectionLevel(level: QrErrorCorrectionLevel) {
        _qrErrorCorrectionLevel.value = level
        sharedPreferences.edit().putString("qr_ecc_level", level.name).apply()

        val current = _sendState.value ?: return
        val encrypted = current.encryptedPayload ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val transferId = current.p2pTicket?.transferId ?: UUID.randomUUID().toString().substring(0, 8)
            val newChunks = CryptoManager.createQrChunks(
                encryptedBytes = encrypted.envelopeBytes,
                fileName = current.fileName,
                mimeType = current.mimeType,
                originalSize = current.originalSize,
                originalSha256 = encrypted.sha256Original,
                transferId = transferId,
                targetChunkSizeBytes = current.densityPreset.chunkSizeBytes
            )
            QrCodeGenerator.preloadChunks(newChunks, level.zxingLevel)
            _sendState.value = current.copy(qrChunks = newChunks)
            _currentChunkIndex.value = 0
            _loopCount.value = 1
        }
    }

    fun setQrModuleShape(shape: QrModuleShape) {
        _qrModuleShape.value = shape
        sharedPreferences.edit().putString("qr_module_shape", shape.name).apply()
    }

    fun toggleQrInverted() {
        setQrInverted(!_isQrInverted.value)
    }

    fun setQrInverted(inverted: Boolean) {
        _isQrInverted.value = inverted
        sharedPreferences.edit().putBoolean("qr_inverted", inverted).apply()
    }

    fun setScannerContrastMode(mode: ScannerContrastBoostMode) {
        _scannerContrastMode.value = mode
        sharedPreferences.edit().putString("scanner_contrast_mode", mode.name).apply()
    }

    fun setScreenBrightnessBoostEnabled(enabled: Boolean) {
        _isScreenBrightnessBoostEnabled.value = enabled
        sharedPreferences.edit().putBoolean("qr_brightness_boost", enabled).apply()
    }

    // Tactile Haptic Feedback Preference
    private val _isHapticEnabled = MutableStateFlow(HapticFeedbackHelper.isHapticEnabled(application))
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    fun setHapticEnabled(enabled: Boolean) {
        _isHapticEnabled.value = enabled
        HapticFeedbackHelper.setHapticEnabled(getApplication(), enabled)
        viewModelScope.launch {
            if (enabled) {
                HapticFeedbackHelper.vibrateStreamCompleted(getApplication())
                _toastEvent.emit("Tactile haptic feedback enabled")
            } else {
                _toastEvent.emit("Tactile haptic feedback disabled")
            }
        }
    }

    fun toggleHapticEnabled() {
        setHapticEnabled(!_isHapticEnabled.value)
    }

    fun testHapticPattern(patternIndex: Int, context: Context) {
        when (patternIndex) {
            0 -> HapticFeedbackHelper.vibrateFrameDetected(context)
            1 -> HapticFeedbackHelper.vibrateStreamCompleted(context)
            2 -> HapticFeedbackHelper.vibrateDecryptionSuccess(context)
            3 -> HapticFeedbackHelper.vibratePassphraseError(context)
            4 -> HapticFeedbackHelper.vibrateTimeoutAlert(context)
            else -> HapticFeedbackHelper.vibrateBiometricSuccess(context)
        }
    }

    // Biometric Security & App Lock Preference
    private val securityPrefs = application.getSharedPreferences("cipher_security_prefs", Context.MODE_PRIVATE)
    private val _isBiometricEnabled = MutableStateFlow(securityPrefs.getBoolean("biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(securityPrefs.getBoolean("biometric_enabled", false))
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _hasCustomPasscode = MutableStateFlow(!securityPrefs.getString("vault_pin", "").isNullOrBlank())
    val hasCustomPasscode: StateFlow<Boolean> = _hasCustomPasscode.asStateFlow()

    private val _biometricStatus = MutableStateFlow<BiometricStatus>(BiometricAuthManager.checkBiometricStatus(application))
    val biometricStatus: StateFlow<BiometricStatus> = _biometricStatus.asStateFlow()

    fun refreshBiometricStatus(context: Context) {
        _biometricStatus.value = BiometricAuthManager.checkBiometricStatus(context)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        securityPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
        viewModelScope.launch {
            if (enabled) {
                _toastEvent.emit("Biometric protection enabled")
            } else {
                _toastEvent.emit("Biometric protection disabled")
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockVault() {
        _isAppLocked.value = true
        viewModelScope.launch {
            _toastEvent.emit("CipherQR Vault locked")
        }
    }

    fun setAppPasscode(pin: String) {
        val trimmed = pin.trim()
        securityPrefs.edit().putString("vault_pin", trimmed).apply()
        _hasCustomPasscode.value = trimmed.isNotBlank()
        viewModelScope.launch {
            if (trimmed.isNotBlank()) {
                _toastEvent.emit("Security PIN set successfully")
            } else {
                _toastEvent.emit("Security PIN cleared")
            }
        }
    }

    fun verifyPasscode(pin: String): Boolean {
        val stored = securityPrefs.getString("vault_pin", "") ?: ""
        // Fallback default PIN if none set is 1234
        val expected = if (stored.isNotBlank()) stored else "1234"
        val matched = pin.trim() == expected
        if (matched) {
            unlockApp()
        }
        return matched
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sharedPreferences.edit().putString("app_theme_mode", mode.name).apply()
    }

    fun cycleThemeMode() {
        setThemeMode(_themeMode.value.next())
    }

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
        _streamFps.value = fps.coerceIn(1, 15)
        startStreamLoop()
    }

    fun setDensityPreset(preset: QrDensityPreset) {
        _densityPreset.value = preset
        val current = _sendState.value ?: return
        val encrypted = current.encryptedPayload ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val transferId = current.p2pTicket?.transferId ?: UUID.randomUUID().toString().substring(0, 8)
            val newChunks = CryptoManager.createQrChunks(
                encryptedBytes = encrypted.envelopeBytes,
                fileName = current.fileName,
                mimeType = current.mimeType,
                originalSize = current.originalSize,
                originalSha256 = encrypted.sha256Original,
                transferId = transferId,
                targetChunkSizeBytes = preset.chunkSizeBytes
            )

            // Warm up QR generator cache in background
            QrCodeGenerator.preloadChunks(newChunks)

            _sendState.value = current.copy(
                qrChunks = newChunks,
                densityPreset = preset
            )
            _currentChunkIndex.value = 0
            _loopCount.value = 1
        }
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
        val total = _sendState.value?.qrChunks?.size ?: 1
        _currentChunkIndex.value = index.coerceIn(0, (total - 1).coerceAtLeast(0))
    }

    fun stepNextChunk() {
        val chunks = _sendState.value?.qrChunks ?: return
        if (chunks.isEmpty()) return
        val current = _currentChunkIndex.value
        val next = (current + 1) % chunks.size
        if (next == 0 && chunks.size > 1) {
            _loopCount.value = _loopCount.value + 1
        }
        _currentChunkIndex.value = next
    }

    fun stepPrevChunk() {
        val chunks = _sendState.value?.qrChunks ?: return
        if (chunks.isEmpty()) return
        val current = _currentChunkIndex.value
        val prev = if (current - 1 < 0) chunks.size - 1 else current - 1
        _currentChunkIndex.value = prev
    }

    fun jumpToFirstChunk() {
        _currentChunkIndex.value = 0
    }

    fun jumpToLastChunk() {
        val chunks = _sendState.value?.qrChunks ?: return
        if (chunks.isNotEmpty()) {
            _currentChunkIndex.value = chunks.size - 1
        }
    }

    private fun startStreamLoop() {
        streamLoopJob?.cancel()
        streamLoopJob = viewModelScope.launch {
            while (isActive) {
                val chunks = _sendState.value?.qrChunks
                if (chunks != null && chunks.isNotEmpty() && _isStreamPlaying.value) {
                    val delayMs = (1000L / _streamFps.value).coerceAtLeast(60L)
                    delay(delayMs)
                    val current = _currentChunkIndex.value
                    val next = (current + 1) % chunks.size
                    if (next == 0 && chunks.size > 1) {
                        _loopCount.value = _loopCount.value + 1
                    }
                    _currentChunkIndex.value = next
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

            val currentPreset = _densityPreset.value
            val targetChunkSize = currentPreset.chunkSizeBytes
            val chunks = CryptoManager.createQrChunks(
                encryptedBytes = encrypted.envelopeBytes,
                fileName = fileName,
                mimeType = mimeType,
                originalSize = rawBytes.size.toLong(),
                originalSha256 = encrypted.sha256Original,
                transferId = transferId,
                targetChunkSizeBytes = targetChunkSize
            )

            // Warm up QR generator cache in background
            QrCodeGenerator.preloadChunks(chunks, _qrErrorCorrectionLevel.value.zxingLevel)

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
            val textPreview = if (mimeType.startsWith("text/") || fileName.endsWith(".txt") || fileName.endsWith(".json") || fileName.endsWith(".csv") || fileName.endsWith(".kt") || fileName.endsWith(".md") || fileName.endsWith(".py")) {
                try {
                    String(rawBytes, Charsets.UTF_8).take(2000)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

            val cachedFile = try {
                FileUtils.saveBytesToInternalStorage(getApplication(), "preview_$fileName", rawBytes)
            } catch (_: Exception) {
                null
            }

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
                p2pTicket = p2pTicket,
                densityPreset = currentPreset,
                sourceFilePath = cachedFile?.absolutePath,
                rawTextPreview = textPreview
            )

            _sendState.value = state
            _currentChunkIndex.value = 0
            _loopCount.value = 1

            // If P2P mode, automatically start local server
            if (mode == TransferMode.P2P_DIRECT) {
                p2pServer.startServer(
                    port = 8989,
                    fileName = fileName,
                    mimeType = mimeType,
                    encryptedPayload = encrypted.envelopeBytes,
                    transferId = transferId,
                    sha256 = encrypted.sha256Original
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
                sourceInfo = "This Device (Sender)",
                destinationInfo = when (mode) {
                    TransferMode.P2P_DIRECT -> "P2P Receiver (${p2pTicket.hostIp})"
                    TransferMode.QR_STREAM -> "QR Optical Receiver ($teamName)"
                    TransferMode.QR_SECRET -> "Secret Receiver ($teamName)"
                },
                teamMemberName = "Me (Sender)",
                teamName = teamName,
                timestamp = System.currentTimeMillis(),
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
                transferId = current.p2pTicket.transferId,
                sha256 = current.encryptedPayload.sha256Original
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
                HapticFeedbackHelper.vibrateFrameDetected(context)
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
                HapticFeedbackHelper.vibrateStreamCompleted(context)
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
                totalChunks = chunk.total,
                firstChunkTimestamp = System.currentTimeMillis()
            )
        }

        val decodedChunkBytes = try {
            android.util.Base64.decode(chunk.payloadBase64, android.util.Base64.DEFAULT)
        } catch (_: Exception) {
            _scanProgress.value = currentProg.copy(
                corruptedCount = currentProg.corruptedCount + 1,
                validationMessage = "Corrupted Base64 encoding in Chunk #${chunk.index + 1}"
            )
            HapticFeedbackHelper.vibrateCorruptedChunk(context)
            return
        }

        val computedChunkSha = CryptoManager.computeSha256(decodedChunkBytes)
        if (computedChunkSha != chunk.chunkSha256) {
            _scanProgress.value = currentProg.copy(
                corruptedCount = currentProg.corruptedCount + 1,
                validationMessage = "Checksum mismatch on Chunk #${chunk.index + 1}"
            )
            HapticFeedbackHelper.vibrateCorruptedChunk(context)
            _toastEvent.emit("Corrupted QR chunk #${chunk.index + 1} (SHA-256 mismatch)")
            return
        }

        val isDuplicate = currentProg.receivedChunks.containsKey(chunk.index)
        currentProg.receivedChunks[chunk.index] = decodedChunkBytes

        val now = System.currentTimeMillis()
        val timeDeltaMs = (now - currentProg.lastReceivedTimestamp).coerceAtLeast(1L)
        val instantSpeed = if (timeDeltaMs in 10..5000 && !isDuplicate) {
            (decodedChunkBytes.size * 1000f) / timeDeltaMs
        } else {
            currentProg.instantaneousSpeedBytesPerSec
        }
        val prevSmoothed = currentProg.smoothedSpeedBytesPerSec
        val newSmoothed = if (prevSmoothed <= 0f) {
            instantSpeed
        } else if (instantSpeed > 0f) {
            prevSmoothed * 0.65f + instantSpeed * 0.35f
        } else {
            prevSmoothed
        }

        val updatedProg = currentProg.copy(
            receivedChunks = HashMap(currentProg.receivedChunks),
            lastReceivedIndex = chunk.index,
            lastReceivedTimestamp = now,
            duplicateCount = if (isDuplicate) currentProg.duplicateCount + 1 else currentProg.duplicateCount,
            instantaneousSpeedBytesPerSec = instantSpeed,
            smoothedSpeedBytesPerSec = newSmoothed,
            validationMessage = if (isDuplicate) {
                "Frame #${chunk.index + 1}/${chunk.total} (Already in buffer)"
            } else {
                "Chunk #${chunk.index + 1}/${chunk.total} Validated ✓ [${computedChunkSha.take(8)}]"
            }
        )

        _scanProgress.value = updatedProg
        _frameCaptureEvent.emit(chunk.index)

        // Provide tactile confirmation on every newly validated frame chunk
        if (!isDuplicate) {
            HapticFeedbackHelper.vibrateFrameDetected(context)
        }

        if (updatedProg.isComplete) {
            cancelStreamTimeoutTimer()
            // Trigger celebratory completion haptic pattern
            HapticFeedbackHelper.vibrateStreamCompleted(context)
            val assembledEnvelope = CryptoManager.assembleChunks(updatedProg.receivedChunks, updatedProg.totalChunks)
            if (assembledEnvelope != null) {
                completeQrStreamTransfer(updatedProg, assembledEnvelope, context)
            }
        } else {
            // Multi-part stream still in progress: restart the inactivity timeout timer
            restartStreamTimeoutTimer(
                context = context,
                fileName = updatedProg.fileName,
                receivedCount = updatedProg.receivedCount,
                totalChunks = updatedProg.totalChunks
            )
        }
    }

    private fun restartStreamTimeoutTimer(
        context: Context,
        fileName: String,
        receivedCount: Int,
        totalChunks: Int
    ) {
        streamTimeoutJob?.cancel()
        val timeoutSec = _streamTimeoutSeconds.value
        if (timeoutSec <= 0) {
            _streamRemainingSeconds.value = null
            return
        }

        _streamRemainingSeconds.value = timeoutSec

        streamTimeoutJob = viewModelScope.launch {
            for (sec in timeoutSec downTo 1) {
                _streamRemainingSeconds.value = sec
                delay(1000L)
            }
            _streamRemainingSeconds.value = 0

            // Stream timed out: Reset scanner and alert user
            val notice = StreamTimeoutNotice(
                fileName = fileName,
                receivedCount = receivedCount,
                totalChunks = totalChunks,
                timeoutSeconds = timeoutSec
            )
            _lastTimeoutNotice.value = notice
            _scanProgress.value = null
            _streamRemainingSeconds.value = null

            HapticFeedbackHelper.vibrateTimeoutAlert(context)
            _toastEvent.emit("QR Stream Timed Out (${timeoutSec}s): Stream reset ($receivedCount/$totalChunks chunks received).")
            _streamTimeoutEvent.emit(notice)
        }
    }

    private fun cancelStreamTimeoutTimer() {
        streamTimeoutJob?.cancel()
        streamTimeoutJob = null
        _streamRemainingSeconds.value = null
    }

    fun setStreamTimeoutSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(5, 120)
        _streamTimeoutSeconds.value = clamped
        sharedPreferences.edit().putInt("stream_timeout_sec", clamped).apply()
    }

    fun dismissTimeoutNotice() {
        _lastTimeoutNotice.value = null
    }

    private suspend fun completeQrStreamTransfer(
        progress: QrChunkProgress,
        assembledEnvelope: ByteArray,
        context: Context
    ) {
        withContext(Dispatchers.IO) {
            // Try decrypting with active team key or all available team keys
            val keyList = mutableListOf<String>()
            _activeTeamKey.value?.let { keyList.add(it.passphraseOrKey) }
            val allKeys = teamKeyRepository.getDefaultTeamKey()
            allKeys?.let { if (!keyList.contains(it.passphraseOrKey)) keyList.add(it.passphraseOrKey) }

            // Add all other team keys
            val allTeamKeys = teamKeys.value
            for (tk in allTeamKeys) {
                if (!keyList.contains(tk.passphraseOrKey)) {
                    keyList.add(tk.passphraseOrKey)
                }
            }

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
                    _toastEvent.emit("Warning: Decrypted data SHA-256 mismatch!")
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
                    sourceInfo = "Optical QR Stream Broadcast",
                    destinationInfo = "Local Storage Vault (${_activeTeamKey.value?.teamName ?: "Team Vault"})",
                    teamMemberName = "Team Peer",
                    teamName = _activeTeamKey.value?.teamName ?: "Team Vault",
                    timestamp = System.currentTimeMillis(),
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
                _pendingDecryption.value = null
                HapticFeedbackHelper.vibrateDecryptionSuccess(context)
                _toastEvent.emit("Successfully assembled & decrypted ${progress.fileName}!")
            } else {
                // Prompt user for custom passphrase
                _pendingDecryption.value = PendingDecryptionState(progress, assembledEnvelope)
                HapticFeedbackHelper.vibrateStreamCompleted(context)
                _toastEvent.emit("All ${progress.totalChunks} chunks assembled! Enter passphrase to decrypt.")
            }
        }
    }

    fun decryptPendingWithPassphrase(passphrase: String, context: Context) {
        val pending = _pendingDecryption.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decryptedBytes = CryptoManager.decryptData(pending.assembledEnvelope, passphrase.trim())
                val computedOriginalSha = CryptoManager.computeSha256(decryptedBytes)
                if (computedOriginalSha != pending.progress.originalSha256) {
                    _toastEvent.emit("Warning: Decrypted data SHA-256 mismatch!")
                }

                val savedFile = FileUtils.saveBytesToInternalStorage(context, pending.progress.fileName, decryptedBytes)
                val safetyNum = CryptoManager.generateSafetyNumber(pending.progress.originalSha256, passphrase.trim())
                val textPreview = if (pending.progress.mimeType.startsWith("text/")) String(decryptedBytes, Charsets.UTF_8).take(200) else null

                val record = TransferRecord(
                    transferId = pending.progress.transferId,
                    fileName = pending.progress.fileName,
                    mimeType = pending.progress.mimeType,
                    originalSize = pending.progress.originalSize,
                    encryptedSize = pending.assembledEnvelope.size.toLong(),
                    isReceived = true,
                    transferMode = TransferMode.QR_STREAM,
                    sourceInfo = "Optical QR Stream Broadcast",
                    destinationInfo = "Local Storage Vault (Ad-hoc Passphrase)",
                    teamMemberName = "Ad-hoc Peer",
                    teamName = "Custom Passphrase",
                    timestamp = System.currentTimeMillis(),
                    status = TransferStatus.COMPLETED,
                    sha256Checksum = pending.progress.originalSha256,
                    safetyNumber = safetyNum,
                    localFilePath = savedFile.absolutePath,
                    decryptedTextPreview = textPreview
                )

                val id = transferRepository.insert(record)
                val savedRecord = record.copy(id = id)
                _inspectedRecord.value = savedRecord
                _scanProgress.value = null
                _pendingDecryption.value = null
                HapticFeedbackHelper.vibrateDecryptionSuccess(context)
                _toastEvent.emit("Decryption successful: ${pending.progress.fileName}")
            } catch (e: Exception) {
                HapticFeedbackHelper.vibratePassphraseError(context)
                _toastEvent.emit("Decryption failed: Invalid passphrase or corrupted key.")
            }
        }
    }

    fun dismissPendingDecryption() {
        _pendingDecryption.value = null
    }

    fun refreshNetworkInfo(context: Context? = null) {
        val ctx = context ?: getApplication()
        _networkInfo.value = NetworkUtils.getNetworkInfo(ctx)
    }

    fun openWifiSettings(context: Context) {
        NetworkUtils.openWifiSettings(context)
    }

    fun openHotspotSettings(context: Context) {
        NetworkUtils.openHotspotSettings(context)
    }

    fun scanLanForPeers(context: Context? = null) {
        if (_isScanningPeers.value) return
        viewModelScope.launch {
            _isScanningPeers.value = true
            try {
                val subnet = _networkInfo.value.subnetPrefix
                val peers = LocalTransferClient.discoverPeers(subnetPrefix = subnet, port = 8989)
                _discoveredPeers.value = peers
                if (peers.isEmpty()) {
                    _toastEvent.emit("No active Sender hosts detected on subnet $subnet")
                } else {
                    _toastEvent.emit("Discovered ${peers.size} active Sender host(s) on LAN!")
                }
            } catch (e: Exception) {
                _toastEvent.emit("LAN scan error: ${e.localizedMessage}")
            } finally {
                _isScanningPeers.value = false
            }
        }
    }

    fun startReceiverFileDropServer(port: Int = 8990, context: Context) {
        viewModelScope.launch {
            receiverServer.startReceiverServer(port) { fileName, mimeType, fileBytes, clientIp ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val savedFile = FileUtils.saveBytesToInternalStorage(context, fileName, fileBytes)
                        val sha = CryptoManager.computeSha256(fileBytes)
                        val record = TransferRecord(
                            transferId = UUID.randomUUID().toString(),
                            fileName = fileName,
                            mimeType = mimeType,
                            originalSize = fileBytes.size.toLong(),
                            encryptedSize = fileBytes.size.toLong(),
                            isReceived = true,
                            transferMode = TransferMode.P2P_DIRECT,
                            sourceInfo = "LAN Web Drop ($clientIp)",
                            destinationInfo = "Local Storage Vault",
                            teamMemberName = "LAN Client ($clientIp)",
                            teamName = "Wi-Fi Web Drop",
                            timestamp = System.currentTimeMillis(),
                            status = TransferStatus.COMPLETED,
                            sha256Checksum = sha,
                            safetyNumber = CryptoManager.generateSafetyNumber(sha, "DIRECT_WEB_DROP"),
                            localFilePath = savedFile.absolutePath,
                            decryptedTextPreview = if (fileName.endsWith(".txt") || fileName.endsWith(".json")) String(fileBytes.take(200).toByteArray(), Charsets.UTF_8) else null
                        )
                        val id = transferRepository.insert(record)
                        val savedRecord = record.copy(id = id)
                        withContext(Dispatchers.Main) {
                            _inspectedRecord.value = savedRecord
                            HapticFeedbackHelper.vibrateTransferSuccess(context)
                            _toastEvent.emit("File received via Web Drop: $fileName")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _toastEvent.emit("Failed to save received file: ${e.localizedMessage}")
                        }
                    }
                }
            }
            refreshNetworkInfo(context)
            _toastEvent.emit("Receiver Drop Portal active on port $port")
        }
    }

    fun stopReceiverFileDropServer() {
        receiverServer.stopServer()
    }

    fun downloadDiscoveredPeer(peer: DiscoveredPeer, customKeyOrPass: String? = null, context: Context) {
        val ticket = P2PTransferTicket(
            transferId = peer.transferId.ifBlank { UUID.randomUUID().toString() },
            fileName = peer.fileName,
            mimeType = peer.mimeType.ifBlank { "application/octet-stream" },
            originalSize = peer.fileSize,
            encryptedSize = peer.fileSize,
            sha256 = peer.sha256,
            hostIp = peer.ip,
            port = peer.port,
            encryptionKeyBase64 = customKeyOrPass ?: (_activeTeamKey.value?.passphraseOrKey ?: ""),
            teamName = _activeTeamKey.value?.teamName ?: "Direct LAN"
        )
        downloadAndDecryptP2PTicket(ticket, context)
    }

    fun downloadManualIp(
        hostIp: String,
        port: Int = 8989,
        passphraseOrKey: String,
        context: Context
    ) {
        viewModelScope.launch {
            _isDownloadingP2P.value = true
            _p2pDownloadProgress.value = 0f
            _p2pDownloadSpeed.value = 0L

            val metadataResult = withContext(Dispatchers.IO) {
                LocalTransferClient.fetchTransferMetadata(hostIp, port)
            }
            val metadata = metadataResult.getOrNull()

            val fileName = metadata?.fileName ?: "lan_transfer.dat"
            val mimeType = metadata?.mimeType ?: "application/octet-stream"
            val transferId = metadata?.transferId ?: UUID.randomUUID().toString()

            val result = LocalTransferClient.downloadEncryptedPayload(hostIp, port) { bytesRead, total, frac, speed ->
                _p2pDownloadProgress.value = frac
                _p2pDownloadSpeed.value = speed
            }

            _isDownloadingP2P.value = false
            _p2pDownloadSpeed.value = 0L

            result.onSuccess { encryptedEnvelope ->
                withContext(Dispatchers.IO) {
                    try {
                        val keyToUse = passphraseOrKey.ifBlank { _activeTeamKey.value?.passphraseOrKey ?: "" }
                        val decrypted = CryptoManager.decryptData(encryptedEnvelope, keyToUse)
                        val computedSha = CryptoManager.computeSha256(decrypted)

                        val savedFile = FileUtils.saveBytesToInternalStorage(context, fileName, decrypted)
                        val safetyNum = CryptoManager.generateSafetyNumber(computedSha, keyToUse)
                        val textPreview = if (mimeType.startsWith("text/")) String(decrypted, Charsets.UTF_8).take(200) else null

                        val record = TransferRecord(
                            transferId = transferId,
                            fileName = fileName,
                            mimeType = mimeType,
                            originalSize = decrypted.size.toLong(),
                            encryptedSize = encryptedEnvelope.size.toLong(),
                            isReceived = true,
                            transferMode = TransferMode.P2P_DIRECT,
                            sourceInfo = "Direct Host ($hostIp:$port)",
                            destinationInfo = "Local Storage Vault",
                            teamMemberName = "LAN Peer",
                            teamName = _activeTeamKey.value?.teamName ?: "Direct Wi-Fi / Hotspot",
                            timestamp = System.currentTimeMillis(),
                            status = TransferStatus.COMPLETED,
                            sha256Checksum = computedSha,
                            safetyNumber = safetyNum,
                            localFilePath = savedFile.absolutePath,
                            decryptedTextPreview = textPreview
                        )

                        val id = transferRepository.insert(record)
                        val savedRecord = record.copy(id = id)
                        withContext(Dispatchers.Main) {
                            _inspectedRecord.value = savedRecord
                            HapticFeedbackHelper.vibrateTransferSuccess(context)
                            _toastEvent.emit("Direct LAN transfer complete: $fileName")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            HapticFeedbackHelper.vibratePassphraseError(context)
                            _toastEvent.emit("Failed to decrypt: Check passphrase or encryption key.")
                        }
                    }
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    HapticFeedbackHelper.vibratePassphraseError(context)
                    _toastEvent.emit("Connection failed to $hostIp:$port (${err.localizedMessage})")
                }
            }
        }
    }

    fun downloadAndDecryptP2PTicket(ticket: P2PTransferTicket, context: Context) {
        viewModelScope.launch {
            _isDownloadingP2P.value = true
            _p2pDownloadProgress.value = 0f
            _p2pDownloadSpeed.value = 0L

            val result = LocalTransferClient.downloadEncryptedPayload(ticket.hostIp, ticket.port) { bytesRead, total, frac, speed ->
                _p2pDownloadProgress.value = frac
                _p2pDownloadSpeed.value = speed
            }

            _isDownloadingP2P.value = false
            _p2pDownloadSpeed.value = 0L

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
                            sourceInfo = "P2P Host (${ticket.hostIp}:${ticket.port})",
                            destinationInfo = "Local Storage Vault (${ticket.teamName ?: "Direct P2P"})",
                            teamMemberName = "Team Peer (LAN)",
                            teamName = ticket.teamName ?: "Direct P2P",
                            timestamp = System.currentTimeMillis(),
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
                        HapticFeedbackHelper.vibrateTransferSuccess(context)
                        _toastEvent.emit("P2P File received & verified: ${ticket.fileName}")
                    } catch (e: Exception) {
                        HapticFeedbackHelper.vibratePassphraseError(context)
                        _toastEvent.emit("Failed to decrypt P2P payload: ${e.localizedMessage}")
                    }
                }
            }.onFailure { err ->
                HapticFeedbackHelper.vibratePassphraseError(context)
                _toastEvent.emit("Failed to download from ${ticket.hostIp}: ${err.localizedMessage}")
            }
        }
    }

    fun dismissP2PTicket() {
        _scannedP2PTicket.value = null
    }

    fun resetScanProgress() {
        cancelStreamTimeoutTimer()
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

    fun deleteRecords(records: List<TransferRecord>) {
        if (records.isEmpty()) return
        viewModelScope.launch {
            records.forEach { record ->
                record.localFilePath?.let {
                    try {
                        File(it).delete()
                    } catch (e: Exception) {
                        // Ignore file delete errors
                    }
                }
            }
            transferRepository.deleteRecords(records)
            if (_inspectedRecord.value != null && records.any { it.id == _inspectedRecord.value?.id }) {
                _inspectedRecord.value = null
            }
            _toastEvent.emit("Deleted ${records.size} records")
        }
    }

    fun purgeAllTransfers() {
        viewModelScope.launch {
            val all = transfers.value
            all.forEach { r ->
                r.localFilePath?.let {
                    try { File(it).delete() } catch (_: Exception) {}
                }
            }
            transferRepository.deleteRecords(all)
            _inspectedRecord.value = null
            _toastEvent.emit("All transfer history purged")
        }
    }

    fun clearAppCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = FileUtils.clearTemporaryCache(context)
            if (success) {
                _toastEvent.emit("Temporary cache cleared")
            } else {
                _toastEvent.emit("Cache cleanup completed")
            }
        }
    }

    fun shareApk(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = FileUtils.shareAppApk(context)
            if (!success) {
                _toastEvent.emit("Could not extract APK file")
            }
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
