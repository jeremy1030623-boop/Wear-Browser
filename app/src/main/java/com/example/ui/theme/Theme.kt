package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

private val WearDarkColorScheme = ColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF000000), // Pure black for OLED
    onBackground = Color(0xFFE6E1E5),
    outline = Color(0xFF938F99)
)

@Composable
fun WearAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearDarkColorScheme,
        content = content
    )
}
