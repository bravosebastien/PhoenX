package com.example.phoenx.ui.screens.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
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
 * AssistantChatPanel (v9.4.25)
 * Interface de discussion avec l'assistant IA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChatPanel(
    viewModel: AssistantViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var question by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.contentColor.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Assistant PHOEN-X",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.clearChat() }) {
                    Text("Effacer", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // Liste des messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                reverseLayout = true // Pour voir les derniers messages en bas
            ) {
                if (isLoading) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = accent
                        )
                    }
                }
                
                items(messages.reversed()) { msg ->
                    ChatBubble(msg, theme.contentColor, accent)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Champ de saisie (v9.4.25 : Ancré en bas avec support clavier)
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = { Text("Posez votre question...", fontSize = 14.sp) },
                trailingIcon = {
                    IconButton(
                        onClick = { 
                            viewModel.askQuestion(question)
                            question = "" 
                        },
                        enabled = question.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = accent)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                    focusedTextColor = theme.contentColor,
                    unfocusedTextColor = theme.contentColor
                ),
                maxLines = 3
            )
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, contentColor: Color, accent: Color) {
    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
    val bgColor = if (msg.isUser) accent.copy(alpha = 0.1f) else contentColor.copy(alpha = 0.05f)
    
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (msg.isUser) accent.copy(alpha = 0.2f) else contentColor.copy(alpha = 0.1f))
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}
