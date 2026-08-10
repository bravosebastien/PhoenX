package com.example.phoenx.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewBookScreen(
    recipientUid: String,
    onNavigateBack: () -> Unit,
    onConsultBook: () -> Unit, // v9.4.27
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current

    LaunchedEffect(recipientUid) {
        viewModel.loadPreview(recipientUid)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Livre de Vie (Aperçu)", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(state.recipientName, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(64.dp), tint = accent.copy(alpha = 0.2f))
                Spacer(Modifier.height(24.dp))
                
                val title = state.bookTitle ?: "Livre de Ma Vie"
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (state.hasBookDraft && state.isBookShared) {
                    Text(
                        "Un manuscrit est déjà disponible pour consultation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onConsultBook,
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Consulter le manuscrit", color = theme.backgroundColor)
                    }
                } else if (state.hasBookDraft && !state.isBookShared) {
                    Text(
                        "Un Livre existe, mais il n'est pas partagé avec ${state.recipientName}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pour le partager, retournez dans l'éditeur de livre et ajoutez ce proche à la liste des destinataires.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Text(
                        "Aucun Livre n'a encore été généré — le Destinataire ne verra rien de ce côté pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pour créer votre Livre, retournez dans votre Bibliothèque et utilisez l'IA Co-écrivain.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
