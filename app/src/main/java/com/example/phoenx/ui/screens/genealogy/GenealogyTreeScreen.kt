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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.example.phoenx.domain.model.VisualGroup
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.components.InfoButton
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
    viewModel: GenealogyTreeViewModel = hiltViewModel(),
    assistantViewModel: com.example.phoenx.ui.screens.assistant.AssistantViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val assistantX by assistantViewModel.bubbleX.collectAsState()
    val assistantY by assistantViewModel.bubbleY.collectAsState()
    val isAssistantChatOpen by assistantViewModel.isChatOpen.collectAsState()

    val treeGroups by viewModel.treeGroups.collectAsState() // v9.4.26
    val allPersons by viewModel.allPersons.collectAsState()
    val treeLayout by viewModel.treeLayout.collectAsState()
    
    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var isPreviewMode by remember { mutableStateOf(false) }
    val isReadOnly = (targetCreatorId != null && targetCreatorId != myUid) || isPreviewMode

    var selectedPersonForDetails by remember { mutableStateOf<PersonEntity?>(null) }
    var selectedPersonForAddingRelation by remember { mutableStateOf<PersonEntity?>(null) }
    var isAddingAscendant by remember { mutableStateOf(false) }
    var showRelationTypeChoice by remember { mutableStateOf(false) }
    var showChildSelectionForCoParent by remember { mutableStateOf(false) }
    val selectedChildrenIds = remember { mutableStateListOf<String>() }
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
                    InfoButton(
                        title = "Confidentialité de l'Arbre",
                        points = listOf("Cet arbre sera visible dans son intégralité par tous vos Destinataires une fois votre héritage activé, sans restriction personne par personne — contrairement au reste de l'application.")
                    )
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
                            onClick = { 
                                selectedPersonForAddingRelation = null
                                isAddingAscendant = false
                                showCreateDialog = true 
                            },
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
                        selectedPersonForAddingRelation = allPersons.find { it.id == resolved.id }
                        showRelationTypeChoice = true
                    },
                    enabled = !isReadOnly
                )
            } else {
                // VUE LISTE PAR GROUPES (v9.4.26)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(treeGroups) { group ->
                        GroupTreeNode(
                            group = group,
                            allPersons = allPersons,
                            onAddChild = { 
                                selectedPersonForAddingRelation = it
                                showRelationTypeChoice = true
                            },
                            onShowDetails = { selectedPersonForDetails = it },
                            enabled = !isReadOnly,
                            accent = accent
                        )
                    }
                }
            }
        }
    }

    if (showRelationTypeChoice && selectedPersonForAddingRelation != null) {
        ModalBottomSheet(
            onDismissRequest = { showRelationTypeChoice = false },
            containerColor = theme.backgroundColor
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Nouvelle relation pour ${selectedPersonForAddingRelation!!.firstName}",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.contentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    InfoButton(
                        title = "Types de liens",
                        points = listOf(
                            "Parent : Cette personne apparaîtra au-dessus, reliée par un lien de filiation directe.",
                            "Enfant : Cette personne apparaîtra en dessous.",
                            "Co-parent : Pour lier une personne qui partage un enfant déjà existant avec celle-ci."
                        )
                    )
                }
                Spacer(Modifier.height(24.dp))
                
                RelationOption(
                    title = "Ajouter un parent",
                    description = "Cette personne apparaîtra au-dessus, reliée par un lien de filiation directe.",
                    icon = Icons.Default.ArrowUpward,
                    accent = accent,
                    contentColor = theme.contentColor,
                    onClick = {
                        isAddingAscendant = true
                        showRelationTypeChoice = false
                        showCreateDialog = true
                    }
                )
                
                Spacer(Modifier.height(16.dp))
                
                RelationOption(
                    title = "Ajouter un enfant",
                    description = "Cette personne apparaîtra en dessous.",
                    icon = Icons.Default.ArrowDownward,
                    accent = accent,
                    contentColor = theme.contentColor,
                    onClick = {
                        isAddingAscendant = false
                        showRelationTypeChoice = false
                        showCreateDialog = true
                    }
                )
                
                Spacer(Modifier.height(16.dp))
                
                RelationOption(
                    title = "Ajouter un co-parent",
                    description = "Pour lier une personne qui partage un enfant déjà existant avec celle-ci.",
                    icon = Icons.Default.Group,
                    accent = accent,
                    contentColor = theme.contentColor,
                    onClick = {
                        showRelationTypeChoice = false
                        showChildSelectionForCoParent = true
                    }
                )
            }
        }
    }

    if (showChildSelectionForCoParent && selectedPersonForAddingRelation != null) {
        val children by viewModel.getChildrenOf(selectedPersonForAddingRelation!!.id).collectAsState(initial = emptyList())
        
        AlertDialog(
            onDismissRequest = { showChildSelectionForCoParent = false },
            containerColor = theme.backgroundColor,
            title = { Text("Sélectionner l'enfant concerné", color = theme.contentColor) },
            text = {
                if (children.isEmpty()) {
                    Text("Cette personne n'a pas encore d'enfant dans l'arbre — ajoutez d'abord un enfant avant de pouvoir lui associer un co-parent.", color = theme.contentColor.copy(alpha = 0.7f))
                } else {
                    Column {
                        Text("À quel(s) enfant(s) de ${selectedPersonForAddingRelation!!.firstName} souhaitez-vous lier ce nouveau parent ?", 
                            style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(children) { child ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (selectedChildrenIds.contains(child.id)) selectedChildrenIds.remove(child.id)
                                        else selectedChildrenIds.add(child.id)
                                    }.padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedChildrenIds.contains(child.id),
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = accent)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(child.firstName, color = theme.contentColor)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (children.isNotEmpty()) {
                    Button(
                        onClick = {
                            showChildSelectionForCoParent = false
                            showCreateDialog = true
                        },
                        enabled = selectedChildrenIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) { Text("Suivant", color = theme.backgroundColor) }
                } else {
                    TextButton(onClick = { showChildSelectionForCoParent = false }) { Text("Compris", color = accent) }
                }
            },
            dismissButton = {
                if (children.isNotEmpty()) {
                    TextButton(onClick = { 
                        showChildSelectionForCoParent = false
                        selectedChildrenIds.clear()
                    }) { Text("Annuler", color = theme.contentColor) }
                }
            }
        )
    }

    if (selectedPersonForDetails != null) {
        PersonDetailsDialog(
            person = selectedPersonForDetails!!,
            viewModel = viewModel,
            onDismiss = { selectedPersonForDetails = null },
            accent = accent,
            isReadOnly = isReadOnly,
            onEditLinks = { 
                selectedPersonForAddingRelation = null
                showCreateDialog = true 
            }
        )
    }

    if (showCreateDialog) {
        CreateOrEditPersonInTreeDialog(
            initialPerson = if (selectedPersonForAddingRelation == null) selectedPersonForDetails else null,
            initialParents = if (!isAddingAscendant && selectedChildrenIds.isEmpty()) {
                // v9.4.25: On ne pré-remplit le parent que pour le flux "Ajouter un enfant"
                selectedPersonForAddingRelation?.let { listOf(it) } ?: emptyList()
            } else emptyList(),
            allPersons = allPersons,
            onConfirm = { firstName, lastName, parentIds ->
                if (selectedPersonForAddingRelation == null && selectedPersonForDetails != null) {
                    viewModel.updatePersonIdentity(selectedPersonForDetails!!.id, firstName, lastName, parentIds)
                } else {
                    val childrenToLink = if (selectedChildrenIds.isNotEmpty()) {
                        selectedChildrenIds.toList()
                    } else if (isAddingAscendant) {
                        listOfNotNull(selectedPersonForAddingRelation?.id)
                    } else {
                        emptyList()
                    }

                    viewModel.createAndLinkPerson(
                        firstName = firstName, 
                        lastName = lastName, 
                        parentIds = parentIds,
                        childrenIdsToLink = childrenToLink
                    )
                }
                showCreateDialog = false
                selectedPersonForAddingRelation = null
                selectedPersonForDetails = null 
                selectedChildrenIds.clear()
                isAddingAscendant = false
            },
            onDismiss = { 
                showCreateDialog = false
                selectedPersonForAddingRelation = null
                selectedChildrenIds.clear()
                isAddingAscendant = false
            },
            accent = accent
        )
    }

    // v9.4.25 : Bulle Assistant IA
    com.example.phoenx.ui.components.FloatingAssistantBubble(
        initialX = assistantX,
        initialY = assistantY,
        onPositionChanged = { x, y -> assistantViewModel.savePosition(x, y) },
        onClick = { assistantViewModel.toggleChat() }
    )

    if (isAssistantChatOpen) {
        com.example.phoenx.ui.screens.assistant.AssistantChatPanel(
            viewModel = assistantViewModel,
            onDismiss = { assistantViewModel.toggleChat() }
        )
    }
}

@Composable
fun RelationOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = contentColor.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = contentColor)
                Text(description, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = contentColor.copy(alpha = 0.3f))
        }
    }
}

/**
 * Composant de rendu pour un groupe de co-parents (v9.4.26)
 */
@Composable
fun GroupTreeNode(
    group: VisualGroup,
    allPersons: List<PersonEntity>,
    onAddChild: (PersonEntity) -> Unit,
    onShowDetails: (PersonEntity) -> Unit,
    enabled: Boolean = true,
    accent: Color
) {
    val theme = LocalAppTheme.current

    Column(modifier = Modifier.padding(start = (if (group.level > 0) 24 else 0).dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = theme.contentColor.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                group.members.forEachIndexed { index, resolved ->
                    val personEntity = allPersons.find { it.id == resolved.id }
                    if (personEntity != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowDetails(personEntity) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CameoPortrait(
                                imagePath = resolved.photoUrl,
                                firstName = resolved.firstName,
                                size = 40.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resolved.firstName + (resolved.lastName?.let { " $it" } ?: ""),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (resolved.isDeceased) theme.contentColor.copy(alpha = 0.5f) else theme.contentColor
                                )
                                if (resolved.isDeceased) {
                                    Text("Décédé(e)", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.3f))
                                }
                            }
                            if (enabled) {
                                IconButton(onClick = { onAddChild(personEntity) }) {
                                    Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        
                        if (index < group.members.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = theme.contentColor.copy(alpha = 0.05f)
                            )
                        }
                    }
                }
            }
        }

        if (group.children.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            group.children.forEach { childGroup ->
                GroupTreeNode(
                    group = childGroup,
                    allPersons = allPersons,
                    onAddChild = onAddChild,
                    onShowDetails = onShowDetails,
                    enabled = enabled,
                    accent = accent
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
    var relationLabel by remember(person) { mutableStateOf(person.reparentedRelationLabel ?: "") }
    var profilePhotoPath by remember(person) { mutableStateOf(person.imagePath) }
    var showDeleteConfirm by remember { mutableStateOf(false) } // v9.4.22
    val children by viewModel.getChildrenOf(person.id).collectAsState(initial = emptyList())
    val deleteRelationLabels = remember { mutableStateMapOf<String, String>() }

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

    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val fileName = "person_profile_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            profilePhotoPath = file.absolutePath
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = theme.backgroundColor,
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // CONTENU SCROLLABLE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            CameoPortrait(
                                imagePath = profilePhotoPath, 
                                firstName = person.firstName, 
                                size = 80.dp
                            )
                            if (!isReadOnly) {
                                IconButton(
                                    onClick = { profilePhotoPicker.launch("image/*") },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(accent, CircleShape)
                                        .border(2.dp, theme.backgroundColor, CircleShape)
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, tint = theme.backgroundColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(person.firstName, style = MaterialTheme.typography.headlineSmall, color = theme.contentColor)
                            
                            if (!isReadOnly) {
                                // Case "Décédé(e)"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isDeceased,
                                        onCheckedChange = { isDeceased = it },
                                        colors = CheckboxDefaults.colors(checkedColor = accent)
                                    )
                                    Text("Décédé(e)", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                                }
                                
                                // Bouton "Modifier identité / liens" (v9.4.24: Correction Layout)
                                OutlinedButton(
                                    onClick = onEditLinks,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor)
                                ) {
                                    Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Modifier identité / liens", style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (person.isDeceased) {
                                Text("Décédé(e)", style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    if (person.isReparented) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = relationLabel,
                            onValueChange = { relationLabel = it },
                            label = { Text("Nature du lien (ex: Petit-fils)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    Text("BIOGRAPHIE", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                    if (!isReadOnly) {
                        OutlinedTextField(
                            value = biography,
                            onValueChange = { biography = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            placeholder = { Text("Quelques mots sur sa vie...", fontStyle = FontStyle.Italic) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                        )
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("GALERIE MÉDIA", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                            InfoButton(
                                title = "Photo de profil vs Galerie",
                                points = listOf("La photo de profil est celle affichée dans l'arbre. La galerie peut contenir d'autres photos et vidéos, visibles uniquement en ouvrant le détail de cette personne.")
                            )
                        }
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
                }

                // BARRE D'ACTIONS FIXE (STICKY)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = theme.backgroundColor,
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f)),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!isReadOnly) {
                            Button(
                                onClick = { 
                                    viewModel.savePersonDetails(person.id, biography, relationLabel.ifBlank { null }, isDeceased, profilePhotoPath)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("Enregistrer les modifications", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(12.dp))

                            TextButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp), tint = Error)
                                Spacer(Modifier.width(8.dp))
                                Text("Supprimer de l'arbre", color = Error, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = theme.contentColor.copy(alpha = 0.05f), contentColor = theme.contentColor)
                            ) { Text("Fermer") }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = theme.backgroundColor,
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Supprimer cette personne ?", color = theme.contentColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    InfoButton(
                        title = "Continuité des liens",
                        points = listOf("Si cette personne a des parents renseignés, ses enfants seront automatiquement rattachés à eux plutôt que de perdre tout lien. Vous pourrez préciser vous-même la nature de ce nouveau lien juste après.")
                    )
                }
            },
            text = {
                Column {
                    Text("Cette action est irréversible. Elle sera retirée de votre répertoire, de l'arbre généalogique et tous ses médias seront effacés.", color = theme.contentColor.copy(alpha = 0.7f))
                    
                    if (children.isNotEmpty() && !person.parentIds.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Les enfants suivants seront rattachés directement à leurs grands-parents. Comment décrire ce nouveau lien ?", style = MaterialTheme.typography.labelSmall, color = accent)
                        
                        children.forEach { child ->
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = deleteRelationLabels[child.id] ?: "",
                                onValueChange = { deleteRelationLabels[child.id] = it },
                                label = { Text("Lien pour ${child.firstName}") },
                                placeholder = { Text("ex: Petit-fils") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePerson(person.id, deleteRelationLabels.toMap())
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
