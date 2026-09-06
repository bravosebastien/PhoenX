package com.example.phoenx.ui.screens.questions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HundredQuestionsLeaderboardScreen(
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: HundredQuestionsLeaderboardViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(creatorId) {
        viewModel.loadResults(creatorId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Classement Devinettes", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else if (uiState.results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Personne n'a encore deviné de questions.", color = theme.contentColor.copy(alpha = 0.4f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.results) { index, result ->
                    GuessLeaderboardCard(index + 1, result, theme)
                }
            }
        }
    }
}

@Composable
fun GuessLeaderboardCard(rank: Int, result: GuessRecipientResult, theme: AppThemeState) {
    val accent = theme.accentColor
    var isExpanded by remember { mutableStateOf(false) }
    
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = if (rank <= 3) rankColor else theme.contentColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(rank.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (rank <= 3) Color.Black else theme.contentColor)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.recipientName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    Text("${result.totalCorrect} bonnes réponses", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = theme.contentColor.copy(alpha = 0.3f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    result.details.forEach { detail ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = detail.question,
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            if (detail.isCorrect) {
                                Icon(Icons.Default.Check, null, tint = Success, modifier = Modifier.size(14.dp))
                            } else {
                                Icon(Icons.Default.Close, null, tint = Error, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class GuessRecipientResult(
    val recipientId: String,
    val recipientName: String,
    val totalCorrect: Int,
    val details: List<GuessDetail>
)

data class GuessDetail(
    val question: String,
    val isCorrect: Boolean,
    val attempts: Int
)
