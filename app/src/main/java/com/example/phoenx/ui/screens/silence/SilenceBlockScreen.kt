package com.example.phoenx.ui.screens.silence

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@Composable
fun SilenceBlockScreen(
    daysSinceLastCheckIn: Int,
    onImHere: () -> Unit
) {
    // Désactiver le bouton retour physique
    BackHandler { /* Ne rien faire */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1F))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ça fait $daysSinceLastCheckIn jours qu'on ne t'a pas eu.",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = Color(0xFFF2EDE8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Dis-nous juste que tu es là.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF9B9590),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onImHere,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC97B3A))
        ) {
            Text("Je suis là", color = Color(0xFF1A1A1F), fontWeight = FontWeight.Bold)
        }
    }
}
