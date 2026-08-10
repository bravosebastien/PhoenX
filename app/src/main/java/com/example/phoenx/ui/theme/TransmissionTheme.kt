package com.example.phoenx.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.phoenx.ui.screens.book.BookThemeOptions

@Composable
fun TransmissionTheme(
    backgroundId: String?,
    fontId: String?,
    content: @Composable () -> Unit
) {
    val customFont = remember(fontId) { BookThemeOptions.getFont(fontId ?: "playfair_display") }
    val customBg = remember(backgroundId) { BookThemeOptions.getBackground(backgroundId ?: "classic_ivory") }
    
    val currentTheme = LocalAppTheme.current
    val theme = currentTheme.copy(
        fontFamily = customFont,
        backgroundColor = customBg.color,
        contentColor = if (customBg.darkText) Color.Black else Color.White
    )

    CompositionLocalProvider(LocalAppTheme provides theme) {
        content()
    }
}
