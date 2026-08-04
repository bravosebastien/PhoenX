package com.example.phoenx.ui.screens.characters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
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
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.components.OnboardingPopup
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    onNavigateBack: () -> Unit,
    onEditCharacter: (String) -> Unit,
    onAddCharacter: () -> Unit,
    selectionMode: Boolean = false,
    onPersonSelected: ((com.example.phoenx.data.local.PersonEntity) -> Unit)? = null,
    viewModel: CharactersViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    OnboardingPopup(
        pageKey = "characters",
        title = "Les Personnages",
        contentPoints = listOf(
            "Gère ici la liste des personnes qui peuplent ton histoire.",
            "Ajoute les personnes qui te sont chères pour enrichir ton récit."
        ),
        preferenceManager = themeViewModel.preferenceManager
    )

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (selectionMode) "Choisir un personnage" else "Mes Personnages",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCharacter,
                containerColor = accent,
                contentColor = theme.backgroundColor
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is CharactersUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
                }
                is CharactersUiState.Success -> {
                    if (state.characters.isEmpty()) {
                        EmptyCharacters(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.characters) { character ->
                                CharacterItem(
                                    character = character,
                                    onClick = { 
                                        if (selectionMode && onPersonSelected != null) {
                                            onPersonSelected(character.person)
                                            onNavigateBack()
                                        } else {
                                            onEditCharacter(character.person.id) 
                                        }
                                    },
                                    onEditClick = if (selectionMode) { { onEditCharacter(character.person.id) } } else null,
                                    accent = accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterItem(
    character: CharacterWithStats,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    accent: Color
) {
    val theme = LocalAppTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameoPortrait(
                imagePath = character.person.imagePath,
                firstName = character.person.firstName,
                size = 48.dp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.person.firstName + (character.person.lastName?.let { " $it" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Text(
                    text = character.person.relationship ?: "Proche",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            
            if (onEditClick == null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${character.appearanceCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    Text(
                        text = if (character.appearanceCount > 1) "souvenirs" else "souvenir",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = theme.contentColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Éditer",
                        tint = theme.contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCharacters(modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Aucun personnage encore cité.",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f)
        )
        Text(
            "Ajoute les personnes qui peuplent ton histoire.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.3f)
        )
    }
}
