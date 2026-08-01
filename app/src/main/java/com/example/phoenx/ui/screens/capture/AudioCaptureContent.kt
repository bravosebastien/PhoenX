package com.example.phoenx.ui.screens.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AudioCaptureContent(
    isRecording: Boolean,
    transcript: String,
    partialText: String,
    onTranscriptChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
    recipients: List<com.example.phoenx.data.local.RecipientEntity> = emptyList(),
    selectedRecipientIds: MutableList<String>,
    visibility: String,
    onVisibilityChange: (String) -> Unit,
    // NOUVEAUTÉ v8.9.8
    notifyByEmail: Boolean = false,
    onNotifyByEmailChange: ((Boolean) -> Unit)? = null,
    // Personnes citées (v8.8)
    selectedPersons: List<com.example.phoenx.data.local.PersonEntity> = emptyList(),
    suggestedPersons: List<com.example.phoenx.data.local.PersonEntity> = emptyList(),
    onSearchPersons: (String) -> Unit = {},
    onSelectPerson: (com.example.phoenx.data.local.PersonEntity) -> Unit = {},
    onSelectMe: () -> Unit = {}, // v9.0
    onCreatePerson: (String, String?, String?, String?, String?, android.net.Uri?, String) -> Unit = { _, _, _, _, _, _, _ -> },
    onRemovePerson: (String) -> Unit = {},
    onManageCharacters: () -> Unit = {},
    // Menus déroulants (v8.9.2)
    selectedCategory: String = "Sagesse",
    onCategoryChange: (String) -> Unit = {},
    isTonaliteExpanded: Boolean = false,
    onTonaliteToggle: () -> Unit = {},
    isTiroirsExpanded: Boolean = false,
    onTiroirsToggle: () -> Unit = {},
    currentStep: Int = 1, // v9.0
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
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isRecording && transcript.isNotEmpty()) {
            Text("Donne une âme à cet enregistrement :", style = MaterialTheme.typography.labelSmall, color = theme.accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = transcript,
                onValueChange = onTranscriptChange,
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = theme.contentColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accentColor,
                    unfocusedBorderColor = theme.accentColor.copy(alpha = 0.3f),
                    focusedTextColor = theme.contentColor,
                    unfocusedTextColor = theme.contentColor
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = if (isRecording) "On t'écoute..." else if (transcript.isEmpty()) "Parle, nous écrivons pour toi" else "Continuer l'enregistrement ?",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily),
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )
        
        if (isRecording && partialText.isNotEmpty()) {
            Text(
                text = "... $partialText",
                style = MaterialTheme.typography.bodyMedium,
                color = accent.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            modifier = Modifier
                .size(140.dp)
                .scale(if (isRecording) scale else 1f)
                .shadow(if (isRecording) 20.dp else 0.dp, CircleShape, spotColor = accent)
                .clickable { if (isRecording) onStop() else onStart() },
            shape = CircleShape,
            color = if (isRecording) Error.copy(alpha = 0.2f) else accent.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (isRecording) Error else accent)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isRecording) Error else accent,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        if (isRecording) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Appuie pour arrêter", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
        } else if (transcript.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            if (currentStep == 1) {
                // v8.8 : Personnes citées (Étape 1)
                com.example.phoenx.ui.components.PersonSelector(
                    selectedPersons = selectedPersons,
                    suggestedPersons = suggestedPersons,
                    onSearch = onSearchPersons,
                    onSelect = onSelectPerson,
                    onSelectMe = onSelectMe,
                    onCreate = onCreatePerson,
                    onRemove = onRemovePerson,
                    onManageCharacters = onManageCharacters,
                    accent = accent
                )
            } else {
                // ÉTAPE 2 : HABILLAGE & DESTINATION (Audio)
                Text(
                    "HABILLAGE & DESTINATION", 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // POUR QUI (Tiroirs)
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
                        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    }
                }

                AnimatedVisibility(visible = isTiroirsExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        com.example.phoenx.ui.components.RecipientSelector(
                            recipients = recipients,
                            selectedIds = selectedRecipientIds.toList(),
                            onToggleRecipient = { id ->
                                if (selectedRecipientIds.contains(id)) selectedRecipientIds.remove(id)
                                else selectedRecipientIds.add(id)
                            },
                            visibility = visibility,
                            onVisibilityChange = onVisibilityChange,
                            accent = accent,
                            notifyByEmail = notifyByEmail,
                            onNotifyByEmailChange = onNotifyByEmailChange
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                // TONALITÉ
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
                        Text(
                            text = "QUELLE TONALITÉ ?", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                        Text(text = selectedCategory, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
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
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(24.dp))

                // CAPSULE TEMPORELLE (Ouverture programmée)
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
                } ?: "Dès maintenant"
                
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
                        Icon(Icons.Default.Event, null, tint = accent, modifier = Modifier.size(18.dp))
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
                        }
                    ) { DatePicker(state = datePickerState) }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(24.dp))

                // VERROU
                Text(
                    "PROTECTION SECRÈTE", 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = enigmaQuestion,
                    onValueChange = onEnigmaQuestionChange,
                    label = { Text("Question secrète") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = enigmaAnswer,
                    onValueChange = onEnigmaAnswerChange,
                    label = { Text("Réponse attendue") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor)
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = theme.backgroundColor)
                ) {
                    Text("Sceller ce souvenir", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
