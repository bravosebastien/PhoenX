package com.example.phoenx.ui.screens.youngselfletters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoungSelfLetterScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: YoungSelfLetterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val existingLetters by viewModel.existingLetters.collectAsState()
    var showSuggestions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("Lettre à mon Jeune Moi", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AccentPrimary)
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
                .padding(24.dp)
        ) {
            // ── HISTORIQUE (v8.6.2) ──────────────────
            if (existingLetters.isNotEmpty()) {
                Text(
                    "VOS LETTRES SCELLÉES", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = AccentPrimary, 
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
                    items(existingLetters) { letter ->
                        Card(
                            onClick = { navController.navigate(Screen.MemoryDetail.createRoute(letter.id)) },
                            modifier = Modifier.width(160.dp).height(100.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.HistoryEdu, null, tint = AccentPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("À mes ${letter.targetAge} ans", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                val year = uiState.birthYear + (letter.targetAge ?: 0)
                                Text("Écrit pour $year", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                        }
                    }
                }
                HorizontalDivider(color = TextTertiary.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 32.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Qu'aurais-tu voulu entendre, à l'âge où tout semblait encore incertain ?",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                        lineHeight = 32.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                InfoButton(
                    title = "Lettre à Mon Jeune Moi",
                    points = listOf(
                        "Choisis un âge dans ton passé et écris une lettre à toi-même.",
                        "L'application calcule l'année correspondante depuis ta date de naissance.",
                        "Si tu manques d'inspiration, tape sur l'ampoule pour des suggestions.",
                        "Cette lettre sera conservée comme un souvenir normal dans ton Fil de Pensée.",
                        "C'est l'une des fonctionnalités les plus touchantes pour tes proches."
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SÉLECTEUR D'ÂGE
            Text(
                text = "À mes ${uiState.targetAge} ans",
                style = MaterialTheme.typography.headlineSmall,
                color = AccentPrimary
            )
            Text(
                text = "C'était en ${uiState.calculatedYear}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            
            Slider(
                value = uiState.targetAge.toFloat(),
                onValueChange = { viewModel.updateTargetAge(it.toInt()) },
                valueRange = 10f..80f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentPrimary,
                    activeTrackColor = AccentPrimary,
                    inactiveTrackColor = SurfaceCard
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ZONE D'ÉCRITURE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF242429))
                    .padding(20.dp)
            ) {
                if (uiState.letterContent.isEmpty()) {
                    Text(
                        text = "Cher moi de ${uiState.targetAge} ans...",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF5C5855)
                        )
                    )
                }

                BasicTextField(
                    value = uiState.letterContent,
                    onValueChange = { viewModel.updateContent(it) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                        lineHeight = 28.sp
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOUTON SUGGESTIONS
            TextButton(
                onClick = { 
                    viewModel.getSuggestions()
                    showSuggestions = true 
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voir des pistes d'inspiration", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { 
                    viewModel.saveLetter {
                        onNavigateBack()
                    }
                },
                enabled = uiState.letterContent.isNotBlank() && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Sceller cette lettre", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showSuggestions) {
            ModalBottomSheet(
                onDismissRequest = { showSuggestions = false },
                sheetState = sheetState,
                containerColor = BackgroundSecondary
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
                    Text("PISTES D'INSPIRATION", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (uiState.isLoadingSuggestions) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = AccentPrimary)
                    } else if (uiState.suggestions.isEmpty()) {
                        Text("Aucune suggestion pour le moment. Continue à remplir ton héritage !", color = TextSecondary)
                    } else {
                        uiState.suggestions.forEach { suggestion ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { 
                                        viewModel.updateContent(uiState.letterContent + "\n" + suggestion)
                                        showSuggestions = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                            ) {
                                Text(
                                    text = suggestion,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
