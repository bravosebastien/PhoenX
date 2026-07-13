package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.BackgroundPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipientSelector(
    recipients: List<RecipientEntity>,
    selectedIds: MutableList<String>,
    visibility: String,
    onVisibilityChange: (String) -> Unit,
    accent: Color
) {
    val isEveryone = visibility == "EVERYONE"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option "Tout le monde"
            FilterChip(
                selected = isEveryone,
                onClick = { 
                    if (isEveryone) onVisibilityChange("RESTRICTED")
                    else onVisibilityChange("EVERYONE")
                },
                label = { Text("Tout le monde") },
                leadingIcon = { Icon(Icons.Default.Public, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = BackgroundPrimary,
                    selectedLeadingIconColor = BackgroundPrimary
                )
            )

            if (!isEveryone) {
                TextButton(
                    onClick = {
                        if (selectedIds.size == recipients.size) selectedIds.clear()
                        else {
                            selectedIds.clear()
                            selectedIds.addAll(recipients.map { it.id })
                        }
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    val allSelected = selectedIds.size == recipients.size && recipients.isNotEmpty()
                    Text(
                        text = if (allSelected) "Tout désélectionner" else "Ajouter tous les destinataires", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = accent
                    )
                }
            }
        }

        if (!isEveryone) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipients.forEach { recipient ->
                    val isSelected = selectedIds.contains(recipient.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selectedIds.remove(recipient.id)
                            else selectedIds.add(recipient.id)
                        },
                        label = { Text(recipient.name) },
                        leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent, 
                            selectedLabelColor = BackgroundPrimary,
                            selectedLeadingIconColor = BackgroundPrimary
                        )
                    )
                }
            }
        }
    }
}
