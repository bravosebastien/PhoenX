package com.example.phoenx.ui.screens.book

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.phoenx.R

data class BookFontOption(
    val id: String, 
    val name: String, 
    val fontFamily: FontFamily
)

data class BookBackgroundOption(
    val id: String, 
    val name: String, 
    val color: Color, 
    val darkText: Boolean
)

object BookThemeOptions {
    val fonts = listOf(
        BookFontOption("playfair_display", "Playfair Display", FontFamily(Font(R.font.playfair_display))),
        BookFontOption("cormorant_garamond", "Cormorant Garamond", FontFamily(Font(R.font.cormorant_garamond))),
        BookFontOption("monsieur_la_doulaise", "Plume Élégante", FontFamily(Font(R.font.monsieur_la_doulaise))),
        BookFontOption("almendra_bold", "Chronique Royale", FontFamily(Font(R.font.almendra_bold))),
        BookFontOption("great_vibes", "Plume Script", FontFamily(Font(R.font.great_vibes))),
        BookFontOption("cormorant_variable", "Livre Moderne", FontFamily(Font(R.font.cormorantgaramond_variablefont)))
    )

    val backgrounds = listOf(
        BookBackgroundOption("classic_ivory", "Ivoire", Color(0xFFFFFDF5), true),
        BookBackgroundOption("antique_parchment", "Parchemin", Color(0xFFF5F2E1), true),
        BookBackgroundOption("natural_linen", "Lin", Color(0xFFE8E4D8), true),
        BookBackgroundOption("velvet_night", "Nuit", Color(0xFF121212), false),
        BookBackgroundOption("dusty_rose", "Poudré", Color(0xFFF2E9E4), true),
        BookBackgroundOption("midnight_forest", "Forêt", Color(0xFF0D1B1E), false)
    )

    fun getFont(id: String): FontFamily {
        return fonts.find { it.id == id }?.fontFamily ?: FontFamily.Serif
    }

    fun getBackground(id: String): BookBackgroundOption {
        return backgrounds.find { it.id == id } ?: backgrounds[0]
    }
}
