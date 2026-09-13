package com.example.phoenx.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.favorites_title), style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
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
                onClick = { showAddDialog = true },
                containerColor = accent,
                contentColor = theme.backgroundColor
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is FavoritesUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
                is FavoritesUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (state.tasteMap.isNotEmpty()) {
                            item {
                                TasteMapCard(state.tasteMap, theme)
                            }
                        }

                        if (state.items.isEmpty()) {
                            item { EmptyFavorites(theme = theme) }
                        } else {
                            items(state.items) { item ->
                                FavoriteCard(item, theme)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddFavoriteDialog(
                onDismiss = { showAddDialog = false },
                theme = theme,
                onConfirm = { cat, title, why ->
                    viewModel.saveFavorite(cat, title, why)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TasteMapCard(content: String, theme: AppThemeState) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier.fillMaxWidth().phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.favorites_taste_map_title), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent, letterSpacing = 2.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(content, style = MaterialTheme.typography.bodySmall.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold), color = theme.contentColor)
        }
    }
}

@Composable
fun FavoriteCard(item: FavoriteItem, theme: AppThemeState) {
    val accent = theme.accentColor
    val icon = when(item.category) {
        stringResource(R.string.favorites_cat_books) -> Icons.Default.AutoStories
        stringResource(R.string.favorites_cat_movies) -> Icons.Default.Theaters
        stringResource(R.string.favorites_cat_music) -> Icons.Default.MusicNote
        else -> Icons.Default.Public
    }

    Card(
        modifier = Modifier.fillMaxWidth().phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = accent.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                Text(item.category, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent, letterSpacing = 1.sp)
                if (item.why.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.why, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = theme.contentColor.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun EmptyFavorites(modifier: Modifier = Modifier, theme: AppThemeState) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.favorites_empty_title), color = theme.contentColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        Text(stringResource(R.string.favorites_empty_subtitle), color = theme.contentColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFavoriteDialog(onDismiss: () -> Unit, theme: AppThemeState, onConfirm: (String, String, String) -> Unit) {
    val accent = theme.accentColor
    var title by remember { mutableStateOf("") }
    var why by remember { mutableStateOf("") }
    val catBooks = stringResource(R.string.favorites_cat_books)
    val catMovies = stringResource(R.string.favorites_cat_movies)
    val catMusic = stringResource(R.string.favorites_cat_music)
    val catTravels = stringResource(R.string.favorites_cat_travels)
    var category by remember { mutableStateOf(catBooks) }
    val categories = listOf(catBooks, catMovies, catMusic, catTravels)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text(stringResource(R.string.favorites_dialog_add_title), style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Catégorie
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent,
                                selectedLabelColor = theme.backgroundColor,
                                containerColor = theme.contentColor.copy(alpha = 0.05f),
                                labelColor = theme.contentColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.favorites_label_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                        focusedTextColor = theme.contentColor,
                        unfocusedTextColor = theme.contentColor
                    )
                )
                
                OutlinedTextField(
                    value = why,
                    onValueChange = { why = it },
                    label = { Text(stringResource(R.string.favorites_label_why)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                        focusedTextColor = theme.contentColor,
                        unfocusedTextColor = theme.contentColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(category, title, why) },
                enabled = title.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.phoenXMatiere()
            ) {
                Text(stringResource(R.string.favorites_button_add), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.favorites_button_cancel), color = theme.contentColor)
            }
        }
    )
}
