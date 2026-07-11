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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
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
    val accent by themeViewModel.accentColor.collectAsState()
    val backgroundColor by themeViewModel.backgroundColor.collectAsState()
    val backgroundStyle by themeViewModel.backgroundStyle.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var isAppearingExpanded by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val colors = listOf(
        Color(0xFFC97B3A), Color(0xFFFFD700), Color(0xFFFFBF00), Color(0xFFFF9800),
        Color(0xFFFF4500), Color(0xFFFF4E11), Color(0xFFF44336), Color(0xFFE91E63),
        Color(0xFFFF00FF), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
        Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00FFFF), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF2ECC71), Color(0xFF4CAF50), Color(0xFF8BC34A),
        Color(0xFFC0C0C0), Color(0xFF607D8B)
    )

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
                title = { Text("Mon Profil", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
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
                        color = TextPrimary,
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
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Ce nom sera visible par les personnes que vous invitez (Dépositaires, Témoins, Destinataires).",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // --- ACCORDÉON APPARENCE ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.5f)),
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
                                Text("Apparence & Style", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            }
                            Icon(
                                if (isAppearingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = TextTertiary
                            )
                        }

                        if (isAppearingExpanded) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // 1. COULEUR DE FOND
                            Text("COULEUR DE FOND", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            colors.chunked(6).forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowColors.forEach { color ->
                                        val isSelected = color == backgroundColor
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(color, CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { themeViewModel.setBackground(color) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 2. COULEUR D'ÉCRITURE
                            Text("COULEUR D'ÉCRITURE", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            colors.chunked(6).forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowColors.forEach { color ->
                                        val isSelected = color == accent
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(color, CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { themeViewModel.setAccent(color) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 3. STYLE DE FOND
                            Text("STYLE DE FOND", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("RADIAL", "LINEAR", "SOLID").forEach { style ->
                                    val isSelected = backgroundStyle == style
                                    val label = when(style) {
                                        "RADIAL" -> "Profondeur (Radial)"
                                        "LINEAR" -> "Élégance (Linéaire)"
                                        "SOLID" -> "Sobre (Uni)"
                                        else -> style
                                    }
                                    Surface(
                                        onClick = { themeViewModel.setBackgroundStyle(style) },
                                        color = if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accent) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(label, color = if (isSelected) TextPrimary else TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- ARBRE GÉNÉALOGIQUE ---
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    onClick = { /* TODO: Navigation vers Arbre */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.3f)),
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
                            Text("Ma Généalogie", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("Visualise ton cercle de confiance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextTertiary)
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
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                ) {
                    Text("Retour aux réglages", color = TextPrimary)
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = BackgroundSecondary,
            title = { Text("Modifier mon nom", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom d'usage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = TextTertiary,
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary
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
                    Text("Enregistrer", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler", color = TextPrimary)
                }
            }
        )
    }
}
