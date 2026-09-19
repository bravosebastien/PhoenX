package com.example.phoenx.ui.screens.characters

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.components.CameoCropDialog
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterEditScreen(
    personId: String,
    onNavigateBack: () -> Unit,
    viewModel: CharacterEditViewModel = hiltViewModel()
) {
    val character by viewModel.character.collectAsState()
    val appearanceCount by viewModel.appearanceCount.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLinkedElsewhereInfo by remember { mutableStateOf(false) }

    // v13.0 : une personne présente dans l'Arbre Généalogique et/ou les Rencontres
    // ne peut pas être supprimée depuis cet écran générique "Mes Personnages" —
    // il faut passer par l'écran dédié pour ne rien casser (lignée, données de rencontre).
    val personCategories = character?.categories?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val isLinkedElsewhere = personCategories.contains("FAMILY") || personCategories.contains("ENCOUNTER")

    LaunchedEffect(personId) {
        viewModel.loadCharacter(personId)
    }

    LaunchedEffect(Unit) {
        viewModel.isSaved.collect {
            onNavigateBack()
        }
    }

    // États du formulaire
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var distinctionType by remember { mutableStateOf("autre") }
    var distinctionValue by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Nouveaux champs v9.0
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var eyeColor by remember { mutableStateOf("") }
    var hairColor by remember { mutableStateOf("") }
    var clothingStyle by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var hasChildren by remember { mutableStateOf<Boolean?>(null) }
    var relationshipDetail by remember { mutableStateOf("") }
    var characterType by remember { mutableStateOf("HUMAN") }

    var showCropDialog by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            tempImageUri = uri
            showCropDialog = true
        }
    }

    // Initialisation
    LaunchedEffect(character) {
        character?.let {
            firstName = it.firstName
            lastName = it.lastName ?: ""
            relationship = it.relationship ?: ""
            distinctionType = it.distinctionType ?: "autre"
            distinctionValue = it.distinctionValue ?: ""
            height = it.height?.toString() ?: ""
            weight = it.weight?.toString() ?: ""
            eyeColor = it.eyeColor ?: ""
            hairColor = it.hairColor ?: ""
            clothingStyle = it.clothingStyle ?: ""
            profession = it.profession ?: ""
            hasChildren = it.hasChildren
            relationshipDetail = it.relationshipDetail ?: ""
            characterType = it.characterType ?: "HUMAN"
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.character_edit_title), style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor) }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateCharacter(
                                firstName, if(lastName.isBlank()) null else lastName, 
                                if(relationship.isBlank()) null else relationship, 
                                distinctionType, if(distinctionValue.isBlank()) null else distinctionValue, 
                                selectedImageUri, height.toIntOrNull(), weight.toIntOrNull(),
                                if(eyeColor.isBlank()) null else eyeColor, if(hairColor.isBlank()) null else hairColor,
                                if(clothingStyle.isBlank()) null else clothingStyle, if(profession.isBlank()) null else profession,
                                hasChildren, if(relationshipDetail.isBlank()) null else relationshipDetail,
                                characterType
                            )
                        },
                        enabled = firstName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.character_edit_button_save), color = accent, fontWeight = FontWeight.Bold)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TYPE SELECTOR (v9.1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = characterType == "HUMAN",
                    onClick = { characterType = "HUMAN" },
                    label = { Text(stringResource(R.string.character_type_human)) },
                    leadingIcon = if (characterType == "HUMAN") {
                        { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor, selectedLeadingIconColor = theme.backgroundColor)
                )
                Spacer(Modifier.width(12.dp))
                FilterChip(
                    selected = characterType == "ANIMAL",
                    onClick = { characterType = "ANIMAL" },
                    label = { Text(stringResource(R.string.character_type_animal)) },
                    leadingIcon = if (characterType == "ANIMAL") {
                        { Icon(Icons.Default.Pets, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor, selectedLeadingIconColor = theme.backgroundColor)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PORTRAIT CAMEO
            Box(contentAlignment = Alignment.BottomEnd) {
                CameoPortrait(
                    imagePath = if (selectedImageUri != null) selectedImageUri.toString() else character?.imagePath,
                    firstName = firstName,
                    size = 120.dp
                )
                FloatingActionButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.size(40.dp),
                    containerColor = accent,
                    contentColor = theme.backgroundColor,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // IDENTITÉ DE BASE
            SectionTitle(stringResource(R.string.character_section_identity), accent)
            
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(if (characterType == "HUMAN") stringResource(R.string.character_label_first_name) else stringResource(R.string.character_label_pet_name)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )
            
            if (characterType == "HUMAN") {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(stringResource(R.string.character_label_last_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.character_section_relationship), accent)

            OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text(if (characterType == "HUMAN") stringResource(R.string.character_label_relationship_human) else stringResource(R.string.character_label_relationship_animal)) },
                placeholder = { Text(if (characterType == "HUMAN") stringResource(R.string.character_placeholder_relationship_human) else stringResource(R.string.character_placeholder_relationship_animal)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = relationshipDetail,
                onValueChange = { relationshipDetail = it },
                label = { Text(stringResource(R.string.character_label_history_details)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(if (characterType == "HUMAN") stringResource(R.string.character_section_physical_human) else stringResource(R.string.character_section_physical_animal), accent)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text(stringResource(R.string.character_label_height)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(stringResource(R.string.character_label_weight)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = eyeColor,
                    onValueChange = { eyeColor = it },
                    label = { Text(stringResource(R.string.character_label_eyes)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                OutlinedTextField(
                    value = hairColor,
                    onValueChange = { hairColor = it },
                    label = { Text(if (characterType == "HUMAN") stringResource(R.string.character_label_hair_human) else stringResource(R.string.character_label_hair_animal)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
            }

            if (characterType == "HUMAN") {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(stringResource(R.string.character_section_social), accent)

                OutlinedTextField(
                    value = profession,
                    onValueChange = { profession = it },
                    label = { Text(stringResource(R.string.character_label_profession)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = clothingStyle,
                    onValueChange = { clothingStyle = it },
                    label = { Text(stringResource(R.string.character_label_clothing)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.character_label_has_children), color = theme.contentColor, modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = hasChildren == true,
                        onClick = { hasChildren = if (hasChildren == true) null else true },
                        label = { Text(stringResource(R.string.character_label_yes)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = hasChildren == false,
                        onClick = { hasChildren = if (hasChildren == false) null else false },
                        label = { Text(stringResource(R.string.character_label_no)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                    )
                }
            } else {
                // ANIMAL - On réutilise le champ profession pour la Race/Espèce
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(stringResource(R.string.character_section_species), accent)
                OutlinedTextField(
                    value = profession,
                    onValueChange = { profession = it },
                    label = { Text(stringResource(R.string.character_label_species)) },
                    placeholder = { Text(stringResource(R.string.character_placeholder_species)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
            
            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { if (isLinkedElsewhere) showLinkedElsewhereInfo = true else showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.character_button_delete))
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.character_delete_dialog_title, character?.firstName ?: "")) },
                text = { 
                    Text(stringResource(R.string.character_delete_dialog_text, appearanceCount)) 
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showDeleteConfirm = false
                            viewModel.deleteCharacter(personId) 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(stringResource(R.string.character_delete_confirm), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.character_delete_cancel), color = theme.contentColor.copy(alpha = 0.6f))
                    }
                },
                containerColor = theme.backgroundColor,
                titleContentColor = theme.contentColor,
                textContentColor = theme.contentColor.copy(alpha = 0.8f)
            )
        }

        if (showLinkedElsewhereInfo) {
            AlertDialog(
                onDismissRequest = { showLinkedElsewhereInfo = false },
                title = { Text(stringResource(R.string.character_delete_linked_title)) },
                text = { Text(stringResource(R.string.character_delete_linked_text)) },
                confirmButton = {
                    Button(
                        onClick = { showLinkedElsewhereInfo = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text(stringResource(R.string.character_delete_linked_ok), color = theme.backgroundColor)
                    }
                },
                containerColor = theme.backgroundColor,
                titleContentColor = theme.contentColor,
                textContentColor = theme.contentColor.copy(alpha = 0.8f)
            )
        }

        if (showCropDialog && tempImageUri != null) {
            CameoCropDialog(
                imageUri = tempImageUri!!,
                onDismiss = { showCropDialog = false },
                onConfirmed = { croppedUri ->
                    selectedImageUri = croppedUri
                    showCropDialog = false
                },
                accent = accent
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, accent: Color) {
    val theme = LocalAppTheme.current
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = theme.contentColor.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
