package com.example.phoenx.ui.screens.capture

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    initialType: String = "TEXT",
    onNavigateBack: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    var text by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Sagesse") }
    var visibility by remember { mutableStateOf("Privé") }
    
    // État pour le rituel de dépôt (l'enveloppe qui part)
    var isDepositing by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Success) {
            isDepositing = true
            delay(800) // Laisse le temps à l'animation de se jouer
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    Text(
                        "🔒 Chiffré avant envoi",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = BackgroundPrimary, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("Annuler", color = TextSecondary)
                    }
                    Button(
                        onClick = { viewModel.saveEntry(text, initialType, selectedCategory, visibility) },
                        enabled = text.isNotEmpty() && uiState !is CaptureUiState.Loading && !isDepositing,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (uiState is CaptureUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BackgroundPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Déposer", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        // Animation du Rituel de Dépôt : Le contenu glisse vers le haut comme une lettre qu'on poste
        AnimatedVisibility(
            visible = !isDepositing,
            exit = slideOutVertically(tween(800)) { -it } + fadeOut(tween(600)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(BackgroundSecondary, BackgroundPrimary), radius = 2000f)
            )) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Zone de saisie (Matière)
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { 
                            Text(
                                "Écris ce qui ne doit pas se perdre...", 
                                style = MaterialTheme.typography.displaySmall,
                                color = TextTertiary
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        textStyle = MaterialTheme.typography.displaySmall.copy(color = TextPrimary, lineHeight = 34.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Catégories (Chips élégants)
                    Text("ÉTAT ÉMOTIONNEL", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf("Espoir", "Poésie", "Coups de gueule", "Angoisses", "Bonheur", "Plaisir", "Regret", "Sagesse", "Amour", "Valeurs")
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentPrimary,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedCategory == cat,
                                    borderColor = TextTertiary.copy(alpha = 0.1f),
                                    selectedBorderColor = AccentPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Visibilité
                    Text("DESTINATION", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Privé", "Tous", "Choisir", "Public").forEach { vis ->
                            AssistChip(
                                onClick = { visibility = vis },
                                label = { Text(vis) },
                                shape = CircleShape,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (visibility == vis) SurfaceCard else Color.Transparent,
                                    labelColor = if (visibility == vis) TextPrimary else TextSecondary
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = if (visibility == vis) AccentPrimary else TextTertiary.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Overlay de succès (Rituel)
        if (isDepositing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Souvenir déposé.", 
                    style = MaterialTheme.typography.displayMedium, 
                    color = AccentPrimary,
                    modifier = Modifier.animateContentSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = { content() }
    )
}
