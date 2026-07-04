package com.example.phoenx.ui.screens.silence

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilenceCheckInScreen(
    onImHere: () -> Unit,
    onTraversingSomething: (String) -> Unit
) {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Point pulsant animé
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(pulseScale)
                    .background(Color(0xFFC97B3A), CircleShape)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Tu es toujours là.",
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFFF2EDE8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = {
                    onImHere()
                    Toast.makeText(context, "Noté. À bientôt.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC97B3A))
            ) {
                Text("Je suis là", color = Color(0xFF1A1A1F), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC97B3A)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC97B3A))
            ) {
                Text("Je traverse quelque chose")
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1A1A1F),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2E2E35)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "On est là.",
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                        color = Color(0xFFF2EDE8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tu n'as rien à expliquer. Choisis ce qui te convient le mieux en ce moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9B9590),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    TraversingOptionCard(
                        icon = "🎙️",
                        title = "Enregistrer une pensée maintenant",
                        onClick = {
                            onTraversingSomething("record")
                            showBottomSheet = false
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TraversingOptionCard(
                        icon = "💭",
                        title = "Juste passer pour l'instant",
                        onClick = {
                            onTraversingSomething("pass")
                            showBottomSheet = false
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = { showBottomSheet = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Fermer", color = Color(0xFF9B9590))
                    }
                }
            }
        }
    }
}

@Composable
fun TraversingOptionCard(icon: String, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E35)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color(0xFFF2EDE8), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
