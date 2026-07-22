package com.example.phoenx.ui.screens.book

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.model.BookTheme
import com.example.phoenx.ui.theme.LocalAccentColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderFlowScreen(
    navController: NavController,
    targetCreatorId: String? = null,
    viewModel: BookViewerViewModel = hiltViewModel()
) {
    val bookDraft by viewModel.bookDraft.collectAsState()
    val decryptedChapters by viewModel.decryptedChapters.collectAsState()
    val decryptedIntro by viewModel.decryptedGlobalIntro.collectAsState()
    val mediaMap by viewModel.mediaMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val fontSizeScale by viewModel.fontSizeScale.collectAsState()

    val theme = bookDraft?.theme ?: BookTheme()
    val fontFamily = BookThemeOptions.getFont(theme.fontId)
    val background = BookThemeOptions.getBackground(theme.backgroundId)
    val textColor = if (background.darkText) Color(0xFF1A1A1A) else Color(0xFFF2EDE8)
    val accent = LocalAccentColor.current

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showResumeBanner by remember { mutableStateOf(false) }

    // 1. Détection de position pour le marque-page avec Debounce (v8.7.0)
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        // On attend 2 secondes d'immobilité totale avant de sauvegarder
        kotlinx.coroutines.delay(2000)
        
        if (listState.firstVisibleItemIndex > 0) {
            val userId = targetCreatorId ?: bookDraft?.userId
            if (userId != null) {
                viewModel.saveReadingProgress(
                    userId, 
                    listState.firstVisibleItemIndex, 
                    listState.firstVisibleItemScrollOffset
                )
            }
        }
    }

    // 2. Affichage du bandeau de reprise
    LaunchedEffect(readingProgress) {
        if (readingProgress != null && readingProgress!!.itemIndex > 0) {
            showResumeBanner = true
        }
    }

    LaunchedEffect(targetCreatorId) {
        viewModel.loadBook(targetCreatorId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon Livre de Vie", fontFamily = fontFamily) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                    }
                },
                actions = {
                    // Menu de Confort (v8.7.0)
                    var showSizeMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSizeMenu = true }) {
                        Icon(Icons.Default.TextFields, "Taille du texte", tint = textColor)
                    }
                    DropdownMenu(
                        expanded = showSizeMenu,
                        onDismissRequest = { showSizeMenu = false },
                        containerColor = background.color,
                    ) {
                        Text("Confort de lecture", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                            IconButton(onClick = { viewModel.updateFontSize(fontSizeScale - 0.1f) }) {
                                Icon(Icons.Default.Remove, null, tint = textColor)
                            }
                            Text("${(fontSizeScale * 100).toInt()}%", color = textColor, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { viewModel.updateFontSize(fontSizeScale + 0.1f) }) {
                                Icon(Icons.Default.Add, null, tint = textColor)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background.color.copy(alpha = 0.95f),
                    titleContentColor = textColor
                )
            )
        },
        containerColor = background.color
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp, start = 28.dp, end = 28.dp, top = 20.dp)
                ) {
                    // ── PAGE DE GARDE ──
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Livre de Vie",
                                style = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.Light, color = textColor.copy(alpha = 0.6f))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = viewModel.creatorName.collectAsState().value,
                                style = TextStyle(fontFamily = fontFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor)
                            )
                            Spacer(Modifier.height(40.dp))
                            HorizontalDivider(modifier = Modifier.width(60.dp), thickness = 1.dp, color = accent.copy(alpha = 0.4f))
                        }
                    }

                    // ── INTRODUCTION GLOBALE ──
                    if (decryptedIntro.isNotEmpty()) {
                        item {
                            Text(
                                text = decryptedIntro,
                                style = TextStyle(
                                    fontFamily = fontFamily,
                                    fontSize = (18 * fontSizeScale).sp,
                                    lineHeight = (32 * fontSizeScale).sp,
                                    color = textColor,
                                    fontStyle = FontStyle.Italic
                                ),
                                modifier = Modifier.padding(bottom = 60.dp)
                            )
                        }
                    }

                    // ── CHAPITRES ──
                    bookDraft?.chapters?.sortedBy { it.orderIndex }?.forEach { chapter ->
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Chapitre ${chapter.orderIndex + 1}",
                                    style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = accent.copy(alpha = 0.7f), letterSpacing = 2.sp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = chapter.title,
                                    style = TextStyle(fontFamily = fontFamily, fontSize = (26 * fontSizeScale).sp, fontWeight = FontWeight.Bold, color = textColor)
                                )
                                Spacer(Modifier.height(32.dp))

                                val content = decryptedChapters[chapter.id] ?: ""
                                ReaderIllustrableText(content, mediaMap, fontFamily, textColor, accent, fontSizeScale)

                                Spacer(Modifier.height(60.dp))
                                HorizontalDivider(modifier = Modifier.fillMaxWidth(0.3f).align(Alignment.CenterHorizontally), color = textColor.copy(alpha = 0.1f))
                                Spacer(Modifier.height(60.dp))
                            }
                        }
                    }
                }

                // BANDEAU DE REPRISE (MARQUE-PAGE)
                AnimatedVisibility(
                    visible = showResumeBanner,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Reprendre la lecture ?", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row {
                                TextButton(onClick = { showResumeBanner = false }) {
                                    Text("Ignorer", color = Color.White.copy(alpha = 0.7f))
                                }
                                Button(
                                    onClick = {
                                        showResumeBanner = false
                                        coroutineScope.launch {
                                            readingProgress?.let { progress ->
                                                // Recalcul de l'offset selon le ratio de taille (v8.7.0)
                                                val ratio = fontSizeScale / progress.savedAtScale
                                                val adjustedOffset = (progress.offset * ratio).toInt()
                                                
                                                listState.scrollToItem(progress.itemIndex, adjustedOffset)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                ) {
                                    Text("Reprendre", color = accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderIllustrableText(
    text: String,
    mediaMap: Map<String, OfflineEntry>,
    fontFamily: FontFamily,
    textColor: Color,
    accent: Color,
    fontSizeScale: Float
) {
    val regex = Regex("\\[(PHOTO|AUDIO):([a-f0-9\\-]+)\\]")
    val parts = text.split(regex)
    val matches = regex.findAll(text).toList()

    Column {
        parts.forEachIndexed { index, part ->
            if (part.isNotBlank()) {
                Text(
                    text = part.trim(),
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontSize = (18 * fontSizeScale).sp,
                        color = textColor,
                        lineHeight = (32 * fontSizeScale).sp
                    ),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            
            if (index < matches.size) {
                val type = matches[index].groupValues[1]
                val id = matches[index].groupValues[2]
                val entry = mediaMap[id]

                if (type == "PHOTO" && entry != null) {
                    val mediaSource = entry.localMediaPath ?: entry.mediaUrl
                    if (mediaSource != null) {
                        AsyncImage(
                            model = mediaSource,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .padding(vertical = 16.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (type == "AUDIO" && entry != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = accent.copy(alpha = 0.2f)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = accent, modifier = Modifier.padding(8.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("FRAGMENT VOCAL", style = MaterialTheme.typography.labelSmall, color = accent)
                                Text("Écouter ce souvenir", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}
