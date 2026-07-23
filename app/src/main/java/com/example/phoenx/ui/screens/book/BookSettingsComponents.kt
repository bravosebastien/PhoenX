package com.example.phoenx.ui.screens.book

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.model.BookChapter
import com.example.phoenx.data.model.ChapterStatus
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun SealedMessageOptions(
    userName: String,
    currentMessage: String,
    onMessageSelected: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val options = listOf(
        "$userName a décidé de vous partager le livre de sa vie. Visible le moment venu.",
        "$userName a préparé un précieux cadeau pour vous : le récit de sa vie, protégé avec tendresse jusqu'au moment de vous être transmis.",
        "Un trésor de mots et de souvenirs vous attend : le Livre de Vie de $userName, scellé pour éclairer votre chemin le moment venu."
    )

    var isCustomMode by remember(currentMessage) { 
        mutableStateOf(currentMessage.isNotEmpty() && !options.contains(currentMessage)) 
    }
    var customText by remember(currentMessage) { 
        mutableStateOf(if (isCustomMode) currentMessage else "") 
    }

    Column {
        options.forEach { phrase ->
            val isSelected = !isCustomMode && currentMessage == phrase
            Card(
                onClick = { 
                    isCustomMode = false
                    onMessageSelected(phrase) 
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accent.copy(alpha = 0.15f) else theme.contentColor.copy(alpha = 0.05f)
                ),
                border = BorderStroke(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
            ) {
                Text(
                    text = phrase,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, fontFamily = theme.fontFamily),
                    modifier = Modifier.padding(12.dp),
                    color = if (isSelected) theme.contentColor else theme.contentColor.copy(alpha = 0.6f)
                )
            }
        }

        // Option Personnalisée
        Card(
            onClick = { 
                isCustomMode = true 
                if (customText.isNotBlank()) onMessageSelected(customText)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isCustomMode) accent.copy(alpha = 0.15f) else theme.contentColor.copy(alpha = 0.05f)
            ),
            border = BorderStroke(1.dp, if (isCustomMode) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Écrire mon propre message...",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCustomMode) theme.contentColor else theme.contentColor.copy(alpha = 0.6f)
                )
                if (isCustomMode) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { 
                            customText = it
                            onMessageSelected(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                            focusedTextColor = theme.contentColor,
                            unfocusedTextColor = theme.contentColor
                        ),
                        placeholder = { Text("Votre message personnel...", fontSize = 12.sp, color = theme.contentColor.copy(alpha = 0.3f)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterCard(
    chapter: BookChapter,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val statusColor = when (chapter.status) {
        ChapterStatus.DRAFT     -> Color(0xFFFFB74D)
        ChapterStatus.IN_REVIEW -> accent
        ChapterStatus.VALIDATED -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // Indicateur de statut (petit cercle)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Chapitre ${chapter.orderIndex + 1}",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            color = theme.contentColor.copy(alpha = 0.4f),
                            letterSpacing = 0.05.em
                        )
                    )
                    Text(
                        text = chapter.title,
                        style = TextStyle(
                            fontFamily = theme.fontFamily,
                            fontSize = 16.sp,
                            color = theme.contentColor,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = if (chapter.status == ChapterStatus.VALIDATED) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (chapter.status == ChapterStatus.VALIDATED) statusColor else theme.contentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
