package com.example.phoenx.ui.screens.universal

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalMessageScreen(
    navController: NavController,
    viewModel: UniversalMessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    val uiState by viewModel.uiState.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Profil public
    var showLastName by remember { mutableStateOf(false) }
    var showProfession by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }
    var bioLine by remember { mutableStateOf("") }
    var charteAccepted by remember { mutableStateOf(false) }

    fun checkVideoDuration(uri: Uri): Boolean {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            duration <= 30_000L // 30 secondes max
        } catch (e: Exception) {
            false
        } finally {
            retriever.release()
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        photoUris = (photoUris + uris).take(3)
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val isValid = checkVideoDuration(uri)
            if (!isValid) {
                errorMessage = "Cette vidéo dépasse 30 secondes. Choisis un extrait plus court."
                videoUri = null
            } else {
                videoUri = uri
                errorMessage = null
            }
        }
    }

    LaunchedEffect(Unit) {
        if (userId.isNotEmpty()) {
            viewModel.checkExistingMessage(userId)
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                is UniversalMessageState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
                }
                is UniversalMessageState.AlreadyExists -> {
                    SealedMessageState(onViewMessage = { 
                        // Navigation vers la lecture ou affichage simple
                    }, theme = theme)
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ma Lettre à l'Humanité",
                                style = TextStyle(fontFamily = theme.fontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = theme.contentColor)
                            )
                            InfoButton(
                                title = "Ma Lettre à l'Humanité",
                                points = listOf(
                                    "Un seul message, pour tout le monde — rendu public après ton départ.",
                                    "Tu peux ajouter jusqu'à 3 photos et une vidéo de 30 secondes maximum.",
                                    "L'IA classe automatiquement ton message dans une catégorie (Amour, Espoir, Sagesse...).",
                                    "Notre équipe valide chaque message avant publication.",
                                    "Les lecteurs n'ont besoin que d'un compte gratuit pour lire les lettres."
                                )
                            )
                        }
                        Text(
                            "Un seul message. Pour tout le monde. Il sera lu après ton départ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // ZONE TEXTE
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.contentColor.copy(alpha = 0.05f))
                                .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(20.dp)
                        ) {
                            if (messageText.isEmpty()) {
                                Text("Écris ici ton message universel...", color = theme.contentColor.copy(alpha = 0.3f), style = TextStyle(fontFamily = theme.fontFamily, fontSize = 17.sp, fontStyle = FontStyle.Italic))
                            }
                            BasicTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                textStyle = TextStyle(fontFamily = theme.fontFamily, fontSize = 17.sp, color = theme.contentColor, lineHeight = 28.sp),
                                modifier = Modifier.fillMaxSize(),
                                cursorBrush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(accent, accent))
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // PHOTOS
                        Text("PHOTOS (MAX 3)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            photoUris.forEach { uri ->
                                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    IconButton(
                                        onClick = { photoUris = photoUris - uri },
                                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f))
                                    ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                }
                            }
                            if (photoUris.size < 3) {
                                Box(
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(theme.contentColor.copy(alpha = 0.05f)).clickable { photoLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Add, null, tint = accent) }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // VIDÉO
                        Text("VIDÉO (MAX 30 SEC)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (videoUri == null) {
                            OutlinedButton(
                                onClick = { videoLauncher.launch("video/*") },
                                border = BorderStroke(1.dp, accent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                            ) { Text("Ajouter une vidéo") }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Vidéo sélectionnée", color = Success, style = MaterialTheme.typography.bodySmall)
                                IconButton(onClick = { videoUri = null }) { Icon(Icons.Default.Close, null, tint = Error) }
                            }
                        }

                        if (errorMessage != null) {
                            Text(errorMessage!!, color = Error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // PROFIL PUBLIC
                        Text("INFORMATIONS VISIBLES PAR LES LECTEURS :", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggle("Mon nom de famille", showLastName, theme) { showLastName = it }
                        ProfileToggle("Ma profession", showProfession, theme) { showProfession = it }
                        ProfileToggle("Ma ville", showCity, theme) { showCity = it }
                        
                        OutlinedTextField(
                            value = bioLine,
                            onValueChange = { bioLine = it },
                            label = { Text("Une phrase de présentation") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                                focusedTextColor = theme.contentColor,
                                unfocusedTextColor = theme.contentColor
                            )
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // CHARTE
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = charteAccepted, onCheckedChange = { charteAccepted = it }, colors = CheckboxDefaults.colors(checkedColor = accent))
                            Text(
                                "J'accepte que ce message soit relu par l'équipe PHOEN-X avant publication, et publié après mon départ.",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.contentColor.copy(alpha = 0.6f),
                                modifier = Modifier.clickable { charteAccepted = !charteAccepted }
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    try {
                                        // 1. Upload Media
                                        val photoUrls = photoUris.mapIndexed { i, uri ->
                                            val ref = storage.reference.child("universal/$userId/photo_$i.jpg")
                                            ref.putFile(uri).await()
                                            ref.downloadUrl.await().toString()
                                        }
                                        var videoUrl: String? = null
                                        videoUri?.let {
                                            val ref = storage.reference.child("universal/$userId/video.mp4")
                                            ref.putFile(it).await()
                                            videoUrl = ref.downloadUrl.await().toString()
                                        }

                                        // 2. Sauvegarde Firestore
                                        val messageData = hashMapOf(
                                            "creatorId" to userId,
                                            "messageText" to messageText, 
                                            "photoUrls" to photoUrls,
                                            "videoUrl" to videoUrl,
                                            "bioLine" to bioLine,
                                            "showLastName" to showLastName,
                                            "showProfession" to showProfession,
                                            "showCity" to showCity,
                                            "isPublished" to false,
                                            "isModerated" to false,
                                            "createdAt" to com.google.firebase.Timestamp.now()
                                        )
                                        db.collection("universalMessages").document(userId).set(messageData).await()
                                        
                                        Toast.makeText(context, "Ta lettre est scellée. Elle sera publiée après ton départ.", Toast.LENGTH_LONG).show()
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = messageText.isNotBlank() && charteAccepted && !isSaving,
                            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            if (isSaving) CircularProgressIndicator(color = theme.backgroundColor, modifier = Modifier.size(24.dp))
                            else Text("Sceller cette lettre", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileToggle(label: String, checked: Boolean, theme: AppThemeState, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = theme.accentColor))
        Text(label, style = MaterialTheme.typography.bodySmall, color = theme.contentColor)
    }
}

@Composable
fun SealedMessageState(onViewMessage: () -> Unit, theme: AppThemeState) {
    val accent = theme.accentColor
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Ta lettre est scellée.",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tu as déjà déposé ta lettre à l'humanité. Elle sera publiée après ton départ, une fois validée par notre équipe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                TextButton(onClick = onViewMessage) {
                    Text("Voir ma lettre", color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
