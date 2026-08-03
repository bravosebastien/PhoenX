package com.example.phoenx.ui.screens.genealogy

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.local.PersonMediaEntity
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.components.PersonSelector
import com.example.phoenx.ui.theme.Error
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenealogyTreeScreen(
    navController: NavController,
    targetCreatorId: String? = null,
    viewModel: GenealogyTreeViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val rootPersons by viewModel.rootPersons.collectAsState()
    val allPersons by viewModel.allPersons.collectAsState()
    val treeLayout by viewModel.treeLayout.collectAsState()
    
    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var isPreviewMode by remember { mutableStateOf(false) }
    val isReadOnly = (targetCreatorId != null && targetCreatorId != myUid) || isPreviewMode

    var selectedPersonForDetails by remember { mutableStateOf<PersonEntity?>(null) }
    var selectedPersonForAddingChild by remember { mutableStateOf<PersonEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isTreeView by remember { mutableStateOf(true) }

    LaunchedEffect(targetCreatorId) {
        viewModel.loadTree(targetCreatorId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val title = if (isReadOnly) "L'Arbre de votre proche" else "Mon Arbre Généalogique"
                    Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    if (targetCreatorId == null || targetCreatorId == myUid) {
                        IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                            Icon(
                                if (isPreviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Aperçu Destinataire",
                                tint = if (isPreviewMode) accent else theme.contentColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = { isTreeView = !isTreeView }) {
                        Icon(if (isTreeView) Icons.AutoMirrored.Filled.List else Icons.Default.AccountTree, null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (allPersons.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(64.dp), tint = theme.contentColor.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))
                        Text("Ton arbre est encore vide.", color = theme.contentColor.copy(alpha = 0.4f))
                        Button(
                            onClick = { showCreateDialog = true; selectedPersonForAddingChild = null },
                            modifier = Modifier.padding(top = 24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text("Ajouter la première personne", color = theme.backgroundColor)
                        }
                    }
                }
            } else if (isTreeView) {
                GenealogyTreeRenderer(
                    layout = treeLayout,
                    onPersonClick = { resolved ->
                        selectedPersonForDetails = allPersons.find { it.id == resolved.id }
                    },
                    onAddChild = { resolved ->
                        selectedPersonForAddingChild = allPersons.find { it.id == resolved.id }
                        showCreateDialog = true
                    },
                    enabled = !isReadOnly
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(rootPersons) { person ->
                        PersonTreeNode(
                            person = person,
                            level = 0,
                            viewModel = viewModel,
                            onAddChild = { 
                                selectedPersonForAddingChild = it
                                showCreateDialog = true 
                            },
                            onShowDetails = { selectedPersonForDetails = it },
                            enabled = !isReadOnly
                        )
                    }
                    
                    if (!isReadOnly) {
                        item {
                            TextButton(
                                onClick = { showCreateDialog = true; selectedPersonForAddingChild = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null, tint = accent)
                                Spacer(Modifier.width(8.dp))
                                Text("Ajouter une autre racine", color = accent)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedPersonForDetails != null) {
        PersonDetailsDialog(
            person = selectedPersonForDetails!!,
            viewModel = viewModel,
            onDismiss = { selectedPersonForDetails = null },
            accent = accent,
            isReadOnly = isReadOnly,
            onEditLinks = { showCreateDialog = true }
        )
    }

    if (showCreateDialog) {
        CreateOrEditPersonInTreeDialog(
            initialPerson = if (selectedPersonForAddingChild == null) selectedPersonForDetails else null,
            initialParents = selectedPersonForAddingChild?.let { listOf(it) } ?: emptyList(),
            allPersons = allPersons,
            onConfirm = { firstName, lastName, parentIds ->
                if (selectedPersonForAddingChild == null && selectedPersonForDetails != null) {
                    viewModel.updatePersonIdentity(selectedPersonForDetails!!.id, firstName, lastName, parentIds)
                } else {
                    viewModel.createAndLinkPerson(firstName, lastName, parentIds)
                }
                showCreateDialog = false
                selectedPersonForAddingChild = null
                selectedPersonForDetails = null 
            },
            onDismiss = { 
                showCreateDialog = false
                selectedPersonForAddingChild = null
            },
            accent = accent
        )
    }
}

@Composable
fun PersonTreeNode(
    person: PersonEntity,
    level: Int,
    viewModel: GenealogyTreeViewModel,
    onAddChild: (PersonEntity) -> Unit,
    onShowDetails: (PersonEntity) -> Unit,
    enabled: Boolean = true
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val children by viewModel.getChildrenOf(person.id).collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(start = (level * 24).dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowDetails(person) },
            shape = RoundedCornerShape(12.dp),
            color = theme.contentColor.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameoPortrait(
                    imagePath = person.imagePath,
                    firstName = person.firstName,
                    size = 40.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = person.firstName + (person.lastName?.let { " $it" } ?: ""),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (person.isDeceased) theme.contentColor.copy(alpha = 0.5f) else theme.contentColor
                    )
                    if (person.isDeceased) {
                        Text("Décédé(e)", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.3f))
                    }
                }
                if (enabled) {
                    IconButton(onClick = { onAddChild(person) }) {
                        Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (children.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            children.forEach { child ->
                PersonTreeNode(
                    person = child,
                    level = level + 1,
                    viewModel = viewModel,
                    onAddChild = onAddChild,
                    onShowDetails = onShowDetails,
                    enabled = enabled
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailsDialog(
    person: PersonEntity,
    viewModel: GenealogyTreeViewModel,
    onDismiss: () -> Unit,
    accent: Color,
    isReadOnly: Boolean = false,
    onEditLinks: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val mediaList by viewModel.getMediaForPerson(person.id).collectAsState(initial = emptyList())
    val resolvedUrls by viewModel.resolvedUrls.collectAsState()
    
    LaunchedEffect(mediaList) {
        mediaList.forEach { media ->
            viewModel.resolveMediaUrl(person.id, media)
        }
    }
    
    var biography by remember(person) { mutableStateOf(person.biography) }
    var isDeceased by remember(person) { mutableStateOf(person.isDeceased) }
    var showDeleteConfirm by remember { mutableStateOf(false) } // v9.4.22

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val fileName = "person_media_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            viewModel.addMedia(person.id, file.absolutePath, "PHOTO")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = theme.backgroundColor,
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CameoPortrait(imagePath = person.imagePath, firstName = person.firstName, size = 64.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(person.firstName, style = MaterialTheme.typography.headlineSmall, color = theme.contentColor)
                        if (!isReadOnly) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isDeceased,
                                    onCheckedChange = { 
                                        isDeceased = it
                                        viewModel.toggleDeceased(person.id)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = accent)
                                )
                                Text("Décédé(e)", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                                
                                Spacer(Modifier.width(16.dp))
                                
                                TextButton(onClick = onEditLinks) {
                                    Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Modifier identité / liens", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else if (person.isDeceased) {
                            Text("Décédé(e)", style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                Text("BIOGRAPHIE", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                if (!isReadOnly) {
                    OutlinedTextField(
                        value = biography,
                        onValueChange = { biography = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        placeholder = { Text("Quelques mots sur sa vie...", fontStyle = FontStyle.Italic) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                    )
                    Button(
                        onClick = { viewModel.updateBiography(person.id, biography) },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.1f), contentColor = accent)
                    ) { Text("Enregistrer bio") }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
                    ) {
                        Text(
                            text = biography.ifBlank { "Aucune biographie renseignée." },
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                            color = theme.contentColor.copy(alpha = 0.8f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("GALERIE MÉDIA", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                    if (!isReadOnly) {
                        IconButton(onClick = { photoPicker.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = accent)
                        }
                    }
                }
                
                if (!isReadOnly) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        color = accent.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Ce média sera visible par TOUS vos Destinataires une fois l'héritage activé, sans restriction possible.",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (mediaList.isEmpty()) {
                    Text("Aucun média ajouté.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    mediaList.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { media ->
                                MediaThumbnail(
                                    media = media.copy(mediaPath = resolvedUrls[media.id] ?: media.mediaPath),
                                    onRemove = { viewModel.removeMedia(media) },
                                    isReadOnly = isReadOnly
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(32.dp))
                
                if (!isReadOnly) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp), tint = Error)
                        Spacer(Modifier.width(8.dp))
                        Text("Supprimer du répertoire et de l'arbre", color = Error, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.contentColor.copy(alpha = 0.05f), contentColor = theme.contentColor)
                ) { Text("Fermer") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = theme.backgroundColor,
            title = { Text("Supprimer cette personne ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Cette action est irréversible. Elle sera retirée de votre répertoire, de l'arbre généalogique et tous ses médias seront effacés.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePerson(person.id)
                    showDeleteConfirm = false
                    onDismiss()
                }) { Text("Supprimer définitivement", color = Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler", color = theme.contentColor) }
            }
        )
    }
}

@Composable
fun MediaThumbnail(media: PersonMediaEntity, onRemove: () -> Unit, isReadOnly: Boolean = false) {
    Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = media.mediaPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (!isReadOnly) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditPersonInTreeDialog(
    initialPerson: PersonEntity? = null,
    initialParents: List<PersonEntity> = emptyList(),
    allPersons: List<PersonEntity>,
    onConfirm: (firstName: String, lastName: String?, parentIds: List<String>) -> Unit,
    onDismiss: () -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    var firstName by remember { mutableStateOf(initialPerson?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialPerson?.lastName ?: "") }
    
    val selectedParentIds = remember { mutableStateListOf<String>().apply { 
        if (initialPerson != null) {
            addAll(initialPerson.parentIds.trim(',').split(",").filter { it.isNotBlank() })
        } else {
            addAll(initialParents.map { it.id })
        }
    } }

    var query by remember { mutableStateOf("") }
    val suggestedParents = if (query.isBlank()) emptyList() else allPersons.filter { 
        it.firstName.contains(query, ignoreCase = true) && 
        it.id != initialPerson?.id && 
        !selectedParentIds.contains(it.id)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.backgroundColor,
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (initialPerson == null) "Nouvelle Personne" else "Modifier les liens",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Nom (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    "PARENT(S) DE CETTE PERSONNE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedParentIds.forEach { pid ->
                        val p = allPersons.find { it.id == pid }
                        if (p != null) {
                            InputChip(
                                selected = true,
                                onClick = { selectedParentIds.remove(pid) },
                                label = { Text(p.firstName) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                                colors = InputChipDefaults.inputChipColors(selectedContainerColor = accent.copy(alpha = 0.2f))
                            )
                        }
                    }
                }

                if (selectedParentIds.size < 2) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Rechercher un parent...", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )

                    if (suggestedParents.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = theme.contentColor.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column {
                                suggestedParents.forEach { p ->
                                    ListItem(
                                        headlineContent = { Text(p.firstName, fontWeight = FontWeight.Bold, color = theme.contentColor) },
                                        supportingContent = { Text(p.relationship ?: "Proche", color = theme.contentColor.copy(alpha = 0.6f)) },
                                        modifier = Modifier.clickable {
                                            selectedParentIds.add(p.id)
                                            query = ""
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
                    Button(
                        onClick = { onConfirm(firstName, lastName.ifBlank { null }, selectedParentIds.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = firstName.isNotBlank()
                    ) { 
                        Text(if (initialPerson == null) "Créer" else "Enregistrer", color = theme.backgroundColor, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}
