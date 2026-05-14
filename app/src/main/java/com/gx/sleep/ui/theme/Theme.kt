package com.gx.sleep.ui.theme

import android.app.Activity
import android.os.Build
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
import com.gx.sleep.data.datastore.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8E99C4),
    onPrimary = Color(0xFF1A2340),
    primaryContainer = Color(0xFF1C2640),
    onPrimaryContainer = Color(0xFFB8C8F0),
    secondary = Color(0xFF8FB8A0),
    onSecondary = Color(0xFF1A3020),
    secondaryContainer = Color(0xFF1C2E22),
    onSecondaryContainer = Color(0xFFA0D4B0),
    tertiary = Color(0xFFD4B896),
    onTertiary = Color(0xFF2E2418),
    tertiaryContainer = Color(0xFF2E2418),
    onTertiaryContainer = Color(0xFFE0C8A0),
    error = Color(0xFFE8A0A0),
    onError = Color(0xFF2E1818),
    errorContainer = Color(0xFF2E1818),
    onErrorContainer = Color(0xFFE8A0A0),
    background = Color(0xFF0A0D14),
    onBackground = Color(0xFFD8DCE4),
    surface = Color(0xFF10141C),
    onSurface = Color(0xFFD8DCE4),
    surfaceVariant = Color(0xFF1B2030),
    onSurfaceVariant = Color(0xFF7A8294),
    surfaceContainerLow = Color(0xFF161B26),
    surfaceContainer = Color(0xFF1E2433),
    surfaceContainerHigh = Color(0xFF252C3D),
    outline = Color(0xFF3A4050),
    outlineVariant = Color(0xFF2A3040)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3A5090),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E0F8),
    onPrimaryContainer = Color(0xFF1A2850),
    secondary = Color(0xFF3A7050),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F0DC),
    onSecondaryContainer = Color(0xFF1A3020),
    tertiary = Color(0xFF8A6830),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8E8C8),
    onTertiaryContainer = Color(0xFF2E2418),
    error = Color(0xFFB03030),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF1A1D24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1D24),
    surfaceVariant = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFF5A6274),
    surfaceContainerLow = Color(0xFFEEF0F6),
    surfaceContainer = Color(0xFFE4E8F0),
    surfaceContainerHigh = Color(0xFFD8DCE6),
    outline = Color(0xFFC0C4D0),
    outlineVariant = Color(0xFFD0D4E0)
)

@Composable
fun GxSleepTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
