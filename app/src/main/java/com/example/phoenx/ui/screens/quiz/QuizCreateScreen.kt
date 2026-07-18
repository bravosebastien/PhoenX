package com.example.phoenx.ui.screens.quiz

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.domain.util.EnigmaUtils
import com.example.phoenx.data.model.Quiz
import com.example.phoenx.data.model.QuizQuestion
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizCreateScreen(
    navController: NavController,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    val backgroundBrush = LocalBackgroundBrush.current

    var title by remember { mutableStateOf("") }
    var finalMessage by remember { mutableStateOf("") }
    var availableNow by remember { mutableStateOf(false) }
    var availableAfterDeath by remember { mutableStateOf(true) }
    var showNames by remember { mutableStateOf(false) }
    val selectedRecipientIds = remember { mutableStateListOf<String>() }
    val recipients by viewModel.recipients.collectAsState()

    var numQuestionsGoal by remember { mutableIntStateOf(5) }
    val questions = remember { mutableStateListOf<QuizQuestion>() }

    var showInspiration by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Créer mon quiz", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic))
                            Text("Tes proches devineront ta vie.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
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
                    .padding(bottom = 32.dp)
            ) {
                // ÉTAPE 1 — TITRE ET OPTIONS
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E23)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Titre du quiz") },
                            placeholder = { Text("Connais-tu vraiment [Prénom] ?", style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = accent.copy(alpha = 0.5f))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = finalMessage,
                            onValueChange = { finalMessage = it },
                            label = { Text("Message final (optionnel)") },
                            placeholder = { Text("Ce que tu veux leur dire après avoir joué...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = accent.copy(alpha = 0.5f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Accessible maintenant", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Switch(checked = availableNow, onCheckedChange = { availableNow = it }, colors = SwitchDefaults.colors(checkedThumbColor = accent))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Accessible après mon départ", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Switch(checked = availableAfterDeath, onCheckedChange = { availableAfterDeath = it }, colors = SwitchDefaults.colors(checkedThumbColor = accent))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Afficher les noms dans le classement", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Switch(checked = showNames, onCheckedChange = { showNames = it }, colors = SwitchDefaults.colors(checkedThumbColor = accent))
                        }
                        Text("Si désactivé, seuls les rangs seront visibles.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextTertiary)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("QUI PEUT JOUER ?", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        RecipientSelector(
                            recipients = recipients,
                            selectedIds = selectedRecipientIds,
                            visibility = "RESTRICTED", // Quiz est restreint par défaut
                            onVisibilityChange = {},
                            accent = accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ÉTAPE 2 — NOMBRE DE QUESTIONS
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E23)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("NOMBRE DE QUESTIONS", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("$numQuestionsGoal questions", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif), color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { if (numQuestionsGoal > 1) numQuestionsGoal-- },
                                    modifier = Modifier.size(36.dp).background(Color(0xFF2E2E35), RoundedCornerShape(10.dp))
                                ) {
                                    Text("-", color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { if (numQuestionsGoal < 20) numQuestionsGoal++ },
                                    modifier = Modifier.size(36.dp).background(accent, RoundedCornerShape(10.dp))
                                ) {
                                    Text("+", color = Color(0xFF1A1A1F), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Slider(
                            value = numQuestionsGoal.toFloat(),
                            onValueChange = { numQuestionsGoal = it.toInt() },
                            valueRange = 1f..20f,
                            steps = 18,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ÉTAPE 3 — QUESTIONS
                Text("MES QUESTIONS (${questions.size}/$numQuestionsGoal)", style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(start = 14.dp, bottom = 6.dp), fontWeight = FontWeight.Bold)

                questions.forEachIndexed { index, question ->
                    QuestionItemCard(
                        index = index,
                        question = question,
                        onDelete = { questions.removeAt(index) },
                        onUpdate = { updated -> questions[index] = updated },
                        viewModel = viewModel
                    )
                }

                if (questions.size < numQuestionsGoal) {
                    OutlinedButton(
                        onClick = { showInspiration = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter une question")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val canPublish = questions.size > 0 && 
                                 questions.all { q -> q.text.isNotBlank() && q.correctAnswer.isNotBlank() } &&
                                 (availableNow || availableAfterDeath)

                Button(
                    onClick = {
                        val quiz = Quiz(
                            title = if (title.isBlank()) "Connais-tu vraiment ?" else title,
                            availableNow = availableNow,
                            availableAfterDeath = availableAfterDeath,
                            showNames = showNames,
                            recipientIds = selectedRecipientIds.toList(),
                            finalMessage = finalMessage,
                            questions = questions.toList()
                        )
                        viewModel.saveQuiz(quiz)
                        Toast.makeText(context, "Quiz créé. Tes proches peuvent jouer.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    enabled = canPublish,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Publier ce quiz", color = Color(0xFF1A1A1F), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showInspiration) {
            InspirationBottomSheet(
                onDismiss = { showInspiration = false },
                onSelect = { inspiration ->
                    val correct = inspiration.exampleAnswers.firstOrNull() ?: ""
                    val others = if (inspiration.exampleAnswers.size > 1) inspiration.exampleAnswers.drop(1) else emptyList()
                    
                    questions.add(
                        QuizQuestion(
                            id = UUID.randomUUID().toString(),
                            text = inspiration.question,
                            correctAnswer = correct,
                            correctHash = EnigmaUtils.hashAnswer(correct) ?: "",
                            distractors = others
                        )
                    )
                    showInspiration = false
                }
            )
        }
    }
}

@Composable
fun QuestionItemCard(
    index: Int,
    question: QuizQuestion,
    onDelete: () -> Unit,
    onUpdate: (QuizQuestion) -> Unit,
    viewModel: QuizViewModel
) {
    val accent = LocalAccentColor.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val photoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onUpdate(question.copy(mediaUrl = uri.toString(), mediaType = "PHOTO"))
        }
    }

    val videoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onUpdate(question.copy(mediaUrl = uri.toString(), mediaType = "VIDEO"))
        }
    }

    val audioLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onUpdate(question.copy(mediaUrl = uri.toString(), mediaType = "AUDIO"))
        }
    }

    Card(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E23)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.align(Alignment.CenterStart).width(3.dp).fillMaxHeight().background(accent))
            
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Question ${index + 1}", style = MaterialTheme.typography.labelSmall, color = accent)
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Error, modifier = Modifier.size(18.dp))
                    }
                }

                // SUPPORT MÉDIA (PHOTO/VIDÉO/SON)
                if (question.mediaUrl != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp).clip(RoundedCornerShape(8.dp))) {
                        if (question.mediaType == "PHOTO") {
                            coil3.compose.AsyncImage(
                                model = question.mediaUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (question.mediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.Mic,
                                        null,
                                        tint = accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(question.mediaType ?: "MÉDIA", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        IconButton(
                            onClick = { onUpdate(question.copy(mediaUrl = null, mediaType = null)) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { photoLauncher.launch("image/*") }) {
                            Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Photo", fontSize = 11.sp)
                        }
                        TextButton(onClick = { videoLauncher.launch("video/*") }) {
                            Icon(Icons.Default.Videocam, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Vidéo (10s)", fontSize = 11.sp)
                        }
                        TextButton(onClick = { audioLauncher.launch("audio/*") }) {
                            Icon(Icons.Default.Mic, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Vocal", fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = question.text,
                    onValueChange = { onUpdate(question.copy(text = it)) },
                    label = { Text("La question") },
                    placeholder = { Text("Ex: Dans quelle ville suis-je né(e) ?", style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = accent.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = question.correctAnswer,
                    onValueChange = { 
                        onUpdate(question.copy(
                            correctAnswer = it,
                            correctHash = EnigmaUtils.hashAnswer(it) ?: ""
                        )) 
                    },
                    label = { Text("La bonne réponse (Sera chiffrée)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Success, unfocusedBorderColor = Success.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Les pièges (distracteurs)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    
                    // BOUTON MAGIE IA
                    TextButton(
                        onClick = {
                            if (question.text.isNotBlank() && question.correctAnswer.isNotBlank()) {
                                viewModel.generateDistractorsForQuestion(question.text, question.correctAnswer) { distractors ->
                                    if (distractors.isNotEmpty()) {
                                        onUpdate(question.copy(distractors = distractors))
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Saisis d'abord la question et la réponse.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = accent)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Magie IA", fontSize = 11.sp)
                    }
                }

                // On s'assure d'avoir au moins 1 champ de distracteur si on veut le mode QCM
                val currentDistractors = if (question.distractors.isEmpty()) listOf("") else question.distractors
                
                currentDistractors.forEachIndexed { dIndex, distractor ->
                    OutlinedTextField(
                        value = distractor,
                        onValueChange = { 
                            val newList = currentDistractors.toMutableList()
                            newList[dIndex] = it
                            onUpdate(question.copy(distractors = newList.filter { it.isNotBlank() }))
                        },
                        placeholder = { Text("Fausse réponse ${dIndex + 1}") },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = accent.copy(alpha = 0.2f))
                    )
                }
                
                if (currentDistractors.size < 3) {
                    TextButton(onClick = { 
                        val newList = currentDistractors + ""
                        onUpdate(question.copy(distractors = newList))
                    }) {
                        Text("+ Ajouter un piège", fontSize = 12.sp, color = accent)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Messages taquins (Si mode aide utilisé)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                
                question.teasingMessages.forEachIndexed { tIndex, msg ->
                    OutlinedTextField(
                        value = msg,
                        onValueChange = { 
                            val newList = question.teasingMessages.toMutableList()
                            newList[tIndex] = it
                            onUpdate(question.copy(teasingMessages = newList.filter { it.isNotBlank() }))
                        },
                        placeholder = { Text("Ex: On voit qui ne suivait pas à table !") },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = accent.copy(alpha = 0.2f))
                    )
                }
                
                if (question.teasingMessages.size < 3) {
                    TextButton(onClick = { 
                        val newList = question.teasingMessages + ""
                        onUpdate(question.copy(teasingMessages = newList))
                    }) {
                        Text("+ Ajouter une pique", fontSize = 12.sp, color = accent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspirationBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (QuizInspiration) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2E2E35)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Besoin d'inspiration ?", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif), color = TextPrimary)
            Text("Tap sur une question pour l'utiliser comme base.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QuizInspirationData.questions) { item ->
                    Card(
                        onClick = { onSelect(item) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242429)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.question, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = TextPrimary)
                            Text(item.exampleAnswers.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}
