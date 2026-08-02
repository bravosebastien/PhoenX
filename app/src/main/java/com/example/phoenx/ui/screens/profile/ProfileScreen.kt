package com.example.phoenx.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.components.CameoCropDialog
import com.example.phoenx.ui.components.PhoenXAvatar
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRichProfile: () -> Unit,
    onNavigateToGenealogy: () -> Unit,
    onLogoutSuccess: () -> Unit,
    mainViewModel: MainViewModel,
    viewModel: ProfileViewModel = hiltViewModel(),
    themeViewModel: com.example.phoenx.ui.theme.ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    
    // v8.9.0 : Thème Global
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundId by themeViewModel.globalBackgroundId.collectAsState()
    val fontId by themeViewModel.globalFontId.collectAsState()
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var isAppearingExpanded by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    var showPhotoOptions by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var showSuspendDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    val isDeleteButtonEnabled = deleteConfirmText == "SUPPRIMER"

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            tempPhotoUri = uri
            showCropDialog = true
        }
    }

    // Gestion de la caméra (v9.2.2)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            showCropDialog = true
        }
    }

    if (showCropDialog && tempPhotoUri != null) {
        CameoCropDialog(
            imageUri = tempPhotoUri!!,
            onDismiss = { showCropDialog = false },
            onConfirmed = { croppedUri ->
                viewModel.updateProfilePhoto(croppedUri)
                showCropDialog = false
            },
            accent = accent
        )
    }

    if (showSuspendDialog) {
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Suspendre mon compte ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Votre compte deviendra inactif et vous serez déconnecté. Vos proches ne recevront plus de notifications de votre part.\n\nRien ne sera supprimé. Vous pourrez réactiver votre compte à tout moment en vous reconnectant.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.suspendAccount {
                        mainViewModel.logout()
                        onLogoutSuccess()
                    }
                }) { Text("Confirmer la suspension", color = Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSuspendDialog = false }) { Text("Annuler", color = theme.contentColor) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("SUPPRESSION DÉFINITIVE", color = Error, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Cette action est IRREVERSIBLE. Toutes vos données (souvenirs, photos, voix, cercle) seront définitivement effacées conformément au RGPD.\n\nPour confirmer, tapez SUPPRIMER ci-dessous :", color = theme.contentColor.copy(alpha = 0.7f))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it.uppercase() },
                        placeholder = { Text("SUPPRIMER") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Error,
                            unfocusedBorderColor = theme.contentColor.copy(alpha = 0.4f),
                            focusedTextColor = theme.contentColor,
                            unfocusedTextColor = theme.contentColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount { result ->
                            when(result) {
                                is DeleteResult.Success -> { onLogoutSuccess() }
                                is DeleteResult.RequiresReauth -> {
                                    Toast.makeText(context, "Sécurité : Reconnectez-vous avant de supprimer votre compte.", Toast.LENGTH_LONG).show()
                                    mainViewModel.logout()
                                    onLogoutSuccess()
                                }
                                is DeleteResult.Error -> {
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = isDeleteButtonEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text("Supprimer définitivement", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler", color = theme.contentColor) }
            }
        )
    }

    if (showPhotoOptions) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoOptions = false },
            containerColor = theme.backgroundColor
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("Ma photo de profil", style = MaterialTheme.typography.titleLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                
                ListItem(
                    headlineContent = { Text("Prendre une photo", color = theme.contentColor) },
                    leadingContent = { Icon(Icons.Default.PhotoCamera, null, tint = accent) },
                    modifier = Modifier.clickable {
                        showPhotoOptions = false
                        val uri = com.example.phoenx.domain.util.FileUtils.getTempImageUri(context)
                        tempPhotoUri = uri
                        cameraLauncher.launch(uri)
                    }
                )
                ListItem(
                    headlineContent = { Text("Choisir dans la galerie", color = theme.contentColor) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null, tint = accent) },
                    modifier = Modifier.clickable {
                        showPhotoOptions = false
                        photoPickerLauncher.launch("image/*")
                    }
                )
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mon Profil", color = theme.contentColor, fontFamily = theme.fontFamily) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // AVATAR
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { showPhotoOptions = true },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    PhoenXAvatar(
                        photoUrl = uiState.photoUrl,
                        name = uiState.displayName,
                        size = 100.dp,
                        borderColor = accent.copy(alpha = 0.3f)
                    )
                    
                    if (uploadProgress != null) {
                        CircularProgressIndicator(
                            progress = { uploadProgress!! },
                            modifier = Modifier.fillMaxSize(),
                            color = accent,
                            strokeWidth = 4.dp
                        )
                    }
                    
                    // Badge édit
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = accent,
                        border = BorderStroke(2.dp, theme.backgroundColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, null, tint = theme.backgroundColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // IDENTITÉ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.displayName.ifEmpty { "Utilisateur" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.contentColor,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { 
                        newName = uiState.displayName
                        showEditDialog = true 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier le nom",
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Text(
                    text = uiState.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Ce nom sera visible par les personnes que vous invitez (Dépositaires, Témoins, Destinataires).",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.contentColor.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // --- PORTRAIT DE VIE (v9.1) ---
                Card(
                    onClick = onNavigateToRichProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.05f)),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = accent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mon Portrait de Vie", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                            Text("Enrichis ton histoire pour l'IA Biographe", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.4f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- ACCORDÉON APPARENCE (v8.9.0 Global) ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAppearingExpanded = !isAppearingExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, null, tint = accent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Apparence & Style", style = MaterialTheme.typography.bodyLarge, color = theme.contentColor)
                            }
                            Icon(
                                if (isAppearingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = theme.contentColor.copy(alpha = 0.4f)
                            )
                        }

                        if (isAppearingExpanded) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            com.example.phoenx.ui.components.GlobalThemeSelector(
                                currentBackgroundId = backgroundId,
                                currentFontId = fontId
                            ) { bg, font -> themeViewModel.setGlobalTheme(bg, font) }

                            Spacer(modifier = Modifier.height(24.dp))

                            TextButton(
                                onClick = { themeViewModel.resetToDefaults() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Réinitialiser les réglages par défaut")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // --- ZONE DE DANGER ---
                Text("ZONE DE DANGER", style = MaterialTheme.typography.labelSmall, color = Error.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { showSuspendDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Suspendre mon compte")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Supprimer définitivement mon compte", color = Color.White)
                }

                Spacer(modifier = Modifier.height(48.dp))

                // BOUTON DE RETOUR
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.contentColor.copy(alpha = 0.1f),
                        contentColor = theme.contentColor
                    )
                ) {
                    Text("Retour aux réglages", color = theme.contentColor)
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Modifier mon nom", color = theme.contentColor) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom d'usage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = theme.contentColor.copy(alpha = 0.4f),
                        focusedLabelColor = accent,
                        cursorColor = accent,
                        unfocusedLabelColor = theme.contentColor.copy(alpha = 0.6f),
                        focusedTextColor = theme.contentColor,
                        unfocusedTextColor = theme.contentColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateDisplayName(newName)
                        showEditDialog = false
                    }
                }) {
                    Text("Enregistrer", color = accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }
}
