package com.example.phoenx.ui.screens.portraits

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.domain.util.AgeUtils
import com.example.phoenx.ui.screens.recipient.RecipientUiState
import com.example.phoenx.ui.screens.recipient.RecipientViewModel
import com.example.phoenx.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortraitProcheScreen(
    initialRecipientId: String? = null,
    navController: NavController,
    viewModel: PortraitViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val recipients by viewModel.recipients.collectAsState()
    val existingPortrait by viewModel.existingPortrait.collectAsState()
    
    var selectedRecipient by remember { mutableStateOf(recipients.find { it.id == initialRecipientId }) }
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(recipients) {
        if (selectedRecipient == null && initialRecipientId != null) {
            selectedRecipient = recipients.find { it.id == initialRecipientId }
        }
    }

    LaunchedEffect(initialRecipientId) {
        viewModel.setRecipient(initialRecipientId)
    }

    LaunchedEffect(selectedRecipient) {
        viewModel.setRecipient(selectedRecipient?.id)
    }

    LaunchedEffect(existingPortrait) {
        if (existingPortrait != null && text.isEmpty()) {
            text = existingPortrait!!
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("Miroir du proche", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
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
                .padding(24.dp)
        ) {
            Text(
                text = "Voici comment je t'ai vu. Voici ce que j'ai vu en toi que tu n'as peut-être jamais su.",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary,
                    lineHeight = 30.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SÉLECTION DU PROCHE
            Text("POUR QUI EST CE PORTRAIT ?", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (recipients.isEmpty()) {
                Button(
                    onClick = { navController.navigate("recipients") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                ) {
                    Text("Ajouter d'abord un proche", color = AccentPrimary)
                }
            } else {
                Box {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedRecipient == null) Error else TextTertiary)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedRecipient?.name ?: "Choisir un proche",
                                color = if (selectedRecipient == null) TextTertiary else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = AccentPrimary)
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(SurfaceCard).fillMaxWidth(0.85f)
                    ) {
                        recipients.forEach { recipient ->
                            DropdownMenuItem(
                                text = { Text(recipient.name, color = TextPrimary) },
                                onClick = {
                                    selectedRecipient = recipient
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ZONE D'ÉCRITURE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF242429))
                    .padding(20.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Écris librement ce que tu as vu en ${selectedRecipient?.name ?: "ton proche"}...",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 17.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF5C5855)
                        )
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        color = TextPrimary,
                        lineHeight = 28.sp
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QUESTIONS DE GUIDAGE
            var showGuidance by remember { mutableStateOf(false) }
            if (!showGuidance) {
                TextButton(onClick = { showGuidance = true }) {
                    Text("Voir des questions de guidage", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                val guidance = listOf(
                    "Quel souvenir revient en premier ?",
                    "Quelle qualité unique possède-t-il/elle ?",
                    "Qu'est-ce qu'il/elle t'a appris ?",
                    "Que veux-tu qu'il/elle sache sur ce qu'il/elle représente ?"
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    guidance.forEach { query ->
                        SuggestionChip(
                            onClick = { text += (if (text.isNotEmpty()) "\n\n" else "") + query },
                            label = { Text(query, fontSize = 12.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    val recipient = selectedRecipient ?: return@Button
                    viewModel.savePortrait(recipient.id, text)
                },
                enabled = text.isNotBlank() && selectedRecipient != null && uiState !is PortraitUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                if (uiState is PortraitUiState.Loading) CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                else Text("Sceller ce portrait", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
            }

            if (uiState is PortraitUiState.Success) {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Portrait scellé et synchronisé.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            }
        }
    }
}
