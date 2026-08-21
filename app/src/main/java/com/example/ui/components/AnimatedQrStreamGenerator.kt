package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ShapeLine
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/**
 * Animated QR Stream Generator Component:
 * Splits large encrypted files into a sequenced carousel of scannable QR codes for high-capacity, air-gapped transfers.
 */
@Composable
fun AnimatedQrStreamGenerator(
    chunks: List<String>,
    currentChunkIndex: Int,
    isPlaying: Boolean,
    streamFps: Int,
    densityPreset: QrDensityPreset,
    loopCount: Int,
    fileName: String,
    originalSizeBytes: Long,
    encryptedSizeBytes: Long,
    colorScheme: QrColorScheme = QrColorScheme.HIGH_CONTRAST_MONO,
    errorCorrectionLevel: QrErrorCorrectionLevel = QrErrorCorrectionLevel.LEVEL_M,
    moduleShape: QrModuleShape = QrModuleShape.SQUARE,
    isQrInverted: Boolean = false,
    onTogglePlay: () -> Unit,
    onSelectChunk: (Int) -> Unit,
    onNextChunk: () -> Unit,
    onPrevChunk: () -> Unit,
    onJumpFirst: () -> Unit,
    onJumpLast: () -> Unit,
    onSetFps: (Int) -> Unit,
    onSetDensityPreset: (QrDensityPreset) -> Unit,
    onSetColorScheme: (QrColorScheme) -> Unit = {},
    onSetErrorCorrectionLevel: (QrErrorCorrectionLevel) -> Unit = {},
    onSetModuleShape: (QrModuleShape) -> Unit = {},
    onToggleInverted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFullScreen by remember { mutableStateOf(false) }
    var showOpticalTuning by remember { mutableStateOf(false) }
    val totalChunks = chunks.size.coerceAtLeast(1)
    val safeIndex = currentChunkIndex.coerceIn(0, totalChunks - 1)
    val currentQrString = chunks.getOrNull(safeIndex) ?: ""
    val parsedEnvelope = remember(currentQrString) {
        if (currentQrString.isNotEmpty()) CryptoManager.parseQrChunk(currentQrString) else null
    }

    val progressFraction = (safeIndex + 1).toFloat() / totalChunks.toFloat()
    val estimatedBandwidthBytesPerSec = (densityPreset.chunkSizeBytes * streamFps).toLong()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    val listState = rememberLazyListState()
    LaunchedEffect(safeIndex) {
        if (totalChunks > 1) {
            listState.animateScrollToItem(safeIndex)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animated_qr_stream_generator_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.6f), CyberEmerald.copy(alpha = 0.6f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: Stream Title, Loop Counter, Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = CyberEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Optical Air-Gap Stream",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$fileName (${FileUtils.formatBytes(encryptedSizeBytes)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Loop count chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Loop #$loopCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = CyberCyanBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Live playback indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPlaying) CyberEmerald.copy(alpha = 0.18f) else CyberAmber.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, if (isPlaying) CyberEmerald.copy(alpha = haloAlpha) else CyberAmber)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) CyberEmeraldBright else CyberAmber)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isPlaying) "${streamFps} FPS" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (isPlaying) CyberEmeraldBright else CyberAmber
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Visual QR Code Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colorScheme.composeLightColor)
                    .border(
                        BorderStroke(
                            1.5.dp,
                            if (isPlaying) CyberEmerald.copy(alpha = haloAlpha) else CyberCyan.copy(alpha = 0.4f)
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { isFullScreen = true }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                // QR Display with customizable ColorScheme & ErrorCorrectionLevel
                QrCodeView(
                    qrContent = currentQrString,
                    sizePx = 700,
                    colorScheme = colorScheme,
                    errorCorrectionLevel = errorCorrectionLevel,
                    moduleShape = moduleShape,
                    isInverted = isQrInverted,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Floating Overlays: Chunk Tag & Action Controls
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
                            text = "FRAME ${safeIndex + 1}/$totalChunks  •  ${errorCorrectionLevel.badgeLabel}",
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
                            onClick = onToggleInverted,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                .testTag("invert_contrast_quick_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Invert Contrast",
                                tint = if (isQrInverted) CyberAmber else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { isFullScreen = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                .testTag("fullscreen_qr_stream_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Expand Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Bottom HUD Overlay: Stream Loop Progress Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = colorScheme.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.LightGray
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = CyberCyanBright
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyberEmeraldBright,
                        trackColor = Color.DarkGray.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chunk Checksum & Optical Telemetry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyberEmeraldBright,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val chunkHash = parsedEnvelope?.chunkSha256?.take(10) ?: "..."
                    Text(
                        text = "SHA: $chunkHash",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Bandwidth: ~${FileUtils.formatBytes(estimatedBandwidthBytesPerSec)}/s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = CyberCyanBright
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Playback Controls: Jump First, Prev, Play/Pause, Next, Jump Last
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onJumpFirst,
                    modifier = Modifier.testTag("jump_first_chunk_btn")
                ) {
                    Icon(Icons.Default.FirstPage, contentDescription = "First Chunk", tint = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(
                    onClick = onPrevChunk,
                    modifier = Modifier.testTag("prev_chunk_btn")
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Chunk", tint = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onTogglePlay,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("toggle_play_stream_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) CyberEmerald else CyberCyan
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Stream" else "Start Animated Stream",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = onNextChunk,
                    modifier = Modifier.testTag("next_chunk_btn")
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Chunk", tint = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(
                    onClick = onJumpLast,
                    modifier = Modifier.testTag("jump_last_chunk_btn")
                ) {
                    Icon(Icons.Default.LastPage, contentDescription = "Last Chunk", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Optical Contrast & Customization Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { showOpticalTuning = !showOpticalTuning }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberCyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Optical Tuning & Contrast",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (showOpticalTuning) "Hide Options ▲" else "Customize ▼",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyberCyanBright
                )
            }

            // Expandable Optical Tuning Panel
            AnimatedVisibility(visible = showOpticalTuning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Color Schemes Palette
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = CyberCyanBright, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QR Color Contrast Palette", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(QrColorScheme.values().size) { idx ->
                                val scheme = QrColorScheme.values()[idx]
                                val isSelected = colorScheme == scheme
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetColorScheme(scheme) },
                                    label = {
                                        Text(scheme.badgeLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(scheme.composeDarkColor)
                                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                        selectedLabelColor = CyberCyanBright
                                    )
                                )
                            }
                        }
                    }

                    // Error Correction Level (ECC)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberEmeraldBright, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Error Correction Level (ECC)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            QrErrorCorrectionLevel.values().forEach { ecc ->
                                val isSelected = errorCorrectionLevel == ecc
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetErrorCorrectionLevel(ecc) },
                                    label = {
                                        Text(
                                            text = ecc.badgeLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberEmerald.copy(alpha = 0.25f),
                                        selectedLabelColor = CyberEmeraldBright
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Module Shape
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShapeLine, contentDescription = null, tint = CyberVioletBright, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Matrix Geometry Shape", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QrModuleShape.values().forEach { shape ->
                                val isSelected = moduleShape == shape
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetModuleShape(shape) },
                                    label = {
                                        Text(
                                            text = shape.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberViolet.copy(alpha = 0.25f),
                                        selectedLabelColor = CyberVioletBright
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // FPS Speed Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyberCyanBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Frame Rate (FPS)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "$streamFps FPS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberCyanBright
                        )
                    )
                }

                Slider(
                    value = streamFps.toFloat(),
                    onValueChange = { onSetFps(it.toInt()) },
                    valueRange = 1f..15f,
                    steps = 13,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fps_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyanBright,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // Speed Preset Chips (2x, 4x, 8x, 12x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(2, 4, 8, 12).forEach { presetFps ->
                        val isSelected = streamFps == presetFps
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetFps(presetFps) },
                            label = {
                                Text(
                                    text = "${presetFps} FPS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                selectedLabelColor = CyberCyanBright
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // QR Capacity & Chunk Density Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = CyberVioletBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Density / Capacity",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "${totalChunks} QR Frames",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberVioletBright
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QrDensityPreset.values().forEach { preset ->
                        val isSelected = densityPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetDensityPreset(preset) },
                            label = {
                                Column(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = preset.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = if (isSelected) CyberVioletBright else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberViolet.copy(alpha = 0.25f),
                                selectedLabelColor = CyberVioletBright
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Timeline Matrix Scrubber (when multiple chunks exist)
            if (totalChunks > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Chunk Frame Scrubber",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(chunks) { idx, _ ->
                            val isCurrent = idx == safeIndex
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (isCurrent) CyberEmerald else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) CyberEmeraldBright else Color.Transparent,
                                        RoundedCornerShape(7.dp)
                                    )
                                    .clickable { onSelectChunk(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full-Screen Immersive Air-Gap Broadcast Presentation Modal
    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF030712)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Optical Air-Gap Broadcast",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Chunk ${safeIndex + 1} of $totalChunks • Loop #$loopCount (${streamFps} FPS)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberEmeraldBright
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onToggleInverted,
                                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = "Invert", tint = if (isQrInverted) CyberAmber else Color.White)
                            }

                            IconButton(
                                onClick = { isFullScreen = false },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.FullscreenExit, contentDescription = "Close Fullscreen", tint = Color.White)
                            }
                        }
                    }

                    // Centered Oversized QR Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colorScheme.composeLightColor)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeView(
                            qrContent = currentQrString,
                            sizePx = 900,
                            colorScheme = colorScheme,
                            errorCorrectionLevel = errorCorrectionLevel,
                            moduleShape = moduleShape,
                            isInverted = isQrInverted,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom Controls & Progress
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberEmeraldBright,
                            trackColor = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onPrevChunk) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(32.dp))
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Button(
                                onClick = onTogglePlay,
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            IconButton(onClick = onNextChunk) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Align receiver camera directly with the QR code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
