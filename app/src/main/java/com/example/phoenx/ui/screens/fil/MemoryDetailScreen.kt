package com.example.phoenx.ui.screens.fil

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailScreen(
    entryId: String,
    onNavigateBack: () -> Unit,
    navController: NavController,
    targetCreatorId: String? = null,
    triggerAction: String? = null, // v9.4.27
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsState()
    val complements by viewModel.complements.collectAsState()
    val textComplements by viewModel.decryptedTextComplements.collectAsState()
    val content by viewModel.decryptedContent.collectAsState()
    val structuredPortrait by viewModel.structuredPortrait.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isVoiceNoteOverlayOpen by viewModel.isVoiceNoteOverlayOpen.collectAsState() // v9.4.27
    val sttPartialText by viewModel.sttPartialText.collectAsState() 
    
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // v9.4.27 : Gestion des fichiers temporaires pour la caméra
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher PHOTO (Caméra)
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            val file = viewModel.uriToFile(tempPhotoUri!!)
            if (file != null) viewModel.addMediaComplement(entryId, file, "PHOTO")
        }
    }

    // Launcher VIDÉO (Caméra)
    val recordVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoUri != null) {
            val isValid = com.example.phoenx.ui.util.VideoUtils.isVideoDurationValid(
                context, tempVideoUri!!, com.example.phoenx.ui.util.VideoUtils.MAX_VIDEO_DURATION_SECONDS_STANDARD
            )
            if (isValid) {
                val file = viewModel.uriToFile(tempVideoUri!!)
                if (file != null) viewModel.addMediaComplement(entryId, file, "VIDEO")
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.memory_detail_error_video_too_long_snackbar))
                }
            }
        }
    }

    // Gestion des permissions (v9.4.27)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    val checkCameraPermission = { onGranted: () -> Unit ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    val requestAudioPermission = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.openVoiceNoteOverlay()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    val startCameraPhoto = {
        checkCameraPermission {
            val photoFile = java.io.File(context.cacheDir, "PHX_CAM_${UUID.randomUUID()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempPhotoUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    val startCameraVideo = {
        checkCameraPermission {
            val videoFile = java.io.File(context.cacheDir, "PHX_CAM_${UUID.randomUUID()}.mp4")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            tempVideoUri = uri
            recordVideoLauncher.launch(uri)
        }
    }

    val cleanCreatorId = targetCreatorId?.takeIf { it.isNotBlank() && !it.startsWith("{") && it != "null" }
    android.util.Log.d("PHOENX_ATELIER_TRACE", "MemoryDetailScreen: brut=[$targetCreatorId] nettoye=[$cleanCreatorId]")
    
    val isReadOnly = cleanCreatorId != null && cleanCreatorId != com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    // Sélecteur MULTIPLE (v9.4.26)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val mime = context.contentResolver.getType(uri)
                val type = if (mime?.contains("video") == true) "VIDEO" else "PHOTO"
                
                if (type == "VIDEO") {
                    val isValid = com.example.phoenx.ui.util.VideoUtils.isVideoDurationValid(
                        context, uri, com.example.phoenx.ui.util.VideoUtils.MAX_VIDEO_DURATION_SECONDS_STANDARD
                    )
                    if (isValid) {
                        val file = viewModel.uriToFile(uri)
                        if (file != null) viewModel.addMediaComplement(entryId, file, "VIDEO")
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.memory_detail_error_video_too_long_snackbar))
                        }
                    }
                } else {
                    val file = viewModel.uriToFile(uri)
                    if (file != null) viewModel.addMediaComplement(entryId, file, "PHOTO")
                }
            }
        }
    }

    // DÉCLENCHEMENT AUTOMATIQUE DES ACTIONS (v9.4.27)
    LaunchedEffect(triggerAction, entry) {
        if (entry != null && triggerAction != null && !isReadOnly) {
            when (triggerAction) {
                "PHOTO", "GALLERY" -> {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
                "AUDIO" -> { requestAudioPermission() }
                Screen.Capture.TYPE_CAMERA_PHOTO -> { startCameraPhoto() }
                Screen.Capture.TYPE_CAMERA_VIDEO -> { startCameraVideo() }
            }
        }
    }

    val pickedLocationId by navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("pickedLocationId", null)?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(pickedLocationId) {
        pickedLocationId?.let { id ->
            android.util.Log.d("PHOENX_LOCATION_TRACE", "SavedStateHandle recu: id=$id")
            viewModel.assignLocationFromId(id)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("pickedLocationId")
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmStep by remember { mutableIntStateOf(1) }
    var editableTitle by remember { mutableStateOf("") }
    var editableText by remember { mutableStateOf("") }
    var isStoryEditorOpen by remember { mutableStateOf(false) }
    var isTonaliteExpanded by remember { mutableStateOf(false) } // v9.4.27 : Déplacé ici

    LaunchedEffect(entryId, cleanCreatorId) {
        viewModel.loadEntry(entryId, cleanCreatorId)
    }

    LaunchedEffect(entry, content) {
        if (entry != null) {
            if (editableTitle.isEmpty()) editableTitle = entry!!.userTitle.ifBlank { entry!!.aiSummary }
            if (editableText.isEmpty() || entry!!.parentEntryId != null) {
                editableText = content
            }
        }
    }

    LaunchedEffect(editableTitle) {
        if (!isReadOnly && entry != null && !entry!!.isChild() && entry!!.entryType != "QUESTION_ANSWER") {
            if (editableTitle.isNotEmpty() && editableTitle != entry!!.userTitle.ifBlank { entry!!.aiSummary }) {
                delay(1000)
                viewModel.updateTitle(editableTitle)
            }
        }
    }

    LaunchedEffect(editableText) {
        if (!isReadOnly && editableText.isNotEmpty() && editableText != content) {
            delay(1000)
            viewModel.updateContent(editableText)
        }
    }

    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) onNavigateBack()
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; deleteConfirmStep = 1 },
            containerColor = theme.backgroundColor,
            title = { Text(if (deleteConfirmStep == 1) stringResource(R.string.memory_detail_delete_dialog_title) else stringResource(R.string.memory_detail_delete_confirm_title), color = theme.contentColor) },
            text = {
                if (deleteConfirmStep == 1) {
                    val mediaCount = complements.count { it.entryType != "TEXT" }
                    val message = if (mediaCount > 0) stringResource(R.string.memory_detail_delete_has_media, mediaCount) else stringResource(R.string.memory_detail_delete_no_media)
                    Text(message, color = theme.contentColor.copy(alpha = 0.7f))
                } else {
                    Text(stringResource(R.string.memory_detail_delete_warning_permanent), color = Error)
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (deleteConfirmStep == 1) deleteConfirmStep = 2
                        else { viewModel.deleteMemory(); showDeleteDialog = false }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (deleteConfirmStep == 1) accent else Error)
                ) { Text(if (deleteConfirmStep == 1) stringResource(R.string.memory_detail_delete_button_continue) else stringResource(R.string.memory_detail_delete_button_permanent), color = theme.backgroundColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; deleteConfirmStep = 1 }) { Text(stringResource(R.string.memory_detail_delete_button_cancel), color = theme.contentColor) }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!isStoryEditorOpen) {
                TopAppBar(
                    title = { 
                        val titleText = when(entry?.entryType) {
                            "PORTRAIT" -> entry?.aiSummary ?: stringResource(R.string.memory_detail_topbar_portrait)
                            "QUESTION_ANSWER" -> stringResource(R.string.memory_detail_topbar_question, entry?.aiSummary ?: "")
                            else -> if (entry?.parentEntryId != null) stringResource(R.string.memory_detail_topbar_portrait_response) else stringResource(R.string.memory_detail_topbar_spark_story)
                        }
                        Text(titleText, style = MaterialTheme.typography.labelLarge, color = theme.contentColor) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor) }
                    },
                    actions = {
                        if (!isReadOnly) {
                            IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.memory_detail_delete_button_delete), tint = Error) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        if (entry == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
        } else {
            val isChildEntry = entry!!.parentEntryId != null

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    // ── SECTION 1 : L'ESSENTIEL ────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.memory_detail_essential_section), 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp), 
                            color = Color.Black, 
                            letterSpacing = 2.sp
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = theme.contentColor.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.35f)) // Bordure renforcée v9.4.27
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                if (entry!!.entryType != "PORTRAIT") {
                                    val subjectLabel = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") stringResource(R.string.memory_detail_question_label) else stringResource(R.string.memory_detail_subject_label)
                                    Column {
                                        Text(subjectLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER" || isReadOnly) {
                                                    val essentialText = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") {
                                                        entry!!.aiSummary
                                                    } else {
                                                        entry!!.userTitle.ifBlank { entry!!.aiSummary }
                                                    }
                                                    Text(
                                                        text = essentialText,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontFamily = theme.fontFamily,
                                                            fontStyle = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") FontStyle.Italic else null,
                                                            fontWeight = if (isReadOnly && !isChildEntry && entry!!.entryType != "QUESTION_ANSWER") FontWeight.Bold else null,
                                                            color = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") accent else theme.contentColor
                                                        )
                                                    )
                                                } else {
                                                    TextField(
                                                        value = editableTitle,
                                                        onValueChange = { editableTitle = it },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = { Text(stringResource(R.string.memory_detail_subject_placeholder), color = theme.contentColor.copy(alpha = 0.3f)) },
                                                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, color = theme.contentColor),
                                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // IMAGE PRINCIPALE (v12.3 : Pour Questions de vie)
                                if (entry!!.mediaUrl != null || entry!!.localMediaPath != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(theme.contentColor.copy(alpha = 0.05f))
                                    ) {
                                        com.example.phoenx.ui.components.SecureAsyncImage(
                                            mediaUrl = entry!!.mediaUrl,
                                            localPath = entry!!.localMediaPath,
                                            mediaManager = viewModel.mediaManager,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            docType = "entries",
                                            docId = entry!!.id
                                        )
                                    }
                                }

                                Column {
                                    val récitLabel = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") stringResource(R.string.memory_detail_response_label) else stringResource(R.string.memory_detail_story_label)
                                    Text(récitLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { isStoryEditorOpen = true },
                                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            text = editableText.ifEmpty { stringResource(R.string.memory_detail_story_placeholder) },
                                            modifier = Modifier.padding(16.dp),
                                            maxLines = 4, // v9.4.27 : Troncature stricte 3-5 lignes
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp, color = if (editableText.isEmpty()) theme.contentColor.copy(alpha = 0.4f) else theme.contentColor)
                                        )
                                    }
                                }

                                // TONALITÉ (v9.4.27 : Déplacé dans L'ESSENTIEL)
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.memory_detail_tonality_label), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                                        Spacer(Modifier.width(8.dp))
                                        com.example.phoenx.ui.components.InfoPoint(
                                            title = stringResource(R.string.memory_detail_tonality_info_title),
                                            content = stringResource(R.string.memory_detail_tonality_info_content)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isTonaliteExpanded = !isTonaliteExpanded }
                                            .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                        color = theme.contentColor.copy(alpha = 0.03f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = entry!!.emotionalCategory, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                                            Icon(imageVector = if (isTonaliteExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = theme.contentColor.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    AnimatedVisibility(visible = isTonaliteExpanded) {
                                        Column {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            val categories = stringArrayResource(R.array.tonality_categories)
                                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                categories.forEach { cat ->
                                                    val isSelected = entry!!.emotionalCategory == cat
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = { 
                                                            android.util.Log.d("PHOENX_CLICK_TRACE", "Clic tonalite recu, cat=$cat, isReadOnly=$isReadOnly, entryNull=${entry == null}")
                                                            if (!isReadOnly) viewModel.updateCategory(cat) 
                                                        },
                                                        label = { Text(cat) },
                                                        enabled = !isReadOnly || isSelected,
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = accent,
                                                            selectedLabelColor = theme.backgroundColor,
                                                            containerColor = theme.contentColor.copy(alpha = 0.05f),
                                                            labelColor = theme.contentColor.copy(alpha = 0.6f)
                                                        ),
                                                        border = BorderStroke(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
                                                    )
                                                }
                                            }

                                            // CHAMP NUANCE LIBRE (v9.4.27)
                                            Spacer(modifier = Modifier.height(24.dp))
                                            var nuanceText by remember(entry!!.tonalNuance) { mutableStateOf(entry!!.tonalNuance ?: "") }
                                            OutlinedTextField(
                                                value = nuanceText,
                                                onValueChange = { if (it.length <= 100) { nuanceText = it; viewModel.updateTonalNuance(it) } },
                                                modifier = Modifier.fillMaxWidth(),
                                                label = { Text(stringResource(R.string.memory_detail_tonality_nuance_label), fontSize = 11.sp) },
                                                placeholder = { Text(stringResource(R.string.memory_detail_tonality_nuance_placeholder), fontSize = 11.sp) },
                                                maxLines = 3,
                                                enabled = !isReadOnly,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = accent.copy(alpha = 0.5f),
                                                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                                                    focusedTextColor = theme.contentColor,
                                                    unfocusedTextColor = theme.contentColor
                                                ),
                                                supportingText = {
                                                    Text("${nuanceText.length}/100", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 10.sp)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isChildEntry) {
                        MemoryMetadataSection(
                            entry = entry!!,
                            viewModel = viewModel,
                            theme = theme,
                            accent = accent,
                            navController = navController,
                            recipients = recipients,
                            isReadOnly = isReadOnly
                        )

                        // ── SECTION 5 : COMPLÉMENTS MÉDIA ────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.memory_detail_complements_section),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp), 
                            color = Color.Black, 
                            letterSpacing = 2.sp
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = theme.contentColor.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.35f))
                        ) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    MemoryComplementsSection(
                                        entryId = entryId,
                                        complements = complements,
                                        targetCreatorId = cleanCreatorId,
                                        viewModel = viewModel,
                                        theme = theme,
                                        accent = accent,
                                        navController = navController,
                                        isReadOnly = isReadOnly,
                                        onStartAudioRecording = requestAudioPermission,
                                        onStartCameraPhoto = startCameraPhoto,
                                        onStartCameraVideo = startCameraVideo
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(60.dp))
                }

                if (isVoiceNoteOverlayOpen) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).shadow(20.dp, RoundedCornerShape(24.dp)),
                        color = theme.backgroundColor,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            if (!isRecording) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Mic, null, tint = accent.copy(alpha = 0.4f))
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.memory_detail_voice_note_ready), color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                                }
                                Row {
                                    TextButton(onClick = { viewModel.closeVoiceNoteOverlay() }) { Text(stringResource(R.string.memory_detail_delete_button_cancel), color = theme.contentColor.copy(alpha = 0.4f)) }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = { viewModel.startAudioRecording() }, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Default.PlayArrow, null, tint = theme.backgroundColor)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.memory_detail_voice_note_button_start), color = theme.backgroundColor)
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.size(12.dp).background(Color.Red, CircleShape))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(stringResource(R.string.memory_detail_voice_note_recording), color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                                        if (sttPartialText.isNotEmpty()) {
                                            Text(text = sttPartialText, color = theme.contentColor, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                Button(onClick = { viewModel.stopAudioRecording(entryId) }, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(12.dp)) {
                                    Icon(Icons.Default.Stop, null, tint = theme.backgroundColor)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.memory_detail_voice_note_button_finish), color = theme.backgroundColor)
                                }
                            }
                        }
                    }
                }

                if (isStoryEditorOpen) {
                    Surface(
                        modifier = Modifier.fillMaxSize(), 
                        color = theme.backgroundColor
                    ) {
                        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // GAUCHE : Libellé de contexte
                                Text(
                                    text = stringResource(R.string.memory_detail_story_editor_title), 
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                                    color = accent
                                )
                                
                                // DROITE : Actions
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.updateContent(editableText); isStoryEditorOpen = false },
                                        modifier = Modifier.size(40.dp)
                                    ) { 
                                        Icon(Icons.Default.Check, stringResource(R.string.memory_metadata_date_confirm), tint = accent) 
                                    }
                                }
                            }

                            TextField(
                                value = editableText,
                                onValueChange = { editableText = it },
                                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                                placeholder = { Text(stringResource(R.string.memory_detail_story_editor_placeholder), color = theme.contentColor.copy(alpha = 0.3f)) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp, color = theme.contentColor),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
