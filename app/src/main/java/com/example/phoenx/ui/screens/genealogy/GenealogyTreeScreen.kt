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
import androidx.compose.ui.text.style.TextOverflow
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
    viewModel: GenealogyTreeViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

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
                    creatorId = targetCreatorId ?: myUid, // v9.4.29
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




