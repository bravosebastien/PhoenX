package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.LocalAppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivingLinkDialog(
    recipients: List<RecipientEntity>,
    onDismiss: () -> Unit,
    onConfirm: (recipientId: String, scheduledAt: Long?) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    var selectedRecipientId by remember { mutableStateOf("") }
    var isScheduled by remember { mutableStateOf(false) }
    var scheduledDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text("Transmettre un Lien Vivant", color = theme.contentColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    "Envoyez ce souvenir directement à l'un de vos proches, sans attendre la transmission de votre héritage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.7f)
                )

                // CHOIX DU DESTINATAIRE
                Column {
                    Text("À QUI ?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    Spacer(Modifier.height(8.dp))
                    
                    // Liste simplifiée pour le dialogue
                    recipients.forEach { recipient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRecipientId == recipient.linkedUid,
                                onClick = { selectedRecipientId = recipient.linkedUid ?: "" },
                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                            )
                            Text(recipient.name, color = theme.contentColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // MODE D'ENVOI
                Column {
                    Text("QUAND ?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    Spacer(Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dès maintenant", style = MaterialTheme.typography.bodyMedium, color = if(!isScheduled) theme.contentColor else theme.contentColor.copy(alpha = 0.4f))
                        Switch(
                            checked = isScheduled,
                            onCheckedChange = { isScheduled = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            colors = SwitchDefaults.colors(checkedThumbColor = accent)
                        )
                        Text("Programmer", style = MaterialTheme.typography.bodyMedium, color = if(isScheduled) theme.contentColor else theme.contentColor.copy(alpha = 0.4f))
                    }

                    if (isScheduled) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                        ) {
                            Icon(Icons.Default.Event, null)
                            Spacer(Modifier.width(8.dp))
                            val txt = scheduledDate?.let { SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(it)) } ?: "Choisir une date"
                            Text(txt)
                        }
                    }
                }

                // TEXTE EXPLICATIF (v9.4.27)
                if (selectedRecipientId.isNotEmpty()) {
                    val recipientName = recipients.find { it.linkedUid == selectedRecipientId }?.name?.split(" ")?.firstOrNull() ?: "votre proche"
                    Text(
                        text = "Grâce au Lien Vivant, ce souvenir sera reçu par $recipientName pendant que vous êtes là pour en parler ensemble — indépendamment de la transmission générale de votre héritage.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.5f),
                        lineHeight = 16.sp
                    )
                }
                
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                scheduledDate = datePickerState.selectedDateMillis
                                showDatePicker = false
                            }) { Text("Confirmer", color = accent) }
                        }
                    ) { DatePicker(state = datePickerState) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedRecipientId, if(isScheduled) scheduledDate else null) },
                enabled = selectedRecipientId.isNotEmpty() && (!isScheduled || scheduledDate != null),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Valider la transmission", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor) }
        }
    )
}
