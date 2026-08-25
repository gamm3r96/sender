package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.p2p.NetworkInfoState
import com.example.p2p.NetworkType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright

/**
 * Key Essential Component: Real-time Network & Air-Gap status bar.
 * Informs users of zero-leakage air-gapped isolation or active local P2P capabilities.
 */
@Composable
fun NetworkStatusBar(
    networkInfo: NetworkInfoState,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAirGapped = !networkInfo.isConnected || networkInfo.networkType == NetworkType.LOCAL_LOOPBACK
    val isWifiOrHotspot = networkInfo.networkType == NetworkType.WIFI || networkInfo.networkType == NetworkType.HOTSPOT

    val bannerBg = if (isAirGapped) {
        CyberEmerald.copy(alpha = 0.08f)
    } else {
        CyberCyan.copy(alpha = 0.08f)
    }

    val bannerBorder = if (isAirGapped) {
        CyberEmerald.copy(alpha = 0.25f)
    } else {
        CyberCyan.copy(alpha = 0.25f)
    }

    val accentColor = if (isAirGapped) CyberEmeraldBright else CyberCyanBright

    Surface(
        color = bannerBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, bannerBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDiagnostics() }
            .testTag("network_status_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isAirGapped) Icons.Default.Security else Icons.Default.Wifi,
                    contentDescription = "Network Status",
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isAirGapped -> "AIR-GAPPED SHIELD: OFFLINE (ZERO LEAKAGE)"
                        networkInfo.isHotspotActive -> "HOTSPOT P2P ACTIVE [${networkInfo.ipAddress}]"
                        networkInfo.wifiSsid != null -> "WIFI P2P READY: ${networkInfo.wifiSsid}"
                        else -> "CONNECTED: ${networkInfo.connectionSummary}"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Diagnostics",
                    tint = accentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DIAGNOSTICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = accentColor
                )
            }
        }
    }
}
