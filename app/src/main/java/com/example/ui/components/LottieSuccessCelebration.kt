package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TransferRecord
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberVioletBright
import com.example.util.FileUtils
import com.example.util.HapticFeedbackHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val angleRad: Float,
    val distanceMax: Float,
    val size: Float,
    val color: Color,
    val particleType: ParticleType,
    val spinSpeed: Float,
    val arcCurve: Float
)

private enum class ParticleType {
    CIRCLE, DIAMOND, STAR, SPARK, RECT
}

/**
 * High-fidelity Lottie-style vector animation featuring:
 * 1. Bouncing elastic circle badge
 * 2. Radiating cyber shockwaves
 * 3. 360-degree confetti particle burst explosion
 * 4. Smooth trim-path SVG checkmark drawing animation
 * 5. Synchronized haptic feedback burst
 */
@Composable
fun LottieSuccessAnimation(
    modifier: Modifier = Modifier,
    diameter: Dp = 140.dp,
    triggerKey: Any? = Unit,
    enableHaptics: Boolean = true,
    primaryColor: Color = CyberEmeraldBright,
    secondaryColor: Color = CyberCyanBright
) {
    val context = LocalContext.current

    // Animation progress drivers
    val badgeScaleAnim = remember { Animatable(0f) }
    val checkmarkProgressAnim = remember { Animatable(0f) }
    val shockwaveAnim = remember { Animatable(0f) }
    val particlesAnim = remember { Animatable(0f) }
    val glowPulseAnim = remember { Animatable(0.5f) }

    // Pre-calculated deterministic particles
    val particles = remember {
        val colors = listOf(
            CyberEmeraldBright,
            CyberCyanBright,
            Color(0xFFFFD700), // Gold
            Color(0xFF00FFA3), // Mint Green
            CyberVioletBright,
            Color.White
        )
        val types = ParticleType.values()
        val random = Random(42)

        List(32) { index ->
            val angle = (index.toFloat() / 32f) * 2f * Math.PI.toFloat() + (random.nextFloat() * 0.2f - 0.1f)
            val distance = 45f + random.nextFloat() * 42f
            ConfettiParticle(
                angleRad = angle,
                distanceMax = distance,
                size = 3f + random.nextFloat() * 5.5f,
                color = colors[random.nextInt(colors.size)],
                particleType = types[random.nextInt(types.size)],
                spinSpeed = (random.nextFloat() - 0.5f) * 720f,
                arcCurve = (random.nextFloat() - 0.5f) * 15f
            )
        }
    }

    LaunchedEffect(triggerKey) {
        // Reset states
        badgeScaleAnim.snapTo(0f)
        checkmarkProgressAnim.snapTo(0f)
        shockwaveAnim.snapTo(0f)
        particlesAnim.snapTo(0f)

        if (enableHaptics) {
            HapticFeedbackHelper.vibrateCelebrationSuccess(context)
        }

        // Orchestrated phase choreography
        launch {
            badgeScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        launch {
            delay(80)
            shockwaveAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        }

        launch {
            delay(100)
            particlesAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        }

        launch {
            delay(220)
            checkmarkProgressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
            )
        }
    }

    // Subtle continuous idle glow
    val infiniteTransition = rememberInfiniteTransition(label = "success_idle_glow")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .size(diameter)
            .testTag("lottie_success_animation"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width * 0.28f

            // 1. Expanding Neon Shockwaves
            if (shockwaveAnim.value > 0f) {
                val wave1Radius = baseRadius + (shockwaveAnim.value * size.width * 0.42f)
                val wave1Alpha = (1f - shockwaveAnim.value).coerceIn(0f, 1f)
                drawCircle(
                    color = primaryColor.copy(alpha = wave1Alpha * 0.7f),
                    radius = wave1Radius,
                    center = center,
                    style = Stroke(width = (4f * (1f - shockwaveAnim.value)).coerceAtLeast(1f).dp.toPx())
                )

                val wave2Progress = (shockwaveAnim.value - 0.15f).coerceAtLeast(0f) / 0.85f
                if (wave2Progress > 0f) {
                    val wave2Radius = baseRadius + (wave2Progress * size.width * 0.36f)
                    val wave2Alpha = (1f - wave2Progress).coerceIn(0f, 1f)
                    drawCircle(
                        color = secondaryColor.copy(alpha = wave2Alpha * 0.6f),
                        radius = wave2Radius,
                        center = center,
                        style = Stroke(width = (2.5f * (1f - wave2Progress)).coerceAtLeast(1f).dp.toPx())
                    )
                }
            }

            // 2. Confetti & Particle Burst Explosion
            if (particlesAnim.value > 0f) {
                val pProg = particlesAnim.value
                val particleAlpha = (1f - (pProg * pProg)).coerceIn(0f, 1f)

                particles.forEach { particle ->
                    val curDist = particle.distanceMax * pProg * (size.width / 140.dp.toPx())
                    val x = center.x + cos(particle.angleRad) * curDist
                    val y = center.y + sin(particle.angleRad) * curDist + (pProg * pProg * 14f) // subtle gravity drop
                    val pCenter = Offset(x, y)
                    val pSize = (particle.size * (1f - pProg * 0.4f)).dp.toPx()
                    val pColor = particle.color.copy(alpha = particleAlpha)

                    rotate(
                        degrees = particle.spinSpeed * pProg,
                        pivot = pCenter
                    ) {
                        when (particle.particleType) {
                            ParticleType.CIRCLE -> {
                                drawCircle(
                                    color = pColor,
                                    radius = pSize / 2f,
                                    center = pCenter
                                )
                            }
                            ParticleType.DIAMOND -> {
                                val diamondPath = Path().apply {
                                    moveTo(pCenter.x, pCenter.y - pSize)
                                    lineTo(pCenter.x + pSize * 0.7f, pCenter.y)
                                    lineTo(pCenter.x, pCenter.y + pSize)
                                    lineTo(pCenter.x - pSize * 0.7f, pCenter.y)
                                    close()
                                }
                                drawPath(diamondPath, pColor)
                            }
                            ParticleType.STAR -> {
                                drawStar(pCenter, pSize, pColor)
                            }
                            ParticleType.SPARK -> {
                                drawLine(
                                    color = pColor,
                                    start = Offset(pCenter.x - pSize, pCenter.y),
                                    end = Offset(pCenter.x + pSize, pCenter.y),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = pColor,
                                    start = Offset(pCenter.x, pCenter.y - pSize),
                                    end = Offset(pCenter.x, pCenter.y + pSize),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            ParticleType.RECT -> {
                                drawRect(
                                    color = pColor,
                                    topLeft = Offset(pCenter.x - pSize / 2f, pCenter.y - pSize / 3f),
                                    size = Size(pSize, pSize * 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Central Badge with Elastic Bounce Scale
            val currentBadgeScale = badgeScaleAnim.value
            if (currentBadgeScale > 0f) {
                val badgeRadius = baseRadius * currentBadgeScale

                // Glowing Outer Glow Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.45f),
                            primaryColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = badgeRadius * 1.55f * (if (currentBadgeScale >= 1f) idlePulse else 1f)
                    ),
                    radius = badgeRadius * 1.55f * (if (currentBadgeScale >= 1f) idlePulse else 1f),
                    center = center
                )

                // Outer Neon Ring
                drawCircle(
                    color = primaryColor,
                    radius = badgeRadius,
                    center = center,
                    style = Stroke(width = 3.5.dp.toPx())
                )

                // Background Disc with Cyber Gradient
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F2E23),
                            Color(0xFF061812)
                        ),
                        startY = center.y - badgeRadius,
                        endY = center.y + badgeRadius
                    ),
                    radius = badgeRadius - 1.5.dp.toPx(),
                    center = center
                )

                // 4. Smooth Trim-Path Animated SVG Checkmark
                val checkProgress = checkmarkProgressAnim.value
                if (checkProgress > 0f) {
                    val checkmarkWidth = badgeRadius * 0.95f
                    val strokeW = 4.5.dp.toPx()

                    // Normalized checkmark coordinates within badge
                    val startPt = Offset(center.x - checkmarkWidth * 0.45f, center.y - checkmarkWidth * 0.05f)
                    val midPt = Offset(center.x - checkmarkWidth * 0.12f, center.y + checkmarkWidth * 0.32f)
                    val endPt = Offset(center.x + checkmarkWidth * 0.48f, center.y - checkmarkWidth * 0.38f)

                    val fullPath = Path().apply {
                        moveTo(startPt.x, startPt.y)
                        lineTo(midPt.x, midPt.y)
                        lineTo(endPt.x, endPt.y)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(fullPath, false)
                    val totalLength = pathMeasure.length

                    val animatedPath = Path()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = totalLength * checkProgress,
                        destination = animatedPath,
                        startWithMoveTo = true
                    )

                    // Draw checkmark glow shadow
                    drawPath(
                        path = animatedPath,
                        color = primaryColor.copy(alpha = 0.5f),
                        style = Stroke(
                            width = strokeW * 1.7f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw solid crisp checkmark
                    drawPath(
                        path = animatedPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White, primaryColor, secondaryColor),
                            start = startPt,
                            end = endPt
                        ),
                        style = Stroke(
                            width = strokeW,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStar(center: Offset, size: Float, color: Color) {
    val path = Path()
    val outerRadius = size
    val innerRadius = size * 0.45f
    val points = 5
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (i * Math.PI / points) - (Math.PI / 2)
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/**
 * Full-screen or modal celebratory completion dialog displayed immediately upon
 * finishing an optical QR stream assembly, LAN P2P transfer, or cryptographic decryption.
 */
@Composable
fun TransferSuccessCelebrationDialog(
    record: TransferRecord,
    onDismiss: () -> Unit,
    onOpenFile: (TransferRecord) -> Unit,
    onSaveToDownloads: (TransferRecord) -> Unit,
    onShare: (TransferRecord) -> Unit,
    onViewDetails: (TransferRecord) -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    BorderStroke(1.5.dp, CyberEmeraldBright.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
                .testTag("transfer_success_celebration_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lottie-Style Animated Celebration Header
                LottieSuccessAnimation(
                    modifier = Modifier.padding(vertical = 4.dp),
                    diameter = 130.dp,
                    triggerKey = record.transferId
                )

                Text(
                    text = "TRANSFER COMPLETED!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    ),
                    color = CyberEmeraldBright,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Cryptographically verified & assembled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File Information Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CyberEmerald.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyberEmeraldBright,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.fileName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${FileUtils.formatBytes(record.originalSize)} • ${record.mimeType.substringAfterLast('/')}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Security & Verification Hash Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CyberCyan.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = CyberCyanBright,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "AES-256-GCM OK",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CyberCyanBright
                                    )
                                }
                            }

                            Text(
                                text = "SHA-256: ${record.sha256Checksum.take(12)}…",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onOpenFile(record)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldBright),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("celebration_open_file_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Open",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = { onShare(record) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyanBright),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("celebration_share_file_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Share",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSaveToDownloads(record) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("celebration_save_downloads_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Downloads", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            onViewDetails(record)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("celebration_details_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Details", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
