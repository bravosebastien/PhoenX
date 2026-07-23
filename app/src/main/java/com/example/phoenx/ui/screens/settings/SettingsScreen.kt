package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToProfile: () -> Unit,
    onNavigateToProtocol: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToNotificationContacts: () -> Unit,
    onNavigateToReconciliation: () -> Unit,
    onNavigateToRecipients: () -> Unit,
    onNavigateToUniqueKey: () -> Unit,
    onNavigateToDetective: () -> Unit,
    onVerifyBiometrics: (onSuccess: () -> Unit) -> Unit,
    mainViewModel: MainViewModel,
    initialShowRecovery: Boolean = false
) {
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
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
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Réglages", style = MaterialTheme.typography.labelLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor, titleContentColor = theme.contentColor)
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
            Text("MON COMPTE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Mon Profil",
                subtitle = "Gère ton nom d'usage et tes infos",
                icon = Icons.Default.AccountCircle,
                theme = theme,
                onClick = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("SÉCURITÉ ET TRANSMISSION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsItem(
                title = "Protocole d'activation",
                subtitle = "Gère ton dépositaire et tes délais",
                icon = Icons.Default.Lock,
                theme = theme,
                onClick = onNavigateToProtocol
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Mon Cercle de Confiance",
                subtitle = "Gère tes destinataires",
                icon = Icons.Default.Person,
                theme = theme,
                onClick = onNavigateToRecipients
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Contacts à prévenir",
                subtitle = "Personnes informées de ton départ",
                icon = Icons.Default.NotificationsNone,
                theme = theme,
                onClick = onNavigateToNotificationContacts
            )

            Spacer(modifier = Modifier.height(16.dp))

            var showRhythmDialog by remember { mutableStateOf(false) }
            val currentRhythm by mainViewModel.silenceRhythmDays.collectAsState()

            SettingsItem(
                title = "Fréquence de présence",
                subtitle = "Vérification tous les $currentRhythm jours",
                icon = Icons.Default.Timer,
                theme = theme,
                onClick = { showRhythmDialog = true }
            )

            if (showRhythmDialog) {
                RhythmSelectionDialog(
                    initialRhythm = currentRhythm,
                    onDismiss = { showRhythmDialog = false },
                    theme = theme,
                    onConfirm = { days ->
                        scope.launch {
                            mainViewModel.setSilenceConfig(days)
                            showRhythmDialog = false
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = theme.contentColor.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = accent)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Empreinte Digitale", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                        Text("Ouverture sécurisée de l'application", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { mainViewModel.toggleBiometric(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Le Tiroir à Clé Unique",
                subtitle = "Ton secret le plus précieux",
                icon = Icons.Default.Key,
                theme = theme,
                onClick = onNavigateToUniqueKey
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Protocole de Réconciliation",
                subtitle = "Mots secrets à ouverture différée",
                icon = Icons.Default.Mail,
                theme = theme,
                onClick = onNavigateToReconciliation
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Tester le Mode Détective",
                subtitle = "Déchiffre tes propres énigmes",
                icon = Icons.Default.Fingerprint,
                theme = theme,
                onClick = onNavigateToDetective
            )

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(32.dp))

            /* ═══ SYSTÈME AVANCÉ EN VEILLE ═══
            // Masqué jusqu'à réactivation V2 Pro
            Text("SÉCURITÉ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Revoir ma phrase de récupération",
                subtitle = "Tes 12 mots de secours",
                icon = Icons.Default.VpnKey,
                theme = theme,
                onClick = {
                    onVerifyBiometrics {
                        showRecoveryPhrase = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
            */

            Text("ACCESSIBILITÉ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Mode Vocal Total",
                subtitle = "Navigation par la voix",
                icon = Icons.Default.RecordVoiceOver,
                theme = theme,
                onClick = onNavigateToAccessibility
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Réafficher la vidéo d'accueil",
                subtitle = "Réinitialiser la bannière vidéo",
                icon = Icons.Default.VideoLibrary,
                theme = theme,
                onClick = { mainViewModel.resetVideoBanner() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("PERSONNALISATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = theme.contentColor.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Couleur d'accentuation", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val colors = listOf(
                        Color(0xFFC97B3A), // Gold
                        Color(0xFFE91E63), // Pink
                        Color(0xFF2196F3), // Blue
                        Color(0xFF4CAF50), // Green
                        Color(0xFF9C27B0), // Purple
                        Color(0xFFF44336)  // Red
                    )
                    
                    val currentAccentInt by mainViewModel.accentColor.collectAsState()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { color ->
                            val isSelected = color.toArgb() == currentAccentInt
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) theme.contentColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { mainViewModel.setAccentColor(color.toArgb()) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = theme.contentColor.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Style de fond", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val styles = listOf(
                        "RADIAL" to "Radial (Défaut)",
                        "LINEAR" to "Linéaire",
                        "SOLID" to "Uni"
                    )
                    
                    val currentStyle by mainViewModel.backgroundStyle.collectAsState()
                    
                    Column(Modifier.selectableGroup()) {
                        styles.forEach { (style, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = currentStyle == style,
                                        onClick = { mainViewModel.setBackgroundStyle(style) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentStyle == style,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = accent)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (currentStyle == style) theme.contentColor else theme.contentColor.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RhythmSelectionDialog(
    initialRhythm: Int,
    onDismiss: () -> Unit,
    theme: AppThemeState,
    onConfirm: (Int) -> Unit
) {
    val accent = theme.accentColor
    var selectedRythm by remember { mutableIntStateOf(initialRhythm) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text("Fréquence de présence", color = theme.contentColor, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold)) },
        text = {
            Column(Modifier.selectableGroup()) {
                RhythmOptionItem(14, "Toutes les 2 semaines", selectedRythm == 14, theme) { selectedRythm = 14 }
                RhythmOptionItem(30, "Une fois par mois", selectedRythm == 30, theme) { selectedRythm = 30 }
                RhythmOptionItem(60, "Tous les 2 mois", selectedRythm == 60, theme) { selectedRythm = 60 }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Si tu ne confirmes pas ta présence, on te relancera chaque semaine. Au bout de 3 semaines sans réponse, ta personne de confiance sera prévenue pour prendre de tes nouvelles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedRythm) },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.phoenXMatiere()
            ) {
                Text("Enregistrer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = theme.contentColor.copy(alpha = 0.4f))
            }
        }
    )
}

@Composable
fun RhythmOptionItem(days: Int, label: String, selected: Boolean, theme: AppThemeState, onClick: () -> Unit) {
    val accent = theme.accentColor
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
            colors = RadioButtonDefaults.colors(selectedColor = accent)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) theme.contentColor else theme.contentColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
        }
    }
}
