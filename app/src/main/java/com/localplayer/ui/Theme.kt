package com.localplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern dark gray + orange color scheme (softer orange ~50% opacity)
private val OrangeAccent = Color(0x80FF8C00)        // Softer orange (50% opacity)
private val OrangeLight = Color(0x80FFAB40)         // Softer light orange (50% opacity)
private val OrangeDark = Color(0x80E67E00)          // Softer dark orange (50% opacity)

private val DarkGray900 = Color(0xFF121212)         // Deepest background
private val DarkGray850 = Color(0xFF1A1A1A)         // Background
private val DarkGray800 = Color(0xFF1E1E1E)         // Surface
private val DarkGray750 = Color(0xFF252525)         // Surface variant
private val DarkGray700 = Color(0xFF2C2C2C)         // Elevated surface
private val DarkGray600 = Color(0xFF383838)         // Cards/containers

private val TextPrimary = Color(0xFFFAFAFA)         // Primary text
private val TextSecondary = Color(0xFFB0B0B0)       // Secondary text
private val TextTertiary = Color(0xFF808080)        // Tertiary/hint text

private val DarkColors = darkColorScheme(
    primary = OrangeAccent,
    onPrimary = Color.Black,
    primaryContainer = OrangeDark,
    onPrimaryContainer = Color.White,
    
    secondary = OrangeLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D2E00),
    onSecondaryContainer = OrangeLight,
    
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color.Black,
    
    background = DarkGray850,
    onBackground = TextPrimary,
    
    surface = DarkGray800,
    onSurface = TextPrimary,
    
    surfaceVariant = DarkGray750,
    onSurfaceVariant = TextSecondary,
    
    surfaceTint = OrangeAccent,
    
    outline = DarkGray600,
    outlineVariant = DarkGray700,
    
    error = Color(0xFFCF6679),
    onError = Color.Black,
    
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkGray900,
    inversePrimary = OrangeDark
)

@Composable
fun LocalPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
