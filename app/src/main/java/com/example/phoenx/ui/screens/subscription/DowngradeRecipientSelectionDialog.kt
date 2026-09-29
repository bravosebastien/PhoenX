package com.example.phoenx.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.phoenx.R
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * Écran 2 (LOT D) : Écran dynamique à la confirmation de rétrogradation/sortie.
 * Nomme concrètement quels destinataires inclus seront mis en pause selon la règle de tri par défaut (les plus anciens restent actifs),
 * et offre au Créateur la possibilité de modifier lui-même la sélection avant de valider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DowngradeRecipientSelectionDialog(
    currentIncludedRecipients: List<RecipientEntity>,
    targetTierName: String = "votre nouveau palier",
    targetAllowedIncludedCount: Int,
    confirmTitle: String = "Ajustement de vos Destinataires",
    customDescription: String? = null,
    onConfirmSelection: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    // Tri par défaut : les plus anciens d'abord (createdAt croissant)
    val sortedByOldest = remember(currentIncludedRecipients) {
        currentIncludedRecipients.sortedBy { it.createdAt }
    }

    // Sélection par défaut : les targetAllowedIncludedCount plus anciens
    val defaultSelectedIds = remember(sortedByOldest, targetAllowedIncludedCount) {
        sortedByOldest.take(targetAllowedIncludedCount).map { it.id }.toSet()
    }

    var selectedIds by remember { mutableStateOf(defaultSelectedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = {
            Text(
                text = confirmTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = customDescription ?: stringResource(
                        R.string.downgrade_selection_dialog_description,
                        targetTierName,
                        targetAllowedIncludedCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.8f)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Destinataires sélectionnés : ${selectedIds.size} / $targetAllowedIncludedCount",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedByOldest) { recipient ->
                        val isChecked = selectedIds.contains(recipient.id)
                        val canSelect = isChecked || selectedIds.size < targetAllowedIncludedCount

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) accent.copy(alpha = 0.08f) else theme.contentColor.copy(alpha = 0.03f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isChecked) accent else theme.contentColor.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recipient.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = theme.contentColor
                                    )
                                    Text(
                                        text = recipient.relationship,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent
                                    )
                                }

                                Checkbox(
                                    checked = isChecked,
                                    enabled = canSelect || isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked && selectedIds.size < targetAllowedIncludedCount) {
                                            selectedIds = selectedIds + recipient.id
                                        } else if (!checked) {
                                            selectedIds = selectedIds - recipient.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accent,
                                        uncheckedColor = theme.contentColor.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSelection(selectedIds) },
                enabled = selectedIds.size <= targetAllowedIncludedCount,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Valider mon choix", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    )
}
