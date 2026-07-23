package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonSelector(
    selectedPersons: List<PersonEntity>,
    suggestedPersons: List<PersonEntity>,
    onSearch: (String) -> Unit,
    onSelect: (PersonEntity) -> Unit,
    onCreate: (firstName: String, lastName: String?, relation: String?, distType: String?, distValue: String?) -> Unit,
    onRemove: (String) -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var duplicateNameDialog by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Personnes mentionnées",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Tags des personnes sélectionnées
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedPersons.forEach { person ->
                InputChip(
                    selected = true,
                    onClick = { onRemove(person.id) },
                    label = { Text(person.firstName) },
                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.2f),
                        selectedLabelColor = accent
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Champ de recherche
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            placeholder = { Text("Qui est présent ?", fontSize = 14.sp, color = theme.contentColor.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.PersonAdd, null, tint = accent) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = {
                        val existing = suggestedPersons.find { it.firstName.equals(query, ignoreCase = true) }
                        if (existing != null) {
                            duplicateNameDialog = query
                        } else {
                            showCreateDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Add, null, tint = accent)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                focusedTextColor = theme.contentColor,
                unfocusedTextColor = theme.contentColor
            )
        )

        // Suggestions
        if (suggestedPersons.isNotEmpty() && query.isNotBlank()) {
            Surface(
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = theme.contentColor.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                tonalElevation = 2.dp
            ) {
                Column {
                    suggestedPersons.forEach { person ->
                        ListItem(
                            headlineContent = { Text(person.firstName + (person.lastName?.let { " $it" } ?: ""), color = theme.contentColor, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(person.relationship ?: "Proche", color = theme.contentColor.copy(alpha = 0.6f)) },
                            leadingContent = {
                                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = accent.copy(alpha = 0.1f)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(person.firstName.take(1), color = accent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { 
                                onSelect(person)
                                query = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog de création
    if (showCreateDialog) {
        CreatePersonDialog(
            initialFirstName = query,
            onDismiss = { showCreateDialog = false },
            onConfirm = { f, l, r, dt, dv ->
                onCreate(f, l, r, dt, dv)
                showCreateDialog = false
                query = ""
            },
            accent = accent
        )
    }

    // Dialog si nom en double
    if (duplicateNameDialog != null) {
        AlertDialog(
            onDismissRequest = { duplicateNameDialog = null },
            title = { Text("Nom déjà existant", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Il y a déjà un(e) ${duplicateNameDialog} dans votre liste. Voulez-vous utiliser la personne existante ou en créer une nouvelle ?", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { 
                    showCreateDialog = true
                    duplicateNameDialog = null 
                }) { Text("Créer un nouveau", color = accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    val p = suggestedPersons.first { it.firstName.equals(duplicateNameDialog, ignoreCase = true) }
                    onSelect(p)
                    duplicateNameDialog = null
                    query = ""
                }) { Text("Utiliser l'existant", color = theme.contentColor) }
            },
            containerColor = theme.backgroundColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePersonDialog(
    initialFirstName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, String?, String?) -> Unit,
    accent: Color
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var distinctionType by remember { mutableStateOf("nom_famille") }
    var distinctionValue by remember { mutableStateOf("") }

    val distTypes = listOf(
        "nom_famille" to "Nom de famille",
        "surnom" to "Surnom",
        "ville" to "Ville",
        "autre" to "Autre"
    )

    val theme = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.backgroundColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Nouvelle Personne", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Lien (ex: fils, collègue)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                Spacer(Modifier.height(16.dp))

                Text("Pour les différencier (si besoin)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = distTypes.find { it.first == distinctionType }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type de distinction") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        distTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.second, color = theme.contentColor) },
                                onClick = {
                                    distinctionType = type.first
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(textColor = theme.contentColor)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = distinctionValue,
                    onValueChange = { distinctionValue = it },
                    label = { Text("Valeur (ex: Lyon, Le Grand...)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
                    Button(
                        onClick = { onConfirm(firstName, if(lastName.isBlank()) null else lastName, if(relationship.isBlank()) null else relationship, distinctionType, if(distinctionValue.isBlank()) null else distinctionValue) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = firstName.isNotBlank(),
                        modifier = Modifier.phoenXMatiere()
                    ) { Text("Créer", color = theme.backgroundColor, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
