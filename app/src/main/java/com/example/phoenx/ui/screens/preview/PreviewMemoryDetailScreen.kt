package com.example.phoenx.ui.screens.preview

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush
import com.example.phoenx.ui.theme.phoenXMatiere
import dagger.hilt.android.EntryPointAccessors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreviewMemoryDetailScreen(
    entryId: String,
    recipientUid: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit, // v9.4.27
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
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

    LaunchedEffect(recipientUid) {
        viewModel.loadPreview(recipientUid)
    }

    val entry = remember(state.allFilteredEntries, entryId) {
        state.allFilteredEntries.find { it.id == entryId }
    }

    val complements = remember(state.allFilteredEntries, entryId) {
        state.allFilteredEntries.filter { it.parentEntryId == entryId }
    }

    var selectedComplement by remember { mutableStateOf<OfflineEntry?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { Text("Détail (Aperçu)", style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (entry == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isLoading) CircularProgressIndicator(color = accent)
                else Text("Souvenir introuvable ou non partagé", color = theme.contentColor.copy(alpha = 0.5f))
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
                    text = entry.aiSummary.ifBlank { "Sans titre" },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )

                // QUAND ET OÙ
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val dateText = remember(entry) {
                        when {
                            entry.memoryDate != null -> SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date(entry.memoryDate))
                            entry.memoryDateStart != null && entry.memoryDateEnd != null -> {
                                val start = SimpleDateFormat("dd/MM/yy").format(Date(entry.memoryDateStart))
                                val end = SimpleDateFormat("dd/MM/yy").format(Date(entry.memoryDateEnd))
                                "$start - $end"
                            }
                            else -> SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date(entry.createdAt))
                        }
                    }
                    PreviewInfoChip(icon = Icons.Default.Event, text = dateText, accent = accent, theme = theme)
                    
                    if (!entry.locationName.isNullOrBlank()) {
                        PreviewInfoChip(icon = Icons.Default.LocationOn, text = entry.locationName!!, accent = accent, theme = theme)
                    }
                }

                // RÉCIT
                Surface(
                    color = theme.contentColor.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val decryptedText = remember(entry) { viewModel.decryptContent(entry.encryptedPayload, entry.aiSummary) }
                    Text(
                        text = decryptedText.ifBlank { "Pas de contenu écrit." },
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.8f),
                        lineHeight = 24.sp
                    )
                }
                
                // COMMENTAIRE
                if (!entry.userComment.isNullOrBlank()) {
                    Column {
                        Text(
                            "COMMENTAIRE DU CRÉATEUR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = entry.userComment!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = theme.contentColor.copy(alpha = 0.7f)
                        )
                    }
                }

                // COMPLÉMENTS
                if (complements.isNotEmpty()) {
                    Text(
                        "COMPLÉMENTS MÉDIA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        complements.forEach { complement ->
                            PreviewComplementItem(
                                complement = complement,
                                theme = theme,
                                mediaManager = mediaManager,
                                onClick = { 
                                    if (complement.mediaUrl?.startsWith("http") == true) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(complement.mediaUrl!!))
                                            context.startActivity(intent)
                                        } catch(_: Exception) { /* ... */ }
                                    } else {
                                        selectedComplement = complement
                                        showSheet = true
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                
                Surface(
                    color = accent.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Ce souvenir est partagé avec ${state.recipientName} selon vos règles de visibilité.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (showSheet && selectedComplement != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = theme.backgroundColor,
                scrimColor = Color.Black.copy(alpha = 0.5f)
            ) {
                @androidx.media3.common.util.UnstableApi
                PreviewMediaPanel(
                    entry = selectedComplement!!,
                    mediaManager = mediaManager,
                    onDismiss = { showSheet = false }
                )
            }
        }
    }
}

@Composable
fun PreviewInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, accent: Color, theme: com.example.phoenx.ui.theme.AppThemeState) {
    Surface(
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
fun PreviewComplementItem(
    complement: OfflineEntry, 
    theme: com.example.phoenx.ui.theme.AppThemeState,
    mediaManager: MediaManager,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
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
            val hasImage = complement.coverUrl != null || complement.localCoverPath != null || complement.localMediaPath != null
            if (hasImage) {
                SecureAsyncImage(
                    mediaUrl = complement.coverUrl ?: complement.mediaUrl,
                    localPath = complement.localCoverPath ?: complement.localMediaPath,
                    mediaManager = mediaManager,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                val icon = when(complement.entryType) {
                    "PHOTO" -> Icons.Default.PhotoCamera
                    "VIDEO" -> Icons.Default.Videocam
                    "AUDIO" -> Icons.Default.Mic
                    else -> Icons.Default.Description
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
