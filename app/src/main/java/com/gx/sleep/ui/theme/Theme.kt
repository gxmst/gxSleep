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

// Soft indigo-purple palette for sleep app
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    secondary = Color(0xFFC5CAE9),
    tertiary = Color(0xFFD1C4E9),
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF1C2333),
    surfaceContainerLow = Color(0xFF1A1F2E),
    surfaceContainer = Color(0xFF222839),
    onPrimary = Color(0xFF1A237E),
    onSecondary = Color(0xFF283593),
    onBackground = Color(0xFFDCE0E8),
    onSurface = Color(0xFFDCE0E8),
    onSurfaceVariant = Color(0xFF8B92A0),
    error = Color(0xFFEF9A9A),
    primaryContainer = Color(0xFF1E2A4A),
    onPrimaryContainer = Color(0xFFB8C4FF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFF5C6BC0),
    tertiary = Color(0xFF7E57C2),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF0F7),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun GxSleepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
