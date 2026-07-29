package com.example.phoenx.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.data.model.StandaloneMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiteraryLibraryScreen(
    navController: NavController,
    isCreatorMode: Boolean = true,
    targetCreatorId: String? = null,
    viewModel: LiteraryLibraryViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val excerpts by viewModel.excerpts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(targetCreatorId) {
        viewModel.setTargetCreator(targetCreatorId)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Bibliothèque Littéraire", color = theme.contentColor) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
                )
                // BANDEAU D'AIDE (v9.3.2)
                if (isCreatorMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = accent.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = accent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Note : Ce média sera déposé seul. Pour l'associer à un souvenir précis, utilisez l'écran de Capture.",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (isCreatorMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = accent,
                    contentColor = theme.backgroundColor
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        },
        containerColor = theme.backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (excerpts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun extrait littéraire.", color = theme.contentColor.copy(alpha = 0.4f))
                    }
                }
            }

            items(excerpts) { excerpt ->
                ExcerptCard(excerpt, theme)
            }
        }
    }

    if (showAddDialog) {
        AddExcerptDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, recipients ->
                viewModel.addExcerpt(title, content, recipients)
                showAddDialog = false
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun ExcerptCard(excerpt: StandaloneMedia, theme: com.example.phoenx.ui.theme.AppThemeState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                excerpt.title.ifEmpty { "Sans titre" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = theme.contentColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                excerpt.content,
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AddExcerptDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>) -> Unit,
    viewModel: LiteraryLibraryViewModel
) {
    val theme = LocalAppTheme.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val recipients by viewModel.recipients.collectAsState()
    val selectedIds = remember { mutableStateListOf<String>() }
    var visibility by remember { mutableStateOf("EVERYONE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text("Déposer un extrait", color = theme.contentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'ouvrage") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Extrait / Passage") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                )
                
                Text("Visibilité", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                RecipientSelector(
                    recipients = recipients,
                    selectedIds = selectedIds,
                    visibility = visibility,
                    onVisibilityChange = { visibility = it },
                    accent = theme.accentColor
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, content, selectedIds.toList()) },
                enabled = content.isNotBlank()
            ) {
                Text("Sauvegarder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
