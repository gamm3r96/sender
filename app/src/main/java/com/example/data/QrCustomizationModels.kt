package com.example.data

import androidx.compose.ui.graphics.Color
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

enum class QrColorScheme(
    val title: String,
    val darkColor: Long, // ARGB or Color long
    val lightColor: Long,
    val description: String,
    val badgeLabel: String
) {
    HIGH_CONTRAST_MONO(
        title = "Mono High-Contrast",
        darkColor = 0xFF000000,
        lightColor = 0xFFFFFFFF,
        description = "Solid black on pure white. Universal compatibility with all cameras & sensors.",
        badgeLabel = "100% Contrast"
    ),
    INVERTED_DARK(
        title = "OLED White on Dark",
        darkColor = 0xFFFFFFFF,
        lightColor = 0xFF0A0F1D,
        description = "Pure white modules on deep dark background. Battery-efficient on OLED displays.",
        badgeLabel = "OLED Dark"
    ),
    CYBER_EMERALD(
        title = "Cyber Emerald Matrix",
        darkColor = 0xFF10B981,
        lightColor = 0xFF041C16,
        description = "High-luminescence neon emerald on pitch matrix. Sender signature style.",
        badgeLabel = "Neon Emerald"
    ),
    CYBER_CYAN(
        title = "Electric Cyan",
        darkColor = 0xFF06B6D4,
        lightColor = 0xFF071E2D,
        description = "Sharp electric cyan with anti-glare navy background. Ideal for LCD screens.",
        badgeLabel = "Electric Cyan"
    ),
    AMBER_WARM(
        title = "Night Vision Amber",
        darkColor = 0xFFF59E0B,
        lightColor = 0xFF1E1404,
        description = "Warm low-wavelength amber for optical capture in dark environments.",
        badgeLabel = "Night Amber"
    ),
    VIOLET_NEON(
        title = "Neon Violet",
        darkColor = 0xFFA855F7,
        lightColor = 0xFF1E0A2D,
        description = "Cyberpunk ultraviolet spectrum for high-contrast digital displays.",
        badgeLabel = "Cyber Violet"
    );

    val composeDarkColor: Color get() = Color(darkColor)
    val composeLightColor: Color get() = Color(lightColor)
}

enum class QrErrorCorrectionLevel(
    val title: String,
    val zxingLevel: ErrorCorrectionLevel,
    val recoveryPercent: String,
    val description: String,
    val badgeLabel: String
) {
    LEVEL_L(
        title = "Level L (7% Recovery)",
        zxingLevel = ErrorCorrectionLevel.L,
        recoveryPercent = "~7%",
        description = "Lowest redundancy. Minimal matrix size, highest data density per frame for pristine screens.",
        badgeLabel = "Max Density"
    ),
    LEVEL_M(
        title = "Level M (15% Recovery)",
        zxingLevel = ErrorCorrectionLevel.M,
        recoveryPercent = "~15%",
        description = "Standard balance between payload capacity and optical error resilience.",
        badgeLabel = "Recommended"
    ),
    LEVEL_Q(
        title = "Level Q (25% Recovery)",
        zxingLevel = ErrorCorrectionLevel.Q,
        recoveryPercent = "~25%",
        description = "High redundancy. Withstands screen scratches, reflections, motion blur, and glare.",
        badgeLabel = "Anti-Glare"
    ),
    LEVEL_H(
        title = "Level H (30% Recovery)",
        zxingLevel = ErrorCorrectionLevel.H,
        recoveryPercent = "~30%",
        description = "Maximum resilience for outdoor sunlight, heavy camera vibrations, or long distances.",
        badgeLabel = "Ultra Rugged"
    )
}

enum class QrModuleShape(
    val title: String,
    val cornerRadiusFraction: Float,
    val description: String
) {
    SQUARE(
        title = "Sharp Pixels",
        cornerRadiusFraction = 0f,
        description = "Standard sharp square pixels for maximum optical edge detection."
    ),
    ROUNDED(
        title = "Rounded Matrix",
        cornerRadiusFraction = 0.35f,
        description = "Soft rounded geometric corners for modern visual aesthetic."
    ),
    DOTS(
        title = "Dot Matrix",
        cornerRadiusFraction = 0.5f,
        description = "Circular dot pattern providing high distinction."
    )
}

enum class ScannerContrastBoostMode(
    val title: String,
    val description: String
) {
    STANDARD(
        title = "Standard Sensor Feed",
        description = "Default high-speed hardware camera viewfinder balance."
    ),
    HIGH_CONTRAST(
        title = "High-Contrast B&W",
        description = "Enhances edge separation in low-light and high-glare environments."
    ),
    INVERTED_NEGATIVE(
        title = "Inverted Scan Assistant",
        description = "Optimized for capturing dark-mode & white-on-black QR broadcasts."
    )
}
