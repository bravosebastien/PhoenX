package com.example.phoenx.ui.screens.portrait

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssencePortraitScreen(
    onNavigateBack: () -> Unit,
    viewModel: EssencePortraitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Portrait d'Essence", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(BackgroundSecondary, BackgroundPrimary), radius = 2000f)
        )) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState is PortraitUiState.Idle) {
                    PortraitIntro(onGenerate = { viewModel.generatePortrait() })
                }

                when (val state = uiState) {
                    is PortraitUiState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 100.dp)) {
                            CircularProgressIndicator(color = AccentPrimary)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("L'IA dessine ton essence à travers tes mots...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    is PortraitUiState.Empty -> {
                        Text(
                            "Pas assez de pensées pour dessiner ton portrait.\nContinue de capturer tes souvenirs.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 100.dp)
                        )
                    }
                    is PortraitUiState.Success -> {
                        PortraitContent(state.content)
                    }
                    is PortraitUiState.Error -> {
                        Text("Erreur : ${state.message}", color = Error)
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun PortraitIntro(onGenerate: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
        Icon(Icons.Default.AutoAwesome, null, tint = AccentPrimary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Ton héritage prend forme",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "L'IA de PHOEN-X analyse la trajectoire de tes pensées pour rédiger une synthèse de qui tu sembles être.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onGenerate,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Générer mon Portrait", color = BackgroundPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PortraitContent(content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .phoenXMatiere(isPaper = true),
        colors = CardDefaults.cardColors(containerColor = MateriauPapier.copy(alpha = 0.95f)),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HistoryEdu, null, tint = AccentPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("VOTRE ESSENCE", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 2.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 32.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF24211F) // Texte bois sur papier
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    "Synthèse IA PHOEN-X 5.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}
