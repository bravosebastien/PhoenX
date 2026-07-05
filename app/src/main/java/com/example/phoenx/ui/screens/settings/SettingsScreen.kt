package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.RecoveryPhraseBottomSheet
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProtocol: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToReconciliation: () -> Unit,
    onNavigateToRecipients: () -> Unit,
    onNavigateToUniqueKey: () -> Unit,
    onNavigateToDetective: () -> Unit,
    onVerifyBiometrics: (onSuccess: () -> Unit) -> Unit,
    mainViewModel: MainViewModel,
    initialShowRecovery: Boolean = false
) {
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
    var showRecoveryPhrase by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ═══ SYSTÈME AVANCÉ EN VEILLE ═══
    /*
    val recoveryPhraseString by mainViewModel.recoveryPhrase.collectAsState(initial = null)

    // Déclenchement automatique de la biométrie si demandé par le rappel
    LaunchedEffect(initialShowRecovery) {
        if (initialShowRecovery) {
            onVerifyBiometrics {
                showRecoveryPhrase = true
            }
        }
    }

    if (showRecoveryPhrase && recoveryPhraseString != null) {
        RecoveryPhraseBottomSheet(
            phrase = recoveryPhraseString!!.split(" "),
            onDismiss = { showRecoveryPhrase = false }
        )
    }
    */

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Réglages", style = MaterialTheme.typography.labelLarge)
                        InfoButton(
                            title = "Réglages",
                            points = listOf(
                                "Active la biométrie pour protéger l'accès à l'application.",
                                "Choisis la fréquence des vérifications de présence.",
                                "Tes données ne sont jamais vendues ni utilisées commercialement.",
                                "PHOEN-X ne contient aucune publicité.",
                                "Tu peux gérer tes Destinataires et ton Dépositaire depuis ici."
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary, titleContentColor = TextPrimary)
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
            Text("SÉCURITÉ ET TRANSMISSION", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsItem(
                title = "Protocole d'activation",
                subtitle = "Gère ton dépositaire et tes délais",
                icon = Icons.Default.Lock,
                onClick = onNavigateToProtocol
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Mon Cercle de Confiance",
                subtitle = "Gère tes destinataires",
                icon = Icons.Default.Person,
                onClick = onNavigateToRecipients
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = SurfaceCard,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = AccentPrimary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Empreinte Digitale", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("Ouverture sécurisée de l'application", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { mainViewModel.toggleBiometric(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Le Tiroir à Clé Unique",
                subtitle = "Ton secret le plus précieux",
                icon = Icons.Default.Key,
                onClick = onNavigateToUniqueKey
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Protocole de Réconciliation",
                subtitle = "Mots secrets à ouverture différée",
                icon = Icons.Default.Mail,
                onClick = onNavigateToReconciliation
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Tester le Mode Détective",
                subtitle = "Déchiffre tes propres énigmes",
                icon = Icons.Default.Fingerprint,
                onClick = onNavigateToDetective
            )

            Spacer(modifier = Modifier.height(16.dp))

            var showRhythmDialog by remember { mutableStateOf(false) }
            val currentRhythm by mainViewModel.silenceRhythmDays.collectAsState()

            SettingsItem(
                title = "Fréquence de présence",
                subtitle = "Vérification tous les $currentRhythm jours",
                icon = Icons.Default.Timer,
                onClick = { showRhythmDialog = true }
            )

            if (showRhythmDialog) {
                RhythmSelectionDialog(
                    initialRhythm = currentRhythm,
                    onDismiss = { showRhythmDialog = false },
                    onConfirm = { days ->
                        scope.launch {
                            mainViewModel.setSilenceConfig(days)
                            showRhythmDialog = false
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            /* ═══ SYSTÈME AVANCÉ EN VEILLE ═══
            // Masqué jusqu'à réactivation V2 Pro
            Text("SÉCURITÉ", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Revoir ma phrase de récupération",
                subtitle = "Tes 12 mots de secours",
                icon = Icons.Default.VpnKey,
                onClick = {
                    onVerifyBiometrics {
                        showRecoveryPhrase = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
            */

            Text("ACCESSIBILITÉ", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Mode Vocal Total",
                subtitle = "Navigation par la voix",
                icon = Icons.Default.RecordVoiceOver,
                onClick = onNavigateToAccessibility
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Réafficher la vidéo d'accueil",
                subtitle = "Réinitialiser la bannière vidéo",
                icon = Icons.Default.VideoLibrary,
                onClick = { mainViewModel.resetVideoBanner() }
            )
        }
    }
}

@Composable
fun RhythmSelectionDialog(
    initialRhythm: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedRythm by remember { mutableIntStateOf(initialRhythm) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        title = { Text("Fréquence de présence", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.selectableGroup()) {
                RhythmOptionItem(14, "Toutes les 2 semaines", selectedRythm == 14) { selectedRythm = 14 }
                RhythmOptionItem(30, "Une fois par mois", selectedRythm == 30) { selectedRythm = 30 }
                RhythmOptionItem(60, "Tous les 2 mois", selectedRythm == 60) { selectedRythm = 60 }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Si tu ne confirmes pas ta présence, on te relancera chaque semaine. Au bout de 3 semaines sans réponse, ta personne de confiance sera prévenue pour prendre de tes nouvelles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedRythm) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Enregistrer", color = BackgroundPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextSecondary)
            }
        }
    )
}

@Composable
fun RhythmOptionItem(days: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = AccentPrimary)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) TextPrimary else TextSecondary,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = SurfaceCard,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentPrimary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextTertiary)
        }
    }
}
