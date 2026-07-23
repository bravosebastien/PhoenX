package com.example.phoenx.ui.screens.reconciliation

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay

import androidx.compose.animation.core.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.scale
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReconciliationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var intent by remember { mutableStateOf("") }
    var isRitualPlaying by remember { mutableStateOf(false) }
    var contentType by remember { mutableStateOf("TEXT") } // TEXT ou AUDIO
    
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    // Permission pour l'audio
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contentType = "AUDIO"
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            isRitualPlaying = true
            delay(3500)
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Protocole de Réconciliation", style = MaterialTheme.typography.labelLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        InfoButton(
                            title = "Protocole de Réconciliation",
                            points = listOf(
                                "Écris un message à quelqu'un à qui tu n'as jamais dit ce que tu aurais dû dire.",
                                "Ce message sera délivré après ton départ, avec un délai que tu choisis.",
                                "Le délai permet au deuil de s'apaiser avant que la personne lise ton message.",
                                "La personne n'a pas besoin d'avoir un compte PHOEN-X pour recevoir ce message.",
                                "Tu peux choisir un délai entre 30 jours et 1 an après ton départ."
                            )
                        )
                    }
                },
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
                    .padding(24.dp)
            ) {
                Text(
                    "Y a-t-il quelqu'un à qui tu n'as jamais dit ce que tu aurais dû dire ?",
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    lineHeight = 34.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Prénom du destinataire") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                        focusedTextColor = theme.contentColor,
                        unfocusedTextColor = theme.contentColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = intent,
                    onValueChange = { intent = it },
                    label = { Text("Ton intention (ex: demander pardon, dire merci...)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                        focusedTextColor = theme.contentColor,
                        unfocusedTextColor = theme.contentColor
                    )
                )

                if (uiState.aiHelp == null) {
                    TextButton(
                        onClick = { viewModel.getAIHelp(recipientName, intent) },
                        enabled = recipientName.isNotEmpty() && intent.isNotEmpty() && !uiState.isLoadingHelp
                    ) {
                        if (uiState.isLoadingHelp) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accent, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = accent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Demander l'aide de l'IA pour formuler", color = accent, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        color = accent.copy(alpha = 0.05f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(vertical = 16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SUGGESTIONS DE L'IA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(uiState.aiHelp!!, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SÉLECTEUR DE MODE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { contentType = "TEXT" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (contentType == "TEXT") accent else theme.contentColor.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("✍️ Texte", color = if (contentType == "TEXT") theme.backgroundColor else theme.contentColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                contentType = "AUDIO"
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (contentType == "AUDIO") accent else theme.contentColor.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🎙️ Audio", color = if (contentType == "AUDIO") theme.backgroundColor else theme.contentColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (contentType == "TEXT") {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp)
                            .background(theme.contentColor.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = theme.fontFamily,
                            color = theme.contentColor
                        ),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text(
                                    "Écris ton message ici...",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = theme.fontFamily,
                                        color = theme.contentColor.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                } else {
                    // MODE AUDIO (Simplifié pour l'instant)
                    Card(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { /* Démarrer enregistrement */ },
                                    modifier = Modifier.size(64.dp).background(accent.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Mic, null, tint = accent, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Appuie pour enregistrer", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Surface(
                    color = Warning.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, Warning.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockClock, null, tint = Warning, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "RÈGLE D'OR : Ce message sera verrouillé pendant 30 jours après l'activation de ton héritage. Pour laisser le temps au deuil de s'apaiser.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Warning,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { viewModel.saveReconciliationMessage(text, recipientName) },
                    enabled = text.isNotEmpty() && recipientName.isNotEmpty() && !uiState.isSaving && !isRitualPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .phoenXMatiere()
                        .alpha(if (text.isNotEmpty() && recipientName.isNotEmpty()) 1f else 0.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        disabledContainerColor = accent.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = theme.backgroundColor, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Confier au secret", color = theme.backgroundColor, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            if (isRitualPlaying) {
                val infiniteTransition = rememberInfiniteTransition(label = "ritual_pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier.fillMaxSize().background(theme.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).scale(scale),
                            tint = accent
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Message scellé pour 30 jours.",
                            style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                            color = accent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
