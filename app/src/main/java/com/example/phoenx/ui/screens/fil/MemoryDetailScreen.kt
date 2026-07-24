package com.example.phoenx.ui.screens.fil

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailScreen(
    entryId: String,
    onNavigateBack: () -> Unit,
    navController: NavController,
    targetCreatorId: String? = null,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsState()
    val complements by viewModel.complements.collectAsState()
    val textComplements by viewModel.decryptedTextComplements.collectAsState()
    val content by viewModel.decryptedContent.collectAsState()
    val structuredPortrait by viewModel.structuredPortrait.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observation du retour du Picker de lieu
    val pickedLocationId by navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("pickedLocationId", null)?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(pickedLocationId) {
        pickedLocationId?.let { id ->
            viewModel.assignLocationFromId(id)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("pickedLocationId")
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var editableTitle by remember { mutableStateOf("") }
    var editableText by remember { mutableStateOf("") }

    LaunchedEffect(entryId, targetCreatorId) {
        viewModel.loadEntry(entryId, targetCreatorId)
    }

    LaunchedEffect(entry, content) {
        if (entry != null) {
            if (editableTitle.isEmpty()) editableTitle = entry!!.aiSummary
            if (editableText.isEmpty() || entry!!.parentEntryId != null) {
                editableText = content
            }
        }
    }

    // Sauvegarde auto du titre (Sujet)
    LaunchedEffect(editableTitle) {
        if (entry != null && !entry!!.isChild() && entry!!.entryType != "QUESTION_ANSWER") {
            if (editableTitle.isNotEmpty() && editableTitle != entry!!.aiSummary) {
                delay(1000)
                viewModel.updateTitle(editableTitle)
            }
        }
    }

    // Sauvegarde auto du texte avec debounce
    LaunchedEffect(editableText) {
        if (editableText.isNotEmpty() && editableText != content) {
            delay(1000)
            viewModel.updateContent(editableText)
        }
    }

    // Retour après suppression réussie
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            onNavigateBack()
        }
    }

    // Affichage des erreurs
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Supprimer ce souvenir ?", color = theme.contentColor) },
            text = { Text("Cette action est irréversible et supprimera le souvenir de votre fil ainsi que du Cloud.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.deleteMemory()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Supprimer", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = when(entry?.entryType) {
                        "PORTRAIT" -> entry?.aiSummary ?: "Portrait"
                        "QUESTION_ANSWER" -> "Question : ${entry?.aiSummary}"
                        else -> {
                            if (entry?.parentEntryId != null) "Réponse au Portrait"
                            else "L'Étincelle & son Récit"
                        }
                    }
                    Text(titleText, style = MaterialTheme.typography.labelLarge, color = theme.contentColor) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (entry == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            val isChildEntry = entry!!.parentEntryId != null

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // ÉTAPE 1 : LE SUJET (Titre / Question)
                if (entry!!.entryType != "PORTRAIT") {
                    Column {
                        val subjectLabel = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") "LA QUESTION" else "LE SUJET"
                        Text(subjectLabel, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = theme.contentColor.copy(alpha = 0.05f)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") {
                                    Text(
                                        text = entry!!.aiSummary,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = theme.fontFamily,
                                            fontStyle = FontStyle.Italic,
                                            color = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") accent else theme.contentColor
                                        )
                                    )
                                } else {
                                    TextField(
                                        value = editableTitle,
                                        onValueChange = { editableTitle = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { 
                                            Text(
                                                "Donne un titre à ce souvenir...", 
                                                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                                                color = theme.contentColor.copy(alpha = 0.4f)
                                            ) 
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = theme.fontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.contentColor
                                        ),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = theme.contentColor,
                                            unfocusedTextColor = theme.contentColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Titre fixe pour le Portrait
                    Column {
                        Text("LE SUJET", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = entry!!.aiSummary,
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                    }
                }

                // ÉTAPE 2 : LE RÉCIT / LA RÉPONSE
                if (entry!!.entryType == "PORTRAIT") {
                    PortraitAccordion(
                        items = structuredPortrait, 
                        accent = accent,
                        onEditItem = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id, targetCreatorId)) }
                    )
                } else {
                    Column {
                        val récitLabel = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") "MA RÉPONSE" else "LE RÉCIT"
                        Text(récitLabel, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (isChildEntry || textComplements.isEmpty()) {
                            // Édition en place pour les réponses atomiques
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                            ) {
                                TextField(
                                    value = editableText,
                                    onValueChange = { editableText = it },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, color = theme.contentColor),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = theme.contentColor,
                                        unfocusedTextColor = theme.contentColor
                                    )
                                )
                            }
                        } else {
                            // Liste des compléments texte
                            textComplements.forEach { (compId, text) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FormatQuote, null, tint = accent.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                                            Row {
                                                IconButton(onClick = { navController.navigate(Screen.MemoryDetail.createRoute(compId, targetCreatorId)) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Edit, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                IconButton(onClick = { viewModel.deleteComplement(compId) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = theme.fontFamily,
                                                lineHeight = 24.sp
                                            ),
                                            color = theme.contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ON CACHE LE RESTE POUR LES RÉPONSES ATOMIQUES (v8.5.9)
                if (!isChildEntry) {
                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp)

                    MemoryMetadataSection(
                        entry = entry!!,
                        viewModel = viewModel,
                        theme = theme,
                        accent = accent,
                        navController = navController,
                        recipients = recipients
                    )

                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp)

                    // COMPLÉMENTS MÉDIA
                    MemoryComplementsSection(
                        entryId = entryId,
                        complements = complements,
                        targetCreatorId = targetCreatorId,
                        viewModel = viewModel,
                        theme = theme,
                        accent = accent,
                        navController = navController
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

