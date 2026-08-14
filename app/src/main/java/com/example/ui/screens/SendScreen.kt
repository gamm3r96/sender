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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Wifi
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
import com.example.data.TeamKey
import com.example.data.TransferMode
import com.example.p2p.LocalTransferServer
import com.example.ui.components.AnimatedPulseBadge
import com.example.ui.components.CyberSecurityBadge
import com.example.ui.components.QrCodeView
import com.example.ui.components.SafetyNumberBox
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
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
    p2pServerStatus: LocalTransferServer.ServerStatus,
    p2pServerProgress: Float,
    onSelectFile: (Uri, TransferMode, String) -> Unit,
    onSendSecretText: (String, String, TransferMode, String) -> Unit,
    onSwitchMode: (TransferMode) -> Unit,
    onTogglePlay: () -> Unit,
    onSelectChunk: (Int) -> Unit,
    onSetStreamFps: (Int) -> Unit,
    onSelectTeamKey: (TeamKey) -> Unit,
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
                // File Picker Upload Zone
                item {
                    Card(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("file_picker_dropzone"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, CyberEmerald.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(CyberEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload",
                                    tint = CyberEmeraldBright,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select File to Encrypt",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PDFs, Documents, Archives, Photos, or Binaries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

            if (sendState.mode == TransferMode.QR_STREAM) {
                // QR STREAM ANIMATED CAROUSEL
                item {
                    val chunks = sendState.qrChunks
                    val safeIndex = currentChunkIndex.coerceIn(0, (chunks.size - 1).coerceAtLeast(0))
                    val currentQrString = chunks.getOrNull(safeIndex) ?: ""

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Chunk indicator badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedPulseBadge(
                                text = "CHUNK ${safeIndex + 1} OF ${chunks.size}",
                                color = CyberEmeraldBright
                            )

                            Text(
                                text = "Stream Rate: $streamFps FPS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyberCyanBright
                            )
                        }

                        // QR Code Box
                        QrCodeView(
                            qrContent = currentQrString,
                            sizePx = 600,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clickable { isFullScreenQr = true }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls: Prev / Play-Pause / Next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val prev = if (safeIndex - 1 < 0) chunks.size - 1 else safeIndex - 1
                                    onSelectChunk(prev)
                                },
                                modifier = Modifier.testTag("prev_chunk_btn")
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = onTogglePlay,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("toggle_play_stream_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (isStreamPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isStreamPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                onClick = {
                                    val next = (safeIndex + 1) % chunks.size
                                    onSelectChunk(next)
                                },
                                modifier = Modifier.testTag("next_chunk_btn")
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next")
                            }
                        }

                        // FPS Speed Slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Speed: ${streamFps}x", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = streamFps.toFloat(),
                                onValueChange = { onSetStreamFps(it.toInt()) },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("fps_slider")
                            )
                        }

                        // Chunk Dots / Quick Jump Matrix
                        if (chunks.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                itemsIndexed(chunks) { idx, _ ->
                                    val isCurrent = idx == safeIndex
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isCurrent) CyberEmerald else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { onSelectChunk(idx) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // P2P LOCAL NETWORK DIRECT TRANSFER HOST CARD
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("p2p_host_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = CyberCyanBright, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Local P2P Direct Room",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                AnimatedPulseBadge(text = "HOSTING", color = CyberCyanBright)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val p2pQrTicket = sendState.p2pTicket?.let { CryptoManager.createP2PTicketQr(it) } ?: ""
                            QrCodeView(
                                qrContent = p2pQrTicket,
                                sizePx = 512,
                                modifier = Modifier
                                    .size(240.dp)
                                    .clickable { isFullScreenQr = true }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Scan this ticket on the receiver device to start direct LAN transfer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // P2P Live Progress
                            when (p2pServerStatus) {
                                is LocalTransferServer.ServerStatus.Running -> {
                                    Text(
                                        text = "Server active at http://${p2pServerStatus.hostIp}:${p2pServerStatus.port}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = CyberCyanBright
                                    )
                                }
                                is LocalTransferServer.ServerStatus.ClientConnected -> {
                                    Text(
                                        text = "Client downloading: ${p2pServerStatus.clientIp}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyberEmeraldBright
                                    )
                                    LinearProgressIndicator(
                                        progress = { p2pServerProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    )
                                }
                                is LocalTransferServer.ServerStatus.Completed -> {
                                    Text(
                                        text = "Transfer completed successfully!",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CyberEmeraldBright
                                    )
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
