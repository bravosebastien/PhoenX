package com.example.phoenx.ui.screens.youngselfletters

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.phoenx.R
import com.example.phoenx.ui.components.PhoenXRiveAnimation
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.screens.capture.CaptureUiState
import com.example.phoenx.ui.screens.capture.CaptureViewModel
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoungSelfLetterScreen(
    onNavigateBack: () -> Unit,
    viewModel: YoungSelfLetterViewModel = hiltViewModel(),
    captureViewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val captureState by captureViewModel.uiState.collectAsState()
    
    var text by remember { mutableStateOf("") }
    var targetAge by remember { mutableFloatStateOf(20f) }
    var isRitualPlaying by remember { mutableStateOf(false) }

    // RITUEL DE DÉPÔT (ADN 5.0)
    LaunchedEffect(captureState) {
        if (captureState is CaptureUiState.Success) {
            isRitualPlaying = true
            delay(3500)
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Lettre à mon Jeune Moi", style = MaterialTheme.typography.displaySmall) },
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // SÉLECTION D'ÂGE
                Text(
                    text = "À quel âge t'écris-tu ?",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPrimary,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Slider(
                    value = targetAge,
                    onValueChange = { targetAge = it },
                    valueRange = 10f..(uiState.currentAge.toFloat().coerceAtLeast(15f)),
                    steps = (uiState.currentAge - 10).coerceAtLeast(1),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPrimary,
                        activeTrackColor = AccentPrimary,
                        inactiveTrackColor = TextTertiary.copy(alpha = 0.3f)
                    )
                )
                
                Text(
                    text = "C'est à celui que tu étais à ${targetAge.toInt()} ans que tu vas parler.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(40.dp))

                // LA LETTRE (MATIÈRE PAPIER)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .phoenXMatiere(isPaper = true),
                    colors = CardDefaults.cardColors(containerColor = MateriauPapier.copy(alpha = 0.1f)),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(2.dp).height(30.dp).background(AccentPrimary))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "À toi, à ${targetAge.toInt()} ans —",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 20.sp
                                ),
                                color = AccentPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { 
                                Text("Qu'aurais-tu voulu entendre à cet âge-là ?", 
                                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                                color = TextTertiary) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 30.sp,
                                color = TextPrimary
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // SUGGESTIONS IA
                if (uiState.aiSuggestions != null) {
                    Surface(
                        color = AccentPrimary.copy(alpha = 0.05f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.aiSuggestions!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.getSuggestions(targetAge.toInt()) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Besoin d'un fil conducteur ?", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // BOUTON DÉPÔT
                Button(
                    onClick = { 
                        captureViewModel.saveEntry(
                            content = text,
                            mediaFile = null,
                            type = Screen.Capture.TYPE_TEXT,
                            category = "Sagesse",
                            visibility = "Privé",
                            isYoungSelfLetter = true,
                            targetAge = targetAge.toInt()
                        )
                    },
                    enabled = text.isNotEmpty() && captureState !is CaptureUiState.Loading && !isRitualPlaying,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (captureState is CaptureUiState.Loading) {
                        CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Déposer la lettre", color = BackgroundPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // OVERLAY RITUEL (RIVE)
            if (isRitualPlaying) {
                Box(
                    modifier = Modifier.fillMaxSize().background(BackgroundPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PhoenXRiveAnimation(
                            resId = R.raw.depot,
                            modifier = Modifier.size(320.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Lettre confiée au temps.",
                            style = MaterialTheme.typography.displaySmall,
                            color = AccentPrimary
                        )
                    }
                }
            }
        }
    }
}
