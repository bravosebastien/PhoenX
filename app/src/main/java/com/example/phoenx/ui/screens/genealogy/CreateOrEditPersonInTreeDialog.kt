package com.example.phoenx.ui.screens.genealogy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.ui.theme.LocalAppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditPersonInTreeDialog(
    initialPerson: PersonEntity? = null,
    initialParents: List<PersonEntity> = emptyList(),
    allPersons: List<PersonEntity>,
    onConfirm: (firstName: String, lastName: String?, parentIds: List<String>, existingPersonId: String?) -> Unit,
    onDismiss: () -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    var firstName by remember { mutableStateOf(initialPerson?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialPerson?.lastName ?: "") }
    var selectedExistingPersonId by remember { mutableStateOf<String?>(null) }
    
    val selectedParentIds = remember { mutableStateListOf<String>().apply { 
        if (initialPerson != null) {
            addAll(initialPerson.parentIds.trim(',').split(",").filter { it.isNotBlank() })
        } else {
            addAll(initialParents.map { it.id })
        }
    } }
    val initialParentIds = remember { selectedParentIds.toList() }

    // Suggestions de parents pour la personne (existantes)
    var parentQuery by remember { mutableStateOf("") }
    val suggestedParents = if (parentQuery.isBlank()) emptyList() else allPersons.filter { 
        it.firstName.contains(parentQuery, ignoreCase = true) && 
        it.id != initialPerson?.id && 
        it.id != selectedExistingPersonId &&
        !selectedParentIds.contains(it.id)
    }

    // Suggestions de personnes existantes pour LIAISON (doublons)
    val matchingExistingPersons = if (initialPerson == null && selectedExistingPersonId == null && firstName.length >= 2) {
        allPersons.filter { 
            it.firstName.contains(firstName, ignoreCase = true) || 
            (it.lastName?.contains(firstName, ignoreCase = true) == true)
        }.take(5)
    } else emptyList()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.backgroundColor,
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        initialPerson != null -> "Modifier les liens"
                        selectedExistingPersonId != null -> "Lier une personne"
                        else -> "Nouvelle Personne"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                
                if (selectedExistingPersonId != null) {
                    Surface(
                        modifier = Modifier.padding(top = 8.dp),
                        color = accent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, null, tint = accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Profil existant sélectionné", style = MaterialTheme.typography.labelSmall, color = accent)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Close, 
                                null, 
                                tint = accent, 
                                modifier = Modifier.size(14.dp).clickable { 
                                    selectedExistingPersonId = null 
                                    selectedParentIds.clear()
                                    selectedParentIds.addAll(initialParentIds)
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { 
                        firstName = it
                        if (selectedExistingPersonId != null) selectedExistingPersonId = null
                    },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor),
                    enabled = selectedExistingPersonId == null
                )
                
                if (matchingExistingPersons.isNotEmpty()) {
                    Text(
                        "CETTE PERSONNE EXISTE-T-ELLE DÉJÀ ?",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = accent,
                        modifier = Modifier.align(Alignment.Start).padding(top = 12.dp, bottom = 4.dp)
                    )
                    matchingExistingPersons.forEach { p ->
                        ListItem(
                            headlineContent = { Text(p.firstName + (p.lastName?.let { " $it" } ?: ""), fontWeight = FontWeight.Bold, color = theme.contentColor) },
                            supportingContent = { Text(p.relationship ?: "Déjà dans l'arbre", color = theme.contentColor.copy(alpha = 0.6f)) },
                            leadingContent = { 
                                Icon(Icons.Default.Link, null, tint = accent)
                            },
                            modifier = Modifier.clickable {
                                selectedExistingPersonId = p.id
                                firstName = p.firstName
                                lastName = p.lastName ?: ""
                                val merged = (initialParentIds + p.parentIds.trim(',').split(",").filter { it.isNotBlank() }).distinct()
                                selectedParentIds.clear()
                                selectedParentIds.addAll(merged)
                            },
                            colors = ListItemDefaults.colors(containerColor = theme.contentColor.copy(alpha = 0.05f))
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { 
                        lastName = it
                        if (selectedExistingPersonId != null) selectedExistingPersonId = null
                    },
                    label = { Text("Nom (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor),
                    enabled = selectedExistingPersonId == null
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    "PARENT(S) DE CETTE PERSONNE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedParentIds.forEach { pid ->
                        val p = allPersons.find { it.id == pid }
                        if (p != null) {
                            InputChip(
                                selected = true,
                                onClick = { selectedParentIds.remove(pid) },
                                label = { Text(p.firstName) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                                colors = InputChipDefaults.inputChipColors(selectedContainerColor = accent.copy(alpha = 0.2f))
                            )
                        }
                    }
                }

                if (selectedParentIds.size < 2) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = parentQuery,
                        onValueChange = { parentQuery = it },
                        placeholder = { Text("Rechercher un parent...", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )

                    if (suggestedParents.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = theme.contentColor.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column {
                                suggestedParents.forEach { p ->
                                    ListItem(
                                        headlineContent = { Text(p.firstName, fontWeight = FontWeight.Bold, color = theme.contentColor) },
                                        supportingContent = { Text(p.relationship ?: "Proche", color = theme.contentColor.copy(alpha = 0.6f)) },
                                        modifier = Modifier.clickable {
                                            selectedParentIds.add(p.id)
                                            parentQuery = ""
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
                    Button(
                        onClick = { onConfirm(firstName, lastName.ifBlank { null }, selectedParentIds.toList(), selectedExistingPersonId) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = firstName.isNotBlank()
                    ) { 
                        Text(
                            text = when {
                                initialPerson != null -> "Enregistrer"
                                selectedExistingPersonId != null -> "Confirmer le lien"
                                else -> "Créer"
                            }, 
                            color = theme.backgroundColor, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
