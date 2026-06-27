package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: RecipientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Mon Cercle", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentPrimary,
                contentColor = BackgroundPrimary
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(BackgroundSecondary, BackgroundPrimary), radius = 2000f)
        )) {
            when (val state = uiState) {
                is RecipientUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is RecipientUiState.Success -> {
                    if (state.recipients.isEmpty()) {
                        EmptyRecipients(modifier = Modifier.padding(padding))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.recipients) { recipient ->
                                RecipientCard(
                                    recipient = recipient,
                                    onDelete = { viewModel.deleteRecipient(recipient) },
                                    onClick = { onNavigateToDetail(recipient.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddRecipientDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, email, rel ->
                    viewModel.addRecipient(name, email, rel)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun RecipientCard(recipient: RecipientEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = AccentPrimary.copy(alpha = 0.1f),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Person, null, tint = AccentPrimary, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(recipient.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(recipient.relationship, style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                Text(recipient.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = TextTertiary.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun EmptyRecipients(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ton cercle est vide.", color = TextTertiary, style = MaterialTheme.typography.bodyLarge)
        Text("Ajoute les personnes qui recevront ton héritage.", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AddRecipientDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        title = { Text("Ajouter un proche", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Lien (ex: Fils, Épouse...)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, relationship) },
                enabled = name.isNotEmpty() && email.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Ajouter", color = BackgroundPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextSecondary)
            }
        }
    )
}
