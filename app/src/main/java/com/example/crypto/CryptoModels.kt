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
    val firstChunkTimestamp: Long = System.currentTimeMillis()
) {
    val receivedCount: Int get() = receivedChunks.size
    val progressFraction: Float get() = if (totalChunks > 0) receivedCount.toFloat() / totalChunks else 0f
    val isComplete: Boolean get() = totalChunks > 0 && receivedCount >= totalChunks
    val missingIndices: List<Int> get() = (0 until totalChunks).filter { !receivedChunks.containsKey(it) }
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
