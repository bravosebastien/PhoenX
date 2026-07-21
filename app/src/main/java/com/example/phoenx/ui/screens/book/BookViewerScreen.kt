package com.example.phoenx.ui.screens.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.theme.AccentPrimary
import com.example.phoenx.ui.theme.LocalAccentColor

@Composable
fun BookViewerScreen(
    navController: NavController,
    isRecipientMode: Boolean = false,
    targetCreatorId: String? = null,
    viewModel: BookViewerViewModel = hiltViewModel()
) {
    val bookDraft by viewModel.bookDraft.collectAsState()
    val decryptedChapters by viewModel.decryptedChapters.collectAsState()
    val mediaMap by viewModel.mediaMap.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val sealedMessage by viewModel.sealedMessage.collectAsState()
    val creatorName by viewModel.creatorName.collectAsState()

    LaunchedEffect(targetCreatorId) {
        viewModel.loadBook(targetCreatorId)
    }

    if (isLocked && isRecipientMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1410)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AccentPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = sealedMessage ?: "Le Livre de $creatorName vous sera ouvert le moment venu.",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFFF2EDE8),
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (sealedMessage != null) "Ce récit est actuellement scellé." else "Ce récit est actuellement scellé. Seul le Gardien pourra lever le sceau.",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF9B9590),
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Retour", color = Color(0xFF1A1A1F))
                }
            }
        }
        return
    }

    val chapters = bookDraft?.chapters
        ?.sortedBy { it.orderIndex }
        ?: emptyList()

    if (chapters.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1410)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = if (isRecipientMode)
                        "Le livre de vie n'a pas encore été rédigé."
                    else
                        "Ton livre n'est pas encore créé.",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF9B9590),
                        textAlign = TextAlign.Center
                    )
                )
                if (!isRecipientMode) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            navController.navigate("book_editor")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary
                        )
                    ) {
                        Text(
                            "Créer mon livre",
                            color = Color(0xFF1A1A1F)
                        )
                    }
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        pageCount = { chapters.size }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val chapter = chapters[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                Text(
                    text = "Chapitre ${chapter.orderIndex + 1}",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = Color(0xFF5C5855),
                        letterSpacing = 0.1.em
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = chapter.title,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        fontStyle = FontStyle.Italic,
                        color = AccentPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(AccentPrimary)
                )
                Spacer(modifier = Modifier.height(24.dp))

                val decryptedText = decryptedChapters[chapter.id] ?: ""
                IllustrableText(decryptedText, mediaMap)

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1410),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = AccentPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF1C1410)
                        )
                    )
                )
                .padding(bottom = 24.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chapitre ${pagerState.currentPage + 1} / ${chapters.size}",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = AccentPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chapters.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(
                                if (index == pagerState.currentPage)
                                    8.dp else 5.dp
                            )
                            .background(
                                if (index == pagerState.currentPage)
                                    AccentPrimary
                                else
                                    Color(0xFF3E3E45),
                                CircleShape
                            )
                    )
                }
            }

            if (!isRecipientMode) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { navController.navigate("book_editor") }
                ) {
                    Text(
                        "Modifier ce livre",
                        color = Color(0xFF9B9590),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IllustrableText(
    text: String,
    mediaMap: Map<String, OfflineEntry>
) {
    val accent = LocalAccentColor.current
    // Regex pour détecter [PHOTO:uuid] ou [AUDIO:uuid]
    val regex = Regex("\\[(PHOTO|AUDIO):([a-f0-9\\-]+)\\]")
    val parts = text.split(regex)
    val matches = regex.findAll(text).toList()

    Column {
        parts.forEachIndexed { index, part ->
            if (part.isNotBlank()) {
                Text(
                    text = part.trim(),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        color = Color(0xFFF2EDE8),
                        lineHeight = 30.sp
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
                                .clip(RoundedCornerShape(12.dp))
                                .padding(vertical = 16.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (type == "AUDIO" && entry != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = accent.copy(alpha = 0.2f)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = accent, modifier = Modifier.padding(8.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("FRAGMENT VOCAL", style = MaterialTheme.typography.labelSmall, color = accent)
                                Text("L'essence de ce souvenir", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9B9590))
                            }
                        }
                    }
                }
            }
        }
    }
}
