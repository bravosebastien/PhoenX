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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@Composable
fun SilenceOnboardingScreen(
    onConfirmRythm: (Int) -> Unit
) {
    var selectedRythm by remember { mutableIntStateOf(30) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1F))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Point pulsant animé
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(pulseScale)
                .background(Color(0xFFC97B3A), CircleShape)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Une présence, même dans le silence",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                color = Color(0xFFF2EDE8)
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "De temps en temps, PHOEN-X te demandera juste de confirmer que tu es là. Un simple tap suffit. Si tu ne réponds pas pendant un moment, ta personne de confiance sera doucement prévenue pour prendre de tes nouvelles.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9B9590),
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "À quelle fréquence veux-tu qu'on te contacte ?",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFF2EDE8),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        RythmRadioOption(14, "Toutes les 2 semaines", selectedRythm == 14) { selectedRythm = 14 }
        RythmRadioOption(30, "Une fois par mois", selectedRythm == 30) { selectedRythm = 30 }
        RythmRadioOption(60, "Tous les 2 mois", selectedRythm == 60) { selectedRythm = 60 }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Si tu ne réponds pas 3 fois de suite, ta personne de confiance recevra un message doux.",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF5C5855),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                onConfirmRythm(selectedRythm)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC97B3A))
        ) {
            Text("Je choisis ce rythme", color = Color(0xFF1A1A1F), fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RythmRadioOption(days: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC97B3A), unselectedColor = Color(0xFF5C5855))
        )
        Text(
            text = label,
            color = if (selected) Color(0xFFF2EDE8) else Color(0xFF9B9590),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
