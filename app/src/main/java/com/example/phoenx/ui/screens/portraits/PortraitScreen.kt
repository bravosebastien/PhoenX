package com.example.phoenx.ui.screens.portraits

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
import androidx.hilt.navigation.compose.hiltViewModel
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

    val questions = remember {
        listOf(
            "Quel trait de caractère admires-tu le plus chez cette personne ?",
            "Quel souvenir vous lie le plus fortement ?",
            "Qu'est-ce qu'elle ne sait peut-être pas sur elle-même ?",
            "Comment a-t-elle changé depuis que tu la connais ?",
            "Qu'est-ce que tu veux qu'elle sache de la façon dont tu le/la vois ?",
            "Quelle est la première chose qui te vient à l'esprit quand tu penses à elle ?",
            "Quel défi majeur avez-vous surmonté ensemble ?",
            "Quelle est la qualité que tu aimerais lui emprunter ?",
            "Quel lieu symbolise le mieux votre relation ?",
            "Quelle chanson ou quel film te fait instantanément penser à elle ?",
            "Quel conseil de sa part a marqué ta trajectoire ?",
            "Comment décrirais-tu son rire ou sa joie ?",
            "Quelle est la plus grande preuve d'attachement qu'elle t'ait donnée ?",
            "S'il ne devait rester qu'une image d'elle, laquelle serait-ce ?",
            "Qu'est-ce qui la rend unique au milieu de mille personnes ?",
            "Quel est son talent caché que peu de gens voient ?",
            "Comment a-t-elle influencé ta vision de la vie ?",
            "Quel secret ou quelle confidence partagée vous a rapprochés ?",
            "Qu'est-ce que tu aimerais lui dire si vous aviez 100 ans tous les deux ?",
            "Quelle trace penses-tu qu'elle laissera dans le cœur des gens ?"
        )
    }
    val answers = remember { mutableStateListOf(*Array(questions.size) { "" }) }
    
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is PortraitUiState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { 
                    val recipientName = recipients.find { it.id == selectedRecipientId }?.name
                    Text(recipientName?.let { "Portrait de $it" } ?: "Portrait d'un proche", style = MaterialTheme.typography.displaySmall) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(BackgroundSecondary, BackgroundPrimary), radius = 2000f)
        )) {
            if (selectedRecipientId == null) {
                // Écran de sélection du destinataire
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    Text("Pour qui écris-tu ce portrait ?", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (recipients.isEmpty()) {
                        Text("Ton cercle est vide. Ajoute tes proches sur l'accueil d'abord.", color = TextSecondary)
                    } else {
                        recipients.forEach { recipient ->
                            Surface(
                                onClick = { selectedRecipientId = recipient.id },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                color = SurfaceCard,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(recipient.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(recipient.relationship, style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
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
                        color = AccentPrimary,
                        trackColor = TextTertiary.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "QUESTION ${step + 1} SUR ${questions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary,
                            letterSpacing = 2.sp
                        )
                        
                        if (answers.any { it.isNotEmpty() }) {
                            TextButton(onClick = { 
                                val fullContent = answers.filter { it.isNotBlank() }.joinToString("\n\n")
                                viewModel.savePortrait(selectedRecipientId!!, fullContent) 
                            }) {
                                Text("Terminer maintenant", color = AccentSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = questions[step],
                        style = MaterialTheme.typography.displaySmall.copy(lineHeight = 34.sp),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f).phoenXMatiere(isPaper = true),
                        colors = CardDefaults.cardColors(containerColor = MateriauPapier.copy(alpha = 0.05f)),
                        shape = MaterialTheme.shapes.large,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.1f))
                    ) {
                        TextField(
                            value = answers[step],
                            onValueChange = { answers[step] = it },
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            placeholder = { Text("Écris tes pensées ici... (Optionnel)", style = MaterialTheme.typography.bodyLarge, color = TextTertiary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
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
                                Text("Précédent", color = TextSecondary)
                            }
                        } else {
                            TextButton(onClick = { selectedRecipientId = null }) {
                                Text("Changer de proche", color = TextTertiary)
                            }
                        }

                        Button(
                            onClick = { 
                                if (step < questions.size - 1) step++ 
                                else { 
                                    val fullContent = answers.filter { it.isNotBlank() }.joinToString("\n\n")
                                    viewModel.savePortrait(selectedRecipientId!!, fullContent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (uiState is PortraitUiState.Loading) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BackgroundPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val buttonText = if (step < questions.size - 1) {
                                    if (answers[step].isEmpty()) "Passer" else "Suivant"
                                } else "Finaliser"
                                Text(buttonText, color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
