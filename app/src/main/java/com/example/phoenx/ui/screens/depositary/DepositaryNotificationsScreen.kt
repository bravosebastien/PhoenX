package com.example.phoenx.ui.screens.depositary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositaryNotificationsScreen(
    onNavigateBack: () -> Unit
) {
    val notifications = listOf(
        "Activation confirmée. Délai de 72h en cours.",
        "Alerte de silence : pas de réponse depuis 30 jours.",
        "Silence résolu : présence confirmée.",
        "Rappel : Tu es le Dépositaire de [Prénom]."
    )

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(notifications) { note ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceCard,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = note,
                        color = TextPrimary,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
