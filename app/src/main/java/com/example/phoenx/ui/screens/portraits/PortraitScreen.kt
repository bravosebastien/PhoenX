package com.example.phoenx.ui.screens.portraits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortraitScreen(
    initialRecipientId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PortraitViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedRecipientId by remember { mutableStateOf(initialRecipientId) }
    val recipients by viewModel.recipients.collectAsState()

    val questions = stringArrayResource(R.array.portrait_questions).toList()
    val answers = remember { mutableStateListOf(*Array(questions.size) { "" }) }
    
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    LaunchedEffect(uiState) {
        if (uiState is PortraitUiState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    val recipientName = recipients.find { it.id == selectedRecipientId }?.name
                    val title = if (recipientName != null) stringResource(R.string.portrait_title_with_name, recipientName) else stringResource(R.string.portrait_title_fallback)
                    Text(title, style = MaterialTheme.typography.displaySmall, color = theme.contentColor) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            if (selectedRecipientId == null) {
                // Écran de sélection du destinataire
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    Text(stringResource(R.string.portrait_selection_title), style = MaterialTheme.typography.headlineSmall, color = theme.contentColor)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (recipients.isEmpty()) {
                        Text(stringResource(R.string.portrait_selection_empty), color = theme.contentColor.copy(alpha = 0.6f))
                    } else {
                        recipients.forEach { recipient ->
                            Surface(
                                onClick = { selectedRecipientId = recipient.id },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                color = theme.contentColor.copy(alpha = 0.05f),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.1f))
                            ) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(recipient.name, style = MaterialTheme.typography.bodyLarge, color = theme.contentColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(recipient.relationship, style = MaterialTheme.typography.labelSmall, color = accent)
                                }
                            }
                        }
                    }
                }
            } else {
                // Flux des questions
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                ) {
                    // Barre de progression
                    LinearProgressIndicator(
                        progress = { (step + 1) / questions.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = accent,
                        trackColor = theme.contentColor.copy(alpha = 0.1f)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.portrait_progress_label, step + 1, questions.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            letterSpacing = 2.sp
                        )
                        
                        if (answers.any { it.isNotEmpty() }) {
                            TextButton(onClick = { 
                                viewModel.savePortrait(selectedRecipientId!!, questions, answers.toList()) 
                            }) {
                                Text(stringResource(R.string.portrait_button_finish_now), color = accent.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = questions[step],
                        style = MaterialTheme.typography.displaySmall.copy(lineHeight = 34.sp),
                        color = theme.contentColor
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f).phoenXMatiere(isPaper = true),
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.02f)),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.1f))
                    ) {
                        TextField(
                            value = answers[step],
                            onValueChange = { answers[step] = it },
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            placeholder = { Text(stringResource(R.string.portrait_placeholder_thoughts), style = MaterialTheme.typography.bodyLarge, color = theme.contentColor.copy(alpha = 0.3f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = theme.contentColor,
                                unfocusedTextColor = theme.contentColor
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, color = theme.contentColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 0) {
                            TextButton(onClick = { step-- }) {
                                Text(stringResource(R.string.portrait_button_previous), color = theme.contentColor.copy(alpha = 0.6f))
                            }
                        } else {
                            TextButton(onClick = { selectedRecipientId = null }) {
                                Text(stringResource(R.string.portrait_button_change_recipient), color = theme.contentColor.copy(alpha = 0.4f))
                            }
                        }

                        Button(
                            onClick = { 
                                if (step < questions.size - 1) step++ 
                                else { 
                                    viewModel.savePortrait(selectedRecipientId!!, questions, answers.toList())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (uiState is PortraitUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = theme.backgroundColor,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val buttonText = if (step < questions.size - 1) {
                                    if (answers[step].isEmpty()) stringResource(R.string.portrait_button_skip) else stringResource(R.string.portrait_button_next)
                                } else stringResource(R.string.portrait_button_finalize)
                                Text(buttonText, color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
