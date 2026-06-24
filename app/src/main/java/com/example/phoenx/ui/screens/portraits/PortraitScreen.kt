package com.example.phoenx.ui.screens.portraits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortraitScreen(
    onNavigateBack: () -> Unit,
    viewModel: PortraitViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(0) }
    val questions = listOf(
        "Quel trait de caractère admires-tu le plus chez [Prénom] ?",
        "Quel souvenir vous lie le plus fortement ?",
        "Qu'est-ce que [Prénom] ne sait peut-être pas sur lui/elle-même ?",
        "Comment [Prénom] a-t-il/elle changé depuis que tu le/la connais ?",
        "Qu'est-ce que tu veux qu'il/elle sache de la façon dont tu le/la vois ?"
    )
    val answers = remember { mutableStateListOf("", "", "", "", "") }
    
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
                title = { Text("Portrait d'un proche", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary, titleContentColor = TextPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            LinearProgressIndicator(
                progress = { (step + 1) / 5f },
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                color = AccentPrimary,
                trackColor = SurfaceCard
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Question ${step + 1} sur 5",
                style = MaterialTheme.typography.labelSmall,
                color = AccentPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = questions[step],
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextField(
                value = answers[step],
                onValueChange = { answers[step] = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = { Text("Écris ici...", color = TextTertiary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 0) {
                    TextButton(onClick = { step-- }) {
                        Text("Précédent", color = TextSecondary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = { 
                        if (step < 4) step++ 
                        else { 
                            viewModel.savePortrait("friend_id", answers.toList())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    enabled = answers[step].isNotEmpty() && uiState !is PortraitUiState.Loading
                ) {
                    if (uiState is PortraitUiState.Loading) {
                        CircularProgressIndicator(size = 20.dp, color = BackgroundPrimary)
                    } else {
                        Text(if (step < 4) "Suivant" else "Finaliser", color = BackgroundPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun CircularProgressIndicator(size: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp
    )
}
