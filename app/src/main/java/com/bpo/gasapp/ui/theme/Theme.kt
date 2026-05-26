package com.bpo.gasapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Green = Color(0xFF2E7D32)
private val GreenDark = Color(0xFF81C784)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = Color(0xFF00897B)
)

private val DarkColors = darkColorScheme(
    primary = GreenDark,
    secondary = Color(0xFF4DB6AC)
)

@Composable
fun GasAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        window.statusBarColor = colors.primary.toArgb()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
