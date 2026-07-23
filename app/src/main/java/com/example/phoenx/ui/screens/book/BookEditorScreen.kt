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
    val decryptedGlobalIntro by viewModel.decryptedGlobalIntro.collectAsState()
    val isGeneratingGlobalIntro by viewModel.isGeneratingGlobalIntro.collectAsState()
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
    var showAiExplanation by remember { mutableStateOf(false) }
    var showIntroEditor by remember { mutableStateOf(false) }
    var isStyleExpanded by remember { mutableStateOf(false) }
    var isTransmissionExpanded by remember { mutableStateOf(false) }
    var isIntroExpanded by remember { mutableStateOf(false) }
    
    // v8.9.0 : Thème Global
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    // Onboarding automatique (v8.6.3)
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("phoenx_prefs", android.content.Context.MODE_PRIVATE) }
    
    LaunchedEffect(Unit) {
        if (!prefs.getBoolean("seen_book_onboarding", false)) showOnboarding = true
        else if (!prefs.getBoolean("seen_book_ai_explanation", false)) showAiExplanation = true
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
            .background(theme.backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
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
                                tint = accent
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accent, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Sauvegarde...", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
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
                        fontFamily = theme.fontFamily,
                        fontSize = 28.sp,
                        color = theme.contentColor,
                        fontWeight = FontWeight.Bold
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
                            fontSize = 13.sp,
                            color = theme.contentColor.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }

                // ── ACTIONS PRIORITAIRES (v8.9.2) ──────────
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Bouton LIRE MON LIVRE (Adouci)
                            OutlinedButton(
                                onClick = { navController.navigate("book_viewer") },
                                modifier = Modifier.weight(1.5f).height(56.dp),
                                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("LIRE MON LIVRE", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                            }

                            OutlinedButton(
                                onClick = { showRegenerateConfirm = true },
                                modifier = Modifier.weight(1f).height(56.dp),
                                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor.copy(alpha = 0.6f))
                            ) {
                                Text("Régénérer", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // PRÉFACE DU MANUSCRIT (Bandeau déroulant)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isIntroExpanded = !isIntroExpanded },
                            color = theme.contentColor.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HistoryEdu, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("PRÉFACE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f))
                                    }
                                    Icon(
                                        imageVector = if (isIntroExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = theme.contentColor.copy(alpha = 0.2f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                AnimatedVisibility(visible = isIntroExpanded) {
                                    Column {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        GlobalIntroCard(
                                            content = decryptedGlobalIntro,
                                            isGenerating = isGeneratingGlobalIntro,
                                            onEdit = { showIntroEditor = true },
                                            onGenerate = { viewModel.generateGlobalIntro() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── RÉGLAGES CÔTE À CÔTE (v8.9.2) ────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // TRANSMISSION
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { isTransmissionExpanded = !isTransmissionExpanded },
                            color = theme.contentColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isTransmissionExpanded) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(Icons.Default.Lock, null, tint = accent, modifier = Modifier.size(18.dp))
                                Text(
                                    "TRANSMISSION", 
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                                    color = theme.contentColor.copy(alpha = 0.6f),
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = if (isTransmissionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, 
                                    null, 
                                    tint = theme.contentColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        // STYLE
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp) 
                                .clickable { isStyleExpanded = !isStyleExpanded },
                            color = theme.contentColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isStyleExpanded) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(Icons.Default.Palette, null, tint = accent, modifier = Modifier.size(18.dp))
                                Text(
                                    "STYLE", 
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                                    color = theme.contentColor.copy(alpha = 0.6f)
                                )
                                Icon(
                                    imageVector = if (isStyleExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = theme.contentColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                
                // Zone de sélection Transmission (Pleine largeur)
                item {
                    AnimatedVisibility(visible = isTransmissionExpanded) {
                        Column(modifier = Modifier.padding(bottom = 24.dp)) {
                            SealedMessageOptions(
                                userName = userName,
                                currentMessage = bookDraft!!.sealedMessage,
                                onMessageSelected = { 
                                    viewModel.updateSealedMessage(it)
                                    isTransmissionExpanded = false 
                                }
                            )
                        }
                    }
                }
                
                // Overlay Style si ouvert (Pleine largeur)
                item {
                    AnimatedVisibility(visible = isStyleExpanded) {
                        Column(modifier = Modifier.padding(bottom = 24.dp)) {
                            Text(
                                "Note : Le choix du Papier et de la Plume définit l'univers visuel que vos proches découvriront lors de la lecture de votre héritage.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = theme.fontFamily
                                ),
                                color = theme.contentColor
                            )
                            Spacer(Modifier.height(16.dp))
                            com.example.phoenx.ui.components.GlobalThemeSelector(
                                currentBackgroundId = bookDraft!!.theme.backgroundId,
                                currentFontId = bookDraft!!.theme.fontId,
                                onThemeChange = { bg, font -> viewModel.updateTheme(bg, font) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(top = 24.dp), color = theme.contentColor.copy(alpha = 0.1f))
                        }
                    }
                }

                // ── SÉLECTEUR DE DESTINATAIRES ────────────
                item {
                    Text(
                        text = "DESTINATAIRES DU MANUSCRIT", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                        color = theme.contentColor.copy(alpha = 0.4f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RecipientSelector(
                        recipients = recipients,
                        selectedIds = selectedRecipientIds,
                        visibility = if (selectedRecipientIds.isEmpty() && !forceRestricted) "EVERYONE" else "RESTRICTED",
                        onVisibilityChange = { newVis -> 
                            forceRestricted = (newVis == "RESTRICTED")
                            if (newVis == "EVERYONE") selectedRecipientIds.clear()
                        },
                        accent = accent,
                        containerColor = Color(0xFFF0F0F0) // Gris Brume forcé
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // ── LISTE DES CHAPITRES (v8.6.3: Master List) ──────────
                item {
                    // NOTE INFORMATIVE SUR L'ATTRIBUTION (v8.9.7)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        color = accent.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Note : Seuls vos souvenirs déjà « Attribués » (ceux que vous avez rangés pour un proche ou dans un compartiment) sont intégrés au récit par l'IA. Les pensées encore en attente dans votre fil ne sont pas prises en compte.",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = theme.contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Text(
                        text = "SOMMAIRE DU MANUSCRIT", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                        color = theme.contentColor.copy(alpha = 0.4f),
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
                containerColor = theme.backgroundColor,
                title = { Text("Réécrire tout le livre ?", color = theme.contentColor, fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold) },
                text = { Text("L'IA va reprendre l'ensemble de vos souvenirs pour créer un nouveau manuscrit. Vos modifications actuelles seront remplacées.", color = theme.contentColor.copy(alpha = 0.7f)) },
                confirmButton = {
                    Button(
                        onClick = { 
                            showRegenerateConfirm = false
                            viewModel.generateBook() 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.phoenXMatiere()
                    ) { Text("Confirmer", color = theme.backgroundColor, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showRegenerateConfirm = false }) {
                        Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
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
                    // Après le 1er popup, on propose le 2e s'il n'a pas été vu
                    if (!prefs.getBoolean("seen_book_ai_explanation", false)) showAiExplanation = true
                }
            )
        }

        if (showAiExplanation) {
            BookAiExplanationDialog(
                onDismiss = {
                    showAiExplanation = false
                    prefs.edit().putBoolean("seen_book_ai_explanation", true).apply()
                }
            )
        }

        // ── BOTTOMSHEET ÉDITEUR D'INTRODUCTION ──
        if (showIntroEditor) {
            GlobalIntroEditorSheet(
                currentContent = decryptedGlobalIntro,
                onDismiss = { showIntroEditor = false },
                onSave = { 
                    viewModel.updateGlobalIntro(it)
                    showIntroEditor = false
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

// ── FONCTIONS DE DESSIN CANVAS (STAND-BY v8.6.3) ──

@Composable
private fun EmptyBookState(onGenerate: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
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
                color = accent,
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.8f),
                style = stroke
            )
            drawRect(
                color = accent,
                topLeft = Offset(w * 0.52f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.8f),
                style = stroke
            )
            drawLine(
                color = accent,
                start = Offset(w * 0.5f, h * 0.1f),
                end = Offset(w * 0.5f, h * 0.9f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            for (i in 1..3) {
                val y = h * (0.25f + i * 0.12f)
                drawLine(
                    color = accent.copy(alpha = 0.4f),
                    start = Offset(w * 0.16f, y),
                    end = Offset(w * 0.44f, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (i in 1..3) {
                val y = h * (0.25f + i * 0.12f)
                drawLine(
                    color = accent.copy(alpha = 0.4f),
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
                fontFamily = theme.fontFamily,
                fontSize = 20.sp,
                color = theme.contentColor,
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
                fontSize = 14.sp,
                color = theme.contentColor.copy(alpha = 0.6f),
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
                containerColor = accent,
                contentColor = theme.backgroundColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Générer mon livre",
                style = TextStyle(
                    fontFamily = theme.fontFamily,
                    fontSize = 16.sp,
                    color = theme.backgroundColor
                )
            )
        }
    }
}

@Composable
private fun GeneratingBookState(progress: String) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
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
                color = accent.copy(alpha = alpha * 0.2f),
                radius = size.width / 2f
            )
            drawCircle(
                color = accent.copy(alpha = alpha),
                radius = size.width / 3f,
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                color = accent.copy(alpha = alpha),
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
                fontFamily = theme.fontFamily,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                color = theme.contentColor.copy(alpha = textAlpha),
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cela peut prendre quelques instants...",
            style = TextStyle(
                fontSize = 12.sp,
                color = theme.contentColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun BookAiExplanationDialog(onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Psychology, null, tint = accent, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "Retouches vs Réécriture", 
                    color = theme.contentColor, 
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.EditNote, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Modifier un chapitre", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text("L'action 'Demander à l'IA' à l'intérieur d'un chapitre ne modifie QUE ce chapitre. C'est idéal pour corriger un détail sans toucher au reste.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                    }
                }
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Régénérer le livre", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text("Le bouton 'Régénérer' (en haut) relance l'écriture de TOUS les chapitres. Utilisez-le uniquement si vous voulez un manuscrit totalement nouveau.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().phoenXMatiere(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("C'est très clair", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun GlobalIntroCard(
    content: String,
    isGenerating: Boolean,
    onEdit: () -> Unit,
    onGenerate: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    // Rendu style "Page de Préface"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(2.dp), // Coins presque carrés pour le papier
        border = BorderStroke(0.5.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accent)
                Spacer(Modifier.height(12.dp))
                Text("Inspiration en cours...", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = theme.contentColor)
            } else if (content.isEmpty()) {
                Text(
                    "« Ton livre attend ses premiers mots d'ouverture. »",
                    style = TextStyle(fontFamily = theme.fontFamily, fontSize = 16.sp, fontStyle = FontStyle.Italic),
                    color = theme.contentColor.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RÉDIGER LA PRÉFACE", color = theme.backgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Ornement haut
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(accent.copy(alpha = 0.3f)))
                Spacer(Modifier.height(20.dp))
                
                Text(
                    text = content,
                    style = TextStyle(
                        fontFamily = theme.fontFamily, 
                        fontSize = 15.sp, 
                        fontStyle = FontStyle.Italic, 
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = theme.contentColor.copy(alpha = 0.9f)
                )
                
                Spacer(Modifier.height(20.dp))
                // Ornement bas
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(accent.copy(alpha = 0.3f)))
                
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) {
                        Text("Modifier manuellement", color = accent, fontSize = 12.sp)
                    }
                    TextButton(onClick = onGenerate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, null, tint = accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("🤖 Renouveler par l'IA", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalIntroEditorSheet(
    currentContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var text by remember { mutableStateOf(currentContent) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = theme.backgroundColor
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(24.dp)) {
            Text("Introduction du Livre", style = MaterialTheme.typography.headlineSmall, fontFamily = theme.fontFamily, color = theme.contentColor)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                textStyle = TextStyle(fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp, color = theme.contentColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                    cursorColor = accent
                )
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(text) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = theme.backgroundColor
                )
            ) {
                Text("Enregistrer l'introduction", color = theme.backgroundColor)
            }
        }
    }
}

@Composable
private fun ChapterCard(
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

@Composable
private fun BookOnboardingDialog(onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.AutoStories, 
                    null, 
                    tint = accent, 
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Comment l'IA écrit votre vie", 
                    color = theme.contentColor, 
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold,
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
                    description = "Chaque souvenir est trié par âge. L'IA utilise ces repères pour tisser un récit fluide, du premier chapitre jusqu'au dernier.",
                    theme = theme
                )
                OnboardingPoint(
                    icon = Icons.Default.People,
                    title = "Reconnaissance des personnages",
                    description = "L'IA identifie vos proches (ex: 'Julie', 'mon fils Marc'). Elle fait le lien entre vos différents souvenirs pour raconter l'évolution de vos relations.",
                    theme = theme
                )
                OnboardingPoint(
                    icon = Icons.Default.HistoryEdu,
                    title = "Une narration personnalisée",
                    description = "Ce n'est pas une simple liste. L'IA rédige à la première personne ('Je') et adapte le ton (joie, nostalgie) selon l'émotion de vos souvenirs.",
                    theme = theme
                )
                
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "💡 Conseil du Biographe",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Plus vos souvenirs sont détaillés (prénoms, lieux, sentiments), plus le récit de l'IA sera précis et fidèle à votre réalité.",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().phoenXMatiere(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("J'ai compris", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun OnboardingPoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f), lineHeight = 18.sp)
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
