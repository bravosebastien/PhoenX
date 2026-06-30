package com.example.phoenx.ui.screens.depositary

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.theme.*

@Composable
fun DepositaryAlertReceivedScreen(
    escalationLevel: Int,
    creatorId: String,
    navController: NavController,
    viewModel: DepositaryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showSuccessInput by remember { mutableStateOf(false) }
    var successNote by remember { mutableStateOf("") }
    var showHelpText by remember { mutableStateOf(false) }

    LaunchedEffect(creatorId) {
        viewModel.loadCreatorStatus(creatorId)
    }

    Scaffold(
        containerColor = BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge d'état
            val badgeColor = if (escalationLevel >= 4) AccentPrimary else Warning
            val badgeText = if (escalationLevel >= 4) "ACTION URGENTE" else "ACTION DEMANDÉE"

            Surface(
                color = badgeColor.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small,
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${uiState.creatorName} n'a pas confirmé sa présence depuis ${uiState.daysSinceLastCheckIn} jours.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
                    color = TextPrimary
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // RAPPEL DES ÉTAPES
            ContactStepItem("1", "Essaie de contacter ${uiState.creatorName}", "(appel, message, ou visite si possible)")
            Spacer(modifier = Modifier.height(16.dp))
            ContactStepItem("2", "Si tu n'y arrives pas, contacte son entourage", "")
            Spacer(modifier = Modifier.height(16.dp))
            ContactStepItem("3", "Une fois que tu as une réponse, dis-le nous ci-dessous", "")

            Spacer(modifier = Modifier.height(48.dp))

            // SECTION RÉPONSE
            if (showSuccessInput) {
                OutlinedTextField(
                    value = successNote,
                    onValueChange = { successNote = it },
                    label = { Text("Que s'est-il passé ? (Optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.resolveAlert(creatorId, "primary", successNote)
                        Toast.makeText(context, "Merci. ${uiState.creatorName} a été marqué comme présent.", Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success)
                ) {
                    Text("Confirmer le rétablissement", color = Color.White)
                }
            } else if (showHelpText) {
                Surface(
                    color = SurfaceCard,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Continue d'essayer dans les prochains jours. Si la situation persiste ou si tu confirmes une absence définitive, tu pourras accéder au protocole d'activation depuis ton tableau de bord.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) {
                            Text("Retour au tableau de bord", color = BackgroundPrimary)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showSuccessInput = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("J'ai eu de ses nouvelles, tout va bien", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showHelpText = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Warning)
                ) {
                    Icon(Icons.Default.Warning, null, tint = Warning)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Je n'arrive pas à le/la joindre", color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = { navController.navigate("depositary_onboarding") }) {
                Text("Revoir comment tout ça fonctionne", color = TextSecondary)
            }
        }
    }
}

@Composable
fun ContactStepItem(number: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(AccentPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = BackgroundPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
