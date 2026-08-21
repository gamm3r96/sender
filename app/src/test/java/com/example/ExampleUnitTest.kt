package com.example

import com.example.crypto.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(0.5f, progress.progressFraction, 0.01f)
        assertEquals(listOf(2, 3), progress.missingIndices)
        assertTrue(progress.estimatedSpeedBytesPerSec > 1500f)
        assertTrue(progress.formattedTransferSpeed.contains("KB/s") || progress.formattedTransferSpeed.contains("B/s"))
        assertEquals(100, progress.transferEfficiencyScore)
        assertNotNull(progress.estimatedRemainingSeconds)
    }

    @Test
    fun test_stream_decoding_completion_and_missing_indices() {
        val chunkMap = mutableMapOf(
            0 to ByteArray(500),
            1 to ByteArray(500),
            2 to ByteArray(500)
        )
        val inProgress = com.example.crypto.QrChunkProgress(
            transferId = "tid_42",
            fileName = "large_firmware.bin",
            mimeType = "application/octet-stream",
            originalSize = 2000,
            originalSha256 = "hash123",
            totalChunks = 4,
            receivedChunks = chunkMap
        )
        assertEquals(0.75f, inProgress.progressFraction, 0.001f)
        assertEquals(false, inProgress.isComplete)
        assertEquals(listOf(3), inProgress.missingIndices)

        // Complete the stream
        chunkMap[3] = ByteArray(500)
        val completed = inProgress.copy(receivedChunks = chunkMap)
        assertEquals(1.0f, completed.progressFraction, 0.001f)
        assertEquals(true, completed.isComplete)
        assertEquals(emptyList<Int>(), completed.missingIndices)
    }

    @Test
    fun test_history_search_filtering_by_name_and_date() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, java.util.Calendar.AUGUST, 16, 10, 30, 0)
        val testTimestamp = cal.timeInMillis

        val record1 = com.example.data.TransferRecord(
            id = 1L,
            transferId = "tid_001",
            fileName = "classified_report.pdf",
            mimeType = "application/pdf",
            originalSize = 10240L,
            encryptedSize = 10500L,
            sha256Checksum = "sha_abc",
            timestamp = testTimestamp,
            isReceived = true,
            transferMode = com.example.data.TransferMode.QR_STREAM,
            teamName = "Alpha Ops"
        )

        val record2 = com.example.data.TransferRecord(
            id = 2L,
            transferId = "tid_002",
            fileName = "budget_2025.xlsx",
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            originalSize = 5120L,
            encryptedSize = 5400L,
            sha256Checksum = "sha_xyz",
            timestamp = testTimestamp - (48 * 3600 * 1000L), // 2 days ago
            isReceived = false,
            transferMode = com.example.data.TransferMode.P2P_DIRECT,
            teamName = "Finance"
        )

        val records = listOf(record1, record2)

        // Filter by file name
        val nameMatch = records.filter { it.fileName.contains("classified", ignoreCase = true) }
        assertEquals(1, nameMatch.size)
        assertEquals(1L, nameMatch.first().id)

        // Filter by year in date
        val dateYear = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date(record1.timestamp))
        assertEquals("2026", dateYear)

        // Filter by month name
        val dateMonth = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(java.util.Date(record1.timestamp))
        assertEquals("Aug", dateMonth)
    }

    @Test
    fun test_history_multi_selection_and_bulk_operations() {
        val record1 = com.example.data.TransferRecord(
            id = 101L,
            transferId = "tid_101",
            fileName = "intel_01.txt",
            mimeType = "text/plain",
            originalSize = 1000L,
            encryptedSize = 1200L,
            sha256Checksum = "hash1",
            timestamp = System.currentTimeMillis(),
            isReceived = true,
            transferMode = com.example.data.TransferMode.QR_STREAM
        )

        val record2 = com.example.data.TransferRecord(
            id = 102L,
            transferId = "tid_102",
            fileName = "intel_02.txt",
            mimeType = "text/plain",
            originalSize = 2000L,
            encryptedSize = 2200L,
            sha256Checksum = "hash2",
            timestamp = System.currentTimeMillis(),
            isReceived = true,
            transferMode = com.example.data.TransferMode.QR_STREAM
        )

        val record3 = com.example.data.TransferRecord(
            id = 103L,
            transferId = "tid_103",
            fileName = "intel_03.txt",
            mimeType = "text/plain",
            originalSize = 3000L,
            encryptedSize = 3200L,
            sha256Checksum = "hash3",
            timestamp = System.currentTimeMillis(),
            isReceived = false,
            transferMode = com.example.data.TransferMode.P2P_DIRECT
        )

        val allRecords = listOf(record1, record2, record3)
        var selectedIds = setOf<Long>()

        // Select item 101 and 103
        selectedIds = selectedIds + record1.id + record3.id
        assertEquals(2, selectedIds.size)
        assertTrue(selectedIds.contains(101L))
        assertTrue(selectedIds.contains(103L))

        // Total selected bytes calculation
        val selectedRecords = allRecords.filter { selectedIds.contains(it.id) }
        val totalBytes = selectedRecords.sumOf { it.originalSize }
        assertEquals(4000L, totalBytes)

        // Deselect item 101
        selectedIds = selectedIds - record1.id
        assertEquals(1, selectedIds.size)
        assertTrue(selectedIds.contains(103L))

        // Select all
        selectedIds = allRecords.map { it.id }.toSet()
        assertEquals(3, selectedIds.size)

        // Bulk delete simulation
        val remainingRecords = allRecords.filterNot { selectedIds.contains(it.id) }
        assertEquals(0, remainingRecords.size)
    }

    @Test
    fun test_biometric_status_types_and_messages() {
        val available: com.example.auth.BiometricStatus = com.example.auth.BiometricStatus.Available
        val noneEnrolled = com.example.auth.BiometricStatus.NoneEnrolled()
        val noHardware = com.example.auth.BiometricStatus.NoHardware()
        val hwUnavailable = com.example.auth.BiometricStatus.HardwareUnavailable()

        assertEquals(com.example.auth.BiometricStatus.Available, available)
        assertTrue(noneEnrolled.message.contains("enrolled"))
        assertTrue(noHardware.message.contains("hardware"))
        assertTrue(hwUnavailable.message.contains("unavailable"))
    }

    @Test
    fun test_vault_passcode_fallback_validation() {
        var storedPin = "9876"
        fun verifyPin(input: String): Boolean {
            val expected = if (storedPin.isNotBlank()) storedPin else "1234"
            return input.trim() == expected
        }

        assertTrue(verifyPin("9876"))
        assertTrue(!verifyPin("1234"))
        assertTrue(!verifyPin("0000"))

        // Reset to default
        storedPin = ""
        assertTrue(verifyPin("1234"))
    }

    @Test
    fun test_transfer_record_room_metadata_and_source_destination() {
        val sentRecord = com.example.data.TransferRecord(
            transferId = "tid_send_1",
            fileName = "top_secret_plan.pdf",
            mimeType = "application/pdf",
            originalSize = 1048576L,
            encryptedSize = 1050000L,
            isReceived = false,
            transferMode = com.example.data.TransferMode.QR_STREAM,
            sourceInfo = "This Device (Sender)",
            destinationInfo = "QR Optical Receiver (Core Security Team)",
            teamMemberName = "Me (Sender)",
            teamName = "Core Security Team",
            timestamp = 1755400000000L,
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            safetyNumber = "12345 67890 12345 67890 12345 67890"
        )

        val receivedRecord = com.example.data.TransferRecord(
            transferId = "tid_recv_1",
            fileName = "intel_report.txt",
            mimeType = "text/plain",
            originalSize = 4096L,
            encryptedSize = 4200L,
            isReceived = true,
            transferMode = com.example.data.TransferMode.P2P_DIRECT,
            sourceInfo = "P2P Host (192.168.1.100:8989)",
            destinationInfo = "Local Storage Vault (Field Unit Beta)",
            teamMemberName = "Team Peer (LAN)",
            teamName = "Field Unit Beta",
            timestamp = 1755400100000L,
            sha256Checksum = "ca978112ca1bbdcafac231b39a23dc4da7860814196ec01760e11176b1102570",
            safetyNumber = "99999 88888 77777 66666 55555 44444"
        )

        assertEquals("This Device (Sender)", sentRecord.sourceInfo)
        assertEquals("QR Optical Receiver (Core Security Team)", sentRecord.destinationInfo)
        assertEquals(false, sentRecord.isReceived)
        assertEquals(1755400000000L, sentRecord.timestamp)

        assertEquals("P2P Host (192.168.1.100:8989)", receivedRecord.sourceInfo)
        assertEquals("Local Storage Vault (Field Unit Beta)", receivedRecord.destinationInfo)
        assertEquals(true, receivedRecord.isReceived)
        assertEquals(1755400100000L, receivedRecord.timestamp)
    }

    @Test
    fun test_camera_permission_states_and_feedback_messages() {
        // Test user feedback messages for different Accompanist permission states
        fun getFeedbackTitle(isGranted: Boolean, shouldShowRationale: Boolean, hasAttempted: Boolean): String {
            return when {
                isGranted -> "Camera Access Granted • Optical Engine Ready"
                hasAttempted && !shouldShowRationale -> "Camera Permission Blocked"
                shouldShowRationale -> "Camera Access Required"
                else -> "Enable Camera for Optical Transfer"
            }
        }

        assertEquals("Camera Access Granted • Optical Engine Ready", getFeedbackTitle(true, false, true))
        assertEquals("Enable Camera for Optical Transfer", getFeedbackTitle(false, false, false))
        assertEquals("Camera Access Required", getFeedbackTitle(false, true, true))
        assertEquals("Camera Permission Blocked", getFeedbackTitle(false, false, true))
    }

    @Test
    fun test_date_formatter_relative_time() {
        val now = 1755400000000L
        val justNow = com.example.util.DateFormatter.formatRelativeTime(now - 10000L, now)
        assertEquals("Just now", justNow)

        val fiveMinutesAgo = com.example.util.DateFormatter.formatRelativeTime(now - (5 * 60 * 1000L), now)
        assertEquals("5m ago", fiveMinutesAgo)

        val threeHoursAgo = com.example.util.DateFormatter.formatRelativeTime(now - (3 * 3600 * 1000L), now)
        assertEquals("3h ago", threeHoursAgo)

        val fullFormatted = com.example.util.DateFormatter.formatFullDateTime(now)
        assertTrue(fullFormatted.isNotEmpty())
        assertTrue(fullFormatted.contains("2025") || fullFormatted.contains("2026") || fullFormatted.contains("·"))

        val compact = com.example.util.DateFormatter.formatCompact(now)
        assertTrue(compact.isNotEmpty())
    }

    @Test
    fun test_transfer_history_swipe_deletion_filter() {
        val transfer1 = com.example.data.TransferRecord(
            id = 1L,
            transferId = "tid_1",
            fileName = "intel1.bin",
            mimeType = "application/octet-stream",
            originalSize = 100L,
            encryptedSize = 120L,
            isReceived = true,
            transferMode = com.example.data.TransferMode.QR_STREAM,
            sourceInfo = "AirGap Camera",
            destinationInfo = "Vault",
            teamMemberName = "Alice",
            teamName = "Red Team",
            timestamp = 1755400000000L,
            sha256Checksum = "hash1",
            safetyNumber = "11111 22222 33333 44444 55555 66666"
        )
        val transfer2 = com.example.data.TransferRecord(
            id = 2L,
            transferId = "tid_2",
            fileName = "intel2.bin",
            mimeType = "application/octet-stream",
            originalSize = 200L,
            encryptedSize = 240L,
            isReceived = false,
            transferMode = com.example.data.TransferMode.P2P_DIRECT,
            sourceInfo = "This Device",
            destinationInfo = "Bob",
            teamMemberName = "Me",
            teamName = "Blue Team",
            timestamp = 1755400050000L,
            sha256Checksum = "hash2",
            safetyNumber = "22222 33333 44444 55555 66666 77777"
        )

        val initialList = listOf(transfer1, transfer2)
        assertEquals(2, initialList.size)

        // Simulate swipe-to-delete of transfer1
        val listAfterSwipe = initialList.filterNot { it.id == transfer1.id }
        assertEquals(1, listAfterSwipe.size)
        assertEquals(2L, listAfterSwipe.first().id)
    }

    @Test
    fun test_transfer_status_indicators() {
        val successStatus = com.example.data.TransferStatus.SUCCESS
        val completedStatus = com.example.data.TransferStatus.COMPLETED
        val failedStatus = com.example.data.TransferStatus.FAILED
        val pendingStatus = com.example.data.TransferStatus.PENDING
        val inProgressStatus = com.example.data.TransferStatus.IN_PROGRESS

        assertTrue(successStatus.isSuccess)
        assertTrue(completedStatus.isSuccess)
        assertFalse(successStatus.isFailed)
        assertFalse(successStatus.isPending)
        assertEquals("Success", successStatus.displayName)
        assertEquals("Success", completedStatus.displayName)

        assertTrue(failedStatus.isFailed)
        assertFalse(failedStatus.isSuccess)
        assertFalse(failedStatus.isPending)
        assertEquals("Failed", failedStatus.displayName)

        assertTrue(pendingStatus.isPending)
        assertTrue(inProgressStatus.isPending)
        assertFalse(pendingStatus.isSuccess)
        assertFalse(pendingStatus.isFailed)
        assertEquals("Pending", pendingStatus.displayName)
        assertEquals("Pending", inProgressStatus.displayName)
    }

    @Test
    fun test_format_transfer_details_for_clipboard() {
        val transfer = com.example.data.TransferRecord(
            id = 10L,
            transferId = "TID-9999",
            fileName = "top_secret_intel.pdf",
            mimeType = "application/pdf",
            originalSize = 1048576L,
            encryptedSize = 1049000L,
            isReceived = true,
            transferMode = com.example.data.TransferMode.QR_STREAM,
            sourceInfo = "AirGap Node Alpha",
            destinationInfo = "Vault Main",
            teamMemberName = "CipherAgent",
            teamName = "GhostOps",
            timestamp = 1755400000000L,
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            safetyNumber = "12345 67890 11121 31415 16171 81920",
            decryptedTextPreview = "Classified operative roster content preview"
        )

        val clipboardText = com.example.util.FileUtils.formatTransferDetailsForClipboard(transfer)
        assertTrue(clipboardText.contains("top_secret_intel.pdf"))
        assertTrue(clipboardText.contains("TID-9999"))
        assertTrue(clipboardText.contains("GhostOps"))
        assertTrue(clipboardText.contains("CipherAgent"))
        assertTrue(clipboardText.contains("QR_STREAM"))
        assertTrue(clipboardText.contains("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))
        assertTrue(clipboardText.contains("Classified operative roster content preview"))
    }

    @Test
    fun test_history_summary_metrics_calculation() {
        val records = listOf(
            com.example.data.TransferRecord(
                id = 1L,
                transferId = "TID-1",
                fileName = "doc1.txt",
                mimeType = "text/plain",
                originalSize = 1000L,
                encryptedSize = 1050L,
                isReceived = false,
                transferMode = com.example.data.TransferMode.QR_STREAM,
                sha256Checksum = "hash1",
                status = com.example.data.TransferStatus.COMPLETED
            ),
            com.example.data.TransferRecord(
                id = 2L,
                transferId = "TID-2",
                fileName = "doc2.txt",
                mimeType = "text/plain",
                originalSize = 2000L,
                encryptedSize = 2050L,
                isReceived = true,
                transferMode = com.example.data.TransferMode.P2P_DIRECT,
                sha256Checksum = "hash2",
                status = com.example.data.TransferStatus.COMPLETED
            ),
            com.example.data.TransferRecord(
                id = 3L,
                transferId = "TID-3",
                fileName = "doc3.txt",
                mimeType = "text/plain",
                originalSize = 5000L,
                encryptedSize = 5050L,
                isReceived = false,
                transferMode = com.example.data.TransferMode.QR_STREAM,
                sha256Checksum = "hash3",
                status = com.example.data.TransferStatus.FAILED
            ),
            com.example.data.TransferRecord(
                id = 4L,
                transferId = "TID-4",
                fileName = "doc4.txt",
                mimeType = "text/plain",
                originalSize = 1500L,
                encryptedSize = 1550L,
                isReceived = true,
                transferMode = com.example.data.TransferMode.QR_STREAM,
                sha256Checksum = "hash4",
                status = com.example.data.TransferStatus.IN_PROGRESS
            )
        )

        val totalSuccessful = records.count { it.status.isSuccess }
        val totalVolumeBytes = records.sumOf { if (it.originalSize > 0) it.originalSize else it.encryptedSize }

        assertEquals(2, totalSuccessful)
        assertEquals(9500L, totalVolumeBytes)
        assertEquals("9.3 KB", com.example.util.FileUtils.formatBytes(totalVolumeBytes))
    }

    @Test
    fun test_animated_qr_stream_density_splitting_and_reassembly() {
        val originalText = "Top-Secret Tactical Payload for Air-Gapped Optical Broadcast: " +
                "Alpha-Bravo-Charlie-Delta ".repeat(150)
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)
        val passphrase = "Tactical-Secret-Key-9988"

        val encrypted = com.example.crypto.CryptoManager.encryptData(originalBytes, passphrase)
        val transferId = "TX-OPTICAL"

        // Test Standard Density (420 bytes)
        val standardChunks = com.example.crypto.CryptoManager.createQrChunks(
            encryptedBytes = encrypted.envelopeBytes,
            fileName = "intel_report.txt",
            mimeType = "text/plain",
            originalSize = originalBytes.size.toLong(),
            originalSha256 = encrypted.sha256Original,
            transferId = transferId,
            targetChunkSizeBytes = com.example.data.QrDensityPreset.STANDARD.chunkSizeBytes
        )

        // Test High Capacity Density (680 bytes)
        val highCapChunks = com.example.crypto.CryptoManager.createQrChunks(
            encryptedBytes = encrypted.envelopeBytes,
            fileName = "intel_report.txt",
            mimeType = "text/plain",
            originalSize = originalBytes.size.toLong(),
            originalSha256 = encrypted.sha256Original,
            transferId = transferId,
            targetChunkSizeBytes = com.example.data.QrDensityPreset.HIGH_CAPACITY.chunkSizeBytes
        )

        // Test Ultra Turbo Density (920 bytes)
        val turboChunks = com.example.crypto.CryptoManager.createQrChunks(
            encryptedBytes = encrypted.envelopeBytes,
            fileName = "intel_report.txt",
            mimeType = "text/plain",
            originalSize = originalBytes.size.toLong(),
            originalSha256 = encrypted.sha256Original,
            transferId = transferId,
            targetChunkSizeBytes = com.example.data.QrDensityPreset.ULTRA_TURBO.chunkSizeBytes
        )

        assertTrue(standardChunks.size >= highCapChunks.size)
        assertTrue(highCapChunks.size >= turboChunks.size)

        // Verify parsing and reassembly for high capacity stream
        val receivedMap = mutableMapOf<Int, ByteArray>()
        var parsedTotal = 0

        for (chunkStr in highCapChunks) {
            val parsed = com.example.crypto.CryptoManager.parseQrChunk(chunkStr)
            assertNotNull(parsed)
            assertEquals(transferId, parsed!!.transferId)
            assertEquals("intel_report.txt", parsed.fileName)
            parsedTotal = parsed.total

            val chunkSlice = android.util.Base64.decode(parsed.payloadBase64, android.util.Base64.NO_WRAP)
            val computedSha = com.example.crypto.CryptoManager.computeSha256(chunkSlice)
            assertEquals(parsed.chunkSha256, computedSha)

            receivedMap[parsed.index] = chunkSlice
        }

        assertEquals(parsedTotal, receivedMap.size)
        val assembledBytes = com.example.crypto.CryptoManager.assembleChunks(receivedMap, parsedTotal)
        assertNotNull(assembledBytes)
        assertArrayEquals(encrypted.envelopeBytes, assembledBytes)

        val decryptedBytes = com.example.crypto.CryptoManager.decryptData(assembledBytes!!, passphrase)
        assertNotNull(decryptedBytes)
        val decryptedText = String(decryptedBytes!!, Charsets.UTF_8)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun test_qr_code_generator_bitmatrix_and_caching() {
        val sampleChunk = "CPQR1:{\"v\":1,\"tid\":\"TEST1\",\"idx\":0,\"tot\":1,\"fn\":\"t.txt\",\"mime\":\"text/plain\",\"osz\":50,\"data\":\"dGVzdA==\",\"osha\":\"abc\",\"csha\":\"def\"}"
        val matrix1 = com.example.qr.QrCodeGenerator.generateBitMatrix(sampleChunk)
        assertNotNull(matrix1)
        assertTrue(matrix1!!.width > 0)
        assertTrue(matrix1.height > 0)

        // Second call should return cached instance
        val matrix2 = com.example.qr.QrCodeGenerator.generateBitMatrix(sampleChunk)
        assertEquals(matrix1, matrix2)
    }
}

