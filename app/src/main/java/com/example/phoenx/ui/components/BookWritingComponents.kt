package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * BookWritingMode (Signature PHOEN-X 5.0)
 * La plume est désormais aimantée à la position réelle du texte.
 */
@Composable
fun BookWritingMode(
    value: String,
    onValueChange: (String) -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean = false,
    placeholder: String = "Commence à écrire ton souvenir...",
    modifier: Modifier = Modifier,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var tipPosition by remember { mutableStateOf<Offset>(Offset.Zero) }
    var isActivelyWriting by remember { mutableStateOf(false) }

    // On calcule la position exacte de la fin du texte
    LaunchedEffect(value, textLayoutResult) {
        if (value.isNotEmpty() && textLayoutResult != null) {
            isActivelyWriting = true
            val lastCharIndex = value.length
            val rect = textLayoutResult!!.getCursorRect(lastCharIndex)
            tipPosition = Offset(rect.left, rect.top)
            delay(600L)
            isActivelyWriting = false
        } else {
            tipPosition = Offset.Zero
        }
    }

    Box(modifier = modifier.fillMaxSize()) { // Remplit tout l'espace (Plein Écran)
        OpenBookCanvas(modifier = Modifier.fillMaxSize())

        // Bouton Micro (Flottant sur le livre)
        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 40.dp, top = 40.dp)
                .background(if (isListening) Color.Red.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Dictée vocale",
                tint = if (isListening) Color.Red else Color(0xFFC97B3A)
            )
        }

        // Conteneur du texte (Page de droite)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp, top = 60.dp, bottom = 60.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(fontSize = 14.sp, color = Color(0x60C97B3A), fontStyle = FontStyle.Italic),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp, 
                    color = Color(0xFF2A1F10), 
                    lineHeight = 24.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                ),
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier.fillMaxSize()
            )

            // La Plume (superposée exactement sur le texte)
            QuillPenCanvas(
                tipOffset = tipPosition,
                isWriting = isActivelyWriting || isListening,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * BookRevealMode (Côté Destinataire)
 */
@Composable
fun BookRevealMode(
    text: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var tipPosition by remember { mutableStateOf<Offset>(Offset.Zero) }
    var isWriting by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        OpenBookCanvas(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp, top = 60.dp, bottom = 60.dp)
        ) {
            InkRevealText(
                fullText = text,
                onProgress = { charIndex ->
                    textLayoutResult?.let { layout ->
                        val rect = layout.getCursorRect(charIndex)
                        tipPosition = Offset(rect.left, rect.top)
                        isWriting = true
                    }
                },
                onComplete = {
                    isWriting = false
                    onComplete()
                },
                onLayoutMeasured = { textLayoutResult = it },
                modifier = Modifier.fillMaxSize()
            )

            QuillPenCanvas(
                tipOffset = tipPosition,
                isWriting = isWriting,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
