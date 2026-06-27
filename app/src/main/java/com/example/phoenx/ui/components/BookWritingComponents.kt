package com.example.phoenx.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * BookWritingMode (Côté Créateur)
 * La plume suit la saisie en temps réel sur la page de droite.
 */
@Composable
fun BookWritingMode(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Commence à écrire ton souvenir...",
    modifier: Modifier = Modifier
) {
    val charsPerLine = 35
    val currentChar = value.length
    val currentLine = (currentChar / charsPerLine) % 10
    val progressOnLine = (currentChar % charsPerLine).toFloat() / charsPerLine.toFloat()
    
    var isActivelyWriting by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value.isNotEmpty()) {
            isActivelyWriting = true
            delay(800)
            isActivelyWriting = false
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(400.dp)) {
        OpenBookCanvas(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.45f)
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp, top = 40.dp, bottom = 40.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(fontSize = 13.sp, color = Color(0x60C97B3A), fontStyle = FontStyle.Italic)
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF2A1F10), lineHeight = 22.sp),
                modifier = Modifier.fillMaxSize()
            )
        }

        QuillPenCanvas(
            progress = progressOnLine,
            currentLine = currentLine,
            isWriting = isActivelyWriting,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.45f)
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp, top = 30.dp, bottom = 40.dp)
        )
    }
}

/**
 * BookRevealMode (Côté Destinataire)
 * Le texte se dévoile avec la plume.
 */
@Composable
fun BookRevealMode(
    text: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quillProgress by remember { mutableStateOf(0f) }
    var quillLine by remember { mutableStateOf(0) }
    var isWriting by remember { mutableStateOf(true) }
    var isFinished by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        OpenBookCanvas(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.45f)
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp, top = 40.dp, bottom = 40.dp)
        ) {
            InkRevealText(
                fullText = text,
                onProgress = { p, l ->
                    quillProgress = p
                    quillLine = l
                    isWriting = true
                },
                onComplete = {
                    isWriting = false
                    isFinished = true
                    onComplete()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isFinished) {
            QuillPenCanvas(
                progress = quillProgress,
                currentLine = quillLine,
                isWriting = isWriting,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp, top = 30.dp, bottom = 40.dp)
            )
        }
    }
}
