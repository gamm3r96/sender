package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberVioletBright
import com.example.util.FileUtils

enum class TransferPhase {
    IDLE,
    ENCRYPTING,
    TRANSFERRING,
    DECRYPTING,
    COMPLETED,
    ERROR
}

@Composable
fun SecureTransferProgressBar(
    phase: TransferPhase,
    progress: Float, // 0.0 to 1.0
    speedBytesPerSec: Long = 0L,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "secure_transfer_pulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "animated_progress"
    )

    val phaseColor = when (phase) {
        TransferPhase.IDLE -> Color.Gray
        TransferPhase.ENCRYPTING -> CyberVioletBright
        TransferPhase.TRANSFERRING -> CyberCyanBright
        TransferPhase.DECRYPTING -> CyberAmber
        TransferPhase.COMPLETED -> CyberEmeraldBright
        TransferPhase.ERROR -> Color.Red
    }

    val phaseIcon = when (phase) {
        TransferPhase.IDLE -> Icons.Default.Sync
        TransferPhase.ENCRYPTING -> Icons.Default.Lock
        TransferPhase.TRANSFERRING -> Icons.Default.Sync
        TransferPhase.DECRYPTING -> Icons.Default.LockOpen
        TransferPhase.COMPLETED -> Icons.Default.CheckCircle
        TransferPhase.ERROR -> Icons.Default.Sync
    }

    val phaseText = when (phase) {
        TransferPhase.IDLE -> "Standing By..."
        TransferPhase.ENCRYPTING -> "Securing Payload: AES-256 GCM..."
        TransferPhase.TRANSFERRING -> "Transmitting Data..."
        TransferPhase.DECRYPTING -> "Decrypting Payload..."
        TransferPhase.COMPLETED -> "Transfer Complete."
        TransferPhase.ERROR -> "Transfer Failed."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .border(1.dp, phaseColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: Status & Icon
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
                    imageVector = phaseIcon,
                    contentDescription = null,
                    tint = phaseColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = phaseColor
                )
            }

            AnimatedVisibility(visible = phase == TransferPhase.TRANSFERRING && speedBytesPerSec > 0) {
                Text(
                    text = "${FileUtils.formatBytes(speedBytesPerSec)}/s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = CyberCyan
                )
            }
            
            AnimatedVisibility(visible = phase != TransferPhase.TRANSFERRING && phase != TransferPhase.IDLE) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = phaseColor
                )
            }
        }

        // The Custom Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Color(0xFF1E293B))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val corner = height.toPx() / 2f
                val activeWidth = size.width * animatedProgress

                // If doing crypto, show an indeterminate pulse/stripe effect inside the progress area
                if (phase == TransferPhase.ENCRYPTING || phase == TransferPhase.DECRYPTING) {
                    // Draw base progress
                    drawRoundRect(
                        color = phaseColor.copy(alpha = pulseAlpha),
                        size = Size(activeWidth, size.height),
                        cornerRadius = CornerRadius(corner, corner)
                    )

                    // Draw moving stripes for encryption/decryption processing
                    val stripeWidth = 40.dp.toPx()
                    val offset = (System.currentTimeMillis() % 1000) / 1000f * stripeWidth
                    
                    val gradient = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        start = Offset(activeWidth * shimmerOffset - 100f, 0f),
                        end = Offset(activeWidth * shimmerOffset + 100f, size.height)
                    )

                    drawRoundRect(
                        brush = gradient,
                        size = Size(activeWidth, size.height),
                        cornerRadius = CornerRadius(corner, corner)
                    )
                } else {
                    // Normal Transfer Progress
                    drawRoundRect(
                        color = phaseColor,
                        size = Size(activeWidth, size.height),
                        cornerRadius = CornerRadius(corner, corner)
                    )

                    // Transfer Shimmer Effect
                    if (phase == TransferPhase.TRANSFERRING) {
                        val gradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            startX = size.width * shimmerOffset - 150f,
                            endX = size.width * shimmerOffset + 150f
                        )
                        drawRoundRect(
                            brush = gradient,
                            size = Size(activeWidth, size.height),
                            cornerRadius = CornerRadius(corner, corner)
                        )
                    }
                }
            }
        }

        // Sub-text showing actual percent for transferring state
        if (phase == TransferPhase.TRANSFERRING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transfer Progress",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.Gray
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    color = phaseColor
                )
            }
        }
    }
}
