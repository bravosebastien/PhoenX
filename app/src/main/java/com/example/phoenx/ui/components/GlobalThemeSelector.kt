package com.example.phoenx.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.screens.book.BookThemeOptions
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.TextPrimary
import com.example.phoenx.ui.theme.TextSecondary
import com.example.phoenx.ui.theme.TextTertiary

@Composable
fun GlobalThemeSelector(
    currentBackgroundId: String,
    currentFontId: String,
    onThemeChange: (String, String) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. SÉLECTEUR DE PAPIER (FOND)
        Text("Papier", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(BookThemeOptions.backgrounds) { bg ->
                val isSelected = currentBackgroundId == bg.id
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg.color)
                            .border(2.dp, if (isSelected) accent else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onThemeChange(bg.id, currentFontId) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = if (bg.darkText) Color.Black else Color.White)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = bg.name.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isSelected) accent else theme.contentColor.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // 2. SÉLECTEUR DE PLUME (POLICE)
        Text("Plume", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(BookThemeOptions.fonts) { font ->
                val isSelected = currentFontId == font.id
                Card(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp)
                        .clickable { onThemeChange(currentBackgroundId, font.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) accent.copy(alpha = 0.15f) else theme.contentColor.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(1.dp, if (isSelected) accent else Color.Transparent)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Abc",
                            style = TextStyle(
                                fontFamily = font.fontFamily, 
                                fontSize = 16.sp, 
                                color = if (isSelected) accent else theme.contentColor
                            )
                        )
                    }
                }
            }
        }
    }
}
