package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.ThemeMode

/**
 * Compact icon button to cycle between System, Light, and Dark themes.
 */
@Composable
fun ThemeToggleIconButton(
    themeMode: ThemeMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label, tint) = when (themeMode) {
        ThemeMode.SYSTEM -> Triple(
            Icons.Default.BrightnessAuto,
            "Auto Theme",
            CyberCyanBright
        )
        ThemeMode.LIGHT -> Triple(
            Icons.Default.LightMode,
            "Light Theme",
            Color(0xFFF59E0B) // Bright sun amber
        )
        ThemeMode.DARK -> Triple(
            Icons.Default.DarkMode,
            "Dark Theme",
            CyberEmeraldBright
        )
        ThemeMode.OLED -> Triple(
            Icons.Default.Nightlight,
            "OLED Theme",
            Color.White
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .testTag("theme_toggle_btn")
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .minimumInteractiveComponentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = themeMode.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Segmented pill row allowing direct 1-tap switching between Auto, Light, and Dark modes.
 */
@Composable
fun ThemeToggleSegmentedControl(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.testTag("theme_segmented_control")
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemePillItem(
                title = "Auto",
                icon = Icons.Default.BrightnessAuto,
                isSelected = selectedMode == ThemeMode.SYSTEM,
                activeColor = CyberCyanBright,
                onClick = { onSelectMode(ThemeMode.SYSTEM) },
                testTag = "theme_btn_system"
            )
            ThemePillItem(
                title = "Light",
                icon = Icons.Default.LightMode,
                isSelected = selectedMode == ThemeMode.LIGHT,
                activeColor = Color(0xFFF59E0B),
                onClick = { onSelectMode(ThemeMode.LIGHT) },
                testTag = "theme_btn_light"
            )
            ThemePillItem(
                title = "Dark",
                icon = Icons.Default.DarkMode,
                isSelected = selectedMode == ThemeMode.DARK,
                activeColor = CyberEmeraldBright,
                onClick = { onSelectMode(ThemeMode.DARK) },
                testTag = "theme_btn_dark"
            )
            ThemePillItem(
                title = "OLED",
                icon = Icons.Default.Nightlight,
                isSelected = selectedMode == ThemeMode.OLED,
                activeColor = Color.White,
                onClick = { onSelectMode(ThemeMode.OLED) },
                testTag = "theme_btn_oled"
            )
        }
    }
}

@Composable
private fun ThemePillItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val bgAnimated by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(200),
        label = "pill_bg"
    )

    val contentColorAnimated by animateColorAsState(
        targetValue = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "pill_content"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgAnimated)
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, activeColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColorAnimated,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = contentColorAnimated
            )
        }
    }
}
