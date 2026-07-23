package com.example.phoenx.ui.screens.book

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                            border = BorderStroke(1.dp, if (isIntroExpanded) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
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
                        containerColor = theme.contentColor.copy(alpha = 0.05f)
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



