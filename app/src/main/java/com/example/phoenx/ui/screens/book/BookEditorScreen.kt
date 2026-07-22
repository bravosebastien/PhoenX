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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.model.BookChapter
import com.example.phoenx.data.model.ChapterStatus
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.theme.*

@Composable
fun BookEditorScreen(
    navController: NavController,
    viewModel: BookEditorViewModel = hiltViewModel()
) {
    val bookDraft by viewModel.bookDraft.collectAsState()
    val decryptedContents by viewModel.decryptedContents.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val selectedChapter by viewModel.selectedChapter.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val isModifyingWithAi by viewModel.isModifyingWithAi.collectAsState()
    val error by viewModel.error.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val isUserCreator by viewModel.isUserCreator.collectAsState()
    val userName by viewModel.userName.collectAsState()
    var showChapterEditor by remember { mutableStateOf(false) }
    var forceRestricted by remember { mutableStateOf(false) } // v8.6.3: État indépendant pour le toggle visibilité
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }

    // Onboarding automatique (v8.6.3)
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("phoenx_prefs", android.content.Context.MODE_PRIVATE) }
    
    LaunchedEffect(Unit) {
        val hasSeen = prefs.getBoolean("seen_book_onboarding", false)
        if (!hasSeen) showOnboarding = true
    }

    // ÉTAPE 1 : Stabilisation de l'état au sommet (v8.6.3)
    val selectedRecipientIds = remember(bookDraft?.recipientIds) {
        mutableStateListOf<String>().apply { 
            bookDraft?.recipientIds?.let { addAll(it) } 
        }
    }

    // ÉTAPE 2 : Sauvegarde auto vers Firestore (v8.6.3)
    LaunchedEffect(selectedRecipientIds.toList()) {
        if (bookDraft != null && selectedRecipientIds.toList() != bookDraft!!.recipientIds) {
            viewModel.updateRecipients(selectedRecipientIds.toList())
        }
    }

    // Redirection de sécurité
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // ── EN-TÊTE ───────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

                    // Indicateur de sauvegarde (v8.6.3)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Sauvegarde...", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        } else if (saveSuccess) {
                            Icon(Icons.Default.CloudDone, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Enregistré", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Mon Livre de Vie",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 28.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (bookDraft != null) {
                item {
                    Text(
                        text = "${bookDraft!!.chapters.size} chapitres · " +
                               "${bookDraft!!.totalEntries} souvenirs intégrés",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            color = TextSecondary
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }

                // ── ACTIONS PRIORITAIRES (v8.6.2) ──────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate("book_viewer") },
                            modifier = Modifier.weight(1.5f).height(56.dp).phoenXMatiere(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = BackgroundPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("LIRE MON LIVRE", color = BackgroundPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { showRegenerateConfirm = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            border = BorderStroke(1.dp, Color(0xFF3E3E45)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Régénérer", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                // ── SÉLECTEUR DE DESTINATAIRES (Fix v8.6.3) ────────────
                item {
                    RecipientSelector(
                        recipients = recipients,
                        selectedIds = selectedRecipientIds,
                        visibility = if (selectedRecipientIds.isEmpty() && !forceRestricted) "EVERYONE" else "RESTRICTED",
                        onVisibilityChange = { newVis -> 
                            forceRestricted = (newVis == "RESTRICTED")
                            if (newVis == "EVERYONE") selectedRecipientIds.clear()
                        },
                        accent = AccentPrimary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── LE SCEAU PERSONNALISÉ (v8.6.2) ─────────
                item {
                    SealedMessageSection(
                        userName = userName,
                        currentMessage = bookDraft!!.sealedMessage,
                        onMessageSelected = { viewModel.updateSealedMessage(it) }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // ── LISTE DES CHAPITRES (v8.6.3: Master List) ──────────
                item {
                    Text(
                        text = "SOMMAIRE DU MANUSCRIT", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = TextTertiary, 
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                items(
                    items = bookDraft!!.chapters.sortedBy { it.orderIndex },
                    key = { it.id }
                ) { chapter ->
                    ChapterCard(
                        chapter = chapter,
                        onClick = {
                            viewModel.selectChapter(chapter)
                            showChapterEditor = true
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── ÉTAT 1 : AUCUN LIVRE ──────────────────
            if (!isGenerating && bookDraft == null) {
                item {
                    EmptyBookState(
                        onGenerate = { viewModel.generateBook() }
                    )
                }
            }
        }

        // ── ÉTAT 2 : GÉNÉRATION EN COURS (Overlay centré - v8.6.3) ──
        if (isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {}, // Absorbe les clics
                contentAlignment = Alignment.Center
            ) {
                GeneratingBookState(progress = generationProgress)
            }
        }

        // ── ÉTAT 3 : ERREUR (Toujours visible en haut - v8.6.3) ────
        error?.let { errorMsg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                color = Color(0xFFD32F2F),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }

        // ── POPUP DE CONFIRMATION RÉGÉNÉRATION ────
        if (showRegenerateConfirm) {
            AlertDialog(
                onDismissRequest = { showRegenerateConfirm = false },
                containerColor = BackgroundSecondary,
                title = { Text("Réécrire tout le livre ?", color = TextPrimary, fontFamily = FontFamily.Serif) },
                text = { Text("L'IA va reprendre l'ensemble de vos souvenirs pour créer un nouveau manuscrit. Vos modifications actuelles seront remplacées.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { 
                            showRegenerateConfirm = false
                            viewModel.generateBook() 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) { Text("Confirmer", color = BackgroundPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { showRegenerateConfirm = false }) {
                        Text("Annuler", color = TextPrimary)
                    }
                }
            )
        }

        // ── POPUP D'ACCUEIL : COMMENT ÇA MARCHE ? (v8.6.3) ──
        if (showOnboarding) {
            BookOnboardingDialog(
                onDismiss = { 
                    showOnboarding = false 
                    prefs.edit().putBoolean("seen_book_onboarding", true).apply()
                }
            )
        }

        // ── BOTTOMSHEET ÉDITEUR DE CHAPITRE ───────
        if (showChapterEditor && selectedChapter != null) {
            ChapterEditorSheet(
                chapter = selectedChapter!!,
                decryptedContent = decryptedContents[selectedChapter!!.id] ?: "",
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
fun SealedMessageSection(
    userName: String,
    currentMessage: String,
    onMessageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
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
        // BANDEAU DÉROULANT (v8.6.3)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            color = SurfaceCard.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (expanded) AccentPrimary.copy(alpha = 0.4f) else Color.Transparent)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "MESSAGE DE TRANSMISSION", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AccentPrimary, 
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Modifier le message d'attente", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = TextTertiary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, 
                    null, 
                    tint = TextTertiary
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                options.forEach { phrase ->
                    val isSelected = !isCustomMode && currentMessage == phrase
                    Card(
                        onClick = { 
                            isCustomMode = false
                            onMessageSelected(phrase) 
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) AccentPrimary.copy(alpha = 0.15f) else SurfaceCard.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, if (isSelected) AccentPrimary else Color.Transparent)
                    ) {
                        Text(
                            text = phrase,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(12.dp),
                            color = if (isSelected) TextPrimary else TextSecondary
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
                        containerColor = if (isCustomMode) AccentPrimary.copy(alpha = 0.15f) else SurfaceCard.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, if (isCustomMode) AccentPrimary else Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Écrire mon propre message...",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCustomMode) TextPrimary else TextSecondary
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
                                    focusedBorderColor = AccentPrimary,
                                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f)
                                ),
                                placeholder = { Text("Votre message personnel...", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── FONCTIONS DE DESSIN CANVAS (STAND-BY v8.6.3) ──

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
        ChapterStatus.IN_REVIEW -> "En attente de validation"
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
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 16.dp)
        )
    }
}

@Composable
private fun BookOnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.AutoStories, 
                    null, 
                    tint = AccentPrimary, 
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Comment l'IA écrit votre vie", 
                    color = TextPrimary, 
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OnboardingPoint(
                    icon = Icons.Default.Timeline,
                    title = "Respect de votre chronologie",
                    description = "Chaque souvenir est trié par âge. L'IA utilise ces repères pour tisser un récit fluide, du premier chapitre jusqu'au dernier."
                )
                OnboardingPoint(
                    icon = Icons.Default.People,
                    title = "Reconnaissance des personnages",
                    description = "L'IA identifie vos proches (ex: 'Julie', 'mon fils Marc'). Elle fait le lien entre vos différents souvenirs pour raconter l'évolution de vos relations."
                )
                OnboardingPoint(
                    icon = Icons.Default.HistoryEdu,
                    title = "Une narration personnalisée",
                    description = "Ce n'est pas une simple liste. L'IA rédige à la première personne ('Je') et adapte le ton (joie, nostalgie) selon l'émotion de vos souvenirs."
                )
                
                Surface(
                    color = AccentPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "💡 Conseil du Biographe",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Plus vos souvenirs sont détaillés (prénoms, lieux, sentiments), plus le récit de l'IA sera précis et fidèle à votre réalité.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("J'ai compris", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun OnboardingPoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = AccentPrimary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterEditorSheet(
    chapter: BookChapter,
    decryptedContent: String,
    isModifyingWithAi: Boolean,
    onDismiss: () -> Unit,
    onContentChange: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onValidate: () -> Unit,
    onUnvalidate: () -> Unit
) {
    var editableContent by remember(chapter.id, decryptedContent) {
        mutableStateOf(decryptedContent)
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
