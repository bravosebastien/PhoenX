package com.example.phoenx.ui.screens.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

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
    parentEntryId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
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
    var visibility by remember { mutableStateOf("RESTRICTED") }
    val selectedRecipientIds = remember { mutableStateListOf<String>() }
    var notifyByEmail by remember { mutableStateOf(true) } // Nouveauté v8.9.8
    var isTonaliteExpanded by remember { mutableStateOf(false) }
    var isTiroirsExpanded by remember { mutableStateOf(false) }
    
    val recipients by viewModel.recipients.collectAsState()
    val isSttListening by viewModel.isSttListening.collectAsState()
    val sttPartialText by viewModel.sttPartialText.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val suggestedPersons by viewModel.suggestedPersons.collectAsState()
    val selectedPersons by viewModel.selectedPersons.collectAsState()

    LaunchedEffect(transcript) {
        if (transcript.isNotEmpty()) {
            text = transcript
        }
    }

    var showAdvancedOptions by remember { mutableStateOf(false) }
    var enigmaQuestion by remember { mutableStateOf("") }
    var enigmaAnswer by remember { mutableStateOf("") }
    var enigmaHint by remember { mutableStateOf("") }
    var enigmaAutoUnlockDays by remember { mutableStateOf<Int?>(null) }
    var scheduledTimestamp by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val isNightMode = initialType == Screen.Capture.TYPE_NIGHT
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    LaunchedEffect(capturedPhotoFile) {
        if (capturedPhotoFile?.name == "clear") {
            capturedPhotoFile = null
        }
    }
    var isRitualPlaying by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.newEntryId.collect { id ->
            if (isNightMode || parentEntryId != null) {
                // Rien de plus
            } else {
                delay(3500)
                onNavigateToDetail(id)
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Success) {
            isRitualPlaying = true
            if (isNightMode || parentEntryId != null) {
                delay(3500)
                onNavigateBack()
            }
        }
    }

    val backgroundColor = if (isNightMode) Color.Black else theme.backgroundColor

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
                        visibility = visibility,
                        pendingQuestionId = pendingQuestionId,
                        locationId = locationId,
                        parentEntryId = parentEntryId
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
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            fontFamily = theme.fontFamily
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = theme.contentColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAdvancedOptions = true }) {
                            Icon(Icons.Default.Tune, null, tint = accent)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        titleContentColor = theme.contentColor
                    )
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
                        TextButton(onClick = { onNavigateBack() }) {
                            Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
                        }
                        Button(
                            onClick = {
                                val mediaFile = if (initialType == Screen.Capture.TYPE_GALLERY) {
                                    selectedGalleryUri?.let { viewModel.uriToFile(it) }
                                } else {
                                    capturedPhotoFile
                                }

                                viewModel.saveEntry(
                                    content = text,
                                    mediaFile = mediaFile,
                                    type = initialType,
                                    category = selectedCategory,
                                    visibility = visibility,
                                    recipientIds = selectedRecipientIds.toList(),
                                    silentAttribution = !notifyByEmail, // Nouveauté v8.9.8
                                    pendingQuestionId = pendingQuestionId,
                                    enigmaQuestion = if (enigmaQuestion.isNotBlank()) enigmaQuestion else null,
                                    enigmaAnswer = if (enigmaAnswer.isNotBlank()) enigmaAnswer else null,
                                    enigmaHint = if (enigmaHint.isNotBlank()) enigmaHint else null,
                                    enigmaAutoUnlockDays = enigmaAutoUnlockDays,
                                    scheduledTimestamp = scheduledTimestamp,
                                    pactId = pactId,
                                    latitude = latitude,
                                    longitude = longitude,
                                    locationName = locationName,
                                    locationId = locationId,
                                    parentEntryId = parentEntryId
                                )
                            },
                            enabled = (text.isNotEmpty() || capturedPhotoFile != null || selectedGalleryUri != null || initialType == Screen.Capture.TYPE_PHOTO) && uiState !is CaptureUiState.Loading && !isRitualPlaying,
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (uiState is CaptureUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.backgroundColor, strokeWidth = 2.dp)
                            } else {
                                Text("Déposer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
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
                    Modifier.fillMaxSize().background(theme.backgroundColor)
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
                                onTranscriptChange = { text = it },
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
                                        silentAttribution = !notifyByEmail, // Nouveauté v8.9.8
                                        pendingQuestionId = pendingQuestionId,
                                        locationId = locationId,
                                        parentEntryId = parentEntryId
                                    )
                                },
                                recipients = recipients,
                                selectedRecipientIds = selectedRecipientIds,
                                visibility = visibility,
                                onVisibilityChange = { visibility = it },
                                notifyByEmail = notifyByEmail,
                                onNotifyByEmailChange = { notifyByEmail = it },
                                selectedPersons = selectedPersons,
                                suggestedPersons = suggestedPersons,
                                onSearchPersons = { viewModel.searchPersons(it) },
                                onSelectPerson = { viewModel.selectPerson(it) },
                                onCreatePerson = { f, l, r, dt, dv -> viewModel.createAndSelectPerson(f, l, r, dt, dv) },
                                onRemovePerson = { viewModel.removePerson(it) },
                                selectedCategory = selectedCategory,
                                onCategoryChange = { selectedCategory = it },
                                isTonaliteExpanded = isTonaliteExpanded,
                                onTonaliteToggle = { isTonaliteExpanded = !isTonaliteExpanded },
                                isTiroirsExpanded = isTiroirsExpanded,
                                onTiroirsToggle = { isTiroirsExpanded = !isTiroirsExpanded }
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
                                selectedRecipientIds = selectedRecipientIds,
                                visibility = visibility,
                                onVisibilityChange = { visibility = it },
                                notifyByEmail = notifyByEmail,
                                onNotifyByEmailChange = { notifyByEmail = it },
                                selectedPersons = selectedPersons,
                                suggestedPersons = suggestedPersons,
                                onSearchPersons = { viewModel.searchPersons(it) },
                                onSelectPerson = { viewModel.selectPerson(it) },
                                onCreatePerson = { f, l, r, dt, dv -> viewModel.createAndSelectPerson(f, l, r, dt, dv) },
                                onRemovePerson = { viewModel.removePerson(it) },
                                selectedCategory = selectedCategory,
                                onCategoryChange = { selectedCategory = it },
                                isTonaliteExpanded = isTonaliteExpanded,
                                onTonaliteToggle = { isTonaliteExpanded = !isTonaliteExpanded },
                                isTiroirsExpanded = isTiroirsExpanded,
                                onTiroirsToggle = { isTiroirsExpanded = !isTiroirsExpanded }
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
                                visibility = visibility,
                                onVisibilityChange = { visibility = it },
                                notifyByEmail = notifyByEmail,
                                onNotifyByEmailChange = { notifyByEmail = it },
                                isListening = false,
                                onMicClick = { },
                                preselectedName = preselectedName,
                                galleryUri = selectedGalleryUri,
                                selectedPersons = selectedPersons,
                                suggestedPersons = suggestedPersons,
                                onSearchPersons = { viewModel.searchPersons(it) },
                                onSelectPerson = { viewModel.selectPerson(it) },
                                onCreatePerson = { f, l, r, dt, dv -> viewModel.createAndSelectPerson(f, l, r, dt, dv) },
                                onRemovePerson = { viewModel.removePerson(it) },
                                isTonaliteExpanded = isTonaliteExpanded,
                                onTonaliteToggle = { isTonaliteExpanded = !isTonaliteExpanded },
                                isTiroirsExpanded = isTiroirsExpanded,
                                onTiroirsToggle = { isTiroirsExpanded = !isTiroirsExpanded }
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
                                visibility = visibility,
                                onVisibilityChange = { visibility = it },
                                notifyByEmail = notifyByEmail,
                                onNotifyByEmailChange = { notifyByEmail = it },
                                isListening = isSttListening,
                                onMicClick = {
                                    if (isSttListening) viewModel.stopVocalCapture()
                                    else viewModel.startVocalCapture(text)
                                },
                                preselectedName = preselectedName,
                                galleryUri = selectedGalleryUri,
                                isComplement = parentEntryId != null,
                                initialType = initialType,
                                selectedPersons = selectedPersons,
                                suggestedPersons = suggestedPersons,
                                onSearchPersons = { viewModel.searchPersons(it) },
                                onSelectPerson = { viewModel.selectPerson(it) },
                                onCreatePerson = { f, l, r, dt, dv -> viewModel.createAndSelectPerson(f, l, r, dt, dv) },
                                onRemovePerson = { viewModel.removePerson(it) },
                                isTonaliteExpanded = isTonaliteExpanded,
                                onTonaliteToggle = { isTonaliteExpanded = !isTonaliteExpanded },
                                isTiroirsExpanded = isTiroirsExpanded,
                                onTiroirsToggle = { isTiroirsExpanded = !isTiroirsExpanded }
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

                Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).scale(scale),
                            tint = accent
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (isNightMode) "Capturé. Dors bien." else "Souvenir déposé.",
                            style = MaterialTheme.typography.displaySmall,
                            color = accent,
                            textAlign = TextAlign.Center,
                            fontFamily = theme.fontFamily
                        )
                        
                        if (!isNightMode) {
                            val recipientNames = selectedRecipientIds.mapNotNull { id -> 
                                recipients.find { it.id == id }?.name 
                            }
                            
                            val summary = if (visibility == "EVERYONE") {
                                "Rangé dans $selectedCategory, partagé avec tout le monde."
                            } else {
                                if (recipientNames.isNotEmpty()) {
                                    "Rangé dans $selectedCategory, destiné à ${recipientNames.joinToString(", ")}."
                                } else {
                                    "Rangé dans $selectedCategory. Aucun destinataire choisi pour l'instant."
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.contentColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )

                            // NOUVEAUTÉ v8.9.8 : Lien Vivant
                            if (selectedRecipientIds.size == 1) {
                                val recipient = recipients.find { it.id == selectedRecipientIds.first() }
                                if (recipient != null) {
                                    Spacer(modifier = Modifier.height(32.dp))
                                    com.example.phoenx.ui.components.LienVivantBanner(
                                        recipientName = recipient.name,
                                        recipientPhone = recipient.phone,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (suggestPin && detectedLocation != null) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                    Snackbar(
                        containerColor = SurfaceCard,
                        contentColor = theme.backgroundColor,
                        action = {
                            Row {
                                TextButton(onClick = { viewModel.confirmPin(detectedLocation!!) }) {
                                    Text("Épingler", color = AccentPrimary)
                                }
                                TextButton(onClick = { viewModel.dismissPin() }) {
                                    Text("Non merci", color = theme.contentColor.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
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
                    enigmaHint = enigmaHint,
                    onEnigmaHintChange = { enigmaHint = it },
                    enigmaAutoUnlockDays = enigmaAutoUnlockDays,
                    onEnigmaAutoUnlockDaysChange = { enigmaAutoUnlockDays = it },
                    scheduledTimestamp = scheduledTimestamp,
                    onScheduledTimestampChange = { scheduledTimestamp = it }
                )
            }
        }
    }
}
