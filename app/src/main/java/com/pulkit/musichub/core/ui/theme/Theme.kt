package com.pulkit.musichub.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentSage,
    onPrimary = AccentOnSage,
    primaryContainer = DarkSurfaceRaised,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = DarkBackground,
    background = AppleDarkBackground,
    onBackground = TextPrimary,
    surface = AppleCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppleCardSurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = StatusError,
    onError = AppleDarkBackground
)

val MusicHubShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun MusicHubTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = AppleDarkBackground.toArgb()
                window.navigationBarColor = AppleDarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = MusicHubShapes,
        content = content
    )
}
