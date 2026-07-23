package com.example.phoenx.ui.screens.depositary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositaryInfoScreen(
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit,
    mainViewModel: com.example.phoenx.ui.MainViewModel,
    viewModel: DepositaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Mes Informations", color = theme.contentColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AVATAR
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = accent, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // IDENTITÉ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.personalName.ifEmpty { "Utilisateur" },
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = theme.fontFamily),
                    color = theme.contentColor,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { 
                    newName = uiState.personalName
                    showEditDialog = true 
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier le nom",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Text(
                text = uiState.personalEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ce nom sera visible par les personnes que vous invitez (Dépositaires, Témoins, Destinataires).",
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // RÔLE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = Success)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Rôle : Gardien de Confiance", color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text("Actif pour ${uiState.creatorName}", color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DESCRIPTION DU RÔLE
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "En tant que Dépositaire, vous êtes le dernier rempart de l'héritage de votre proche. Vous seul avez le pouvoir de confirmer son absence définitive pour sceller la transmission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // BOUTON DE RETOUR
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Retour au tableau de bord", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOUTON DE DÉCONNEXION
            TextButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Se déconnecter", color = Error)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Se déconnecter ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Es-tu sûr de vouloir fermer ta session Dépositaire ?", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    mainViewModel.logout()
                    onLogoutSuccess()
                    showLogoutDialog = false
                }) {
                    Text("Déconnexion", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Modifier mon nom", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom d'usage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                        focusedLabelColor = accent,
                        cursorColor = accent,
                        focusedTextColor = theme.contentColor,
                        unfocusedTextColor = theme.contentColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateMyDisplayName(newName)
                        showEditDialog = false
                    }
                }) {
                    Text("Enregistrer", color = accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }
}
