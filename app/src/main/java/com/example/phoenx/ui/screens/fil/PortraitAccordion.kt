package com.example.phoenx.ui.screens.fil

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * PortraitAccordion — Affichage expansible des questions/réponses du Portrait d'un Proche.
 * v8.9.8 : Extraction modulaire et thémage dynamique.
 */
@Composable
fun PortraitAccordion(
    items: List<MemoryDetailViewModel.PortraitItem>, 
    accent: Color,
    onEditItem: (String) -> Unit
) {
    var isMasterExpanded by remember { mutableStateOf(false) }
    val theme = LocalAppTheme.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // LE BANDEAU MAÎTRE (v8.9.8 : Liseré Accent 0.5f)
        Card(
            onClick = { isMasterExpanded = !isMasterExpanded },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoStories, null, tint = accent)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "LES RÉPONSES AU PORTRAIT (${items.size})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isMasterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = accent
                )
            }
        }

        // LE CONTENU DÉROULANT
        AnimatedVisibility(
            visible = isMasterExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEachIndexed { index, item ->
                    var expanded by remember { mutableStateOf(index == 0) }
                    
                    Card(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (expanded) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = accent.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall, color = accent)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = item.question.ifBlank { "Pensée libre" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (expanded) accent else theme.contentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                if (item.id != null) {
                                    IconButton(
                                        onClick = { onEditItem(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = theme.contentColor.copy(alpha = 0.2f)
                                )
                            }
                            
                            if (expanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = item.answer,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, lineHeight = 26.sp),
                                    color = theme.contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
