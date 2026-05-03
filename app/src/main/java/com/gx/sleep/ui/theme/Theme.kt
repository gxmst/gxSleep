package com.gx.sleep.ui.theme

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

// Warm dark palette with muted indigo + green/amber accents
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8B8E8),
    secondary = Color(0xFF8FB8A0),
    tertiary = Color(0xFFD4B896),
    background = Color(0xFF0C0F14),
    surface = Color(0xFF141820),
    surfaceVariant = Color(0xFF1B2030),
    surfaceContainerLow = Color(0xFF161B26),
    surfaceContainer = Color(0xFF1E2433),
    surfaceContainerHigh = Color(0xFF252C3D),
    onPrimary = Color(0xFF1A2340),
    onSecondary = Color(0xFF1A3020),
    onBackground = Color(0xFFD8DCE4),
    onSurface = Color(0xFFD8DCE4),
    onSurfaceVariant = Color(0xFF7A8294),
    error = Color(0xFFE8A0A0),
    primaryContainer = Color(0xFF1C2640),
    onPrimaryContainer = Color(0xFFB8C8F0),
    secondaryContainer = Color(0xFF1C2E22),
    onSecondaryContainer = Color(0xFFA0D4B0),
    tertiaryContainer = Color(0xFF2E2418),
    onTertiaryContainer = Color(0xFFE0C8A0),
    errorContainer = Color(0xFF2E1818),
    onErrorContainer = Color(0xFFE8A0A0),
    outline = Color(0xFF3A4050),
    outlineVariant = Color(0xFF2A3040)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3A5090),
    secondary = Color(0xFF3A7050),
    tertiary = Color(0xFF8A6830),
    background = Color(0xFFF6F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF0F6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1D24),
    onSurface = Color(0xFF1A1D24)
)

@Composable
fun GxSleepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
