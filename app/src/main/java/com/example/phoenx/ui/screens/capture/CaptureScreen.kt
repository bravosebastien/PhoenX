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
import com.example.phoenx.ui.components.BookWritingMode
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.components.PhoenXRiveAnimation
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    initialType: String = Screen.Capture.TYPE_TEXT,
    initialText: String = "",
    pactId: String? = null,
    latitude: Double? = null,
    longitude: Double? = null,
    locationName: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // GESTION DES PERMISSIONS
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) onNavigateBack()
    }

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
    var useBookMode by remember { mutableStateOf(true) }
    val selectedRecipientIds = remember { mutableStateListOf<String>() }
    val recipients by viewModel.recipients.collectAsState()
    
    // OPTIONS AVANCÉES (ADN 5.0)
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var enigmaQuestion by remember { mutableStateOf("") }
    var enigmaAnswer by remember { mutableStateOf("") }
    var scheduledTimestamp by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState()

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
                        IconButton(onClick = { showAdvancedOptions = true }) {
                            Icon(Icons.Default.Tune, null, tint = AccentPrimary)
                        }
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
                                viewModel.saveEntry(
                                    content = text, 
                                    mediaFile = capturedPhotoFile, 
                                    type = initialType, 
                                    category = selectedCategory, 
                                    visibility = visibility,
                                    recipientIds = selectedRecipientIds.toList(),
                                    enigmaQuestion = if (enigmaQuestion.isNotBlank()) enigmaQuestion else null,
                                    enigmaAnswer = if (enigmaAnswer.isNotBlank()) enigmaAnswer else null,
                                    scheduledTimestamp = scheduledTimestamp,
                                    pactId = pactId,
                                    latitude = latitude,
                                    longitude = longitude,
                                    locationName = locationName
                                ) 
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                                onCategoryChange = { selectedCategory = it },
                                recipients = recipients,
                                selectedRecipientIds = selectedRecipientIds,
                                useBookMode = useBookMode,
                                onToggleMode = { useBookMode = !useBookMode }
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

        if (showAdvancedOptions) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedOptions = false },
                sheetState = sheetState,
                containerColor = BackgroundSecondary
            ) {
                AdvancedOptionsContent(
                    enigmaQuestion = enigmaQuestion,
                    onEnigmaQuestionChange = { enigmaQuestion = it },
                    enigmaAnswer = enigmaAnswer,
                    onEnigmaAnswerChange = { enigmaAnswer = it },
                    scheduledTimestamp = scheduledTimestamp,
                    onScheduledTimestampChange = { scheduledTimestamp = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedOptionsContent(
    enigmaQuestion: String,
    onEnigmaQuestionChange: (String) -> Unit,
    enigmaAnswer: String,
    onEnigmaAnswerChange: (String) -> Unit,
    scheduledTimestamp: Long?,
    onScheduledTimestampChange: (Long?) -> Unit
) {
    Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
        Text("OPTIONS AVANCÉES", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(32.dp))

        // MODE DÉTECTIVE
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Mode Détective", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            InfoPoint(
                title = "Le Jeu de Piste",
                content = "Transformez la lecture de vos souvenirs en une quête. Vos proches devront répondre à cette question personnelle pour déverrouiller ce fragment. C'est idéal pour les anecdotes de famille ou les secrets partagés."
            )
        }
        Text(
            "Verrouille ce souvenir derrière une énigme personnelle.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = enigmaQuestion,
            onValueChange = onEnigmaQuestionChange,
            label = { Text("Ta question secrète") },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            placeholder = { Text("Ex: Quel était le nom de notre premier chien ?") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = enigmaAnswer,
            onValueChange = onEnigmaAnswerChange,
            label = { Text("La réponse attendue") },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // BOÎTE AUX LETTRES
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Ouverture Programmée", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        Text(
            "Ce souvenir ne sera visible qu'à partir d'une date précise.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val dateText = scheduledTimestamp?.let {
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(it))
        } ?: "Choisir une date"
        
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState()

        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp)
        ) {
            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(dateText)
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onScheduledTimestampChange(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) { Text("Confirmer", color = AccentPrimary) }
                }
            ) {
                DatePicker(state = datePickerState)
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
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("Caméra active", color = Color.White)
                IconButton(
                    onClick = { /* Simulate capture */ },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).size(80.dp).background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.DarkGray)) {
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
    onCategoryChange: (String) -> Unit,
    recipients: List<com.example.phoenx.data.local.RecipientEntity>,
    selectedRecipientIds: MutableList<String>,
    useBookMode: Boolean,
    onToggleMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (useBookMode) "ÉCRITURE SACRÉE" else "VUE SIMPLE",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onToggleMode) {
                Text(if (useBookMode) "Passer en vue simple" else "Activer la plume", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            }
        }

        if (useBookMode) {
            BookWritingMode(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { 
                    Text("Écris ce qui ne doit pas se perdre...", style = MaterialTheme.typography.displaySmall, color = TextTertiary) 
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                textStyle = MaterialTheme.typography.displaySmall.copy(color = TextPrimary, lineHeight = 34.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("POUR QUI ?", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (recipients.isEmpty()) {
            Text("Personne dans ton cercle. Ajoute tes proches dans l'accueil.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                recipients.forEach { recipient ->
                    val isSelected = selectedRecipientIds.contains(recipient.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selectedRecipientIds.remove(recipient.id)
                            else selectedRecipientIds.add(recipient.id)
                        },
                        label = { Text(recipient.name) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("ÉTAT ÉMOTIONNEL", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val categories = listOf("Espoir", "Poésie", "Bonheur", "Regret", "Sagesse", "Amour")
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            InfoPoint(
                title = "Les Tiroirs Émotionnels",
                content = "L'IA utilise ce choix pour ranger automatiquement votre souvenir dans le bon tiroir de votre commode. Si vous ne choisissez rien, l'IA analysera vos mots pour le faire à votre place."
            )
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
