package com.tinyai.geekbox.core.ui.theme

import android.os.Build
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Cyan400,
    onPrimary = Color(0xFF00202A),
    primaryContainer = Color(0xFF0B3B47),
    onPrimaryContainer = Cyan400,
    secondary = Violet400,
    onSecondary = Color(0xFF20123A),
    tertiary = Emerald400,
    onTertiary = Color(0xFF00281C),
    background = Ink900,
    onBackground = Mist100,
    surface = Ink800,
    onSurface = Mist100,
    surfaceVariant = Ink700,
    onSurfaceVariant = Mist300,
    outline = Ink500,
    outlineVariant = Ink600,
    error = Rose400,
    onError = Color(0xFF3A0518)
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    secondary = Violet500,
    tertiary = Color(0xFF047857),
    background = LightSurface,
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EDF2)
)

@Composable
fun GeekBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = GeekBoxTypography,
        content = content
    )
}
