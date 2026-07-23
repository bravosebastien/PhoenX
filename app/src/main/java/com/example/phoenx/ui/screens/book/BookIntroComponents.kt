package com.example.phoenx.ui.screens.book

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun GlobalIntroCard(
    content: String,
    isGenerating: Boolean,
    onEdit: () -> Unit,
    onGenerate: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    // Rendu style "Page de Préface"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(2.dp), // Coins presque carrés pour le papier
        border = BorderStroke(0.5.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accent)
                Spacer(Modifier.height(12.dp))
                Text("Inspiration en cours...", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = theme.contentColor)
            } else if (content.isEmpty()) {
                Text(
                    "« Ton livre attend ses premiers mots d'ouverture. »",
                    style = TextStyle(fontFamily = theme.fontFamily, fontSize = 16.sp, fontStyle = FontStyle.Italic),
                    color = theme.contentColor.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RÉDIGER LA PRÉFACE", color = theme.backgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Ornement haut
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(accent.copy(alpha = 0.3f)))
                Spacer(Modifier.height(20.dp))
                
                Text(
                    text = content,
                    style = TextStyle(
                        fontFamily = theme.fontFamily, 
                        fontSize = 15.sp, 
                        fontStyle = FontStyle.Italic, 
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = theme.contentColor.copy(alpha = 0.9f)
                )
                
                Spacer(Modifier.height(20.dp))
                // Ornement bas
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(accent.copy(alpha = 0.3f)))
                
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) {
                        Text("Modifier manuellement", color = accent, fontSize = 12.sp)
                    }
                    TextButton(onClick = onGenerate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, null, tint = accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("🤖 Renouveler par l'IA", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalIntroEditorSheet(
    currentContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var text by remember { mutableStateOf(currentContent) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = theme.backgroundColor
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(24.dp)) {
            Text("Introduction du Livre", style = MaterialTheme.typography.headlineSmall, fontFamily = theme.fontFamily, color = theme.contentColor)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                textStyle = TextStyle(fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp, color = theme.contentColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                    cursorColor = accent
                )
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(text) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = theme.backgroundColor
                )
            ) {
                Text("Enregistrer l'introduction", color = theme.backgroundColor)
            }
        }
    }
}
