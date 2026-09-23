package com.example.phoenx.ui.screens.rankings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.OnboardingPopup
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.ThemeViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingDetailScreen(
    rankingId: String,
    navController: NavController,
    targetCreatorId: String? = null,
    viewModel: RankingViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ranking by viewModel.currentRanking.collectAsState()
    val rankMediaMap by viewModel.rankMediaMap.collectAsState()
    
    val isReadOnly = targetCreatorId != null

    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var selectedRankIndexForMedia by remember { mutableStateOf<Int?>(null) }

    // Onboarding Pop-up explicatif (v12.8) - UNIQUEMENT POUR LE CRÉATEUR
    if (!isReadOnly) {
        OnboardingPopup(
            pageKey = "ranking_detail_media",
            title = stringResource(R.string.ranking_media_onboarding_title),
            contentPoints = listOf(
                stringResource(R.string.ranking_media_onboarding_point1),
                stringResource(R.string.ranking_media_onboarding_point2)
            ),
            preferenceManager = themeViewModel.preferenceManager
        )
    }

    // Launcher pour la couverture du classement
    val coverPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val compressedFile = com.example.phoenx.ui.util.ImageUtils.compressAndResize(context, uri)
            if (compressedFile != null) {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@rememberLauncherForActivityResult
                scope.launch {
                    try {
                        val path = mediaManager.uploadCameo(userId, "ranking_$rankingId", compressedFile)
                        viewModel.updateCoverImage(rankingId, path)
                    } catch (e: Exception) {
                        android.util.Log.e("RankingDetail", "Erreur upload cover: ${e.message}")
                    }
                }
            }
        }
    }

    // Launcher pour la photo/vidéo d'un rang individuel avec vérification de la limite de 5 médias
    val rankMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val rankIndex = selectedRankIndexForMedia ?: return@rememberLauncherForActivityResult
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

    fun triggerAddMediaForRank(rankIndex: Int) {
        val currentMediaList = rankMediaMap[rankIndex] ?: emptyList()
        if (currentMediaList.size >= 5) {
            Toast.makeText(
                context,
                context.getString(R.string.ranking_media_limit_reached),
                Toast.LENGTH_LONG
            ).show()
        } else {
            selectedRankIndexForMedia = rankIndex
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
    val sortedMediaList = remember(rankMediaMap, currentRanking.items) {
        currentRanking.items.indices.flatMap { idx ->
            rankMediaMap[idx] ?: emptyList()
        }.sortedWith(compareBy({ it.rankIndex }, { it.createdAt }))
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    if (!isReadOnly) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, null, tint = theme.contentColor)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                containerColor = theme.backgroundColor
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.ranking_detail_menu_rename), color = theme.contentColor) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = accent) },
                                    onClick = {
                                        expanded = false
                                        showEditTitleDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.ranking_detail_menu_delete), color = com.example.phoenx.ui.theme.Error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = com.example.phoenx.ui.theme.Error) },
                                    onClick = {
                                        expanded = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.backgroundColor,
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f)),
                shadowElevation = 8.dp
            ) {
                val filledCount = currentRanking.items.count { it.isNotBlank() }
                Text(
                    text = stringResource(R.string.ranking_detail_bottom_count, filledCount, currentRanking.itemCount),
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // 1. BANDEAU SUPÉRIEUR
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.contentColor.copy(alpha = 0.05f))
                        .clickable(enabled = !isReadOnly) { coverPhotoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentRanking.coverImageUrl != null) {
                        SecureAsyncImage(
                            mediaUrl = currentRanking.coverImageUrl,
                            mediaManager = mediaManager,
                            isEncrypted = false,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            creatorId = targetCreatorId,
                            docType = "rankings",
                            docId = currentRanking.id,
                            field = "coverImageUrl"
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = accent.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(if (isReadOnly) stringResource(R.string.ranking_detail_no_image) else stringResource(R.string.ranking_detail_add_image), color = accent.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    // Titre en overlay
                    Text(
                        text = currentRanking.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp).align(Alignment.BottomCenter)
                    )
                }
            }

            // 2. LISTE DES RANGS
            itemsIndexed(currentRanking.items) { index, itemText ->
                val rankMediaList = rankMediaMap[index] ?: emptyList()
                val firstMedia = rankMediaList.firstOrNull()

                RankItemRow(
                    index = index + 1,
                    text = itemText,
                    mediaList = rankMediaList,
                    rankingId = rankingId,
                    targetCreatorId = targetCreatorId,
                    isReadOnly = isReadOnly,
                    mediaManager = mediaManager,
                    accent = accent,
                    theme = theme,
                    onRowClick = {
                        navController.navigate(
                            com.example.phoenx.ui.navigation.Screen.RankItemDetail.createRoute(
                                rankingId = rankingId,
                                rankIndex = index,
                                creatorId = targetCreatorId
                            )
                        )
                    },
                    onEditClick = { if (!isReadOnly) editingItemIndex = index },
                    onMediaClick = {
                        firstMedia?.let { media ->
                            navController.navigate(
                                com.example.phoenx.ui.navigation.Screen.MediaViewer.createRoute(
                                    entryId = media.id,
                                    creatorId = targetCreatorId ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                    mediaUrl = media.mediaPath,
                                    entryType = media.mediaType,
                                    aiSummary = itemText.ifBlank { "Rang #${index + 1}" },
                                    sourceDocType = "rankMedia",
                                    personId = rankingId,
                                    isEncrypted = false
                                )
                            )
                        }
                    }
                )
            }

            // 3. GALERIE D'ILLUSTRATION DES RANGS (Grille à 4 colonnes, v12.8)
            if (sortedMediaList.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
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
                        Spacer(modifier = Modifier.height(12.dp))

                        val chunkedRows = sortedMediaList.chunked(4)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chunkedRows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { mediaItem ->
                                        val activeUrl = mediaItem.thumbnailPath ?: mediaItem.mediaPath
                                        val fieldParam = if (mediaItem.thumbnailPath != null) "thumbnailPath" else "mediaPath"
                                        val itemRankTitle = currentRanking.items.getOrNull(mediaItem.rankIndex) ?: "Rang #${mediaItem.rankIndex + 1}"

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(theme.contentColor.copy(alpha = 0.05f))
                                                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    navController.navigate(
                                                        com.example.phoenx.ui.navigation.Screen.MediaViewer.createRoute(
                                                            entryId = mediaItem.id,
                                                            creatorId = targetCreatorId ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                                            mediaUrl = mediaItem.mediaPath,
                                                            entryType = mediaItem.mediaType,
                                                            aiSummary = itemRankTitle.ifBlank { "Rang #${mediaItem.rankIndex + 1}" },
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

                                            // Badge numéro de rang
                                            Surface(
                                                modifier = Modifier.padding(4.dp).align(Alignment.TopStart),
                                                shape = CircleShape,
                                                color = accent,
                                                shadowElevation = 2.dp
                                            ) {
                                                Text(
                                                    text = "#${mediaItem.rankIndex + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = theme.backgroundColor,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }

                                            // Icône vidéo
                                            if (mediaItem.mediaType == "VIDEO") {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp).align(Alignment.Center)
                                                )
                                            }
                                        }
                                    }
                                    // Compléter la ligne si moins de 4 éléments
                                    repeat(4 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    // DIALOGUES D'ÉDITION (CRÉATEUR)
    if (showEditTitleDialog) {
        var newTitle by remember { mutableStateOf(currentRanking.title) }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text(stringResource(R.string.ranking_detail_dialog_rename_title), color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.replace("|", "") },
                    label = { Text(stringResource(R.string.ranking_detail_dialog_rename_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateRankingTitle(rankingId, newTitle)
                        showEditTitleDialog = false
                    },
                    enabled = newTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text(stringResource(R.string.ranking_detail_dialog_rename_button_save), color = theme.backgroundColor) }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) { Text(stringResource(R.string.ranking_detail_dialog_rename_button_cancel), color = theme.contentColor.copy(alpha = 0.6f)) }
            },
            containerColor = theme.backgroundColor
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.ranking_detail_dialog_delete_title), color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.ranking_detail_dialog_delete_text), color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRanking(rankingId)
                        showDeleteConfirmDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.phoenx.ui.theme.Error)
                ) { Text(stringResource(R.string.ranking_detail_dialog_delete_button_confirm), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text(stringResource(R.string.ranking_detail_dialog_delete_button_cancel), color = theme.contentColor) }
            },
            containerColor = theme.backgroundColor
        )
    }

    if (editingItemIndex != null) {
        val index = editingItemIndex!!
        var text by remember { mutableStateOf(currentRanking.items[index]) }
        val mediaListForRank = rankMediaMap[index] ?: emptyList()

        AlertDialog(
            onDismissRequest = { editingItemIndex = null },
            title = { Text(stringResource(R.string.ranking_detail_dialog_item_title, index + 1), color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.replace("|", "") },
                        placeholder = { Text(stringResource(R.string.ranking_detail_dialog_item_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Médias de ce rang :",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor.copy(alpha = 0.7f)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accent.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = stringResource(R.string.ranking_media_count_indicator, mediaListForRank.size),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bouton Ajouter Photo/Vidéo (avec garde-fou 5)
                    OutlinedButton(
                        onClick = {
                            triggerAddMediaForRank(index)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ranking_dialog_add_media), color = accent, fontWeight = FontWeight.Bold)
                    }

                    // Liste des médias existants avec possibilité de suppression individuelle
                    if (mediaListForRank.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mediaListForRank.forEach { mediaItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(theme.contentColor.copy(alpha = 0.03f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (mediaItem.mediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.Photo,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = if (mediaItem.mediaType == "VIDEO") "Vidéo" else "Photo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = theme.contentColor
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.removeRankMedia(rankingId, mediaItem.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = com.example.phoenx.ui.theme.Error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    if (currentRanking.items[index].isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.clearRankItem(rankingId, index)
                                editingItemIndex = null
                            }
                        ) { Text(stringResource(R.string.ranking_detail_dialog_item_button_clear), color = com.example.phoenx.ui.theme.Error) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            viewModel.updateRankItem(rankingId, index, text)
                            editingItemIndex = null
                        },
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) { Text(stringResource(R.string.ranking_detail_dialog_item_button_save), color = theme.backgroundColor) }
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItemIndex = null }) { Text(stringResource(R.string.ranking_detail_dialog_item_button_cancel), color = theme.contentColor.copy(alpha = 0.6f)) }
            },
            containerColor = theme.backgroundColor
        )
    }
}

@Composable
fun RankItemRow(
    index: Int,
    text: String,
    mediaList: List<RankMediaItem>,
    rankingId: String,
    targetCreatorId: String?,
    isReadOnly: Boolean,
    mediaManager: MediaManager,
    accent: Color,
    theme: com.example.phoenx.ui.theme.AppThemeState,
    onRowClick: () -> Unit,
    onEditClick: () -> Unit,
    onMediaClick: () -> Unit
) {
    val firstMedia = mediaList.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zone principale du rang (Numéro + Texte) - Cliquable pour afficher les détails du rang
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onRowClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (text.isBlank()) accent.copy(alpha = 0.1f) else accent,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (text.isBlank()) accent else theme.backgroundColor
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.contentColor,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = stringResource(R.string.ranking_detail_row_empty),
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = theme.contentColor.copy(alpha = 0.3f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Miniature si média attaché au rang (Cliquable pour ouvrir le lecteur grand écran)
        if (firstMedia != null) {
            val activeUrl = firstMedia.thumbnailPath ?: firstMedia.mediaPath
            val fieldParam = if (firstMedia.thumbnailPath != null) "thumbnailPath" else "mediaPath"

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .clickable(onClick = onMediaClick)
            ) {
                SecureAsyncImage(
                    mediaUrl = activeUrl,
                    mediaManager = mediaManager,
                    isEncrypted = false,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    creatorId = targetCreatorId,
                    docType = "rankMedia",
                    docId = firstMedia.id,
                    field = fieldParam,
                    personId = rankingId
                )
                if (firstMedia.mediaType == "VIDEO") {
                    Icon(
                        Icons.Default.PlayCircle,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp).align(Alignment.Center)
                    )
                }
                // Si plus de 1 média sur ce rang, afficher un petit badge indicateur "+N"
                if (mediaList.size > 1) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        shape = RoundedCornerShape(topStart = 4.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "+${mediaList.size - 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        
        // Icône crayon - Uniquement pour le Créateur pour ouvrir l'édition
        if (!isReadOnly) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = theme.contentColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = theme.contentColor.copy(alpha = 0.05f),
        thickness = 0.5.dp
    )
}
