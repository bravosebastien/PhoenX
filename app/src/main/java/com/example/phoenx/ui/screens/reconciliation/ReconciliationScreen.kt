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
import androidx.compose.ui.res.stringResource
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
                        Text(stringResource(R.string.reconciliation_title), style = MaterialTheme.typography.labelLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        InfoButton(
                            title = stringResource(R.string.reconciliation_info_title),
                            points = listOf(
                                stringResource(R.string.reconciliation_info_p1),
                                stringResource(R.string.reconciliation_info_p2),
                                stringResource(R.string.reconciliation_info_p3),
                                stringResource(R.string.reconciliation_info_p4),
                                stringResource(R.string.reconciliation_info_p5)
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
                    stringResource(R.string.reconciliation_main_question),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    lineHeight = 34.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text(stringResource(R.string.reconciliation_label_recipient)) },
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
                    label = { Text(stringResource(R.string.reconciliation_label_intent)) },
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
                            Text(stringResource(R.string.reconciliation_button_ai_help), color = accent, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        color = accent.copy(alpha = 0.05f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(vertical = 16.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.reconciliation_ai_suggestions_title), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
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
                        Text(stringResource(R.string.reconciliation_mode_text), color = if (contentType == "TEXT") theme.backgroundColor else theme.contentColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.reconciliation_mode_audio), color = if (contentType == "AUDIO") theme.backgroundColor else theme.contentColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
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
                                    stringResource(R.string.reconciliation_placeholder_message),
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
                                Text(stringResource(R.string.reconciliation_audio_tap_to_record), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Surface(
                    color = Warning.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockClock, null, tint = Warning, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.reconciliation_golden_rule),
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
                        Text(stringResource(R.string.reconciliation_button_seal), color = theme.backgroundColor, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
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
                            stringResource(R.string.reconciliation_success_sealed),
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
