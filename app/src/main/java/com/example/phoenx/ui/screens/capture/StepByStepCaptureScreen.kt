package com.example.phoenx.ui.screens.capture

// RESTAURÉS le 18/09 : ces trois imports avaient disparu de l'en-tête du fichier,
// alors que le code qui les utilise était resté en place.
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.phoenx.ui.components.RecipientSelector
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.components.EnigmaForm
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StepByStepCaptureScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit, // v9.4.26
    viewModel: StepByStepCaptureViewModel = hiltViewModel(),
    captureViewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val captureState by captureViewModel.uiState.collectAsState()
    val recipients by captureViewModel.recipients.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    // v13.0 : Géolocalisation directe (alternative à la sélection manuelle sur la Mappemonde)
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isLocatingGps by remember { mutableStateOf(false) }

    fun resolveAndUseCurrentLocation() {
        isLocatingGps = true
        scope.launch {
            try {
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ).await()
                if (location != null) {
                    val geocoder = android.location.Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = addresses?.firstOrNull()
                    val placeName = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                        ?: context.getString(R.string.step_capture_viewmodel_location_unknown)
                    val countryName = address?.countryName ?: ""
                    viewModel.useCurrentGpsLocation(location.latitude, location.longitude, placeName, countryName)
                } else {
                    android.widget.Toast.makeText(context, context.getString(R.string.step_capture_location_gps_unavailable), android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.step_capture_location_gps_unavailable), android.widget.Toast.LENGTH_LONG).show()
            } finally {
                isLocatingGps = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) resolveAndUseCurrentLocation() }

    fun onUseCurrentLocationClick() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            resolveAndUseCurrentLocation()
        } else {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.step_capture_title, uiState.currentStep),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        fontFamily = theme.fontFamily
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep > 1) viewModel.previousStep()
                        else onNavigateBack()
                    }) {
                        Icon(
                            imageVector = if (uiState.currentStep > 1) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = null,
                            tint = theme.contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = theme.backgroundColor, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.currentStep > 1) {
                        TextButton(onClick = { viewModel.nextStep() }) {
                            Text(stringResource(R.string.step_capture_skip), color = theme.contentColor.copy(alpha = 0.6f))
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (uiState.currentStep < 8) {
                                viewModel.nextStep()
                            } else {
                                // v12.6 CORRECTIF : le souvenir est TOUJOURS créé en type TEXT ; tous
                                // les médias (y compris le premier) deviennent des compléments, jamais
                                // le média principal — pour un placement uniforme dans "Compléments Média".
                                captureViewModel.saveEntry(
                                    content = uiState.story.ifBlank { uiState.title },
                                    type = "TEXT",
                                    category = uiState.category,
                                    tonalNuance = uiState.tonalNuance,
                                    visibility = uiState.visibility,
                                    recipientIds = uiState.selectedRecipientIds,
                                    locationId = uiState.locationId,
                                    locationName = uiState.locationName,
                                    enigmaQuestion = if (uiState.enigmaEnabled) uiState.enigmaQuestion else null,
                                    enigmaAnswer = if (uiState.enigmaEnabled) uiState.enigmaAnswer else null,
                                    enigmaHint = if (uiState.enigmaEnabled) uiState.enigmaHint else null,
                                    enigmaAutoUnlockDays = if (uiState.enigmaEnabled) uiState.autoUnlockDays else null,
                                    includeInBook = uiState.includeInBook,
                                    onSuccess = { entryId ->
                                        uiState.mediaAttachments.forEach { (file, type) ->
                                            captureViewModel.addMediaComplement(entryId, file, type)
                                        }
                                        // v12.6 : Ajouter les liens externes (Spotify/Deezer/YouTube) en attente
                                        uiState.pendingLinks.forEach { link ->
                                            captureViewModel.addLinkComplement(
                                                parentId = entryId,
                                                provider = link.provider,
                                                title = link.title,
                                                comment = link.comment,
                                                url = link.url,
                                                recipientUids = uiState.selectedRecipientIds,
                                                visibility = uiState.visibility,
                                                thumbnailUrl = link.thumbnailUrl
                                            )
                                        }
                                        onNavigateBack()
                                    }
                                )
                            }
                        },
                        enabled = when (uiState.currentStep) {
                            1 -> uiState.title.isNotBlank()
                            8 -> uiState.story.isNotBlank() && captureState !is CaptureUiState.Loading
                            else -> captureState !is CaptureUiState.Loading
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (captureState is CaptureUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.backgroundColor, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (uiState.currentStep < 8) stringResource(R.string.step_capture_next) else stringResource(R.string.capture_audio_button_save),
                                color = theme.backgroundColor, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.currentStep) {
                1 -> StepEtincelle(uiState.title, { viewModel.updateTitle(it) }, theme, accent)
                2 -> StepTonalite(
                    uiState.category,
                    uiState.tonalNuance,
                    { viewModel.updateCategory(it) },
                    { viewModel.updateTonalNuance(it) },
                    theme,
                    accent
                )
                3 -> StepDate(
                    isPeriodMode = uiState.isPeriodMode,
                    memoryDate = uiState.memoryDate,
                    memoryDateStart = uiState.memoryDateStart,
                    memoryDateEnd = uiState.memoryDateEnd,
                    onDateChange = { viewModel.updateMemoryDate(it) },
                    onPeriodChange = { s, e -> viewModel.updateMemoryPeriod(s, e) },
                    onTogglePeriod = { viewModel.togglePeriodMode(it) },
                    theme = theme,
                    accent = accent
                )
                4 -> StepLieu(
                    locationName = uiState.locationName,
                    onChooseLocation = onNavigateToMap,
                    onClearLocation = { viewModel.setLocation(null) },
                    onUseCurrentLocation = { onUseCurrentLocationClick() },
                    isLocatingGps = isLocatingGps,
                    theme = theme,
                    accent = accent
                )
                5 -> StepEnigma(
                    isEnabled = uiState.enigmaEnabled,
                    onToggleEnabled = { viewModel.updateEnigmaEnabled(it) },
                    question = uiState.enigmaQuestion,
                    onQuestionChange = { viewModel.updateEnigmaQuestion(it) },
                    answer = uiState.enigmaAnswer,
                    onAnswerChange = { viewModel.updateEnigmaAnswer(it) },
                    hint = uiState.enigmaHint,
                    onHintChange = { viewModel.updateEnigmaHint(it) },
                    autoUnlockDays = uiState.autoUnlockDays,
                    onAutoUnlockDaysChange = { viewModel.updateAutoUnlockDays(it) },
                    answerType = uiState.answerType,
                    onAnswerTypeChange = { viewModel.updateAnswerType(it) },
                    expectedWordCount = uiState.expectedWordCount,
                    onExpectedWordCountChange = { viewModel.updateExpectedWordCount(it) },
                    includeInBook = uiState.includeInBook,
                    onIncludeInBookToggle = { viewModel.updateIncludeInBook(it) },
                    theme = theme,
                    accent = accent
                )
                6 -> StepDestinataires(
                    recipients = recipients,
                    selectedIds = uiState.selectedRecipientIds,
                    visibility = uiState.visibility,
                    onToggleRecipient = { viewModel.toggleRecipient(it) },
                    onVisibilityChange = { viewModel.updateVisibility(it) },
                    theme = theme,
                    accent = accent
                )
                7 -> StepMedias(
                    attachments = uiState.mediaAttachments,
                    onAddMedia = { file, type -> viewModel.addMediaAttachment(file, type) },
                    onRemoveMedia = { viewModel.removeMediaAttachment(it) },
                    pendingLinks = uiState.pendingLinks,
                    onAddLink = { viewModel.addPendingLink(it) },
                    onRemoveLink = { viewModel.removePendingLink(it) },
                    recipients = recipients,
                    captureViewModel = captureViewModel, // v12.5 : Pour l'audio
                    theme = theme,
                    accent = accent
                )
                8 -> StepRecit(
                    title = uiState.title,
                    story = uiState.story,
                    onStoryChange = { viewModel.updateStory(it) },
                    theme = theme,
                    accent = accent
                )
            }
        }
    }
}

/**
 * Étape 8 : le Récit.
 * RECONSTRUIT le 18/09 — ce composant avait entièrement disparu du fichier alors
 * qu'il était toujours appelé à l'étape 8. Reconstruit à l'identique de la
 * signature attendue par l'appelant, dans le style des autres étapes.
 * Utilise des clés de texte déjà existantes (aucune nouvelle traduction requise).
 */
@Composable
fun StepRecit(
    title: String,
    story: String,
    onStoryChange: (String) -> Unit,
    theme: AppThemeState,
    accent: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.memory_detail_story_editor_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = accent
        )

        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = story,
            onValueChange = onStoryChange,
            placeholder = {
                Text(
                    stringResource(R.string.memory_detail_story_editor_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.contentColor.copy(alpha = 0.3f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = theme.contentColor,
                fontFamily = theme.fontFamily
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = theme.contentColor.copy(alpha = 0.1f),
                focusedTextColor = theme.contentColor,
                unfocusedTextColor = theme.contentColor
            )
        )
    }
}

@Composable
fun StepDestinataires(
    recipients: List<com.example.phoenx.data.local.RecipientEntity>,
    selectedIds: List<String>,
    visibility: String,
    onToggleRecipient: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    theme: AppThemeState,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.capture_compartments_label),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = accent
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        RecipientSelector(
            recipients = recipients,
            selectedIds = selectedIds,
            onToggleRecipient = onToggleRecipient,
            visibility = visibility,
            onVisibilityChange = onVisibilityChange,
            accent = accent
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepMedias(
    attachments: List<Pair<java.io.File, String>>,
    onAddMedia: (java.io.File, String) -> Unit,
    onRemoveMedia: (Int) -> Unit,
    pendingLinks: List<PendingLink> = emptyList(), // v12.6
    onAddLink: (PendingLink) -> Unit = {}, // v12.6
    onRemoveLink: (Int) -> Unit = {}, // v12.6
    recipients: List<com.example.phoenx.data.local.RecipientEntity> = emptyList(), // v12.6
    captureViewModel: CaptureViewModel,
    theme: AppThemeState,
    accent: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val captureState by captureViewModel.uiState.collectAsState()
    val isRecording = captureState is CaptureUiState.RecordingAudio
    var addingLinkProvider by remember { mutableStateOf<String?>(null) } // v12.6

    // v12.6 : L'enregistrement audio exige la permission RECORD_AUDIO au runtime.
    // Sans cette vérification, MediaRecorder.setAudioSource() plante l'application
    // si la permission n'a jamais été accordée (ex: jamais utilisé la dictée vocale).
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            captureViewModel.startAudioRecording(context.cacheDir)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.capture_photo_label_camera),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = accent
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Liste des médias déjà ajoutés
        if (attachments.isNotEmpty()) {
            attachments.forEachIndexed { index, (file, type) ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.contentColor.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when(type) {
                                "PHOTO" -> Icons.Default.Photo
                                "VIDEO" -> Icons.Default.Videocam
                                "AUDIO" -> Icons.Default.Mic
                                else -> Icons.Default.Attachment
                            },
                            null,
                            tint = accent
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(file.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = theme.contentColor, maxLines = 1)
                        IconButton(onClick = { onRemoveMedia(index) }) {
                            Icon(Icons.Default.Delete, null, tint = Error.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // v12.6 : Liste des liens externes déjà ajoutés
        if (pendingLinks.isNotEmpty()) {
            pendingLinks.forEachIndexed { index, link ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.contentColor.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (link.provider == "YOUTUBE") Icons.Default.OndemandVideo else Icons.Default.MusicNote,
                            null,
                            tint = accent
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(link.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = theme.contentColor, maxLines = 1)
                        IconButton(onClick = { onRemoveLink(index) }) {
                            Icon(Icons.Default.Delete, null, tint = Error.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val file = captureViewModel.uriToFile(it)
                if (file != null) onAddMedia(file, "PHOTO")
            }
        }

        val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                // v12.6 CORRECTIF : la limite de durée (1min30) existait déjà pour une vidéo
                // ajoutée à un souvenir existant (MemoryComplementsSection), mais manquait ici,
                // dans l'Étape par Étape — une vidéo de n'importe quelle durée passait sans contrôle.
                val isValid = com.example.phoenx.ui.util.VideoUtils.isVideoDurationValid(
                    context, it, com.example.phoenx.ui.util.VideoUtils.MAX_VIDEO_DURATION_SECONDS_STANDARD
                )
                if (isValid) {
                    val file = captureViewModel.uriToFile(it)
                    if (file != null) onAddMedia(file, "VIDEO")
                } else {
                    android.widget.Toast.makeText(context, context.getString(R.string.memory_complements_error_video_too_long_toast), android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }

        fun toggleAudioRecording() {
            if (isRecording) {
                val file = captureViewModel.stopAudioRecording()
                if (file != null) onAddMedia(file, "AUDIO")
            } else {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    captureViewModel.startAudioRecording(context.cacheDir)
                } else {
                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        // v12.6 : Un enregistrement en cours reste toujours visible et accessible en un geste,
        // même quand le menu déroulant n'est pas ouvert.
        if (isRecording) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { toggleAudioRecording() },
                shape = RoundedCornerShape(16.dp),
                color = Error.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Error.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Stop, null, tint = Error)
                    Spacer(Modifier.width(12.dp))
                    Text("Arrêter l'enregistrement vocal", color = Error, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // v12.6 : Menu déroulant unique "Ajouter" — plus lisible qu'une rangée d'icônes seules
            var showAddMenu by remember { mutableStateOf(false) }
            Box {
                Surface(
                    modifier = Modifier.clickable { showAddMenu = true },
                    shape = RoundedCornerShape(24.dp),
                    color = accent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddCircle, null, tint = accent)
                        Spacer(Modifier.width(10.dp))
                        Text("Ajouter un média ou un lien", color = accent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowDropDown, null, tint = accent)
                    }
                }

                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                    containerColor = theme.backgroundColor
                ) {
                    Text(
                        "MÉDIAS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = theme.contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Photo", color = theme.contentColor) },
                        leadingIcon = { Icon(Icons.Default.AddAPhoto, null, tint = accent) },
                        onClick = { showAddMenu = false; photoLauncher.launch("image/*") }
                    )
                    DropdownMenuItem(
                        text = { Text("Vidéo", color = theme.contentColor) },
                        leadingIcon = { Icon(Icons.Default.VideoCall, null, tint = accent) },
                        onClick = { showAddMenu = false; videoLauncher.launch("video/*") }
                    )
                    DropdownMenuItem(
                        text = { Text("Enregistrement vocal", color = theme.contentColor) },
                        leadingIcon = { Icon(Icons.Default.Mic, null, tint = accent) },
                        onClick = { showAddMenu = false; toggleAudioRecording() }
                    )

                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f))

                    Text(
                        "LIENS EXTERNES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = theme.contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Spotify", color = theme.contentColor) },
                        leadingIcon = { Icon(Icons.Default.MusicNote, null, tint = accent) },
                        onClick = { showAddMenu = false; addingLinkProvider = "SPOTIFY" }
                    )
                    DropdownMenuItem(
                        text = { Text("Deezer", color = theme.contentColor) },
                        leadingIcon = { Icon(Icons.Default.LibraryMusic, null, tint = accent) },
                        onClick = { showAddMenu = false; addingLinkProvider = "DEEZER" }
                    )
                    DropdownMenuItem(
                        text = { Text("YouTube", color = theme.contentColor) },
                        leadingIcon = { Icon(Icons.Default.OndemandVideo, null, tint = accent) },
                        onClick = { showAddMenu = false; addingLinkProvider = "YOUTUBE" }
                    )
                }
            }
        }

        Text(
            text = "Ajoutez autant de photos, vidéos, vocaux ou liens que vous le souhaitez",
            style = MaterialTheme.typography.labelSmall,
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    // v12.6 : DIALOGUE D'AJOUT D'UN LIEN EXTERNE
    if (addingLinkProvider != null) {
        com.example.phoenx.ui.components.DirectMediaDialog(
            type = addingLinkProvider!!,
            recipients = recipients,
            onDismiss = { addingLinkProvider = null },
            onSave = { title, comment, url, _, _, thumbUrl, _ ->
                onAddLink(
                    PendingLink(
                        provider = addingLinkProvider!!,
                        title = title,
                        comment = comment,
                        url = url,
                        thumbnailUrl = thumbUrl
                    )
                )
                addingLinkProvider = null
            }
        )
    }
}

@Composable
fun StepEnigma(
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    question: String,
    onQuestionChange: (String) -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    hint: String,
    onHintChange: (String) -> Unit,
    autoUnlockDays: Int?,
    onAutoUnlockDaysChange: (Int?) -> Unit,
    answerType: String = "WORD",
    onAnswerTypeChange: (String) -> Unit = {},
    expectedWordCount: Int? = 1,
    onExpectedWordCountChange: (Int?) -> Unit = {},
    includeInBook: Boolean, // v9.4.27
    onIncludeInBookToggle: (Boolean) -> Unit, // v9.4.27
    theme: AppThemeState,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.step_capture_vault_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = accent
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        EnigmaForm(
            isEnabled = isEnabled,
            onToggleEnabled = onToggleEnabled,
            question = question,
            onQuestionChange = onQuestionChange,
            answer = answer,
            onAnswerChange = onAnswerChange,
            hint = hint,
            onHintChange = onHintChange,
            autoUnlockDays = autoUnlockDays,
            onAutoUnlockDaysChange = onAutoUnlockDaysChange,
            answerType = answerType,
            onAnswerTypeChange = onAnswerTypeChange,
            expectedWordCount = expectedWordCount,
            onExpectedWordCountChange = onExpectedWordCountChange,
            theme = theme,
            accent = accent,
            isReadOnly = false
        )

        Spacer(modifier = Modifier.height(40.dp))
        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Option Souveraineté (v9.4.27)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIncludeInBookToggle(!includeInBook) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = includeInBook,
                onCheckedChange = { onIncludeInBookToggle(it) },
                colors = CheckboxDefaults.colors(checkedColor = accent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.step_capture_book_label), 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Text(
                    stringResource(R.string.step_capture_book_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun StepLieu(
    locationName: String?,
    onChooseLocation: () -> Unit,
    onClearLocation: () -> Unit,
    onUseCurrentLocation: () -> Unit = {}, // v13.0 : Géolocalisation directe
    isLocatingGps: Boolean = false,
    theme: AppThemeState,
    accent: Color
) {
    Text(
        text = stringResource(R.string.step_capture_location_title),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = accent
    )
    Text(
        text = stringResource(R.string.step_capture_location_desc),
        style = MaterialTheme.typography.bodySmall,
        color = theme.contentColor.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChooseLocation() }
            .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (locationName != null) Icons.Default.LocationOn else Icons.Default.AddLocation,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = locationName ?: stringResource(R.string.step_capture_location_choose),
                color = if (locationName != null) theme.contentColor else theme.contentColor.copy(alpha = 0.5f),
                fontWeight = if (locationName != null) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (locationName == null) {
        // v13.0 : Alternative à la sélection manuelle sur la Mappemonde — géolocalisation directe.
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = theme.contentColor.copy(alpha = 0.1f))
            Text(
                text = stringResource(R.string.step_capture_location_or),
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = theme.contentColor.copy(alpha = 0.1f))
        }

        TextButton(onClick = onUseCurrentLocation, enabled = !isLocatingGps) {
            if (isLocatingGps) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.step_capture_location_locating), color = accent, style = MaterialTheme.typography.labelMedium)
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.step_capture_location_use_gps), color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (locationName != null) {
        TextButton(
            onClick = onClearLocation,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.step_capture_location_remove), color = Error.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun StepEtincelle(title: String, onTitleChange: (String) -> Unit, theme: AppThemeState, accent: Color) {
    Text(
        text = stringResource(R.string.step_capture_spark_title),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = accent
    )
    Text(
        text = stringResource(R.string.step_capture_spark_desc),
        style = MaterialTheme.typography.bodySmall,
        color = theme.contentColor.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
    )

    TextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = {
            Text(
                stringResource(R.string.step_capture_spark_placeholder),
                style = MaterialTheme.typography.headlineSmall,
                color = theme.contentColor.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            color = theme.contentColor,
            fontFamily = theme.fontFamily,
            textAlign = TextAlign.Center
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = theme.contentColor.copy(alpha = 0.1f),
            focusedTextColor = theme.contentColor,
            unfocusedTextColor = theme.contentColor
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepTonalite(
    selectedCategory: String, 
    tonalNuance: String,
    onCategoryChange: (String) -> Unit, 
    onNuanceChange: (String) -> Unit,
    theme: AppThemeState, 
    accent: Color
) {
    Text(
        text = stringResource(R.string.step_capture_tonality_title),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = accent
    )
    Text(
        text = stringResource(R.string.step_capture_tonality_desc),
        style = MaterialTheme.typography.bodySmall,
        color = theme.contentColor.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
    )

    val categories = androidx.compose.ui.res.stringArrayResource(R.array.tonality_categories)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = selectedCategory == cat,
                onClick = { onCategoryChange(cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = theme.backgroundColor
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    
    OutlinedTextField(
        value = tonalNuance,
        onValueChange = { if (it.length <= 100) onNuanceChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.capture_tonality_nuance_label), fontSize = 11.sp) },
        placeholder = { Text(stringResource(R.string.capture_tonality_nuance_placeholder), fontSize = 11.sp) },
        maxLines = 3,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.copy(alpha = 0.5f),
            unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
            focusedTextColor = theme.contentColor,
            unfocusedTextColor = theme.contentColor
        ),
        supportingText = {
            Text("${tonalNuance.length}/100", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 10.sp)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepDate(
    isPeriodMode: Boolean,
    memoryDate: Long?,
    memoryDateStart: Long?,
    memoryDateEnd: Long?,
    onDateChange: (Long?) -> Unit,
    onPeriodChange: (Long?, Long?) -> Unit,
    onTogglePeriod: (Boolean) -> Unit,
    theme: AppThemeState,
    accent: Color
) {
    Text(
        text = stringResource(R.string.step_capture_moment_title),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = accent
    )
    Text(
        text = stringResource(R.string.step_capture_moment_desc),
        style = MaterialTheme.typography.bodySmall,
        color = theme.contentColor.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Text(stringResource(R.string.step_capture_date_precise), style = MaterialTheme.typography.labelSmall, color = if (!isPeriodMode) accent else theme.contentColor.copy(alpha = 0.4f))
        Switch(
            checked = isPeriodMode,
            onCheckedChange = onTogglePeriod,
            modifier = Modifier.scale(0.7f).padding(horizontal = 8.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = accent)
        )
        Text(stringResource(R.string.step_capture_date_period), style = MaterialTheme.typography.labelSmall, color = if (isPeriodMode) accent else theme.contentColor.copy(alpha = 0.4f))
    }

    if (!isPeriodMode) {
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = memoryDate ?: System.currentTimeMillis())

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            color = theme.contentColor.copy(alpha = 0.03f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                val dateFormat = stringResource(R.string.capture_date_format)
                val dateText = memoryDate?.let {
                    SimpleDateFormat(dateFormat, Locale.FRENCH).format(Date(it))
                } ?: stringResource(R.string.step_capture_date_placeholder)
                Text(dateText, color = theme.contentColor, fontWeight = FontWeight.Bold)
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onDateChange(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) { Text(stringResource(R.string.step_capture_date_confirm), color = accent) }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = theme.backgroundColor,
                        titleContentColor = theme.contentColor,
                        headlineContentColor = theme.contentColor,
                        selectedDayContainerColor = accent,
                        selectedDayContentColor = theme.backgroundColor,
                        todayContentColor = accent,
                        todayDateBorderColor = accent
                    )
                )
            }
        }
    } else {
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }

        val startState = rememberDatePickerState(initialSelectedDateMillis = memoryDateStart ?: System.currentTimeMillis())
        val endState = rememberDatePickerState(initialSelectedDateMillis = memoryDateEnd ?: System.currentTimeMillis())

        val dateFormat = stringResource(R.string.capture_date_format)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                val txt = memoryDateStart?.let { SimpleDateFormat(dateFormat, Locale.FRENCH).format(Date(it)) } ?: stringResource(R.string.step_capture_date_start)
                Text(txt, color = theme.contentColor)
            }

            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                val txt = memoryDateEnd?.let { SimpleDateFormat(dateFormat, Locale.FRENCH).format(Date(it)) } ?: stringResource(R.string.step_capture_date_end)
                Text(txt, color = theme.contentColor)
            }
        }

        if (showStartPicker) {
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onPeriodChange(startState.selectedDateMillis, memoryDateEnd)
                        showStartPicker = false
                    }) { Text(stringResource(R.string.step_capture_date_confirm), color = accent) }
                }
            ) {
                DatePicker(
                    state = startState,
                    colors = DatePickerDefaults.colors(
                        containerColor = theme.backgroundColor,
                        titleContentColor = theme.contentColor,
                        headlineContentColor = theme.contentColor,
                        selectedDayContainerColor = accent,
                        selectedDayContentColor = theme.backgroundColor,
                        todayContentColor = accent,
                        todayDateBorderColor = accent
                    )
                )
            }
        }
        if (showEndPicker) {
            DatePickerDialog(
                onDismissRequest = { showEndPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onPeriodChange(memoryDateStart, endState.selectedDateMillis)
                        showEndPicker = false
                    }) { Text(stringResource(R.string.step_capture_date_confirm), color = accent) }
                }
            ) {
                DatePicker(
                    state = endState,
                    colors = DatePickerDefaults.colors(
                        containerColor = theme.backgroundColor,
                        titleContentColor = theme.contentColor,
                        headlineContentColor = theme.contentColor,
                        selectedDayContainerColor = accent,
                        selectedDayContentColor = theme.backgroundColor,
                        todayContentColor = accent,
                        todayDateBorderColor = accent
                    )
                )
            }
        }
    }
}
