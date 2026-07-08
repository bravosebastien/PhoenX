package com.example.phoenx.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAccentColor = staticCompositionLocalOf { AccentPrimary }
val LocalBackgroundBrush = staticCompositionLocalOf {
    Brush.radialGradient(
        colors = listOf(BackgroundSecondary, BackgroundPrimary),
        radius = 2000f
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = BackgroundPrimary,
    secondary = AccentSecondary,
    onSecondary = BackgroundPrimary,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundSecondary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = BackgroundPrimary
)

@Composable
fun PhoenXTheme(
    accentColor: Color = AccentPrimary,
    backgroundColor: Color = BackgroundPrimary,
    backgroundStyle: String = "RADIAL",
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme.copy(primary = accentColor)
    val view = LocalView.current
    
    val backgroundBrush = when(backgroundStyle) {
        "LINEAR" -> Brush.verticalGradient(
            colors = listOf(backgroundColor.copy(alpha = 0.2f), BackgroundPrimary)
        )
        "SOLID" -> Brush.linearGradient(
            colors = listOf(backgroundColor.copy(alpha = 0.15f), backgroundColor.copy(alpha = 0.15f))
        )
        else -> Brush.radialGradient(
            colors = listOf(backgroundColor.copy(alpha = 0.25f), BackgroundPrimary),
            radius = 2500f
        )
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(
        LocalAccentColor provides accentColor,
        LocalBackgroundBrush provides backgroundBrush
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
