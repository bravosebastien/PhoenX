package com.example.phoenx.ui.screens.rankings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankItemDetailScreen(
    rankingId: String,
    rankIndex: Int,
    navController: NavController,
    targetCreatorId: String? = null,
    viewModel: RankingViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    val isReadOnly = targetCreatorId != null

    val ranking by viewModel.currentRanking.collectAsState()
    val rankMediaMap by viewModel.rankMediaMap.collectAsState()

    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    var showEditTextDialog by remember { mutableStateOf(false) }

    val rankMediaList = rankMediaMap[rankIndex] ?: emptyList()

    val rankMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video")
            val file = if (isVideo) {
                com.example.phoenx.ui.util.ImageUtils.copyUriToTempFile(context, uri)
            } else {
                com.example.phoenx.ui.util.ImageUtils.compressAndResize(context, uri)
            }
            if (file != null) {
                viewModel.attachRankMedia(rankingId, rankIndex, file, isVideo)
            }
        }
    }

    fun triggerAddMedia() {
        if (rankMediaList.size >= 5) {
            Toast.makeText(
                context,
                context.getString(R.string.ranking_media_limit_reached),
                Toast.LENGTH_LONG
            ).show()
        } else {
            rankMediaPickerLauncher.launch("*/*")
        }
    }

    LaunchedEffect(rankingId, targetCreatorId) {
        viewModel.setTargetCreator(targetCreatorId)
        viewModel.setRankingId(rankingId)
    }

    if (ranking == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent)
        }
        return
    }

    val currentRanking = ranking!!
    val rawText = currentRanking.items.getOrNull(rankIndex) ?: ""
    val rankTitle = rawText.ifBlank { stringResource(R.string.ranking_detail_dialog_item_title, rankIndex + 1) }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ranking_detail_dialog_item_title, rankIndex + 1),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold
                        ),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // EN-TÊTE DU RANG
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = theme.contentColor.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = accent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${rankIndex + 1}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = theme.backgroundColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (rawText.isNotBlank()) rawText else stringResource(R.string.ranking_detail_row_empty),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = theme.fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontStyle = if (rawText.isBlank()) FontStyle.Italic else FontStyle.Normal
                            ),
                            color = if (rawText.isBlank()) theme.contentColor.copy(alpha = 0.4f) else theme.contentColor
                        )
                    }

                    if (!isReadOnly) {
                        IconButton(onClick = { showEditTextDialog = true }) {
                            Icon(Icons.Default.Edit, null, tint = accent)
                        }
                    }
                }
            }

            // SECTION MÉDIAS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ranking_gallery_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = theme.fontFamily
                    ),
                    color = accent
                )

                // Indicateur de limite "X/5"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, accent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = stringResource(R.string.ranking_media_count_indicator, rankMediaList.size),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (rankMediaList.isNotEmpty()) {
                val chunkedMedia = rankMediaList.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunkedMedia.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { mediaItem ->
                                val activeUrl = mediaItem.localThumbnailPath ?: mediaItem.thumbnailPath ?: mediaItem.mediaPath
                                val fieldParam = if (mediaItem.thumbnailPath != null) "thumbnailPath" else "mediaPath"

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(theme.contentColor.copy(alpha = 0.05f))
                                        .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            navController.navigate(
                                                com.example.phoenx.ui.navigation.Screen.MediaViewer.createRoute(
                                                    entryId = mediaItem.id,
                                                    creatorId = targetCreatorId,
                                                    mediaUrl = mediaItem.mediaPath,
                                                    entryType = mediaItem.mediaType,
                                                    aiSummary = rankTitle,
                                                    sourceDocType = "rankMedia",
                                                    personId = rankingId,
                                                    isEncrypted = false
                                                )
                                            )
                                        }
                                ) {
                                    SecureAsyncImage(
                                        mediaUrl = activeUrl,
                                        mediaManager = mediaManager,
                                        isEncrypted = false,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        creatorId = targetCreatorId,
                                        docType = "rankMedia",
                                        docId = mediaItem.id,
                                        field = fieldParam,
                                        personId = rankingId
                                    )

                                    if (mediaItem.mediaType == "VIDEO") {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp).align(Alignment.Center)
                                        )
                                    }

                                    if (!isReadOnly) {
                                        IconButton(
                                            onClick = {
                                                viewModel.removeRankMedia(rankingId, mediaItem.id)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.6f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp).padding(3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.contentColor.copy(alpha = 0.02f),
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.ranking_detail_no_image),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // BOUTONS D'ACTION (CRÉATEUR)
            if (!isReadOnly) {
                Button(
                    onClick = { triggerAddMedia() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = theme.backgroundColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.ranking_dialog_add_media),
                        color = theme.backgroundColor,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // DIALOGUE ÉDITION TEXTE
    if (showEditTextDialog) {
        var newText by remember { mutableStateOf(rawText) }
        AlertDialog(
            onDismissRequest = { showEditTextDialog = false },
            title = { Text(stringResource(R.string.ranking_detail_dialog_item_title, rankIndex + 1), color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it.replace("|", "") },
                    placeholder = { Text(stringResource(R.string.ranking_detail_dialog_item_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
            },
            confirmButton = {
                Row {
                    if (rawText.isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.clearRankItem(rankingId, rankIndex)
                                showEditTextDialog = false
                            }
                        ) { Text(stringResource(R.string.ranking_detail_dialog_item_button_clear), color = com.example.phoenx.ui.theme.Error) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            viewModel.updateRankItem(rankingId, rankIndex, newText)
                            showEditTextDialog = false
                        },
                        enabled = newText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) { Text(stringResource(R.string.ranking_detail_dialog_item_button_save), color = theme.backgroundColor) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTextDialog = false }) { Text(stringResource(R.string.ranking_detail_dialog_item_button_cancel), color = theme.contentColor.copy(alpha = 0.6f)) }
            },
            containerColor = theme.backgroundColor
        )
    }
}
