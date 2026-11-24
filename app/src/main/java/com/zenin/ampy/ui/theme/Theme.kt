package com.zenin.ampy.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AmpyDarkPrimary,
    onPrimary = Color(0xFF003D33),
    primaryContainer = AmpyDarkPrimaryContainer,
    onPrimaryContainer = AmpyDarkOnPrimaryContainer,
    secondary = AmpyDarkSecondary,
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = AmpyDarkSecondaryContainer,
    onSecondaryContainer = AmpyDarkOnSecondaryContainer,
    tertiary = AmpyDarkTertiary,
    onTertiary = Color(0xFF0D47A1),
    tertiaryContainer = AmpyDarkTertiaryContainer,
    onTertiaryContainer = AmpyDarkOnTertiaryContainer,
    background = AmpyDarkBackground,
    onBackground = AmpyDarkOnSurface,
    surface = AmpyDarkSurface,
    onSurface = AmpyDarkOnSurface,
    surfaceVariant = AmpyDarkSurfaceVariant,
    onSurfaceVariant = AmpyDarkOnSurfaceVariant,
    surfaceContainer = AmpyDarkSurfaceContainer,
    surfaceContainerLowest = AmpyDarkBackground,
    surfaceContainerLow = AmpyDarkSurface,
    surfaceContainerHigh = Color(0xFF1D2E28),
    surfaceContainerHighest = AmpyDarkSurfaceVariant,
    outline = AmpyDarkOutline,
    outlineVariant = Color(0xFF5B6B65)
)

private val LightColorScheme = lightColorScheme(
    primary = AmpyLightPrimary,
    onPrimary = Color.White,
    primaryContainer = AmpyLightPrimaryContainer,
    onPrimaryContainer = AmpyLightOnPrimaryContainer,
    secondary = AmpyLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmpyLightSecondaryContainer,
    onSecondaryContainer = AmpyLightOnSecondaryContainer,
    tertiary = AmpyLightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmpyLightTertiaryContainer,
    onTertiaryContainer = AmpyLightOnTertiaryContainer,
    background = AmpyLightBackground,
    onBackground = AmpyLightOnSurface,
    surface = AmpyLightSurface,
    onSurface = AmpyLightOnSurface,
    surfaceVariant = AmpyLightSurfaceVariant,
    onSurfaceVariant = AmpyLightOnSurfaceVariant,
    surfaceContainer = AmpyLightSurfaceContainer,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AmpyLightBackground,
    surfaceContainerHigh = Color(0xFFE4ECE8),
    surfaceContainerHighest = AmpyLightSurfaceVariant,
    outline = AmpyLightOutline,
    outlineVariant = Color(0xFFBCCBC5)
)

@Composable
fun AmpyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Follow the device wallpaper/theme by default (Material You).
    dynamicColor: Boolean = true,
    enableStatusBarEffect: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode && enableStatusBarEffect && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AmpyTypography,
        content = content
    )
}
