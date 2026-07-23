package com.example.phoenx.ui.screens.portrait

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
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
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Portrait d'Essence", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState is PortraitUiState.Idle) {
                    PortraitIntro(onGenerate = { viewModel.generatePortrait() }, theme = theme)
                }

                when (val state = uiState) {
                    is PortraitUiState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 100.dp)) {
                            CircularProgressIndicator(color = accent)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("L'IA dessine ton essence à travers tes mots...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.7f))
                        }
                    }
                    is PortraitUiState.Empty -> {
                        Text(
                            "Pas assez de pensées pour dessiner ton portrait.\nContinue de capturer tes souvenirs.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                            color = theme.contentColor.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 100.dp)
                        )
                    }
                    is PortraitUiState.Success -> {
                        PortraitContent(state.content, theme)
                    }
                    is PortraitUiState.Error -> {
                        Text("Erreur : ${state.message}", color = Error, fontWeight = FontWeight.Bold)
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun PortraitIntro(onGenerate: () -> Unit, theme: AppThemeState) {
    val accent = theme.accentColor
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
        Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Ton héritage prend forme",
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "L'IA de PHOEN-X analyse la trajectoire de tes pensées pour rédiger une synthèse de qui tu sembles être.",
            style = MaterialTheme.typography.bodyLarge,
            color = theme.contentColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onGenerate,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Générer mon Portrait", color = theme.backgroundColor, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun PortraitContent(content: String, theme: AppThemeState) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .phoenXMatiere(isPaper = true),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HistoryEdu, null, tint = accent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("VOTRE ESSENCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent, letterSpacing = 2.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = theme.fontFamily,
                    lineHeight = 32.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = theme.contentColor 
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    "Synthèse IA PHOEN-X v8.9.7",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}
