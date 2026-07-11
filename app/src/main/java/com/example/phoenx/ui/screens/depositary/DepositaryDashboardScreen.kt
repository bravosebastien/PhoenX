package com.example.phoenx.ui.screens.depositary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositaryDashboardScreen(
    creatorId: String,
    onNavigateToActivation: (String) -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToInfo: () -> Unit,
    viewModel: DepositaryViewModel = hiltViewModel()
) {
    android.util.Log.d("PHOENX_DEBUG", "DepositaryDashboardScreen rendu avec creatorId=$creatorId")
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(creatorId) {
        viewModel.loadCreatorStatus(creatorId)
    }

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
                        Text("Tableau de bord Dépositaire", color = TextPrimary)
                        InfoButton(
                            title = "Tableau de Bord Dépositaire",
                            points = listOf(
                                "Tu es la personne de confiance choisie par ton proche.",
                                "Le point vert indique que tout va bien et qu'il confirme régulièrement sa présence.",
                                "Le point orange signale qu'il n'a pas confirmé depuis un moment — pas d'inquiétude.",
                                "Le point rouge pulsant signifie qu'il faut essayer de le contacter.",
                                "Tu n'activeras le protocole que si tu confirmes son absence définitive."
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StatusCard(
                        name = uiState.creatorName,
                        missedCycles = uiState.missedCycles,
                        days = uiState.daysSinceLastCheckIn,
                        onAction = { onNavigateToActivation(creatorId) }
                    )
                }

                item {
                    DepositarySection(
                        title = "MON RÔLE",
                        icon = Icons.Default.Security,
                        onClick = onNavigateToOnboarding
                    )
                }
                
                item {
                    DepositarySection(
                        title = "NOTIFICATIONS",
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToNotifications
                    )
                }

                item {
                    DepositarySection(
                        title = "MES INFORMATIONS",
                        icon = Icons.Default.Person,
                        onClick = onNavigateToInfo
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    name: String,
    missedCycles: Int,
    days: Int,
    onAction: () -> Unit
) {
    val statusColor = when {
        missedCycles >= 3 -> Error
        missedCycles > 0 -> Warning
        else -> Success
    }

    val statusText = when {
        missedCycles >= 3 -> "$name n'a pas confirmé sa présence depuis $days jours."
        missedCycles > 0 -> "$name n'a pas confirmé depuis $days jours. Pas d'inquiétude."
        else -> "$name a confirmé sa présence il y a $days jours."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceCard,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(statusColor, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusText,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            if (missedCycles >= 3) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onAction) {
                    Text("Voir les options", color = AccentPrimary)
                }
            }
        }
    }
}

@Composable
fun DepositarySection(
    title: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = SurfaceCard.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = TextSecondary, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
        }
    }
}
