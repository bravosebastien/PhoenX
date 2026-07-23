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
import com.example.phoenx.ui.screens.book.BookThemeOptions
import androidx.compose.ui.text.font.FontFamily

// v8.9.0 : Structure du Thème Global
data class AppThemeState(
    val backgroundColor: Color = Color(0xFFFFFDF5), // Ivoire par défaut
    val fontFamily: FontFamily = FontFamily.Serif,
    val contentColor: Color = Color(0xFF1A1A1A),
    val accentColor: Color = AccentPrimary
)

val LocalAppTheme = staticCompositionLocalOf { AppThemeState() }
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
    onBackground = Color(0xFFF2EDE8),
    surface = BackgroundSecondary,
    onSurface = Color(0xFFF2EDE8),
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = Color(0xFF9B9590),
    error = Error,
    onError = BackgroundPrimary
)

@Composable
fun PhoenXTheme(
    accentColor: Color = AccentPrimary,
    backgroundId: String = "classic_ivory", // v8.9.0
    fontId: String = "eb_garamond",         // v8.9.0
    backgroundStyle: String = "RADIAL",
    content: @Composable () -> Unit
) {
    val bgOption = BookThemeOptions.getBackground(backgroundId)
    val fontFamily = BookThemeOptions.getFont(fontId)
    val backgroundColor = bgOption.color
    val contentColor = if (bgOption.darkText) Color(0xFF1A1A1A) else Color(0xFFF2EDE8)

    val themeState = AppThemeState(
        backgroundColor = backgroundColor,
        fontFamily = fontFamily,
        contentColor = contentColor,
        accentColor = accentColor
    )

    val colorScheme = DarkColorScheme.copy(
        primary = accentColor,
        background = backgroundColor,
        onBackground = contentColor,
        surface = backgroundColor,
        onSurface = contentColor
    )
    
    val view = LocalView.current
    
    val backgroundBrush = when(backgroundStyle) {
        "LINEAR" -> Brush.verticalGradient(
            colors = listOf(backgroundColor.copy(alpha = 0.6f), backgroundColor)
        )
        "SOLID" -> Brush.linearGradient(
            colors = listOf(backgroundColor, backgroundColor)
        )
        else -> Brush.radialGradient(
            colors = listOf(
                backgroundColor.copy(alpha = 0.7f),
                backgroundColor.copy(alpha = 0.95f)
            ),
            radius = 1200f
        )
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            val controller = WindowCompat.getInsetsController(window, view)
            // v8.9.0 : Adapter les icônes de la barre de statut au fond (Clair vs Sombre)
            controller.isAppearanceLightStatusBars = bgOption.darkText
            controller.isAppearanceLightNavigationBars = bgOption.darkText
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides themeState,
        LocalAccentColor provides accentColor,
        LocalBackgroundBrush provides backgroundBrush
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography.copy(
                bodyLarge = Typography.bodyLarge.copy(fontFamily = fontFamily, color = contentColor),
                headlineSmall = Typography.headlineSmall.copy(fontFamily = fontFamily, color = contentColor),
                displayMedium = Typography.displayMedium.copy(fontFamily = fontFamily, color = contentColor)
            ),
            shapes = Shapes,
            content = content
        )
    }
}
