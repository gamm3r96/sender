package com.example

import com.example.crypto.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun test_aes256_gcm_encryption_and_decryption() {
        val originalText = "TopSecret-Team-Credentials-12345!@#"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)
        val key = CryptoManager.generateEphemeralKey()

        val encrypted = CryptoManager.encryptData(originalBytes, key)
        assertNotNull(encrypted.envelopeBytes)
        assertTrue(encrypted.envelopeBytes.size > originalBytes.size)

        val decrypted = CryptoManager.decryptData(encrypted.envelopeBytes, key)
        val decryptedText = String(decrypted, Charsets.UTF_8)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun test_qr_chunking_and_reassembly() {
        val samplePayload = "A".repeat(1500).toByteArray(Charsets.UTF_8)
        val key = "TeamSecretPassphrase2026!"
        val encrypted = CryptoManager.encryptData(samplePayload, key)

        val chunks = CryptoManager.createQrChunks(
            encryptedBytes = encrypted.envelopeBytes,
            fileName = "document.pdf",
            mimeType = "application/pdf",
            originalSize = samplePayload.size.toLong(),
            originalSha256 = encrypted.sha256Original,
            transferId = "transfer123",
            targetChunkSizeBytes = 400
        )

        assertTrue(chunks.size > 1)

        val chunkMap = mutableMapOf<Int, ByteArray>()
        for (c in chunks) {
            val parsed = CryptoManager.parseQrChunk(c)
            assertNotNull(parsed)
            val decodedSlice = android.util.Base64.decode(parsed!!.payloadBase64, android.util.Base64.DEFAULT)
            chunkMap[parsed.index] = decodedSlice
        }

        val reassembled = CryptoManager.assembleChunks(chunkMap, chunks.size)
        assertNotNull(reassembled)

        val decrypted = CryptoManager.decryptData(reassembled!!, key)
        assertEquals(samplePayload.size, decrypted.size)
    }

    @Test
    fun test_safety_number_generation() {
        val hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val safetyNumber = CryptoManager.generateSafetyNumber(hash, "team_key_1")
        val blocks = safetyNumber.split(" ")
        assertEquals(6, blocks.size)
        blocks.forEach { assertEquals(5, it.length) }
    }

    @Test
    fun test_stream_timeout_notice_creation() {
        val notice = com.example.viewmodel.StreamTimeoutNotice(
            fileName = "archive.zip",
            receivedCount = 8,
            totalChunks = 15,
            timeoutSeconds = 15
        )
        assertEquals("archive.zip", notice.fileName)
        assertEquals(8, notice.receivedCount)
        assertEquals(15, notice.totalChunks)
        assertEquals(15, notice.timeoutSeconds)
    }

    @Test
    fun test_qr_chunk_progress_transfer_speed_and_formatting() {
        val now = System.currentTimeMillis()
        val chunkData1 = ByteArray(1024) { 0x41 }
        val chunkData2 = ByteArray(1024) { 0x42 }

        val progress = com.example.crypto.QrChunkProgress(
            transferId = "tid_test_99",
            fileName = "payload.bin",
            mimeType = "application/octet-stream",
            originalSize = 4096,
            originalSha256 = "abc",
            totalChunks = 4,
            receivedChunks = mutableMapOf(0 to chunkData1, 1 to chunkData2),
            firstChunkTimestamp = now - 1000L, // 1 second elapsed
            lastReceivedIndex = 1,
            lastReceivedTimestamp = now,
            corruptedCount = 0,
            duplicateCount = 0
        )

        assertEquals(2, progress.receivedCount)
        assertEquals(2048L, progress.assembledBytes)
        assertTrue(progress.estimatedSpeedBytesPerSec > 1500f)
        assertTrue(progress.formattedTransferSpeed.contains("KB/s") || progress.formattedTransferSpeed.contains("B/s"))
        assertEquals(100, progress.transferEfficiencyScore)
    }
}
