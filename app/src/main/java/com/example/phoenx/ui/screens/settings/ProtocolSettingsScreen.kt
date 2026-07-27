package com.example.phoenx.ui.screens.settings

import android.net.Uri
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.DepositaryEntity
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.components.PhoenXAvatar
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProtocolViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showInviteDialog by remember { mutableStateOf<String?>(null) } // role: primary | secondary

    // Gestion du succès
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Invitation envoyée avec succès.")
            viewModel.clearSuccess()
        }
    }

    // Gestion des erreurs
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
                title = { 
                    Text(
                        "Transmission & Protocole", 
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                actions = {
                    InfoPoint(
                        title = "La Transmission",
                        content = "Les Gardiens (Dépositaires) sont les seules personnes qui pourront confirmer votre départ pour ouvrir l'accès à vos souvenirs. Le délai de contestation est votre sécurité pour annuler une fausse alerte."
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.contentColor
                )
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
            Text(
                "Gère ton héritage",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 1 : LES GARDIENS
            Text(
                "MES GARDIENS DE CONFIANCE", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                color = theme.contentColor.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Gardien Principal
            DepositaryCard(
                title = "Gardien Principal",
                role = "primary",
                depositary = uiState.depositaries.find { it.role == "primary" },
                onInvite = { showInviteDialog = "primary" },
                onDelete = { viewModel.removeDepositary("primary") },
                accent = accent
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gardien Secondaire
            DepositaryCard(
                title = "Gardien Secondaire",
                role = "secondary",
                depositary = uiState.depositaries.find { it.role == "secondary" },
                onInvite = { showInviteDialog = "secondary" },
                onDelete = { viewModel.removeDepositary("secondary") },
                accent = accent
            )

            Spacer(modifier = Modifier.height(40.dp))

            // SECTION 2 : DÉLAI DE CONTESTATION
            Text(
                "SÉCURITÉ ANTI-ERREUR", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                color = theme.contentColor.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Temps dont tu disposeras pour annuler une activation par erreur avant que tes souvenirs ne soient transmis.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Slider(
                value = uiState.thresholdHours.toFloat(),
                onValueChange = { viewModel.updateThreshold(it.toInt()) },
                valueRange = 24f..72f,
                steps = 2,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
            Text("${uiState.thresholdHours} heures", style = MaterialTheme.typography.bodyLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                "Note : Ce protocole est moral et privé. Il ne remplace pas les dispositions légales de succession.",
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showInviteDialog != null) {
            InviteDepositaryDialog(
                role = showInviteDialog!!,
                onDismiss = { showInviteDialog = null },
                onConfirm = { name, email, imageUri ->
                    viewModel.inviteDepositary(name, email, showInviteDialog!!, imageUri)
                    showInviteDialog = null
                },
                accent = accent
            )
        }
    }
}

@Composable
fun DepositaryCard(
    title: String,
    role: String,
    depositary: DepositaryEntity?,
    onInvite: () -> Unit,
    onDelete: () -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    val isInvited = depositary?.status == "invited"
    val isActive = depositary?.status == "active"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (depositary != null) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f))
                if (depositary != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (depositary == null) {
                Button(
                    onClick = onInvite,
                    colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.1f), contentColor = accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Désigner un Gardien", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PhoenXAvatar(
                        photoUrl = depositary.photoUrl,
                        name = depositary.name,
                        size = 40.dp,
                        borderColor = accent.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(depositary.name, style = MaterialTheme.typography.bodyLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text(depositary.email, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (isActive) Success else if (isInvited) Warning else theme.contentColor.copy(alpha = 0.4f)
                    Surface(modifier = Modifier.size(6.dp), shape = RoundedCornerShape(3.dp), color = statusColor) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isActive) "Liaison active" else "Invitation en attente",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
fun InviteDepositaryDialog(
    role: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Uri?) -> Unit,
    accent: Color
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val theme = LocalAppTheme.current
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            tempPhotoUri = uri
            showCropDialog = true
        }
    }

    if (showCropDialog && tempPhotoUri != null) {
        com.example.phoenx.ui.components.CameoCropDialog(
            imageUri = tempPhotoUri!!,
            onDismiss = { showCropDialog = false },
            onConfirmed = { croppedUri ->
                selectedImageUri = croppedUri
                showCropDialog = false
            },
            accent = accent
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { 
            Text(
                if (role == "primary") "Désigner le Gardien Principal" else "Désigner le Gardien Secondaire", 
                color = theme.contentColor,
                style = MaterialTheme.typography.titleMedium
            ) 
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // SÉLECTEUR DE PHOTO (v9.2.2)
                Box(
                    modifier = Modifier
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    PhoenXAvatar(
                        photoUrl = selectedImageUri?.toString(),
                        name = if (name.isBlank()) "?" else name,
                        size = 80.dp
                    )
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = accent,
                        border = androidx.compose.foundation.BorderStroke(2.dp, theme.backgroundColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddAPhoto, null, tint = theme.backgroundColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Text(
                    "Cette personne recevra une invitation par email pour confirmer son rôle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Adresse email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, selectedImageUri) },
                enabled = name.isNotBlank() && email.contains("@"),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Inviter", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    )
}
