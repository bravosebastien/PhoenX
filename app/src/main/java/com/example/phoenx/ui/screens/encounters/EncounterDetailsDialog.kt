package com.example.phoenx.ui.screens.encounters

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EncounterDetailsDialog(
    initialPerson: PersonEntity? = null,
    allPersons: List<PersonEntity>,
    onConfirm: (PersonEntity) -> Unit,
    onDismiss: () -> Unit,
    onRemoveCategory: (PersonEntity) -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    
    // État local du formulaire
    var firstName by remember { mutableStateOf(initialPerson?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialPerson?.lastName ?: "") }
    var biography by remember { mutableStateOf(initialPerson?.biography ?: "") }
    var encounterAge by remember { mutableStateOf(initialPerson?.encounterAge?.toString() ?: "") }
    var linkNature by remember { mutableStateOf(initialPerson?.linkNature ?: "") }
    var linkStatus by remember { mutableStateOf(initialPerson?.linkStatus ?: "PRESENT") }
    var locationLabel by remember { mutableStateOf(initialPerson?.encounterLocationLabel ?: "") }
    var introducedById by remember { mutableStateOf(initialPerson?.introducedById) }
    var isPrivate by remember { mutableStateOf(initialPerson?.visibility == "PRIVATE") }

    // Recherche pour "Présenté par"
    var query by remember { mutableStateOf("") }
    val suggestedIntroducers = if (query.isBlank()) emptyList() else allPersons.filter { 
        it.firstName.contains(query, ignoreCase = true) && it.id != initialPerson?.id
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = theme.backgroundColor,
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialPerson == null) "Nouvelle Rencontre" else "Modifier la Rencontre",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.4f))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Identité
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Nom (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )

                Spacer(Modifier.height(24.dp))

                // Ce qu'il/elle m'a apporté (Bio)
                OutlinedTextField(
                    value = biography,
                    onValueChange = { biography = it },
                    label = { Text("Ce qu'il ou elle m'a apporté") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )

                Spacer(Modifier.height(24.dp))

                // Détails de la rencontre
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = encounterAge,
                        onValueChange = { if (it.all { char -> char.isDigit() }) encounterAge = it },
                        label = { Text("Âge") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                    OutlinedTextField(
                        value = locationLabel,
                        onValueChange = { locationLabel = it },
                        label = { Text("Lieu") },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Nature du lien
                Text("NATURE DU LIEN", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                val natures = listOf("Ami", "Mentor", "Collègue", "Amour", "Compagnon", "Voisin")
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    natures.forEach { nature ->
                        FilterChip(
                            selected = linkNature == nature,
                            onClick = { linkNature = nature },
                            label = { Text(nature) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.2f), selectedLabelColor = accent)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // État du lien
                Text("ÉTAT DU LIEN", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusMap = mapOf("PRESENT" to "Présent", "LOST" to "Perdu de vue", "PASSED" to "Disparu")
                    statusMap.forEach { (key, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = linkStatus == key,
                            onClick = { linkStatus = key },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.2f), selectedLabelColor = accent)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Présenté par
                Text("PRÉSENTÉ(E) PAR", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                
                if (introducedById != null) {
                    val introducer = allPersons.find { it.id == introducedById }
                    if (introducer != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = theme.contentColor.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (introducer.categories.contains("FAMILY")) Icons.Default.Home else Icons.Default.Handshake,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(introducer.firstName, fontWeight = FontWeight.Bold, color = theme.contentColor, modifier = Modifier.weight(1f))
                                IconButton(onClick = { introducedById = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Rechercher une personne...", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )

                    if (suggestedIntroducers.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = theme.contentColor.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column {
                                suggestedIntroducers.take(3).forEach { p ->
                                    ListItem(
                                        headlineContent = { Text(p.firstName, fontWeight = FontWeight.Bold, color = theme.contentColor) },
                                        leadingContent = { 
                                            Icon(
                                                imageVector = if (p.categories.contains("FAMILY")) Icons.Default.Home else Icons.Default.Handshake,
                                                contentDescription = null,
                                                tint = accent.copy(alpha = 0.6f)
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            introducedById = p.id
                                            query = ""
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Confidentialité
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gardé pour moi", fontWeight = FontWeight.Bold, color = theme.contentColor)
                        Text("Invisible pour les héritiers", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent)
                    )
                }

                Spacer(Modifier.height(40.dp))

                // Boutons d'action
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val newCategories = if (initialPerson != null) {
                                (initialPerson.categories.split(",") + "ENCOUNTER").distinct().joinToString(",")
                            } else ",ENCOUNTER,"

                            val personToSave = (initialPerson ?: PersonEntity(firstName = "")).copy(
                                firstName = firstName,
                                lastName = lastName.ifBlank { null },
                                biography = biography,
                                encounterAge = encounterAge.toIntOrNull(),
                                linkNature = linkNature.ifBlank { null },
                                linkStatus = linkStatus,
                                encounterLocationLabel = locationLabel.ifBlank { null },
                                introducedById = introducedById,
                                visibility = if (isPrivate) "PRIVATE" else "PUBLIC",
                                categories = newCategories
                            )
                            onConfirm(personToSave)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = firstName.isNotBlank()
                    ) {
                        Text(if (initialPerson == null) "Créer la rencontre" else "Enregistrer les modifications", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (initialPerson != null) {
                        TextButton(
                            onClick = { onRemoveCategory(initialPerson) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retirer des Rencontres", color = Error)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
