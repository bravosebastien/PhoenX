package com.example.phoenx.ui.screens.witness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.local.WitnessEntity
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.InvitationConfirmDialog
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
                            "Les Témoins", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = theme.fontFamily, 
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        InfoButton(
                            title = "Les Témoins",
                            points = listOf(
                                "Invite des proches à témoigner sur toi — tu ne verras jamais ce qu'ils écrivent.",
                                "Leurs témoignages sont chiffrés et scellés jusqu'à l'activation du protocole.",
                                "C'est une mémoire à 360° : ton histoire vue par les yeux de ceux qui t'aiment.",
                                "Chaque témoin reçoit un lien unique par email.",
                                "Tu peux choisir d'autoriser la lecture de ton vivant pour certains témoins."
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
                "Invite des proches à raconter un souvenir sur toi. Leurs mots enrichiront ton héritage.",
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
                        "Aucun témoin pour l'instant.",
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
                title = { Text("Témoignage de ${witnessToReview?.name}", color = theme.contentColor, fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), contentAlignment = Alignment.Center) {
                        if (isReading) {
                            CircularProgressIndicator(color = accent)
                        } else if (reviewText != null) {
                            Text(reviewText!!, color = theme.contentColor, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = theme.fontFamily))
                        } else {
                            Text("Impossible de lire le témoignage.", color = Error)
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
                                Text("Refuser", color = Error)
                            }
                            Button(
                                onClick = {
                                    viewModel.reviewTestimony(witnessToReview!!.id, true)
                                    witnessToReview = null
                                    reviewText = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Success)
                            ) {
                                Text("Valider", color = Color.White)
                            }
                        }
                    } else {
                        TextButton(onClick = { 
                            witnessToReview = null
                            reviewText = null
                        }) {
                            Text("Fermer", color = theme.contentColor)
                        }
                    }
                }
            )
        }

        if (showDialog) {
            InviteWitnessDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, email, allowRead, allowReject, prompt ->
                    viewModel.inviteWitness(name, email, allowRead, allowReject, creatorName, prompt)
                    showDialog = false
                }
            )
        }

        if (witnessToDelete != null) {
            AlertDialog(
                onDismissRequest = { witnessToDelete = null },
                containerColor = theme.backgroundColor,
                title = { Text("Supprimer ce témoin ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
                text = { Text("Veux-tu vraiment annuler l'invitation de ${witnessToDelete?.name} ?", color = theme.contentColor.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(onClick = {
                        witnessToDelete?.let { viewModel.deleteWitness(it.id) }
                        witnessToDelete = null
                    }) {
                        Text("Supprimer", color = Error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { witnessToDelete = null }) {
                        Text("Annuler", color = theme.contentColor)
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
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PersonOutline, null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
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
                "submitted" -> Warning to "À vérifier"
                "validated" -> Success to "Validé"
                "rejected" -> Error to "Refusé"
                else -> AccentPrimary to "Invité"
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
fun InviteWitnessDialog(onDismiss: () -> Unit, onConfirm: (String, String, Boolean, Boolean, String?) -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var requestPrompt by remember { mutableStateOf("") }
    var allowRead by remember { mutableStateOf(false) }
    var allowReject by remember { mutableStateOf(false) }
    var showInvitationConfirm by remember { mutableStateOf(false) }

    if (!showInvitationConfirm) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = theme.backgroundColor,
            title = { 
                Text(
                    "Inviter un témoin", 
                    color = theme.contentColor, 
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold)
                ) 
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom complet") },
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
                        label = { Text("Email") },
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
                            Text("ORIENTATION DU TÉMOIGNAGE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                            Spacer(modifier = Modifier.width(8.dp))
                            com.example.phoenx.ui.components.InfoPoint(
                                title = "Guider le témoin",
                                content = "Tu peux poser une question précise ou suggérer un thème (ex: 'Raconte notre voyage en Italie', 'Qu'est-ce qui t'a le plus marqué dans mon caractère ?'). Cela aide le témoin à savoir par où commencer."
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = requestPrompt,
                            onValueChange = { requestPrompt = it },
                            placeholder = { Text("Ex: Quel est ton souvenir le plus drôle avec moi ?", fontSize = 14.sp) },
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
                            Text("M'autoriser à lire ce témoignage de mon vivant", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allowReject,
                                onCheckedChange = { allowReject = it },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Text("Droit de regard : pouvoir refuser le témoignage si inapproprié", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
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
                    Text("Suivant", color = theme.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    } else {
        InvitationConfirmDialog(
            personName = name,
            onConfirm = { onConfirm(name, email, allowRead, allowReject, if (requestPrompt.isNotBlank()) requestPrompt else null) },
            onDismiss = { showInvitationConfirm = false }
        )
    }
}
