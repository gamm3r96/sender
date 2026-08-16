package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransferRecord
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileDetailDialog
import com.example.ui.screens.ReceiveScreen
import com.example.ui.screens.SendScreen
import com.example.ui.screens.TeamKeysScreen
import com.example.ui.screens.VaultScreen
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
    VAULT("Vault", Icons.Default.Folder, "nav_vault"),
    TEAMS("Keys", Icons.Default.Key, "nav_teams")
}

class MainActivity : ComponentActivity() {
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
    val snackbarHostState = remember { SnackbarHostState() }
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    val teamKeys by viewModel.teamKeys.collectAsStateWithLifecycle()
    val activeTeamKey by viewModel.activeTeamKey.collectAsStateWithLifecycle()
    val sendState by viewModel.sendState.collectAsStateWithLifecycle()
    val currentChunkIndex by viewModel.currentChunkIndex.collectAsStateWithLifecycle()
    val isStreamPlaying by viewModel.isStreamPlaying.collectAsStateWithLifecycle()
    val streamFps by viewModel.streamFps.collectAsStateWithLifecycle()
    val p2pServerStatus by viewModel.p2pServerStatus.collectAsStateWithLifecycle()
    val p2pServerProgress by viewModel.p2pServerProgress.collectAsStateWithLifecycle()

    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scannedP2PTicket by viewModel.scannedP2PTicket.collectAsStateWithLifecycle()
    val isDownloadingP2P by viewModel.isDownloadingP2P.collectAsStateWithLifecycle()
    val p2pDownloadProgress by viewModel.p2pDownloadProgress.collectAsStateWithLifecycle()
    val pendingDecryption by viewModel.pendingDecryption.collectAsStateWithLifecycle()
    val streamTimeoutSeconds by viewModel.streamTimeoutSeconds.collectAsStateWithLifecycle()
    val streamRemainingSeconds by viewModel.streamRemainingSeconds.collectAsStateWithLifecycle()
    val lastTimeoutNotice by viewModel.lastTimeoutNotice.collectAsStateWithLifecycle()
    val inspectedRecord by viewModel.inspectedRecord.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

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
                            contentDescription = "CipherQR Logo",
                            tint = CyberEmeraldBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CipherQR",
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
                    ThemeToggleIconButton(
                        themeMode = themeMode,
                        onToggle = { viewModel.cycleThemeMode() },
                        modifier = Modifier.padding(end = 12.dp)
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
                        onNavigateToSend = { currentDestination = AppDestination.SEND },
                        onNavigateToReceive = { currentDestination = AppDestination.RECEIVE },
                        onNavigateToVault = { currentDestination = AppDestination.VAULT },
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
                        p2pServerStatus = p2pServerStatus,
                        p2pServerProgress = p2pServerProgress,
                        onSelectFile = { uri, mode, pass ->
                            viewModel.prepareFileForSending(context, uri, mode, pass)
                        },
                        onSendSecretText = { title, content, mode, pass ->
                            viewModel.prepareSecretTextForSending(title, content, mode, pass)
                        },
                        onSwitchMode = { viewModel.switchSendMode(it) },
                        onTogglePlay = { viewModel.toggleStreamPlaying() },
                        onSelectChunk = { viewModel.selectChunkIndex(it) },
                        onSetStreamFps = { viewModel.setStreamFps(it) },
                        onSelectTeamKey = { viewModel.setActiveTeamKey(it) },
                        onClearState = { viewModel.clearSendState() }
                    )
                }

                AppDestination.RECEIVE -> {
                    ReceiveScreen(
                        scanProgress = scanProgress,
                        scannedP2PTicket = scannedP2PTicket,
                        isDownloadingP2P = isDownloadingP2P,
                        p2pDownloadProgress = p2pDownloadProgress,
                        pendingDecryption = pendingDecryption,
                        streamTimeoutSeconds = streamTimeoutSeconds,
                        streamRemainingSeconds = streamRemainingSeconds,
                        lastTimeoutNotice = lastTimeoutNotice,
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
                        onDismissPendingDecryption = { viewModel.dismissPendingDecryption() }
                    )
                }

                AppDestination.VAULT -> {
                    VaultScreen(
                        transfers = transfers,
                        onInspectTransfer = { viewModel.inspectRecord(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteTransfer = { viewModel.deleteRecord(it) }
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
