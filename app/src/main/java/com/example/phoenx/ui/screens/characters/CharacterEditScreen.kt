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
import androidx.hilt.navigation.compose.hiltViewModel
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
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Modifier le Personnage", style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
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
                                hasChildren, if(relationshipDetail.isBlank()) null else relationshipDetail
                            )
                        },
                        enabled = firstName.isNotBlank()
                    ) {
                        Text("Enregistrer", color = accent, fontWeight = FontWeight.Bold)
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
            SectionTitle("Identité", accent)
            
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Prénom") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Nom (facultatif)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Relation", accent)

            OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Lien (ex: Compagne, Ami d'enfance)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = relationshipDetail,
                onValueChange = { relationshipDetail = it },
                label = { Text("Précisions sur votre histoire") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Portrait Physique (v9.0)", accent)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Taille (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Poids (kg)") },
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
                    label = { Text("Yeux") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                OutlinedTextField(
                    value = hairColor,
                    onValueChange = { hairColor = it },
                    label = { Text("Cheveux") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Vie Sociale (v9.0)", accent)

            OutlinedTextField(
                value = profession,
                onValueChange = { profession = it },
                label = { Text("Métier / Occupation") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = clothingStyle,
                onValueChange = { clothingStyle = it },
                label = { Text("Style vestimentaire") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("A des enfants ?", color = theme.contentColor, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = hasChildren == true,
                    onClick = { hasChildren = if (hasChildren == true) null else true },
                    label = { Text("Oui") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = hasChildren == false,
                    onClick = { hasChildren = if (hasChildren == false) null else false },
                    label = { Text("Non") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
            
            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Supprimer ce personnage")
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Supprimer ${character?.firstName} ?") },
                text = { 
                    Text("Ce personnage apparaît dans $appearanceCount souvenirs — le supprimer le retirera de ces récits, sans supprimer les souvenirs eux-mêmes.") 
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showDeleteConfirm = false
                            viewModel.deleteCharacter(personId) 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Supprimer définitivement", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
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
