package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay

/**
 * Robust Permission Request wrapper using Accompanist Permissions.
 * Renders clear feedback when granted, denied with rationale, or permanently denied.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionFlow(
    cameraPermissionState: PermissionState,
    onOpenEducationalDialog: () -> Unit,
    modifier: Modifier = Modifier,
    contentWhenGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    var hasAttemptedRequest by remember { mutableStateOf(false) }
    var showGrantedToast by remember { mutableStateOf(false) }

    val isGranted = cameraPermissionState.status.isGranted
    val shouldShowRationale = cameraPermissionState.status.shouldShowRationale

    // Flash a positive feedback toast when permission transitions to granted
    LaunchedEffect(isGranted) {
        if (isGranted && hasAttemptedRequest) {
            showGrantedToast = true
            delay(3000)
            showGrantedToast = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isGranted) {
            // Render Camera Content
            contentWhenGranted()

            // Transient Success Feedback Toast
            AnimatedVisibility(
                visible = showGrantedToast,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color(0xFF0F291E).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, CyberEmeraldBright),
                    shadowElevation = 8.dp,
                    modifier = Modifier.testTag("camera_permission_granted_toast")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyberEmeraldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Camera Access Granted • Optical Engine Ready",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = CyberEmeraldBright
                        )
                    }
                }
            }
        } else {
            // Clear, Informative Permission Denied / Request State
            CameraPermissionDeniedScreen(
                shouldShowRationale = shouldShowRationale,
                hasAttemptedRequest = hasAttemptedRequest,
                onRequestPermission = {
                    hasAttemptedRequest = true
                    cameraPermissionState.launchPermissionRequest()
                },
                onOpenEducationalDialog = onOpenEducationalDialog,
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

/**
 * Screen presenting comprehensive feedback when camera permission is needed or denied.
 */
@Composable
fun CameraPermissionDeniedScreen(
    shouldShowRationale: Boolean,
    hasAttemptedRequest: Boolean,
    onRequestPermission: () -> Unit,
    onOpenEducationalDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPermanentlyDenied = hasAttemptedRequest && !shouldShowRationale

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("camera_permission_denied_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Badge
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    if (isPermanentlyDenied) Color(0xFFEF4444).copy(alpha = 0.12f)
                    else CyberEmerald.copy(alpha = 0.15f)
                )
                .border(
                    width = 1.5.dp,
                    color = if (isPermanentlyDenied) Color(0xFFEF4444).copy(alpha = 0.7f) else CyberEmeraldBright.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPermanentlyDenied) Icons.Default.VideocamOff else Icons.Default.QrCodeScanner,
                contentDescription = "Camera Permission Status",
                tint = if (isPermanentlyDenied) Color(0xFFF87171) else CyberEmeraldBright,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title with clear state feedback
        Text(
            text = when {
                isPermanentlyDenied -> "Camera Permission Blocked"
                shouldShowRationale -> "Camera Access Required"
                else -> "Enable Camera for Optical Transfer"
            },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // User Feedback Description
        Text(
            text = when {
                isPermanentlyDenied -> "Camera permission has been blocked. To scan animated QR streams and encrypted team keys, please allow camera access in Android System Settings."
                shouldShowRationale -> "CipherQR uses your device camera solely for real-time optical barcode and QR stream scanning. No photos or video frames are ever saved, tracked, or sent to the cloud."
                else -> "CipherQR utilizes zero-knowledge air-gapped optical scanning to reconstruct encrypted multi-part files and verify safety numbers without network connectivity."
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp
            ),
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status Feedback Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("camera_permission_feedback_card"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(
                1.dp,
                if (isPermanentlyDenied) Color(0xFFEF4444).copy(alpha = 0.4f)
                else CyberCyan.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPermanentlyDenied) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isPermanentlyDenied) Color(0xFFF87171) else CyberCyanBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isPermanentlyDenied) "PERMISSION STATUS: DENIED" else "OPTICAL PRIVACY GUARANTEE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isPermanentlyDenied) Color(0xFFF87171) else CyberCyanBright
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isPermanentlyDenied) {
                        "The camera permission was declined. Click 'Open App Settings' below, navigate to Permissions, and toggle Camera to 'Allow'."
                    } else {
                        "• 100% on-device optical processing via CameraX\n• Zero data collection, analytics, or cloud transmission\n• Encrypted team key & chunk integrity verified locally"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Primary Action Button
        if (isPermanentlyDenied) {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("camera_permission_open_settings_btn")
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open App Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberEmerald,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("camera_permission_request_btn")
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (shouldShowRationale) "Grant Camera Permission" else "Allow Camera Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Educational Transparency Action
        OutlinedButton(
            onClick = onOpenEducationalDialog,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("camera_permission_why_btn")
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyberCyanBright)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Why CipherQR needs this", fontSize = 13.sp, color = Color(0xFFCBD5E1))
        }
    }
}
