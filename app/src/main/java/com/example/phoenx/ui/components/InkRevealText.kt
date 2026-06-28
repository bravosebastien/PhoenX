package com.example.phoenx.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * InkRevealText (Signature PHOEN-X 5.0)
 * Révèle le texte avec un suivi précis du curseur pour la plume.
 */
@Composable
fun InkRevealText(
    fullText: String,
    modifier: Modifier = Modifier,
    revealSpeedMs: Long = 40L,
    textStyle: TextStyle = TextStyle(
        fontSize = 14.sp,
        color = Color(0xFF2A1F10),
        lineHeight = 24.sp,
        fontFamily = FontFamily.Serif
    ),
    onProgress: (Int) -> Unit,
    onComplete: () -> Unit = {},
    onLayoutMeasured: (TextLayoutResult) -> Unit
) {
    var displayedText by remember(fullText) { mutableStateOf("") }

    LaunchedEffect(fullText) {
        displayedText = ""
        delay(800L) // Attente ouverture du livre

        fullText.forEachIndexed { index, char ->
            displayedText += char
            onProgress(index + 1)

            val charDelay = when(char) {
                '.', '!', '?' -> revealSpeedMs * 8
                ',' -> revealSpeedMs * 4
                ' ' -> revealSpeedMs * 2
                else -> revealSpeedMs
            }
            delay(charDelay)
        }
        onComplete()
    }

    Text(
        text = displayedText,
        style = textStyle,
        onTextLayout = { onLayoutMeasured(it) },
        modifier = modifier
    )
}
