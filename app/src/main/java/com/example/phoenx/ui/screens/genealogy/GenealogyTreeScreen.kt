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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenealogyTreeScreen(
    navController: NavController,
    viewModel: GenealogyTreeViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val rootPersons by viewModel.rootPersons.collectAsState()
    val allPersons by viewModel.allPersons.collectAsState()

    var selectedPersonForDetails by remember { mutableStateOf<PersonEntity?>(null) }
    var selectedPersonForAddingChild by remember { mutableStateOf<PersonEntity?>(null) }
    var showPersonSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon Arbre Généalogique", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (rootPersons.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(64.dp), tint = theme.contentColor.copy(alpha = 0.2f))
                            Spacer(Modifier.height(16.dp))
                            Text("Ton arbre est encore vide.", color = theme.contentColor.copy(alpha = 0.4f))
                            Button(
                                onClick = { showPersonSelector = true; selectedPersonForAddingChild = null },
                                modifier = Modifier.padding(top = 24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("Ajouter la première personne", color = theme.backgroundColor)
                            }
                        }
                    }
                }
            } else {
                items(rootPersons) { person ->
                    PersonTreeNode(
                        person = person,
                        level = 0,
                        viewModel = viewModel,
                        onAddChild = { 
                            selectedPersonForAddingChild = it
                            showPersonSelector = true 
                        },
                        onShowDetails = { selectedPersonForDetails = it }
                    )
                }
                
                item {
                    TextButton(
                        onClick = { showPersonSelector = true; selectedPersonForAddingChild = null },
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

    if (showPersonSelector) {
        val suggestedPersons = allPersons.filter { 
            val isAlreadyRoot = rootPersons.any { root -> root.id == it.id }
            !isAlreadyRoot && it.id != selectedPersonForAddingChild?.id
        }

        ModalBottomSheet(onDismissRequest = { showPersonSelector = false; selectedPersonForAddingChild = null }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text(
                    text = if (selectedPersonForAddingChild != null) "Ajouter un enfant de ${selectedPersonForAddingChild!!.firstName}" else "Ajouter une personne à l'arbre",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.contentColor
                )
                Spacer(Modifier.height(16.dp))
                
                PersonSelector(
                    selectedPersons = emptyList(),
                    suggestedPersons = suggestedPersons,
                    onSearch = { /* Implementation handled by ViewModel if needed, but here simple filter is enough */ },
                    onSelect = { person ->
                        if (selectedPersonForAddingChild != null) {
                            viewModel.linkParent(selectedPersonForAddingChild!!.id, person.id)
                        }
                        showPersonSelector = false
                        selectedPersonForAddingChild = null
                    },
                    onCreate = { f, l, r, dt, dv, uri, ct ->
                        // TODO: Create person and link parent if needed
                        // This would need a separate function in VM to create and link
                        showPersonSelector = false
                        selectedPersonForAddingChild = null
                    },
                    onRemove = {},
                    accent = accent
                )
            }
        }
    }

    if (selectedPersonForDetails != null) {
        PersonDetailsDialog(
            person = selectedPersonForDetails!!,
            viewModel = viewModel,
            onDismiss = { selectedPersonForDetails = null },
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
    onShowDetails: (PersonEntity) -> Unit
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
                IconButton(onClick = { onAddChild(person) }) {
                    Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(20.dp))
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
                    onShowDetails = onShowDetails
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
    accent: Color
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val mediaList by viewModel.getMediaForPerson(person.id).collectAsState(initial = emptyList())
    
    var biography by remember(person) { mutableStateOf(person.biography) }
    var isDeceased by remember(person) { mutableStateOf(person.isDeceased) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Save to internal storage then add to DB
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
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                Text("BIOGRAPHIE", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
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

                Spacer(Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("GALERIE MÉDIA", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                    IconButton(onClick = { photoPicker.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = accent)
                    }
                }
                
                // Privacy Warning
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

                if (mediaList.isEmpty()) {
                    Text("Aucun média ajouté.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    mediaList.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { media ->
                                MediaThumbnail(media = media, onRemove = { viewModel.removeMedia(media) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.contentColor.copy(alpha = 0.05f), contentColor = theme.contentColor)
                ) { Text("Fermer") }
            }
        }
    }
}

@Composable
fun MediaThumbnail(media: PersonMediaEntity, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = media.mediaPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}
