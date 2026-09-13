package com.example.phoenx.ui.screens.witness

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.data.local.WitnessEntity
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.InvitationConfirmDialog
import com.example.phoenx.ui.components.PhoenXAvatar
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WitnessInviteScreen(
    navController: NavController,
    mainViewModel: com.example.phoenx.ui.MainViewModel,
    viewModel: WitnessViewModel = hiltViewModel()
) {
    val witnesses by viewModel.witnesses.collectAsState()
    val creatorName by mainViewModel.userName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showDialog by remember { mutableStateOf(false) }
    var witnessToDelete by remember { mutableStateOf<WitnessEntity?>(null) }
    var witnessToReview by remember { mutableStateOf<WitnessEntity?>(null) }
    var reviewText by remember { mutableStateOf<String?>(null) }
    var isReading by remember { mutableStateOf(false) }

    val error by viewModel.error.collectAsState()
    LaunchedEffect(error) {
        error?.let {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.witness_title), 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = theme.fontFamily, 
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        InfoButton(
                            title = stringResource(R.string.witness_info_title),
                            points = listOf(
                                stringResource(R.string.witness_info_p1),
                                stringResource(R.string.witness_info_p2),
                                stringResource(R.string.witness_info_p3),
                                stringResource(R.string.witness_info_p4),
                                stringResource(R.string.witness_info_p5)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.contentColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = accent,
                contentColor = theme.backgroundColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                stringResource(R.string.witness_invite_description),
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && witnesses.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
                } else if (witnesses.isEmpty()) {
                    Text(
                        stringResource(R.string.witness_empty_list),
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontFamily = theme.fontFamily),
                        color = theme.contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(witnesses) { witness ->
                            WitnessCard(
                                witness = witness,
                                onDelete = { witnessToDelete = witness },
                                onReview = {
                                    if (witness.status == "submitted" || witness.allowCreatorToRead) {
                                        witnessToReview = witness
                                        isReading = true
                                        scope.launch {
                                            reviewText = viewModel.getTestimonyContent(witness.id)
                                            isReading = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (witnessToReview != null) {
            AlertDialog(
                onDismissRequest = { 
                    witnessToReview = null
                    reviewText = null
                },
                containerColor = theme.backgroundColor,
                title = { Text(stringResource(R.string.witness_review_dialog_title, witnessToReview?.name ?: ""), color = theme.contentColor, fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), contentAlignment = Alignment.Center) {
                        if (isReading) {
                            CircularProgressIndicator(color = accent)
                        } else if (reviewText != null) {
                            Text(reviewText!!, color = theme.contentColor, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = theme.fontFamily))
                        } else {
                            Text(stringResource(R.string.witness_review_error), color = Error)
                        }
                    }
                },
                confirmButton = {
                    if (witnessToReview?.status == "submitted") {
                        Row {
                            TextButton(onClick = {
                                viewModel.reviewTestimony(witnessToReview!!.id, false)
                                witnessToReview = null
                                reviewText = null
                            }) {
                                Text(stringResource(R.string.witness_review_button_reject), color = Error)
                            }
                            Button(
                                onClick = {
                                    viewModel.reviewTestimony(witnessToReview!!.id, true)
                                    witnessToReview = null
                                    reviewText = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Success)
                            ) {
                                Text(stringResource(R.string.witness_review_button_validate), color = Color.White)
                            }
                        }
                    } else {
                        TextButton(onClick = { 
                            witnessToReview = null
                            reviewText = null
                        }) {
                            Text(stringResource(R.string.witness_review_button_close), color = theme.contentColor)
                        }
                    }
                }
            )
        }

        if (showDialog) {
            InviteWitnessDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, email, allowRead, allowReject, prompt, imageUri ->
                    viewModel.inviteWitness(name, email, allowRead, allowReject, creatorName, prompt, imageUri)
                    showDialog = false
                }
            )
        }

        if (witnessToDelete != null) {
            AlertDialog(
                onDismissRequest = { witnessToDelete = null },
                containerColor = theme.backgroundColor,
                title = { Text(stringResource(R.string.witness_delete_dialog_title), color = theme.contentColor, fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.witness_delete_dialog_text, witnessToDelete?.name ?: ""), color = theme.contentColor.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(onClick = {
                        witnessToDelete?.let { viewModel.deleteWitness(it.id) }
                        witnessToDelete = null
                    }) {
                        Text(stringResource(R.string.witness_delete_button_confirm), color = Error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { witnessToDelete = null }) {
                        Text(stringResource(R.string.witness_delete_button_cancel), color = theme.contentColor)
                    }
                }
            )
        }
    }
}

@Composable
fun WitnessCard(witness: WitnessEntity, onDelete: () -> Unit, onReview: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onReview() },
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PhoenXAvatar(
                    photoUrl = witness.photoUrl,
                    name = witness.name,
                    size = 44.dp,
                    borderColor = accent.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = witness.name.split(" ").firstOrNull() ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = accent,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    witness.name, 
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold
                    ), 
                    color = theme.contentColor
                )
                Text(witness.email, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
            }

            val (statusColor, statusText) = when (witness.status) {
                "submitted" -> Warning to stringResource(R.string.witness_status_to_verify)
                "validated" -> Success to stringResource(R.string.witness_status_validated)
                "rejected" -> Error to stringResource(R.string.witness_status_rejected)
                else -> AccentPrimary to stringResource(R.string.witness_status_invited)
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = statusColor
                    )
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.padding(top = 4.dp).size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun InviteWitnessDialog(onDismiss: () -> Unit, onConfirm: (String, String, Boolean, Boolean, String?, Uri?) -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var requestPrompt by remember { mutableStateOf("") }
    var allowRead by remember { mutableStateOf(false) }
    var allowReject by remember { mutableStateOf(false) }
    var showInvitationConfirm by remember { mutableStateOf(false) }

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

    if (!showInvitationConfirm) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = theme.backgroundColor,
            title = { 
                Text(
                    stringResource(R.string.witness_dialog_title), 
                    color = theme.contentColor, 
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold)
                ) 
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // SÉLECTEUR DE PHOTO (v9.2.2)
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable { photoPickerLauncher.launch("image/*") },
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

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.witness_dialog_label_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                            focusedLabelColor = accent,
                            unfocusedLabelColor = theme.contentColor.copy(alpha = 0.4f),
                            focusedTextColor = theme.contentColor,
                            unfocusedTextColor = theme.contentColor
                        )
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.witness_dialog_label_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                            focusedLabelColor = accent,
                            unfocusedLabelColor = theme.contentColor.copy(alpha = 0.4f),
                            focusedTextColor = theme.contentColor,
                            unfocusedTextColor = theme.contentColor
                        )
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.witness_dialog_section_orientation), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                            Spacer(modifier = Modifier.width(8.dp))
                            com.example.phoenx.ui.components.InfoPoint(
                                title = stringResource(R.string.witness_dialog_info_title),
                                content = stringResource(R.string.witness_dialog_info_content)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = requestPrompt,
                            onValueChange = { requestPrompt = it },
                            placeholder = { Text(stringResource(R.string.witness_dialog_placeholder_prompt), fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                                focusedTextColor = theme.contentColor,
                                unfocusedTextColor = theme.contentColor
                            )
                        )
                    }
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allowRead,
                                onCheckedChange = { allowRead = it },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Text(stringResource(R.string.witness_dialog_check_allow_read), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allowReject,
                                onCheckedChange = { allowReject = it },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Text(stringResource(R.string.witness_dialog_check_allow_reject), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInvitationConfirm = true },
                    enabled = name.isNotBlank() && email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(stringResource(R.string.witness_dialog_button_next), color = theme.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.witness_dialog_button_cancel), color = theme.contentColor)
                }
            }
        )
    } else {
        InvitationConfirmDialog(
            personName = name,
            onConfirm = { onConfirm(name, email, allowRead, allowReject, if (requestPrompt.isNotBlank()) requestPrompt else null, selectedImageUri) },
            onDismiss = { showInvitationConfirm = false }
        )
    }
}
