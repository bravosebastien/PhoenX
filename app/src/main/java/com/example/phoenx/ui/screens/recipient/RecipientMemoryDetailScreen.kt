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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = when(entry?.entryType) {
                        "QUESTION_ANSWER" -> "Question de vie"
                        else -> "Souvenir de votre proche"
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
                    Text("Ce souvenir est scellé.", color = theme.contentColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    Text("Il sera déchiffré lors de l'activation de la transmission.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.4f))
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
                    text = entry!!.userTitle.ifBlank { entry!!.aiSummary }.ifBlank { "Souvenir" },
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

                // RÉCIT (Tronqué v9.4.27)
                Column {
                    val récitLabel = if (entry!!.entryType == "QUESTION_ANSWER") "MA RÉPONSE" else "LE RÉCIT"
                    Text(récitLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = theme.contentColor.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { showFullStory = true }
                    ) {
                        Text(
                            text = content.ifBlank { "Pas de contenu écrit." },
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
                            "COMMENTAIRE DE VOTRE PROCHE",
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
                        "COMPLÉMENTS MÉDIA",
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

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // PLEIN ÉCRAN RÉCIT (v9.4.27)
    if (showFullStory) {
        Dialog(onDismissRequest = { showFullStory = false }) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = theme.backgroundColor
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LE RÉCIT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showFullStory = false }) { Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.3f)) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 32.sp, 
                                fontFamily = theme.fontFamily,
                                fontStyle = if (entry?.entryType == "QUESTION_ANSWER") FontStyle.Italic else null,
                                color = if (entry?.entryType == "QUESTION_ANSWER") accent else theme.contentColor
                            )
                        )
                    }
                }
            }
        }
    }
}

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
                    text = complement.aiSummary.ifBlank { "Média" },
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
