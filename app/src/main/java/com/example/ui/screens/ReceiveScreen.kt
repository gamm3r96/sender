package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.p2p.DiscoveredPeer
import com.example.p2p.LocalTransferServer
import com.example.p2p.NetworkInfoState
import com.example.qr.QrCodeScannerAnalyzer
import com.example.ui.components.AnimatedStreamProgressBar
import com.example.ui.components.CameraPermissionFlow
import com.example.ui.components.CompactStreamProgressBar
import com.example.ui.components.PermissionsEducationalDialog
import com.example.ui.components.SecureTransferProgressBar
import com.example.ui.components.TransferPhase
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
    p2pDownloadSpeed: Long = 0L,
    pendingDecryption: PendingDecryptionState?,
    networkInfo: NetworkInfoState = NetworkInfoState(),
    p2pDiagnostics: com.example.p2p.P2PConnectionMetrics = com.example.p2p.P2PConnectionMetrics(),
    onOpenDiagnostics: () -> Unit = {},
    onRunPingTest: (String?) -> Unit = {},
    discoveredPeers: List<DiscoveredPeer> = emptyList(),
    isScanningPeers: Boolean = false,
    receiverServerStatus: LocalTransferServer.ServerStatus = LocalTransferServer.ServerStatus.Stopped,
    receiverServerProgress: Float = 0f,
    streamTimeoutSeconds: Int = 15,
    streamRemainingSeconds: Int? = null,
    lastTimeoutNotice: StreamTimeoutNotice? = null,
    isHapticEnabled: Boolean = true,
    onToggleHaptic: (Boolean) -> Unit = {},
    onTestHaptic: (Int) -> Unit = {},
    onQrCodeDetected: (String) -> Unit,
    onDownloadP2P: (P2PTransferTicket) -> Unit,
    onDismissP2P: () -> Unit,
    onResetScan: () -> Unit,
    onSetStreamTimeoutSeconds: (Int) -> Unit = {},
    onDismissTimeoutNotice: () -> Unit = {},
    onGalleryImageSelected: (android.graphics.Bitmap) -> Unit,
    onDecryptPendingPassphrase: (String) -> Unit,
    onDismissPendingDecryption: () -> Unit,
    onScanLanPeers: () -> Unit = {},
    onDownloadDiscoveredPeer: (DiscoveredPeer, String?) -> Unit = { _, _ -> },
    onDownloadManualIp: (String, Int, String) -> Unit = { _, _, _ -> },
    onStartReceiverServer: (Int) -> Unit = {},
    onStopReceiverServer: () -> Unit = {},
    onOpenWifiSettings: () -> Unit = {},
    onOpenHotspotSettings: () -> Unit = {},
    onRefreshNetworkInfo: () -> Unit = {},
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

    var selectedReceiveTab by remember { mutableStateOf(0) } // 0 = Optical QR Scanner, 1 = Wi-Fi / Hotspot LAN

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Receiver Mode Switcher
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedReceiveTab == 0) CyberEmerald.copy(alpha = 0.25f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedReceiveTab == 0) CyberEmeraldBright else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedReceiveTab = 0 }
                        .testTag("tab_receive_optical_qr")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = if (selectedReceiveTab == 0) CyberEmeraldBright else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nearby QR Transfer",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedReceiveTab == 0) CyberEmeraldBright else Color.LightGray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedReceiveTab == 1) CyberCyan.copy(alpha = 0.25f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedReceiveTab == 1) CyberCyanBright else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedReceiveTab = 1 }
                        .testTag("tab_receive_wifi_lan")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (networkInfo.isHotspotActive) Icons.Default.WifiTethering else Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (selectedReceiveTab == 1) CyberCyanBright else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Local P2P Wi-Fi",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedReceiveTab == 1) CyberCyanBright else Color.LightGray
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedReceiveTab == 0) {
                CameraPermissionFlow(
                    cameraPermissionState = cameraPermissionState,
                    onOpenEducationalDialog = { showPermissionEducationalDialog = true },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
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
                    // Stream Timeout & Tactile Settings Button
                    IconButton(
                        onClick = { showTimeoutSettingsDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, CyberCyanBright.copy(alpha = 0.4f), CircleShape)
                            .testTag("stream_timeout_config_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Scanner & Haptic Settings",
                            tint = CyberCyanBright
                        )
                    }

                    // Tactile Haptic Feedback Quick Toggle
                    IconButton(
                        onClick = { onToggleHaptic(!isHapticEnabled) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isHapticEnabled) CyberEmerald.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, if (isHapticEnabled) CyberEmeraldBright else Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("toggle_haptic_quick_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = if (isHapticEnabled) "Tactile Haptics Active" else "Tactile Haptics Disabled",
                            tint = if (isHapticEnabled) CyberEmeraldBright else Color.Gray
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
            androidx.compose.animation.AnimatedVisibility(
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
    }
} else {
    // Wi-Fi / Hotspot LAN Direct Receiver Hub
    WifiDirectReceiverView(
        networkInfo = networkInfo,
        p2pDiagnostics = p2pDiagnostics,
        onOpenDiagnostics = onOpenDiagnostics,
        discoveredPeers = discoveredPeers,
        isScanningPeers = isScanningPeers,
        isDownloading = isDownloadingP2P,
        downloadProgress = p2pDownloadProgress,
        downloadSpeed = p2pDownloadSpeed,
        receiverServerStatus = receiverServerStatus,
        receiverServerProgress = receiverServerProgress,
        onScanLanPeers = onScanLanPeers,
        onDownloadDiscoveredPeer = onDownloadDiscoveredPeer,
        onDownloadManualIp = onDownloadManualIp,
        onStartReceiverServer = onStartReceiverServer,
        onStopReceiverServer = onStopReceiverServer,
        onOpenWifiSettings = onOpenWifiSettings,
        onOpenHotspotSettings = onOpenHotspotSettings,
        onRefreshNetworkInfo = onRefreshNetworkInfo,
        modifier = Modifier.fillMaxSize()
    )
}
}
}

    // Modal: Inactivity Stream Timeout & Tactile Feedback Config Dialog
    if (showTimeoutSettingsDialog) {
        StreamTimeoutSettingsDialog(
            currentTimeoutSeconds = streamTimeoutSeconds,
            isHapticEnabled = isHapticEnabled,
            onToggleHaptic = onToggleHaptic,
            onTestHaptic = onTestHaptic,
            onSelectTimeoutSeconds = {
                onSetStreamTimeoutSeconds(it)
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

                        Spacer(modifier = Modifier.height(6.dp))

                        // Reticle animated mini progress track with cyber sweep shimmer
                        CompactStreamProgressBar(
                            scanProgress = scanProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

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

            // Animated QR Stream Decoding Progress Bar with Real-Time Throughput & ETA
            AnimatedStreamProgressBar(
                scanProgress = scanProgress,
                height = 12.dp,
                showDetailedMetrics = true,
                showBufferStrip = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Real-Time Transfer Efficiency & Frame Validation Chip
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
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
                            text = "${scanProgress.transferEfficiencyScore}% eff",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
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
                    val detected = remember(pending.progress.fileName, pending.progress.mimeType) {
                        com.example.ui.components.FileTypeDetector.detect(pending.progress.fileName, pending.progress.mimeType)
                    }

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
                            Text("Type Preview:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(detected.categoryName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = detected.primaryColor)
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
                        SecureTransferProgressBar(
                            phase = if (downloadProgress < 1f) TransferPhase.TRANSFERRING else TransferPhase.DECRYPTING,
                            progress = downloadProgress,
                            speedBytesPerSec = 0L,
                            modifier = Modifier.fillMaxWidth()
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
 * Stream Inactivity Timeout & Tactile Feedback Settings Modal
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StreamTimeoutSettingsDialog(
    currentTimeoutSeconds: Int,
    isHapticEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit,
    onTestHaptic: (Int) -> Unit,
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
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
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = CyberCyanBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "Scanner & Feedback",
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
                }

                // Tactile Haptic Vibration Toggle Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHapticEnabled) CyberEmerald.copy(alpha = 0.12f) else Color(0xFF1E293B)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHapticEnabled) CyberEmeraldBright.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = null,
                                        tint = if (isHapticEnabled) CyberEmeraldBright else Color.LightGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Tactile Vibration",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Haptic ticks on frame scans & completion",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray
                                        )
                                    }
                                }

                                Switch(
                                    checked = isHapticEnabled,
                                    onCheckedChange = { onToggleHaptic(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberEmeraldBright,
                                        checkedTrackColor = CyberEmerald.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("switch_haptic_feedback")
                                )
                            }

                            if (isHapticEnabled) {
                                Text(
                                    text = "Test Vibration Waveforms:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyberCyanBright
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { onTestHaptic(0) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("test_haptic_tick")
                                    ) {
                                        Text("Scan Tick (12ms)", style = MaterialTheme.typography.labelSmall, color = CyberEmeraldBright)
                                    }

                                    OutlinedButton(
                                        onClick = { onTestHaptic(1) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("test_haptic_stream_complete")
                                    ) {
                                        Text("Stream Done", style = MaterialTheme.typography.labelSmall, color = CyberCyanBright)
                                    }

                                    OutlinedButton(
                                        onClick = { onTestHaptic(2) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("test_haptic_decrypt")
                                    ) {
                                        Text("Decrypted", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA78BFA))
                                    }

                                    OutlinedButton(
                                        onClick = { onTestHaptic(3) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("test_haptic_error")
                                    ) {
                                        Text("Error Buzz", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }

                // Inactivity Stream Timeout Duration Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CyberCyanBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Stream Inactivity Timeout",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Auto-resets chunk reception buffer if no frames arrive within the chosen duration.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )

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
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) CyberCyanBright else Color.White
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = CyberCyanBright,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Done Button
                item {
                    Spacer(modifier = Modifier.height(4.dp))
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
}

/**
 * High-Tech Wi-Fi & Hotspot Direct Receiver Hub
 * Enables LAN auto-discovery of peers, manual IP downloads, and local browser drop receiving.
 */
@Composable
fun WifiDirectReceiverView(
    networkInfo: NetworkInfoState,
    p2pDiagnostics: com.example.p2p.P2PConnectionMetrics,
    onOpenDiagnostics: () -> Unit,
    discoveredPeers: List<DiscoveredPeer>,
    isScanningPeers: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadSpeed: Long,
    receiverServerStatus: LocalTransferServer.ServerStatus,
    receiverServerProgress: Float,
    onScanLanPeers: () -> Unit,
    onDownloadDiscoveredPeer: (DiscoveredPeer, String?) -> Unit,
    onDownloadManualIp: (String, Int, String) -> Unit,
    onStartReceiverServer: (Int) -> Unit,
    onStopReceiverServer: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenHotspotSettings: () -> Unit,
    onRefreshNetworkInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8989") }
    var manualPassphrase by remember { mutableStateOf("") }

    // Prefill manual IP subnet if available
    val detectedIp = networkInfo.localIp
    LaunchedEffect(detectedIp) {
        if (manualIp.isEmpty() && detectedIp != null) {
            val parts = detectedIp.split(".")
            if (parts.size == 4) {
                manualIp = "${parts[0]}.${parts[1]}.${parts[2]}."
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Network Status Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receiver_network_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (networkInfo.isConnected) CyberCyan.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(
                                        if (networkInfo.isConnected) CyberCyan.copy(alpha = 0.2f)
                                        else Color.Red.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (networkInfo.isHotspotActive) Icons.Default.WifiTethering
                                    else if (networkInfo.isConnected) Icons.Default.Wifi
                                    else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = if (networkInfo.isConnected) CyberCyanBright else Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (networkInfo.isHotspotActive) "Hotspot Tethering Active"
                                    else if (networkInfo.isConnected) "Wi-Fi Connected: ${networkInfo.ssid ?: "LAN"}"
                                    else "Offline / No Local Network",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (networkInfo.isConnected) "Local IP: ${networkInfo.localIp ?: "Detecting..."}"
                                    else "Connect Wi-Fi or turn on Hotspot to receive files",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (networkInfo.isConnected) CyberEmeraldBright else Color.LightGray
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshNetworkInfo,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .testTag("refresh_network_receiver_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Network",
                                tint = CyberCyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenHotspotSettings,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_hotspot_receiver_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyanBright)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hotspot AP", style = MaterialTheme.typography.labelSmall, color = CyberCyanBright)
                        }

                        OutlinedButton(
                            onClick = onOpenWifiSettings,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_wifi_receiver_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wi-Fi Settings", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                    }
                }
            }
        }

        // Live Real-Time P2P RF Signal & Diagnostic Telemetry Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("p2p_diagnostic_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
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
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = CyberCyanBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "RF Signal & Connection Diagnostics",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Health Score: ${p2pDiagnostics.healthScore}% • Status: ${p2pDiagnostics.healthGrade.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberEmeraldBright
                                )
                            }
                        }

                        Button(
                            onClick = onOpenDiagnostics,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_diagnostics_from_receive_btn")
                        ) {
                            Text("Dashboard", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("RSSI Signal", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
                                Text(
                                    text = "${p2pDiagnostics.rssiDbm} dBm",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = CyberEmeraldBright
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Link Speed", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
                                Text(
                                    text = "${p2pDiagnostics.linkSpeedMbps} Mbps",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = CyberCyanBright
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("RTT Latency", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
                                Text(
                                    text = "${p2pDiagnostics.rttPingMs} ms",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = if (p2pDiagnostics.rttPingMs <= 40) CyberEmeraldBright else Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. LAN Peer Auto-Discovery Radar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peer_radar_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = CyberEmeraldBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Local Peer Radar",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onScanLanPeers,
                            enabled = !isScanningPeers && networkInfo.isConnected,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("scan_lan_peers_btn")
                        ) {
                            if (isScanningPeers) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scanning...", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan Subnet", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (discoveredPeers.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isScanningPeers) "Scanning 254 subnet IPs for Cipher transmitters..."
                                    else "No active sender streams discovered yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ensure the sender has started P2P Direct Stream or Web Portal on the same network.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            discoveredPeers.forEach { peer ->
                                DiscoveredPeerItemCard(
                                    peer = peer,
                                    isDownloading = isDownloading,
                                    onDownload = { pass -> onDownloadDiscoveredPeer(peer, pass) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Direct Manual IP / Port Connect
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_ip_connect_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = CyberCyanBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Manual Host IP Connect",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect directly to a sender IP when multicast peer broadcast is restricted by a router.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("Sender Host IP") },
                            placeholder = { Text("192.168.43.1") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyanBright,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("input_manual_ip")
                        )

                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port") },
                            placeholder = { Text("8989") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyanBright,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_manual_port")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualPassphrase,
                        onValueChange = { manualPassphrase = it },
                        label = { Text("Decryption Passphrase (Optional)") },
                        placeholder = { Text("Leave blank if unencrypted") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberEmeraldBright,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_manual_passphrase")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val port = manualPort.toIntOrNull() ?: 8989
                            if (manualIp.isNotBlank()) {
                                onDownloadManualIp(manualIp.trim(), port, manualPassphrase.trim())
                            }
                        },
                        enabled = manualIp.isNotBlank() && !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_download_connect_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect & Stream", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        // 4. Web Browser File Drop Portal (Receive mode on device)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receiver_web_portal_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (receiverServerStatus is LocalTransferServer.ServerStatus.Running) CyberEmeraldBright else Color.White.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = if (receiverServerStatus is LocalTransferServer.ServerStatus.Running) CyberEmeraldBright else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Web File Drop Server",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Receive files from any browser on LAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }

                        Switch(
                            checked = receiverServerStatus is LocalTransferServer.ServerStatus.Running,
                            onCheckedChange = { active ->
                                if (active) onStartReceiverServer(8990)
                                else onStopReceiverServer()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberEmeraldBright,
                                checkedTrackColor = CyberEmerald.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("switch_receiver_web_server")
                        )
                    }

                    if (receiverServerStatus is LocalTransferServer.ServerStatus.Running) {
                        val dropUrl = "http://${networkInfo.localIp ?: "localhost"}:${receiverServerStatus.port}/"
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dropUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberCyanBright
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Drop URL", dropUrl)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Drop URL copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyanBright, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, dropUrl)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Drop Portal URL"))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = CyberCyanBright, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (receiverServerProgress > 0f && receiverServerProgress < 1f) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Receiving upload: ${(receiverServerProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberEmeraldBright
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            SecureTransferProgressBar(
                                phase = TransferPhase.TRANSFERRING,
                                progress = receiverServerProgress,
                                speedBytesPerSec = downloadSpeed,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // 5. Active Download Speed HUD Card
        if (isDownloading) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("downloading_hud_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF091F1A)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberEmeraldBright)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.5.dp,
                                    color = CyberEmeraldBright
                                )
                                Text(
                                    text = "Streaming P2P Direct Data...",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyberEmeraldBright
                                )
                            }

                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CyberEmeraldBright
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SecureTransferProgressBar(
                            phase = if (downloadProgress < 1f) TransferPhase.TRANSFERRING else TransferPhase.DECRYPTING,
                            progress = downloadProgress,
                            speedBytesPerSec = downloadSpeed,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Transfer Speed:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "${FileUtils.formatBytes(downloadSpeed)}/s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = CyberCyanBright
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Discovered Peer Card for LAN Radar
 */
@Composable
fun DiscoveredPeerItemCard(
    peer: DiscoveredPeer,
    isDownloading: Boolean,
    onDownload: (String?) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var showPassphraseInput by remember { mutableStateOf(peer.encrypted) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovered_peer_${peer.ip}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = CyberEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = peer.fileName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${FileUtils.formatBytes(peer.fileSize)} • ${peer.ip}:${peer.port}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = CyberCyanBright
                        )
                    }
                }

                if (peer.encrypted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFA78BFA))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = Color(0xFFA78BFA), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("AES-256", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA78BFA))
                        }
                    }
                }
            }

            if (showPassphraseInput) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Decryption Passphrase") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberEmeraldBright,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onDownload(passphrase.takeIf { it.isNotBlank() }) },
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download File", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
            }
        }
    }
}

