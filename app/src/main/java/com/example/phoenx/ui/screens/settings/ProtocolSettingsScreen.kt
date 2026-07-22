package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProtocolViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val snackbarHostState = remember { SnackbarHostState() }
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var threshold by remember { mutableFloatStateOf(72f) }

    // Synchronisation initiale uniquement (Évite d'écraser la saisie en cours)
    var hasInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (!hasInitialized && uiState.name.isNotEmpty()) {
            name = uiState.name
            email = uiState.email
            phone = uiState.phone
            threshold = uiState.thresholdHours.toFloat()
            hasInitialized = true
        }
    }

    // Gestion du succès
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Réglages enregistrés avec succès.")
            kotlinx.coroutines.delay(1500)
            onNavigateBack()
        }
    }

    // Gestion des erreurs
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Protocole d'activation", 
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                actions = {
                    InfoPoint(
                        title = "La Transmission",
                        content = "C'est ici que tout se joue. Le dépositaire est la seule personne qui pourra confirmer votre départ pour ouvrir l'accès à vos souvenirs. Le délai de contestation est votre sécurité pour annuler une fausse alerte."
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.contentColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                "Gère ton héritage",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("STATUT ACTUEL", style = MaterialTheme.typography.labelSmall, color = Success)
                    Text(uiState.status, style = MaterialTheme.typography.bodyLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("LA PERSONNE DE CONFIANCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                    focusedLabelColor = accent,
                    unfocusedLabelColor = theme.contentColor.copy(alpha = 0.4f),
                    focusedTextColor = theme.contentColor,
                    unfocusedTextColor = theme.contentColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                    focusedLabelColor = accent,
                    unfocusedLabelColor = theme.contentColor.copy(alpha = 0.4f),
                    focusedTextColor = theme.contentColor,
                    unfocusedTextColor = theme.contentColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Téléphone") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                    focusedLabelColor = accent,
                    unfocusedLabelColor = theme.contentColor.copy(alpha = 0.4f),
                    focusedTextColor = theme.contentColor,
                    unfocusedTextColor = theme.contentColor
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("DÉLAI DE CONTESTATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Temps dont tu disposeras pour annuler une activation par erreur.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f)
            )
            
            Slider(
                value = threshold,
                onValueChange = { threshold = it },
                enabled = !uiState.isLoading,
                valueRange = 24f..72f,
                steps = 2,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
            Text("${threshold.toInt()} heures", style = MaterialTheme.typography.bodyLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { viewModel.saveProtocol(name, email, phone, threshold.toInt()) },
                enabled = !uiState.isLoading && name.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = theme.backgroundColor, strokeWidth = 2.dp)
                } else {
                    Text("Enregistrer les réglages", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Note : Ce protocole est moral et privé. Il ne remplace pas les dispositions légales de succession.",
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
