package com.example.phoenx.ui.screens.genealogy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.domain.model.VisualGroup
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * Composant de rendu pour un groupe de co-parents (v9.4.26)
 */
@Composable
fun GroupTreeNode(
    group: VisualGroup,
    allPersons: List<PersonEntity>,
    onAddChild: (PersonEntity) -> Unit,
    onShowDetails: (PersonEntity) -> Unit,
    enabled: Boolean = true,
    accent: Color
) {
    val theme = LocalAppTheme.current

    Column(modifier = Modifier.padding(start = (if (group.level > 0) 24 else 0).dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = theme.contentColor.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                group.members.forEachIndexed { index, resolved ->
                    val personEntity = allPersons.find { it.id == resolved.id }
                    if (personEntity != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowDetails(personEntity) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CameoPortrait(
                                imagePath = resolved.photoUrl,
                                firstName = resolved.firstName,
                                size = 40.dp,
                                resolvedUrl = resolved.photoUrl // v9.4.27 : Source unique déjà résolue
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resolved.firstName + (resolved.lastName?.let { " $it" } ?: ""),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (resolved.isDeceased) theme.contentColor.copy(alpha = 0.5f) else theme.contentColor
                                )
                                if (resolved.isDeceased) {
                                    Text("Décédé(e)", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.3f))
                                }
                            }
                            if (enabled) {
                                IconButton(onClick = { onAddChild(personEntity) }) {
                                    Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        
                        if (index < group.members.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = theme.contentColor.copy(alpha = 0.05f)
                            )
                        }
                    }
                }
            }
        }

        if (group.children.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            group.children.forEach { childGroup ->
                GroupTreeNode(
                    group = childGroup,
                    allPersons = allPersons,
                    onAddChild = onAddChild,
                    onShowDetails = onShowDetails,
                    enabled = enabled,
                    accent = accent
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
