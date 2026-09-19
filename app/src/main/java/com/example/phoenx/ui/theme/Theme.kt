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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.phoenx.ui.screens.book.BookThemeOptions
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.graphics.luminance

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
    backgroundColor: Color = Color(0xFFFFFDF5),
    fontId: String = "eb_garamond",         // v8.9.0
    content: @Composable () -> Unit
) {
    val fontFamily = BookThemeOptions.getFont(fontId)
    val isLight = backgroundColor.luminance() > 0.5f
    val contentColor = if (isLight) Color(0xFF1A1A1A) else Color(0xFFF2EDE8)

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
    
    // v12.7 : On mélange désormais vraiment la couleur du "papier" avec la couleur d'encre 
    // (contentColor) pour obtenir un dégradé doux et élégant par défaut.
    val toned = lerp(backgroundColor, contentColor, 0.08f)
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(backgroundColor, toned),
        radius = 1200f
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            val controller = WindowCompat.getInsetsController(window, view)
            // v8.9.0 : Adapter les icônes de la barre de statut au fond (Clair vs Sombre)
            controller.isAppearanceLightStatusBars = isLight
            controller.isAppearanceLightNavigationBars = isLight
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
