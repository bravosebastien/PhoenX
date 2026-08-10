package com.example.phoenx.ui.screens.book

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GFont
import com.example.phoenx.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

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
    private val caveatFont = FontFamily(
        GFont(googleFont = GoogleFont("Caveat"), fontProvider = provider)
    )

    val fonts = listOf(
        BookFontOption("playfair_display", "Classique", FontFamily(Font(R.font.playfair_display))),
        BookFontOption("cormorant_garamond", "Moderne", FontFamily(Font(R.font.cormorant_garamond))),
        BookFontOption("caveat", "Cursive (Lisible)", caveatFont),
        BookFontOption("monsieur_la_doulaise", "Plume Élégante", FontFamily(Font(R.font.monsieur_la_doulaise))),
        BookFontOption("almendra_bold", "Chronique Royale", FontFamily(Font(R.font.almendra_bold))),
        BookFontOption("great_vibes", "Plume Script", FontFamily(Font(R.font.great_vibes))),
        BookFontOption("cormorant_variable", "Livre Moderne", FontFamily(Font(R.font.cormorantgaramond_variablefont)))
    )

    val backgrounds = listOf(
        BookBackgroundOption("classic_ivory", "Ivoire", Color(0xFFFFFDF5), true),
        BookBackgroundOption("antique_parchment", "Parchemin", Color(0xFFF5F2E1), true),
        BookBackgroundOption("velvet_night", "Nuit", Color(0xFF121212), false),
        BookBackgroundOption("natural_linen", "Lin", Color(0xFFE8E4D8), true),
        BookBackgroundOption("mist_gray", "Brume", Color(0xFFF0F0F0), true),
        BookBackgroundOption("ash_gray", "Cendré", Color(0xFFDCDCDC), true),
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
