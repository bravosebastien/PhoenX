package com.example.phoenx.ui.screens.depositary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@Composable
fun DepositaryScreen(
    creatorName: String = "Fab",
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(containerColor = theme.backgroundColor) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Une décision importante",
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                color = theme.contentColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Tu as été désigné(e) comme gardien(ne) de confiance par $creatorName. " +
                "Cette confirmation est irréversible. Elle donnera accès aux souvenirs préparés pour ses proches.",
                style = MaterialTheme.typography.bodyLarge,
                color = theme.contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Je confirme, $creatorName est décédé(e)", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor)
            ) {
                Text("Annuler", color = theme.contentColor)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Ce protocole ne remplace pas un testament. Il n'a aucune valeur légale.",
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}
