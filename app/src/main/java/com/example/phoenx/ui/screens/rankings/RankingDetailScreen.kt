package com.example.phoenx.ui.screens.rankings

import android.net.Uri
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingDetailScreen(
    rankingId: String,
    navController: NavController,
    targetCreatorId: String? = null,
    viewModel: RankingViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ranking by viewModel.currentRanking.collectAsState()
    
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Upload et mise à jour coverImageUrl
            // Pattern habituel: compresser puis upload
            val compressedFile = com.example.phoenx.ui.util.ImageUtils.compressAndResize(context, uri)
            if (compressedFile != null) {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@rememberLauncherForActivityResult
                scope.launch {
                    try {
                        val path = mediaManager.uploadCameo(userId, "ranking_$rankingId", compressedFile)
                        viewModel.updateCoverImage(rankingId, path)
                    } catch (e: Exception) {
                        android.util.Log.e("RankingDetail", "Erreur upload: ${e.message}")
                    }
                }
            }
        }
    }

    LaunchedEffect(rankingId) {
        viewModel.setRankingId(rankingId)
    }

    if (ranking == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent)
        }
        return
    }

    val currentRanking = ranking!!

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
                                    text = { Text("Renommer", color = theme.contentColor) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = accent) },
                                    onClick = {
                                        expanded = false
                                        showEditTitleDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Supprimer ce classement", color = com.example.phoenx.ui.theme.Error) },
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
                    text = "$filledCount rangs remplis sur ${currentRanking.itemCount}",
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
                        .clickable(enabled = !isReadOnly) { photoPickerLauncher.launch("image/*") },
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
                            Text(if (isReadOnly) "Pas d'image" else "Ajouter une image", color = accent.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
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
                RankItemRow(
                    index = index + 1,
                    text = itemText,
                    accent = accent,
                    theme = theme,
                    onClick = { if (!isReadOnly) editingItemIndex = index }
                )
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    // DIALOGUES
    if (showEditTitleDialog) {
        var newTitle by remember { mutableStateOf(currentRanking.title) }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Renommer le classement", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.replace("|", "") },
                    label = { Text("Titre") },
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
                ) { Text("Enregistrer", color = theme.backgroundColor) }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
            },
            containerColor = theme.backgroundColor
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer ce classement ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Cette action est irréversible. Supprimer définitivement ce classement ?", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRanking(rankingId)
                        showDeleteConfirmDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.phoenx.ui.theme.Error)
                ) { Text("Supprimer", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Annuler", color = theme.contentColor) }
            },
            containerColor = theme.backgroundColor
        )
    }

    if (editingItemIndex != null) {
        val index = editingItemIndex!!
        var text by remember { mutableStateOf(currentRanking.items[index]) }
        AlertDialog(
            onDismissRequest = { editingItemIndex = null },
            title = { Text("Rang #${index + 1}", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.replace("|", "") },
                    placeholder = { Text("Entrez votre choix") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
            },
            confirmButton = {
                Row {
                    if (currentRanking.items[index].isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.clearRankItem(rankingId, index)
                                editingItemIndex = null
                            }
                        ) { Text("Vider", color = com.example.phoenx.ui.theme.Error) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            viewModel.updateRankItem(rankingId, index, text)
                            editingItemIndex = null
                        },
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) { Text("Enregistrer", color = theme.backgroundColor) }
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItemIndex = null }) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
            },
            containerColor = theme.backgroundColor
        )
    }
}

@Composable
fun RankItemRow(
    index: Int,
    text: String,
    accent: Color,
    theme: com.example.phoenx.ui.theme.AppThemeState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
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
                text = "Rang libre — appuyez pour remplir",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = theme.contentColor.copy(alpha = 0.3f),
                modifier = Modifier.weight(1f)
            )
        }
        
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = theme.contentColor.copy(alpha = 0.1f),
            modifier = Modifier.size(16.dp)
        )
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = theme.contentColor.copy(alpha = 0.05f),
        thickness = 0.5.dp
    )
}
