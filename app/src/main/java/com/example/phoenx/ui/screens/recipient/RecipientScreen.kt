package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var showAddDialog by remember { mutableStateOf(false) }
    var recipientToDelete by remember { mutableStateOf<RecipientEntity?>(null) }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mon Cercle", 
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.contentColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = accent,
                contentColor = theme.backgroundColor
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            when (val state = uiState) {
                is RecipientUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
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
                                    onDelete = { recipientToDelete = recipient },
                                    onClick = { onNavigateToDetail(recipient.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (recipientToDelete != null) {
            AlertDialog(
                onDismissRequest = { recipientToDelete = null },
                containerColor = theme.backgroundColor,
                title = { Text("Supprimer ce proche ?", color = theme.contentColor, fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold) },
                text = { Text("Veux-tu vraiment retirer ${recipientToDelete?.name} de ton Cercle de Confiance ? Cette personne n'aura plus accès à ton héritage.", color = theme.contentColor.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            recipientToDelete?.let { viewModel.deleteRecipient(it) }
                            recipientToDelete = null
                        }
                    ) {
                        Text("Supprimer", color = Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipientToDelete = null }) {
                        Text("Annuler", color = theme.contentColor)
                    }
                }
            )
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
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = accent.copy(alpha = 0.1f),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Person, null, tint = accent, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recipient.name, 
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold
                    ), 
                    color = theme.contentColor
                )
                Text(recipient.relationship, style = MaterialTheme.typography.labelSmall, color = accent)
                Text(recipient.email, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Error.copy(alpha = 0.7f))
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
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var showInvitationConfirm by remember { mutableStateOf(false) }

    if (!showInvitationConfirm) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = theme.backgroundColor,
            title = { Text("Ajouter un proche", color = theme.contentColor, fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text("Lien (ex: Fils, Épouse...)") },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInvitationConfirm = true },
                    enabled = name.isNotEmpty() && email.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Suivant", color = theme.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = { showInvitationConfirm = false },
            containerColor = theme.backgroundColor,
            title = { Text("Confirmer l'invitation", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
            text = {
                Column {
                    Text(
                        "Un email va être envoyé à $name pour l'informer qu'il/elle fait partie des personnes que tu as choisies dans PHOEN-X. Veux-tu continuer ?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = theme.contentColor.copy(alpha = 0.05f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("APERÇU DE L'EMAIL", style = MaterialTheme.typography.labelSmall, color = accent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "$name vient d'être ajouté(e) par ton proche.\nTu fais partie des personnes choisies pour recevoir son héritage PHOEN-X.",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirm(name, email, relationship) },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Envoyer l'invitation", color = theme.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvitationConfirm = false }) {
                    Text("Retour", color = theme.contentColor)
                }
            }
        )
    }
}
