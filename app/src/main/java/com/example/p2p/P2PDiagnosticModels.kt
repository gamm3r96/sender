package com.example.p2p

enum class SignalQuality(val label: String, val colorHex: Long) {
    EXCELLENT("Excellent", 0xFF10B981),
    GOOD("Good", 0xFF00E5FF),
    FAIR("Fair", 0xFFF59E0B),
    POOR("Poor", 0xFFEF4444),
    CRITICAL("Critical / Lost", 0xFF991B1B)
}

enum class HealthGrade(val label: String, val colorHex: Long) {
    OPTIMAL("Optimal Link", 0xFF10B981),
    EXCELLENT("High Performance", 0xFF00E5FF),
    STABLE("Stable Connection", 0xFF3B82F6),
    DEGRADED("Degraded Link", 0xFFF59E0B),
    CRITICAL("Unstable / Dropped", 0xFFEF4444)
}

data class P2PConnectionMetrics(
    val rssiDbm: Int = -50,
    val signalPercentage: Int = 85,
    val signalQuality: SignalQuality = SignalQuality.EXCELLENT,
    val linkSpeedMbps: Int = 433,
    val downstreamBandwidthKbps: Int = 100000,
    val upstreamBandwidthKbps: Int = 100000,
    val frequencyMhz: Int = 5180,
    val frequencyBand: String = "5.0 GHz (Fast P2P)",
    val wifiStandard: String = "Wi-Fi 5 / 6",
    val bssid: String? = null,
    val gatewayIp: String = "192.168.43.1",
    val localIp: String = "192.168.43.100",
    val remotePeerIp: String? = null,
    val port: Int = 8989,
    val isHotspotActive: Boolean = false,
    val ssid: String? = null,
    val rttPingMs: Long = 3L,
    val jitterMs: Long = 1L,
    val minPingMs: Long = 2L,
    val maxPingMs: Long = 8L,
    val packetLossPercent: Float = 0.0f,
    val healthScore: Int = 94,
    val healthGrade: HealthGrade = HealthGrade.OPTIMAL,
    val healthSummary: String = "High-speed 5GHz P2P link with ultra-low latency.",
    val diagnosticTips: List<String> = listOf(
        "Optimal 5.0 GHz RF channel in use",
        "Sub-5ms RTT latency is ideal for high-throughput binary bursts",
        "Zero packet retransmissions detected"
    ),
    val recentSpeedSamples: List<Long> = emptyList(),
    val recentPingSamples: List<Long> = emptyList(),
    val currentSpeedBytesPerSec: Long = 0L,
    val peakSpeedBytesPerSec: Long = 0L,
    val averageSpeedBytesPerSec: Long = 0L,
    val bytesTransferred: Long = 0L,
    val totalBytesToTransfer: Long = 0L,
    val mtuBytes: Int = 1500,
    val isActivelyTestingPing: Boolean = false,
    val pingTestResultText: String? = null
)
