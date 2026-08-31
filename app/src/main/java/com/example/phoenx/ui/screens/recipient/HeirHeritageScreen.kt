package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeirHeritageScreen(
    creatorId: String,
    navController: NavController,
    viewModel: RecipientMediaViewModel = hiltViewModel()
) {
    val heritageEntries by viewModel.heritageEntries.collectAsState()
    val heirKey by viewModel.heirKey.collectAsState()
    val bookMessage by viewModel.bookSealedMessage.collectAsState()
    val bookTitle by viewModel.bookTitle.collectAsState()
    val creatorName by viewModel.creatorName.collectAsState()
    val protocolStatus by viewModel.protocolStatus.collectAsState()
    val canAsk by viewModel.canAskQuestions.collectAsState()
    val maxQuestions by viewModel.maxQuestions.collectAsState()
    val questionsAsked by viewModel.questionsAsked.collectAsState()
    val recipientId by viewModel.recipientId.collectAsState()
    val ambiance by viewModel.ambiance.collectAsState()

    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current
    val context = LocalContext.current

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    TransmissionTheme(
        backgroundId = ambiance.backgroundId,
        fontId = ambiance.fontId
    ) {
        val theme = LocalAppTheme.current
        Scaffold(
            containerColor = theme.backgroundColor,
            topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Mon Héritage",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                        Text(
                            "BUILD-DEBUG-v9427-fix3",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = accent),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    if (canAsk && recipientId != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                            IconButton(
                                onClick = { navController.navigate(Screen.AskQuestion.createRoute(creatorId, recipientId!!)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = "Poser une question", tint = accent)
                            }
                            if (maxQuestions != null) {
                                val remaining = (maxQuestions!! - questionsAsked).coerceAtLeast(0)
                                Text(
                                    text = "$remaining rest.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = accent.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "${heritageEntries.size} souvenirs vous ont été destinés",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            // ACCÈS SPÉCIAUX (Livre, Coffre, Quiz)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpecialAccessCard(
                        title = bookTitle ?: "Livre",
                        subtitle = if (protocolStatus != RecipientMediaViewModel.ProtocolStatus.ACTIVATED) bookMessage ?: "$creatorName a décidé de vous partager le livre de sa vie. Visible le moment venu." else "Récit de vie",
                        icon = Icons.Outlined.MenuBook,
                        modifier = Modifier.weight(1.3f),
                        theme = theme
                    ) { navController.navigate("book_viewer_recipient?creatorId=$creatorId") }
                    SpecialAccessCard(
                        title = "Coffre",
                        icon = Icons.Outlined.Lock,
                        modifier = Modifier.weight(1f),
                        theme = theme
                    ) { navController.navigate(Screen.RecipientDetective.createRoute(creatorId)) }
                    SpecialAccessCard(
                        title = "Arbre", // v9.4.27 : Accès à l'Arbre Généalogique
                        icon = Icons.Default.AccountTree,
                        modifier = Modifier.weight(1f),
                        theme = theme
                    ) { navController.navigate(Screen.Genealogy.createRoute(creatorId)) }
                    SpecialAccessCard(
                        title = "Mappemonde", // v9.4.27 : Accès à la Mappemonde
                        icon = Icons.Default.Public,
                        modifier = Modifier.weight(1f),
                        theme = theme
                    ) { navController.navigate(Screen.Map.createRoute(targetCreatorId = creatorId)) }
                    SpecialAccessCard(
                        title = "Quiz",
                        icon = Icons.Outlined.EmojiEvents,
                        modifier = Modifier.weight(1f),
                        theme = theme
                    ) { 
                        // Navigation automatique vers le quiz du créateur (v8.5.9)
                        navController.navigate("quiz_play/$creatorId/main_quiz") 
                    }
                    SpecialAccessCard(
                        title = "Persos",
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f),
                        theme = theme
                    ) { navController.navigate(Screen.Personalities.createRoute(creatorId)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // LISTE DES SOUVENIRS
            items(heritageEntries) { entry ->
                HeritageEntryRow(
                    entry = entry,
                    heirKey = heirKey,
                    mediaManager = viewModel.mediaManager,
                    theme = theme,
                    creatorId = creatorId, // v9.4.27
                    onClick = { 
                        // v9.4.27 : Gestion intelligente du clic (Direct ou Détail)
                        // On vérifie à la fois mediaUrl et le contenu déchiffré
                        val contentStr = String(entry.encryptedContent)
                        val url = if (entry.mediaUrl?.startsWith("http") == true) entry.mediaUrl else contentStr
                        
                        val isExternal = url.startsWith("http") && 
                                       (entry.mediaProvider != null || url.contains("spotify") || url.contains("youtube") || url.contains("deezer"))
                        
                        if (isExternal) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                            } catch(_: Exception) { 
                                navController.navigate("recipient_memory_detail/${entry.id}/$creatorId")
                            }
                        } else {
                            navController.navigate("recipient_memory_detail/${entry.id}/$creatorId")
                        }
                    }
                )
            }
        }
    }
}
}

@Composable
fun SpecialAccessCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    Surface(
        onClick = onClick,
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
        modifier = modifier.heightIn(min = 80.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = theme.contentColor, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                    color = theme.contentColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HeritageEntryRow(
    entry: PhoenXEntry,
    heirKey: ByteArray?,
    mediaManager: MediaManager,
    theme: AppThemeState,
    creatorId: String, // v9.4.27
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH).withZone(ZoneId.systemDefault()) }
    val formattedDate = dateFormatter.format(entry.timestamp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // MINIATURE MÉDIA
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.contentColor.copy(alpha = 0.05f))
            ) {
                if (entry.type == EntryType.PHOTO) {
                    SecureAsyncImage(
                        mediaUrl = entry.mediaUrl,
                        localPath = entry.localMediaPath,
                        explicitKey = heirKey,
                        mediaManager = mediaManager,
                        modifier = Modifier.fillMaxSize(),
                        creatorId = creatorId,           // v9.4.27
                        docType = entry.sourceDocType,   // v9.4.27
                        docId = entry.id                 // v9.4.27
                    )
                } else {
                    val icon = when(entry.type) {
                        EntryType.AUDIO -> Icons.Default.Mic
                        EntryType.VIDEO -> Icons.Default.Videocam
                        else -> Icons.Default.Description
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.aiSummary.ifEmpty { "Souvenir" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Aperçu court
                val preview = String(entry.encryptedContent).take(60) + "..."
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
        }
    }
}
