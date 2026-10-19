package com.example.jetpack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = CardWhite,
    primaryContainer = Color(0xFFD7E8DE),
    onPrimaryContainer = InkDark,
    secondary = Bronze,
    onSecondary = CardWhite,
    background = Paper,
    onBackground = InkDark,
    surface = CardWhite,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFE7E1D6),
    onSurfaceVariant = Color(0xFF514C44),
    outline = Color(0xFF7C776E),
)

private val DarkColors = darkColorScheme(
    primary = Leaf,
    onPrimary = InkDark,
    primaryContainer = Ink,
    onPrimaryContainer = Leaf,
    secondary = Color(0xFFE4C39A),
    onSecondary = InkDark,
    background = Night,
    onBackground = Paper,
    surface = NightCard,
    onSurface = Paper,
    surfaceVariant = Color(0xFF2A3330),
    onSurfaceVariant = Leaf,
)

@Composable
fun JetpackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
