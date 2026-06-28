package com.example.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.core.content.ContextCompat

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
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val systemAccent = Color(ContextCompat.getColor(context, android.R.color.system_accent1_300))
            val systemAccentContainer = Color(ContextCompat.getColor(context, android.R.color.system_accent1_700))
            val systemOnAccentContainer = Color(ContextCompat.getColor(context, android.R.color.system_accent1_100))
            
            val systemSecondary = Color(ContextCompat.getColor(context, android.R.color.system_accent2_300))
            val systemSecondaryContainer = Color(ContextCompat.getColor(context, android.R.color.system_accent2_700))
            val systemOnSecondaryContainer = Color(ContextCompat.getColor(context, android.R.color.system_accent2_100))

            WearDarkColorScheme.copy(
                primary = systemAccent,
                primaryContainer = systemAccentContainer,
                onPrimaryContainer = systemOnAccentContainer,
                secondary = systemSecondary,
                secondaryContainer = systemSecondaryContainer,
                onSecondaryContainer = systemOnSecondaryContainer
            )
        } catch (e: Exception) {
            WearDarkColorScheme
        }
    } else {
        WearDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
