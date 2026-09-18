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

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = when(entry?.entryType) {
                        "QUESTION_ANSWER" -> stringResource(R.string.recipient_memory_detail_topbar_question)
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

                // RÉCIT (Plein écran direct v12.5)
                Column {
                    val récitLabel = if (entry!!.entryType == "QUESTION_ANSWER") stringResource(R.string.recipient_memory_detail_label_response) else stringResource(R.string.recipient_memory_detail_label_story)
                    Text(récitLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = accent.copy(alpha = 0.6f))
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        text = content.ifBlank { stringResource(R.string.recipient_memory_detail_empty_content) },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = if (entry!!.entryType == "QUESTION_ANSWER") FontStyle.Italic else null,
                            color = if (entry!!.entryType == "QUESTION_ANSWER") accent else theme.contentColor.copy(alpha = 0.9f),
                            lineHeight = 28.sp,
                            fontFamily = theme.fontFamily
                        )
                    )
                }

                // COMPLÉMENTS MÉDIA (Affichage Direct v12.5 - Point 3)
                if (complements.isNotEmpty()) {
                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.05f))
                    
                    Text(
                        stringResource(R.string.recipient_memory_detail_complements_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        complements.forEach { complement ->
                            RecipientInlineMediaItem(
                                complement = complement,
                                theme = theme,
                                mediaManager = mediaManager,
                                heirKey = heirKey,
                                creatorId = creatorId,
                                onClick = {
                                    val url = complement.mediaUrl
                                    val isExternal = url?.startsWith("http") == true && 
                                                   (complement.mediaProvider != null || url.contains("spotify") || url.contains("youtube") || url.contains("deezer") || url.contains("youtu.be"))
                                    
                                    if (isExternal) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url!!))
                                            context.startActivity(intent)
                                        } catch(_: Exception) { 
                                            navController.navigate(Screen.MediaViewer.createRoute(complement.id, creatorId, complement.mediaUrl, complement.entryType, complement.aiSummary, "entries"))
                                        }
                                    } else {
                                        navController.navigate(Screen.MediaViewer.createRoute(complement.id, creatorId, complement.mediaUrl, complement.entryType, complement.aiSummary, "entries"))
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
}

@Composable
fun RecipientInlineMediaItem(
    complement: OfflineEntry,
    theme: com.example.phoenx.ui.theme.AppThemeState,
    mediaManager: MediaManager,
    heirKey: ByteArray?,
    creatorId: String,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
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
                    field = if (complement.coverUrl != null) "coverUrl" else null
                )
            } else {
                val icon = when(complement.entryType) {
                    "VIDEO" -> Icons.Default.Videocam
                    "AUDIO" -> Icons.Default.Mic
                    "TEXT" -> Icons.Default.Description
                    else -> Icons.Default.Attachment
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                }
            }

            // Overlay Titre
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when(complement.entryType) {
                        "VIDEO" -> Icons.Default.PlayCircleFilled
                        "AUDIO" -> Icons.Default.Headset
                        else -> Icons.Default.Image
                    }
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = complement.aiSummary.ifBlank { stringResource(R.string.recipient_memory_detail_complement_fallback) },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
