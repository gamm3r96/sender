package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.QrChunkProgress
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberVioletBright
import com.example.util.FileUtils

/**
 * Animated High-Tech QR Stream Decoding Progress Bar.
 * Visualizes real-time frame buffering, decoding throughput, SHA-256 validation,
 * smooth spring progress progression, dynamic sweep shimmer, and ETA countdown.
 */
@Composable
fun AnimatedStreamProgressBar(
    scanProgress: QrChunkProgress,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    showDetailedMetrics: Boolean = true,
    showBufferStrip: Boolean = true
) {
    // Smooth progress interpolation for seamless transitions between discrete chunk updates
    val animatedFraction by animateFloatAsState(
        targetValue = scanProgress.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "animated_stream_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "stream_decoder_effects")

    // Moving laser shimmer sweep along the progress bar
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    // Pulsing glowing dot at the leading head of the progress track
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Frame ingestion pulse on newly captured frames
    val isComplete = scanProgress.isComplete
    val percentage = (animatedFraction * 100).toInt().coerceIn(0, 100)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animated_stream_progress_bar"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showDetailedMetrics) {
            // Header row: Status Indicator + Live Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge with active glowing dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isComplete) CyberEmeraldBright
                                else CyberCyanBright.copy(alpha = pulseAlpha)
                            )
                    )

                    Text(
                        text = if (isComplete) "DECODING COMPLETE ✓" else "DECODING QR STREAM...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isComplete) CyberEmeraldBright else CyberCyanBright,
                        modifier = Modifier.testTag("stream_decoding_status_text")
                    )
                }

                // High-visibility percentage badge
                Surface(
                    color = if (isComplete) CyberEmerald.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isComplete) CyberEmeraldBright.copy(alpha = 0.7f) else CyberCyanBright.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        color = if (isComplete) CyberEmeraldBright else CyberCyanBright,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("stream_progress_percentage_badge")
                    )
                }
            }
        }

        // The Custom Animated Progress Bar Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Color(0xFF0F172A))
                .border(
                    width = 1.dp,
                    color = if (isComplete) CyberEmerald.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(height / 2)
                )
                .testTag("stream_progress_bar_canvas")
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val barHeight = size.height
                val progressWidth = (width * animatedFraction).coerceAtMost(width)

                if (progressWidth > 0f) {
                    // Base multi-stop gradient fill (Cyan -> Emerald -> Emerald Bright)
                    val baseGradient = Brush.horizontalGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.85f),
                            CyberCyanBright,
                            CyberEmeraldBright
                        ),
                        startX = 0f,
                        endX = progressWidth.coerceAtLeast(1f)
                    )

                    // Draw main filled progress bar
                    drawRoundRect(
                        brush = baseGradient,
                        size = Size(progressWidth, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                    )

                    // Draw traveling laser shimmer highlight across the active portion
                    if (!isComplete) {
                        val shimmerStart = (progressWidth * shimmerOffset) - 60f
                        val shimmerEnd = shimmerStart + 120f
                        val shimmerBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.65f),
                                Color.Transparent
                            ),
                            startX = shimmerStart,
                            endX = shimmerEnd
                        )

                        drawRoundRect(
                            brush = shimmerBrush,
                            topLeft = Offset(0f, 0f),
                            size = Size(progressWidth, barHeight),
                            cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                        )
                    }

                    // Draw leading edge glowing head dot
                    if (progressWidth < width && !isComplete) {
                        drawCircle(
                            color = CyberEmeraldBright.copy(alpha = pulseAlpha),
                            radius = (barHeight / 2) + 1.5f,
                            center = Offset(progressWidth, barHeight / 2)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = (barHeight / 4) + 0.5f,
                            center = Offset(progressWidth, barHeight / 2)
                        )
                    }
                }

                // Draw discrete chunk tick marks if total chunks is reasonable (<= 50)
                val totalChunks = scanProgress.totalChunks
                if (totalChunks in 2..50) {
                    val stepWidth = width / totalChunks
                    for (i in 1 until totalChunks) {
                        val tickX = i * stepWidth
                        drawLine(
                            color = Color(0xFF0B1120).copy(alpha = 0.7f),
                            start = Offset(tickX, 0f),
                            end = Offset(tickX, barHeight),
                            strokeWidth = 1.2f
                        )
                    }
                }
            }
        }

        if (showDetailedMetrics) {
            // Secondary metrics row: Chunk count, transferred bytes, and throughput rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chunk count & Assembled size
                Text(
                    text = "${scanProgress.receivedCount}/${scanProgress.totalChunks} Chunks • ${FileUtils.formatBytes(scanProgress.assembledBytes)} / ${FileUtils.formatBytes(scanProgress.originalSize)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Transfer speed & throughput FPS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Decoding Rate",
                        tint = CyberEmeraldBright,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = scanProgress.formattedTransferSpeed,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = CyberEmeraldBright
                    )

                    val fps = scanProgress.estimatedSpeedChunksPerSec
                    if (fps > 0f) {
                        Text(
                            text = "(%.1f FPS)".format(fps),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = CyberCyanBright
                        )
                    }
                }
            }

            // ETA and Expectation Management Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Integrity & Efficiency Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (scanProgress.corruptedCount == 0) Icons.Default.Shield else Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = if (scanProgress.corruptedCount == 0) CyberEmeraldBright else Color(0xFFFBBF24),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (scanProgress.corruptedCount == 0) "SHA-256 Validated ✓" else "${scanProgress.corruptedCount} Retries Handled",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = if (scanProgress.corruptedCount == 0) CyberEmeraldBright else Color(0xFFFBBF24)
                    )
                }

                // Estimated Time Remaining (ETA)
                val eta = scanProgress.estimatedRemainingSeconds
                if (eta != null && !isComplete) {
                    Surface(
                        color = CyberCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ETA: ~${eta}s remaining",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CyberCyanBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isComplete) {
                    Text(
                        text = "Assembly Ready",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CyberEmeraldBright
                    )
                }
            }

            // Real-Time Frame Validation Message / Missing Frames Alert
            if (scanProgress.missingIndices.isNotEmpty() && !isComplete && scanProgress.receivedCount > 0) {
                val missingText = "Waiting for stream frames: " + scanProgress.missingIndices.take(5).map { "#${it + 1}" }.joinToString(", ") +
                        if (scanProgress.missingIndices.size > 5) " (+${scanProgress.missingIndices.size - 5} more)" else ""
                
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = missingText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = CyberCyanBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Optional Micro Buffer Strip: Visual segment distribution for large transfers
        if (showBufferStrip && scanProgress.totalChunks in 4..64) {
            StreamChunkBufferStrip(
                scanProgress = scanProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}

/**
 * Micro Buffer Strip showing real-time distribution of buffered vs missing chunks
 */
@Composable
fun StreamChunkBufferStrip(
    scanProgress: QrChunkProgress,
    modifier: Modifier = Modifier
) {
    val total = scanProgress.totalChunks.coerceAtMost(64)
    Row(
        modifier = modifier
            .height(5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.5.dp))
            .background(Color(0xFF0F172A)),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (i in 0 until total) {
            val isReceived = scanProgress.receivedChunks.containsKey(i)
            val isLatest = scanProgress.lastReceivedIndex == i

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(
                        when {
                            isLatest -> CyberCyanBright
                            isReceived -> CyberEmerald.copy(alpha = 0.85f)
                            else -> Color(0xFF1E293B)
                        }
                    )
            )
        }
    }
}

/**
 * Compact Animated Stream Progress Bar for floating reticle HUD or tight camera views
 */
@Composable
fun CompactStreamProgressBar(
    scanProgress: QrChunkProgress,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    val animatedFraction by animateFloatAsState(
        targetValue = scanProgress.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "compact_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "compact_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "compact_shimmer_offset"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compact_pulse"
    )

    val isComplete = scanProgress.isComplete

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Color(0xFF0F172A))
            .border(
                width = 0.8.dp,
                color = if (isComplete) CyberEmerald.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.4f),
                shape = RoundedCornerShape(height / 2)
            )
            .testTag("compact_stream_progress_bar")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val barHeight = size.height
            val progressWidth = (width * animatedFraction).coerceAtMost(width)

            if (progressWidth > 0f) {
                val baseGradient = Brush.horizontalGradient(
                    colors = listOf(
                        CyberCyanBright,
                        CyberEmeraldBright
                    ),
                    startX = 0f,
                    endX = progressWidth.coerceAtLeast(1f)
                )

                drawRoundRect(
                    brush = baseGradient,
                    size = Size(progressWidth, barHeight),
                    cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                )

                if (!isComplete) {
                    val shimmerStart = (progressWidth * shimmerOffset) - 40f
                    val shimmerEnd = shimmerStart + 80f
                    val shimmerBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        startX = shimmerStart,
                        endX = shimmerEnd
                    )

                    drawRoundRect(
                        brush = shimmerBrush,
                        topLeft = Offset(0f, 0f),
                        size = Size(progressWidth, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                    )
                }

                if (progressWidth < width && !isComplete) {
                    drawCircle(
                        color = CyberEmeraldBright.copy(alpha = pulseAlpha),
                        radius = (barHeight / 2) + 1f,
                        center = Offset(progressWidth, barHeight / 2)
                    )
                }
            }
        }
    }
}
