package com.example.phoenx.ui.screens.encounters

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncounterScreen(
    onNavigateBack: () -> Unit,
    navController: androidx.navigation.NavController,
    mainViewModel: MainViewModel,
    targetCreatorId: String? = null, // v9.6.0
    heirKey: ByteArray? = null, // v9.6.5 : Clé de déchiffrement héritage
    viewModel: EncounterViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    
    val filteredEncounters by viewModel.filteredEncounters.collectAsState()
    val allPersons by viewModel.allSelectablePersons.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val availableContexts by viewModel.availableContexts.collectAsState()
    val activeFilter by viewModel.contextFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val groupingMode by viewModel.groupingMode.collectAsState()
    
    var showSearch by remember { mutableStateOf(false) }

    // Mode lecture seule si consultation d'un héritage
    val isReadOnly = targetCreatorId != null

    LaunchedEffect(targetCreatorId, heirKey) {
        if (targetCreatorId != null) {
             viewModel.setHeirKey(heirKey)
             viewModel.loadRemoteEncounters(targetCreatorId)
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var selectedPerson by remember { mutableStateOf<PersonEntity?>(null) }

    val mediaManager = remember {
        EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager()
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            Column(modifier = Modifier.background(theme.backgroundColor)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Les Rencontres",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = theme.contentColor
                            )
                            Text(
                                "Ceux qui ont compté",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.contentColor.copy(alpha = 0.5f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Default.Search, null, tint = theme.contentColor)
                        }
                        InfoButton(
                            title = "Les Rencontres",
                            points = listOf(
                                "Les Rencontres, c'est l'endroit pour raconter les personnes qui ont marqué ta vie en dehors de ta famille — un ami, un mentor, une rencontre décisive.",
                                "Classe-les par âge ou découvre qui t'a présenté qui grâce au badge cliquable sur chaque fiche.",
                                "Par défaut, une rencontre est visible par tes Destinataires une fois ton héritage activé — mais tu peux la marquer 'Gardée pour moi' pour qu'elle reste ton jardin secret, invisible pour tout le monde."
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        placeholder = { Text("Rechercher un nom...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                        trailingIcon = {
                            IconButton(onClick = { 
                                viewModel.updateSearchQuery("")
                                showSearch = false 
                            }) {
                                Icon(Icons.Default.Close, null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                    )
                }

                // Stats discrètes
                Text(
                    text = buildString {
                        append("${stats.total} personne${if (stats.total > 1) "s" else ""}")
                        if (stats.minAge != null && stats.maxAge != null) {
                            append(" · de ${stats.minAge} à ${stats.maxAge} ans")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = theme.contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                // Sélecteur de regroupement (Âge / Présenté par)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val terracotta = Color(0xFFBF6338)
                    listOf("Âge", "Présenté par").forEach { mode ->
                        val isSelected = groupingMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateGroupingMode(mode) },
                            label = { Text(mode) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = terracotta,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = theme.contentColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = terracotta),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                // Barre de filtres
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableContexts) { filter ->
                        val isSelected = activeFilter == filter
                        val terracotta = Color(0xFFBF6338)
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateContextFilter(filter) },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = terracotta,
                                selectedLabelColor = Color.White,
                                containerColor = terracotta.copy(alpha = 0.1f),
                                labelColor = terracotta
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = { 
                        selectedPerson = null
                        showDialog = true 
                    },
                    containerColor = Color(0xFFBF6338),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Fil de vie décoratif
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .offset(x = 28.dp)
                    .background(Color(0xFFDED3C0))
            )

            if (filteredEncounters.isEmpty()) {
                EmptyEncounters(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    if (groupingMode == "Âge") {
                        val grouped = filteredEncounters.groupBy { it.encounterAge }
                        val sortedAges = grouped.keys.sortedBy { it ?: Int.MAX_VALUE }

                        sortedAges.forEach { age ->
                            val persons = grouped[age] ?: emptyList()
                            
                            item {
                                AgeHeader(age = age)
                            }

                            val rows = persons.chunked(2)
                            itemsIndexed(rows) { rowIndex, rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 56.dp, end = 20.dp, bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    rowItems.forEachIndexed { colIndex, person ->
                                        val isLeft = colIndex == 0
                                        EncounterCard(
                                            person = person,
                                            isLeft = isLeft,
                                            rowIndex = rowIndex,
                                            targetCreatorId = targetCreatorId,
                                            heirKey = heirKey,
                                            mediaManager = mediaManager,
                                            allPersons = allPersons,
                                            onClick = {
                                                navController.navigate(Screen.EncounterDetail.createRoute(person.id, targetCreatorId))
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // Mode Présenté par
                        val introducerMap = allPersons.associateBy(
                            { it.id }, 
                            { it.firstName + (it.lastName?.let { n -> " $n" } ?: "") }
                        )
                        val grouped = filteredEncounters.groupBy { it.introducedById }
                        
                        // Tri alphabétique du nom du présentateur. Les non-renseignés à la fin.
                        val sortedKeys = grouped.keys.sortedWith(
                            compareBy<String?> { it.isNullOrBlank() }.thenBy { introducerMap[it] ?: "ZZZ" }
                        )

                        sortedKeys.forEach { introducerId ->
                            val persons = grouped[introducerId] ?: emptyList()
                            val introducerName = if (introducerId.isNullOrBlank()) "Non renseigné" else introducerMap[introducerId] ?: "Personne inconnue"
                            
                            item {
                                IntroducerHeader(name = introducerName)
                            }
                            
                            val rows = persons.chunked(2)
                            itemsIndexed(rows) { rowIndex, rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 56.dp, end = 20.dp, bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    rowItems.forEachIndexed { colIndex, person ->
                                        val isLeft = colIndex == 0
                                        EncounterCard(
                                            person = person,
                                            isLeft = isLeft,
                                            rowIndex = rowIndex,
                                            targetCreatorId = targetCreatorId,
                                            heirKey = heirKey,
                                            mediaManager = mediaManager,
                                            allPersons = allPersons,
                                            onClick = {
                                                navController.navigate(Screen.EncounterDetail.createRoute(person.id, targetCreatorId))
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        EncounterDetailsDialog(
            initialPerson = selectedPerson,
            allPersons = allPersons,
            navController = navController,
            onConfirm = { 
                viewModel.saveEncounter(it)
                showDialog = false 
            },
            onDismiss = { showDialog = false },
            onRemoveCategory = {
                viewModel.removeEncounterCategory(it)
                showDialog = false
            },
            accent = accent
        )
    }
}

@Composable
fun IntroducerHeader(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 22.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFDF5)) // Fond parchemin
                .border(1.5.dp, Color(0xFFBF6338), CircleShape)
        )
        
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFFBF6338),
            modifier = Modifier.padding(start = 22.dp)
        )
    }
}

@Composable
fun AgeHeader(age: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 22.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFDF5)) // Fond parchemin
                .border(1.5.dp, Color(0xFFBF6338), CircleShape)
        )
        
        Text(
            text = if (age == null) "Sans date" else "À $age ans",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFFBF6338),
            modifier = Modifier.padding(start = 22.dp)
        )
    }
}

@Composable
fun EncounterCard(
    person: PersonEntity,
    isLeft: Boolean,
    rowIndex: Int,
    targetCreatorId: String? = null,
    heirKey: ByteArray? = null,
    mediaManager: MediaManager,
    allPersons: List<PersonEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val nature = displayLinkNature(person.linkNature)
    val isPartner = nature == "Partenaire"
    val isLost = person.linkStatus == "LOST"
    val isPassed = person.linkStatus == "PASSED"
    
    // Alternance d'inclinaison
    val tilt = if ((rowIndex + (if (isLeft) 0 else 1)) % 2 == 0) 1.5f else -1.5f

    Column(
        modifier = modifier
            .rotate(tilt)
            .clickable(onClick = onClick)
    ) {
        // Image
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(4.dp, RoundedCornerShape(11.dp))
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE9E4D1), Color(0xFFDED3C0))
                    )
                )
                .then(
                    if (isPartner) Modifier.border(2.dp, Color(0xFFB4646A), RoundedCornerShape(11.dp))
                    else Modifier
                )
        ) {
            val activePath = person.encounterImagePath ?: person.imagePath
            if (activePath != null) {
                val isPathEncrypted = (person.encounterImagePath != null) && !(person.encounterImagePath.startsWith("/data/") || !person.encounterImagePath.startsWith("users/"))
                val fieldParam = if (person.encounterImagePath != null) "encounterImagePath" else "imageUrl"

                SecureAsyncImage(
                    mediaUrl = activePath,
                    mediaManager = mediaManager,
                    isEncrypted = isPathEncrypted,
                    explicitKey = if (targetCreatorId != null) heirKey else null, // Utilisation de la vraie clé d'héritier
                    creatorId = targetCreatorId,
                    docType = "persons",
                    docId = person.id,
                    field = fieldParam,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isLost || isPassed) 0.5f else 1f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .alpha(0.2f),
                    tint = theme.contentColor
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Nom
        Text(
            text = person.firstName + (person.lastName?.let { " $it" } ?: ""),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = theme.fontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isLost || isPassed) Color(0xFF8E8578) else theme.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Nature
        Row(verticalAlignment = Alignment.CenterVertically) {
            val natureColor = if (isLost || isPassed) Color.Gray else getNatureColor(person.linkNature)
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(natureColor))
            Spacer(Modifier.width(6.dp))
            
            val natureText = buildString {
                append(nature)
                if (isPartner && person.relationEndAge != null) {
                    append(" · de ${person.encounterAge} à ${person.relationEndAge} ans")
                }
            }
            
            Text(
                text = natureText,
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.6f)
            )
        }

        // Contexte et infos
        val contextLabel = when(person.encounterContext) {
            "SCHOOL" -> "École"
            "WORK" -> "Travail"
            "SPORT" -> "Sport"
            "PASSION" -> "Passion"
            "TRAVEL" -> "Voyage"
            "OTHER" -> "Autre"
            else -> null
        }
        
        val introducerName = if (!person.introducedById.isNullOrBlank()) {
            allPersons.find { it.id == person.introducedById }?.firstName
        } else null

        val footerText = buildList {
            if (isLost) add("perdu de vue")
            if (isPassed) add("n'est plus là")
            contextLabel?.let { add(it) }
            person.encounterContextLabel?.takeIf { it.isNotBlank() }?.let { add(it) }
            introducerName?.let { add("présentée par $it") }
        }.joinToString(" · ")

        if (footerText.isNotBlank()) {
            Text(
                text = footerText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontStyle = if (isLost || isPassed) FontStyle.Italic else null
                ),
                color = theme.contentColor.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyEncounters(modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Handshake,
            null,
            modifier = Modifier.size(64.dp),
            tint = theme.contentColor.copy(alpha = 0.1f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Aucune rencontre enregistrée.",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f)
        )
    }
}

fun getNatureColor(nature: String?): Color {
    return when(displayLinkNature(nature)) {
        "Ami" -> Color(0xFF7C9068)
        "Partenaire" -> Color(0xFFB4646A)
        "Mentor" -> Color(0xFF7A8CA3)
        "Collègue" -> Color(0xFFA08A5F)
        "Voisin" -> Color(0xFF7FA8A8)
        else -> Color(0xFFA39A8E)
    }
}
