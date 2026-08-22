package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
import com.example.data.TeamKey
import com.example.data.TransferMode
import com.example.p2p.LocalTransferServer
import com.example.p2p.NetworkInfoState
import com.example.ui.components.AnimatedPulseBadge
import com.example.ui.components.AnimatedQrStreamGenerator
import com.example.ui.components.CyberSecurityBadge
import com.example.ui.components.FilePreviewCard
import com.example.ui.components.QrCodeView
import com.example.ui.components.SafetyNumberBox
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
import com.example.util.BatteryInfo
import com.example.util.FileUtils
import com.example.viewmodel.SendPreparationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    sendState: SendPreparationState?,
    teamKeys: List<TeamKey>,
    activeTeamKey: TeamKey?,
    currentChunkIndex: Int,
    isStreamPlaying: Boolean,
    streamFps: Int,
    densityPreset: QrDensityPreset = QrDensityPreset.STANDARD,
    loopCount: Int = 1,
    colorScheme: QrColorScheme = QrColorScheme.HIGH_CONTRAST_MONO,
    errorCorrectionLevel: QrErrorCorrectionLevel = QrErrorCorrectionLevel.LEVEL_M,
    moduleShape: QrModuleShape = QrModuleShape.SQUARE,
    isQrInverted: Boolean = false,
    batteryInfo: BatteryInfo? = null,
    isBatterySaverEnabled: Boolean = true,
    effectiveFps: Int = streamFps,
    p2pServerStatus: LocalTransferServer.ServerStatus,
    p2pServerProgress: Float,
    p2pServerSpeed: Long = 0L,
    networkInfo: NetworkInfoState = NetworkInfoState(),
    p2pDiagnostics: com.example.p2p.P2PConnectionMetrics = com.example.p2p.P2PConnectionMetrics(),
    onOpenDiagnostics: () -> Unit = {},
    onSelectFile: (Uri, TransferMode, String) -> Unit,
    onSendSecretText: (String, String, TransferMode, String) -> Unit,
    onSwitchMode: (TransferMode) -> Unit,
    onTogglePlay: () -> Unit,
    onSelectChunk: (Int) -> Unit,
    onNextChunk: () -> Unit = {},
    onPrevChunk: () -> Unit = {},
    onJumpFirst: () -> Unit = {},
    onJumpLast: () -> Unit = {},
    onSetStreamFps: (Int) -> Unit,
    onSetDensityPreset: (QrDensityPreset) -> Unit = {},
    onSetColorScheme: (QrColorScheme) -> Unit = {},
    onSetErrorCorrectionLevel: (QrErrorCorrectionLevel) -> Unit = {},
    onSetModuleShape: (QrModuleShape) -> Unit = {},
    onToggleInverted: () -> Unit = {},
    onSelectTeamKey: (TeamKey) -> Unit,
    onOpenWifiSettings: () -> Unit = {},
    onOpenHotspotSettings: () -> Unit = {},
    onRefreshNetworkInfo: () -> Unit = {},
    onClearState: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = File, 1 = Secret Note
    var selectedMode by remember { mutableStateOf(TransferMode.QR_STREAM) }
    var secretTitle by remember { mutableStateOf("") }
    var secretContent by remember { mutableStateOf("") }
    var customPassphrase by remember { mutableStateOf("") }
    var useCustomKey by remember { mutableStateOf(false) }
    var showTeamKeyDropdown by remember { mutableStateOf(false) }
    var isFullScreenQr by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val pass = if (useCustomKey) customPassphrase else ""
            onSelectFile(it, selectedMode, pass)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Encrypted Transmission",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Send files & credentials end-to-end via QR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (sendState != null) {
                    IconButton(
                        onClick = onClearState,
                        modifier = Modifier.testTag("clear_send_state_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Mode Selector: QR Stream vs P2P
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf(
                    TransferMode.QR_STREAM to "Air-Gapped QR Stream",
                    TransferMode.P2P_DIRECT to "High-Speed P2P LAN"
                )

                modes.forEach { (mode, label) ->
                    val isSelected = (sendState?.mode ?: selectedMode) == mode
                    Surface(
                        onClick = {
                            selectedMode = mode
                            if (sendState != null) onSwitchMode(mode)
                        },
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (mode == TransferMode.QR_STREAM) Icons.Default.QrCode else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Team Key & Encryption Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyberEmeraldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ENCRYPTION KEY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            onClick = { useCustomKey = !useCustomKey },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (useCustomKey) "Use Team Key" else "Custom Password",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (!useCustomKey) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showTeamKeyDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("team_key_selector_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = CyberEmeraldBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = activeTeamKey?.teamName ?: "Select Team Key",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Text(
                                        text = "Change",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showTeamKeyDropdown,
                                onDismissRequest = { showTeamKeyDropdown = false }
                            ) {
                                teamKeys.forEach { key ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(key.teamName, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Safety: ${key.safetyNumber.take(17)}...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectTeamKey(key)
                                            showTeamKeyDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customPassphrase,
                            onValueChange = { customPassphrase = it },
                            placeholder = { Text("Enter secret passphrase or one-time team PIN") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_passphrase_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // If NO file or secret is actively prepared, show selector tabs
        if (sendState == null) {
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Choose File / Doc", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.FileOpen, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Secret Text / Key", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.TextSnippet, contentDescription = null) }
                    )
                }
            }

            if (selectedTab == 0) {
                // File Picker Upload & Category Selection Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Main Interactive File Dropzone Card
                        Card(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .testTag("file_picker_dropzone"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(2.dp, CyberEmerald.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald.copy(alpha = 0.2f))
                                        .border(1.5.dp, CyberEmeraldBright.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload",
                                        tint = CyberEmeraldBright,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Select File from Local Storage",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to open device file browser for instant AES-256 encryption",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("browse_local_storage_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Browse Storage",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        // Quick Filter Shortcuts for Specific File Categories
                        Text(
                            text = "Or filter by category:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            val categories = listOf(
                                Triple("All Files", "*/*", Icons.Default.FileOpen),
                                Triple("PDF & Docs", "application/pdf", Icons.Default.TextSnippet),
                                Triple("Photos & Images", "image/*", Icons.Default.QrCode),
                                Triple("Text & Code", "text/*", Icons.Default.TextSnippet),
                                Triple("Archives (.zip)", "application/zip", Icons.Default.Folder)
                            )
                            items(categories.size) { idx ->
                                val (label, mime, icon) = categories[idx]
                                Surface(
                                    onClick = { filePickerLauncher.launch(mime) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.testTag("filter_btn_${label.replace(" ", "_").lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = CyberCyanBright,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Security and QR Streaming Technical Summary Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CyberEmeraldBright,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Client-Side Cryptographic Pipeline",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Selected files are encrypted with authenticated AES-256-GCM and sliced into animated QR streams for optical air-gapped reception.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Secret Text / Credential Creator
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = secretTitle,
                            onValueChange = { secretTitle = it },
                            label = { Text("Secret Title / Description") },
                            placeholder = { Text("e.g. AWS Production Token, Master Password") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("secret_title_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = secretContent,
                            onValueChange = { secretContent = it },
                            label = { Text("Confidential Content / Key Payload") },
                            placeholder = { Text("Paste confidential credentials, code snippet, or private notes here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("secret_content_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Quick Presets Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            val presets = listOf(
                                "API Secret" to "sk_live_e2e_prod_98471204891823901",
                                "Wi-Fi Pass" to "WIFI:S:SecureTeamHQ;T:WPA2;P:CyberTeam999;;",
                                "SSH Key Note" to "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...\n-----END RSA PRIVATE KEY-----"
                            )
                            items(presets.size) { idx ->
                                val (title, template) = presets[idx]
                                Surface(
                                    onClick = {
                                        secretTitle = title
                                        secretContent = template
                                    },
                                    shape = RoundedCornerShape(100.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "+ $title",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (secretContent.isNotBlank()) {
                                    val pass = if (useCustomKey) customPassphrase else ""
                                    onSendSecretText(secretTitle, secretContent, selectedMode, pass)
                                }
                            },
                            enabled = secretContent.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("encrypt_secret_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Encrypt & Generate QR Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ACTIVELY PREPARED TRANSMISSION VIEW
        if (sendState != null) {
            // File Metadata Pill
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sendState.fileName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Size: ${FileUtils.formatBytes(sendState.originalSize)}  •  AES-256 Envelope: ${FileUtils.formatBytes(sendState.encryptedPayload?.envelopeBytes?.size?.toLong() ?: 0)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { isFullScreenQr = true },
                            modifier = Modifier.testTag("fullscreen_qr_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full Screen QR",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Interactive Pre-Broadcast Content Preview (Thumbnail / Code Snippet / Hex)
            item {
                FilePreviewCard(
                    fileName = sendState.fileName,
                    mimeType = sendState.mimeType,
                    fileSize = sendState.originalSize,
                    filePath = sendState.sourceFilePath,
                    rawTextPreview = sendState.rawTextPreview
                )
            }

            if (sendState.mode == TransferMode.QR_STREAM) {
                // HIGH CAPACITY ANIMATED QR STREAM GENERATOR
                item {
                    AnimatedQrStreamGenerator(
                        chunks = sendState.qrChunks,
                        currentChunkIndex = currentChunkIndex,
                        isPlaying = isStreamPlaying,
                        streamFps = streamFps,
                        densityPreset = densityPreset,
                        loopCount = loopCount,
                        fileName = sendState.fileName,
                        originalSizeBytes = sendState.originalSize,
                        encryptedSizeBytes = sendState.encryptedPayload?.envelopeBytes?.size?.toLong() ?: 0L,
                        colorScheme = colorScheme,
                        errorCorrectionLevel = errorCorrectionLevel,
                        moduleShape = moduleShape,
                        isQrInverted = isQrInverted,
                        batteryInfo = batteryInfo,
                        isBatterySaverEnabled = isBatterySaverEnabled,
                        effectiveFps = effectiveFps,
                        onTogglePlay = onTogglePlay,
                        onSelectChunk = onSelectChunk,
                        onNextChunk = onNextChunk,
                        onPrevChunk = onPrevChunk,
                        onJumpFirst = onJumpFirst,
                        onJumpLast = onJumpLast,
                        onSetFps = onSetStreamFps,
                        onSetDensityPreset = onSetDensityPreset,
                        onSetColorScheme = onSetColorScheme,
                        onSetErrorCorrectionLevel = onSetErrorCorrectionLevel,
                        onSetModuleShape = onSetModuleShape,
                        onToggleInverted = onToggleInverted
                    )
                }
            } else {
                // P2P / WI-FI / HOTSPOT DIRECT TRANSFER HUB
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("p2p_host_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyanBright.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(CyberCyanBright.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (networkInfo.isHotspotActive) Icons.Default.WifiTethering else Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = CyberCyanBright,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = if (networkInfo.isHotspotActive) "Hotspot Access Point Hub" else "Wi-Fi Direct P2P Room",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (networkInfo.isHotspotActive) "Mobile AP: ${networkInfo.ipAddress}" else "LAN IP: ${networkInfo.ipAddress}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = CyberCyanBright
                                        )
                                    }
                                }
                                AnimatedPulseBadge(text = "HOSTING", color = CyberEmeraldBright)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Settings & Refresh Network Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenHotspotSettings,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyanBright)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hotspot AP", style = MaterialTheme.typography.labelSmall, color = CyberCyanBright)
                                }

                                OutlinedButton(
                                    onClick = onOpenWifiSettings,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberEmeraldBright)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Wi-Fi Settings", style = MaterialTheme.typography.labelSmall, color = CyberEmeraldBright)
                                }

                                IconButton(
                                    onClick = onRefreshNetworkInfo,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // P2P Signal & Diagnostic Health Quick Telemetry Pill
                            Surface(
                                onClick = onOpenDiagnostics,
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_diagnostics_from_send_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = CyberCyanBright,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "P2P RF Health: ${p2pDiagnostics.healthScore}% (${p2pDiagnostics.healthGrade.name})",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Signal: ${p2pDiagnostics.rssiDbm} dBm • Link: ${p2pDiagnostics.linkSpeedMbps} Mbps • RTT: ${p2pDiagnostics.rttPingMs}ms",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                                                color = CyberCyanBright
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = onOpenDiagnostics,
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Metrics", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = CyberEmeraldBright)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive High-Contrast QR Code Ticket
                            val p2pQrTicket = sendState.p2pTicket?.let { CryptoManager.createP2PTicketQr(it) } ?: ""
                            QrCodeView(
                                qrContent = p2pQrTicket,
                                sizePx = 512,
                                modifier = Modifier
                                    .size(240.dp)
                                    .clickable { isFullScreenQr = true }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Scan this ticket on the receiver device to start direct ultra high-speed LAN transfer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Web Browser Direct Download Link Banner
                            val webPortalUrl = "http://${networkInfo.ipAddress}:8989"
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Web Browser Direct Download:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray
                                        )
                                        Text(
                                            text = webPortalUrl,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = CyberCyanBright
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(webPortalUrl))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", tint = CyberCyanBright, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "Download file via local Wi-Fi: $webPortalUrl")
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Download Link"))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share URL", tint = CyberEmeraldBright, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // P2P Live Progress & Speed Meter
                            when (p2pServerStatus) {
                                is LocalTransferServer.ServerStatus.Running -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CyberEmeraldBright)
                                        )
                                        Text(
                                            text = "Awaiting connection on port ${p2pServerStatus.port}...",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = CyberEmeraldBright
                                        )
                                    }
                                }
                                is LocalTransferServer.ServerStatus.ClientConnected -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Transferring to ${p2pServerStatus.clientIp}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = CyberEmeraldBright
                                            )
                                            if (p2pServerSpeed > 0) {
                                                Text(
                                                    text = "${FileUtils.formatBytes(p2pServerSpeed)}/s",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = CyberCyanBright
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { p2pServerProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = CyberEmeraldBright,
                                            trackColor = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${(p2pServerProgress * 100).toInt()}% completed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                                is LocalTransferServer.ServerStatus.Completed -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CyberEmerald.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmeraldBright.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = CyberEmeraldBright, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "Transfer completed successfully!",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = CyberEmeraldBright
                                            )
                                        }
                                    }
                                }
                                is LocalTransferServer.ServerStatus.Error -> {
                                    Text(
                                        text = "Server Error: ${p2pServerStatus.message}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // Cryptographic Safety Number Box
            item {
                sendState.encryptedPayload?.let { payload ->
                    val safetyNumber = CryptoManager.generateSafetyNumber(
                        payload.sha256Original,
                        sendState.customPassphrase.ifBlank { activeTeamKey?.passphraseOrKey ?: "" }
                    )
                    SafetyNumberBox(safetyNumber = safetyNumber)
                }
            }
        }
    }

    // Full Screen QR Presentation Modal
    if (isFullScreenQr && sendState != null) {
        Dialog(
            onDismissRequest = { isFullScreenQr = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = sendState.fileName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (sendState.mode == TransferMode.QR_STREAM) "Chunk ${currentChunkIndex + 1}/${sendState.qrChunks.size}" else "P2P Ticket",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberEmeraldBright
                            )
                        }

                        IconButton(onClick = { isFullScreenQr = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    val contentToDisplay = if (sendState.mode == TransferMode.QR_STREAM) {
                        sendState.qrChunks.getOrNull(currentChunkIndex) ?: ""
                    } else {
                        sendState.p2pTicket?.let { CryptoManager.createP2PTicketQr(it) } ?: ""
                    }

                    QrCodeView(
                        qrContent = contentToDisplay,
                        sizePx = 800,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    Text(
                        text = "Hold receiver camera steady to scan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}
