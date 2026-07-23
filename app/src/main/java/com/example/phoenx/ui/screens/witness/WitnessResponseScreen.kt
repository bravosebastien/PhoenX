package com.example.phoenx.ui.screens.witness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.CheckCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WitnessResponseScreen(
    creatorId: String,
    witnessId: String,
    token: String?,
    navController: NavController,
    viewModel: WitnessViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var testimonyText by remember { mutableStateOf("") }
    var isRitualPlaying by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val creatorName by viewModel.creatorName.collectAsState()
    val witnessConfig by viewModel.witnessConfig.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.verifyToken(creatorId, witnessId, token)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // GESTION ÉTAT : DÉJÀ SOUMIS
    if (witnessConfig?.submittedAt != null && !isRitualPlaying) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = Success, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text("Témoignage déjà scellé", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Vous avez déjà envoyé votre témoignage pour $creatorName. Il est maintenant en sécurité.",
                    color = theme.contentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = accent), modifier = Modifier.phoenXMatiere()) {
                    Text("Retour", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    if (isRitualPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "ritual_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).scale(scale),
                    tint = accent
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Témoignage scellé.\nMerci pour ce souvenir.", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = accent, textAlign = TextAlign.Center)
            }
        }
        LaunchedEffect(Unit) {
            delay(4000)
            navController.popBackStack()
        }
        return
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Un témoignage pour ${creatorName ?: "ton proche"}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                val instructionText = if (!witnessConfig?.requestPrompt.isNullOrBlank()) {
                    witnessConfig!!.requestPrompt!!
                } else {
                    "Raconte un moment où ${creatorName ?: "ton proche"} t'a surpris. Ce souvenir sera gardé précieusement et transmis à ses héritiers."
                }

                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (!witnessConfig?.requestPrompt.isNullOrBlank()) accent else theme.contentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    fontStyle = if (!witnessConfig?.requestPrompt.isNullOrBlank()) FontStyle.Italic else FontStyle.Normal
                )

                Spacer(modifier = Modifier.height(40.dp))

                // ZONE PAPIER SACRÉ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        if (testimonyText.isEmpty()) {
                            Text(
                                "Écris ton histoire ici...",
                                style = TextStyle(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = theme.contentColor.copy(alpha = 0.3f))
                            )
                        }
                        BasicTextField(
                            value = testimonyText,
                            onValueChange = { testimonyText = it },
                            textStyle = TextStyle(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = theme.contentColor, lineHeight = 30.sp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                            cursorBrush = Brush.verticalGradient(listOf(accent, accent))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        viewModel.submitTestimony(creatorId, witnessId, token, testimonyText) {
                            isRitualPlaying = true
                        }
                    },
                    enabled = testimonyText.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = theme.backgroundColor, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = theme.backgroundColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sceller mon témoignage", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val (transparencyTitle, transparencyDesc) = if (witnessConfig?.allowReject == true) {
                    "Validation préalable" to "Ton proche pourra lire ce témoignage avant de le transmettre, et pourra choisir de ne pas le transmettre s'il le juge inapproprié."
                } else if (witnessConfig?.allowRead == true) {
                    "Témoignage ouvert" to "Ton proche a demandé à pouvoir lire ce témoignage de son vivant. Il sera également transmis à ses héritiers."
                } else {
                    "Confidentialité totale" to "Ton témoignage est chiffré. Seul ton proche et ses héritiers pourront le lire après l'activation du protocole."
                }

                Text(
                    text = transparencyTitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transparencyDesc,
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.contentColor.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
