package com.example.ui.components

import android.content.Context
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.qr.ChunkProcessResult
import com.example.qr.QrStreamReassembler
import com.example.qr.QrStreamScannerAnalyzer
import com.example.qr.StreamReassemblyState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * High-performance, turnkey CameraX Viewfinder Composable designed for scanning
 * animated QR code streams and reassembling fragmented payloads in real time.
 */
@Composable
fun CameraXStreamScanner(
    reassembler: QrStreamReassembler,
    onAssemblyComplete: (StreamReassemblyState.Complete) -> Unit,
    onNonStreamQrDetected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val streamState by reassembler.state.collectAsState()
    val progress by reassembler.chunkProgress.collectAsState()

    var hasTorch by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var previewViewInstance: PreviewView? by remember { mutableStateOf(null) }

    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    val zoomLevels = listOf(1f, 2f, 3f, 5f)

    // Tap-to-focus indicator
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusRing by remember { mutableStateOf(false) }

    // Frame capture pulse animation
    val burstAnim = remember { Animatable(0f) }
    var lastCapturedIndex by remember { mutableStateOf<Int?>(null) }

    // Watch for completed assembly
    LaunchedEffect(streamState) {
        if (streamState is StreamReassemblyState.Complete) {
            onAssemblyComplete(streamState as StreamReassemblyState.Complete)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Live CameraX Viewfinder with Pinch-to-Zoom & Tap-to-Focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoom = (currentZoomRatio * zoom).coerceIn(1f, 8f)
                        currentZoomRatio = newZoom
                        cameraControl?.setZoomRatio(newZoom)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        focusPoint = tapOffset
                        showFocusRing = true
                        previewViewInstance?.let { pv ->
                            val factory = pv.meteringPointFactory
                            val point = factory.createPoint(tapOffset.x, tapOffset.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                .setAutoCancelDuration(2, TimeUnit.SECONDS)
                                .build()
                            cameraControl?.startFocusAndMetering(action)
                        }
                        scope.launch {
                            delay(1200)
                            showFocusRing = false
                        }
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    previewViewInstance = previewView
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, QrStreamScannerAnalyzer(reassembler) { chunkResult ->
                                    when (chunkResult) {
                                        is ChunkProcessResult.NewChunk -> {
                                            lastCapturedIndex = chunkResult.index
                                            scope.launch {
                                                burstAnim.snapTo(1f)
                                                burstAnim.animateTo(0f, tween(350, easing = LinearEasing))
                                            }
                                        }
                                        is ChunkProcessResult.NonStreamQr -> {
                                            onNonStreamQrDetected(chunkResult.rawContent)
                                        }
                                        else -> {}
                                    }
                                })
                            }

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                            hasTorch = camera.cameraInfo.hasFlashUnit()
                            camera.cameraControl.setZoomRatio(currentZoomRatio)
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camerax_stream_preview")
            )

            // Reticle & Scanner Laser Overlay
            ScannerLaserReticle(
                isStreaming = progress != null,
                burstProgress = burstAnim.value,
                modifier = Modifier.fillMaxSize()
            )

            // Focus Ring Indicator
            if (showFocusRing && focusPoint != null) {
                FocusRingIndicator(point = focusPoint!!)
            }
        }

        // Top HUD Controls Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Pill
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (progress != null) CyberCyanBright.copy(alpha = 0.8f) else CyberEmerald.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (progress != null) CyberCyanBright else CyberEmeraldBright)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (progress != null) {
                            "STREAMING: ${progress!!.receivedCount}/${progress!!.totalChunks} CHUNKS"
                        } else {
                            "CAMERAX QR DETECTOR ACTIVE"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                }
            }

            // Quick Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch Toggle
                if (hasTorch) {
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraControl?.enableTorch(isTorchOn)
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isTorchOn) CyberEmerald.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, if (isTorchOn) CyberEmeraldBright else Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("camerax_torch_btn")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (isTorchOn) CyberEmeraldBright else Color.White
                        )
                    }
                }

                // Lens Switcher
                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("camerax_switch_lens_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // Zoom Preset Column
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(100.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                zoomLevels.forEach { zoom ->
                    val isSelected = (currentZoomRatio - zoom).let { Math.abs(it) < 0.3f }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CyberEmerald else Color.Transparent)
                            .clickable {
                                currentZoomRatio = zoom
                                cameraControl?.setZoomRatio(zoom)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${zoom.toInt()}x",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Real-Time Fragment Reassembly Dashboard Overlay
        progress?.let { prog ->
            StreamReassemblyHudCard(
                progress = prog,
                onReset = { reassembler.resetReassembler() },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun ScannerLaserReticle(
    isStreaming: Boolean,
    burstProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = -110f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = if (burstProgress > 0.05f) 3.dp else 1.5.dp,
                    color = if (burstProgress > 0.05f) CyberCyanBright else CyberEmerald.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    if (burstProgress > 0.05f) CyberCyanBright.copy(alpha = 0.15f * burstProgress) else Color.Transparent
                )
        ) {
            // Corner Bracket Highlights
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val cornerLength = 28.dp.toPx()
                val color = if (burstProgress > 0.05f) CyberCyanBright else CyberEmeraldBright

                // Top Left
                drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

                // Top Right
                drawLine(color, Offset(size.width - cornerLength, 0f), Offset(size.width, 0f), strokeWidth)
                drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)

                // Bottom Left
                drawLine(color, Offset(0f, size.height - cornerLength), Offset(0f, size.height), strokeWidth)
                drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)

                // Bottom Right
                drawLine(color, Offset(size.width - cornerLength, size.height), Offset(size.width, size.height), strokeWidth)
                drawLine(color, Offset(size.width, size.height - cornerLength), Offset(size.width, size.height), strokeWidth)
            }

            // Laser line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.Center)
                    .offset(y = laserOffset.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                if (isStreaming) CyberCyanBright else CyberEmeraldBright,
                                Color.White,
                                if (isStreaming) CyberCyanBright else CyberEmeraldBright,
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun FocusRingIndicator(point: Offset) {
    Box(
        modifier = Modifier
            .offset(x = (point.x / 3f).dp - 28.dp, y = (point.y / 3f).dp - 28.dp)
            .size(56.dp)
            .border(2.dp, CyberCyanBright, CircleShape)
    )
}

@Composable
private fun StreamReassemblyHudCard(
    progress: com.example.crypto.QrChunkProgress,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.92f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = progress.fileName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${progress.receivedCount}/${progress.totalChunks} Chunks • ${progress.formattedTransferSpeed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberCyanBright
                    )
                }

                Text(
                    text = "${(progress.progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = CyberEmeraldBright
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.progressFraction)
                        .height(6.dp)
                        .background(Brush.horizontalGradient(listOf(CyberCyan, CyberEmeraldBright)))
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = progress.validationMessage,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}
