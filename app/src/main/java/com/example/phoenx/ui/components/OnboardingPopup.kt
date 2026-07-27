package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch

/**
 * OnboardingPopup (v9.2.2) — Affiche une aide contextuelle à la première ouverture.
 * S'inspire des contenus InfoPoint existants.
 */
@Composable
fun OnboardingPopup(
    pageKey: String,
    title: String,
    contentPoints: List<String>,
    preferenceManager: com.example.phoenx.data.preferences.PreferenceManager
) {
    val isDismissedBySystem by preferenceManager.isPageOnboardingDismissed(pageKey).collectAsState(initial = true)
    var isVisibleLocally by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    // On ne montre que si le système ne l'a pas déjà marqué comme "ne plus afficher"
    LaunchedEffect(isDismissedBySystem) {
        if (!isDismissedBySystem) {
            isVisibleLocally = true
        }
    }

    if (isVisibleLocally) {
        AlertDialog(
            onDismissRequest = { isVisibleLocally = false },
            containerColor = theme.backgroundColor,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = theme.contentColor
                    )
                    IconButton(onClick = { isVisibleLocally = false }) {
                        Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.4f))
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    contentPoints.forEach { point ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", color = accent, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.contentColor.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                            colors = CheckboxDefaults.colors(checkedColor = accent)
                        )
                        Text(
                            "Ne plus afficher cette aide",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dontShowAgain) {
                            scope.launch {
                                preferenceManager.dismissPageOnboardingPermanent(pageKey)
                            }
                        }
                        isVisibleLocally = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("J'ai compris", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
