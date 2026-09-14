package com.example.phoenx.ui.screens.questions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.domain.model.PendingQuestion
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingQuestionsScreen(
    onNavigateBack: () -> Unit,
    onAnswerQuestion: (String) -> Unit,
    viewModel: PendingQuestionsViewModel = hiltViewModel()
) {
    val questions by viewModel.questions.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedQuestion by remember { mutableStateOf<PendingQuestion?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(theme.backgroundColor),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.questions_pending_title), style = MaterialTheme.typography.labelLarge, color = theme.contentColor)
                        InfoButton(
                            title = stringResource(R.string.questions_pending_info_title),
                            points = listOf(
                                stringResource(R.string.questions_pending_info_p1),
                                stringResource(R.string.questions_pending_info_p2),
                                stringResource(R.string.questions_pending_info_p3),
                                stringResource(R.string.questions_pending_info_p4),
                                stringResource(R.string.questions_pending_info_p5)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
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
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accent)
            }

            Text(
                text = stringResource(R.string.questions_pending_count, questions.size, questions.map { it.recipientName }.distinct().size),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = theme.contentColor.copy(alpha = 0.4f)
            )

            if (questions.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.questions_pending_empty), color = theme.contentColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(questions) { question ->
                        QuestionCard(
                            question = question,
                            theme = theme
                        ) { selectedQuestion = question }
                    }
                }
            }
        }

        if (selectedQuestion != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedQuestion = null },
                sheetState = sheetState,
                containerColor = theme.backgroundColor
            ) {
                QuestionActionContent(
                    question = selectedQuestion!!,
                    theme = theme,
                    onAnswer = { 
                        onAnswerQuestion(selectedQuestion!!.id)
                        selectedQuestion = null 
                    },
                    onDecline = { note ->
                        viewModel.declineQuestion(selectedQuestion!!.id, note)
                        selectedQuestion = null
                    },
                    onDismiss = { selectedQuestion = null }
                )
            }
        }
    }
}

@Composable
fun QuestionCard(question: PendingQuestion, theme: AppThemeState, onClick: () -> Unit) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(question.recipientName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(8.dp))
            Text(question.questionText, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
            Spacer(modifier = Modifier.height(12.dp))
            
            val date = DateTimeFormatter.ofPattern(stringResource(R.string.questions_date_format))
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(question.askedAt))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(date, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.weight(1f))
                
                Surface(
                    color = when(question.status) {
                        "pending" -> accent.copy(alpha = 0.2f)
                        "answered" -> Success.copy(alpha = 0.2f)
                        else -> theme.contentColor.copy(alpha = 0.1f)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when(question.status) {
                            "pending" -> stringResource(R.string.questions_status_pending)
                            "answered" -> stringResource(R.string.questions_status_answered_pending)
                            else -> stringResource(R.string.questions_status_declined)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when(question.status) {
                            "pending" -> accent
                            "answered" -> Success
                            else -> theme.contentColor.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionActionContent(
    question: PendingQuestion,
    theme: AppThemeState,
    onAnswer: () -> Unit,
    onDecline: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = theme.accentColor
    var showDeclineNote by remember { mutableStateOf(false) }
    var declineNote by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
        Text(
            text = question.questionText,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic),
            color = theme.contentColor,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (!showDeclineNote) {
            Button(
                onClick = onAnswer,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.questions_pending_btn_answer_now), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showDeclineNote = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor)
            ) {
                Text(stringResource(R.string.questions_pending_btn_decline), color = theme.contentColor)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.questions_pending_btn_later), color = theme.contentColor.copy(alpha = 0.4f))
            }
        } else {
            Text(
                stringResource(R.string.questions_pending_decline_note_info, question.recipientName),
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = declineNote,
                onValueChange = { declineNote = it },
                placeholder = { Text(stringResource(R.string.questions_pending_decline_note_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = theme.contentColor),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onDecline(declineNote) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.contentColor.copy(alpha = 0.1f))
            ) {
                Text(stringResource(R.string.questions_pending_btn_confirm_decline), color = theme.contentColor)
            }
            TextButton(onClick = { showDeclineNote = false }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.questions_pending_btn_back), color = accent)
            }
        }
    }
}
