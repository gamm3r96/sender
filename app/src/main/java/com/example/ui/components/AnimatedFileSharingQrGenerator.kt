package com.example.ui.components

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.crypto.CryptoManager
import com.example.data.QrColorScheme
import com.example.data.QrDensityPreset
import com.example.data.QrErrorCorrectionLevel
import com.example.data.QrModuleShape
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
import com.example.util.FileUtils
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * AnimatedFileSharingQrGenerator:
 * A specialized, high-fidelity UI component that sequences and displays animated QR code frames
 * for sharing files over an optical air-gapped stream, featuring rich visual progress indicators,
 * real-time frame buffering matrices, transmission bitrate gauges, loop cycle counters,
 * and comprehensive optical tuning controls.
 */
@Composable
fun AnimatedFileSharingQrGenerator(
    chunks: List<String>,
    fileName: String,
    fileSizeBytes: Long,
    modifier: Modifier = Modifier,
    initialFps: Int = 8,
    initialDensityPreset: QrDensityPreset = QrDensityPreset.HIGH_CAPACITY,
    initialColorScheme: QrColorScheme = QrColorScheme.HIGH_CONTRAST_MONO,
    initialErrorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.LEVEL_M,
    initialModuleShape: QrModuleShape = QrModuleShape.SQUARE,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current
    val totalChunks = chunks.size.coerceAtLeast(1)

    var currentChunkIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var streamFps by remember { mutableIntStateOf(initialFps.coerceIn(1, 30)) }
    var densityPreset by remember { mutableStateOf(initialDensityPreset) }
    var colorScheme by remember { mutableStateOf(initialColorScheme) }
    var errorCorrection by remember { mutableStateOf(initialErrorCorrection) }
    var moduleShape by remember { mutableStateOf(initialModuleShape) }
    var isQrInverted by remember { mutableStateOf(false) }
    var loopCount by remember { mutableIntStateOf(1) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showOpticalSettings by remember { mutableStateOf(false) }

    // Automatic Stream Animation Loop
    LaunchedEffect(isPlaying, streamFps, totalChunks) {
        if (!isPlaying || totalChunks <= 1) return@LaunchedEffect
        val intervalMs = (1000L / streamFps.coerceAtLeast(1)).coerceAtLeast(30L)
        while (isPlaying) {
            delay(intervalMs)
            val nextIndex = currentChunkIndex + 1
            if (nextIndex >= totalChunks) {
                currentChunkIndex = 0
                loopCount++
            } else {
                currentChunkIndex = nextIndex
            }
        }
    }

    val safeIndex = currentChunkIndex.coerceIn(0, totalChunks - 1)
    val currentQrData = chunks.getOrNull(safeIndex) ?: ""

    // Progress fractions
    val progressFraction = (safeIndex + 1).toFloat() / totalChunks.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = (1000 / streamFps).coerceIn(40, 300), easing = LinearEasing),
        label = "stream_progress"
    )

    // Bandwidth calculation (estimated payload transfer rate)
    val chunkEstimatedSize = remember(currentQrData) { currentQrData.toByteArray(Charsets.UTF_8).size }
    val transmissionBandwidthBytesPerSec = chunkEstimatedSize.toLong() * streamFps

    // Calculate time remaining in current loop
    val framesRemainingInLoop = totalChunks - (safeIndex + 1)
    val secondsRemainingInLoop = (framesRemainingInLoop.toFloat() / streamFps.coerceAtLeast(1).toFloat()).coerceAtLeast(0f)

    // Infinite ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "sharing_ambient")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val laserSweep by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_sweep"
    )

    // Horizontal frame scrubber list state
    val listState = rememberLazyListState()
    LaunchedEffect(safeIndex) {
        if (totalChunks > 1) {
            listState.animateScrollToItem(safeIndex)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animated_file_sharing_qr_generator"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    CyberCyanBright.copy(alpha = if (isPlaying) pulseGlow else 0.4f),
                    CyberEmeraldBright.copy(alpha = if (isPlaying) pulseGlow else 0.4f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: Stream Identity, Active State & Loop Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Air-Gap Stream",
                            tint = CyberEmeraldBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${FileUtils.formatBytes(fileSizeBytes)} • $totalChunks Frames",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Loop Counter Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Loop Counter",
                                tint = CyberCyanBright,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Loop #$loopCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = CyberCyanBright
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Optical settings toggle
                    IconButton(
                        onClick = { showOpticalSettings = !showOpticalSettings },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("stream_optical_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tune Optical Stream",
                            tint = if (showOpticalSettings) CyberCyanBright else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Optical Customization Panel (Expandable)
            AnimatedVisibility(
                visible = showOpticalSettings,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "OPTICAL TRANSMISSION TUNING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = CyberCyanBright
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed / FPS Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Frame Rate: ${streamFps} FPS",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(1000f / streamFps).roundToInt()} ms/frame",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = CyberEmeraldBright
                        )
                    }
                    Slider(
                        value = streamFps.toFloat(),
                        onValueChange = { streamFps = it.roundToInt() },
                        valueRange = 2f..24f,
                        steps = 21,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyanBright,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Error Correction Level selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ECC:",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(42.dp)
                        )
                        QrErrorCorrectionLevel.entries.forEach { level ->
                            FilterChip(
                                selected = errorCorrection == level,
                                onClick = { errorCorrection = level },
                                label = { Text(level.badgeLabel, fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberEmerald.copy(alpha = 0.25f),
                                    selectedLabelColor = CyberEmeraldBright
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Animated QR Code Stage with Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isQrInverted) colorScheme.composeDarkColor else colorScheme.composeLightColor)
                    .border(
                        2.dp,
                        if (isPlaying) CyberEmerald.copy(alpha = pulseGlow) else CyberCyan.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(12.dp)
                    .clickable { isFullScreen = true }
                    .testTag("animated_qr_stage"),
                contentAlignment = Alignment.Center
            ) {
                // QR Display
                QrCodeView(
                    qrContent = currentQrData,
                    sizePx = 700,
                    colorScheme = colorScheme,
                    errorCorrectionLevel = errorCorrection,
                    moduleShape = moduleShape,
                    isInverted = isQrInverted,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Floating Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "FRAME ${safeIndex + 1}/$totalChunks",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = CyberEmeraldBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { isQrInverted = !isQrInverted },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Invert Contrast",
                                tint = if (isQrInverted) CyberAmber else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = { isFullScreen = true },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Bottom HUD: Circular & Linear Transmission Speed Indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) CyberEmeraldBright else CyberAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "${streamFps} FPS • ${((safeIndex + 1).toFloat() / totalChunks * 100).toInt()}%" else "STREAM PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp
                            ),
                            color = if (isPlaying) CyberCyanBright else CyberAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // VISUAL PROGRESS INDICATORS FOR STREAM TRANSMISSION
            // ==========================================

            // 1. Primary Linear Stream Transmission Progress Bar with Laser Sweep
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = CyberCyanBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STREAM CYCLE PROGRESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = CyberCyanBright
                        )
                    }

                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        ),
                        color = CyberEmeraldBright
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // High-Tech Animated Stream Progress Bar Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF0D1527))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val progressWidth = (w * animatedProgress).coerceIn(0f, w)

                        // Base track gradient
                        if (progressWidth > 0f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    listOf(CyberCyan, CyberEmeraldBright),
                                    0f,
                                    progressWidth
                                ),
                                size = Size(progressWidth, h),
                                cornerRadius = CornerRadius(h / 2, h / 2)
                            )
                        }

                        // Shimmer beam sweep
                        if (isPlaying && progressWidth > 10f) {
                            val shimmerStartX = (progressWidth * laserSweep).coerceIn(-50f, progressWidth + 50f)
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.7f),
                                        Color.Transparent
                                    ),
                                    shimmerStartX,
                                    shimmerStartX + 60f
                                ),
                                size = Size(progressWidth, h),
                                cornerRadius = CornerRadius(h / 2, h / 2)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Telemetry Metrics Row: Bandwidth, Frame ETA, Active Loop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Throughput: ~${FileUtils.formatBytes(transmissionBandwidthBytesPerSec)}/s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (isPlaying && secondsRemainingInLoop > 0) "Loop ETA: ${String.format("%.1f", secondsRemainingInLoop)}s" else "Loop Synchronized",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.5.sp
                        ),
                        color = CyberCyanBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Segmented Frame Matrix / Buffer Block Grid (Visualizing discrete frames)
            if (totalChunks > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FRAME BUFFER MATRIX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Frame ${safeIndex + 1} of $totalChunks",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = CyberCyanBright
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(chunks) { index, _ ->
                            val isCurrent = index == safeIndex
                            val isPast = index < safeIndex

                            Box(
                                modifier = Modifier
                                    .size(width = 20.dp, height = 14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isCurrent -> CyberEmeraldBright
                                            isPast -> CyberCyan.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) Color.White else Color.Transparent,
                                        RoundedCornerShape(3.dp)
                                    )
                                    .clickable {
                                        currentChunkIndex = index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isCurrent) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Interactive Playback Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { currentChunkIndex = 0 },
                    enabled = totalChunks > 1,
                    modifier = Modifier.testTag("stream_jump_first_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FirstPage,
                        contentDescription = "First Frame",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        currentChunkIndex = if (currentChunkIndex > 0) currentChunkIndex - 1 else totalChunks - 1
                    },
                    enabled = totalChunks > 1,
                    modifier = Modifier.testTag("stream_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Frame",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { isPlaying = !isPlaying },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("stream_toggle_play_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) CyberEmerald else CyberCyan
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Stream" else "Play Stream",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        currentChunkIndex = if (currentChunkIndex < totalChunks - 1) currentChunkIndex + 1 else 0
                    },
                    enabled = totalChunks > 1,
                    modifier = Modifier.testTag("stream_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Frame",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { currentChunkIndex = totalChunks - 1 },
                    enabled = totalChunks > 1,
                    modifier = Modifier.testTag("stream_jump_last_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LastPage,
                        contentDescription = "Last Frame",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Fullscreen Maximum Brightness Scanning Modal
    if (isFullScreen) {
        val activity = context as? Activity
        DisposableEffect(Unit) {
            val originalBrightness = activity?.window?.attributes?.screenBrightness
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
            onDispose {
                activity?.window?.attributes = activity?.window?.attributes?.apply {
                    screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }

        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("fullscreen_animated_stream_dialog"),
                color = Color.Black
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AIR-GAP STREAM TRANSMITTER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = CyberAmber
                            )
                        }

                        IconButton(
                            onClick = { isFullScreen = false },
                            modifier = Modifier.testTag("close_fullscreen_stream_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Fullscreen",
                                tint = Color.White
                            )
                        }
                    }

                    // Huge QR Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isQrInverted) colorScheme.composeDarkColor else colorScheme.composeLightColor)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeView(
                            qrContent = currentQrData,
                            sizePx = 900,
                            colorScheme = colorScheme,
                            errorCorrectionLevel = errorCorrection,
                            moduleShape = moduleShape,
                            isInverted = isQrInverted,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom Controls & Fullscreen Stream Progress
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Linear Progress
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val progressW = w * animatedProgress
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(
                                        listOf(CyberCyanBright, CyberEmeraldBright)
                                    ),
                                    size = Size(progressW, h),
                                    cornerRadius = CornerRadius(h / 2, h / 2)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Frame ${safeIndex + 1} of $totalChunks • Loop #$loopCount (${streamFps} FPS)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    currentChunkIndex = if (currentChunkIndex > 0) currentChunkIndex - 1 else totalChunks - 1
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.DarkGray.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                            }

                            Button(
                                onClick = { isPlaying = !isPlaying },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) CyberEmerald else CyberCyan,
                                    contentColor = Color.Black
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(54.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    currentChunkIndex = if (currentChunkIndex < totalChunks - 1) currentChunkIndex + 1 else 0
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.DarkGray.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
