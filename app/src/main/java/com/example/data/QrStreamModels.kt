package com.example.data

enum class QrDensityPreset(
    val title: String,
    val chunkSizeBytes: Int,
    val description: String,
    val badgeLabel: String
) {
    STANDARD(
        title = "Standard",
        chunkSizeBytes = 420,
        description = "High compatibility for standard cameras & low-light conditions",
        badgeLabel = "420 B/Frame"
    ),
    HIGH_CAPACITY(
        title = "High Capacity",
        chunkSizeBytes = 680,
        description = "Balanced density for fast optical air-gap transfers",
        badgeLabel = "680 B/Frame"
    ),
    ULTRA_TURBO(
        title = "Ultra Turbo",
        chunkSizeBytes = 920,
        description = "Maximum throughput with minimal frame count for HD sensors",
        badgeLabel = "920 B/Frame"
    )
}

data class QrStreamStatistics(
    val totalChunks: Int,
    val currentChunkIndex: Int,
    val fps: Int,
    val densityPreset: QrDensityPreset,
    val originalSizeBytes: Long,
    val encryptedSizeBytes: Long,
    val loopCount: Int,
    val estimatedLoopDurationSec: Float,
    val estimatedBandwidthBytesPerSec: Long,
    val currentChunkChecksum: String = ""
)
