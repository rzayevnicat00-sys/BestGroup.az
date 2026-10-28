package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF4D6D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF400A15),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFFF4D6D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E121B),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFFF758F),
    onTertiary = Color.Black,
    background = Color(0xFF121417),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF181C22),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF222832),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2E3642),
    outlineVariant = Color(0xFF222832)
)

private val LightColorScheme = lightColorScheme(
    primary = BirbankRed,
    onPrimary = Color.White,
    primaryContainer = BirbankRedLight,
    onPrimaryContainer = BirbankRedDark,
    secondary = BirbankRed,
    onSecondary = Color.White,
    secondaryContainer = BirbankRedLight,
    onSecondaryContainer = BirbankRed,
    tertiary = BirbankRedDark,
    onTertiary = Color.White,
    background = BirbankBgLight,
    onBackground = BirbankTextPrimary,
    surface = Color.White,
    onSurface = BirbankTextPrimary,
    surfaceVariant = BirbankSearchBg,
    onSurfaceVariant = BirbankTextSecondary,
    outline = BirbankBorder,
    outlineVariant = Color(0xFFF2F4F7)
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun BestGroupTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
