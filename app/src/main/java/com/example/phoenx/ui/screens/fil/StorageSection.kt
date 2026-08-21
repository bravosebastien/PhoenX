package com.example.phoenx.ui.screens.fil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.components.CompartmentSelector
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.AppThemeState

/**
 * Section RANGEMENT (v9.4.27)
 * Permet de classer le souvenir dans les tiroirs de la Bibliothèque.
 */
@Composable
fun StorageSection(
    entry: OfflineEntry,
    onUpdateCompartments: (List<String>) -> Unit,
    theme: AppThemeState,
    accent: Color,
    isReadOnly: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "RANGEMENT", 
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp), 
            color = Color.Black, 
            letterSpacing = 2.sp
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = theme.contentColor.copy(alpha = 0.04f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isReadOnly) { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory, null, tint = accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "DANS QUELS TIROIRS ?", 
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                            color = theme.contentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        InfoPoint(
                            title = "Le rangement par Tiroirs",
                            content = "Votre souvenir est automatiquement rangé selon les médias qu'il contient (Photos, Vidéothèque ou Discothèque).\n\nVous pouvez toujours ajuster ce rangement manuellement pour faire apparaître ce souvenir dans d'autres salles de votre Bibliothèque. Note : ce classement n'influence pas l'écriture de votre Livre de Vie, qui reste gérée par l'option dédiée."
                        )
                    }
                    if (!isReadOnly) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, 
                            contentDescription = null, 
                            tint = theme.contentColor.copy(alpha = 0.2f), 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded || isReadOnly) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        val currentCompartments = entry.compartmentIds.trim(',').split(",").filter { it.isNotBlank() }
                        CompartmentSelector(
                            selectedIds = currentCompartments,
                            onToggleCompartment = { id ->
                                val newList = currentCompartments.toMutableList()
                                if (newList.contains(id)) newList.remove(id)
                                else newList.add(id)
                                onUpdateCompartments(newList)
                            },
                            accent = accent,
                            enabled = !isReadOnly
                        )
                    }
                }
            }
        }
    }
}
