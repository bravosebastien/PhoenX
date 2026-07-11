package com.example.phoenx.ui.screens.witness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonOutline
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
import androidx.navigation.NavController
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WitnessInviteScreen(
    navController: NavController,
    viewModel: WitnessViewModel = hiltViewModel()
) {
    val witnesses by viewModel.witnesses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val accent = LocalAccentColor.current
    val backgroundBrush = LocalBackgroundBrush.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showDialog by remember { mutableStateOf(false) }
    var witnessToDelete by remember { mutableStateOf<WitnessEntity?>(null) }

    val error by viewModel.error.collectAsState()
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundBrush),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Les Témoins", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic))
                        InfoButton(
                            title = "Les Témoins",
                            points = listOf(
                                "Invite des proches à témoigner sur toi — tu ne verras jamais ce qu'ils écrivent.",
                                "Leurs témoignages sont chiffrés et scellés jusqu'à l'activation du protocole.",
                                "C'est une mémoire à 360° : ton histoire vue par les yeux de ceux qui t'aiment.",
                                "Chaque témoin reçoit un lien unique par email.",
                                "Tu peux choisir d'autoriser la lecture de ton vivant pour certains témoins."
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = accent,
                contentColor = BackgroundPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                "Invite des proches à raconter un souvenir sur toi. Leurs mots enrichiront ton héritage.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && witnesses.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
                } else if (witnesses.isEmpty()) {
                    Text(
                        "Aucun témoin pour l'instant.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = TextTertiary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(witnesses) { witness ->
                            WitnessCard(
                                witness = witness,
                                onDelete = { witnessToDelete = witness }
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            InviteWitnessDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, email, allowRead ->
                    viewModel.inviteWitness(name, email, allowRead, "Ton proche")
                    showDialog = false
                }
            )
        }

        if (witnessToDelete != null) {
            AlertDialog(
                onDismissRequest = { witnessToDelete = null },
                containerColor = BackgroundSecondary,
                title = { Text("Supprimer ce témoin ?", color = TextPrimary) },
                text = { Text("Veux-tu vraiment annuler l'invitation de ${witnessToDelete?.name} ?", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        witnessToDelete?.let { viewModel.deleteWitness(it.id) }
                        witnessToDelete = null
                    }) {
                        Text("Supprimer", color = Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { witnessToDelete = null }) {
                        Text("Annuler", color = TextPrimary)
                    }
                }
            )
        }
    }
}

@Composable
fun WitnessCard(witness: WitnessEntity, onDelete: () -> Unit) {
    val accent = LocalAccentColor.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E23)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E35))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PersonOutline, null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(witness.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(witness.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            val statusColor = if (witness.status == "submitted") Success else Warning
            val statusText = if (witness.status == "submitted") "Reçu" else "Invité"

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = statusColor
                    )
                }
                
                if (witness.status != "submitted") {
                    IconButton(onClick = onDelete, modifier = Modifier.padding(top = 4.dp).size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InviteWitnessDialog(onDismiss: () -> Unit, onConfirm: (String, String, Boolean) -> Unit) {
    val accent = LocalAccentColor.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var allowRead by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        title = { Text("Inviter un témoin", color = TextPrimary, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = allowRead,
                        onCheckedChange = { allowRead = it },
                        colors = CheckboxDefaults.colors(checkedColor = accent)
                    )
                    Text("M'autoriser à lire ce témoignage de mon vivant", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, allowRead) },
                enabled = name.isNotBlank() && email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Envoyer l'invitation", color = BackgroundPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextPrimary)
            }
        }
    )
}
