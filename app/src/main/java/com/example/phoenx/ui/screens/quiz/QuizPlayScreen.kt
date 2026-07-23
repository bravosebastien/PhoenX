package com.example.phoenx.ui.screens.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.example.phoenx.domain.util.EnigmaUtils
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun QuizPlayScreen(
    creatorId: String,
    quizId: String,
    navController: NavController,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val quiz by viewModel.currentQuiz.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val score by viewModel.score.collectAsState()
    val userResult by viewModel.userResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val heirKey by viewModel.heirKey.collectAsState()
    
    val scope = rememberCoroutineScope()
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var gameStarted by remember { mutableStateOf(false) }

    LaunchedEffect(creatorId, quizId) {
        viewModel.loadQuiz(creatorId, quizId)
    }

    // Rediriger vers le classement si déjà joué
    LaunchedEffect(userResult) {
        if (userResult != null && !gameStarted) {
            navController.navigate("quiz_leaderboard/$creatorId/$quizId") {
                popUpTo("quiz_play/$creatorId/$quizId") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
        } else if (quiz != null) {
            val totalQuestions = quiz!!.questions.size

            if (!gameStarted) {
                // ÉCRAN D'ACCUEIL DU QUIZ
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("PHOEN-X", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = accent, letterSpacing = 4.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(quiz!!.title, style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold), color = theme.contentColor, textAlign = TextAlign.Center)
                    
                    Card(
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem(totalQuestions.toString(), "QUESTIONS", accent, theme)
                            StatItem((totalQuestions * 3).toString(), "POINTS MAX", accent, theme)
                            StatItem("?", "CLASSÉS", accent, theme)
                        }
                    }

                    Button(
                        onClick = { gameStarted = true },
                        modifier = Modifier.padding(top = 32.dp).fillMaxWidth().height(56.dp).phoenXMatiere(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Commencer le quiz", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (currentIndex < totalQuestions) {
                // ÉCRAN DE JEU
                val currentQuestion = quiz!!.questions[currentIndex]
                val progress by animateFloatAsState(targetValue = (currentIndex.toFloat() / totalQuestions.toFloat()))
                
                var isHelpMode by remember(currentIndex) { mutableStateOf(false) }
                var hardAnswer by remember(currentIndex) { mutableStateOf("") }
                val choices = remember(currentIndex, isHelpMode) {
                    if (isHelpMode) viewModel.getDisplayChoices(currentQuestion, currentQuestion.correctAnswer)
                    else emptyList()
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.1f)
                    )

                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Question ${currentIndex + 1} sur $totalQuestions", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                        
                        Card(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.align(Alignment.CenterStart).width(3.dp).fillMaxHeight().background(accent))
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // SUPPORT MÉDIA (v8.3)
                                    if (currentQuestion.mediaUrl != null) {
                                        Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 16.dp).clip(RoundedCornerShape(8.dp))) {
                                            if (currentQuestion.mediaType == "PHOTO") {
                                                SecureAsyncImage(
                                                    mediaUrl = currentQuestion.mediaUrl,
                                                    explicitKey = heirKey,
                                                    mediaManager = viewModel.mediaManager,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else {
                                                val context = LocalContext.current
                                                val exoPlayer = remember(currentQuestion.mediaUrl) {
                                                    ExoPlayer.Builder(context).build().apply {
                                                        val factory = viewModel.mediaManager.getEncryptedDataSourceFactory()
                                                        val mediaSource = ProgressiveMediaSource.Factory(factory)
                                                            .createMediaSource(MediaItem.fromUri(currentQuestion.mediaUrl.toUri()))
                                                        
                                                        setMediaSource(mediaSource)
                                                        repeatMode = if (currentQuestion.mediaType == "VIDEO") Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                                                        prepare()
                                                    }
                                                }
                                                DisposableEffect(exoPlayer) {
                                                    onDispose { exoPlayer.release() }
                                                }
                                                
                                                if (currentQuestion.mediaType == "VIDEO") {
                                                    AndroidView(
                                                        factory = { PlayerView(it).apply { player = exoPlayer; useController = false } },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    LaunchedEffect(exoPlayer) { exoPlayer.play() }
                                                } else {
                                                    // AUDIO
                                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                                        IconButton(onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                                                            Icon(if (exoPlayer.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, null, tint = accent, modifier = Modifier.size(64.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        currentQuestion.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                                        color = theme.contentColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!isHelpMode) {
                            // MODE HARD
                            OutlinedTextField(
                                value = hardAnswer,
                                onValueChange = { hardAnswer = it },
                                label = { Text("Ta réponse") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.answerQuestion(hardAnswer, usedHelp = false) },
                                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("Vérifier", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                            }
                            if (currentQuestion.difficultyAllowed) {
                                TextButton(onClick = { isHelpMode = true }) {
                                    Text("Donne-moi un indice (QCM)", color = theme.contentColor.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        } else {
                            // MODE AIDE (QCM)
                            choices.forEachIndexed { index, answer ->
                                val isCorrect = EnigmaUtils.hashAnswer(answer) == currentQuestion.correctHash
                                AnswerCard(
                                    index = index,
                                    text = answer,
                                    isSelected = selectedAnswerIndex == index,
                                    isCorrect = isCorrect,
                                    showResult = selectedAnswerIndex != null,
                                    theme = theme,
                                    onClick = {
                                        if (selectedAnswerIndex == null) {
                                            selectedAnswerIndex = index
                                            scope.launch {
                                                delay(1500.milliseconds)
                                                viewModel.answerQuestion(answer, usedHelp = true)
                                                selectedAnswerIndex = null
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Text("$score points accumulés", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // ÉCRAN DE RÉSULTAT FINAL
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = accent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$score/$totalQuestions", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.backgroundColor)
                        }
                    }

                    val percentage = (score.toFloat() / (totalQuestions * 3).toFloat()) * 100
                    val resultText = when {
                        percentage >= 80 -> "Tu me connaissais bien."
                        percentage >= 50 -> "Tu savais l'essentiel."
                        else -> "Il te restait des choses à découvrir."
                    }

                    Text(resultText, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold), color = theme.contentColor, modifier = Modifier.padding(top = 16.dp))

                    if (userResult?.helpUsed == true) {
                        // Petit chambrage si aide utilisée (v8.3)
                        val randomTease = quiz!!.questions.random().let { viewModel.getRandomTeasing(it) }
                        Text(
                            text = "Note : $randomTease",
                            style = MaterialTheme.typography.bodySmall.copy(color = accent, fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (quiz!!.finalMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Halo radial (Simulation)
                                Box(modifier = Modifier.align(Alignment.TopEnd).size(80.dp).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.1f), Color.Transparent))))
                                
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("UN MOT DE TON PROCHE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(quiz!!.finalMessage, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, lineHeight = 24.sp), color = theme.contentColor)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { 
                            viewModel.submitResult(creatorId, quizId, "Participant") // Remplacer par nom réel si dispo
                            navController.navigate("quiz_leaderboard/$creatorId/$quizId")
                        },
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp).phoenXMatiere(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Voir le classement", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerCard(
    index: Int,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    val bgColor = when {
        showResult && isCorrect -> Success.copy(alpha = 0.15f)
        showResult && isSelected -> Error.copy(alpha = 0.15f)
        else -> theme.contentColor.copy(alpha = 0.03f)
    }
    val borderColor = when {
        showResult && isCorrect -> Success
        showResult && isSelected -> Error
        else -> theme.contentColor.copy(alpha = 0.1f)
    }
    val textColor = when {
        showResult && isCorrect -> Success
        showResult && isSelected -> Error
        else -> theme.contentColor
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(enabled = !showResult, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (showResult && (isCorrect || isSelected)) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${'A' + index}. $text", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textColor, modifier = Modifier.weight(1f))
            if (showResult) {
                if (isCorrect) Icon(Icons.Default.Check, null, tint = Success)
                else Icon(Icons.Default.Close, null, tint = Error)
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, accent: Color, theme: AppThemeState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = accent)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
    }
}
