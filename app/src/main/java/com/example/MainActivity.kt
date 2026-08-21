package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.BiometricAuthManager
import com.example.data.TransferRecord
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileDetailDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReceiveScreen
import com.example.ui.screens.SendScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TeamKeysScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.CipherViewModel

enum class AppDestination(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Home", Icons.Default.Shield, "nav_dashboard"),
    SEND("Send", Icons.Default.ArrowUpward, "nav_send"),
    RECEIVE("Scan", Icons.Default.QrCodeScanner, "nav_receive"),
    HISTORY("Vault", Icons.Default.History, "nav_history"),
    TEAMS("Keys", Icons.Default.Key, "nav_teams"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings"),
    ABOUT("About", Icons.Default.Info, "nav_about")
}

class MainActivity : FragmentActivity() {
    private val viewModel: CipherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode) {
                CipherApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CipherApp(viewModel: CipherViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()
    val biometricStatus by viewModel.biometricStatus.collectAsStateWithLifecycle()
    val hasCustomPasscode by viewModel.hasCustomPasscode.collectAsStateWithLifecycle()

    var biometricErrorMessage by remember { mutableStateOf<String?>(null) }

    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    val teamKeys by viewModel.teamKeys.collectAsStateWithLifecycle()
    val activeTeamKey by viewModel.activeTeamKey.collectAsStateWithLifecycle()
    val sendState by viewModel.sendState.collectAsStateWithLifecycle()
    val currentChunkIndex by viewModel.currentChunkIndex.collectAsStateWithLifecycle()
    val isStreamPlaying by viewModel.isStreamPlaying.collectAsStateWithLifecycle()
    val streamFps by viewModel.streamFps.collectAsStateWithLifecycle()
    val densityPreset by viewModel.densityPreset.collectAsStateWithLifecycle()
    val loopCount by viewModel.loopCount.collectAsStateWithLifecycle()
    val p2pServerStatus by viewModel.p2pServerStatus.collectAsStateWithLifecycle()
    val p2pServerProgress by viewModel.p2pServerProgress.collectAsStateWithLifecycle()
    val p2pServerSpeed by viewModel.p2pServerSpeed.collectAsStateWithLifecycle()

    val networkInfo by viewModel.networkInfo.collectAsStateWithLifecycle()
    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val isScanningPeers by viewModel.isScanningPeers.collectAsStateWithLifecycle()
    val receiverServerStatus by viewModel.receiverServerStatus.collectAsStateWithLifecycle()
    val receiverServerProgress by viewModel.receiverServerProgress.collectAsStateWithLifecycle()

    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scannedP2PTicket by viewModel.scannedP2PTicket.collectAsStateWithLifecycle()
    val isDownloadingP2P by viewModel.isDownloadingP2P.collectAsStateWithLifecycle()
    val p2pDownloadProgress by viewModel.p2pDownloadProgress.collectAsStateWithLifecycle()
    val p2pDownloadSpeed by viewModel.p2pDownloadSpeed.collectAsStateWithLifecycle()
    val pendingDecryption by viewModel.pendingDecryption.collectAsStateWithLifecycle()
    val streamTimeoutSeconds by viewModel.streamTimeoutSeconds.collectAsStateWithLifecycle()
    val streamRemainingSeconds by viewModel.streamRemainingSeconds.collectAsStateWithLifecycle()
    val lastTimeoutNotice by viewModel.lastTimeoutNotice.collectAsStateWithLifecycle()
    val inspectedRecord by viewModel.inspectedRecord.collectAsStateWithLifecycle()

    val qrColorScheme by viewModel.qrColorScheme.collectAsStateWithLifecycle()
    val qrErrorCorrectionLevel by viewModel.qrErrorCorrectionLevel.collectAsStateWithLifecycle()
    val qrModuleShape by viewModel.qrModuleShape.collectAsStateWithLifecycle()
    val isQrInverted by viewModel.isQrInverted.collectAsStateWithLifecycle()
    val scannerContrastMode by viewModel.scannerContrastMode.collectAsStateWithLifecycle()
    val isScreenBrightnessBoostEnabled by viewModel.isScreenBrightnessBoostEnabled.collectAsStateWithLifecycle()

    fun triggerBiometricAuth() {
        biometricErrorMessage = null
        activity?.let { act ->
            BiometricAuthManager.authenticate(
                activity = act,
                title = "Unlock CipherQR Vault",
                subtitle = "Verify fingerprint or face unlock to access encrypted files and keys",
                negativeButtonText = "Cancel",
                onSuccess = {
                    biometricErrorMessage = null
                    com.example.util.HapticFeedbackHelper.vibrateBiometricSuccess(context)
                    viewModel.unlockApp()
                },
                onError = { code, msg ->
                    if (code != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        biometricErrorMessage = msg
                        com.example.util.HapticFeedbackHelper.vibratePassphraseError(context)
                    }
                },
                onFailed = {
                    biometricErrorMessage = "Biometric not recognized. Please retry or enter PIN."
                    com.example.util.HapticFeedbackHelper.vibratePassphraseError(context)
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // When Vault is Locked by Biometric Security
    if (isBiometricEnabled && isAppLocked) {
        BiometricLockScreen(
            biometricStatus = biometricStatus,
            onTriggerBiometricAuth = { triggerBiometricAuth() },
            onVerifyPasscode = { pin -> viewModel.verifyPasscode(pin) },
            errorMessage = biometricErrorMessage
        )
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Sender Logo",
                                tint = CyberEmeraldBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Sender",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentDestination.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.shareApk(context) },
                            modifier = Modifier.testTag("top_bar_share_apk_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = "Share APK",
                                tint = CyberEmeraldBright
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val shareText = """
                                        🔒 Sender — Zero-Trust Optical Air-Gapped File & Secrets Transfer
                                        
                                        Military-grade AES-256-GCM + PBKDF2 optical QR stream generator and decoder for high-security air-gapped data transfers.
                                        
                                        👨‍💻 Lead Developer: Elvis Gatwara (elvisgatwara@gmail.com)
                                        🌐 Developer Portfolio: https://elvis-gatwara.vercel.app
                                        📱 App Access: https://ais-pre-kk2pxe7rlwk26tksqnmrfu-804296692629.europe-west2.run.app
                                    """.trimIndent()
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Sender — Air-Gapped Encrypted File Transfer")
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Sender App"))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Unable to share app", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("top_bar_share_app_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share App",
                                tint = CyberCyanBright
                            )
                        }

                        ThemeToggleIconButton(
                            themeMode = themeMode,
                            onToggle = { viewModel.cycleThemeMode() },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    AppDestination.entries.forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberEmeraldBright,
                                selectedTextColor = CyberEmeraldBright,
                                indicatorColor = CyberEmerald.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(destination.tag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentDestination) {
                    AppDestination.DASHBOARD -> {
                        DashboardScreen(
                            transfers = transfers,
                            activeTeamName = activeTeamKey?.teamName,
                            themeMode = themeMode,
                            onSelectThemeMode = { viewModel.setThemeMode(it) },
                            isBiometricEnabled = isBiometricEnabled,
                            biometricStatus = biometricStatus,
                            onToggleBiometricEnabled = { viewModel.setBiometricEnabled(it) },
                            onLockVault = { viewModel.lockVault() },
                            onSetPasscode = { viewModel.setAppPasscode(it) },
                            hasCustomPasscode = hasCustomPasscode,
                            onNavigateToSend = { currentDestination = AppDestination.SEND },
                            onNavigateToReceive = { currentDestination = AppDestination.RECEIVE },
                            onNavigateToHistory = { currentDestination = AppDestination.HISTORY },
                            onNavigateToTeams = { currentDestination = AppDestination.TEAMS },
                            onInspectTransfer = { viewModel.inspectRecord(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }

                    AppDestination.SEND -> {
                        SendScreen(
                            sendState = sendState,
                            teamKeys = teamKeys,
                            activeTeamKey = activeTeamKey,
                            currentChunkIndex = currentChunkIndex,
                            isStreamPlaying = isStreamPlaying,
                            streamFps = streamFps,
                            densityPreset = densityPreset,
                            loopCount = loopCount,
                            colorScheme = qrColorScheme,
                            errorCorrectionLevel = qrErrorCorrectionLevel,
                            moduleShape = qrModuleShape,
                            isQrInverted = isQrInverted,
                            p2pServerStatus = p2pServerStatus,
                            p2pServerProgress = p2pServerProgress,
                            p2pServerSpeed = p2pServerSpeed,
                            networkInfo = networkInfo,
                            onSelectFile = { uri, mode, pass ->
                                viewModel.prepareFileForSending(context, uri, mode, pass)
                            },
                            onSendSecretText = { title, content, mode, pass ->
                                viewModel.prepareSecretTextForSending(title, content, mode, pass)
                            },
                            onSwitchMode = { viewModel.switchSendMode(it) },
                            onTogglePlay = { viewModel.toggleStreamPlaying() },
                            onSelectChunk = { viewModel.selectChunkIndex(it) },
                            onNextChunk = { viewModel.stepNextChunk() },
                            onPrevChunk = { viewModel.stepPrevChunk() },
                            onJumpFirst = { viewModel.jumpToFirstChunk() },
                            onJumpLast = { viewModel.jumpToLastChunk() },
                            onSetStreamFps = { viewModel.setStreamFps(it) },
                            onSetDensityPreset = { viewModel.setDensityPreset(it) },
                            onSetColorScheme = { viewModel.setQrColorScheme(it) },
                            onSetErrorCorrectionLevel = { viewModel.setQrErrorCorrectionLevel(it) },
                            onSetModuleShape = { viewModel.setQrModuleShape(it) },
                            onToggleInverted = { viewModel.toggleQrInverted() },
                            onSelectTeamKey = { viewModel.setActiveTeamKey(it) },
                            onClearState = { viewModel.clearSendState() },
                            onOpenWifiSettings = { viewModel.openWifiSettings(context) },
                            onOpenHotspotSettings = { viewModel.openHotspotSettings(context) },
                            onRefreshNetworkInfo = { viewModel.refreshNetworkInfo() }
                        )
                    }

                    AppDestination.RECEIVE -> {
                        ReceiveScreen(
                            scanProgress = scanProgress,
                            scannedP2PTicket = scannedP2PTicket,
                            isDownloadingP2P = isDownloadingP2P,
                            p2pDownloadProgress = p2pDownloadProgress,
                            p2pDownloadSpeed = p2pDownloadSpeed,
                            pendingDecryption = pendingDecryption,
                            streamTimeoutSeconds = streamTimeoutSeconds,
                            streamRemainingSeconds = streamRemainingSeconds,
                            lastTimeoutNotice = lastTimeoutNotice,
                            isHapticEnabled = isHapticEnabled,
                            networkInfo = networkInfo,
                            discoveredPeers = discoveredPeers,
                            isScanningPeers = isScanningPeers,
                            receiverServerStatus = receiverServerStatus,
                            receiverServerProgress = receiverServerProgress,
                            onToggleHaptic = { viewModel.setHapticEnabled(it) },
                            onTestHaptic = { viewModel.testHapticPattern(it, context) },
                            onQrCodeDetected = { rawText ->
                                viewModel.handleScannedQr(rawText, context)
                            },
                            onDownloadP2P = { ticket ->
                                viewModel.downloadAndDecryptP2PTicket(ticket, context)
                            },
                            onDismissP2P = { viewModel.dismissP2PTicket() },
                            onResetScan = { viewModel.resetScanProgress() },
                            onSetStreamTimeoutSeconds = { viewModel.setStreamTimeoutSeconds(it) },
                            onDismissTimeoutNotice = { viewModel.dismissTimeoutNotice() },
                            onGalleryImageSelected = { bitmap ->
                                viewModel.scanFromGalleryBitmap(bitmap, context)
                            },
                            onDecryptPendingPassphrase = { pass ->
                                viewModel.decryptPendingWithPassphrase(pass, context)
                            },
                            onDismissPendingDecryption = { viewModel.dismissPendingDecryption() },
                            onScanLanPeers = { viewModel.scanLanForPeers(context) },
                            onDownloadDiscoveredPeer = { peer, pass ->
                                viewModel.downloadDiscoveredPeer(peer, pass, context)
                            },
                            onDownloadManualIp = { ip, port, pass ->
                                viewModel.downloadManualIp(ip, port, pass, context)
                            },
                            onStartReceiverServer = { port ->
                                viewModel.startReceiverFileDropServer(port, context)
                            },
                            onStopReceiverServer = { viewModel.stopReceiverFileDropServer() },
                            onOpenWifiSettings = { viewModel.openWifiSettings(context) },
                            onOpenHotspotSettings = { viewModel.openHotspotSettings(context) },
                            onRefreshNetworkInfo = { viewModel.refreshNetworkInfo() }
                        )
                    }

                    AppDestination.HISTORY -> {
                        HistoryScreen(
                            transfers = transfers,
                            onInspectTransfer = { viewModel.inspectRecord(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDeleteTransfer = { viewModel.deleteRecord(it) },
                            onDeleteTransfers = { viewModel.deleteRecords(it) }
                        )
                    }

                    AppDestination.TEAMS -> {
                        TeamKeysScreen(
                            teamKeys = teamKeys,
                            activeTeamKey = activeTeamKey,
                            onSelectActiveKey = { viewModel.setActiveTeamKey(it) },
                            onSetDefaultKey = { viewModel.setDefaultTeamKey(it) },
                            onAddNewKey = { name, pass ->
                                viewModel.addTeamKey(name, pass)
                            },
                            onDeleteKey = { viewModel.deleteTeamKey(it) }
                        )
                    }

                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            themeMode = themeMode,
                            onSelectThemeMode = { viewModel.setThemeMode(it) },
                            qrColorScheme = qrColorScheme,
                            onSelectQrColorScheme = { viewModel.setQrColorScheme(it) },
                            qrErrorCorrectionLevel = qrErrorCorrectionLevel,
                            onSelectQrErrorCorrectionLevel = { viewModel.setQrErrorCorrectionLevel(it) },
                            qrModuleShape = qrModuleShape,
                            onSelectQrModuleShape = { viewModel.setQrModuleShape(it) },
                            isQrInverted = isQrInverted,
                            onToggleQrInverted = { viewModel.toggleQrInverted() },
                            scannerContrastMode = scannerContrastMode,
                            onSelectScannerContrastMode = { viewModel.setScannerContrastMode(it) },
                            isScreenBrightnessBoostEnabled = isScreenBrightnessBoostEnabled,
                            onToggleScreenBrightnessBoost = { viewModel.setScreenBrightnessBoostEnabled(it) },
                            isBiometricEnabled = isBiometricEnabled,
                            biometricStatus = biometricStatus,
                            onToggleBiometricEnabled = { viewModel.setBiometricEnabled(it) },
                            onLockVault = { viewModel.lockVault() },
                            hasCustomPasscode = hasCustomPasscode,
                            onSetPasscode = { viewModel.setAppPasscode(it) },
                            isHapticEnabled = isHapticEnabled,
                            onToggleHaptic = { viewModel.setHapticEnabled(it) },
                            onTestHaptic = { viewModel.testHapticPattern(it, context) },
                            streamTimeoutSeconds = streamTimeoutSeconds,
                            onSetStreamTimeoutSeconds = { viewModel.setStreamTimeoutSeconds(it) },
                            densityPreset = densityPreset,
                            onSelectDensityPreset = { viewModel.setDensityPreset(it) },
                            streamFps = streamFps,
                            onSetStreamFps = { viewModel.setStreamFps(it) },
                            teamKeys = teamKeys,
                            activeTeamKey = activeTeamKey,
                            onSelectTeamKey = { viewModel.setActiveTeamKey(it) },
                            transfers = transfers,
                            onPurgeAllTransfers = { viewModel.purgeAllTransfers() },
                            onClearCache = { viewModel.clearAppCache(context) },
                            onShareApk = { viewModel.shareApk(context) },
                            onNavigateToAbout = { currentDestination = AppDestination.ABOUT }
                        )
                    }

                    AppDestination.ABOUT -> {
                        AboutScreen(
                            onShareApkClick = { viewModel.shareApk(context) }
                        )
                    }
                }

                // Inspect Decrypted File / Secret Dialog
                inspectedRecord?.let { record ->
                    FileDetailDialog(
                        record = record,
                        onDismiss = { viewModel.inspectRecord(null) },
                        onSaveToDownloads = { viewModel.saveToDownloads(it, context) },
                        onShare = { viewModel.shareRecord(it, context) },
                        onDelete = { viewModel.deleteRecord(it) }
                    )
                }
            }
        }
    }
}

