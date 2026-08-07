package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.components.PhoenXAvatar
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientDetailScreen(
    recipientId: String,
    onNavigateBack: () -> Unit,
    onComposePortrait: (String) -> Unit,
    onNavigateToPermissions: (String) -> Unit,
    navController: NavController,
    viewModel: RecipientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recipients = (uiState as? RecipientUiState.Success)?.recipients ?: emptyList()
    val recipient = recipients.find { it.id == recipientId }
    
    // Charger le portrait et le dashboard de contenus (v9.4.27)
    val portraitEntry by viewModel.getPortraitForRecipient(recipientId).collectAsState(initial = null)
    val dashboard by remember(recipient) {
        viewModel.getAssignedContent(recipient?.linkedUid)
    }.collectAsState(initial = RecipientContentDashboard())
    
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        recipient?.name ?: "Détails", 
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.contentColor
                )
            )
        }
    ) { padding ->
        if (recipient == null) {
            Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(theme.backgroundColor)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header Profil
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PhoenXAvatar(
                        photoUrl = recipient.photoUrl,
                        name = recipient.name,
                        size = 64.dp,
                        borderColor = accent.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            recipient.name, 
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = theme.fontFamily,
                                fontWeight = FontWeight.Bold
                            ), 
                            color = theme.contentColor
                        )
                        Text(recipient.relationship, style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // ÉTAT DU PORTRAIT
                Text(
                    "SON PORTRAIT (LE MIROIR)", 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                    color = theme.contentColor.copy(alpha = 0.4f), 
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val isPortraitCompleted = portraitEntry != null
                
                Card(
                    onClick = { if (!isPortraitCompleted) onComposePortrait(recipient.id) },
                    enabled = !isPortraitCompleted, // v9.2.6 : Non cliquable si complété
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = theme.contentColor.copy(alpha = 0.05f),
                        disabledContainerColor = theme.contentColor.copy(alpha = 0.03f)
                    ),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isPortraitCompleted) Success.copy(alpha = 0.3f) else accent.copy(alpha = 0.2f)
                    )
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPortraitCompleted) Icons.Default.CheckCircle else Icons.Default.HistoryEdu,
                            contentDescription = null,
                            tint = if (isPortraitCompleted) Success else accent
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isPortraitCompleted) "Portrait complété" else "Portrait non commencé",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = theme.fontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isPortraitCompleted) theme.contentColor.copy(alpha = 0.6f) else theme.contentColor
                            )
                            if (!isPortraitCompleted) {
                                Text(
                                    text = "Dis-lui ce que tu vois en lui/elle.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.contentColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // TABLEAU DE BORD DU CONTENU (v9.4.27)
                Text(
                    "CONTENUS ATTRIBUÉS", 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                    color = theme.contentColor.copy(alpha = 0.4f), 
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 1. SOUVENIRS
                ContentSection(
                    title = "SOUVENIRS",
                    count = dashboard.souvenirs.size,
                    icon = Icons.Default.HistoryEdu,
                    accent = accent,
                    theme = theme,
                    items = dashboard.souvenirs.map { it.aiSummary to it.id },
                    onClickItem = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id)) },
                    onSeeAll = { navController.navigate(Screen.RecipientAllocation.createRoute(recipient.id)) }
                )

                // 2. PHOTOS
                ContentSection(
                    title = "PHOTOS",
                    count = dashboard.photos.size,
                    icon = Icons.Default.PhotoLibrary,
                    accent = accent,
                    theme = theme,
                    items = dashboard.photos.map { it.aiSummary to it.id },
                    onClickItem = { id -> navController.navigate(Screen.MediaViewer.createRoute(id)) },
                    onSeeAll = { navController.navigate(Screen.RecipientPhotos.createRoute(recipient.id)) }
                )

                // 3. VIDÉOS
                ContentSection(
                    title = "VIDÉOS",
                    count = dashboard.videos.size,
                    icon = Icons.Default.VideoLibrary,
                    accent = accent,
                    theme = theme,
                    items = dashboard.videos.map { it.aiSummary to it.id },
                    onClickItem = { id -> navController.navigate(Screen.MediaViewer.createRoute(id)) },
                    onSeeAll = { navController.navigate(Screen.RecipientVideotheque.createRoute(recipient.id)) }
                )

                // 4. AUDIOS
                ContentSection(
                    title = "AUDIOS",
                    count = dashboard.audios.size,
                    icon = Icons.Default.MusicNote,
                    accent = accent,
                    theme = theme,
                    items = dashboard.audios.map { it.aiSummary to it.id },
                    onClickItem = { id -> navController.navigate(Screen.MediaViewer.createRoute(id)) },
                    onSeeAll = { navController.navigate(Screen.RecipientDiscotheque.createRoute(recipient.id)) }
                )

                // 5. EXTRAITS (LITTÉRATURE)
                ContentSection(
                    title = "EXTRAITS",
                    count = dashboard.extraits.size,
                    icon = Icons.Default.TextFields,
                    accent = accent,
                    theme = theme,
                    items = dashboard.extraits.map { it.aiSummary to it.id },
                    onClickItem = { id -> /* Lecture directe ? */ },
                    onSeeAll = { navController.navigate(Screen.RecipientLibrary.createRoute(recipient.id)) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onNavigateToPermissions(recipient.id) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Security, null, tint = accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Gérer les droits d'accès", color = theme.contentColor, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ContentSection(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    theme: AppThemeState,
    items: List<Pair<String, String>>, // Pair(Title, ID)
    onClickItem: (String) -> Unit,
    onSeeAll: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            color = theme.contentColor.copy(alpha = 0.03f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = theme.contentColor,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
                Spacer(Modifier.width(12.dp))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = theme.contentColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(visible = isExpanded && count > 0) {
            Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp)) {
                items.take(5).forEach { (itemTitle, id) ->
                    TextButton(
                        onClick = { onClickItem(id) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = itemTitle.ifBlank { "Sans titre" },
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowForwardIos, null, tint = accent.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
                        }
                    }
                }
                if (count > 5) {
                    TextButton(onClick = onSeeAll, modifier = Modifier.align(Alignment.End)) {
                        Text("Voir tout ($count) →", style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }
            }
        }
    }
}
