package com.bpo.gasapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BrandGreen = Color(0xFF1B7A3D)
private val BrandGreenDark = Color(0xFF7FD89A)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F0C6),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF00897B),
    secondaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFFF7FBF6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3E9E3)
)

private val DarkColors = darkColorScheme(
    primary = BrandGreenDark,
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF1B5E33),
    onPrimaryContainer = Color(0xFFB8F0C6),
    secondary = Color(0xFF4DB6AC),
    secondaryContainer = Color(0xFF00504A),
    background = Color(0xFF0F1511),
    surface = Color(0xFF161D18),
    surfaceVariant = Color(0xFF3F4942)
)

@Composable
fun GasAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(colorScheme = colors, content = content)
}
