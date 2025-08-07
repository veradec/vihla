package com.kentaro.guts.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Charcoal Theme - Dark gray with blue-gray accents
private val CharcoalColorScheme = darkColorScheme(
    primary = Color(0xFF9bb7c5), // Light blue-gray
    secondary = Color(0xFF7f8c8d), // Medium gray
    tertiary = Color(0xFF34495e), // Dark blue-gray
    background = Color(0xFF222527), // Dark gray
    surface = Color(0xFF2c2f33), // Slightly lighter dark gray
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF959595), // Gray
    onSurface = Color(0xFFbdc3c7), // Light gray
    onSurfaceVariant = Color(0xFF95a5a6) // Medium light gray
)

@Composable
fun GutsTheme(
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> CharcoalColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}