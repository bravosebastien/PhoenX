package com.example.phoenx.ui.screens.capture

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TextCaptureContent(
    padding: PaddingValues,
    text: String,
    onTextChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    recipients: List<com.example.phoenx.data.local.RecipientEntity>,
    selectedRecipientIds: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    visibility: String,
    onVisibilityChange: (String) -> Unit,
    currentStep: Int = 1, // v9.0 : Navigation interne
    // NOUVEAUTÉ v8.9.8
    notifyByEmail: Boolean = false,
    onNotifyByEmailChange: ((Boolean) -> Unit)? = null,
    isListening: Boolean,
    onMicClick: () -> Unit,
    preselectedName: String? = null,
    galleryUri: Uri? = null,
    isComplement: Boolean = false,
    initialType: String = "TEXT", // v8.4
    // Personnes citées (v8.8)
    selectedPersons: List<com.example.phoenx.data.local.PersonEntity> = emptyList(),
    suggestedPersons: List<com.example.phoenx.data.local.PersonEntity> = emptyList(),
    onSearchPersons: (String) -> Unit = {},
    onSelectPerson: (com.example.phoenx.data.local.PersonEntity) -> Unit = {},
    onSelectMe: () -> Unit = {}, // v9.0
    onCreatePerson: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onRemovePerson: (String) -> Unit = {},
    // Menus déroulants (v8.9.2)
    isTonaliteExpanded: Boolean = false,
    onTonaliteToggle: () -> Unit = {},
    isTiroirsExpanded: Boolean = false,
    onTiroirsToggle: () -> Unit = {},
    // OPTIONS AVANCÉES (Intégrées v9.0)
    enigmaQuestion: String = "",
    onEnigmaQuestionChange: (String) -> Unit = {},
    enigmaAnswer: String = "",
    onEnigmaAnswerChange: (String) -> Unit = {},
    scheduledTimestamp: Long? = null,
    onScheduledTimestampChange: (Long?) -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (currentStep == 1) {
            val label = if (isComplement && initialType == "TEXT") "RÉDIGER TON RÉCIT"
                       else if (isComplement) "AJOUTE UN MÉDIA"
                       else "L'ÂME DU SOUVENIR"
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = accent
            )
            
            if (!isComplement) {
                Text(
                    text = "Donne un nom ou un sujet à ce souvenir. Tu l'enrichiras à l'étape suivante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { 
                        Text(
                            text = if (isComplement) "Écris tes mots ici..." else "Quel est le sujet de ce souvenir ?", 
                            style = MaterialTheme.typography.headlineSmall, 
                            color = theme.contentColor.copy(alpha = 0.3f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = theme.contentColor, 
                        fontFamily = theme.fontFamily,
                        fontSize = if (isComplement && initialType == "TEXT") 18.sp else 24.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = theme.contentColor,
                        unfocusedTextColor = theme.contentColor
                    )
                )
                
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier.align(Alignment.TopEnd).background(if (isListening) Color.Red.copy(alpha = 0.1f) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isListening) Color.Red else accent
                    )
                }
            }

            val nudgePhrase = remember { com.example.phoenx.ui.components.NudgePhrases.getRandomPhrase() }
            Text(
                text = nudgePhrase,
                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                color = theme.contentColor.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // v8.8 : Personnes citées
            com.example.phoenx.ui.components.PersonSelector(
                selectedPersons = selectedPersons,
                suggestedPersons = suggestedPersons,
                onSearch = onSearchPersons,
                onSelect = onSelectPerson,
                onSelectMe = onSelectMe,
                onCreate = onCreatePerson,
                onRemove = onRemovePerson,
                accent = accent
            )
        } else {
            // ÉTAPE 2 : HABILLAGE
            Text(
                text = "HABILLAGE & DESTINATION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
                color = accent.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Support Média
            if (galleryUri != null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 24.dp).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(model = galleryUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            } else {
                // Placeholder pour ajout de média (Simulé pour l'instant)
                Surface(
                    modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.contentColor.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = accent.copy(alpha = 0.5f))
                        Spacer(Modifier.width(12.dp))
                        Text("Ajouter une photo ou vidéo", color = theme.contentColor.copy(alpha = 0.4f), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // TONALITÉ
            Column {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTonaliteToggle() }
                        .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    color = theme.contentColor.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "QUELLE TONALITÉ ?", 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                                color = theme.contentColor.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            InfoPoint(
                                title = "L'Esprit du Souvenir",
                                content = "Cette catégorie aide l'IA à comprendre le sens profond de ton récit."
                            )
                        }
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                }
                
                AnimatedVisibility(visible = isTonaliteExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        val categories = listOf("Sagesse", "Aventure", "Secret", "Famille", "Amour", "Nostalgie", "Humour", "Leçon", "Voyage", "Quotidien", "Épreuve")
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { onCategoryChange(cat) },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.accentColor,
                                        selectedLabelColor = theme.backgroundColor
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DANS QUELS TIROIRS ?
            Column {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTiroirsToggle() }
                        .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    color = theme.contentColor.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "DANS QUELS TIROIRS ?", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                        val count = selectedRecipientIds.size
                        val label = if (visibility == "EVERYONE") "Tout le monde" else if (count == 0) "Privé" else "$count choisi(s)"
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                }

                AnimatedVisibility(visible = isTiroirsExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        RecipientSelector(
                            recipients = recipients, 
                            selectedIds = selectedRecipientIds, 
                            visibility = visibility,
                            onVisibilityChange = onVisibilityChange,
                            accent = accent,
                            notifyByEmail = notifyByEmail,
                            onNotifyByEmailChange = onNotifyByEmailChange
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4 : CAPSULE TEMPORELLE (Ouverture programmée)
            Text(
                "CAPSULE TEMPORELLE", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                color = theme.contentColor.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val dateText = scheduledTimestamp?.let {
                java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochMilli(it))
            } ?: "Pas de date (visible dès le départ)"
            
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                color = theme.contentColor.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(dateText, color = if(scheduledTimestamp != null) accent else theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            onScheduledTimestampChange(datePickerState.selectedDateMillis)
                            showDatePicker = false
                        }) { Text("Confirmer", color = accent) }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            onScheduledTimestampChange(null)
                            showDatePicker = false 
                        }) { Text("Effacer", color = theme.contentColor.copy(alpha = 0.6f)) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 5 : LE VERROU (Mode Détective)
            Text(
                "VERROUILLAGE PAR ÉNIGME", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                color = theme.contentColor.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = enigmaQuestion,
                onValueChange = onEnigmaQuestionChange,
                label = { Text("Ta question secrète") },
                placeholder = { Text("Ex: Quel est le nom de notre premier chat ?") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f), focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = enigmaAnswer,
                onValueChange = onEnigmaAnswerChange,
                label = { Text("La réponse attendue") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f), focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
