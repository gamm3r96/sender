package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Modes for the Dynamic Camera Guidance Grid Overlay
 */
enum class GuidanceGridMode(val label: String, val description: String) {
    PRECISION_3X3("Rule of 3rds", "3x3 Optical alignment grid with crosshair"),
    MATRIX_4X4("4x4 Matrix", "Fine density matrix for high-speed dynamic streams"),
    HORIZON_LEVEL("Horizon Level", "Real-time tilt and inclination balance guide"),
    FULL_ASSIST("Full HUD", "All gridlines, alignment reticle, and horizon level"),
    OFF("Grid Off", "Clean unobstructed camera viewfinder")
}

/**
 * Real-time Device Tilt and Level Angles State
 */
data class DeviceAlignmentState(
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val isLevel: Boolean = false,
    val alignmentQualityFraction: Float = 1f
)

/**
 * Remembers and tracks device inclination angles using hardware accelerometer.
 */
@Composable
fun rememberDeviceAlignmentState(): DeviceAlignmentState {
    val context = LocalContext.current
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val ax = event.values[0]
                    val ay = event.values[1]
                    val az = event.values[2]

                    // Calculate pitch and roll in degrees
                    val calculatedPitch = (atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble())) * (180.0 / Math.PI)).toFloat()
                    val calculatedRoll = (atan2(-ax.toDouble(), az.toDouble()) * (180.0 / Math.PI)).toFloat()

                    // Low-pass filter for smooth motion
                    pitch = pitch * 0.85f + calculatedPitch * 0.15f
                    roll = roll * 0.85f + calculatedRoll * 0.15f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Determine if device is level enough for optimal QR optical geometry (within 6 degrees of upright or flat plane)
    val rollDev = abs(roll)
    val pitchDev = abs(pitch)
    // Check if flat (looking down at screen: az approx 9.8) or upright (looking at monitor: ay approx 9.8)
    val isLevelFlat = rollDev <= 7f && pitchDev <= 7f
    val isLevelUpright = rollDev <= 7f && abs(pitchDev - 90f) <= 12f
    val isLevel = isLevelFlat || isLevelUpright

    val totalDeviation = if (isLevelUpright) {
        sqrt(rollDev * rollDev + (abs(pitchDev - 90f)) * (abs(pitchDev - 90f)))
    } else {
        sqrt(rollDev * rollDev + pitchDev * pitchDev)
    }

    val quality = (1f - (totalDeviation / 25f)).coerceIn(0.1f, 1f)

    return DeviceAlignmentState(
        pitchDegrees = pitch,
        rollDegrees = roll,
        isLevel = isLevel,
        alignmentQualityFraction = quality
    )
}

/**
 * Dynamic Guidance Grid Composable overlaid directly inside the camera viewfinder box.
 * Renders rule-of-thirds / matrix gridlines, corner brackets with micro-scales,
 * animated scanning laser, real-time tilt horizon, and alignment status pill.
 */
@Composable
fun DynamicCameraGuidanceGrid(
    gridMode: GuidanceGridMode,
    isStreamActive: Boolean,
    isBurstCapturing: Boolean,
    modifier: Modifier = Modifier,
    alignmentState: DeviceAlignmentState = rememberDeviceAlignmentState()
) {
    if (gridMode == GuidanceGridMode.OFF) return

    val infiniteTransition = rememberInfiniteTransition(label = "grid_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "grid_alpha"
    )

    val animatedHorizonRoll by animateFloatAsState(
        targetValue = alignmentState.rollDegrees.coerceIn(-30f, 30f),
        animationSpec = tween(120, easing = LinearEasing),
        label = "horizon_roll"
    )

    val animatedLevelFraction by animateFloatAsState(
        targetValue = alignmentState.alignmentQualityFraction,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "level_fraction"
    )

    val gridColor = when {
        isBurstCapturing -> CyberCyanBright
        alignmentState.isLevel -> CyberEmeraldBright
        else -> CyberCyan.copy(alpha = pulseAlpha)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main Grid Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dynamic_guidance_grid_canvas")
        ) {
            val w = size.width
            val h = size.height
            val strokeThin = 1.dp.toPx()
            val strokeMedium = 1.5.dp.toPx()
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // 1. RULE OF THIRDS / MATRIX GRID LINES
            val divisions = when (gridMode) {
                GuidanceGridMode.PRECISION_3X3 -> 3
                GuidanceGridMode.MATRIX_4X4 -> 4
                GuidanceGridMode.FULL_ASSIST -> 3
                GuidanceGridMode.HORIZON_LEVEL -> 2
                GuidanceGridMode.OFF -> 0
            }

            if (divisions > 0) {
                // Vertical lines
                for (i in 1 until divisions) {
                    val x = w * (i.toFloat() / divisions)
                    drawLine(
                        color = gridColor.copy(alpha = if (gridMode == GuidanceGridMode.MATRIX_4X4) 0.3f else 0.45f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = strokeThin,
                        pathEffect = if (gridMode == GuidanceGridMode.PRECISION_3X3) dashedEffect else null
                    )

                    // Small top/bottom tick marks
                    drawLine(
                        color = gridColor.copy(alpha = 0.8f),
                        start = Offset(x, 0f),
                        end = Offset(x, 10.dp.toPx()),
                        strokeWidth = strokeMedium
                    )
                    drawLine(
                        color = gridColor.copy(alpha = 0.8f),
                        start = Offset(x, h - 10.dp.toPx()),
                        end = Offset(x, h),
                        strokeWidth = strokeMedium
                    )
                }

                // Horizontal lines
                for (j in 1 until divisions) {
                    val y = h * (j.toFloat() / divisions)
                    drawLine(
                        color = gridColor.copy(alpha = if (gridMode == GuidanceGridMode.MATRIX_4X4) 0.3f else 0.45f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = strokeThin,
                        pathEffect = if (gridMode == GuidanceGridMode.PRECISION_3X3) dashedEffect else null
                    )

                    // Small left/right tick marks
                    drawLine(
                        color = gridColor.copy(alpha = 0.8f),
                        start = Offset(0f, y),
                        end = Offset(10.dp.toPx(), y),
                        strokeWidth = strokeMedium
                    )
                    drawLine(
                        color = gridColor.copy(alpha = 0.8f),
                        start = Offset(w - 10.dp.toPx(), y),
                        end = Offset(w, y),
                        strokeWidth = strokeMedium
                    )
                }
            }

            // 2. CORNER RETICLE TICK SCALES
            val tickLength = 6.dp.toPx()
            val tickSpacing = 8.dp.toPx()
            val numTicks = 4

            // Top-Left corner ticks
            for (k in 1..numTicks) {
                drawLine(
                    color = gridColor.copy(alpha = 0.6f),
                    start = Offset(k * tickSpacing, 0f),
                    end = Offset(k * tickSpacing, tickLength),
                    strokeWidth = strokeThin
                )
                drawLine(
                    color = gridColor.copy(alpha = 0.6f),
                    start = Offset(0f, k * tickSpacing),
                    end = Offset(tickLength, k * tickSpacing),
                    strokeWidth = strokeThin
                )
            }

            // Bottom-Right corner ticks
            for (k in 1..numTicks) {
                drawLine(
                    color = gridColor.copy(alpha = 0.6f),
                    start = Offset(w - k * tickSpacing, h),
                    end = Offset(w - k * tickSpacing, h - tickLength),
                    strokeWidth = strokeThin
                )
                drawLine(
                    color = gridColor.copy(alpha = 0.6f),
                    start = Offset(w, h - k * tickSpacing),
                    end = Offset(w - tickLength, h - k * tickSpacing),
                    strokeWidth = strokeThin
                )
            }

            // 3. OPTICAL FOCAL CENTER TARGET
            val cx = w / 2
            val cy = h / 2
            val centerReticleRadius = 24.dp.toPx()

            // Outer subtle center circle
            drawCircle(
                color = gridColor.copy(alpha = 0.25f),
                radius = centerReticleRadius,
                center = Offset(cx, cy),
                style = Stroke(width = strokeThin, pathEffect = dashedEffect)
            )

            // Inner precise center focal target
            val targetDotRadius = if (alignmentState.isLevel) 4.dp.toPx() else 3.dp.toPx()
            drawCircle(
                color = if (alignmentState.isLevel) CyberEmeraldBright else CyberCyanBright,
                radius = targetDotRadius,
                center = Offset(cx, cy)
            )

            // 4. DYNAMIC HORIZON LEVEL LINE (Shows if phone is tilted)
            if (gridMode == GuidanceGridMode.HORIZON_LEVEL || gridMode == GuidanceGridMode.FULL_ASSIST) {
                val horizonLength = w * 0.65f
                val rad = Math.toRadians(-animatedHorizonRoll.toDouble())
                val dx = (horizonLength / 2 * Math.cos(rad)).toFloat()
                val dy = (horizonLength / 2 * Math.sin(rad)).toFloat()

                val horizonColor = if (alignmentState.isLevel) CyberEmeraldBright else CyberCyanBright.copy(alpha = 0.75f)

                // Left horizon wing
                drawLine(
                    color = horizonColor,
                    start = Offset(cx - dx, cy - dy),
                    end = Offset(cx - 30.dp.toPx(), cy),
                    strokeWidth = if (alignmentState.isLevel) 2.5.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Right horizon wing
                drawLine(
                    color = horizonColor,
                    start = Offset(cx + 30.dp.toPx(), cy),
                    end = Offset(cx + dx, cy + dy),
                    strokeWidth = if (alignmentState.isLevel) 2.5.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Tilt bubble guide in the middle
                val bubbleOffsetPx = (animatedHorizonRoll * 1.5f).dp.toPx()
                drawCircle(
                    color = if (alignmentState.isLevel) CyberEmeraldBright else CyberRose,
                    radius = 3.5.dp.toPx(),
                    center = Offset(cx + bubbleOffsetPx, cy - 14.dp.toPx())
                )
                // Bubble track
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(cx - 20.dp.toPx(), cy - 14.dp.toPx()),
                    end = Offset(cx + 20.dp.toPx(), cy - 14.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Top Dynamic Framing & Level Feedback Banner
        if (gridMode == GuidanceGridMode.FULL_ASSIST || gridMode == GuidanceGridMode.HORIZON_LEVEL) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (alignmentState.isLevel) CyberEmeraldBright.copy(alpha = 0.8f) else CyberCyan.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-36).dp)
                    .testTag("guidance_grid_alignment_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (alignmentState.isLevel) Icons.Default.CheckCircle else Icons.Default.Sensors,
                        contentDescription = null,
                        tint = if (alignmentState.isLevel) CyberEmeraldBright else CyberCyanBright,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (alignmentState.isLevel) {
                            "ALIGNED & LEVEL ✓ • OPTIMAL GEOMETRY"
                        } else {
                            val rollInt = abs(alignmentState.rollDegrees).toInt()
                            "TILT: ${rollInt}° • ALIGN DEVICE PARALLEL"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = if (alignmentState.isLevel) CyberEmeraldBright else CyberCyanBright
                    )
                }
            }
        }
    }
}

/**
 * Guidance Grid Mode Quick Toggle Selector for the Scanner HUD Toolbar
 */
@Composable
fun GuidanceGridModeToggle(
    currentMode: GuidanceGridMode,
    onModeChanged: (GuidanceGridMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextMode = when (currentMode) {
        GuidanceGridMode.FULL_ASSIST -> GuidanceGridMode.PRECISION_3X3
        GuidanceGridMode.PRECISION_3X3 -> GuidanceGridMode.MATRIX_4X4
        GuidanceGridMode.MATRIX_4X4 -> GuidanceGridMode.HORIZON_LEVEL
        GuidanceGridMode.HORIZON_LEVEL -> GuidanceGridMode.OFF
        GuidanceGridMode.OFF -> GuidanceGridMode.FULL_ASSIST
    }

    val icon = when (currentMode) {
        GuidanceGridMode.FULL_ASSIST -> Icons.Default.GridOn
        GuidanceGridMode.PRECISION_3X3 -> Icons.Default.Grid3x3
        GuidanceGridMode.MATRIX_4X4 -> Icons.Default.Grid4x4
        GuidanceGridMode.HORIZON_LEVEL -> Icons.Default.CenterFocusStrong
        GuidanceGridMode.OFF -> Icons.Default.GridOff
    }

    val isEnabled = currentMode != GuidanceGridMode.OFF

    IconButton(
        onClick = { onModeChanged(nextMode) },
        modifier = modifier
            .clip(CircleShape)
            .background(if (isEnabled) CyberCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.65f))
            .border(
                1.dp,
                if (isEnabled) CyberCyanBright else Color.White.copy(alpha = 0.2f),
                CircleShape
            )
            .testTag("toggle_guidance_grid_btn")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Guidance Grid: ${currentMode.label} (Tap to change)",
            tint = if (isEnabled) CyberCyanBright else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}
