package com.example.phoenx.ui.screens.questions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.phoenXMatiere
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.phoenx.ui.components.SecureAsyncImage
import coil3.compose.AsyncImage
import java.io.File
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionAnswerScreen(
    questionId: String,
    onNavigateBack: () -> Unit,
    viewModel: QuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val question = remember(questionId) {
        QuestionsData.allQuestions.find { it.id == questionId }
    }
    
    var answerText by remember { mutableStateOf("") }
    var photoFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    LaunchedEffect(questionId) {
        viewModel.loadExistingAnswer(questionId)
    }

    LaunchedEffect(uiState.currentAnswerText) {
        if (uiState.currentAnswerText != null) {
            answerText = uiState.currentAnswerText!!
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val file = File(context.cacheDir, "temp_qfix_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            photoFile = file
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
            viewModel.resetSuccess()
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Ma Réponse", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        if (question == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Question introuvable", color = theme.contentColor)
            }
        } else if (uiState.currentAnswerText == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = theme.fontFamily, 
                        fontWeight = FontWeight.Bold
                    ),
                    color = theme.contentColor,
                    lineHeight = 32.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // ZONE PHOTO (v12.3)
                if (photoFile != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                        AsyncImage(model = photoFile, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(
                            onClick = { photoFile = null }, 
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else if (uiState.currentMediaUrl != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                        SecureAsyncImage(
                            mediaUrl = uiState.currentMediaUrl,
                            localPath = uiState.currentMediaPath,
                            mediaManager = viewModel.mediaManager,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            docType = "entries",
                            docId = questionId, // Utilisation de l'ID de la question comme clé stable
                            isEncrypted = uiState.currentMediaUrl?.contains(".enc") ?: true
                        )
                        Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                            IconButton(
                                onClick = { viewModel.removeExistingPhoto(questionId) },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { photoLauncher.launch("image/*") },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { photoLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, tint = accent)
                        Spacer(Modifier.width(12.dp))
                        Text("Illustrer ma réponse", color = theme.contentColor)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = answerText,
                    onValueChange = { answerText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    placeholder = { 
                        Text(
                            "Dépose tes mots ici...", 
                            color = theme.contentColor.copy(alpha = 0.3f), 
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                        ) 
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = theme.contentColor),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                        focusedContainerColor = theme.contentColor.copy(alpha = 0.03f),
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = { viewModel.saveAnswer(question, answerText, photoFile) },
                    enabled = (answerText.isNotBlank() || photoFile != null || uiState.currentMediaUrl != null) && !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = theme.backgroundColor, strokeWidth = 2.dp)
                    } else {
                        Text("Enregistrer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
