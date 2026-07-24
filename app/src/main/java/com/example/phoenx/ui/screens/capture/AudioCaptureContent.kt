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
    onCreatePerson: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onRemovePerson: (String) -> Unit = {},
    // Menus déroulants (v8.9.2)
    selectedCategory: String = "Sagesse",
    onCategoryChange: (String) -> Unit = {},
    isTonaliteExpanded: Boolean = false,
    onTonaliteToggle: () -> Unit = {},
    isTiroirsExpanded: Boolean = false,
    onTiroirsToggle: () -> Unit = {}
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
            
            // POUR QUI (v8.9.2 : Menu déroulant)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val count = selectedRecipientIds.size
                        val label = if (visibility == "EVERYONE") "Tout le monde" else if (count == 0) "Privé" else "$count choisi(s)"
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isTiroirsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = theme.contentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
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

            HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

            // TONALITÉ (v8.9.2 : Menu déroulant)
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
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isTonaliteExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = theme.contentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                                    selectedContainerColor = accent,
                                    selectedLabelColor = theme.backgroundColor,
                                    containerColor = theme.contentColor.copy(alpha = 0.05f),
                                    labelColor = theme.contentColor.copy(alpha = 0.8f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp, 
                                    color = if (selectedCategory == cat) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // v8.8 : Personnes citées
            com.example.phoenx.ui.components.PersonSelector(
                selectedPersons = selectedPersons,
                suggestedPersons = suggestedPersons,
                onSearch = onSearchPersons,
                onSelect = onSelectPerson,
                onCreate = onCreatePerson,
                onRemove = onRemovePerson,
                accent = accent
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.accentColor,
                    contentColor = theme.backgroundColor
                )
            ) {
                Text("Sceller ce souvenir", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
