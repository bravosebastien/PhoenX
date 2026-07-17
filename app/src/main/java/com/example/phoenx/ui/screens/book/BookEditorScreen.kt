package com.example.phoenx.ui.screens.book

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.model.BookChapter
import com.example.phoenx.data.model.ChapterStatus
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.AccentPrimary
import com.example.phoenx.ui.theme.BackgroundPrimary
import com.example.phoenx.ui.theme.SurfaceCard
import com.example.phoenx.ui.theme.TextPrimary
import com.example.phoenx.ui.theme.TextSecondary
import com.example.phoenx.ui.theme.TextTertiary

@Composable
fun BookEditorScreen(
    navController: NavController,
    viewModel: BookEditorViewModel = hiltViewModel()
) {
    val bookDraft by viewModel.bookDraft.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val selectedChapter by viewModel.selectedChapter.collectAsState()
    val isModifyingWithAi by viewModel.isModifyingWithAi.collectAsState()
    val error by viewModel.error.collectAsState()
    val isUserCreator by viewModel.isUserCreator.collectAsState()
    var showChapterEditor by remember { mutableStateOf(false) }

    // Redirection de sécurité si le destinataire arrive ici par erreur
    LaunchedEffect(isUserCreator) {
        if (isUserCreator == false) {
            navController.navigate("book_viewer_recipient") {
                popUpTo("book_editor") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // ── EN-TÊTE ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = AccentPrimary
                        )
                    }
                    InfoButton(
                        title = "Le Livre de Ma Vie",
                        points = listOf(
                            "L'IA génère un livre narratif à partir de tes souvenirs.",
                            "Chaque chapitre arrive en brouillon — tu peux le valider, le modifier, ou demander à l'IA de le réécrire.",
                            "L'IA ne lit jamais tes vrais souvenirs — uniquement les résumés anonymisés.",
                            "Un chapitre validé est verrouillé mais tu peux le déverrouiller à tout moment.",
                            "Tes proches liront ce livre comme un vrai livre, page par page."
                        )
                    )
                }
                if (bookDraft != null) {
                    TextButton(
                        onClick = { navController.navigate("book_viewer") }
                    ) {
                        Text(
                            text = "Lire le livre →",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = AccentPrimary
                            )
                        )
                    }
                }
            }

            Text(
                text = "Mon Livre de Vie",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    color = TextPrimary
                ),
                modifier = Modifier.padding(top = 8.dp)
            )

            if (bookDraft != null) {
                Text(
                    text = "${bookDraft!!.chapters.size} chapitres · " +
                           "${bookDraft!!.totalEntries} souvenirs intégrés",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = TextSecondary
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
            }

            // ── ÉTAT 1 : AUCUN LIVRE ──────────────────
            if (!isGenerating && bookDraft == null) {
                EmptyBookState(
                    onGenerate = { viewModel.generateBook() }
                )
            }

            // ── ÉTAT 2 : GÉNÉRATION EN COURS ──────────
            if (isGenerating) {
                GeneratingBookState(progress = generationProgress)
            }

            // ── ÉTAT 3 : ERREUR ───────────────────────
            error?.let { errorMsg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFE57373).copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = errorMsg,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFFE57373)
                        )
                    )
                }
            }

            // ── ÉTAT 4 : LISTE DES CHAPITRES ──────────
            if (!isGenerating && bookDraft != null) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(
                        bookDraft!!.chapters.sortedBy { it.orderIndex }
                    ) { chapter ->
                        ChapterCard(
                            chapter = chapter,
                            onClick = {
                                viewModel.selectChapter(chapter)
                                showChapterEditor = true
                            }
                        )
                    }
                    item {
                        // Bouton régénérer en bas
                        OutlinedButton(
                            onClick = { viewModel.generateBook() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, Color(0xFF3E3E45))
                        ) {
                            Text(
                                "Régénérer le livre",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ── BOTTOMSHEET ÉDITEUR DE CHAPITRE ───────
        if (showChapterEditor && selectedChapter != null) {
            ChapterEditorSheet(
                chapter = selectedChapter!!,
                isModifyingWithAi = isModifyingWithAi,
                onDismiss = {
                    showChapterEditor = false
                    viewModel.selectChapter(null)
                },
                onContentChange = { newContent ->
                    viewModel.updateChapterContent(
                        selectedChapter!!.id,
                        newContent
                    )
                },
                onAskAi = { instruction ->
                    viewModel.askAiToModify(
                        selectedChapter!!.id,
                        instruction
                    )
                },
                onValidate = {
                    viewModel.validateChapter(selectedChapter!!.id)
                    showChapterEditor = false
                    viewModel.selectChapter(null)
                },
                onUnvalidate = {
                    viewModel.unvalidateChapter(selectedChapter!!.id)
                }
            )
        }
    }
}

@Composable
private fun EmptyBookState(onGenerate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            drawRect(
                color = AccentPrimary,
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.8f),
                style = stroke
            )
            drawRect(
                color = AccentPrimary,
                topLeft = Offset(w * 0.52f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.8f),
                style = stroke
            )
            drawLine(
                color = AccentPrimary,
                start = Offset(w * 0.5f, h * 0.1f),
                end = Offset(w * 0.5f, h * 0.9f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            for (i in 1..3) {
                val y = h * (0.25f + i * 0.12f)
                drawLine(
                    color = AccentPrimary.copy(alpha = 0.4f),
                    start = Offset(w * 0.16f, y),
                    end = Offset(w * 0.44f, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (i in 1..3) {
                val y = h * (0.25f + i * 0.12f)
                drawLine(
                    color = AccentPrimary.copy(alpha = 0.4f),
                    start = Offset(w * 0.56f, y),
                    end = Offset(w * 0.84f, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ton histoire mérite d'être racontée.",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PHOEN-X va analyser tes souvenirs et rédiger\n" +
                   "le premier chapitre de ton livre de vie.\n" +
                   "Tu pourras tout relire, modifier et valider.",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Générer mon livre",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = BackgroundPrimary
                )
            )
        }
    }
}

@Composable
private fun GeneratingBookState(progress: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Canvas(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
        ) {
            drawCircle(
                color = AccentPrimary.copy(alpha = alpha * 0.2f),
                radius = size.width / 2f
            )
            drawCircle(
                color = AccentPrimary.copy(alpha = alpha),
                radius = size.width / 3f,
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                color = AccentPrimary.copy(alpha = alpha),
                start = Offset(size.width / 2f, size.height * 0.25f),
                end = Offset(size.width / 2f, size.height * 0.75f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        val textAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "textAlpha"
        )

        Text(
            text = progress,
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                color = TextPrimary.copy(alpha = textAlpha),
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cela peut prendre quelques instants...",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun ChapterCard(
    chapter: BookChapter,
    onClick: () -> Unit
) {
    val statusColor = when (chapter.status) {
        ChapterStatus.DRAFT     -> Color(0xFFFFB74D)
        ChapterStatus.IN_REVIEW -> AccentPrimary
        ChapterStatus.VALIDATED -> Color(0xFF4CAF50)
    }
    val statusLabel = when (chapter.status) {
        ChapterStatus.DRAFT     -> "Brouillon"
        ChapterStatus.IN_REVIEW -> "En révision"
        ChapterStatus.VALIDATED -> "Validé ✓"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(90.dp)
                .background(statusColor)
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f)
        ) {
            Text(
                text = "Chapitre ${chapter.orderIndex + 1}",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    letterSpacing = 0.08.em
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chapter.title,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .background(
                        statusColor.copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = statusColor
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterEditorSheet(
    chapter: BookChapter,
    isModifyingWithAi: Boolean,
    onDismiss: () -> Unit,
    onContentChange: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onValidate: () -> Unit,
    onUnvalidate: () -> Unit
) {
    var editableContent by remember(chapter.id) {
        mutableStateOf(chapter.content)
    }
    var showAiPanel by remember { mutableStateOf(false) }
    var aiInstruction by remember { mutableStateOf("") }
    val isValidated = chapter.status == ChapterStatus.VALIDATED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1410),
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
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    color = AccentPrimary
                )
            )

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(1.dp)
                    .background(AccentPrimary)
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
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            lineHeight = 26.sp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = editableContent,
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontSize = 16.sp,
                                color = TextPrimary,
                                lineHeight = 26.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chapitre validé",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                }

                if (isModifyingWithAi) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(0xFF1C1410).copy(alpha = 0.85f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = AccentPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "L'IA réécrit ce chapitre...",
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = TextSecondary
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
                            SurfaceCard,
                            RoundedCornerShape(12.dp)
                        )
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
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF3E3E45),
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    BasicTextField(
                        value = aiInstruction,
                        onValueChange = { aiInstruction = it },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                BackgroundPrimary,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        decorationBox = { inner ->
                            if (aiInstruction.isEmpty()) {
                                Text(
                                    "Dis à l'IA ce que tu veux modifier...",
                                    fontSize = 14.sp,
                                    color = TextTertiary
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Envoyer à l'IA",
                            color = BackgroundPrimary,
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
                            color = AccentPrimary,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onValidate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "✅ Valider",
                            color = Color(0xFFFFFFFF),
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
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
