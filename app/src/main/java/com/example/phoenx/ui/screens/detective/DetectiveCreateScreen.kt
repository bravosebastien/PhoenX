package com.example.phoenx.ui.screens.detective

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectiveCreateScreen(
    navController: NavController,
    viewModel: DetectiveCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.updatePhotoUri(uri)
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
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
                text = "Un secret protégé par une question",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = TextPrimary
            )
            Text(
                text = "Seul celui qui connaît la réponse pourra accéder à ce que tu déposes ici.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ÉTAPE 1 — L'ÉNIGME
            Text("LA QUESTION", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.enigmaText,
                onValueChange = { viewModel.updateEnigma(it) },
                placeholder = { Text("Ex : Quel était le nom de notre premier chien ?", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                    unfocusedContainerColor = SurfaceCard,
                    focusedContainerColor = SurfaceCard
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ÉTAPE 2 — LA RÉPONSE
            Text("LA RÉPONSE (invisible après saisie)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.secretAnswer,
                onValueChange = { viewModel.updateAnswer(it) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 17.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                    unfocusedContainerColor = SurfaceCard,
                    focusedContainerColor = SurfaceCard
                )
            )
            Text(
                "La réponse est protégée localement par une empreinte numérique SHA-256. Personne ne peut la lire, même nos serveurs.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ÉTAPE 3 — LE CONTENU
            Text("LE CONTENU SECRET", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                "Ce contenu ne sera accessible qu'après avoir répondu correctement à ta question.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.TEXT,
                    label = "✍️ Texte",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.TEXT) }
                )
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.PHOTO,
                    label = "📷 Photo",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.PHOTO) }
                )
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.AUDIO,
                    label = "🎙️ Audio",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.AUDIO) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.contentType) {
                ContentType.TEXT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF242429))
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value = uiState.textContent,
                            onValueChange = { viewModel.updateTextContent(it) },
                            textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, color = TextPrimary),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                ContentType.PHOTO -> {
                    if (uiState.photoUri == null) {
                        Button(
                            onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                        ) {
                            Text("Choisir une photo", color = AccentPrimary)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = uiState.photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.updatePhotoUri(null) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                    }
                }
                ContentType.AUDIO -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        // Simulation bouton enregistrement
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = AccentPrimary,
                            onClick = { /* TODO */ }
                        ) {
                            Icon(Icons.Default.Mic, null, tint = BackgroundPrimary, modifier = Modifier.padding(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            val canSave = uiState.enigmaText.isNotBlank() && 
                          uiState.secretAnswer.isNotBlank() && 
                          ((uiState.contentType == ContentType.TEXT && uiState.textContent.isNotBlank()) ||
                           (uiState.contentType == ContentType.PHOTO && uiState.photoUri != null))

            Button(
                onClick = { 
                    viewModel.saveDetectiveEntry {
                        Toast.makeText(context, "Énigme scellée. Seul celui qui sait pourra l'ouvrir.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                enabled = canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Sceller l'énigme", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ContentTypeButton(selected: Boolean, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text(label, color = BackgroundPrimary, fontSize = 12.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceCard)
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
