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
    onCreatePerson: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onRemovePerson: (String) -> Unit = {},
    // Menus déroulants (v8.9.2)
    isTonaliteExpanded: Boolean = false,
    onTonaliteToggle: () -> Unit = {},
    isTiroirsExpanded: Boolean = false,
    onTiroirsToggle: () -> Unit = {}
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
        val label = if (isComplement && initialType == "TEXT") "RÉDIGER TON RÉCIT"
                   else if (isComplement) "AJOUTE UN MÉDIA"
                   else "ÉTAPE 1 : L'ÉTINCELLE"
        
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

        if (galleryUri != null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 16.dp).clip(RoundedCornerShape(12.dp))) {
                AsyncImage(model = galleryUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
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
                    fontSize = if (isComplement && initialType == "TEXT") 18.sp else 24.sp // Taille récit vs titre
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

        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 24.dp))

        // TONALITÉ (v8.9.2 : Menu déroulant)
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
                            content = "Cette catégorie aide l'IA à comprendre le sens profond de ton récit. Elle influence la rédaction de ton Livre de Vie."
                        )
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
                                label = { 
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Medium
                                        )
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = theme.contentColor.copy(alpha = 0.05f),
                                    labelColor = theme.contentColor.copy(alpha = 0.6f),
                                    selectedContainerColor = theme.accentColor,
                                    selectedLabelColor = theme.backgroundColor
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
        }

        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

        // POUR QUI (v8.9.2 : Menu déroulant)
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
        }
    }
}
