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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        title = {
            Text(
                text = "Ta phrase de récupération est-elle en sécurité ?",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "PHOEN-X utilise une technologie de chiffrement de bout en bout. Tes souvenirs sont protégés par une clé privée unique que nous ne possédons pas.\n\nCes 12 mots sont l'unique sauvegarde de cette clé. Si tu changes de téléphone ou si l'application est désinstallée, sans ces mots, l'accès à ta vie sera DÉFINITIVEMENT verrouillé. Ni toi, ni nous ne pourrons rien faire. C'est ton unique bouclier contre l'oubli définitif.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Vérifier mes 12 mots", color = BackgroundPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("C'est bon, je les ai", color = TextTertiary)
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary.copy(alpha = 0.3f)) }
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
                color = TextSecondary,
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E35)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color(0xFF5C5855)
                            )
                            Text(
                                text = word,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFF2EDE8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Fermer", color = BackgroundPrimary)
            }
        }
    }
}
