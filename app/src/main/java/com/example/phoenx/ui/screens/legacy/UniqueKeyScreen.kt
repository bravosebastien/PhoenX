package com.example.phoenx.ui.screens.legacy

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniqueKeyScreen(
    onNavigateBack: () -> Unit,
    viewModel: UniqueKeyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var content by remember { mutableStateOf("") }
    var recipientId by remember { mutableStateOf("Ami") }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("Clé Unique", style = MaterialTheme.typography.labelLarge, color = theme.contentColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = uiState) {
                    is UniqueKeyState.Loading -> CircularProgressIndicator(color = accent)
                    
                    is UniqueKeyState.AlreadyExists -> {
                        UniqueKeyAlreadyExistsContent(theme)
                    }
                    
                    is UniqueKeyState.Success -> {
                        UniqueKeySuccessContent(state.phrase, theme)
                    }
                    
                    else -> {
                        UniqueKeyCreationContent(
                            content = content,
                            onContentChange = { content = it },
                            onGenerate = { viewModel.generateAndSaveKey(recipientId, content) },
                            isSaving = state is UniqueKeyState.Saving,
                            theme = theme
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UniqueKeyCreationContent(
    content: String,
    onContentChange: (String) -> Unit,
    onGenerate: () -> Unit,
    isSaving: Boolean,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    Icon(Icons.Default.Lock, null, tint = accent, modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        "L'Unique Secret",
        style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
        color = theme.contentColor,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        "Ce contenu est unique. Une seule fois par compte. Il sera chiffré avec une clé physique que vous devrez remettre en main propre.",
        style = MaterialTheme.typography.bodyMedium,
        color = theme.contentColor.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(40.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp).phoenXMatiere(isPaper = true),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        TextField(
            value = content,
            onValueChange = onContentChange,
            placeholder = { Text("Écris ici ce que tu n'as jamais dit à personne d'autre...", color = theme.contentColor.copy(alpha = 0.3f)) },
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontFamily = theme.fontFamily, color = theme.contentColor),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                cursorColor = accent
            )
        )
    }
    
    Spacer(modifier = Modifier.height(48.dp))
    
    Button(
        onClick = onGenerate,
        enabled = content.isNotBlank() && !isSaving,
        modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
        colors = ButtonDefaults.buttonColors(containerColor = accent)
    ) {
        if (isSaving) CircularProgressIndicator(color = theme.backgroundColor, modifier = Modifier.size(24.dp))
        else Text("Sceller et Générer la Clé", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UniqueKeySuccessContent(phrase: String, theme: AppThemeState) {
    val accent = theme.accentColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Key, null, tint = Success, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Secret Scellé", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = Success)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Voici votre Clé Unique. Notez-la. Elle ne sera plus jamais affichée.",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Surface(
            color = theme.contentColor.copy(alpha = 0.05f),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.3f))
        ) {
            Text(
                text = phrase,
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                color = Success,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UniqueKeyAlreadyExistsContent(theme: AppThemeState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 100.dp)) {
        Icon(Icons.Default.Lock, null, tint = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Clé déjà utilisée", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
        Text(
            "Le Tiroir à Clé Unique a déjà été scellé. Conformément à l'ADN de PHOEN-X, ce tiroir est définitif.",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.contentColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
