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
    creatorId: String,
    depositaryId: String,
    creatorName: String,
    onActivationComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: DepositaryViewModel
) {
    var step by remember { mutableIntStateOf(0) }
    var confirmed by remember { mutableStateOf(false) }
    
    // Étape 0 : Tentatives de contact
    var contactAttemptCall by remember { mutableStateOf(false) }
    var contactAttemptFamily by remember { mutableStateOf(false) }
    var contactAttemptVisit by remember { mutableStateOf(false) }
    var contactNote by remember { mutableStateOf("") }
    
    val checkedCount = listOf(contactAttemptCall, contactAttemptFamily, contactAttemptVisit).count { it }
    val canProceedStep0 = checkedCount >= 2 && contactNote.length >= 20

    val activationSuccess by viewModel.activationSuccess.collectAsState(initial = false)

    LaunchedEffect(activationSuccess) {
        if (activationSuccess) {
            step = 3
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (step) {
            0 -> {
                Text(
                    "Avant d'aller plus loin",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif, color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Pour activer ce protocole, tu dois confirmer que tu as " +
                    "tenté de contacter $creatorName directement.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = contactAttemptCall,
                            onCheckedChange = { contactAttemptCall = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
                        )
                        Text("J'ai appelé $creatorName sans réponse", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = contactAttemptFamily,
                            onCheckedChange = { contactAttemptFamily = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
                        )
                        Text("J'ai contacté sa famille ou son entourage", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = contactAttemptVisit,
                            onCheckedChange = { contactAttemptVisit = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
                        )
                        Text("Je me suis rendu(e) à son domicile ou vérifié autrement", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = contactNote,
                    onValueChange = { contactNote = it },
                    label = { Text("Décris brièvement ce que tu as constaté") },
                    placeholder = { Text("Ex: Appelé 3 fois depuis une semaine, sans réponse...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = TextFieldDefaults.colors(focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { step = 1 },
                    enabled = canProceedStep0,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Continuer vers l'activation", color = BackgroundPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onCancel) {
                    Text("Pas encore", color = TextTertiary)
                }
            }
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
                TextButton(onClick = { step = 0 }) {
                    Text("Retour", color = TextTertiary)
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
                    onClick = { 
                        viewModel.activateProtocol(
                            creatorId = creatorId,
                            depositaryId = depositaryId,
                            contactAttemptNote = contactNote,
                            contactAttemptDetails = mapOf(
                                "call" to contactAttemptCall,
                                "family" to contactAttemptFamily,
                                "visit" to contactAttemptVisit
                            ),
                            depositaryNote = null // Optionnel
                        )
                    },
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
