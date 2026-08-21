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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
import com.example.util.FileUtils

/**
 * Visual Progress Bar Component that tracks the current chunk index being scanned or transmitted over P2P.
 * Visualizes real-time active chunk indices, smooth interpolation, traveling shimmer beam,
 * segmented chunk block matrix, transmission throughput, and ETA calculation.
 */
@Composable
fun ChunkTransferProgressBar(
    currentChunkIndex: Int,
    totalChunks: Int,
    modifier: Modifier = Modifier,
    bytesTransferred: Long = 0L,
    totalBytes: Long = 0L,
    speedBytesPerSec: Long = 0L,
    isTransmitting: Boolean = true,
    isScanning: Boolean = false,
    statusTitle: String? = null,
    chunkSizeBytes: Long = 32 * 1024L,
    showSegmentGrid: Boolean = true,
    showDetailedMetrics: Boolean = true,
    height: Dp = 10.dp,
    customAccentColor: Color? = null
) {
    val safeTotal = totalChunks.coerceAtLeast(1)
    val safeIndex = currentChunkIndex.coerceIn(0, safeTotal - 1)
    val isComplete = (safeIndex + 1 >= safeTotal && (totalBytes <= 0 || bytesTransferred >= totalBytes)) || 
            (totalBytes > 0 && bytesTransferred >= totalBytes)

    val progressFraction = if (totalBytes > 0) {
        (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        ((safeIndex + 1).toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    }

    // Smooth progress interpolation
    val animatedFraction by animateFloatAsState(
        targetValue = if (isComplete) 1f else progressFraction,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "chunk_animated_fraction"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "chunk_transfer_effects")

    // Laser shimmer traveling across the active progress width
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chunk_shimmer_offset"
    )

    // Pulsing head glow for the active chunk being processed
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chunk_pulse_alpha"
    )

    var isGridExpanded by remember { mutableStateOf(false) }

    val percentage = (animatedFraction * 100).toInt().coerceIn(0, 100)
    val primaryColor = customAccentColor ?: if (isTransmitting) CyberCyanBright else CyberEmeraldBright
    val secondaryColor = if (isTransmitting) CyberCyan else CyberEmerald

    // Calculate Estimated Time Remaining (ETA)
    val etaSeconds: Long? = remember(bytesTransferred, totalBytes, speedBytesPerSec, currentChunkIndex, safeTotal) {
        if (isComplete) return@remember null
        if (speedBytesPerSec > 0 && totalBytes > bytesTransferred) {
            val remainingBytes = totalBytes - bytesTransferred
            (remainingBytes / speedBytesPerSec).coerceAtLeast(1)
        } else if (speedBytesPerSec > 0 && totalBytes == 0L) {
            val remainingChunks = (safeTotal - (safeIndex + 1)).coerceAtLeast(0)
            val estRemainingBytes = remainingChunks * chunkSizeBytes
            (estRemainingBytes / speedBytesPerSec).coerceAtLeast(1)
        } else {
            null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chunk_transfer_progress_bar"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header: Live Chunk Index Tracker + Percentage Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Status Title with pulsating active indicator
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
                            else primaryColor.copy(alpha = pulseAlpha)
                        )
                )

                val defaultTitle = when {
                    isComplete -> "TRANSFER COMPLETE ✓"
                    isScanning -> "SCANNED CHUNK #${safeIndex + 1} OF $safeTotal"
                    isTransmitting -> "TRANSMITTING CHUNK #${safeIndex + 1} OF $safeTotal"
                    else -> "RECEIVING CHUNK #${safeIndex + 1} OF $safeTotal"
                }

                Text(
                    text = statusTitle ?: defaultTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isComplete) CyberEmeraldBright else primaryColor,
                    modifier = Modifier.testTag("chunk_status_header_text")
                )
            }

            // Right: High-Visibility Chunk Badge & Percentage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Chunk Index Pill
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "CHUNK ${safeIndex + 1}/$safeTotal",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = primaryColor,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("chunk_index_badge")
                    )
                }

                // Percentage Badge
                Surface(
                    color = if (isComplete) CyberEmerald.copy(alpha = 0.25f) else secondaryColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isComplete) CyberEmeraldBright.copy(alpha = 0.8f) else primaryColor.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = if (isComplete) CyberEmeraldBright else primaryColor,
                        modifier = Modifier
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                            .testTag("chunk_percentage_badge")
                    )
                }
            }
        }

        // 2. The Custom Chunk Track Canvas with Laser Shimmer & Chunk Boundaries
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Color(0xFF0B1120))
                .border(
                    width = 1.dp,
                    color = if (isComplete) CyberEmerald.copy(alpha = 0.6f) else primaryColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(height / 2)
                )
                .testTag("chunk_progress_canvas")
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val barHeight = size.height
                val progressWidth = (width * animatedFraction).coerceAtMost(width)

                if (progressWidth > 0f) {
                    // Base multi-stop gradient fill
                    val baseGradient = Brush.horizontalGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.85f),
                            primaryColor,
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

                    // Draw traveling laser shimmer highlight
                    if (!isComplete) {
                        val shimmerStart = (progressWidth * shimmerOffset) - 60f
                        val shimmerEnd = shimmerStart + 120f
                        val shimmerBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.95f),
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

                    // Draw leading edge glowing head dot for the active chunk
                    if (progressWidth < width && !isComplete) {
                        drawCircle(
                            color = primaryColor.copy(alpha = pulseAlpha),
                            radius = (barHeight / 2) + 2f,
                            center = Offset(progressWidth, barHeight / 2)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = (barHeight / 4) + 0.5f,
                            center = Offset(progressWidth, barHeight / 2)
                        )
                    }
                }

                // Draw discrete chunk division ticks when chunk count is reasonable (2..48)
                if (safeTotal in 2..48) {
                    val stepWidth = width / safeTotal
                    for (i in 1 until safeTotal) {
                        val tickX = i * stepWidth
                        drawLine(
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            start = Offset(tickX, 0f),
                            end = Offset(tickX, barHeight),
                            strokeWidth = 1.2f
                        )
                    }
                }
            }
        }

        // 3. Segmented Chunk Block Matrix / Micro Strip
        if (showSegmentGrid && safeTotal in 2..64) {
            SegmentedChunkStrip(
                currentChunkIndex = safeIndex,
                totalChunks = safeTotal,
                isComplete = isComplete,
                isTransmitting = isTransmitting,
                pulseAlpha = pulseAlpha,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }

        // 4. Telemetry Details: Bytes, Speed, Chunk Size, and ETA
        if (showDetailedMetrics) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Byte count & chunk sizing
                val byteInfo = if (totalBytes > 0) {
                    "${FileUtils.formatBytes(bytesTransferred)} / ${FileUtils.formatBytes(totalBytes)}"
                } else {
                    "${FileUtils.formatBytes((safeIndex + 1) * chunkSizeBytes)} / ~${FileUtils.formatBytes(safeTotal * chunkSizeBytes)}"
                }

                Text(
                    text = "$byteInfo • ${FileUtils.formatBytes(chunkSizeBytes)}/blk",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Throughput Speed & ETA
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (speedBytesPerSec > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Speed",
                                tint = primaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${FileUtils.formatBytes(speedBytesPerSec)}/s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = primaryColor
                            )
                        }
                    }

                    if (etaSeconds != null && !isComplete) {
                        Surface(
                            color = primaryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ETA: ~${etaSeconds}s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isComplete) {
                        Text(
                            text = "Verified ✓",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
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

/**
 * Segmented chunk micro-strip showing each chunk block:
 * - Green/Emerald: Completed / Transmitted chunks
 * - Cyan/Pulsing Glow: Current Chunk being scanned or transmitted
 * - Dark Slate: Pending chunks in buffer
 */
@Composable
fun SegmentedChunkStrip(
    currentChunkIndex: Int,
    totalChunks: Int,
    isComplete: Boolean,
    isTransmitting: Boolean,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val count = totalChunks.coerceAtMost(64)
    Row(
        modifier = modifier
            .height(6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0F172A)),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (i in 0 until count) {
            val isCurrent = i == currentChunkIndex && !isComplete
            val isPast = i < currentChunkIndex || isComplete

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        when {
                            isCurrent -> CyberCyanBright.copy(alpha = pulseAlpha)
                            isPast -> CyberEmerald.copy(alpha = 0.85f)
                            else -> Color(0xFF1E293B)
                        }
                    )
            )
        }
    }
}

/**
 * Specialized P2P Direct Chunk Progress Bar for Wi-Fi / Hotspot local streaming.
 */
@Composable
fun P2PChunkProgressBar(
    currentChunkIndex: Int,
    totalChunks: Int,
    bytesTransferred: Long,
    totalBytes: Long,
    speedBytesPerSec: Long,
    modifier: Modifier = Modifier,
    isSending: Boolean = true,
    clientOrHostInfo: String? = null,
    chunkSizeBytes: Long = 32 * 1024L
) {
    val safeTotal = totalChunks.coerceAtLeast(1)
    val safeIndex = currentChunkIndex.coerceIn(0, safeTotal - 1)
    val actionText = if (isSending) "P2P TRANSMITTING" else "P2P RECEIVING"
    val fullTitle = if (clientOrHostInfo != null) "$actionText • $clientOrHostInfo" else "$actionText [CHUNK #${safeIndex + 1}/$safeTotal]"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B1424),
        border = BorderStroke(1.dp, if (isSending) CyberCyan.copy(alpha = 0.4f) else CyberEmerald.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        ChunkTransferProgressBar(
            currentChunkIndex = safeIndex,
            totalChunks = safeTotal,
            bytesTransferred = bytesTransferred,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            isTransmitting = isSending,
            statusTitle = fullTitle,
            chunkSizeBytes = chunkSizeBytes,
            showSegmentGrid = safeTotal in 2..48,
            showDetailedMetrics = true,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Compact Chunk Progress Bar for tight cards, floating camera reticle, and modal sheets.
 */
@Composable
fun CompactChunkProgressBar(
    currentChunkIndex: Int,
    totalChunks: Int,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    isScanning: Boolean = false,
    height: Dp = 6.dp
) {
    val safeTotal = totalChunks.coerceAtLeast(1)
    val safeIndex = currentChunkIndex.coerceIn(0, safeTotal - 1)
    val isComplete = progressFraction >= 1f || safeIndex + 1 >= safeTotal

    val animatedFraction by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "compact_chunk_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "compact_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compact_chunk_pulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isScanning) "SCANNING CHUNK #${safeIndex + 1}/$safeTotal" else "CHUNK #${safeIndex + 1}/$safeTotal",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = if (isComplete) CyberEmeraldBright else CyberCyanBright
            )

            Text(
                text = "${(animatedFraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = if (isComplete) CyberEmeraldBright else CyberCyanBright
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Color(0xFF0F172A))
                .border(
                    width = 0.8.dp,
                    color = if (isComplete) CyberEmerald.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(height / 2)
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val barHeight = size.height
                val progressWidth = (width * animatedFraction).coerceAtMost(width)

                if (progressWidth > 0f) {
                    val baseGradient = Brush.horizontalGradient(
                        colors = listOf(
                            CyberCyan,
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
                        drawCircle(
                            color = CyberEmeraldBright.copy(alpha = pulseAlpha),
                            radius = (barHeight / 2) + 1.2f,
                            center = Offset(progressWidth, barHeight / 2)
                        )
                    }
                }
            }
        }
    }
}
