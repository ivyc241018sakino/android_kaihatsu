package com.example.gametasks.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PurpleButton,
    secondary = GoldAccent,
    tertiary = Purple80,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = WhiteText,
    onSecondary = DarkBackground,
    onBackground = WhiteText,
    onSurface = WhiteText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = GrayText
)

@Composable
fun GameTasksTheme(
    darkTheme: Boolean = true, // Force dark theme as per mockup
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
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
