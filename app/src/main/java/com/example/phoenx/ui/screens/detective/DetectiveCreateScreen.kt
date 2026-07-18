package com.example.phoenx.ui.screens.detective

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectiveCreateScreen(
    navController: NavController,
    viewModel: DetectiveCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.updatePhotoUri(uri)
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Un secret protégé par une question",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                    color = TextPrimary
                )
                InfoButton(
                    title = "Mode Détective",
                    points = listOf(
                        "Cache un contenu derrière une question dont seul ton proche connaît la réponse.",
                        "La réponse est protégée localement — personne ne peut la lire, même nos serveurs.",
                        "Ton proche a plusieurs tentatives pour trouver la réponse.",
                        "Tape sur 'Besoin d'inspiration ?' pour voir 20 exemples de questions.",
                        "C'est différent du Tiroir à Clé Unique — ici, pas de limite d'ouvertures."
                    )
                )
            }
            Text(
                text = "Seul celui qui connaît la réponse pourra accéder à ce que tu déposes ici.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ÉTAPE 1 — L'ÉNIGME
            Text("LA QUESTION", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.enigmaText,
                onValueChange = { viewModel.updateEnigma(it) },
                placeholder = { Text("Ex : Quel était le nom de notre premier chien ?", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                    unfocusedContainerColor = SurfaceCard,
                    focusedContainerColor = SurfaceCard
                ),
                maxLines = 3
            )

            // BOUTON INSPIRATION
            var showInspiration by remember { mutableStateOf(false) }
            Button(
                onClick = { showInspiration = true },
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Besoin d'inspiration ?",
                        color = AccentPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (showInspiration) {
                InspirationBottomSheet(
                    onDismiss = { showInspiration = false },
                    onSelect = { selectedQuestion ->
                        viewModel.updateEnigma(selectedQuestion)
                        showInspiration = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ÉTAPE 2 — LA RÉPONSE
            Text("LA RÉPONSE (invisible après saisie)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.secretAnswer,
                onValueChange = { viewModel.updateAnswer(it) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 17.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                    unfocusedContainerColor = SurfaceCard,
                    focusedContainerColor = SurfaceCard
                )
            )
            Text(
                "La réponse est protégée localement par une empreinte numérique SHA-256. Personne ne peut la lire, même nos serveurs.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ÉTAPE 2 bis — L'INDICE
            Text("INDICE (Affiché après 3 échecs)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.enigmaHint,
                onValueChange = { viewModel.updateEnigmaHint(it) },
                placeholder = { Text("Ex : C'est le nom d'un animal...", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                    unfocusedContainerColor = SurfaceCard,
                    focusedContainerColor = SurfaceCard
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // SECTION DÉLAI DE GRÂCE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DÉBLOCAGE AUTOMATIQUE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                var autoUnlockEnabled by remember { mutableStateOf(uiState.autoUnlockDays.isNotEmpty()) }
                Switch(
                    checked = autoUnlockEnabled,
                    onCheckedChange = { 
                        autoUnlockEnabled = it
                        if (!it) viewModel.updateAutoUnlockDays("")
                        else viewModel.updateAutoUnlockDays("30")
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.autoUnlockDays.isNotEmpty()) {
                val delayOptions = listOf(7, 14, 30, 60, 90, 180)
                var sliderPosition by remember { mutableFloatStateOf(delayOptions.indexOf(uiState.autoUnlockDays.toIntOrNull() ?: 30).coerceAtLeast(0).toFloat()) }
                
                Slider(
                    value = sliderPosition,
                    onValueChange = { 
                        sliderPosition = it
                        viewModel.updateAutoUnlockDays(delayOptions[it.toInt()].toString())
                    },
                    valueRange = 0f..(delayOptions.size - 1).toFloat(),
                    steps = delayOptions.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPrimary,
                        activeTrackColor = AccentPrimary,
                        inactiveTrackColor = SurfaceCard
                    )
                )
                
                Text(
                    text = "Ouvrir après ${uiState.autoUnlockDays} jours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Ton proche pourra voir le contenu automatiquement après ce délai s'il ne trouve pas la réponse.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "Verrouillage permanent tant que la réponse n'est pas trouvée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("MESSAGE DE RÉVÉLATION (FACULTATIF)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.fallbackMessage,
                onValueChange = { viewModel.updateFallbackMessage(it) },
                placeholder = { Text("Ex: La réponse était [ville]. J'espère que tu t'en souviendras un jour...", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 15.sp, fontStyle = FontStyle.Italic, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color(0xFF242429),
                    focusedContainerColor = Color(0xFF242429)
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ÉTAPE 3 — LE CONTENU
            Text("LE CONTENU SECRET", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                "Ce contenu ne sera accessible qu'après avoir répondu correctement à ta question.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.TEXT,
                    label = "✍️ Texte",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.TEXT) }
                )
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.PHOTO,
                    label = "📷 Photo",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.PHOTO) }
                )
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.AUDIO,
                    label = "🎙️ Audio",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.AUDIO) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.contentType) {
                ContentType.TEXT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF242429))
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value = uiState.textContent,
                            onValueChange = { viewModel.updateTextContent(it) },
                            textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, color = TextPrimary),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                ContentType.PHOTO -> {
                    if (uiState.photoUri == null) {
                        Button(
                            onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                        ) {
                            Text("Choisir une photo", color = AccentPrimary)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = uiState.photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.updatePhotoUri(null) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                    }
                }
                ContentType.AUDIO -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        // Simulation bouton enregistrement
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = AccentPrimary,
                            onClick = { /* TODO */ }
                        ) {
                            Icon(Icons.Default.Mic, null, tint = BackgroundPrimary, modifier = Modifier.padding(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            val canSave = uiState.enigmaText.isNotBlank() && 
                          uiState.secretAnswer.isNotBlank() && 
                          ((uiState.contentType == ContentType.TEXT && uiState.textContent.isNotBlank()) ||
                           (uiState.contentType == ContentType.PHOTO && uiState.photoUri != null))

            Button(
                onClick = { 
                    viewModel.saveDetectiveEntry {
                        Toast.makeText(context, "Énigme scellée. Seul celui qui sait pourra l'ouvrir.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                enabled = canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Sceller l'énigme", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspirationBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2E2E35)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Des idées de questions",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = TextPrimary
            )
            Text(
                "Ces exemples sont là pour t'inspirer. La vraie question, c'est toi qui la connais.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(InspirationData.categories) { category ->
                    val isExpanded = expandedCategory == category.title
                    
                    Card(
                        onClick = { expandedCategory = if (isExpanded) null else category.title },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E35))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.emoji, modifier = Modifier.padding(end = 12.dp))
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${category.questions.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextTertiary
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                    category.questions.forEach { question ->
                                        Card(
                                            onClick = { onSelect(question) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF242429))
                                        ) {
                                            Text(
                                                text = question,
                                                modifier = Modifier.padding(12.dp),
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Serif,
                                                    fontStyle = FontStyle.Italic,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF9B9590)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContentTypeButton(selected: Boolean, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text(label, color = BackgroundPrimary, fontSize = 12.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceCard)
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
