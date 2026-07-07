package com.example.phoenx.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.model.QuizResult
import com.example.phoenx.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizLeaderboardScreen(
    creatorId: String,
    quizId: String,
    navController: NavController,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val backgroundBrush = LocalBackgroundBrush.current
    val results by viewModel.results.collectAsState()
    val quiz by viewModel.currentQuiz.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(creatorId, quizId) {
        viewModel.loadQuiz(creatorId, quizId)
        viewModel.loadResults(creatorId, quizId)
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Classement", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic))
                            quiz?.let { Text(it.title, style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
            } else if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Personne n'a encore joué.", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic), color = TextTertiary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(results) { index, result ->
                        LeaderboardCard(index + 1, result, quiz?.showNames == true)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(rank: Int, result: QuizResult, showNames: Boolean) {
    val accent = LocalAccentColor.current
    
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF2E2E35)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E23)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = rankColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(rank.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (rank <= 3) Color(0xFF1A1A1F) else TextPrimary)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (showNames) result.recipientName ?: "Anonyme" else "Participant $rank",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                val date = result.completedAt.toDate()
                val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(date)
                Text("Répondu le $dateStr", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${result.score}/${result.totalQuestions}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif), color = accent)
                val percentage = (result.score.toFloat() / result.totalQuestions.toFloat()) * 100
                Text("${percentage.toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        }
    }
}
