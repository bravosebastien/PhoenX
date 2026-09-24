package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.screens.fil.MemoryDetailViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush
import com.example.phoenx.ui.theme.phoenXMatiere
import dagger.hilt.android.EntryPointAccessors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipientMemoryDetailScreen(
    entryId: String,
    creatorId: String,
    onNavigateBack: () -> Unit,
    navController: NavController,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsState()
    val complements by viewModel.decryptedComplements.collectAsState() // v9.4.27 : Payload déchiffré
    val content by viewModel.decryptedContent.collectAsState()
    val heirKey by viewModel.heirKey.collectAsState()
    val protocolStatus by viewModel.protocolStatus.collectAsState()

    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current
    val context = LocalContext.current
    
    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    LaunchedEffect(entryId, creatorId) {
        android.util.Log.d("PHOENX_MEMORY_OPEN_TRACE", "UI: Demande ouverture id=$entryId pour creator=$creatorId")
        viewModel.loadEntry(entryId, creatorId)
    }

    LaunchedEffect(entry, complements, content) {
        if (entry != null) {
            android.util.Log.d("PHOENX_MEMORY_OPEN_TRACE", "UI: Data reçue. Title=${entry?.aiSummary}, Complements=${complements.size}, ContentLength=${content.length}")
        }
    }

    var showFullStory by remember { mutableStateOf(false) }
    var showGuessDialog by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    
    val attempts by viewModel.attempts.collectAsState()
    val isGuessCorrect by viewModel.isGuessCorrect.collectAsState()
    val error by viewModel.error.collectAsState()

    // v12.7.6 : Vérification de l'état de l'énigme
    val isEnigmaLocked = remember(entry, isGuessCorrect) {
        if (isGuessCorrect) return@remember false
        val e = entry ?: return@remember false
        if (e.enigmaQuestion == null) return@remember false
        
        val autoDays = e.enigmaAutoUnlockDays ?: 0
        val daysSinceCreation = ((System.currentTimeMillis() - e.createdAt) / (1000 * 60 * 60 * 24)).toInt()
        val isAutoUnlocked = e.enigmaAutoUnlockDays != null && daysSinceCreation >= autoDays
        
        e.unlockedAt == null && !isAutoUnlocked
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = when {
                        entry?.isYoungSelfLetter == true -> stringResource(R.string.recipient_young_self_detail_title, entry?.targetAge ?: 0)
                        entry?.entryType == "QUESTION_ANSWER" -> stringResource(R.string.recipient_memory_detail_topbar_question)
                        else -> stringResource(R.string.recipient_memory_detail_topbar_memory)
                    }
                    Text(titleText, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (entry == null || protocolStatus == MemoryDetailViewModel.ProtocolStatus.VERIFYING) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else if (protocolStatus == MemoryDetailViewModel.ProtocolStatus.LOCKED) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = theme.contentColor.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.recipient_memory_detail_sealed_title), color = theme.contentColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.recipient_memory_detail_sealed_subtitle), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.4f))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // TITRE
                Text(
                    text = entry!!.userTitle.ifBlank { entry!!.aiSummary }.ifBlank { stringResource(R.string.recipient_memory_detail_entry_fallback_title) },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )

                // QUAND ET OÙ (Cliquables v9.4.27)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val dateText = when {
                        entry!!.memoryDate != null -> SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date(entry!!.memoryDate!!))
                        entry!!.memoryDateStart != null && entry!!.memoryDateEnd != null -> {
                            val start = SimpleDateFormat("dd/MM/yy").format(Date(entry!!.memoryDateStart!!))
                            val end = SimpleDateFormat("dd/MM/yy").format(Date(entry!!.memoryDateEnd!!))
                            "$start - $end"
                        }
                        else -> SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date(entry!!.createdAt))
                    }
                    
                    RecipientInfoChip(
                        icon = Icons.Default.Event, 
                        text = dateText, 
                        accent = accent, 
                        theme = theme,
                        onClick = {
                            // Focus sur la Mappemonde à cette date/période (v9.4.27)
                            navController.navigate(Screen.Map.createRoute(targetCreatorId = creatorId, focusEntryId = entry!!.id))
                        }
                    )
                    
                    if (!entry!!.locationName.isNullOrBlank()) {
                        RecipientInfoChip(
                            icon = Icons.Default.LocationOn, 
                            text = entry!!.locationName!!, 
                            accent = accent, 
                            theme = theme,
                            onClick = {
                                navController.navigate(Screen.Map.createRoute(targetCreatorId = creatorId, focusEntryId = entry!!.id))
                            }
                        )
                    }
                }

                if (isEnigmaLocked) {
                    // ÉTAT VÉROUILLÉ (v12.7.6)
                    Surface(
                        color = theme.contentColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Lock, 
                                null, 
                                modifier = Modifier.size(48.dp), 
                                tint = accent
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "Ce souvenir est protégé par une énigme",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = theme.contentColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Vous devez deviner la réponse pour débloquer ce contenu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = { showGuessDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                modifier = Modifier.phoenXMatiere()
                            ) {
                                Text("Tenter ma chance", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // IMAGE PRINCIPALE (v12.3)
                    if (entry!!.mediaUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.contentColor.copy(alpha = 0.05f))
                    ) {
                        SecureAsyncImage(
                            mediaUrl = entry!!.mediaUrl,
                            mediaManager = mediaManager,
                            explicitKey = heirKey,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            creatorId = creatorId,
                            docType = "entries",
                            docId = entry!!.id
                        )
                    }
                }

                // RÉCIT (Tronqué v9.4.27)
                Column {
                    val récitLabel = if (entry!!.entryType == "QUESTION_ANSWER") stringResource(R.string.recipient_memory_detail_label_response) else stringResource(R.string.recipient_memory_detail_label_story)
                    Text(récitLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = theme.contentColor.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { showFullStory = true }
                    ) {
                        Text(
                            text = content.ifBlank { stringResource(R.string.recipient_memory_detail_empty_content) },
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = if (entry!!.entryType == "QUESTION_ANSWER") FontStyle.Italic else null,
                                color = if (entry!!.entryType == "QUESTION_ANSWER") accent else theme.contentColor.copy(alpha = 0.8f)
                            ),
                            lineHeight = 24.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // COMMENTAIRE
                if (!entry!!.userComment.isNullOrBlank()) {
                    Column {
                        Text(
                            stringResource(R.string.recipient_memory_detail_comment_label),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = entry!!.userComment!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = theme.contentColor.copy(alpha = 0.7f)
                        )
                    }
                }

                // COMPLÉMENTS (Grille unifiée v9.4.27)
                if (complements.isNotEmpty()) {
                    Text(
                        stringResource(R.string.recipient_memory_detail_complements_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        complements.forEach { complement ->
                            RecipientComplementItem(
                                complement = complement,
                                theme = theme,
                                mediaManager = mediaManager,
                                heirKey = heirKey,
                                creatorId = creatorId, // v9.4.27
                                onClick = {
                                    // Gestion des liens externes (v9.4.27)
                                    val url = complement.mediaUrl
                                    val isExternal = url?.startsWith("http") == true && 
                                                   (complement.mediaProvider != null || url.contains("spotify") || url.contains("youtube") || url.contains("deezer") || url.contains("youtu.be"))
                                    
                                    if (isExternal) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url!!))
                                            context.startActivity(intent)
                                        } catch(_: Exception) { 
                                            navController.navigate(Screen.MediaViewer.createRoute(
                                                entryId = complement.id,
                                                creatorId = creatorId,
                                                mediaUrl = complement.mediaUrl,
                                                entryType = complement.entryType,
                                                aiSummary = complement.aiSummary,
                                                sourceDocType = "entries"
                                            ))
                                        }
                                    } else {
                                        navController.navigate(Screen.MediaViewer.createRoute(
                                            entryId = complement.id,
                                            creatorId = creatorId,
                                            mediaUrl = complement.mediaUrl,
                                            entryType = complement.entryType,
                                            aiSummary = complement.aiSummary,
                                            sourceDocType = "entries"
                                        ))
                                    }
                                }
                            )
                        }
                    }
                }

                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showGuessDialog && entry != null) {
        AlertDialog(
            onDismissRequest = { showGuessDialog = false; answer = ""; viewModel.clearError() },
            containerColor = theme.backgroundColor,
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint, 
                        null, 
                        tint = accent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.detective_personal_enigma), 
                        color = theme.contentColor,
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // PHOTO ÉVENTUELLE (v12.3) - Affichage en grand pour servir de support à la devinette
                    if (entry!!.mediaUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.contentColor.copy(alpha = 0.05f))
                        ) {
                            SecureAsyncImage(
                                mediaUrl = entry!!.mediaUrl,
                                mediaManager = mediaManager,
                                isEncrypted = false,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                creatorId = creatorId,
                                docType = "entries",
                                docId = entry!!.id
                            )
                        }
                    }

                    Text(entry!!.enigmaQuestion ?: "", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily), color = theme.contentColor)
                    
                    // INDICATION DU TYPE DE RÉPONSE / NOMBRE DE MOTS (v12.8)
                    if (entry!!.answerType == "NUMBER") {
                        Text(
                            text = "Un chiffre est attendu",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    } else if (entry!!.expectedWordCount != null && entry!!.expectedWordCount!! > 0) {
                        Text(
                            text = "Réponse attendue en ${entry!!.expectedWordCount} mot(s)",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }

                    // AFFICHAGE DE L'INDICE
                    if (attempts >= 3 && !entry!!.enigmaHint.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.detective_hint_label, entry!!.enigmaHint ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.contentColor
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text(stringResource(R.string.detective_answer_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = if (entry!!.answerType == "NUMBER") androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                            focusedTextColor = theme.contentColor,
                            unfocusedTextColor = theme.contentColor
                        )
                    )
                    if (error != null) {
                        Text(error!!, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.attemptUnlock(answer) },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(stringResource(R.string.detective_verify_button), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LaunchedEffect(isGuessCorrect) {
        if (isGuessCorrect) {
            showGuessDialog = false
            answer = ""
        }
    }
}

@Composable
fun RecipientComplementItem(
    complement: OfflineEntry,
    theme: com.example.phoenx.ui.theme.AppThemeState,
    mediaManager: MediaManager,
    heirKey: ByteArray?,
    creatorId: String, // v9.4.27
    onClick: () -> Unit
) {
    val accent = theme.accentColor

    // Diagnostic v9.4.27 (PHOENX_MEMORY_OPEN_TRACE)
    LaunchedEffect(complement) {
        val targetUrl = complement.coverUrl ?: complement.mediaUrl
        android.util.Log.d("PHOENX_MEMORY_OPEN_TRACE",
            "Complement ID: ${complement.id} | Type: ${complement.entryType} | " +
            "Raw mediaUrl: ${complement.mediaUrl} | coverUrl: ${complement.coverUrl} | " +
            "Final targetUrl: $targetUrl | Summary: ${complement.aiSummary}"
        )
    }

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp)
            .clickable { onClick() }
            .phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val hasImage = complement.coverUrl != null || complement.localCoverPath != null || complement.localMediaPath != null || complement.entryType == "PHOTO"
            if (hasImage) {
                SecureAsyncImage(
                    mediaUrl = complement.coverUrl ?: complement.mediaUrl,
                    localPath = complement.localCoverPath ?: complement.localMediaPath,
                    explicitKey = heirKey,
                    mediaManager = mediaManager,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    creatorId = creatorId,
                    docType = "entries",
                    docId = complement.id,
                    field = if (complement.coverUrl != null) "coverUrl" else null // v9.4.27
                )
            } else {
                val icon = when(complement.entryType) {
                    "VIDEO" -> Icons.Default.Videocam
                    "AUDIO" -> Icons.Default.Mic
                    "TEXT" -> Icons.Default.Description
                    else -> Icons.Default.Attachment
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = complement.aiSummary.ifBlank { stringResource(R.string.recipient_memory_detail_complement_fallback) },
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * RESTAURÉ le 18/09 — cette fonction avait disparu de la fin du fichier alors
 * qu'elle est appelée deux fois plus haut. Reprise à l'identique de la version
 * saine du dernier commit (3934e14).
 */
@Composable
fun RecipientInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, accent: Color, theme: com.example.phoenx.ui.theme.AppThemeState, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = theme.contentColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = theme.contentColor)
        }
    }
}
