package com.nyx.minichat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors lifted 1:1 from static/styles.css :root and [data-theme="light"]
// so the Android app reads as the same product as the web frontend.

// ---- Dark (default) ----
val DarkBg = Color(0xFF14161C)
val DarkSurface = Color(0xFF20232D)
val DarkSurface2 = Color(0xFF2A2E39)
val DarkBorder = Color(0xFF2A2E38)
val DarkText = Color(0xFFE4E6EB)
val DarkMuted = Color(0xFF8B90A0)

// ---- Light ----
val LightBg = Color(0xFFF4F1EA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFF0EDE4)
val LightBorder = Color(0xFFDDD7C8)
val LightText = Color(0xFF1E1C17)
val LightMuted = Color(0xFF7A7368)

// ---- Shared accents ----
val Accent = Color(0xFFE0A458)
val AccentDim = Color(0xFFB8823F)
val Danger = Color(0xFFD97070)
val Success = Color(0xFF7FB896)

private val MinichatDarkScheme = darkColorScheme(
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurface2,
    outline = DarkBorder,
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = DarkMuted,
    primary = Accent,
    onPrimary = Color(0xFF1E1408),
    secondary = AccentDim,
    error = Danger,
)

private val MinichatLightScheme = lightColorScheme(
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurface2,
    outline = LightBorder,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightMuted,
    primary = Accent,
    onPrimary = Color(0xFF1E1408),
    secondary = AccentDim,
    error = Danger,
)

enum class MinichatThemeMode { Dark, Light, System }

@Composable
fun MinichatTheme(
    mode: MinichatThemeMode = MinichatThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val useDark = when (mode) {
        MinichatThemeMode.Dark -> true
        MinichatThemeMode.Light -> false
        MinichatThemeMode.System -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) MinichatDarkScheme else MinichatLightScheme,
        typography = MinichatTypography,
        content = content,
    )
}
