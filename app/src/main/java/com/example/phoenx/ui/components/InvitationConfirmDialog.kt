package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.phoenXMatiere

@Composable
fun InvitationConfirmDialog(
    personName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Avez-vous prévenu $personName de vive voix ?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Pour éviter de l'inquiéter, nous recommandons de le/la prévenir avant d'envoyer cette invitation.",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().height(48.dp).phoenXMatiere(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continuer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    )
}
