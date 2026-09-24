package com.example.phoenx.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import com.example.phoenx.R
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
    hint: String,
    onHintChange: (String) -> Unit,
    autoUnlockDays: Int?,
    onAutoUnlockDaysChange: (Int?) -> Unit,
    answerType: String = "WORD",
    onAnswerTypeChange: (String) -> Unit = {},
    expectedWordCount: Int? = 1,
    onExpectedWordCountChange: (Int?) -> Unit = {},
    theme: AppThemeState,
    accent: Color,
    isReadOnly: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // EN-TÊTE ACCORDÉON (v9.4.27)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .border(1.dp, if (isEnabled) accent.copy(alpha = 0.3f) else theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            color = if (isEnabled) accent.copy(alpha = 0.05f) else theme.contentColor.copy(alpha = 0.02f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.Lock else Icons.Default.LockOpen, 
                        null, 
                        tint = if (isEnabled) accent else theme.contentColor.copy(alpha = 0.3f), 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isEnabled) "EXPÉRIENCE : SOUVENIR SCELLÉ" else "Verrouiller avec une énigme",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isEnabled) accent else theme.contentColor
                    )
                    // v12.7 : point d'information pour expliquer ce qu'est un "Souvenir Scellé"
                    InfoButton(
                        title = stringResource(R.string.enigma_form_info_title),
                        points = listOf(
                            stringResource(R.string.enigma_form_info_point1),
                            stringResource(R.string.enigma_form_info_point2),
                            stringResource(R.string.enigma_form_info_point3),
                            stringResource(R.string.enigma_form_info_point4)
                        )
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = theme.contentColor.copy(alpha = 0.3f)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Surface(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                color = theme.contentColor.copy(alpha = 0.02f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // INTERRUPTEUR D'ACTIVATION
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Activer le verrouillage", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { if (!isReadOnly) onToggleEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accent),
                            enabled = !isReadOnly
                        )
                    }

                    Text(
                        text = "Le Coffre-Fort n'est pas un rempart de sécurité technique supplémentaire, mais une expérience de complicité : vos proches devront deviner la réponse pour débloquer ce moment.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                    
                    if (isEnabled) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // TYPE DE RÉPONSE (Mots / Nombre)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = answerType == "WORD",
                                onClick = { if (!isReadOnly) onAnswerTypeChange("WORD") },
                                label = { Text("Mots") },
                                enabled = !isReadOnly
                            )
                            FilterChip(
                                selected = answerType == "NUMBER",
                                onClick = { if (!isReadOnly) onAnswerTypeChange("NUMBER") },
                                label = { Text("Nombre") },
                                enabled = !isReadOnly
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
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

                        // RÉPONSE
                        Text("LA RÉPONSE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = answer,
                            onValueChange = onAnswerChange,
                            placeholder = { 
                                Text(
                                    if (answerType == "NUMBER") "Saisir le chiffre ou nombre" else "Choisir la réponse attendue",
                                    color = theme.contentColor.copy(alpha = 0.3f),
                                    fontSize = 12.sp
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isReadOnly,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = if (answerType == "NUMBER") androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text
                            ),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f))
                        )

                        if (answerType == "WORD") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Mots attendus :", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                                Spacer(Modifier.width(12.dp))
                                var wordCountStr by remember(expectedWordCount) { mutableStateOf((expectedWordCount ?: 1).toString()) }
                                OutlinedTextField(
                                    value = wordCountStr,
                                    onValueChange = { str ->
                                        if (str.length <= 2) {
                                            val filtered = str.filter { c -> c.isDigit() }
                                            wordCountStr = filtered
                                            onExpectedWordCountChange(filtered.toIntOrNull() ?: 1)
                                        }
                                    },
                                    modifier = Modifier.width(70.dp),
                                    enabled = !isReadOnly,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f))
                                )
                            }
                        }

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

                        // DÉBLOCAGE AUTOMATIQUE
                        Card(
                            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("DÉBLOCAGE AUTOMATIQUE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                                
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
}
