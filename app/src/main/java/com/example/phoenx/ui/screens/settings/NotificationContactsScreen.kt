package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.NotificationContactEntity
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationContactsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationContactsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var showAddDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<NotificationContactEntity?>(null) }

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
                        Text(
                            "Contacts à prévenir",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = theme.fontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = theme.contentColor
                        )
                        InfoButton(
                            title = "Contacts à prévenir",
                            points = listOf(
                                "Ces personnes seront informées de ton départ par email.",
                                "Elles n'auront aucun accès à ton héritage PHOEN-X.",
                                "Maximum 2 contacts — nom et email suffisent.",
                                "Un email sobre : '[Prénom] nous a quittés.'",
                                "Aucun lien, aucune invitation — juste l'information."
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
                    }
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
                .padding(24.dp)
        ) {
            Text(
                text = "Ces personnes recevront un email sobre au moment de ton départ. Elles n'auront pas accès à ton héritage — juste une information, avec dignité.",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (contacts.isEmpty() && !isLoading) {
                    Text(
                        "Aucun contact ajouté pour l'instant.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = theme.fontFamily,
                            fontStyle = FontStyle.Italic
                        ),
                        color = theme.contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(contacts) { contact ->
                            ContactCard(
                                contact = contact,
                                onDelete = { contactToDelete = contact }
                            )
                        }
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    disabledContainerColor = accent.copy(alpha = 0.3f)
                ),
                enabled = contacts.size < 2 && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (contacts.size >= 2) {
                    Text("Maximum 2 contacts atteint.", color = theme.backgroundColor)
                } else {
                    Text("+ Ajouter un contact", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showAddDialog) {
            AddContactDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, email, relationship ->
                    viewModel.addContact(name, email, relationship)
                    showAddDialog = false
                }
            )
        }

        if (contactToDelete != null) {
            AlertDialog(
                onDismissRequest = { contactToDelete = null },
                containerColor = theme.backgroundColor,
                title = { Text("Supprimer ce contact ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
                text = { Text("Veux-tu vraiment retirer ${contactToDelete?.name} de tes contacts à prévenir ?", color = theme.contentColor.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            contactToDelete?.let { viewModel.deleteContact(it.id) }
                            contactToDelete = null
                        }
                    ) {
                        Text("Supprimer", color = Error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { contactToDelete = null }) {
                        Text("Annuler", color = theme.contentColor)
                    }
                }
            )
        }
    }
}

@Composable
fun ContactCard(contact: NotificationContactEntity, onDelete: () -> Unit) {
    val theme = LocalAppTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name, 
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold
                    ), 
                    color = theme.contentColor
                )
                Text(contact.email, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                if (contact.relationship.isNotBlank()) {
                    Text(
                        contact.relationship,
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = theme.accentColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Error.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }

    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val canAdd = name.isNotBlank() && isEmailValid

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = {
            Text(
                "Nouveau contact",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = theme.fontFamily, 
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor
            )
        },
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
                    isError = email.isNotBlank() && !isEmailValid,
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
                    label = { Text("Lien (ex: Fils, Ami...)") },
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
                onClick = { onConfirm(name, email, relationship) },
                enabled = canAdd,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Ajouter", color = theme.backgroundColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = theme.contentColor)
            }
        }
    )
}
