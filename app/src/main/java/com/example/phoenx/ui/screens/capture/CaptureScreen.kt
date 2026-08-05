package com.example.phoenx.ui.screens.capture

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * L'ÂME DU SOUVENIR (Étape 1 unique - v9.4.26)
 * Écran simplifié ne demandant que le sujet du souvenir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    initialTitle: String? = null, // v9.4.27
    locationId: String? = null,
    locationName: String? = null,
    pactId: String? = null, // v9.4.27
    pendingQuestionId: String? = null, // v9.4.27
    parentEntryId: String? = null,
    onNavigateBack: () -> Unit,
    onNext: (String) -> Unit, // Redirection vers l'éditeur complet (L'Étincelle & son Récit)
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    
    var title by remember { mutableStateOf(initialTitle ?: "") }
    val uiState by viewModel.uiState.collectAsState()
    val isSttListening by viewModel.isSttListening.collectAsState()
    val transcript by viewModel.transcript.collectAsState()

    LaunchedEffect(transcript) {
        if (transcript.isNotEmpty()) title = transcript
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("L'Âme du Souvenir", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.Close, null, tint = theme.contentColor) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = theme.backgroundColor, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = {
                            viewModel.saveEntry(
                                content = title,
                                locationId = locationId,
                                locationName = locationName,
                                pactId = pactId,
                                pendingQuestionId = pendingQuestionId,
                                parentEntryId = parentEntryId,
                                onSuccess = { newId -> onNext(newId) }
                            )
                        },
                        enabled = title.isNotBlank() && uiState !is CaptureUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (uiState is CaptureUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.backgroundColor, strokeWidth = 2.dp)
                        } else {
                            Text("Suivant", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Donne un nom ou un sujet à ce souvenir. Tu l'enrichiras à l'étape suivante.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { 
                        Text("Quel est le sujet de ce souvenir ?", color = theme.contentColor.copy(alpha = 0.3f)) 
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = theme.contentColor, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                
                IconButton(
                    onClick = {
                        if (isSttListening) viewModel.stopVocalCapture()
                        else viewModel.startVocalCapture(title)
                    },
                    modifier = Modifier.align(Alignment.TopEnd).background(if (isSttListening) Color.Red.copy(alpha = 0.1f) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSttListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isSttListening) Color.Red else accent
                    )
                }
            }

            if (locationName != null) {
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        "📍 $locationName",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
        }
    }
}
