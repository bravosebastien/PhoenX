package com.example.phoenx.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.BackgroundPrimary
import com.example.phoenx.ui.theme.SurfaceCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipientSelector(
    recipients: List<RecipientEntity>,
    selectedIds: MutableList<String>,
    visibility: String,
    onVisibilityChange: (String) -> Unit,
    accent: Color,
    containerColor: Color? = null,
    // NOUVEAUTÉ v8.9.8 (Contrôle de Notification)
    notifyByEmail: Boolean = false,
    onNotifyByEmailChange: ((Boolean) -> Unit)? = null
) {
    val theme = com.example.phoenx.ui.theme.LocalAppTheme.current
    var expanded by remember { mutableStateOf(false) }
    val isEveryone = visibility == "EVERYONE"

    Column {
        // LE BANDEAU DÉROULANT (v8.6.3)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            color = containerColor ?: SurfaceCard.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (expanded) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEveryone) Icons.Default.Public else Icons.Default.People,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEveryone) "Partagé avec tout le monde" else "Destinataires sélectionnés",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    if (!isEveryone) {
                        Text(
                            text = "${selectedIds.size} personne(s) autorisée(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = theme.contentColor.copy(alpha = 0.4f)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
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
                        selectedLabelColor = theme.backgroundColor,
                        selectedLeadingIconColor = theme.backgroundColor
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (!isEveryone) {
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
                                    selectedLabelColor = theme.backgroundColor,
                                    selectedLeadingIconColor = theme.backgroundColor
                                )
                            )
                        }
                    }
                } else {
                    // v8.6.3 : Si "Tout le monde" est actif, on affiche tout de même les noms
                    // pour confirmer visuellement l'inclusion, mais en mode lecture seule
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recipients.forEach { recipient ->
                            FilterChip(
                                selected = true,
                                onClick = { 
                                    // Cliquer sur un nom individuel désactive "Tout le monde" 
                                    // et ne garde que ce destinataire
                                    onVisibilityChange("RESTRICTED")
                                    selectedIds.clear()
                                    selectedIds.add(recipient.id)
                                },
                                label = { Text(recipient.name) },
                                leadingIcon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.5f), 
                                    selectedLabelColor = theme.backgroundColor
                                )
                            )
                        }
                    }
                }

                // NOUVEAUTÉ v8.9.8 : Interrupteur de notification
                if (onNotifyByEmailChange != null && (selectedIds.isNotEmpty() || isEveryone)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = theme.contentColor.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val label = if (isEveryone) "Prévenir le Cercle par email" 
                                       else if (selectedIds.size == 1) {
                                           val name = recipients.find { it.id == selectedIds.first() }?.name ?: "le destinataire"
                                           "Prévenir $name par email"
                                       } else "Prévenir les destinataires par email"
                            
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.8f)
                            )
                            Switch(
                                checked = notifyByEmail,
                                onCheckedChange = onNotifyByEmailChange,
                                modifier = Modifier.scale(0.7f),
                                colors = SwitchDefaults.colors(checkedThumbColor = accent)
                            )
                        }
                    }
                }
            }
        }
    }
}
