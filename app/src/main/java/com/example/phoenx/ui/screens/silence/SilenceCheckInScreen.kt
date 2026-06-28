package com.example.phoenx.ui.screens.silence

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilenceCheckInScreen(
    onImHere: () -> Unit,
    onTraversingSomething: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Point lumineux pulsant
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(pulseScale)
                .background(AccentPrimary, shape = CircleShape)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Tu es toujours là.",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onImHere,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Je suis là", color = BackgroundPrimary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onTraversingSomething,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Je traverse quelque chose", color = TextPrimary)
        }
    }
}
