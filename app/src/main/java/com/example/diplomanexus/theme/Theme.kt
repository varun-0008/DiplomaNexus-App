package com.example.diplomanexus.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    secondary = AccentAmber,
    onSecondary = DeepDark,
    tertiary = BrandOrangeLight,
    background = DeepDark,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = CardLightDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    error = Color(0xFFFF5252),
    onError = Color.White
)

@Composable
fun DiplomaNexusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
