package com.example.phoenx.ui.screens.questions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedQuestion by remember { mutableStateOf<Question?>(null) }
    var answerText by remember { mutableStateOf("") }
    
    val answeredCount = uiState.answeredQuestionIds.size
    val totalCount = QuestionsData.allQuestions.size

    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val file = File(context.cacheDir, "temp_q_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            capturedPhotoFile = file
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            selectedQuestion = null
            answerText = ""
            capturedPhotoFile = null
            viewModel.resetSuccess()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            Column(modifier = Modifier.background(BackgroundPrimary)) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Les 100 Questions", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif))
                                Text(
                                    text = "$answeredCount / $totalCount questions racontées",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (answeredCount == totalCount) Success else TextSecondary
                                )
                            }
                            InfoButton(
                                title = "Les Questions",
                                points = listOf(
                                    "Plus de 100 questions pour raconter ta vie en profondeur.",
                                    "Chaque réponse est un souvenir scellé, prêt à être transmis.",
                                    "Un badge ✓ apparaît sur les questions déjà traitées.",
                                    "Réponds à ton rythme, catégorie par catégorie."
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (selectedQuestion != null) selectedQuestion = null else onNavigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AccentPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
                )

                if (selectedQuestion == null) {
                    ScrollableTabRow(
                        selectedTabIndex = QuestionsData.categories.indexOf(uiState.selectedCategory),
                        containerColor = BackgroundPrimary,
                        contentColor = AccentPrimary,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[QuestionsData.categories.indexOf(uiState.selectedCategory)]),
                                color = AccentPrimary
                            )
                        },
                        divider = {}
                    ) {
                        QuestionsData.categories.forEach { category ->
                            Tab(
                                selected = uiState.selectedCategory == category,
                                onClick = { viewModel.filterQuestions(category) },
                                text = {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (uiState.selectedCategory == category) TextPrimary else Color(0xFF5C5855)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedQuestion == null) {
                // LISTE DES QUESTIONS
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentPrimary)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.questions) { question ->
                            QuestionListItem(
                                question = question,
                                isAnswered = uiState.answeredQuestionIds.contains(question.id),
                                onClick = { selectedQuestion = question }
                            )
                        }
                    }
                }
            } else {
                // ÉCRAN DE RÉPONSE DÉDIÉ (v8.5.9)
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
                ) {
                    Text(
                        text = selectedQuestion!!.text,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                        color = TextPrimary,
                        lineHeight = 32.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    if (capturedPhotoFile != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = capturedPhotoFile,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { capturedPhotoFile = null },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, tint = AccentPrimary)
                            Spacer(Modifier.width(12.dp))
                            Text("Illustrer ma réponse", color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        placeholder = { Text("Dépose tes mots ici...", color = TextTertiary, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = TextTertiary.copy(alpha = 0.2f),
                            focusedContainerColor = SurfaceCard.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { viewModel.saveAnswer(selectedQuestion!!, answerText, capturedPhotoFile) },
                        enabled = (answerText.isNotBlank() || capturedPhotoFile != null) && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BackgroundPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Sceller ma réponse", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionListItem(
    question: Question,
    isAnswered: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = SurfaceCard.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAnswered) Success.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isAnswered) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.HelpOutline, 
                contentDescription = null, 
                tint = if (isAnswered) Success else AccentPrimary, 
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = question.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextTertiary)
        }
    }
}
