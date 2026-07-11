package com.example.phoenx.ui.screens.witness

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
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
    token: String,
    navController: NavController,
    viewModel: WitnessViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val backgroundBrush = LocalBackgroundBrush.current
    
    var testimonyText by remember { mutableStateOf("") }
    var isRitualPlaying by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()

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

        Box(modifier = Modifier.fillMaxSize().background(BackgroundPrimary), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).scale(scale),
                    tint = accent
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Témoignage scellé.\nMerci pour ce souvenir.", style = MaterialTheme.typography.displaySmall, color = accent, textAlign = TextAlign.Center)
            }
        }
        LaunchedEffect(Unit) {
            delay(4000)
            navController.popBackStack()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
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
                "Un témoignage précieux",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Raconte un moment où ton proche t'a surpris. Ce souvenir sera gardé précieusement et transmis à ses héritiers.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ZONE PAPIER SACRÉ
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242429)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
            ) {
                Box(modifier = Modifier.padding(24.dp)) {
                    if (testimonyText.isEmpty()) {
                        Text(
                            "Écris ton histoire ici...",
                            style = TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = TextTertiary)
                        )
                    }
                    BasicTextField(
                        value = testimonyText,
                        onValueChange = { testimonyText = it },
                        textStyle = TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = TextPrimary, lineHeight = 30.sp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Send, null, tint = BackgroundPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sceller mon témoignage", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Ton témoignage est chiffré. Seul ton proche (s'il l'a autorisé) et ses héritiers pourront le lire.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
