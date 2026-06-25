package com.example.phoenx.ui.screens.capture

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.phoenx.R
import com.example.phoenx.ui.components.PhoenXRiveAnimation
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    initialType: String = Screen.Capture.TYPE_TEXT,
    initialText: String = "",
    onNavigateBack: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // GESTION DES PERMISSIONS (ADN 5.0)
    var permissionToRequest by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) onNavigateBack() // Retour si refus
    }

    // Vérification initiale selon le type
    LaunchedEffect(initialType) {
        val permission = when (initialType) {
            Screen.Capture.TYPE_AUDIO, Screen.Capture.TYPE_NIGHT -> Manifest.permission.RECORD_AUDIO
            Screen.Capture.TYPE_PHOTO -> Manifest.permission.CAMERA
            else -> null
        }
        
        if (permission != null) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(permission)
            }
        }
    }

    var text by remember { mutableStateOf(initialText) }
    var selectedCategory by remember { mutableStateOf("Sagesse") }
    var visibility by remember { mutableStateOf("Privé") }
    val isNightMode = initialType == Screen.Capture.TYPE_NIGHT
    
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var isRitualPlaying by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Success) {
            isRitualPlaying = true
            delay(3500)
            onNavigateBack()
        }
    }

    val backgroundColor = if (isNightMode) Color.Black else BackgroundPrimary

    Scaffold(
        containerColor = backgroundColor,
        modifier = Modifier.onKeyEvent { event ->
            if (uiState is CaptureUiState.RecordingAudio) {
                if (event.key == Key.VolumeUp || event.key == Key.VolumeDown) {
                    viewModel.stopAudioRecording()
                    viewModel.saveEntry(null, null, initialType, "Sagesse", "Privé")
                    return@onKeyEvent true
                }
            }
            false
        },
        topBar = {
            if (!isNightMode) {
                TopAppBar(
                    title = { 
                        Text(
                            text = when(initialType) {
                                Screen.Capture.TYPE_AUDIO -> "Capture Vocale"
                                Screen.Capture.TYPE_PHOTO -> "Capture Visuelle"
                                else -> "Nouvelle Pensée"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextPrimary)
                        }
                    },
                    actions = {
                        Text(
                            "🔒 E2EE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                )
            }
        },
        bottomBar = {
            if (!isNightMode && (initialType == Screen.Capture.TYPE_TEXT || initialType == Screen.Capture.TYPE_PHOTO)) {
                BottomAppBar(containerColor = backgroundColor, tonalElevation = 0.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onNavigateBack) {
                            Text("Annuler", color = TextSecondary)
                        }
                        Button(
                            onClick = { 
                                viewModel.saveEntry(text, capturedPhotoFile, initialType, selectedCategory, visibility) 
                            },
                            enabled = (text.isNotEmpty() || capturedPhotoFile != null || initialType == Screen.Capture.TYPE_PHOTO) && uiState !is CaptureUiState.Loading && !isRitualPlaying,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (uiState is CaptureUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BackgroundPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("Déposer", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        AnimatedVisibility(
            visible = !isRitualPlaying,
            exit = slideOutVertically(tween(800)) { -it } + fadeOut(tween(600)),
            modifier = Modifier.fillMaxSize()
        ) {
            val boxModifier = if (isNightMode) {
                Modifier.fillMaxSize().background(Color.Black).clickable {
                    if (uiState is CaptureUiState.RecordingAudio) {
                        viewModel.stopAudioRecording()
                        viewModel.saveEntry(null, null, Screen.Capture.TYPE_NIGHT, "Sagesse", "Privé")
                    }
                }
            } else {
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(listOf(BackgroundSecondary, BackgroundPrimary), radius = 2000f)
                )
            }

            Box(modifier = boxModifier) {
                when (initialType) {
                    Screen.Capture.TYPE_NIGHT -> {
                        NightCaptureContent(
                            isRecording = uiState is CaptureUiState.RecordingAudio,
                            onStart = { viewModel.startAudioRecording(context.cacheDir) }
                        )
                    }
                    Screen.Capture.TYPE_AUDIO -> {
                        AudioCaptureContent(
                            isRecording = uiState is CaptureUiState.RecordingAudio,
                            onStart = { viewModel.startAudioRecording(context.cacheDir) },
                            onStop = { 
                                viewModel.stopAudioRecording()
                                viewModel.saveEntry(null, null, Screen.Capture.TYPE_AUDIO, "Sagesse", "Privé")
                            }
                        )
                    }
                    Screen.Capture.TYPE_PHOTO -> {
                        PhotoCaptureContent(
                            padding = padding,
                            capturedPhoto = capturedPhotoFile,
                            caption = text,
                            onCaptionChange = { text = it },
                            onPhotoCaptured = { capturedPhotoFile = it }
                        )
                    }
                    else -> {
                        TextCaptureContent(
                            padding = padding,
                            text = text,
                            onTextChange = { text = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it }
                        )
                    }
                }
            }
        }

        if (isRitualPlaying) {
            Box(modifier = Modifier.fillMaxSize().background(BackgroundPrimary), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PhoenXRiveAnimation(
                        resId = R.raw.depot,
                        modifier = Modifier.size(300.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (isNightMode) "Capturé. Dors bien." else "Souvenir déposé.",
                        style = MaterialTheme.typography.displaySmall,
                        color = AccentPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoCaptureContent(
    padding: PaddingValues,
    capturedPhoto: File?,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onPhotoCaptured: (File) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (capturedPhoto == null) {
            // Camera Preview (Simplified)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("Caméra active", color = Color.White)
                // In a real implementation, we would use PreviewView here
                IconButton(
                    onClick = { /* Simulate capture */ },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).size(80.dp).background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
                }
            }
        } else {
            // Photo Preview with Caption overlay
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.DarkGray)) {
                    // Display photo
                    Text("Photo capturée", modifier = Modifier.align(Alignment.Center), color = Color.White)
                    
                    TextField(
                        value = caption,
                        onValueChange = onCaptionChange,
                        placeholder = { Text("Ajoute une légende...", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.6f)) },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TextCaptureContent(
    padding: PaddingValues,
    text: String,
    onTextChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { 
                Text("Écris ce qui ne doit pas se perdre...", style = MaterialTheme.typography.displaySmall, color = TextTertiary) 
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = MaterialTheme.typography.displaySmall.copy(color = TextPrimary, lineHeight = 34.sp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text("ÉTAT ÉMOTIONNEL", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        val categories = listOf("Espoir", "Poésie", "Bonheur", "Regret", "Sagesse", "Amour")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = AccentPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun AudioCaptureContent(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRecording) "On t'écoute..." else "Prêt à enregistrer ta voix ?",
            style = MaterialTheme.typography.displaySmall,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(64.dp))

        Surface(
            modifier = Modifier
                .size(160.dp)
                .scale(if (isRecording) scale else 1f)
                .clickable { if (isRecording) onStop() else onStart() },
            shape = CircleShape,
            color = if (isRecording) Error.copy(alpha = 0.2f) else AccentPrimary.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (isRecording) Error else AccentPrimary)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isRecording) Error else AccentPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        if (isRecording) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Appuie pour arrêter", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}

@Composable
fun NightCaptureContent(
    isRecording: Boolean,
    onStart: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (!isRecording) onStart()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "CAPTURE INVISIBLE",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier.size(4.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.5f)
            ) {}
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Parle. On garde tout.\nTouches VOLUME pour arrêter.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
