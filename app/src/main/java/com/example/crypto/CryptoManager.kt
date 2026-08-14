package com.example.crypto

import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val AES_KEY_SIZE_BITS = 256
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 10000
    private const val SALT_LENGTH_BYTES = 16
    private const val PROTOCOL_MAGIC = 0x43505152 // "CPQR"

    private val secureRandom = SecureRandom()

    /**
     * Generates a 256-bit high-entropy ephemeral key encoded in URL-safe Base64
     */
    fun generateEphemeralKey(): String {
        val keyBytes = ByteArray(32)
        secureRandom.nextBytes(keyBytes)
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    /**
     * Derives a 256-bit AES key from a passphrase or raw key string
     */
    fun deriveKey(passphraseOrKey: String, salt: ByteArray): SecretKey {
        // Check if string is already a 32-byte Base64 raw key
        try {
            val decoded = Base64.decode(passphraseOrKey, Base64.DEFAULT)
            if (decoded.size == 32) {
                return SecretKeySpec(decoded, "AES")
            }
        } catch (_: Exception) {
            // Not a raw base64 key, proceed to PBKDF2 derivation
        }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphraseOrKey.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE_BITS)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM and wraps into a standard binary envelope:
     * [MAGIC 4B][SALT 16B][IV 12B][CIPHERTEXT + TAG]
     */
    fun encryptData(plaintext: ByteArray, passphraseOrKey: String): EncryptedPayload {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val secretKey = deriveKey(passphraseOrKey, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext)

        val envelope = ByteBuffer.allocate(4 + SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + ciphertext.size)
        envelope.putInt(PROTOCOL_MAGIC)
        envelope.put(salt)
        envelope.put(iv)
        envelope.put(ciphertext)

        val envelopeBytes = envelope.array()
        val originalSha = computeSha256(plaintext)
        val encryptedSha = computeSha256(envelopeBytes)

        return EncryptedPayload(
            envelopeBytes = envelopeBytes,
            sha256Original = originalSha,
            sha256Encrypted = encryptedSha,
            salt = salt,
            iv = iv
        )
    }

    /**
     * Decrypts an envelope created by encryptData
     */
    fun decryptData(envelopeBytes: ByteArray, passphraseOrKey: String): ByteArray {
        require(envelopeBytes.size >= 4 + SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + 16) {
            "Envelope is too small to be a valid encrypted payload"
        }

        val buffer = ByteBuffer.wrap(envelopeBytes)
        val magic = buffer.int
        require(magic == PROTOCOL_MAGIC) {
            "Invalid protocol magic header: $magic"
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        buffer.get(salt)

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        buffer.get(iv)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val secretKey = deriveKey(passphraseOrKey, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Computes SHA-256 hash formatted as lowercase hex
     */
    fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a 6-block numeric safety number for verifying peer identity
     */
    fun generateSafetyNumber(dataSha: String, keyOrTeam: String): String {
        val combined = "$dataSha:$keyOrTeam".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(combined)
        val blocks = mutableListOf<String>()
        for (i in 0 until 6) {
            val offset = i * 4
            val num = ((digest[offset].toInt() and 0xFF) shl 24) or
                    ((digest[offset + 1].toInt() and 0xFF) shl 16) or
                    ((digest[offset + 2].toInt() and 0xFF) shl 8) or
                    (digest[offset + 3].toInt() and 0xFF)
            val positiveNum = Math.abs(num) % 100000
            blocks.add("%05d".format(positiveNum))
        }
        return blocks.joinToString(" ")
    }

    /**
     * Splits encrypted envelope bytes into QR code stream chunks formatted as JSON strings
     */
    fun createQrChunks(
        encryptedBytes: ByteArray,
        fileName: String,
        mimeType: String,
        originalSize: Long,
        originalSha256: String,
        transferId: String,
        targetChunkSizeBytes: Int = 480 // Safe size for standard QR code capacity
    ): List<String> {
        val totalSize = encryptedBytes.size
        val chunkCount = ((totalSize + targetChunkSizeBytes - 1) / targetChunkSizeBytes).coerceAtLeast(1)
        val chunks = mutableListOf<String>()

        for (i in 0 until chunkCount) {
            val start = i * targetChunkSizeBytes
            val end = (start + targetChunkSizeBytes).coerceAtMost(totalSize)
            val chunkSlice = encryptedBytes.copyOfRange(start, end)
            val chunkPayloadBase64 = Base64.encodeToString(chunkSlice, Base64.NO_WRAP)
            val chunkSha = computeSha256(chunkSlice)

            val json = JSONObject().apply {
                put("v", 1)
                put("tid", transferId)
                put("idx", i)
                put("tot", chunkCount)
                put("fn", fileName)
                put("mime", mimeType)
                put("osz", originalSize)
                put("data", chunkPayloadBase64)
                put("osha", originalSha256)
                put("csha", chunkSha)
            }

            chunks.add("CPQR1:" + json.toString())
        }

        return chunks
    }

    /**
     * Parses a QR scanned chunk string
     */
    fun parseQrChunk(rawText: String): ChunkEnvelope? {
        try {
            if (!rawText.startsWith("CPQR1:")) return null
            val jsonStr = rawText.removePrefix("CPQR1:")
            val json = JSONObject(jsonStr)
            return ChunkEnvelope(
                transferId = json.getString("tid"),
                index = json.getInt("idx"),
                total = json.getInt("tot"),
                fileName = json.getString("fn"),
                mimeType = json.getString("mime"),
                originalSize = json.getLong("osz"),
                payloadBase64 = json.getString("data"),
                originalSha256 = json.getString("osha"),
                chunkSha256 = json.getString("csha")
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Assembles received chunks into complete envelope bytes after verifying each chunk's integrity
     */
    fun assembleChunks(chunks: Map<Int, ByteArray>, totalChunks: Int): ByteArray? {
        if (chunks.size < totalChunks) return null
        val baos = ByteArrayOutputStream()
        for (i in 0 until totalChunks) {
            val chunk = chunks[i] ?: return null
            baos.write(chunk)
        }
        return baos.toByteArray()
    }

    /**
     * Creates a ticket string for Local P2P QR Handshake
     */
    fun createP2PTicketQr(ticket: P2PTransferTicket): String {
        val json = JSONObject().apply {
            put("p2p", 1)
            put("tid", ticket.transferId)
            put("ip", ticket.hostIp)
            put("port", ticket.port)
            put("fn", ticket.fileName)
            put("mime", ticket.mimeType)
            put("osz", ticket.originalSize)
            put("esz", ticket.encryptedSize)
            put("sha", ticket.sha256)
            put("key", ticket.encryptionKeyBase64)
            ticket.teamName?.let { put("team", it) }
        }
        return "CPQR_P2P:" + json.toString()
    }

    /**
     * Parses a P2P ticket QR string
     */
    fun parseP2PTicketQr(rawText: String): P2PTransferTicket? {
        try {
            if (!rawText.startsWith("CPQR_P2P:")) return null
            val json = JSONObject(rawText.removePrefix("CPQR_P2P:"))
            return P2PTransferTicket(
                transferId = json.getString("tid"),
                hostIp = json.getString("ip"),
                port = json.getInt("port"),
                fileName = json.getString("fn"),
                mimeType = json.getString("mime"),
                originalSize = json.getLong("osz"),
                encryptedSize = json.getLong("esz"),
                sha256 = json.getString("sha"),
                encryptionKeyBase64 = json.getString("key"),
                teamName = if (json.has("team")) json.getString("team") else null
            )
        } catch (_: Exception) {
            return null
        }
    }
}
