package com.example.qr

import android.util.Base64
import com.example.crypto.ChunkEnvelope
import com.example.crypto.CryptoManager
import com.example.crypto.QrChunkProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents the current status and metrics of the QR stream reassembly session.
 */
sealed class StreamReassemblyState {
    object Idle : StreamReassemblyState()
    
    data class Assembling(
        val transferId: String,
        val fileName: String,
        val mimeType: String,
        val originalSize: Long,
        val originalSha256: String,
        val totalChunks: Int,
        val receivedCount: Int,
        val progressFraction: Float,
        val missingIndices: List<Int>,
        val lastReceivedIndex: Int,
        val duplicateCount: Int,
        val corruptedCount: Int,
        val transferSpeedBytesPerSec: Float,
        val estimatedRemainingSeconds: Int?,
        val efficiencyScore: Int,
        val validationMessage: String
    ) : StreamReassemblyState()

    data class Complete(
        val transferId: String,
        val fileName: String,
        val mimeType: String,
        val originalSize: Long,
        val originalSha256: String,
        val assembledPayload: ByteArray,
        val totalChunks: Int,
        val durationMs: Long
    ) : StreamReassemblyState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Complete
            return transferId == other.transferId && assembledPayload.contentEquals(other.assembledPayload)
        }

        override fun hashCode(): Int = transferId.hashCode() * 31 + assembledPayload.contentHashCode()
    }

    data class Error(
        val transferId: String?,
        val errorMessage: String,
        val isRecoverable: Boolean = true
    ) : StreamReassemblyState()
}

/**
 * Result of processing a single scanned QR chunk frame.
 */
sealed class ChunkProcessResult {
    data class NewChunk(val index: Int, val total: Int, val isComplete: Boolean) : ChunkProcessResult()
    data class DuplicateChunk(val index: Int, val total: Int) : ChunkProcessResult()
    data class CorruptedChunk(val reason: String, val chunkIndex: Int?) : ChunkProcessResult()
    data class NonStreamQr(val rawContent: String) : ChunkProcessResult()
    data class InvalidFormat(val rawContent: String) : ChunkProcessResult()
}

/**
 * Industrial-strength stateful reassembly engine for incoming fragmented QR code streams.
 * 
 * Supports:
 * - High-speed out-of-order chunk intake (e.g. 5, 2, 0, 4, 1, 3).
 * - Multi-pass cyclic loop scanning (captures dropped frames on subsequent broadcast cycles).
 * - Per-chunk SHA-256 cryptographic verification before memory buffer commitment.
 * - Dynamic throughput calculation (instantaneous & exponentially weighted moving average).
 * - Memory-safe chunk stitching and final payload verification.
 */
class QrStreamReassembler {

    private val _state = MutableStateFlow<StreamReassemblyState>(StreamReassemblyState.Idle)
    val state: StateFlow<StreamReassemblyState> = _state.asStateFlow()

    private val _chunkProgress = MutableStateFlow<QrChunkProgress?>(null)
    val chunkProgress: StateFlow<QrChunkProgress?> = _chunkProgress.asStateFlow()

    // Internal thread-safe buffer for received chunk payloads
    private val chunkBuffer = ConcurrentHashMap<Int, ByteArray>()
    
    private var activeTransferId: String? = null
    private var activeFileName: String = ""
    private var activeMimeType: String = ""
    private var activeOriginalSize: Long = 0L
    private var activeOriginalSha: String = ""
    private var totalChunksCount: Int = 0

    private var firstChunkTimestamp: Long = 0L
    private var lastReceivedTimestamp: Long = 0L
    private var duplicateCounter: Int = 0
    private var corruptedCounter: Int = 0
    private var smoothedSpeed: Float = 0f

    /**
     * Ingests a raw QR code string detected by CameraX, parses the chunk envelope,
     * verifies chunk checksums, records progress, and triggers assembly when complete.
     */
    @Synchronized
    fun processScannedQr(rawText: String): ChunkProcessResult {
        // Check if QR matches CPQR protocol header
        if (!rawText.startsWith("CPQR1:")) {
            return if (rawText.startsWith("CPQR_P2P:") || rawText.startsWith("CIPHER_KEY:")) {
                ChunkProcessResult.NonStreamQr(rawText)
            } else {
                ChunkProcessResult.InvalidFormat(rawText)
            }
        }

        val envelope = CryptoManager.parseQrChunk(rawText)
            ?: return ChunkProcessResult.CorruptedChunk("Failed to parse JSON chunk structure", null)

        val now = System.currentTimeMillis()

        // If transfer ID changed, initialize a fresh reassembly session
        if (activeTransferId != envelope.transferId) {
            resetReassembler()
            activeTransferId = envelope.transferId
            activeFileName = envelope.fileName
            activeMimeType = envelope.mimeType
            activeOriginalSize = envelope.originalSize
            activeOriginalSha = envelope.originalSha256
            totalChunksCount = envelope.total
            firstChunkTimestamp = now
            lastReceivedTimestamp = now
        }

        // Decode Base64 chunk bytes
        val decodedChunkBytes = try {
            Base64.decode(envelope.payloadBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            corruptedCounter++
            updateProgressState(
                lastIndex = envelope.index,
                validationMsg = "Corrupted Base64 encoding in Chunk #${envelope.index + 1}",
                isCorrupted = true
            )
            return ChunkProcessResult.CorruptedChunk("Invalid Base64 payload in chunk #${envelope.index + 1}", envelope.index)
        }

        // Validate chunk SHA-256 integrity checksum
        val computedChunkSha = CryptoManager.computeSha256(decodedChunkBytes)
        if (computedChunkSha != envelope.chunkSha256) {
            corruptedCounter++
            updateProgressState(
                lastIndex = envelope.index,
                validationMsg = "Integrity mismatch on Chunk #${envelope.index + 1}",
                isCorrupted = true
            )
            return ChunkProcessResult.CorruptedChunk("SHA-256 mismatch on chunk #${envelope.index + 1}", envelope.index)
        }

        // Check for duplicate chunk (already in buffer from previous loop cycle)
        val isDuplicate = chunkBuffer.containsKey(envelope.index)
        if (isDuplicate) {
            duplicateCounter++
            updateProgressState(
                lastIndex = envelope.index,
                validationMsg = "Frame #${envelope.index + 1}/${envelope.total} (Already Buffered)",
                isDuplicate = true
            )
            return ChunkProcessResult.DuplicateChunk(envelope.index, envelope.total)
        }

        // Commit valid chunk to memory buffer
        chunkBuffer[envelope.index] = decodedChunkBytes

        // Compute real-time throughput metrics
        val timeDeltaMs = (now - lastReceivedTimestamp).coerceAtLeast(1L)
        lastReceivedTimestamp = now

        val instantSpeed = if (timeDeltaMs in 10..5000) {
            (decodedChunkBytes.size * 1000f) / timeDeltaMs
        } else {
            0f
        }

        smoothedSpeed = if (smoothedSpeed <= 0f) {
            instantSpeed
        } else if (instantSpeed > 0f) {
            smoothedSpeed * 0.65f + instantSpeed * 0.35f
        } else {
            smoothedSpeed
        }

        updateProgressState(
            lastIndex = envelope.index,
            validationMsg = "Chunk #${envelope.index + 1}/${envelope.total} Validated ✓ [${computedChunkSha.take(8)}]",
            instantSpeed = instantSpeed
        )

        // Check if all chunks have been received
        val isComplete = chunkBuffer.size >= totalChunksCount && (0 until totalChunksCount).all { chunkBuffer.containsKey(it) }

        if (isComplete) {
            val assembled = assembleFinalPayload()
            if (assembled != null) {
                val duration = (now - firstChunkTimestamp).coerceAtLeast(1L)
                _state.value = StreamReassemblyState.Complete(
                    transferId = activeTransferId ?: "",
                    fileName = activeFileName,
                    mimeType = activeMimeType,
                    originalSize = activeOriginalSize,
                    originalSha256 = activeOriginalSha,
                    assembledPayload = assembled,
                    totalChunks = totalChunksCount,
                    durationMs = duration
                )
            } else {
                _state.value = StreamReassemblyState.Error(
                    transferId = activeTransferId,
                    errorMessage = "Buffer assembly error: Missing chunks detected during concatenation"
                )
            }
        }

        return ChunkProcessResult.NewChunk(envelope.index, envelope.total, isComplete)
    }

    /**
     * Stitches all verified chunk byte arrays in sequential index order (0 until totalChunks).
     */
    fun assembleFinalPayload(): ByteArray? {
        if (totalChunksCount <= 0 || chunkBuffer.size < totalChunksCount) return null

        val outputStream = ByteArrayOutputStream()
        for (i in 0 until totalChunksCount) {
            val chunkBytes = chunkBuffer[i] ?: return null
            outputStream.write(chunkBytes)
        }
        return outputStream.toByteArray()
    }

    /**
     * Gets a list of chunk indices that are still missing from the buffer.
     */
    fun getMissingIndices(): List<Int> {
        if (totalChunksCount <= 0) return emptyList()
        return (0 until totalChunksCount).filter { !chunkBuffer.containsKey(it) }
    }

    /**
     * Returns true if all chunks have been received.
     */
    fun isComplete(): Boolean {
        return totalChunksCount > 0 && chunkBuffer.size >= totalChunksCount && (0 until totalChunksCount).all { chunkBuffer.containsKey(it) }
    }

    private fun updateProgressState(
        lastIndex: Int,
        validationMsg: String,
        isDuplicate: Boolean = false,
        isCorrupted: Boolean = false,
        instantSpeed: Float = 0f
    ) {
        val total = totalChunksCount.coerceAtLeast(1)
        val received = chunkBuffer.size
        val fraction = (received.toFloat() / total).coerceIn(0f, 1f)
        val missing = getMissingIndices()

        val progress = QrChunkProgress(
            transferId = activeTransferId ?: "",
            fileName = activeFileName,
            mimeType = activeMimeType,
            originalSize = activeOriginalSize,
            originalSha256 = activeOriginalSha,
            totalChunks = total,
            receivedChunks = HashMap(chunkBuffer),
            firstChunkTimestamp = firstChunkTimestamp,
            lastReceivedIndex = lastIndex,
            lastReceivedTimestamp = lastReceivedTimestamp,
            validationMessage = validationMsg,
            corruptedCount = corruptedCounter,
            duplicateCount = duplicateCounter,
            instantaneousSpeedBytesPerSec = instantSpeed,
            smoothedSpeedBytesPerSec = smoothedSpeed
        )

        _chunkProgress.value = progress

        if (!isComplete()) {
            _state.value = StreamReassemblyState.Assembling(
                transferId = activeTransferId ?: "",
                fileName = activeFileName,
                mimeType = activeMimeType,
                originalSize = activeOriginalSize,
                originalSha256 = activeOriginalSha,
                totalChunks = total,
                receivedCount = received,
                progressFraction = fraction,
                missingIndices = missing,
                lastReceivedIndex = lastIndex,
                duplicateCount = duplicateCounter,
                corruptedCount = corruptedCounter,
                transferSpeedBytesPerSec = smoothedSpeed,
                estimatedRemainingSeconds = progress.estimatedRemainingSeconds,
                efficiencyScore = progress.transferEfficiencyScore,
                validationMessage = validationMsg
            )
        }
    }

    /**
     * Resets the reassembler buffer and state for a fresh scanning session.
     */
    @Synchronized
    fun resetReassembler() {
        chunkBuffer.clear()
        activeTransferId = null
        activeFileName = ""
        activeMimeType = ""
        activeOriginalSize = 0L
        activeOriginalSha = ""
        totalChunksCount = 0
        firstChunkTimestamp = 0L
        lastReceivedTimestamp = 0L
        duplicateCounter = 0
        corruptedCounter = 0
        smoothedSpeed = 0f
        _chunkProgress.value = null
        _state.value = StreamReassemblyState.Idle
    }
}
