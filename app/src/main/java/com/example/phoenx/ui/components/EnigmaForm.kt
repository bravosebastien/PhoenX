package com.example.phoenx.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

/**
 * EnigmaForm (v9.4.27) — Formulaire unifié pour le verrouillage par énigme.
 * Utilisé dans l'Atelier classique et le mode Étape par Étape.
 */
@Composable
fun EnigmaForm(
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    question: String,
    onQuestionChange: (String) -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    hasExistingAnswer: Boolean = false, // v9.4.27
    hint: String,
    onHintChange: (String) -> Unit,
    autoUnlockDays: Int?,
    onAutoUnlockDaysChange: (Int?) -> Unit,
    isUltimateSecret: Boolean,
    onUltimateSecretToggle: (Boolean) -> Unit,
    theme: AppThemeState,
    accent: Color,
    isReadOnly: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // INTERRUPTEUR PRINCIPAL
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Fingerprint, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Verrouiller avec une énigme",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { if (!isReadOnly) onToggleEnabled(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = accent),
                enabled = !isReadOnly
            )
        }

        Text(
            text = "Ce souvenir ne sera visible par vos destinataires qu'après qu'ils aient répondu correctement à la question que vous choisissez. Vous pouvez ajouter un indice, et définir un délai après lequel il se débloque automatiquement — sauf si vous choisissez d'en faire un Secret Ultime, qui ne se débloque jamais tout seul.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.6f),
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        AnimatedVisibility(visible = isEnabled) {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                // QUESTION
                Text("LA QUESTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = onQuestionChange,
                    placeholder = { Text("Ex : Quel était le nom de notre premier chien ?", color = theme.contentColor.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isReadOnly,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // RÉPONSE (v9.4.27 : Support Placeholder)
                Text("LA RÉPONSE (invisible après saisie)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    placeholder = { 
                        Text(
                            if (hasExistingAnswer) "Une réponse est déjà définie (laisser vide pour conserver)" else "Choisir la réponse attendue",
                            color = theme.contentColor.copy(alpha = 0.3f),
                            fontSize = 12.sp
                        ) 
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isReadOnly,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // INDICE
                Text("INDICE (Optionnel, après 3 échecs)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hint,
                    onValueChange = onHintChange,
                    placeholder = { Text("Ex : C'est le nom d'un animal...", color = theme.contentColor.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isReadOnly,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // SECRET ULTIME VS DÉLAI
                Card(
                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Secret Ultime", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                            Switch(
                                checked = isUltimateSecret,
                                onCheckedChange = { if (!isReadOnly) onUltimateSecretToggle(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = accent),
                                enabled = !isReadOnly
                            )
                        }
                        
                        if (isUltimateSecret) {
                            Text(
                                "Ce secret ne se débloquera JAMAIS tout seul. Seule la réponse permettra de l'ouvrir.",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Text("DÉBLOCAGE AUTOMATIQUE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.padding(top = 16.dp))
                            
                            val currentDays = autoUnlockDays ?: 30
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { if (!isReadOnly) onAutoUnlockDaysChange((currentDays - 1).coerceAtLeast(1)) },
                                    enabled = !isReadOnly && currentDays > 1
                                ) {
                                    Icon(Icons.Default.Remove, null, tint = accent)
                                }
                                
                                Text(
                                    text = "$currentDays jours",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = theme.contentColor,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                
                                IconButton(
                                    onClick = { if (!isReadOnly) onAutoUnlockDaysChange((currentDays + 1).coerceAtMost(365)) },
                                    enabled = !isReadOnly && currentDays < 365
                                ) {
                                    Icon(Icons.Default.Add, null, tint = accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
