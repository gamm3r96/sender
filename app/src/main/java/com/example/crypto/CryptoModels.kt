package com.example.crypto

data class EncryptedPayload(
    val envelopeBytes: ByteArray,
    val sha256Original: String,
    val sha256Encrypted: String,
    val salt: ByteArray,
    val iv: ByteArray,
    val algorithm: String = "AES-256-GCM"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        return envelopeBytes.contentEquals(other.envelopeBytes)
    }

    override fun hashCode(): Int = envelopeBytes.contentHashCode()
}

data class ChunkEnvelope(
    val transferId: String,
    val index: Int, // 0-based
    val total: Int,
    val fileName: String,
    val mimeType: String,
    val originalSize: Long,
    val payloadBase64: String,
    val originalSha256: String,
    val chunkSha256: String
)

data class QrChunkProgress(
    val transferId: String,
    val fileName: String,
    val mimeType: String,
    val originalSize: Long,
    val originalSha256: String,
    val totalChunks: Int,
    val receivedChunks: MutableMap<Int, ByteArray> = mutableMapOf(),
    val firstChunkTimestamp: Long = System.currentTimeMillis(),
    val lastReceivedIndex: Int? = null,
    val lastReceivedTimestamp: Long = System.currentTimeMillis(),
    val validationMessage: String = "",
    val corruptedCount: Int = 0,
    val duplicateCount: Int = 0,
    val instantaneousSpeedBytesPerSec: Float = 0f,
    val smoothedSpeedBytesPerSec: Float = 0f
) {
    val receivedCount: Int get() = receivedChunks.size
    val progressFraction: Float get() = if (totalChunks > 0) receivedCount.toFloat() / totalChunks else 0f
    val isComplete: Boolean get() = totalChunks > 0 && receivedCount >= totalChunks
    val missingIndices: List<Int> get() = (0 until totalChunks).filter { !receivedChunks.containsKey(it) }
    val assembledBytes: Long get() = receivedChunks.values.sumOf { it.size.toLong() }

    val estimatedSpeedChunksPerSec: Float get() {
        val durationSec = (lastReceivedTimestamp - firstChunkTimestamp) / 1000f
        return if (durationSec > 0.2f && receivedCount > 1) {
            receivedCount / durationSec
        } else if (receivedCount > 0) {
            1.0f
        } else {
            0f
        }
    }

    val estimatedSpeedBytesPerSec: Float get() {
        if (smoothedSpeedBytesPerSec > 0f) return smoothedSpeedBytesPerSec
        val durationSec = (lastReceivedTimestamp - firstChunkTimestamp) / 1000f
        return if (durationSec > 0.2f && receivedCount > 1 && assembledBytes > 0) {
            assembledBytes.toFloat() / durationSec
        } else if (assembledBytes > 0) {
            // Initial chunk estimate
            assembledBytes.toFloat() / 0.5f
        } else {
            0f
        }
    }

    val formattedTransferSpeed: String get() {
        val speed = estimatedSpeedBytesPerSec
        return when {
            speed <= 0f -> "-- KB/s"
            speed < 1024f -> String.format(java.util.Locale.US, "%.0f B/s", speed)
            speed < 1024f * 1024f -> String.format(java.util.Locale.US, "%.1f KB/s", speed / 1024f)
            else -> String.format(java.util.Locale.US, "%.2f MB/s", speed / (1024f * 1024f))
        }
    }

    val estimatedRemainingSeconds: Int? get() {
        val missing = missingIndices.size
        val speed = estimatedSpeedChunksPerSec
        return if (missing > 0 && speed > 0.2f) {
            (missing / speed).toInt().coerceAtLeast(1)
        } else {
            null
        }
    }

    val transferEfficiencyScore: Int get() {
        val totalAttempts = receivedCount + duplicateCount + corruptedCount
        if (totalAttempts == 0) return 100
        val ratio = receivedCount.toFloat() / totalAttempts
        return (ratio * 100).toInt().coerceIn(10, 100)
    }
}

data class P2PTransferTicket(
    val transferId: String,
    val hostIp: String,
    val port: Int,
    val fileName: String,
    val mimeType: String,
    val originalSize: Long,
    val encryptedSize: Long,
    val sha256: String,
    val encryptionKeyBase64: String,
    val teamName: String? = null
)
