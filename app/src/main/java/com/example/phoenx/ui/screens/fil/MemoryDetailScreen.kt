package com.example.phoenx.ui.screens.fil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.CompartmentIds
import com.example.phoenx.ui.screens.capture.RecipientSelector
import com.example.phoenx.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailScreen(
    entryId: String,
    onNavigateBack: () -> Unit,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsState()
    val content by viewModel.decryptedContent.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val accent = LocalAccentColor.current

    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("Édition du souvenir", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (entry == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // CONTENU DU SOUVENIR (Papier/Parchemin)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)), // Couleur papier
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF2C2C2E),
                                lineHeight = 28.sp
                            )
                        )
                    }
                }

                // DATE RÉELLE (MemoryDate)
                Column {
                    Text("QUAND ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var showDatePicker by remember { mutableStateOf(false) }
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = entry!!.memoryDate ?: entry!!.createdAt)

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp), tint = accent)
                        Spacer(modifier = Modifier.width(12.dp))
                        val dateText = entry!!.memoryDate?.let { 
                            SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(it))
                        } ?: "Ajouter une date précise"
                        Text(dateText, color = TextPrimary)
                    }

                    if (showDatePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.updateMemoryDate(datePickerState.selectedDateMillis)
                                    showDatePicker = false
                                }) { Text("Confirmer", color = accent) }
                            }
                        ) { DatePicker(state = datePickerState) }
                    }
                }

                // TIROIRS / COMPARTIMENTS
                Column {
                    Text("DANS QUELS TIROIRS ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val currentCompartments = entry!!.compartmentIds.trim(',').split(",").filter { it.isNotBlank() }
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompartmentIds.ALL.forEach { id ->
                            val isSelected = currentCompartments.contains(id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newList = if (isSelected) currentCompartments - id else currentCompartments + id
                                    viewModel.updateCompartments(newList)
                                },
                                label = { Text(CompartmentIds.getLabel(id)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = BackgroundPrimary
                                )
                            )
                        }
                    }
                }

                // CATÉGORIE ÉMOTIONNELLE
                Column {
                    Text("QUELLE ÉMOTION ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val categories = listOf("Sagesse", "Aventure", "Secret", "Famille", "Amour")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = entry!!.emotionalCategory == cat,
                                onClick = { viewModel.updateCategory(cat) },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = BackgroundPrimary
                                )
                            )
                        }
                    }
                }

                // DESTINATAIRES
                Column {
                    Text("POUR QUI ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val selectedRecipientIds = remember(entry!!.recipientIds) {
                        mutableStateListOf<String>().apply {
                            addAll(entry!!.recipientIds.split(",").filter { it.isNotBlank() })
                        }
                    }

                    RecipientSelector(
                        recipients = recipients,
                        selectedIds = selectedRecipientIds,
                        accent = accent
                    )
                    
                    // On observe les changements du selector pour updater le VM
                    // Note: RecipientSelector modifie la liste en place, on ajoute un effet pour sync
                    LaunchedEffect(selectedRecipientIds.size) {
                        if (selectedRecipientIds.toList().joinToString(",") != entry!!.recipientIds) {
                            viewModel.updateRecipients(selectedRecipientIds.toList())
                        }
                    }
                }

                // LIEU (Affichage simple)
                Column {
                    Text("OÙ ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = SurfaceCard.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = entry!!.locationName ?: "Lieu non défini",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (entry!!.locationName != null) TextPrimary else TextTertiary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { /* TODO: Modification de lieu */ }) {
                                Icon(Icons.Default.Edit, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
