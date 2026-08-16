package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ZoomIn
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.crypto.P2PTransferTicket
import com.example.crypto.QrChunkProgress
import com.example.qr.QrCodeScannerAnalyzer
import com.example.ui.components.PermissionsEducationalDialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberVioletBright
import com.example.util.FileUtils
import com.example.viewmodel.PendingDecryptionState
import com.example.viewmodel.StreamTimeoutNotice
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReceiveScreen(
    scanProgress: QrChunkProgress?,
    scannedP2PTicket: P2PTransferTicket?,
    isDownloadingP2P: Boolean,
    p2pDownloadProgress: Float,
    pendingDecryption: PendingDecryptionState?,
    streamTimeoutSeconds: Int = 15,
    streamRemainingSeconds: Int? = null,
    lastTimeoutNotice: StreamTimeoutNotice? = null,
    onQrCodeDetected: (String) -> Unit,
    onDownloadP2P: (P2PTransferTicket) -> Unit,
    onDismissP2P: () -> Unit,
    onResetScan: () -> Unit,
    onSetStreamTimeoutSeconds: (Int) -> Unit = {},
    onDismissTimeoutNotice: () -> Unit = {},
    onGalleryImageSelected: (android.graphics.Bitmap) -> Unit,
    onDecryptPendingPassphrase: (String) -> Unit,
    onDismissPendingDecryption: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var showPermissionEducationalDialog by remember { mutableStateOf(false) }
    var showTimeoutSettingsDialog by remember { mutableStateOf(false) }

    var hasTorch by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var previewViewInstance: PreviewView? by remember { mutableStateOf(null) }

    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    val availableZooms = listOf(1f, 2f, 3f, 5f)

    // Tap-to-focus indicator state
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusRing by remember { mutableStateOf(false) }

    // Frame capture pulse animation trigger
    var frameCaptureTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(scanProgress?.lastReceivedTimestamp) {
        if (scanProgress != null && scanProgress.lastReceivedIndex != null) {
            frameCaptureTrigger = scanProgress.lastReceivedTimestamp
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let { b -> onGalleryImageSelected(b) }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (!cameraPermissionState.status.isGranted) {
            showPermissionEducationalDialog = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraPermissionState.status.isGranted) {
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
                                    it.setAnalyzer(cameraExecutor, QrCodeScannerAnalyzer { qrText ->
                                        onQrCodeDetected(qrText)
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
                        .testTag("camera_preview_view")
                )

                // High-Tech Holographic Reticle & Scanning Laser with Live Streaming Progress Overlay
                ScannerOverlay(
                    isStreamActive = scanProgress != null,
                    scanProgress = scanProgress,
                    streamRemainingSeconds = streamRemainingSeconds,
                    frameCaptureTrigger = frameCaptureTrigger,
                    lastCapturedIndex = scanProgress?.lastReceivedIndex
                )

                // Animated Tap-to-Focus Target Indicator
                if (showFocusRing && focusPoint != null) {
                    TapFocusIndicator(point = focusPoint!!)
                }
            }
        } else {
            // Camera Permission Request Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CyberEmerald.copy(alpha = 0.15f))
                        .border(1.dp, CyberEmerald.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = CyberEmeraldBright,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "CipherQR requires camera access to scan multi-frame animated QR streams, P2P connection tickets, and cryptographic team keys.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showPermissionEducationalDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("grant_camera_permission_btn")
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Camera Access", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Scanner HUD Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Indicator Pill
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (scanProgress != null) CyberCyanBright.copy(alpha = 0.7f) else CyberEmerald.copy(alpha = 0.5f)
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
                            .background(if (scanProgress != null) CyberCyanBright else CyberEmeraldBright)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (scanProgress != null) {
                            "ASSEMBLING: ${scanProgress.receivedCount}/${scanProgress.totalChunks}"
                        } else {
                            "AIR-GAP SCANNER ACTIVE"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                }
            }

            // Quick Camera Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Stream Timeout Settings Button
                IconButton(
                    onClick = { showTimeoutSettingsDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, CyberCyanBright.copy(alpha = 0.4f), CircleShape)
                        .testTag("stream_timeout_config_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Stream Inactivity Timeout Settings (${streamTimeoutSeconds}s)",
                        tint = CyberCyanBright
                    )
                }

                // Torch / Flashlight Toggle
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
                            .testTag("toggle_torch_btn")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (isTorchOn) CyberEmeraldBright else Color.White
                        )
                    }
                }

                // Camera Lens Switch (Rear/Front)
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
                        .testTag("switch_camera_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }

                // Photo Gallery Barcode Scanner
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("gallery_picker_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Scan Image",
                        tint = Color.White
                    )
                }
            }
        }

        // Animated Inactivity Stream Timeout Alert Banner
        AnimatedVisibility(
            visible = lastTimeoutNotice != null,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp, start = 16.dp, end = 16.dp)
        ) {
            lastTimeoutNotice?.let { notice ->
                StreamTimeoutAlertCard(
                    notice = notice,
                    onDismiss = onDismissTimeoutNotice,
                    onOpenSettings = {
                        onDismissTimeoutNotice()
                        showTimeoutSettingsDialog = true
                    }
                )
            }
        }

        // Quick Zoom Preset Buttons (Floating on Viewfinder)
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
                availableZooms.forEach { zoom ->
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

        // Bottom HUD: QR Stream Frame Assembly & Real-Time Validation Matrix
        if (scanProgress != null) {
            FrameAssemblyPanel(
                scanProgress = scanProgress,
                streamRemainingSeconds = streamRemainingSeconds,
                onResetScan = onResetScan,
                onOpenTimeoutSettings = { showTimeoutSettingsDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            )
        }
    }

    // Modal: Inactivity Stream Timeout Config Dialog
    if (showTimeoutSettingsDialog) {
        StreamTimeoutSettingsDialog(
            currentTimeoutSeconds = streamTimeoutSeconds,
            onSelectTimeoutSeconds = {
                onSetStreamTimeoutSeconds(it)
                showTimeoutSettingsDialog = false
            },
            onDismiss = { showTimeoutSettingsDialog = false }
        )
    }

    // Educational Onboarding Dialog: Transparent Explanation before Camera Permission Request
    if (showPermissionEducationalDialog) {
        PermissionsEducationalDialog(
            onContinue = {
                showPermissionEducationalDialog = false
                cameraPermissionState.launchPermissionRequest()
            },
            onDismiss = {
                showPermissionEducationalDialog = false
            }
        )
    }

    // Modal: Custom Passphrase Required for Decryption
    if (pendingDecryption != null) {
        CustomPassphraseDecryptionDialog(
            pending = pendingDecryption,
            onDecrypt = onDecryptPendingPassphrase,
            onDismiss = onDismissPendingDecryption
        )
    }

    // Modal: P2P Direct LAN Handshake Prompt
    if (scannedP2PTicket != null) {
        P2PTransferPromptDialog(
            ticket = scannedP2PTicket,
            isDownloading = isDownloadingP2P,
            downloadProgress = p2pDownloadProgress,
            onDownload = { onDownloadP2P(scannedP2PTicket) },
            onDismiss = onDismissP2P
        )
    }
}

/**
 * High-Tech Holographic Scanning Overlay with animated laser, corner brackets, frame detection burst,
 * and real-time streaming segment percentage progress overlay
 */
@Composable
fun ScannerOverlay(
    isStreamActive: Boolean,
    scanProgress: QrChunkProgress? = null,
    streamRemainingSeconds: Int? = null,
    frameCaptureTrigger: Long,
    lastCapturedIndex: Int?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = -110f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Pulse animation when a chunk frame is captured
    val burstAnim = remember { Animatable(0f) }
    LaunchedEffect(frameCaptureTrigger) {
        if (frameCaptureTrigger > 0) {
            burstAnim.snapTo(1f)
            burstAnim.animateTo(0f, tween(400, easing = LinearEasing))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Target Box Frame (280dp)
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = if (burstAnim.value > 0.05f) 3.dp else 1.5.dp,
                    color = if (burstAnim.value > 0.05f) CyberCyanBright else CyberEmerald.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    if (burstAnim.value > 0.05f) CyberCyanBright.copy(alpha = 0.15f * burstAnim.value) else Color.Transparent
                )
        ) {
            // 4 Futuristic Corner Bracket Highlights
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val cornerLength = 28.dp.toPx()
                val color = if (burstAnim.value > 0.05f) CyberCyanBright else CyberEmeraldBright

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

            // Center Crosshair
            Canvas(modifier = Modifier.fillMaxSize()) {
                val crosshairColor = CyberCyan.copy(alpha = 0.35f)
                val chSize = 14.dp.toPx()
                val cx = size.width / 2
                val cy = size.height / 2
                drawLine(crosshairColor, Offset(cx - chSize, cy), Offset(cx + chSize, cy), 1.5.dp.toPx())
                drawLine(crosshairColor, Offset(cx, cy - chSize), Offset(cx, cy + chSize), 1.5.dp.toPx())
            }

            // Animated Laser Line with trailing gradient
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
                                CyberCyanBright,
                                CyberEmeraldBright,
                                CyberCyanBright,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Flash badge when frame is verified
            if (burstAnim.value > 0.1f && lastCapturedIndex != null) {
                Surface(
                    color = CyberCyanBright,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "FRAME #${lastCapturedIndex + 1} CAPTURED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Live In-Reticle Streaming Progress Overlay (Segment Percentage, Speed & Metrics)
            if (scanProgress != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .testTag("camera_progress_overlay")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmeraldBright)
                                )
                                Text(
                                    text = "STREAM RECEIVING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = CyberCyanBright
                                )
                            }

                            // Real-time transfer speed badge (e.g. ⚡ 24.5 KB/s)
                            Surface(
                                color = CyberEmerald.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberEmeraldBright.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("stream_transfer_speed_indicator")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Transfer Speed",
                                        tint = CyberEmeraldBright,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = scanProgress.formattedTransferSpeed,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = CyberEmeraldBright
                                    )
                                }
                            }

                            val percent = (scanProgress.progressFraction * 100).toInt()
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = CyberEmeraldBright
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Reticle mini progress track
                        LinearProgressIndicator(
                            progress = { scanProgress.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberEmeraldBright,
                            trackColor = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${scanProgress.receivedCount}/${scanProgress.totalChunks} chunks • ${FileUtils.formatBytes(scanProgress.assembledBytes)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = Color.White
                            )

                            val speed = scanProgress.estimatedSpeedChunksPerSec
                            if (speed > 0f) {
                                Text(
                                    text = "%.1f fps".format(speed),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = CyberCyanBright
                                )
                            }
                        }
                    }
                }
            }
        }

        // Viewfinder Alignment Guide / Live Status Pill
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(100.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 170.dp)
        ) {
            val statusText = when {
                scanProgress != null && scanProgress.isComplete -> "100% Assembled — Decrypting payload..."
                scanProgress != null && streamRemainingSeconds != null -> "Receiving: ${(scanProgress.progressFraction * 100).toInt()}% • ${scanProgress.formattedTransferSpeed} • Reset in ${streamRemainingSeconds}s"
                scanProgress != null -> "Receiving: ${(scanProgress.progressFraction * 100).toInt()}% • ${scanProgress.formattedTransferSpeed} (${scanProgress.receivedCount}/${scanProgress.totalChunks} chunks)"
                isStreamActive -> "Hold steady — assembling stream chunks"
                else -> "Align QR code inside target reticle"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (scanProgress != null) CyberCyanBright else Color.LightGray,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Animated Tap-to-Focus Indicator Ring
 */
@Composable
fun TapFocusIndicator(point: Offset) {
    val scaleAnim = remember { Animatable(1.4f) }
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(point) {
        scaleAnim.snapTo(1.4f)
        alphaAnim.snapTo(1f)
        scaleAnim.animateTo(1.0f, tween(250, easing = FastOutSlowInEasing))
        delay(600)
        alphaAnim.animateTo(0f, tween(350, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .offset(x = (point.x - 30).dp, y = (point.y - 30).dp)
            .size((60 * scaleAnim.value).dp)
            .clip(CircleShape)
            .border(2.dp, CyberCyanBright.copy(alpha = alphaAnim.value), CircleShape)
    )
}

/**
 * Real-Time QR Stream Frame Assembly & Integrity Validation Panel
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrameAssemblyPanel(
    scanProgress: QrChunkProgress,
    streamRemainingSeconds: Int? = null,
    onResetScan: () -> Unit,
    onOpenTimeoutSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("scan_progress_hud"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B132B).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberEmerald.copy(alpha = 0.7f)),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Payload Details & Transfer ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.2f))
                            .border(1.dp, CyberEmerald.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyberEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = scanProgress.fileName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FileUtils.formatBytes(scanProgress.originalSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                            Text("•", color = Color.DarkGray, fontSize = 10.sp)
                            Text(
                                text = "TID: ${scanProgress.transferId}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = CyberCyanBright
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (streamRemainingSeconds != null) {
                        val isUrgent = streamRemainingSeconds <= 5
                        Surface(
                            color = if (isUrgent) Color(0xFF7F1D1D).copy(alpha = 0.9f) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(100.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isUrgent) Color(0xFFEF4444) else CyberCyanBright.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { onOpenTimeoutSettings() }
                                .testTag("stream_timeout_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUrgent) Icons.Default.HourglassBottom else Icons.Default.Timer,
                                    contentDescription = "Stream Inactivity Timer",
                                    tint = if (isUrgent) Color(0xFFFCA5A5) else CyberCyanBright,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${streamRemainingSeconds}s",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isUrgent) Color(0xFFFCA5A5) else Color.White
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onResetScan,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("reset_scan_hud_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chunk Assembly Matrix Grid (Real-time visual blocks)
            Text(
                text = "FRAME BUFFER MATRIX (${scanProgress.receivedCount}/${scanProgress.totalChunks})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = CyberCyanBright
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Chunk Matrix Flow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val total = scanProgress.totalChunks.coerceAtMost(32) // Render up to 32 chunks cleanly
                for (i in 0 until total) {
                    val isReceived = scanProgress.receivedChunks.containsKey(i)
                    val isLatest = scanProgress.lastReceivedIndex == i

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isLatest -> CyberCyanBright
                                    isReceived -> CyberEmerald.copy(alpha = 0.35f)
                                    else -> Color(0xFF1E293B)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isLatest -> CyberCyanBright
                                    isReceived -> CyberEmeraldBright
                                    else -> Color.White.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isReceived) {
                            Text(
                                text = "${i + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = if (isLatest) Color.Black else CyberEmeraldBright
                            )
                        } else {
                            Text(
                                text = "${i + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overall Progress Bar & Byte Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${(scanProgress.progressFraction * 100).toInt()}% Assembled",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyberEmeraldBright
                    )
                    Text(
                        text = "${FileUtils.formatBytes(scanProgress.assembledBytes)} of ${FileUtils.formatBytes(scanProgress.originalSize)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        color = Color.LightGray
                    )
                }

                // Prominent Real-Time Speed & Throughput Indicator
                Surface(
                    color = CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyanBright.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("transfer_speed_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Transfer Speed Indicator",
                            tint = CyberCyanBright,
                            modifier = Modifier.size(15.dp)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = scanProgress.formattedTransferSpeed,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = CyberCyanBright
                            )
                            val fps = scanProgress.estimatedSpeedChunksPerSec
                            if (fps > 0f) {
                                Text(
                                    text = "%.1f fps".format(fps),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { scanProgress.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CyberEmeraldBright,
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Real-Time Transfer Efficiency & ETA Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = CyberEmeraldBright,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Efficiency: ${scanProgress.transferEfficiencyScore}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = CyberEmeraldBright
                    )
                }

                val eta = scanProgress.estimatedRemainingSeconds
                if (eta != null) {
                    Text(
                        text = "ETA: ~${eta}s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = CyberCyanBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-Time Frame Validation Status Chip
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberEmeraldBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (scanProgress.validationMessage.isNotBlank()) {
                            scanProgress.validationMessage
                        } else {
                            "Validating incoming frame checksums (SHA-256)..."
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Missing Chunks Notice (if any)
            if (scanProgress.missingIndices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Waiting for: " + scanProgress.missingIndices.take(6).map { "#${it + 1}" }.joinToString(", ") +
                            if (scanProgress.missingIndices.size > 6) " (+${scanProgress.missingIndices.size - 6} more)" else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CyberCyanBright
                )
            }
        }
    }
}

/**
 * Modal Dialog for entering Custom Passphrase when decrypting assembled QR streams
 */
@Composable
fun CustomPassphraseDecryptionDialog(
    pending: PendingDecryptionState,
    onDecrypt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_passphrase_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = CyberCyanBright,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Stream Assembled (100%)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter the secret team passphrase used to encrypt '${pending.progress.fileName}'",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File Specifications Box
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payload:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(pending.progress.fileName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Assembled Size:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(FileUtils.formatBytes(pending.assembledEnvelope.size.toLong()), style = MaterialTheme.typography.labelSmall, color = CyberEmeraldBright)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Original SHA-256:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(pending.progress.originalSha256.take(12) + "...", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = CyberCyanBright)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Decryption Passphrase") },
                    placeholder = { Text("Enter secret key or passphrase") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Passphrase Visibility",
                                tint = Color.LightGray
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("passphrase_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Color.LightGray)
                    }

                    Button(
                        onClick = { onDecrypt(passphrase) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_decrypt_passphrase_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyanBright),
                        enabled = passphrase.isNotBlank()
                    ) {
                        Text("Decrypt", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Modal Dialog for handling incoming Local P2P Direct LAN Handshakes
 */
@Composable
fun P2PTransferPromptDialog(
    ticket: P2PTransferTicket,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("p2p_prompt_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = CyberCyanBright,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Direct P2P Stream",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Encrypted local Wi-Fi transfer detected from ${ticket.teamName ?: "Peer"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File Specifications Box
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("File:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(ticket.fileName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Size:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(FileUtils.formatBytes(ticket.originalSize), style = MaterialTheme.typography.labelSmall, color = CyberEmeraldBright)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Peer IP:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("${ticket.hostIp}:${ticket.port}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = CyberCyanBright)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isDownloading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Streaming & Decrypting... ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberEmeraldBright
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = CyberEmeraldBright
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = Color.LightGray)
                        }

                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_p2p_download_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stream Inactivity Timeout Notification Banner
 * Alerts the user when a QR stream capture was reset due to timeout inactivity
 */
@Composable
fun StreamTimeoutAlertCard(
    notice: StreamTimeoutNotice,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stream_timeout_alert_card"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B1315).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.85f)),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "QR Stream Incomplete (Timed Out)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFCA5A5)
                        )
                        Text(
                            text = "Auto-reset after ${notice.timeoutSeconds}s inactivity",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Transfer for \"${notice.fileName}\" stopped receiving chunks (${notice.receivedCount}/${notice.totalChunks} assembled). The scanner buffer has been automatically reset.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = CyberCyanBright
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Adjust Duration (${notice.timeoutSeconds}s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyanBright
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Scan Again",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Stream Inactivity Timeout Settings Modal
 */
@Composable
fun StreamTimeoutSettingsDialog(
    currentTimeoutSeconds: Int,
    onSelectTimeoutSeconds: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timeoutOptions = listOf(
        Pair(10, "10s (Fast / High FPS)"),
        Pair(15, "15s (Recommended)"),
        Pair(30, "30s (Large Payloads)"),
        Pair(60, "60s (Extended / Slow Camera)")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B132B),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyanBright.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberCyanBright.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CyberCyanBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Stream Inactivity Timeout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "If no new QR stream chunks are received within this period, the scanner will automatically clear the buffer and reset to prevent stale or incomplete frame state.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeoutOptions.forEach { (seconds, label) ->
                        val isSelected = currentTimeoutSeconds == seconds
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyanBright else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTimeoutSeconds(seconds) }
                                .testTag("timeout_option_${seconds}s")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) CyberCyanBright else Color.White
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = CyberCyanBright,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
