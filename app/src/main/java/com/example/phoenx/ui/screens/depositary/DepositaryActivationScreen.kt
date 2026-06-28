package com.example.phoenx.ui.screens.depositary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@Composable
fun DepositaryActivationScreen(
    creatorName: String,
    onActivationComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var confirmed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (step) {
            1 -> {
                Text("Avant de continuer", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, color = TextPrimary))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Cette action activera le protocole de transmission de $creatorName. Elle est irréversible.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = { step = 2 }, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Oui, je continue", color = BackgroundPrimary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onCancel) {
                    Text("Pas encore", color = TextTertiary)
                }
            }
            2 -> {
                Text("Une confirmation de ta part", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, color = TextPrimary))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "En confirmant, tu attestes que $creatorName n'est plus parmi nous.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = confirmed, onCheckedChange = { confirmed = it }, colors = CheckboxDefaults.colors(checkedColor = AccentPrimary))
                    Text("Je confirme, en conscience, ce que je déclare ici.", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { step = 3 }, 
                    enabled = confirmed,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary), 
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Confirmer", color = BackgroundPrimary)
                }
            }
            3 -> {
                Text("La transmission commencera dans 72 heures", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, color = TextPrimary), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Ce délai existe pour protéger $creatorName. Si sa famille ou lui-même conteste dans les 72h, le protocole sera annulé. Après ce délai, les Destinataires recevront leur invitation.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(64.dp))
                Button(onClick = onActivationComplete, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Je comprends", color = BackgroundPrimary)
                }
            }
        }
    }
}
