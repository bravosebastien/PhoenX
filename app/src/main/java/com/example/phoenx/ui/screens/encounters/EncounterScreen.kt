package com.example.phoenx.ui.screens.encounters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.theme.LocalAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncounterScreen(
    onNavigateBack: () -> Unit,
    viewModel: EncounterViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val encounters by viewModel.encounterPersons.collectAsState()
    val allPersons by viewModel.allSelectablePersons.collectAsState()
    val graphLayout by viewModel.graphLayout.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var selectedPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var viewMode by remember { mutableStateOf("LIST") } // "LIST" | "GRAPH"

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Les Rencontres", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                        Text("Ceux qui ont compté", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.5f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    // Switcher Graphe / Liste (v9.5.1)
                    IconButton(onClick = { viewMode = if (viewMode == "LIST") "GRAPH" else "LIST" }) {
                        Icon(
                            imageVector = if (viewMode == "LIST") Icons.Default.Timeline else Icons.AutoMirrored.Filled.List,
                            contentDescription = "Changer de vue",
                            tint = theme.contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    selectedPerson = null
                    showDialog = true 
                },
                containerColor = accent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (encounters.isEmpty()) {
                EmptyEncounters(modifier = Modifier.align(Alignment.Center))
            } else {
                if (viewMode == "LIST") {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val grouped = encounters.sortedBy { it.encounterAge ?: 999 }.groupBy { it.encounterAge ?: -1 }
                        
                        grouped.forEach { (age, list) ->
                            item {
                                Text(
                                    text = if (age == -1) "Âge inconnu" else "À $age ans",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accent,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(list) { person ->
                                EncounterItem(
                                    person = person,
                                    accent = accent,
                                    onClick = {
                                        selectedPerson = person
                                        showDialog = true
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // VUE GRAPHE SQUELETTE (v9.5.1 ÉTAPE A)
                    EncounterGraphRenderer(
                        layout = graphLayout,
                        onPersonClick = { person ->
                            selectedPerson = person
                            showDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        EncounterDetailsDialog(
            initialPerson = selectedPerson,
            allPersons = allPersons,
            onConfirm = { 
                viewModel.saveEncounter(it)
                showDialog = false 
            },
            onDismiss = { showDialog = false },
            onRemoveCategory = {
                viewModel.removeEncounterCategory(it)
                showDialog = false
            },
            accent = accent
        )
    }
}

@Composable
fun EncounterItem(
    person: PersonEntity,
    accent: Color,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = theme.contentColor.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameoPortrait(
                imagePath = person.imagePath,
                firstName = person.firstName,
                size = 56.dp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.firstName + (person.lastName?.let { " $it" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Handshake, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = person.linkNature ?: "Rencontre",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.6f)
                    )
                }
                if (!person.encounterLocationLabel.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = person.encounterLocationLabel!!,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            
            if (person.visibility == "PRIVATE") {
                Icon(
                    Icons.Default.Lock,
                    null,
                    tint = theme.contentColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyEncounters(modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Handshake,
            null,
            modifier = Modifier.size(64.dp),
            tint = theme.contentColor.copy(alpha = 0.1f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Aucune rencontre enregistrée.",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f)
        )
        Text(
            "Ajoute les personnes qui ont marqué ta route.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.3f)
        )
    }
}
