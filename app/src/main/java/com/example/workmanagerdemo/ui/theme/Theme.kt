package com.example.workmanagerdemo.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light Colour Scheme ──────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = WorkPrimary,
    onPrimary           = WorkOnPrimary,
    primaryContainer    = WorkPrimaryContainer,
    onPrimaryContainer  = WorkOnPrimaryContainer,
    secondary           = WorkSecondary,
    onSecondary         = WorkOnSecondary,
    secondaryContainer  = WorkSecondaryContainer,
    onSecondaryContainer = WorkOnSecondaryContainer,
    tertiary            = WorkTertiary,
    onTertiary          = WorkOnTertiary,
    tertiaryContainer   = WorkTertiaryContainer,
    onTertiaryContainer = WorkOnTertiaryContainer,
    error               = WorkError,
    onError             = WorkOnError,
    errorContainer      = WorkErrorContainer,
    onErrorContainer    = WorkOnErrorContainer,
    background          = WorkBackground,
    onBackground        = WorkOnBackground,
    surface             = WorkSurface,
    onSurface           = WorkOnSurface,
    surfaceVariant      = WorkSurfaceVariant,
    onSurfaceVariant    = WorkOnSurfaceVariant,
    outline             = WorkOutline
)

// ── Dark Colour Scheme ───────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = WorkPrimaryContainer,
    onPrimary           = WorkOnPrimaryContainer,
    primaryContainer    = WorkPrimary,
    onPrimaryContainer  = WorkOnPrimary,
    secondary           = WorkSecondaryContainer,
    onSecondary         = WorkOnSecondaryContainer,
    secondaryContainer  = WorkSecondary,
    onSecondaryContainer = WorkOnSecondary,
    tertiary            = WorkTertiaryContainer,
    onTertiary          = WorkOnTertiaryContainer,
    tertiaryContainer   = WorkTertiary,
    onTertiaryContainer = WorkOnTertiary,
)

/**
 * Root Material 3 theme for the WorkManager Demo app.
 *
 * - Supports **dynamic colours** on Android 12+ (API 31+) via [dynamicLightColorScheme]
 *   / [dynamicDarkColorScheme], which adapt to the user's wallpaper.
 * - Falls back to our custom [LightColorScheme] / [DarkColorScheme] on older devices.
 * - Updates the status bar colour to match [MaterialTheme.colorScheme.primary] via
 *   [WindowCompat] for a fully edge-to-edge immersive appearance.
 */
@Composable
fun WorkManagerDemoTheme(
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
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
