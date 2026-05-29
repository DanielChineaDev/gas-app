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

// Paleta inspirada en el logo: verde hierba, azul cielo, naranja del surtidor
// y rojo de la manguera.
private val BrandGreen = Color(0xFF1B7A3D)
private val BrandGreenLight = Color(0xFF7FD89A)
private val BrandBlue = Color(0xFF1976D2)
private val BrandBlueLight = Color(0xFF90CAF9)
private val BrandOrange = Color(0xFFF57C00)
private val BrandOrangeLight = Color(0xFFFFB74D)
private val BrandRed = Color(0xFFD32F2F)

/** Red used consistently for the "favorite" heart across cards, markers and detail. */
val FavoriteRed = Color(0xFFE53935)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D36),
    tertiary = BrandOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF2A1700),
    error = BrandRed,
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE4E8F0)
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color(0xFF001C45),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFC7E0FF),
    secondary = BrandBlueLight,
    onSecondary = Color(0xFF003258),
    secondaryContainer = Color(0xFF00497D),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = BrandOrangeLight,
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF643F00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF0F141B),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF3A4150)
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
