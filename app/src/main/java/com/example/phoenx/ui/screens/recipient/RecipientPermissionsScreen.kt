package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.screens.recipient.RecipientUiState
import com.example.phoenx.ui.screens.recipient.RecipientViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientPermissionsScreen(
    recipientId: String,
    onNavigateBack: () -> Unit,
    viewModel: RecipientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val recipients = (uiState as? RecipientUiState.Success)?.recipients ?: emptyList()
    val recipient = recipients.find { it.id == recipientId }

    var canAskQuestions by remember { mutableStateOf(false) }
    var limitQuestions by remember { mutableStateOf(false) }
    var maxQuestions by remember { mutableIntStateOf(10) }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("${recipient?.name ?: "Proche"} — Droits d'accès", style = MaterialTheme.typography.labelLarge, color = theme.contentColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LaunchedEffect(recipient) {
            recipient?.let {
                canAskQuestions = it.canAskQuestions
                limitQuestions = it.maxQuestionsAllowed != null
                maxQuestions = it.maxQuestionsAllowed ?: 10
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text("QUESTIONS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Autoriser ${recipient?.name ?: "ce proche"} à me poser des questions",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                        Switch(
                            checked = canAskQuestions,
                            onCheckedChange = { canAskQuestions = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = accent)
                        )
                    }
                    
                    Text(
                        "${recipient?.name ?: "Ce proche"} pourra déposer des questions scellées. Tu y répondras, ou choisiras consciemment de ne pas y répondre. Les questions restent invisibles pour lui/elle jusqu'à l'activation du protocole.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.4f),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (canAskQuestions) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = theme.contentColor.copy(alpha = 0.1f))
                        
                        Text("LIMITE DE QUESTIONS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !limitQuestions,
                                onClick = { limitQuestions = false },
                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                            )
                            Text("Nombre illimité de questions", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = limitQuestions,
                                onClick = { limitQuestions = true },
                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                            )
                            Text("Limiter à un nombre précis", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor)
                        }

                        if (limitQuestions) {
                            Row(
                                modifier = Modifier.padding(start = 48.dp, top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (maxQuestions > 1) maxQuestions-- },
                                    modifier = Modifier.background(theme.contentColor.copy(alpha = 0.05f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, null, tint = accent)
                                }
                                Text(
                                    text = "$maxQuestions",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = theme.contentColor
                                )
                                IconButton(
                                    onClick = { if (maxQuestions < 100) maxQuestions++ },
                                    modifier = Modifier.background(theme.contentColor.copy(alpha = 0.05f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = accent)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { 
                    viewModel.updatePermissions(
                        recipientId, 
                        canAskQuestions, 
                        if (limitQuestions) maxQuestions else null
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Enregistrer les droits", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
