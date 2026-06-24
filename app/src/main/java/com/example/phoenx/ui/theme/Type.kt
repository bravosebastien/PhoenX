package com.example.phoenx.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Temporairement : Utilisation des polices système pour permettre la compilation
// On réintégrera Google Fonts (Playfair Display & Inter) une fois le projet stabilisé.
val PlayfairDisplayFont = FontFamily.Serif
val InterFont = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlayfairDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlayfairDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlayfairDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
    )
)
