package com.example.phoenx.ui.screens.genealogy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
    onConfirm: (firstName: String, lastName: String?, parentIds: List<String>) -> Unit,
    onDismiss: () -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    var firstName by remember { mutableStateOf(initialPerson?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialPerson?.lastName ?: "") }
    
    val selectedParentIds = remember { mutableStateListOf<String>().apply { 
        if (initialPerson != null) {
            addAll(initialPerson.parentIds.trim(',').split(",").filter { it.isNotBlank() })
        } else {
            addAll(initialParents.map { it.id })
        }
    } }

    var query by remember { mutableStateOf("") }
    val suggestedParents = if (query.isBlank()) emptyList() else allPersons.filter { 
        it.firstName.contains(query, ignoreCase = true) && 
        it.id != initialPerson?.id && 
        !selectedParentIds.contains(it.id)
    }

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
                    text = if (initialPerson == null) "Nouvelle Personne" else "Modifier les liens",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Nom (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
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
                        value = query,
                        onValueChange = { query = it },
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
                                            query = ""
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
                        onClick = { onConfirm(firstName, lastName.ifBlank { null }, selectedParentIds.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = firstName.isNotBlank()
                    ) { 
                        Text(if (initialPerson == null) "Créer" else "Enregistrer", color = theme.backgroundColor, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}
