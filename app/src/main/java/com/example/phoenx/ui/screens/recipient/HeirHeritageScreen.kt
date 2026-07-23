package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
    val creatorName by viewModel.creatorName.collectAsState()
    val isActivated by viewModel.isProtocolActivated.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mon Héritage",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
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
                        title = "Livre",
                        subtitle = if (!isActivated) bookMessage ?: "$creatorName a décidé de vous partager le livre de sa vie. Visible le moment venu." else "Récit de vie",
                        icon = Icons.Outlined.MenuBook,
                        modifier = Modifier.weight(1.3f),
                        theme = theme,
                        onClick = { navController.navigate("book_viewer_recipient?creatorId=$creatorId") }
                    )
                    SpecialAccessCard(
                        title = "Coffre",
                        icon = Icons.Outlined.Lock,
                        modifier = Modifier.weight(1f),
                        theme = theme,
                        onClick = { navController.navigate(Screen.RecipientDetective.createRoute(creatorId)) }
                    )
                    SpecialAccessCard(
                        title = "Quiz",
                        icon = Icons.Outlined.EmojiEvents,
                        modifier = Modifier.weight(1f),
                        theme = theme,
                        onClick = { 
                            // Navigation automatique vers le quiz du créateur (v8.5.9)
                            navController.navigate("quiz_play/$creatorId/main_quiz") 
                        }
                    )
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
                    onClick = { navController.navigate(Screen.MemoryDetail.createRoute(entry.id, creatorId)) }
                )
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
                        modifier = Modifier.fillMaxSize()
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
