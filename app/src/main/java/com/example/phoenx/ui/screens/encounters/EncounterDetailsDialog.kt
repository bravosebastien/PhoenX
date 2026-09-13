package com.example.phoenx.ui.screens.encounters

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.components.SecureAsyncImage
import dagger.hilt.android.EntryPointAccessors
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EncounterDetailsDialog(
    initialPerson: PersonEntity? = null,
    allPersons: List<PersonEntity>,
    navController: androidx.navigation.NavController,
    onConfirm: (PersonEntity) -> Unit,
    onDismiss: () -> Unit,
    onRemoveCategory: (PersonEntity) -> Unit,
    accent: Color,
    viewModel: EncounterViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current

    val heirKey by viewModel.heirKey.collectAsState()

    val mediaList by remember(initialPerson?.id) {
        if (initialPerson != null) viewModel.getMediaForPerson(initialPerson.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    
    // État local du formulaire
    var firstName by remember { mutableStateOf(initialPerson?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialPerson?.lastName ?: "") }
    var encounterBiography by remember { mutableStateOf(initialPerson?.encounterBiography ?: "") }
    var encounterAge by remember { mutableStateOf(initialPerson?.encounterAge?.toString() ?: "") }
    var encounterImagePath by remember { mutableStateOf(initialPerson?.encounterImagePath) }

    // Launcher Image Profil
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // v9.7.9 : Application de la compression unifiée (Modèle Souvenirs)
            val compressedFile = com.example.phoenx.ui.util.ImageUtils.compressAndResize(context, uri)
            if (compressedFile != null) {
                encounterImagePath = compressedFile.absolutePath
            }
        }
    }
    
    // Nature
    var linkNature by remember { mutableStateOf(initialPerson?.linkNature ?: "") }
    var linkNatureCustom by remember { 
        mutableStateOf(
            if (initialPerson?.linkNature != null && !listOf("Ami", "Partenaire", "Mentor", "Collègue", "Voisin").contains(initialPerson.linkNature))
                initialPerson.linkNature 
            else ""
        )
    }
    
    // Contexte
    var encounterContext by remember { mutableStateOf(initialPerson?.encounterContext) }
    var encounterContextLabel by remember { mutableStateOf(initialPerson?.encounterContextLabel ?: "") }
    var locationLabel by remember { mutableStateOf(initialPerson?.encounterLocationLabel ?: "") }
    
    // État du lien
    var linkStatus by remember { mutableStateOf(initialPerson?.linkStatus ?: "PRESENT") }
    var relationEndAge by remember { mutableStateOf(initialPerson?.relationEndAge?.toString() ?: "") }
    var relationEndReason by remember { mutableStateOf(initialPerson?.relationEndReason ?: "") }

    var introducedById by remember { mutableStateOf(initialPerson?.introducedById) }
    var isPrivate by remember { mutableStateOf(initialPerson?.visibility == "PRIVATE") }

    // Recherche pour "Présenté par"
    var query by remember { mutableStateOf("") }
    val suggestedIntroducers = if (query.isBlank()) emptyList() else allPersons.filter { 
        it.firstName.contains(query, ignoreCase = true) && it.id != initialPerson?.id
    }

    // Modal de confirmation de suppression
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteWarningMessage by remember { mutableStateOf("") }

    fun checkBeforeDelete() {
        if (initialPerson == null) return
        val introducedPersons = allPersons.filter { it.introducedById == initialPerson.id }
        if (introducedPersons.isNotEmpty()) {
            val names = introducedPersons.joinToString(", ") { it.firstName }
            deleteWarningMessage = context.getString(R.string.encounter_dialog_delete_warning_introducer, names)
        } else {
            val hasFamily = initialPerson.categories.contains("FAMILY")
            deleteWarningMessage = if (hasFamily) {
                context.getString(R.string.encounter_dialog_delete_warning_family)
            } else {
                context.getString(R.string.encounter_dialog_delete_warning_generic)
            }
        }
        showDeleteConfirm = true
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.encounter_dialog_attention)) },
            text = { Text(deleteWarningMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        initialPerson?.let { onRemoveCategory(it) }
                    }
                ) {
                    Text(stringResource(R.string.encounter_dialog_delete_confirm), color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.encounter_detail_cancel))
                }
            },
            containerColor = theme.backgroundColor,
            titleContentColor = theme.contentColor,
            textContentColor = theme.contentColor
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .zIndex(100f)
            .clickable(enabled = false) { } 
    ) {
        BackHandler { onDismiss() }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = theme.backgroundColor,
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f)
                .align(Alignment.Center)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialPerson == null) stringResource(R.string.encounter_dialog_title_new) else stringResource(R.string.encounter_dialog_title_edit),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.4f))
                    }
                }

                Spacer(Modifier.height(24.dp))

                val mediaManager = remember { EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager() }

                Box(
                    modifier = Modifier
                        .size(120.dp, 156.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.contentColor.copy(alpha = 0.05f))
                        .clickable {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                ) {
                    val activePath = encounterImagePath ?: initialPerson?.imagePath
                    if (activePath != null) {
                        val isPathEncrypted = activePath.endsWith(".enc")
                        SecureAsyncImage(
                            mediaUrl = activePath,
                            mediaManager = mediaManager,
                            isEncrypted = isPathEncrypted,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(32.dp).alpha(0.4f), tint = theme.contentColor)
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.encounter_dialog_add_photo), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Identité
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text(stringResource(R.string.encounter_dialog_firstname_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(stringResource(R.string.encounter_dialog_lastname_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )

                Spacer(Modifier.height(24.dp))
                
                // Nature du lien
                Text(stringResource(R.string.encounter_dialog_nature_section), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                
                val natures = mapOf(
                    "Ami" to stringResource(R.string.encounter_nature_ami),
                    "Partenaire" to stringResource(R.string.encounter_nature_partner),
                    "Mentor" to stringResource(R.string.encounter_nature_mentor),
                    "Collègue" to stringResource(R.string.encounter_nature_collegue),
                    "Voisin" to stringResource(R.string.encounter_nature_voisin),
                    "Autre" to stringResource(R.string.encounter_context_other)
                )

                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    natures.forEach { (key, label) ->
                        val isSelected = linkNature == key || (key == "Autre" && linkNature != "" && !natures.containsKey(linkNature))
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                if (key == "Autre") {
                                    linkNature = "Autre"
                                } else {
                                    linkNature = key 
                                    linkNatureCustom = ""
                                }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.2f), selectedLabelColor = accent)
                        )
                    }
                }
                
                if (linkNature == "Autre") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = linkNatureCustom,
                        onValueChange = { linkNatureCustom = it },
                        label = { Text(stringResource(R.string.encounter_dialog_nature_custom_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Détails de la rencontre
                Text(stringResource(R.string.encounter_dialog_encounter_section), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = encounterAge,
                    onValueChange = { if (it.all { char -> char.isDigit() }) encounterAge = it },
                    label = { Text(stringResource(R.string.encounter_dialog_age_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                Spacer(Modifier.height(16.dp))
                
                val contexts = mapOf(
                    "SCHOOL" to stringResource(R.string.encounter_context_school), 
                    "WORK" to stringResource(R.string.encounter_context_work), 
                    "SPORT" to stringResource(R.string.encounter_context_sport), 
                    "PASSION" to stringResource(R.string.encounter_context_passion), 
                    "TRAVEL" to stringResource(R.string.encounter_context_travel), 
                    "OTHER" to stringResource(R.string.encounter_context_other)
                )
                
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contexts.forEach { (key, label) ->
                        FilterChip(
                            selected = encounterContext == key,
                            onClick = { encounterContext = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.2f), selectedLabelColor = accent)
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = encounterContextLabel,
                        onValueChange = { encounterContextLabel = it },
                        label = { Text(stringResource(R.string.encounter_dialog_precision_label)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                    OutlinedTextField(
                        value = locationLabel,
                        onValueChange = { locationLabel = it },
                        label = { Text(stringResource(R.string.encounter_dialog_location_label)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // État du lien
                Text(stringResource(R.string.encounter_dialog_status_section), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusMap = mapOf(
                        "PRESENT" to stringResource(R.string.encounter_dialog_status_present), 
                        "LOST" to stringResource(R.string.encounter_dialog_status_lost), 
                        "PASSED" to stringResource(R.string.encounter_dialog_status_disparu)
                    )
                    statusMap.forEach { (key, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = linkStatus == key,
                            onClick = { linkStatus = key },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.2f), selectedLabelColor = accent)
                        )
                    }
                }
                
                if (linkNature == "Partenaire" && linkStatus != "PRESENT") {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = relationEndAge,
                        onValueChange = { if (it.all { char -> char.isDigit() }) relationEndAge = it },
                        label = { Text(stringResource(R.string.encounter_dialog_end_age_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = relationEndReason,
                        onValueChange = { relationEndReason = it },
                        label = { Text(stringResource(R.string.encounter_dialog_end_reason_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Présenté par
                Text(stringResource(R.string.encounter_dialog_introducer_section), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                
                if (introducedById != null) {
                    val introducer = allPersons.find { it.id == introducedById }
                    if (introducer != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = theme.contentColor.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (introducer.categories.contains("FAMILY")) Icons.Default.Home else Icons.Default.Handshake,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(introducer.firstName, fontWeight = FontWeight.Bold, color = theme.contentColor, modifier = Modifier.weight(1f))
                                IconButton(onClick = { introducedById = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.encounter_dialog_search_introducer_placeholder), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )

                    if (suggestedIntroducers.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = theme.contentColor.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column {
                                suggestedIntroducers.take(3).forEach { p ->
                                    ListItem(
                                        headlineContent = { Text(p.firstName, fontWeight = FontWeight.Bold, color = theme.contentColor) },
                                        leadingContent = { 
                                            Icon(
                                                imageVector = if (p.categories.contains("FAMILY")) Icons.Default.Home else Icons.Default.Handshake,
                                                contentDescription = null,
                                                tint = accent.copy(alpha = 0.6f)
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            introducedById = p.id
                                            query = ""
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // MÉDIAS SECONDAIRES (v9.6.6): Flow réactif pour mise à jour immédiate
                var videoErrorMessage by remember { mutableStateOf<String?>(null) }
                val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    uri?.let {
                        val mime = context.contentResolver.getType(it)
                        val type = if (mime?.contains("video") == true) "VIDEO" else "PHOTO"
                        
                        if (type == "VIDEO") {
                            val isValid = com.example.phoenx.ui.util.VideoUtils.isVideoDurationValid(
                                context, it, com.example.phoenx.ui.util.VideoUtils.MAX_VIDEO_DURATION_SECONDS_STANDARD
                            )
                            if (isValid) {
                                val file = viewModel.uriToFile(it)
                                if (file != null && initialPerson != null) {
                                    viewModel.addMediaComplement(initialPerson.id, file, "VIDEO")
                                    videoErrorMessage = null
                                }
                            } else {
                                videoErrorMessage = context.getString(R.string.encounter_dialog_video_error_duration)
                            }
                        } else {
                            val file = viewModel.uriToFile(it)
                            if (file != null && initialPerson != null) {
                                viewModel.addMediaComplement(initialPerson.id, file, "PHOTO")
                                videoErrorMessage = null
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.encounter_detail_media_title), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                    if (initialPerson != null) {
                        IconButton(onClick = { 
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = accent)
                        }
                    }
                }
                
                if (videoErrorMessage != null) {
                    Text(
                        text = videoErrorMessage!!,
                        color = Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Spacer(Modifier.height(8.dp))

                if (initialPerson == null) {
                    Text(stringResource(R.string.encounter_dialog_create_first_hint), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.3f))
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = accent.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.encounter_dialog_media_security_warning),
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (mediaList.isEmpty()) {
                        Text(stringResource(R.string.encounter_dialog_gallery_empty), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.3f))
                    } else {
                        mediaList.chunked(3).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { media ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                android.util.Log.d("PHX_MEDIA_DEBUG", "Thumbnail CLICK: mediaId=${media.id}, type=${media.mediaType}")
                                                navController.navigate(
                                                    com.example.phoenx.ui.navigation.Screen.MediaViewer.createRoute(
                                                        entryId = media.id,
                                                        creatorId = null,
                                                        mediaUrl = media.mediaPath,
                                                        entryType = media.mediaType,
                                                        aiSummary = context.getString(R.string.encounter_detail_media_title),
                                                        sourceDocType = "personMedia",
                                                        personId = initialPerson.id,
                                                        isEncrypted = !media.mediaPath.startsWith("/")
                                                    )
                                                )
                                            }
                                    ) {
                                        val activeUrl = media.thumbnailPath ?: media.mediaPath
                                        val isPathEncrypted = !activeUrl.startsWith("/")
                                        val fieldParam = if (media.thumbnailPath != null) "thumbnailPath" else "mediaPath"

                                        SecureAsyncImage(
                                            mediaUrl = activeUrl,
                                            mediaManager = mediaManager,
                                            explicitKey = if (heirKey != null) heirKey else null,
                                            isEncrypted = isPathEncrypted,
                                            creatorId = null,
                                            docType = "personMedia",
                                            docId = media.id,
                                            field = fieldParam,
                                            personId = initialPerson.id,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        
                                        if (media.mediaType == "VIDEO") {
                                            Icon(
                                                Icons.Default.PlayCircle,
                                                null,
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(24.dp).align(Alignment.Center)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeMedia(media) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(18.dp)
                                                .offset(x = (-4).dp, y = 4.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                // Ce qu'il/elle m'a apporté (Bio)
                Text(stringResource(R.string.genealogy_biography_section_title), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = encounterBiography,
                    onValueChange = { encounterBiography = it },
                    label = { Text(stringResource(R.string.encounter_dialog_biography_placeholder)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )

                Spacer(Modifier.height(32.dp))

                // Confidentialité
                var showPrivacyInfo by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.encounter_dialog_privacy_label), fontWeight = FontWeight.Bold, color = theme.contentColor)
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { showPrivacyInfo = !showPrivacyInfo },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = stringResource(R.string.encounter_dialog_privacy_info), tint = accent.copy(alpha = 0.7f))
                            }
                        }
                        Text(stringResource(R.string.encounter_dialog_privacy_subtitle), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                        
                        if (showPrivacyInfo) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = theme.contentColor.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = stringResource(R.string.encounter_dialog_privacy_detail),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.contentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent)
                    )
                }

                Spacer(Modifier.height(40.dp))

                // Boutons d'action
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val newCategories = if (initialPerson != null) {
                                (initialPerson.categories.split(",") + "ENCOUNTER").distinct().joinToString(",")
                            } else ",ENCOUNTER,"

                            val finalNature = if (linkNature == "Autre") linkNatureCustom else linkNature

                            val personToSave = (initialPerson ?: PersonEntity(firstName = "")).copy(
                                firstName = firstName,
                                lastName = lastName.ifBlank { null },
                                encounterBiography = encounterBiography,
                                encounterImagePath = encounterImagePath,
                                encounterAge = encounterAge.toIntOrNull(),
                                linkNature = finalNature.ifBlank { null },
                                encounterContext = encounterContext,
                                linkStatus = linkStatus,
                                relationEndAge = relationEndAge.toIntOrNull(),
                                relationEndReason = relationEndReason.ifBlank { null },
                                encounterLocationLabel = locationLabel.ifBlank { null },
                                encounterContextLabel = encounterContextLabel.ifBlank { null },
                                introducedById = introducedById,
                                visibility = if (isPrivate) "PRIVATE" else "PUBLIC",
                                categories = newCategories
                            )
                            onConfirm(personToSave)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = firstName.isNotBlank()
                    ) {
                        Text(if (initialPerson == null) stringResource(R.string.encounter_dialog_button_create) else stringResource(R.string.encounter_dialog_button_save), color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (initialPerson != null) {
                        TextButton(
                            onClick = { checkBeforeDelete() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.encounter_dialog_button_remove), color = Error)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.encounter_detail_cancel), color = theme.contentColor.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
