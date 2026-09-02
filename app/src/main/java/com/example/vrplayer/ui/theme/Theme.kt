package com.example.vrplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VRDarkColorScheme = darkColorScheme(
    primary = VRPrimaryDark,
    secondary = VRSecondaryDark,
    tertiary = VRTertiaryDark,
    background = VRBackgroundDark,
    surface = VRSurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = VRTextPrimary,
    onSurface = VRTextPrimary,
    surfaceVariant = VRCardBackground,
    onSurfaceVariant = VRTextSecondary,
    outline = VRCardBorder,
    error = VRError,
    onError = Color.White
)

@Composable
fun VRPlayerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = VRDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VRBackgroundDark.toArgb()
            window.navigationBarColor = VRBackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
