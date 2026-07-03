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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalMessageScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()

    var messageText by remember { mutableStateOf("") }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Profil public
    var showLastName by remember { mutableStateOf(false) }
    var showProfession by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }
    var bioLine by remember { mutableStateOf("") }
    var charteAccepted by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        photoUris = (photoUris + uris).take(3)
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Ici on devrait normalement vérifier la durée via un MediaMetadataRetriever
            // Pour le MVP on suppose que l'utilisateur respecte ou on gère l'erreur d'upload
            videoUri = uri
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AccentPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                "Ma Lettre à l'Humanité",
                style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Text(
                "Un seul message. Pour tout le monde. Il sera lu après ton départ.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ZONE TEXTE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF242429))
                    .padding(20.dp)
            ) {
                if (messageText.isEmpty()) {
                    Text("Écris ici ton message universel...", color = Color(0xFF5C5855), style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, fontStyle = FontStyle.Italic))
                }
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, color = TextPrimary, lineHeight = 28.sp),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PHOTOS
            Text("PHOTOS (MAX 3)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, null, tint = AccentPrimary) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // VIDÉO
            Text("VIDÉO (MAX 30 SEC)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            if (videoUri == null) {
                OutlinedButton(
                    onClick = { videoLauncher.launch("video/*") },
                    border = BorderStroke(1.dp, AccentPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary)
                ) { Text("Ajouter une vidéo") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vidéo sélectionnée", color = Success, style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { videoUri = null }) { Icon(Icons.Default.Close, null, tint = Error) }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PROFIL PUBLIC
            Text("INFORMATIONS VISIBLES PAR LES LECTEURS :", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileToggle("Mon nom de famille", showLastName) { showLastName = it }
            ProfileToggle("Ma profession", showProfession) { showProfession = it }
            ProfileToggle("Ma ville", showCity) { showCity = it }
            
            OutlinedTextField(
                value = bioLine,
                onValueChange = { bioLine = it },
                label = { Text("Une phrase de présentation") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            // CHARTE
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = charteAccepted, onCheckedChange = { charteAccepted = it }, colors = CheckboxDefaults.colors(checkedColor = AccentPrimary))
                Text(
                    "J'accepte que ce message soit relu par l'équipe PHOEN-X avant publication, et publié après mon départ.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.clickable { charteAccepted = !charteAccepted }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    val userId = auth.currentUser?.uid ?: return@Button
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
                                "messageText" to messageText, // TODO: Tink Encrypt
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                if (isSaving) CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                else Text("Sceller cette lettre", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = AccentPrimary))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}
