package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.theme.*

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
    mainViewModel: MainViewModel
) {
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Réglages", style = MaterialTheme.typography.labelLarge) },
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

            Spacer(modifier = Modifier.height(32.dp))

            Text("ACCESSIBILITÉ", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Mode Vocal Total",
                subtitle = "Navigation par la voix",
                icon = Icons.Default.RecordVoiceOver,
                onClick = onNavigateToAccessibility
            )
        }
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
