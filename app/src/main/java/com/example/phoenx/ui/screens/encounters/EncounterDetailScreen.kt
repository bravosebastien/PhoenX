package com.example.phoenx.ui.screens.encounters

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
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
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.local.PersonMediaEntity
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.LocalAppTheme
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
    
    val allPersons by viewModel.allSelectablePersons.collectAsState()
    val person = allPersons.find { it.id == personId }
    val isReadOnly = targetCreatorId != null

    val mediaManager = remember {
        EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager()
    }

    // Chargement des médias et statistiques
    var personMedia by remember { mutableStateOf<List<PersonMediaEntity>>(emptyList()) }
    var memoriesCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(personId) {
        // Médias de la personne
        viewModel.getMediaForPerson(personId).collect { personMedia = it }
    }

    LaunchedEffect(personId) {
        // Décompte des souvenirs (v9.6.0)
        viewModel.getMemoriesCountForPerson(personId).collect { memoriesCount = it }
    }

    var showEditDialog by remember { mutableStateOf(false) }

    // Launcher pour changer la photo de profil (Étape 3)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val file = viewModel.uriToFile(uri)
            if (file != null) {
                // On a le nouveau fichier local. On sauvegarde avec le nouveau chemin.
                // SyncWorker s'occupera d'uploader ce chemin local vers encounter_portraits/
                person?.let {
                    viewModel.saveEncounter(it.copy(encounterImagePath = file.absolutePath))
                }
            }
        }
    }

    if (person == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent)
        }
        return
    }

    if (showEditDialog) {
        EncounterDetailsDialog(
            initialPerson = person,
            allPersons = allPersons,
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

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    if (!isReadOnly && person.categories.contains("ENCOUNTER")) {
                        TextButton(onClick = { showEditDialog = true }) {
                            Text("Modifier", color = Color(0xFFBF6338), fontWeight = FontWeight.Bold)
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
                val isPartner = displayLinkNature(person.linkNature) == "Partenaire"
                val activePath = person.encounterImagePath ?: person.imagePath
                val isPathEncrypted = (person.encounterImagePath != null) && !(person.encounterImagePath.startsWith("/data/") || !person.encounterImagePath.startsWith("users/"))

                Box(
                    modifier = Modifier
                        .size(150.dp, 196.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.contentColor.copy(alpha = 0.05f))
                        .then(
                            if (!isReadOnly && person.categories.contains("ENCOUNTER")) Modifier.clickable {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else Modifier
                        )
                        .then(
                            if (isPartner) Modifier.border(2.dp, Color(0xFFB4646A), RoundedCornerShape(10.dp))
                            else Modifier
                        )
                ) {
                    if (activePath != null) {
                        val fieldParam = if (person.encounterImagePath != null) "encounterImagePath" else "imageUrl"
                        SecureAsyncImage(
                            mediaUrl = activePath,
                            mediaManager = mediaManager,
                            isEncrypted = isPathEncrypted,
                            explicitKey = if (isReadOnly) heirKey else null, // Utilisation de la vraie clé
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
                        text = person.firstName,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = theme.fontFamily),
                        color = theme.contentColor,
                        lineHeight = 42.sp
                    )
                    if (!person.lastName.isNullOrBlank()) {
                        Text(
                            text = person.lastName!!.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                            color = theme.contentColor.copy(alpha = 0.6f)
                        )
                    }
                    
                    if (person.categories.contains("ENCOUNTER")) {
                        Spacer(Modifier.height(12.dp))
                        
                        // Nature du lien
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(getNatureColor(person.linkNature)))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = displayLinkNature(person.linkNature).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = theme.contentColor.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Intertitre Rencontre
                        Text("NOTRE RENCONTRE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = accent)
                        Text(
                            text = "J'avais ${person.encounterAge ?: "?"} ans",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                        
                        val contextLabel = when(person.encounterContext) {
                            "SCHOOL" -> "École"
                            "WORK" -> "Travail"
                            "SPORT" -> "Sport"
                            "PASSION" -> "Passion"
                            "TRAVEL" -> "Voyage"
                            "OTHER" -> "Autre"
                            else -> null
                        }
                        val details = listOfNotNull(contextLabel, person.encounterLocationLabel).joinToString(" · ")
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
            if (!person.introducedById.isNullOrBlank()) {
                val introducer = allPersons.find { it.id == person.introducedById }
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
                            CameoPortrait(imagePath = introducer.imagePath, firstName = introducer.firstName, size = 48.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = "Présenté(e) par ${introducer.firstName}",
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
            if (person.encounterBiography.isNotBlank()) {
                Text("CE QU'IL/ELLE M'A APPORTÉ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = person.encounterBiography,
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, lineHeight = 26.sp),
                    color = theme.contentColor.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(40.dp))
            }

            // 4. PHOTOS & VIDÉOS
            if (personMedia.isNotEmpty()) {
                Text("PHOTOS & VIDÉOS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = theme.contentColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(personMedia) { media ->
                        var mediaUrl by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(media.mediaPath) {
                            mediaUrl = if (isReadOnly) {
                                mediaManager.getSafeUrl(media.mediaPath, ByteArray(0), targetCreatorId, "personMedia", media.id)
                            } else {
                                mediaManager.getSafeUrl(media.mediaPath)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.contentColor.copy(alpha = 0.05f))
                        ) {
                            if (mediaUrl != null) {
                                AsyncImage(model = mediaUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }

            // 5. APPARAÎT DANS N SOUVENIRS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accent.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Apparaît dans $memoriesCount souvenir${if (memoriesCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor,
                        modifier = Modifier.weight(1f)
                    )
                    // Note: Le clic vers le Fil de Pensée filtré n'est pas encore possible (filtre non implémenté côté Fil)
                }
            }

            Spacer(Modifier.height(40.dp))

            // 6. ÉTAT DU LIEN (BAS DE FICHE)
            val linkStatus = person.linkStatus ?: "PRESENT"
            if (person.relationEndAge != null) {
                StatusBadge(
                    label = "Nos chemins se sont séparés · j'avais ${person.relationEndAge} ans",
                    color = Color.Gray
                )
                if (!person.relationEndReason.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = person.relationEndReason!!,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = theme.contentColor.copy(alpha = 0.5f)
                    )
                }
            } else {
                when(linkStatus) {
                    "PRESENT" -> StatusBadge("Toujours dans ma vie", accent)
                    "LOST" -> StatusBadge("Perdu de vue", Color.Gray)
                    "PASSED" -> StatusBadge("N'est plus là", Color.Gray)
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
