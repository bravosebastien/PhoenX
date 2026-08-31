package com.example.phoenx.ui.screens.personalities

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.local.PersonalityEntity
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalityEditScreen(
    personalityId: String, // "new" pour ajout
    navController: NavController,
    viewModel: PersonalitiesViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val personalities by viewModel.personalities.collectAsState()
    val existing = if (personalityId != "new") personalities.find { it.id == personalityId } else null
    
    // v9.7.1 : Correction Bug 2 - Collection de la liste média plus robuste
    val mediaList by remember(personalityId) { 
        if (personalityId != "new") viewModel.getMediaForPersonality(personalityId) 
        else kotlinx.coroutines.flow.flowOf(emptyList()) 
    }.collectAsState(initial = emptyList())

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Sport") }
    var customCategoryLabel by remember { mutableStateOf(existing?.customCategoryLabel ?: "") }
    var mainPhotoPath by remember { mutableStateOf(existing?.mainPhotoPath ?: "") }
    var biography by remember { mutableStateOf(existing?.biography ?: "") }
    var personalComment by remember { mutableStateOf(existing?.personalComment ?: "") }

    // v9.7.1 : Correction Bug 1 - Pré-remplissage des champs quand les données arrivent
    LaunchedEffect(existing) {
        existing?.let {
            if (name.isEmpty()) name = it.name
            if (category == "Sport" && it.category != "Sport") category = it.category
            if (customCategoryLabel.isEmpty()) customCategoryLabel = it.customCategoryLabel ?: ""
            if (mainPhotoPath.isEmpty()) mainPhotoPath = it.mainPhotoPath
            if (biography.isEmpty()) biography = it.biography
            if (personalComment.isEmpty()) personalComment = it.personalComment
        }
    }

    var isCheckingContent by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf<String?>(null) }
    
    val categories = listOf("Sport", "Cinéma", "Peinture", "Sculpture", "Sciences", "Symboles de la paix", "Symboles du chaos", "Symboles de l'amour", "Symboles de la haine", "Autre")

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val fileName = "personality_main_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            mainPhotoPath = file.absolutePath
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty() && existing != null) {
            uris.forEach { uri ->
                val fileName = "personality_gal_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                viewModel.addMedia(existing.id, file)
            }
        }
    }

    val mediaManager = remember {
        EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager()
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nouvelle Personnalité" else "Modifier la fiche", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { 
                            viewModel.deletePersonality(existing)
                            navController.popBackStack() 
                        }) {
                            Icon(Icons.Default.Delete, null, tint = com.example.phoenx.ui.theme.Error)
                        }
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // PHOTO PRINCIPALE
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(theme.contentColor.copy(alpha = 0.05f))
                    .border(2.dp, accent.copy(alpha = 0.3f), CircleShape)
                    .clickable { photoPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (mainPhotoPath.isNotEmpty()) {
                    SecureAsyncImage(
                        mediaUrl = mainPhotoPath,
                        mediaManager = mediaManager,
                        docType = "personalities",
                        docId = existing?.id ?: "temp",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddAPhoto, null, tint = accent, modifier = Modifier.size(32.dp))
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = theme.backgroundColor, modifier = Modifier.size(16.dp))
                }
            }

            // NOM
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
            )

            // CATÉGORIE
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { },
                    label = { Text("Catégorie") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f),
                    containerColor = theme.backgroundColor // v9.7.1 : Correction Bug 3
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, color = theme.contentColor) }, // v9.7.1 : Correction Bug 3
                            onClick = {
                                category = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (category == "Autre") {
                OutlinedTextField(
                    value = customCategoryLabel,
                    onValueChange = { customCategoryLabel = it },
                    label = { Text("Précisez la catégorie") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
            }

            // POINT D'INFO PHOTOS
            Surface(
                color = theme.contentColor.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Cette partie n'accueille que des photos — il n'est pas possible d'y ajouter une vidéo ou un son. Pour illustrer une personnalité en vidéo, retrouvez-la dans la Vidéothèque.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.7f)
                    )
                }
            }

            // BIOGRAPHIE
            OutlinedTextField(
                value = biography,
                onValueChange = { biography = it },
                label = { Text("Biographie") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                placeholder = { Text("Copiez-collez ici un texte descriptif (ex: Wikipédia)...") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
            )

            // COMMENTAIRE PERSONNEL
            OutlinedTextField(
                value = personalComment,
                onValueChange = { personalComment = it },
                label = { Text("En quoi cette personne a compté pour vous ?") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text("Partagez votre lien personnel ou son influence sur votre vie...") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
            )

            // GALERIE PHOTOS (Uniquement si déjà créé)
            if (existing != null) {
                SectionHeader(title = "GALERIE PHOTOS")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mediaList.forEach { media ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.contentColor.copy(alpha = 0.05f))
                        ) {
                            SecureAsyncImage(
                                mediaUrl = media.mediaPath,
                                mediaManager = mediaManager,
                                docType = "personalityMedia",
                                docId = media.id,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                isEncrypted = false
                            )
                            IconButton(
                                onClick = { viewModel.removeMedia(media) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    
                    if (mediaList.size < 5) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.1f))
                                .clickable { galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = accent)
                        }
                    }
                }
            }

            // BOUTON ENREGISTRER
            Button(
                onClick = {
                    scope.launch {
                        isCheckingContent = true
                        
                        // Check Bio
                        val bioCheck = viewModel.checkContent(biography)
                        if (!bioCheck.first) {
                            showWarningDialog = bioCheck.second ?: "Ce texte semble aller au-delà d'une simple mention historique — voulez-vous le reformuler ?"
                            isCheckingContent = false
                            return@launch
                        }

                        // Check Comment
                        val commentCheck = viewModel.checkContent(personalComment)
                        if (!commentCheck.first) {
                            showWarningDialog = commentCheck.second ?: "Votre commentaire personnel semble contenir des propos problématiques — voulez-vous le reformuler ?"
                            isCheckingContent = false
                            return@launch
                        }

                        val entity = PersonalityEntity(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            category = category,
                            customCategoryLabel = if (category == "Autre") customCategoryLabel else null,
                            mainPhotoPath = mainPhotoPath,
                            biography = biography,
                            personalComment = personalComment,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                        viewModel.savePersonality(entity)
                        isCheckingContent = false
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank() && mainPhotoPath.isNotEmpty() && !isCheckingContent,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                if (isCheckingContent) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = theme.backgroundColor, strokeWidth = 2.dp)
                } else {
                    Text("Enregistrer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(50.dp))
        }
    }

    if (showWarningDialog != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Vigilance Contenu", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text(showWarningDialog!!, color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { 
                    // On force l'enregistrement quand même si l'utilisateur insiste
                    val entity = PersonalityEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        category = category,
                        customCategoryLabel = if (category == "Autre") customCategoryLabel else null,
                        mainPhotoPath = mainPhotoPath,
                        biography = biography,
                        personalComment = personalComment,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    viewModel.savePersonality(entity)
                    showWarningDialog = null
                    navController.popBackStack()
                }) {
                    Text("Enregistrer quand même", color = accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = null }) {
                    Text("Modifier le texte", color = theme.contentColor)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val theme = LocalAppTheme.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = theme.contentColor.copy(alpha = 0.4f)
    )
}
