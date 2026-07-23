package com.example.phoenx.ui.screens.book

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.model.BookChapter
import com.example.phoenx.data.model.ChapterStatus
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.phoenXMatiere

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterEditorSheet(
    chapter: BookChapter,
    decryptedContent: String,
    isModifyingWithAi: Boolean,
    onDismiss: () -> Unit,
    onContentChange: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onValidate: () -> Unit,
    onUnvalidate: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var editableContent by remember(chapter.id, decryptedContent) {
        mutableStateOf(decryptedContent)
    }
    var showAiPanel by remember { mutableStateOf(false) }
    var aiInstruction by remember { mutableStateOf("") }
    val isValidated = chapter.status == ChapterStatus.VALIDATED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = chapter.title,
                style = TextStyle(
                    fontFamily = theme.fontFamily,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            )

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(1.dp)
                    .background(accent)
                    .padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (!isValidated) {
                    BasicTextField(
                        value = editableContent,
                        onValueChange = { newVal ->
                            editableContent = newVal
                            onContentChange(newVal)
                        },
                        textStyle = TextStyle(
                            fontFamily = theme.fontFamily,
                            fontSize = 16.sp,
                            color = theme.contentColor,
                            lineHeight = 26.sp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        cursorBrush = Brush.verticalGradient(listOf(accent, accent))
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = editableContent,
                            style = TextStyle(
                                fontFamily = theme.fontFamily,
                                fontSize = 16.sp,
                                color = theme.contentColor,
                                lineHeight = 26.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chapitre validé",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (isModifyingWithAi) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                theme.backgroundColor.copy(alpha = 0.85f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = accent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "L'IA réécrit ce chapitre...",
                                style = TextStyle(
                                    fontFamily = theme.fontFamily,
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = theme.contentColor.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showAiPanel && !isValidated) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            theme.contentColor.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    val suggestions = listOf(
                        "Reformule ce passage",
                        "Ajoute plus d'émotion",
                        "Raccourcis ce chapitre",
                        "Change le ton",
                        "Ajoute une introduction",
                        "Termine ce chapitre"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            FilterChip(
                                selected = false,
                                onClick = { aiInstruction = suggestion },
                                label = {
                                    Text(
                                        suggestion,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = theme.contentColor.copy(alpha = 0.1f),
                                    labelColor = theme.contentColor.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    BasicTextField(
                        value = aiInstruction,
                        onValueChange = { aiInstruction = it },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = theme.contentColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                theme.backgroundColor,
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        decorationBox = { inner ->
                            if (aiInstruction.isEmpty()) {
                                Text(
                                    "Dis à l'IA ce que tu veux modifier...",
                                    fontSize = 14.sp,
                                    color = theme.contentColor.copy(alpha = 0.3f)
                                )
                            }
                            inner()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (aiInstruction.isNotBlank()) {
                                onAskAi(aiInstruction)
                                showAiPanel = false
                                aiInstruction = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().phoenXMatiere(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Envoyer à l'IA",
                            color = theme.backgroundColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isValidated) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { showAiPanel = !showAiPanel }
                    ) {
                        Text(
                            "🤖 Demander à l'IA",
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onValidate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.phoenXMatiere()
                    ) {
                        Text(
                            "✅ Valider",
                            color = Color(0xFFFFFFFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                TextButton(
                    onClick = onUnvalidate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        "🔓 Modifier quand même",
                        color = theme.contentColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
