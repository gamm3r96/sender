package com.example.ui.theme

/**
 * Represents the application theme mode preference.
 */
enum class ThemeMode(val title: String) {
    SYSTEM("Auto"),
    LIGHT("Light"),
    DARK("Dark"),
    OLED("OLED");

    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> OLED
        OLED -> SYSTEM
    }
}
