package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet

@Composable
fun CyberSecurityBadge(
    text: String = "AES-256-GCM E2EE",
    modifier: Modifier = Modifier
) {
    Surface(
        color = CyberEmerald.copy(alpha = 0.15f),
        shape = RoundedCornerShape(100.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f)),
        modifier = modifier.testTag("cyber_security_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CyberEmeraldBright)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = CyberEmeraldBright
                )
            )
        }
    }
}

@Composable
fun GlowingSecurityCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CyberEmerald,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor.copy(alpha = 0.6f),
                        borderColor.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        content()
    }
}

@Composable
fun QrCodeView(
    qrContent: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 512,
    colorScheme: com.example.data.QrColorScheme = com.example.data.QrColorScheme.HIGH_CONTRAST_MONO,
    errorCorrectionLevel: com.example.data.QrErrorCorrectionLevel = com.example.data.QrErrorCorrectionLevel.LEVEL_M,
    moduleShape: com.example.data.QrModuleShape = com.example.data.QrModuleShape.SQUARE,
    isInverted: Boolean = false,
    darkColor: Int? = null,
    lightColor: Int? = null
) {
    var bitMatrix by remember { mutableStateOf<com.google.zxing.common.BitMatrix?>(null) }

    LaunchedEffect(qrContent, sizePx, errorCorrectionLevel) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            if (qrContent.isNotEmpty()) {
                try {
                    val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java).apply {
                        put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8")
                        put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel.zxingLevel)
                        put(com.google.zxing.EncodeHintType.MARGIN, 1)
                    }
                    val writer = com.google.zxing.qrcode.QRCodeWriter()
                    bitMatrix = writer.encode(qrContent, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, hints)
                } catch (_: Exception) {
                    bitMatrix = null
                }
            } else {
                bitMatrix = null
            }
        }
    }

    // Determine actual active colors
    val actualDark = if (isInverted) {
        darkColor?.let { Color(it) } ?: colorScheme.composeLightColor
    } else {
        darkColor?.let { Color(it) } ?: colorScheme.composeDarkColor
    }

    val actualLight = if (isInverted) {
        lightColor?.let { Color(it) } ?: colorScheme.composeDarkColor
    } else {
        lightColor?.let { Color(it) } ?: colorScheme.composeLightColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(actualLight)
            .border(1.5.dp, actualDark.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val currentMatrix = bitMatrix
        if (currentMatrix != null) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag("qr_code_image")
                    .drawWithCache {
                        val matrixWidth = currentMatrix.width
                        val matrixHeight = currentMatrix.height
                        val scale = minOf(size.width / matrixWidth, size.height / matrixHeight)
                        val cornerRadius = scale * moduleShape.cornerRadiusFraction
                        val isDots = moduleShape == com.example.data.QrModuleShape.DOTS

                        val path = androidx.compose.ui.graphics.Path()
                        for (y in 0 until matrixHeight) {
                            for (x in 0 until matrixWidth) {
                                if (currentMatrix.get(x, y)) {
                                    val left = x * scale
                                    val top = y * scale
                                    val right = (x + 1) * scale
                                    val bottom = (y + 1) * scale

                                    if (isDots) {
                                        val radius = scale * 0.45f
                                        val centerX = left + scale / 2f
                                        val centerY = top + scale / 2f
                                        path.addOval(
                                            androidx.compose.ui.geometry.Rect(
                                                centerX - radius,
                                                centerY - radius,
                                                centerX + radius,
                                                centerY + radius
                                            )
                                        )
                                    } else if (cornerRadius > 0f) {
                                        path.addRoundRect(
                                            androidx.compose.ui.geometry.RoundRect(
                                                left = left,
                                                top = top,
                                                right = right,
                                                bottom = bottom,
                                                radiusX = cornerRadius,
                                                radiusY = cornerRadius
                                            )
                                        )
                                    } else {
                                        path.addRect(
                                            androidx.compose.ui.geometry.Rect(
                                                left = left,
                                                top = top,
                                                right = right,
                                                bottom = bottom
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        onDrawBehind {
                            drawRect(color = actualLight, size = size)
                            drawPath(path = path, color = actualDark)
                        }
                    }
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generating QR Code...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SafetyNumberBox(
    safetyNumber: String,
    title: String = "Verification Safety Number",
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("safety_number_box")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberEmeraldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(safetyNumber)) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_safety_number_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy safety number",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = safetyNumber.ifBlank { "VERIFY-FINGERPRINT-SAS-00000" },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Compare this 6-block code with your team member to confirm 100% MITM-free encryption.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AnimatedPulseBadge(
    text: String,
    color: Color = CyberEmeraldBright,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
