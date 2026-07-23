package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@Composable
fun RecoveryReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = {
            Text(
                text = "Ta phrase de récupération est-elle en sécurité ?",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                color = theme.contentColor
            )
        },
        text = {
            Text(
                text = "PHOEN-X utilise une technologie de chiffrement de bout en bout. Tes souvenirs sont protégés par une clé privée unique que nous ne possédons pas.\n\nCes 12 mots sont l'unique sauvegarde de cette clé. Si tu changes de téléphone ou si l'application est désinstallée, sans ces mots, l'accès à ta vie sera DÉFINITIVEMENT verrouillé. Ni toi, ni nous ne pourrons rien faire. C'est ton unique bouclier contre l'oubli définitif.",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f),
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.phoenXMatiere()
            ) {
                Text("Vérifier mes 12 mots", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("C'est bon, je les ai", color = theme.contentColor.copy(alpha = 0.4f))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryPhraseBottomSheet(
    phrase: List<String>,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.contentColor.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ces mots sont affichés uniquement sur ton téléphone. Ne les photographiez pas. Ne les partagez pas en ligne. Rangez-les dans un endroit physique sûr.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(phrase) { index, word ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = theme.contentColor.copy(alpha = 0.3f)
                            )
                            Text(
                                text = word,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = theme.fontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = theme.contentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Fermer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
