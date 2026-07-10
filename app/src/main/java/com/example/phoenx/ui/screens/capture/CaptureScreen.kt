package com.example.phoenx.ui.screens.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.phoenx.R
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    initialType: String = Screen.Capture.TYPE_TEXT,
    initialText: String = "",
    pactId: String? = null,
    pendingQuestionId: String? = null,
    latitude: Double? = null,
    longitude: Double? = null,
    locationName: String? = null,
    locationId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val suggestPin by viewModel.suggestPin.collectAsState()
    val detectedLocation by viewModel.detectedLocation.collectAsState()
    val preselectedName by viewModel.preselectedLocationName.collectAsState()

    var selectedGalleryUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedGalleryUri = uri
        } else if (initialType == Screen.Capture.TYPE_GALLERY) {
            onNavigateBack()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.checkLocationForPin(context)
        viewModel.setPreselectedLocation(locationId)
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
        if (initialType == Screen.Capture.TYPE_GALLERY) {
            galleryLauncher.launch("*/*")
        }
    }

    var text by remember { mutableStateOf(initialText) }
    var selectedCategory by remember { mutableStateOf("Sagesse") }
    var visibility by remember { mutableStateOf("private") }
    val selectedRecipientIds = remember { mutableStateListOf<String>() }
    val recipients by viewModel.recipients.collectAsState()
    val isSttListening by viewModel.isSttListening.collectAsState()
    val sttPartialText by viewModel.sttPartialText.collectAsState()
    val transcript by viewModel.transcript.collectAsState()

    LaunchedEffect(transcript) {
        if (transcript.isNotEmpty()) {
            text = transcript
        }
    }

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
            android.util.Log.d("RitualDebug", "isRitualPlaying positionné à true")
            isRitualPlaying = true
            delay(3500)
            android.util.Log.d("RitualDebug", "Fin du delay, navigation retour")
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
                    viewModel.saveEntry(
                        content = null,
                        mediaFile = null,
                        type = initialType,
                        category = "Sagesse",
                        visibility = "private",
                        pendingQuestionId = pendingQuestionId,
                        locationId = locationId
                    )
                    return@onKeyEvent true
                }
            }
            false
        },
        topBar = {
            if (!isNightMode && !isRitualPlaying) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (initialType) {
                                Screen.Capture.TYPE_AUDIO -> "Capture Vocale"
                                Screen.Capture.TYPE_PHOTO -> "Caméra"
                                Screen.Capture.TYPE_GALLERY -> "Galerie"
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
            if (!isNightMode && !isRitualPlaying && (initialType == Screen.Capture.TYPE_TEXT || initialType == Screen.Capture.TYPE_PHOTO || initialType == Screen.Capture.TYPE_GALLERY)) {
                BottomAppBar(containerColor = backgroundColor, tonalElevation = 0.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            Log.d("ClickDebug", "Clic détecté sur le bouton Annuler")
                            onNavigateBack()
                        }) {
                            Text("Annuler", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                Log.d("ClickDebug", "Clic détecté sur le bouton Déposer")
                                viewModel.saveEntry(
                                    content = text,
                                    mediaFile = capturedPhotoFile,
                                    type = initialType,
                                    category = selectedCategory,
                                    visibility = visibility,
                                    recipientIds = selectedRecipientIds.toList(),
                                    pendingQuestionId = pendingQuestionId,
                                    enigmaQuestion = if (enigmaQuestion.isNotBlank()) enigmaQuestion else null,
                                    enigmaAnswer = if (enigmaAnswer.isNotBlank()) enigmaAnswer else null,
                                    scheduledTimestamp = scheduledTimestamp,
                                    pactId = pactId,
                                    latitude = latitude,
                                    longitude = longitude,
                                    locationName = locationName,
                                    locationId = locationId
                                )
                            },
                            enabled = (text.isNotEmpty() || capturedPhotoFile != null || selectedGalleryUri != null || initialType == Screen.Capture.TYPE_PHOTO) && uiState !is CaptureUiState.Loading && !isRitualPlaying,
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedVisibility(
                visible = !isRitualPlaying,
                exit = slideOutVertically(tween(800)) { -it } + fadeOut(tween(600)),
                modifier = Modifier.fillMaxSize()
            ) {
                val boxModifier = if (isNightMode) {
                    Modifier.fillMaxSize().background(Color.Black).clickable {
                        if (uiState is CaptureUiState.RecordingAudio) {
                            viewModel.stopAudioRecording()
                            viewModel.saveEntry(
                                content = null,
                                mediaFile = null,
                                type = Screen.Capture.TYPE_NIGHT,
                                category = "Sagesse",
                                visibility = "private",
                                pendingQuestionId = pendingQuestionId,
                                locationId = locationId
                            )
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
                                isRecording = isSttListening,
                                transcript = text,
                                partialText = sttPartialText,
                                onStart = { viewModel.startVocalCapture(text) },
                                onStop = { viewModel.stopVocalCapture() },
                                onSave = {
                                    viewModel.saveEntry(
                                        content = text,
                                        mediaFile = null,
                                        type = Screen.Capture.TYPE_TEXT,
                                        category = selectedCategory,
                                        visibility = visibility,
                                        recipientIds = selectedRecipientIds.toList(),
                                        pendingQuestionId = pendingQuestionId,
                                        locationId = locationId
                                    )
                                },
                                recipients = recipients,
                                selectedRecipientIds = selectedRecipientIds
                            )
                        }
                        Screen.Capture.TYPE_PHOTO -> {
                            PhotoCaptureContent(
                                padding = padding,
                                capturedPhoto = capturedPhotoFile,
                                caption = text,
                                onCaptionChange = { text = it },
                                onPhotoCaptured = { capturedPhotoFile = it },
                                preselectedName = preselectedName,
                                recipients = recipients,
                                selectedRecipientIds = selectedRecipientIds
                            )
                        }
                        Screen.Capture.TYPE_GALLERY -> {
                            TextCaptureContent(
                                padding = padding,
                                text = text,
                                onTextChange = { text = it },
                                selectedCategory = selectedCategory,
                                onCategoryChange = { selectedCategory = it },
                                recipients = recipients,
                                selectedRecipientIds = selectedRecipientIds,
                                isListening = false,
                                onMicClick = { },
                                preselectedName = preselectedName,
                                galleryUri = selectedGalleryUri
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
                                isListening = isSttListening,
                                onMicClick = {
                                    if (isSttListening) viewModel.stopVocalCapture()
                                    else viewModel.startVocalCapture(text)
                                },
                                preselectedName = preselectedName
                            )
                        }
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

                Box(modifier = Modifier.fillMaxSize().background(BackgroundPrimary), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).scale(scale),
                            tint = AccentPrimary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (isNightMode) "Capturé. Dors bien." else "Souvenir déposé.",
                            style = MaterialTheme.typography.displaySmall,
                            color = AccentPrimary,
                            textAlign = TextAlign.Center
                        )
                        
                        if (!isNightMode) {
                            val recipientNames = selectedRecipientIds.mapNotNull { id -> 
                                recipients.find { it.id == id }?.name 
                            }
                            val summary = if (recipientNames.isNotEmpty()) {
                                "Rangé dans $selectedCategory, destiné à ${recipientNames.joinToString(", ")}."
                            } else {
                                "Rangé dans $selectedCategory. Aucun destinataire choisi pour l'instant."
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )
                        }
                    }
                }
            }

            if (suggestPin && detectedLocation != null) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                    Snackbar(
                        containerColor = SurfaceCard,
                        contentColor = TextPrimary,
                        action = {
                            Row {
                                TextButton(onClick = { viewModel.confirmPin(detectedLocation!!) }) {
                                    Text("Épingler", color = AccentPrimary)
                                }
                                TextButton(onClick = { viewModel.dismissPin() }) {
                                    Text("Non merci", color = TextTertiary)
                                }
                            }
                        }
                    ) {
                        Text("📍 Nouveau lieu détecté : ${detectedLocation!!.placeName}. Épingler sur la Mappemonde ?")
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Mode Détective", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            InfoPoint(
                title = "Le Jeu de Piste",
                content = "Transformez la lecture de vos souvenirs en une quête. Vos proches devront répondre à cette question personnelle pour déverrouiller ce fragment."
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
    onPhotoCaptured: (File) -> Unit,
    preselectedName: String? = null,
    recipients: List<com.example.phoenx.data.local.RecipientEntity> = emptyList(),
    selectedRecipientIds: MutableList<String> = mutableStateListOf()
) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (capturedPhoto == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                CameraPreview(
                    imageCapture = imageCapture,
                    modifier = Modifier.fillMaxSize()
                )
                
                IconButton(
                    onClick = {
                        val photoFile = File(context.cacheDir, "phoenx_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        
                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onPhotoCaptured(photoFile)
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    android.util.Log.e("CaptureScreen", "Photo capture failed", exception)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .size(80.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Prendre une photo", tint = Color.Black)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.DarkGray)) {
                    AsyncImage(
                        model = capturedPhoto,
                        contentDescription = "Photo capturée",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    IconButton(
                        onClick = { onPhotoCaptured(null as File) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Supprimer la photo", tint = Color.White)
                    }

                    if (preselectedName != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LocalAccentColor.current.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).padding(bottom = 80.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = LocalAccentColor.current, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Enregistré pour : $preselectedName", style = MaterialTheme.typography.labelSmall, color = LocalAccentColor.current)
                            }
                        }
                    }

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

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                    Text("POUR QUI ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    RecipientSelector(
                        recipients = recipients,
                        selectedIds = selectedRecipientIds,
                        accent = LocalAccentColor.current
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                } catch (e: Exception) {
                    android.util.Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextCaptureContent(
    padding: PaddingValues,
    text: String,
    onTextChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    recipients: List<com.example.phoenx.data.local.RecipientEntity>,
    selectedRecipientIds: MutableList<String>,
    isListening: Boolean,
    onMicClick: () -> Unit,
    preselectedName: String? = null,
    galleryUri: Uri? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "DÉPOSE TA PENSÉE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = AccentPrimary
        )

        if (preselectedName != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LocalAccentColor.current.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 16.dp, top = 12.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = LocalAccentColor.current, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pour : $preselectedName", style = MaterialTheme.typography.labelSmall, color = LocalAccentColor.current)
                }
            }
        }

        if (galleryUri != null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 16.dp).clip(RoundedCornerShape(12.dp))) {
                AsyncImage(model = galleryUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Écris tes mots ici...", style = MaterialTheme.typography.bodyLarge, color = TextTertiary) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontFamily = FontFamily.Serif),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            IconButton(
                onClick = onMicClick,
                modifier = Modifier.align(Alignment.TopEnd).background(if (isListening) Color.Red.copy(alpha = 0.1f) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isListening) Color.Red else AccentPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("DANS QUEL TIROIR ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Spacer(modifier = Modifier.height(12.dp))
        
        val categories = listOf("Sagesse", "Aventure", "Secret", "Famille", "Amour")
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LocalAccentColor.current, selectedLabelColor = BackgroundPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("POUR QUI ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Spacer(modifier = Modifier.height(12.dp))
        
        RecipientSelector(recipients = recipients, selectedIds = selectedRecipientIds, accent = LocalAccentColor.current)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipientSelector(
    recipients: List<com.example.phoenx.data.local.RecipientEntity>,
    selectedIds: MutableList<String>,
    accent: Color
) {
    Column {
        TextButton(
            onClick = {
                if (selectedIds.size == recipients.size) selectedIds.clear()
                else {
                    selectedIds.clear()
                    selectedIds.addAll(recipients.map { it.id })
                }
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            val allSelected = selectedIds.size == recipients.size && recipients.isNotEmpty()
            Text(if (allSelected) "Tout désélectionner" else "Ajouter tous les destinataires", style = MaterialTheme.typography.labelSmall, color = accent)
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recipients.forEach { recipient ->
                val isSelected = selectedIds.contains(recipient.id)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) selectedIds.remove(recipient.id)
                        else selectedIds.add(recipient.id)
                    },
                    label = { Text(recipient.name) },
                    leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = BackgroundPrimary)
                )
            }
        }
    }
}

@Composable
fun AudioCaptureContent(
    isRecording: Boolean,
    transcript: String,
    partialText: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
    recipients: List<com.example.phoenx.data.local.RecipientEntity> = emptyList(),
    selectedRecipientIds: MutableList<String> = mutableStateListOf()
) {
    val accent = LocalAccentColor.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isRecording && transcript.isNotEmpty()) {
            Text("Voici ce que j'ai compris :", style = MaterialTheme.typography.labelSmall, color = accent)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.5f))
            ) {
                Text(
                    text = transcript,
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = if (isRecording) "On t'écoute..." else if (transcript.isEmpty()) "Parle, nous écrivons pour toi" else "Continuer l'enregistrement ?",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        
        if (isRecording && partialText.isNotEmpty()) {
            Text(
                text = "... $partialText",
                style = MaterialTheme.typography.bodyMedium,
                color = accent.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            modifier = Modifier
                .size(140.dp)
                .scale(if (isRecording) scale else 1f)
                .shadow(if (isRecording) 20.dp else 0.dp, CircleShape, spotColor = accent)
                .clickable { if (isRecording) onStop() else onStart() },
            shape = CircleShape,
            color = if (isRecording) Error.copy(alpha = 0.2f) else accent.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (isRecording) Error else accent)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isRecording) Error else accent,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        if (isRecording) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Appuie pour arrêter", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        } else if (transcript.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("POUR QUI ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Spacer(modifier = Modifier.height(12.dp))

            RecipientSelector(recipients = recipients, selectedIds = selectedRecipientIds, accent = accent)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Sceller ce souvenir", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
            }
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
