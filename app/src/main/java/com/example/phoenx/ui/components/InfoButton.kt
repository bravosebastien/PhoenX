package com.example.phoenx.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.phoenx.ui.theme.LocalAccentColor

@Composable
fun InfoButton(
    title: String,
    points: List<String>,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val accent = LocalAccentColor.current

    // Le bouton ℹ️ discret
    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.size(36.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = Color(0xFF2E2E35),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "i",
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
    }

    // La fenêtre d'information
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A1F)
                ),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Titre
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFFF2EDE8)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ligne dorée fine
                    HorizontalDivider(
                        color = accent.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Points d'explication
                    points.forEach { point ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                color = accent,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(
                                    end = 8.dp, top = 2.dp
                                )
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF9B9590),
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bouton fermer
                    TextButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Compris",
                            color = accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
