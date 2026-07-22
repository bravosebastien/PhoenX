package com.example.phoenx.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    themeViewModel: com.example.phoenx.ui.theme.ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // v8.9.0 : Thème Global
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundId by themeViewModel.globalBackgroundId.collectAsState()
    val fontId by themeViewModel.globalFontId.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var isAppearingExpanded by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mon Profil", color = theme.contentColor, fontFamily = theme.fontFamily) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
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
                    color = accent.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
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
                        text = uiState.displayName.ifEmpty { "Utilisateur" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.contentColor,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { 
                        newName = uiState.displayName
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
                    text = uiState.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.6f)
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

                // --- ACCORDÉON APPARENCE (v8.9.0 Global) ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAppearingExpanded = !isAppearingExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, null, tint = accent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Apparence & Style", style = MaterialTheme.typography.bodyLarge, color = theme.contentColor)
                            }
                            Icon(
                                if (isAppearingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = theme.contentColor.copy(alpha = 0.4f)
                            )
                        }

                        if (isAppearingExpanded) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            com.example.phoenx.ui.components.GlobalThemeSelector(
                                currentBackgroundId = backgroundId,
                                currentFontId = fontId,
                                onThemeChange = { bg, font -> themeViewModel.setGlobalTheme(bg, font) }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            TextButton(
                                onClick = { themeViewModel.resetToDefaults() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Réinitialiser les réglages par défaut")
                            }
                        }
                    }
                }

                // --- ARBRE GÉNÉALOGIQUE ---
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    onClick = { /* TODO: Navigation vers Arbre */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = theme.contentColor.copy(alpha = 0.05f)
                    ),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountTree, null, tint = accent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ma Généalogie", style = MaterialTheme.typography.bodyLarge, color = theme.contentColor)
                            Text("Visualise ton cercle de confiance", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.4f))
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // --- ZONE DE DANGER ---
                Text("ZONE DE DANGER", style = MaterialTheme.typography.labelSmall, color = Error.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { /* TODO: Dialogue de suspension */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Suspendre mon compte")
                }

                Spacer(modifier = Modifier.height(48.dp))

                // BOUTON DE RETOUR
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.contentColor.copy(alpha = 0.1f),
                        contentColor = theme.contentColor
                    )
                ) {
                    Text("Retour aux réglages", color = theme.contentColor)
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Modifier mon nom", color = theme.contentColor) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom d'usage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = theme.contentColor.copy(alpha = 0.4f),
                        focusedLabelColor = accent,
                        cursorColor = accent,
                        unfocusedLabelColor = theme.contentColor.copy(alpha = 0.6f),
                        focusedTextColor = theme.contentColor,
                        unfocusedTextColor = theme.contentColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateDisplayName(newName)
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
