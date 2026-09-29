package com.example.phoenx.ui.screens.subscription

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.phoenx.R
import com.example.phoenx.data.subscription.BillingTierInfo
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * ÉCRAN 1 (LOT D) : Confirmation de souscription affiché au moment de la confirmation d'achat.
 * Intègre les textes exacts spécifiés pour PRESTIGE (avec volet "En savoir plus" dépliable)
 * et pour ESSENCE / LIGNÉE (format dynamique avec nom de palier et quota).
 */
@Composable
fun SubscriptionConfirmationDialog(
    targetTier: BillingTierInfo,
    priceFormatted: String,
    onConfirmPurchase: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var isExpanded by remember { mutableStateOf(false) }

    val isPrestige = targetTier.key == "PRESTIGE"
    val includedRecipients = targetTier.limits["recipients"] ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = {
            Text(
                text = "Confirmation — ${targetTier.nameFr}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPrestige) {
                    Text(
                        text = stringResource(R.string.subscription_confirm_dialog_prestige_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.85f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "En savoir plus",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = accent
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = accent.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = stringResource(R.string.subscription_confirm_dialog_prestige_more_text),
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(
                            R.string.subscription_confirm_dialog_generic_text,
                            targetTier.nameFr,
                            includedRecipients.toInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.85f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPurchase,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(
                    text = "Confirmer ($priceFormatted)",
                    color = theme.backgroundColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    )
}
