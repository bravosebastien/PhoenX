package com.example.phoenx.ui.screens.book

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
    val nameRes: Int, 
    val fontFamily: FontFamily
)

data class BookBackgroundOption(
    val id: String, 
    val nameRes: Int, 
    val color: Color, 
    val darkText: Boolean
)

object BookThemeOptions {
    private val caveatFont = FontFamily(
        GFont(googleFont = GoogleFont("Caveat"), fontProvider = provider)
    )

    val fonts = listOf(
        BookFontOption("playfair_display", R.string.book_font_classic, FontFamily(Font(R.font.playfair_display))),
        BookFontOption("cormorant_garamond", R.string.book_font_modern, FontFamily(Font(R.font.cormorant_garamond))),
        BookFontOption("caveat", R.string.book_font_cursive, caveatFont),
        BookFontOption("monsieur_la_doulaise", R.string.book_font_elegant_plume, FontFamily(Font(R.font.monsieur_la_doulaise))),
        BookFontOption("almendra_bold", R.string.book_font_royal_chronicle, FontFamily(Font(R.font.almendra_bold))),
        BookFontOption("great_vibes", R.string.book_font_plume_script, FontFamily(Font(R.font.great_vibes))),
        BookFontOption("cormorant_variable", R.string.book_font_modern_book, FontFamily(Font(R.font.cormorantgaramond_variablefont)))
    )

    val backgrounds = listOf(
        BookBackgroundOption("classic_ivory", R.string.book_bg_ivory, Color(0xFFFFFDF5), true),
        BookBackgroundOption("antique_parchment", R.string.book_bg_parchment, Color(0xFFF5F2E1), true),
        BookBackgroundOption("velvet_night", R.string.book_bg_night, Color(0xFF121212), false),
        BookBackgroundOption("natural_linen", R.string.book_bg_linen, Color(0xFFE8E4D8), true),
        BookBackgroundOption("mist_gray", R.string.book_bg_mist, Color(0xFFF0F0F0), true),
        BookBackgroundOption("ash_gray", R.string.book_bg_ash, Color(0xFFDCDCDC), true),
        BookBackgroundOption("dusty_rose", R.string.book_bg_powder, Color(0xFFF2E9E4), true),
        BookBackgroundOption("midnight_forest", R.string.book_bg_forest, Color(0xFF0D1B1E), false)
    )

    @Composable
    fun getFontName(id: String): String {
        val res = fonts.find { it.id == id }?.nameRes ?: R.string.book_font_classic
        return stringResource(res)
    }

    @Composable
    fun getBackgroundName(id: String): String {
        val res = backgrounds.find { it.id == id }?.nameRes ?: R.string.book_bg_ivory
        return stringResource(res)
    }

    fun getFont(id: String): FontFamily {
        return fonts.find { it.id == id }?.fontFamily ?: FontFamily.Serif
    }

    fun getBackground(id: String): BookBackgroundOption {
        return backgrounds.find { it.id == id } ?: backgrounds[0]
    }
}
