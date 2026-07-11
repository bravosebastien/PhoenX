package com.example.phoenx.ui.screens.pact

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.PactEntity
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PactScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: PactViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Le Pacte", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showInviteDialog = true },
                containerColor = AccentPrimary,
                contentColor = BackgroundPrimary
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentPrimary)
            } else if (uiState.pacts.isEmpty()) {
                EmptyPactContent(modifier = Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Tes histoires partagées",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(uiState.pacts) { pact ->
                        PactCard(pact) {
                            onNavigateToDetail(pact.id)
                        }
                    }
                }
            }
        }

        if (showInviteDialog) {
            InvitePactDialog(
                onDismiss = { showInviteDialog = false },
                onConfirm = { name, email ->
                    viewModel.invitePartner(name, email)
                    showInviteDialog = false
                }
            )
        }
    }
}

@Composable
fun PactCard(pact: PactEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = AccentPrimary.copy(alpha = 0.1f),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Handshake, null, tint = AccentPrimary, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pact.partnerName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Pacte avec ${pact.partnerEmail}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AccentPrimary)
        }
    }
}

@Composable
fun EmptyPactContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Handshake, null, modifier = Modifier.size(64.dp), tint = TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Aucun pacte en cours.", style = MaterialTheme.typography.bodyLarge, color = TextTertiary)
        Text("Invite un proche à raconter votre histoire commune.", style = MaterialTheme.typography.bodySmall, color = TextTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
    }
}

@Composable
fun InvitePactDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        title = { Text("Initier un Pacte", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Deux points de vue pour une même histoire. Chacun écrit de son côté, et les versions seront révélées ensemble plus tard.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du partenaire") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email du partenaire") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email) },
                enabled = name.isNotBlank() && email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Envoyer l'invitation", color = BackgroundPrimary)
            }
        }
    )
}
