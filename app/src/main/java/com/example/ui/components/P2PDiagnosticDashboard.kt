package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.p2p.HealthGrade
import com.example.p2p.P2PConnectionMetrics
import com.example.p2p.SignalQuality
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
import com.example.util.FileUtils
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-featured P2P & Wireless Signal Diagnostic Dashboard.
 */
@Composable
fun P2PDiagnosticDashboardDialog(
    metrics: P2PConnectionMetrics,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onRunPingTest: (String?) -> Unit,
    onOpenWifiSettings: () -> Unit = {},
    onOpenHotspotSettings: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("p2p_diagnostic_dashboard_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B111E),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyanBright.copy(alpha = 0.5f))
        ) {
            P2PDiagnosticDashboardContent(
                metrics = metrics,
                onClose = onDismiss,
                onRefresh = onRefresh,
                onRunPingTest = onRunPingTest,
                onOpenWifiSettings = onOpenWifiSettings,
                onOpenHotspotSettings = onOpenHotspotSettings
            )
        }
    }
}

@Composable
fun P2PDiagnosticDashboardContent(
    metrics: P2PConnectionMetrics,
    onClose: (() -> Unit)? = null,
    onRefresh: () -> Unit,
    onRunPingTest: (String?) -> Unit,
    onOpenWifiSettings: () -> Unit = {},
    onOpenHotspotSettings: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Overview, 1 = RF & Hardware, 2 = Live Oscilloscope, 3 = Ping Tool
    var customPingTarget by remember { mutableStateOf(metrics.remotePeerIp ?: metrics.gatewayIp) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberCyanBright.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Diagnostics",
                        tint = CyberCyanBright,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "P2P Health & Signal Telemetry",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Text(
                        text = if (metrics.isHotspotActive) "Hotspot AP Diagnostic Mode" else "Wi-Fi Direct RF Diagnostics",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyanBright
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("btn_refresh_diagnostics")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyberCyanBright)
                }

                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close_diagnostics")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }
            }
        }

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF131E30),
            contentColor = CyberCyanBright,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberCyanBright,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Overview", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("RF & Specs", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Waveform", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Ping Tool", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // Tab Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // OVERVIEW TAB: Gauges, Health Score, Quick Stats
                    item {
                        PrimarySignalAndHealthBanner(metrics = metrics)
                    }

                    item {
                        QuickMetricSummaryRow(metrics = metrics)
                    }

                    if (metrics.currentSpeedBytesPerSec > 0 || metrics.bytesTransferred > 0) {
                        item {
                            ActiveTransferTelemetryCard(metrics = metrics)
                        }
                    }

                    item {
                        OptimizationAdvisoryCard(metrics = metrics)
                    }

                    item {
                        NetworkControlsRow(
                            onOpenWifiSettings = onOpenWifiSettings,
                            onOpenHotspotSettings = onOpenHotspotSettings
                        )
                    }
                }
                1 -> {
                    // RF & SPECS TAB
                    item {
                        RfRadioDetailsCard(metrics = metrics)
                    }
                    item {
                        NetworkInterfaceDetailsCard(metrics = metrics)
                    }
                    item {
                        SocketBufferSpecificationsCard(metrics = metrics)
                    }
                }
                2 -> {
                    // LIVE OSCILLOSCOPE / SPARKLINE TAB
                    item {
                        ThroughputSparklineCard(metrics = metrics)
                    }
                    item {
                        LatencySparklineCard(metrics = metrics)
                    }
                }
                3 -> {
                    // PING TOOL TAB
                    item {
                        InteractivePingDiagnosticCard(
                            metrics = metrics,
                            customTarget = customPingTarget,
                            onTargetChange = { customPingTarget = it },
                            onRunPing = { onRunPingTest(customPingTarget.ifBlank { null }) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top Banner with Dual Circular Gauges: RF RSSI Signal & Connection Health Score
 */
@Composable
fun PrimarySignalAndHealthBanner(metrics: P2PConnectionMetrics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("signal_health_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(metrics.healthGrade.colorHex).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Signal Strength dBm Radial Arc Gauge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RfSignalArcGauge(
                            percentage = metrics.signalPercentage,
                            qualityColor = Color(metrics.signalQuality.colorHex)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${metrics.rssiDbm}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "dBm",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyanBright
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Signal: ${metrics.signalQuality.label}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(metrics.signalQuality.colorHex)
                    )
                    Text(
                        text = "${metrics.signalPercentage}% Intensity",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                // Right: Connection Health Score Circular Radar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ConnectionHealthRadar(
                            score = metrics.healthScore,
                            gradeColor = Color(metrics.healthGrade.colorHex)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${metrics.healthScore}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "/ 100",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(metrics.healthGrade.colorHex)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = metrics.healthGrade.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(metrics.healthGrade.colorHex)
                    )
                    Text(
                        text = "Composite Health",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary description
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0B111E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(metrics.healthGrade.colorHex),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = metrics.healthSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing an animated RF Signal Arc Gauge
 */
@Composable
fun RfSignalArcGauge(
    percentage: Int,
    qualityColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sweepAngle = 240f
        val startAngle = 150f
        val strokeWidth = 10.dp.toPx()
        val arcSize = size.minDimension - strokeWidth

        // Background track
        drawArc(
            color = Color.DarkGray.copy(alpha = 0.3f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(arcSize, arcSize)
        )

        // Filled active arc
        val activeSweep = (percentage / 100f) * sweepAngle
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    CyberCyan,
                    qualityColor,
                    qualityColor
                )
            ),
            startAngle = startAngle,
            sweepAngle = activeSweep,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(arcSize, arcSize),
            alpha = pulseAlpha
        )
    }
}

/**
 * Custom Canvas drawing a circular health radar with rotating cyber sweep
 */
@Composable
fun ConnectionHealthRadar(
    score: Int,
    gradeColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 8.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Outer progress ring
        drawCircle(
            color = Color.DarkGray.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val sweep = (score / 100f) * 360f
        drawArc(
            color = gradeColor,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(radius * 2, radius * 2)
        )

        // Concentric radar grid
        drawCircle(
            color = gradeColor.copy(alpha = 0.15f),
            radius = radius * 0.65f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Rotating radar blip line
        val angleRad = Math.toRadians(rotationAngle.toDouble())
        val blipEnd = Offset(
            (center.x + (radius * 0.65f) * cos(angleRad)).toFloat(),
            (center.y + (radius * 0.65f) * sin(angleRad)).toFloat()
        )
        drawLine(
            color = gradeColor.copy(alpha = 0.5f),
            start = center,
            end = blipEnd,
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * 4-Tile Quick Metric Summary
 */
@Composable
fun QuickMetricSummaryRow(metrics: P2PConnectionMetrics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricTileCard(
            modifier = Modifier.weight(1f),
            title = "Link Speed",
            value = "${metrics.linkSpeedMbps} Mbps",
            subtext = metrics.wifiStandard,
            icon = Icons.Default.Speed,
            accentColor = CyberCyanBright
        )

        MetricTileCard(
            modifier = Modifier.weight(1f),
            title = "Frequency",
            value = "${metrics.frequencyMhz} MHz",
            subtext = metrics.frequencyBand,
            icon = Icons.Default.CellTower,
            accentColor = CyberEmeraldBright
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricTileCard(
            modifier = Modifier.weight(1f),
            title = "RTT Ping",
            value = "${metrics.rttPingMs} ms",
            subtext = "Jitter: ±${metrics.jitterMs}ms",
            icon = Icons.Default.Timer,
            accentColor = if (metrics.rttPingMs <= 10) CyberEmeraldBright else CyberVioletBright
        )

        MetricTileCard(
            modifier = Modifier.weight(1f),
            title = "Packet Loss",
            value = "${metrics.packetLossPercent}%",
            subtext = "Zero dropped chunks",
            icon = Icons.Default.CompareArrows,
            accentColor = if (metrics.packetLossPercent == 0f) CyberEmeraldBright else Color.Red
        )
    }
}

@Composable
fun MetricTileCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White
            )

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Active Transfer Progress & Speedometer Card
 */
@Composable
fun ActiveTransferTelemetryCard(metrics: P2PConnectionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1F1C)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmeraldBright.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberEmeraldBright, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Active Transfer Stream Throughput",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyberEmeraldBright
                    )
                }

                Text(
                    text = "${FileUtils.formatBytes(metrics.currentSpeedBytesPerSec)}/s",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (metrics.totalBytesToTransfer > 0) {
                val fraction = (metrics.bytesTransferred.toFloat() / metrics.totalBytesToTransfer.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = CyberEmeraldBright
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${FileUtils.formatBytes(metrics.bytesTransferred)} / ${FileUtils.formatBytes(metrics.totalBytesToTransfer)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = "Peak: ${FileUtils.formatBytes(metrics.peakSpeedBytesPerSec)}/s",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = CyberCyanBright
                    )
                }
            }
        }
    }
}

/**
 * Smart Advisory Cards based on real RF calculations
 */
@Composable
fun OptimizationAdvisoryCard(metrics: P2PConnectionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = CyberCyanBright, modifier = Modifier.size(18.dp))
                Text(
                    text = "P2P RF Optimization Recommendations",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            metrics.diagnosticTips.forEach { tip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmeraldBright, modifier = Modifier.size(14.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

/**
 * Shortcuts to system settings
 */
@Composable
fun NetworkControlsRow(
    onOpenWifiSettings: () -> Unit,
    onOpenHotspotSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onOpenHotspotSettings,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = CyberCyanBright, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Hotspot AP", style = MaterialTheme.typography.labelMedium, color = CyberCyanBright)
        }

        OutlinedButton(
            onClick = onOpenWifiSettings,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = CyberEmeraldBright, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Wi-Fi Settings", style = MaterialTheme.typography.labelMedium, color = CyberEmeraldBright)
        }
    }
}

/**
 * RF Radio & Physical Layer Details
 */
@Composable
fun RfRadioDetailsCard(metrics: P2PConnectionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "RF & Radio Telemetry",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CyberCyanBright
            )
            Spacer(modifier = Modifier.height(10.dp))

            SpecItemRow(label = "Signal RSSI", value = "${metrics.rssiDbm} dBm (${metrics.signalQuality.label})")
            SpecItemRow(label = "Radio Frequency", value = "${metrics.frequencyMhz} MHz (${metrics.frequencyBand})")
            SpecItemRow(label = "Negotiated PHY Rate", value = "${metrics.linkSpeedMbps} Mbps")
            SpecItemRow(label = "802.11 Protocol", value = metrics.wifiStandard)
            SpecItemRow(label = "Downstream Bandwidth", value = "${metrics.downstreamBandwidthKbps / 1000} Mbps")
            SpecItemRow(label = "Upstream Bandwidth", value = "${metrics.upstreamBandwidthKbps / 1000} Mbps")
            if (metrics.bssid != null) {
                SpecItemRow(label = "Access Point BSSID", value = metrics.bssid)
            }
        }
    }
}

@Composable
fun NetworkInterfaceDetailsCard(metrics: P2PConnectionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "IP Layer & Addressing",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CyberEmeraldBright
            )
            Spacer(modifier = Modifier.height(10.dp))

            SpecItemRow(label = "Local IP Address", value = metrics.localIp)
            SpecItemRow(label = "Subnet Gateway", value = metrics.gatewayIp)
            SpecItemRow(label = "Active Remote Peer", value = metrics.remotePeerIp ?: "Listening / Multicast Radar")
            SpecItemRow(label = "TCP Transfer Port", value = "${metrics.port}")
            SpecItemRow(label = "Network Architecture", value = if (metrics.isHotspotActive) "Mobile Hotspot SoftAP" else "Wi-Fi Infrastructure P2P")
            if (metrics.ssid != null) {
                SpecItemRow(label = "Connected SSID", value = metrics.ssid)
            }
        }
    }
}

@Composable
fun SocketBufferSpecificationsCard(metrics: P2PConnectionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Socket & Cryptographic Pipelining",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CyberVioletBright
            )
            Spacer(modifier = Modifier.height(10.dp))

            SpecItemRow(label = "MTU Frame Size", value = "${metrics.mtuBytes} Bytes")
            SpecItemRow(label = "Stream Framing", value = "Chunked HTTP/1.1 with AES-GCM tags")
            SpecItemRow(label = "Zero-Cloud Encryption", value = "AES-256-GCM (128-bit MAC)")
            SpecItemRow(label = "Key Derivation", value = "PBKDF2-HMAC-SHA256 (100,000 rounds)")
        }
    }
}

@Composable
fun SpecItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
    }
}

/**
 * Real-time Rolling Throughput Sparkline Canvas
 */
@Composable
fun ThroughputSparklineCard(metrics: P2PConnectionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = CyberEmeraldBright, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Real-Time Throughput Oscilloscope",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Text(
                    text = "${FileUtils.formatBytes(metrics.currentSpeedBytesPerSec)}/s",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CyberEmeraldBright
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sparkline canvas
            SparklineWaveformCanvas(
                samples = metrics.recentSpeedSamples,
                lineColor = CyberEmeraldBright,
                fillGradientColors = listOf(
                    CyberEmeraldBright.copy(alpha = 0.4f),
                    CyberEmerald.copy(alpha = 0.05f)
                ),
                unitLabel = "MB/s"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rolling 30s Buffer Window",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "Peak: ${FileUtils.formatBytes(metrics.peakSpeedBytesPerSec)}/s",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CyberCyanBright
                )
            }
        }
    }
}

/**
 * Real-time Rolling Ping Latency Sparkline Canvas
 */
@Composable
fun LatencySparklineCard(metrics: P2PConnectionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = CyberCyanBright, modifier = Modifier.size(18.dp))
                    Text(
                        text = "RTT Latency & Jitter Oscilloscope",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Text(
                    text = "${metrics.rttPingMs} ms",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CyberCyanBright
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sparkline canvas for ping
            SparklineWaveformCanvas(
                samples = metrics.recentPingSamples,
                lineColor = CyberCyanBright,
                fillGradientColors = listOf(
                    CyberCyanBright.copy(alpha = 0.4f),
                    CyberCyan.copy(alpha = 0.05f)
                ),
                unitLabel = "ms"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${metrics.minPingMs}ms • Max: ${metrics.maxPingMs}ms",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.LightGray
                )
                Text(
                    text = "Jitter: ±${metrics.jitterMs}ms",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CyberEmeraldBright
                )
            }
        }
    }
}

/**
 * Generic Canvas drawing smoothed waveform sparklines
 */
@Composable
fun SparklineWaveformCanvas(
    samples: List<Long>,
    lineColor: Color,
    fillGradientColors: List<Color>,
    unitLabel: String
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color(0xFF090E17), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (samples.isEmpty()) return@Canvas

        val maxVal = maxOf(1L, samples.maxOrNull() ?: 1L).toFloat()
        val widthStep = size.width / (samples.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        samples.forEachIndexed { index, sample ->
            val x = index * widthStep
            val normalizedY = 1f - (sample.toFloat() / maxVal)
            val y = normalizedY * (size.height - 12.dp.toPx()) + 6.dp.toPx()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        // Draw gradient fill under sparkline
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(fillGradientColors)
        )

        // Draw stroke line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw active pulse point on last sample
        val lastSample = samples.last()
        val lastX = (samples.size - 1) * widthStep
        val lastNormY = 1f - (lastSample.toFloat() / maxVal)
        val lastY = lastNormY * (size.height - 12.dp.toPx()) + 6.dp.toPx()

        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

/**
 * Interactive Ping Diagnostic Tool Card
 */
@Composable
fun InteractivePingDiagnosticCard(
    metrics: P2PConnectionMetrics,
    customTarget: String,
    onTargetChange: (String) -> Unit,
    onRunPing: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyanBright.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = CyberCyanBright, modifier = Modifier.size(20.dp))
                Text(
                    text = "Live ICMP / TCP Socket Ping Probe",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Measure instantaneous round-trip socket handshakes to any LAN peer or gateway.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customTarget,
                onValueChange = onTargetChange,
                label = { Text("Target Peer or Gateway IP") },
                placeholder = { Text("192.168.43.1") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyanBright,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_ping_target_ip")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onRunPing,
                enabled = !metrics.isActivelyTestingPing && customTarget.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_run_ping_diagnostic"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                if (metrics.isActivelyTestingPing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probing $customTarget...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Execute Ping Probe", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (metrics.pingTestResultText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF090E17),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmeraldBright, modifier = Modifier.size(16.dp))
                        Text(
                            text = metrics.pingTestResultText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CyberEmeraldBright
                        )
                    }
                }
            }
        }
    }
}
