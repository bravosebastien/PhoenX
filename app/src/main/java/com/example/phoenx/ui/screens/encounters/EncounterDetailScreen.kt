package com.example.phoenx.ui.screens.encounters

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil3.compose.AsyncImage
import com.example.phoenx.R
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.local.PersonMediaEntity
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.Error
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncounterDetailScreen(
    personId: String,
    targetCreatorId: String? = null,
    heirKey: ByteArray? = null,
    navController: NavController,
    viewModel: EncounterViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    
    val personState by viewModel.getPersonById(personId, targetCreatorId).collectAsState(initial = null)
    val currentPerson = personState // v9.6.6 : Local copy for smart casting
    
    val allPersons by viewModel.allSelectablePersons.collectAsState()
    val isReadOnly = targetCreatorId != null

    val mediaManager = remember {
        EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager()
    }

    // Chargement des médias et statistiques (v9.6.6 : Passage en Flow réactif)
    android.util.Log.d("PHX_MEDIA_DEBUG", "EncounterDetailScreen COMPOSABLE: personId=$personId")
    val personMedia by viewModel.getMediaForPerson(personId).collectAsState(initial = emptyList())
    
    LaunchedEffect(personMedia) {
        android.util.Log.d("PHX_MEDIA_DEBUG", "EncounterDetailScreen personMedia EMIT: size=${personMedia.size}, ids=${personMedia.map { it.id }}")
    }

    val memoriesCount by viewModel.getMemoriesCountForPerson(personId).collectAsState(initial = 0)

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) } // v12.2 : Sorti de la photo
    var loadTimeout by remember { mutableStateOf(false) }

    LaunchedEffect(personId, targetCreatorId) {
        if (targetCreatorId != null) {
            viewModel.loadRemoteEncounters(targetCreatorId)
        }
        // v9.6.6 : Timeout de sécurité pour éviter le chargement infini
        kotlinx.coroutines.delay(8000)
        if (currentPerson == null) {
            loadTimeout = true
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val file = viewModel.uriToFile(uri)
            if (file != null) {
                // On a le nouveau fichier local. On sauvegarde avec le nouveau chemin.
                // SyncWorker s'occupera d'uploader ce chemin local vers encounter_portraits/
                currentPerson?.let {
                    viewModel.saveEncounter(it.copy(encounterImagePath = file.absolutePath))
                }
            }
        }
    }

    if (currentPerson == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loadTimeout) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.encounter_detail_load_error), color = theme.contentColor.copy(alpha = 0.5f))
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(stringResource(R.string.encounter_detail_back_button), color = accent)
                    }
                }
            } else {
                CircularProgressIndicator(color = accent)
            }
        }
        return
    }

    if (showEditDialog) {
        EncounterDetailsDialog(
            initialPerson = currentPerson,
            allPersons = allPersons,
            navController = navController,
            onConfirm = { updatedPerson ->
                viewModel.saveEncounter(updatedPerson)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
            onRemoveCategory = {
                viewModel.removeEncounterCategory(it)
                showEditDialog = false
                navController.popBackStack()
            },
            accent = accent
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = theme.backgroundColor,
            title = { Text(stringResource(R.string.encounter_detail_remove_title), color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.encounter_detail_remove_text), color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeEncounterCategory(currentPerson)
                        showDeleteConfirm = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Text(stringResource(R.string.encounter_detail_remove_confirm), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.encounter_detail_cancel), color = theme.contentColor)
                }
            }
        )
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.encounter_detail_desc_back), 
                            tint = theme.contentColor
                        )
                    }
                },
                actions = {
                    if (!isReadOnly && currentPerson.categories.contains("ENCOUNTER")) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit, 
                                contentDescription = stringResource(R.string.encounter_detail_desc_edit), 
                                tint = theme.contentColor
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete, 
                                contentDescription = stringResource(R.string.encounter_detail_desc_delete), 
                                tint = Error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // 1. HEADER : PORTRAIT ET IDENTITÉ
            Row(modifier = Modifier.fillMaxWidth()) {
                // Portrait
                val natureRaw = displayLinkNature(currentPerson.linkNature)
                val natureLabel = when(natureRaw) {
                    "Partenaire" -> stringResource(R.string.encounter_nature_partner)
                    "Ami" -> stringResource(R.string.encounter_nature_ami)
                    "Mentor" -> stringResource(R.string.encounter_nature_mentor)
                    "Collègue" -> stringResource(R.string.encounter_nature_collegue)
                    "Voisin" -> stringResource(R.string.encounter_nature_voisin)
                    "non renseigné" -> stringResource(R.string.encounter_nature_not_set)
                    else -> natureRaw
                }
                val isPartner = natureRaw == "Partenaire"
                val activePath = currentPerson.encounterImagePath ?: currentPerson.imagePath
                val isPathEncrypted = activePath?.endsWith(".enc") == true

                Box(
                    modifier = Modifier
                        .size(150.dp, 196.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.contentColor.copy(alpha = 0.05f))
                        .then(
                            if (isPartner) Modifier.border(2.dp, Color(0xFFB4646A), RoundedCornerShape(10.dp))
                            else Modifier
                        )
                ) {
                    if (activePath != null) {
                        val fieldParam = if (currentPerson.encounterImagePath != null) "encounterImagePath" else "imageUrl"
                        SecureAsyncImage(
                            mediaUrl = activePath,
                            mediaManager = mediaManager,
                            isEncrypted = isPathEncrypted,
                            explicitKey = if (isReadOnly) heirKey else null, 
                            creatorId = targetCreatorId,
                            docType = "persons",
                            docId = personId,
                            field = fieldParam,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp).align(Alignment.Center).alpha(0.2f), tint = theme.contentColor)
                    }
                }

                Spacer(Modifier.width(20.dp))

                // Identité
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentPerson.firstName,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = theme.fontFamily),
                        color = theme.contentColor,
                        lineHeight = 42.sp
                    )
                    if (!currentPerson.lastName.isNullOrBlank()) {
                        Text(
                            text = currentPerson.lastName!!.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                            color = theme.contentColor.copy(alpha = 0.6f)
                        )
                    }
                    
                    if (currentPerson.categories.contains("ENCOUNTER")) {
                        Spacer(Modifier.height(12.dp))
                        
                        // Nature du lien
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(getNatureColor(currentPerson.linkNature)))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = natureLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = theme.contentColor.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Intertitre Rencontre
                        Text(stringResource(R.string.encounter_detail_header_encounter), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = accent)
                        Text(
                            text = stringResource(R.string.encounter_detail_age_at_encounter, currentPerson.encounterAge ?: "?"),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                        
                        val contextLabel = when(currentPerson.encounterContext) {
                            "SCHOOL" -> stringResource(R.string.encounter_context_school)
                            "WORK" -> stringResource(R.string.encounter_context_work)
                            "SPORT" -> stringResource(R.string.encounter_context_sport)
                            "PASSION" -> stringResource(R.string.encounter_context_passion)
                            "TRAVEL" -> stringResource(R.string.encounter_context_travel)
                            "OTHER" -> stringResource(R.string.encounter_context_other)
                            else -> null
                        }
                        val details = listOfNotNull(contextLabel, currentPerson.encounterLocationLabel).joinToString(" · ")
                        if (details.isNotBlank()) {
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = theme.contentColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // 2. PRÉSENTÉ PAR
            if (!currentPerson.introducedById.isNullOrBlank()) {
                val introducer = allPersons.find { it.id == currentPerson.introducedById }
                if (introducer != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                navController.navigate(Screen.EncounterDetail.createRoute(introducer.id, targetCreatorId))
                            },
                        color = theme.contentColor.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val introducerPath = introducer.encounterImagePath ?: introducer.imagePath
                            val isIntroducerPathEncrypted = introducerPath?.endsWith(".enc") == true
                            val introducerField = if (introducer.encounterImagePath != null) "encounterImagePath" else "imageUrl"

                            CameoPortrait(
                                imagePath = introducerPath,
                                firstName = introducer.firstName,
                                size = 48.dp,
                                creatorId = targetCreatorId,
                                docType = "persons",
                                docId = introducer.id,
                                field = introducerField,
                                isEncrypted = isIntroducerPathEncrypted,
                                explicitKey = if (isReadOnly) heirKey else null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.encounter_detail_introduced_by, introducer.firstName),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = theme.contentColor
                            )
                            Icon(Icons.Default.ChevronRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }

            // 3. CE QU'ELLE M'A APPORTÉ
            if (currentPerson.encounterBiography.isNotBlank()) {
                Text(stringResource(R.string.encounter_detail_legacy_title), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = currentPerson.encounterBiography,
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, lineHeight = 26.sp),
                    color = theme.contentColor.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(40.dp))
            }

            // 4. PHOTOS & VIDÉOS
            if (personMedia.isNotEmpty()) {
                Text(stringResource(R.string.encounter_detail_media_title), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(personMedia) { media ->
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.contentColor.copy(alpha = 0.05f))
                                .clickable {
                                    navController.navigate(
                                        Screen.MediaViewer.createRoute(
                                            entryId = media.id,
                                            creatorId = targetCreatorId,
                                            mediaUrl = media.mediaPath,
                                            entryType = media.mediaType,
                                            aiSummary = context.getString(R.string.encounter_detail_media_title),
                                            sourceDocType = "personMedia",
                                            personId = personId,
                                            isEncrypted = media.mediaPath.endsWith(".enc")
                                        )
                                    )
                                }
                        ) {
                            val activeUrl = media.thumbnailPath ?: media.mediaPath
                            val isPathEncrypted = activeUrl.endsWith(".enc")
                            val fieldParam = if (media.thumbnailPath != null) "thumbnailPath" else "mediaPath"

                            SecureAsyncImage(
                                mediaUrl = activeUrl,
                                mediaManager = mediaManager,
                                isEncrypted = isPathEncrypted,
                                explicitKey = if (isReadOnly) heirKey else null,
                                creatorId = targetCreatorId,
                                docType = "personMedia",
                                docId = media.id,
                                field = fieldParam,
                                personId = personId,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            if (media.mediaType == "VIDEO") {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp).align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accent.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val label = if (memoriesCount > 1) stringResource(R.string.encounter_detail_memories_count_plural, memoriesCount) 
                                else stringResource(R.string.encounter_detail_memories_count_singular, memoriesCount)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // 6. ÉTAT DU LIEN (BAS DE FICHE)
            val linkStatus = currentPerson.linkStatus ?: "PRESENT"
            if (currentPerson.relationEndAge != null) {
                StatusBadge(
                    label = stringResource(R.string.encounter_detail_status_separated, currentPerson.relationEndAge),
                    color = Color.Gray
                )
                if (!currentPerson.relationEndReason.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = currentPerson.relationEndReason!!,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = theme.contentColor.copy(alpha = 0.5f)
                    )
                }
            } else {
                when(linkStatus) {
                    "PRESENT" -> StatusBadge(stringResource(R.string.encounter_detail_status_present), accent)
                    "LOST" -> StatusBadge(stringResource(R.string.encounter_detail_status_lost), Color.Gray)
                    "PASSED" -> StatusBadge(stringResource(R.string.encounter_detail_status_passed), Color.Gray)
                }
            }
            
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
