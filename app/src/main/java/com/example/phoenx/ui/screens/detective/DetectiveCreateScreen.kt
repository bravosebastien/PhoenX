package com.example.phoenx.ui.screens.detective

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    
    // v8.9.0 : Thème Global
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.updatePhotoUri(uri)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.isUltimateSecret) "Le Secret Ultime" else "Mode Détective", 
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        fontFamily = theme.fontFamily
                    ) 
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (uiState.isUltimateSecret) theme.contentColor.copy(alpha = 0.03f) else Color.Transparent)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // SÉLECTEUR DE TYPE (v8.9.4 : Fusion Secret Ultime)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.contentColor.copy(alpha = 0.05f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TypeSelectorButton(
                    selected = !uiState.isUltimateSecret,
                    label = "Énigme Classique",
                    icon = Icons.Default.Extension,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toggleUltimateSecret(false) },
                    theme = theme
                )
                TypeSelectorButton(
                    selected = uiState.isUltimateSecret,
                    label = "Secret Ultime",
                    icon = Icons.Default.Key,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toggleUltimateSecret(true) },
                    theme = theme
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isUltimateSecret) "Le Trésor de ton Héritage" else "Un secret protégé par une question",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = theme.contentColor
                )
                InfoButton(
                    title = if (uiState.isUltimateSecret) "Le Secret Ultime" else "Mode Détective",
                    points = if (uiState.isUltimateSecret) {
                        listOf(
                            "La pièce maîtresse de ton héritage : le secret le plus précieux et le plus intime.",
                            "Protection inviolable : aucun déblocage automatique n'est possible avec le temps.",
                            "Un moment solennel : sa lecture est présentée avec un habillage visuel unique pour tes proches.",
                            "L'unique clé : seul celui qui connaît la réponse pourra un jour briser ce dernier sceau."
                        )
                    } else {
                        listOf(
                            "Protège un souvenir par une énigme personnelle : une question dont seul ton proche possède la clé.",
                            "Sécurité absolue : la réponse est hachée sur ton appareil. Personne ne peut la déchiffrer.",
                            "Un jeu de piste intime : une fois la bonne réponse saisie, le souvenir est révélé pour toujours.",
                            "Un secret à l'épreuve du temps : contrairement à un simple message, ce contenu reste scellé tant que l'énigme n'est pas résolue.",
                            "Tape sur 'Besoin d'inspiration ?' pour voir des exemples de questions."
                        )
                    }
                )
            }
            Text(
                text = if (uiState.isUltimateSecret) 
                    "Une confidence unique, scellée au cœur de votre mémoire." 
                    else "Seul celui qui connaît la réponse pourra accéder à ce que tu déposes ici.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )

            if (uiState.isUltimateSecret) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Traitement Solennel Activé",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ÉTAPE 1 — L'ÉNIGME
            Text("LA QUESTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.enigmaText,
                onValueChange = { viewModel.updateEnigma(it) },
                placeholder = { Text("Ex : Quel était le nom de notre premier chien ?", color = theme.contentColor.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = theme.fontFamily, fontSize = 17.sp, color = theme.contentColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                    unfocusedContainerColor = theme.contentColor.copy(alpha = 0.03f),
                    focusedContainerColor = theme.contentColor.copy(alpha = 0.03f)
                ),
                maxLines = 3
            )

            // BOUTON INSPIRATION
            var showInspiration by remember { mutableStateOf(false) }
            Button(
                onClick = { showInspiration = true },
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Besoin d'inspiration ?",
                        color = accent,
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
            Text("LA RÉPONSE (invisible après saisie)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.secretAnswer,
                onValueChange = { viewModel.updateAnswer(it) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 17.sp, color = theme.contentColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                    unfocusedContainerColor = theme.contentColor.copy(alpha = 0.03f),
                    focusedContainerColor = theme.contentColor.copy(alpha = 0.03f)
                )
            )
            Text(
                "La réponse est protégée localement par une empreinte numérique SHA-256. Personne ne peut la lire, même nos serveurs.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = theme.contentColor.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ÉTAPE 2 bis — L'INDICE
            Text("INDICE (Affiché après 3 échecs)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.enigmaHint,
                onValueChange = { viewModel.updateEnigmaHint(it) },
                placeholder = { Text("Ex : C'est le nom d'un animal...", color = theme.contentColor.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 15.sp, color = theme.contentColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                    unfocusedContainerColor = theme.contentColor.copy(alpha = 0.03f),
                    focusedContainerColor = theme.contentColor.copy(alpha = 0.03f)
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // SECTION DÉLAI DE GRÂCE
            if (!uiState.isUltimateSecret) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DÉBLOCAGE AUTOMATIQUE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                    var autoUnlockEnabled by remember { mutableStateOf(uiState.autoUnlockDays.isNotEmpty()) }
                    Switch(
                        checked = autoUnlockEnabled,
                        onCheckedChange = { 
                            autoUnlockEnabled = it
                            if (!it) viewModel.updateAutoUnlockDays("")
                            else viewModel.updateAutoUnlockDays("30")
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent)
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
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = theme.contentColor.copy(alpha = 0.1f)
                        )
                    )
                    
                    Text(
                        text = "Ouvrir après ${uiState.autoUnlockDays} jours",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    Text(
                        text = "Ton proche pourra voir le contenu automatiquement après ce délai s'il ne trouve pas la réponse.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Verrouillage permanent tant que la réponse n'est pas trouvée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )
                }
            } else {
                // Info solennelle pour le Secret Ultime
                Surface(
                    color = theme.contentColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockPerson, null, tint = accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Le Secret Ultime est scellé sans limite de temps. Il ne pourra être ouvert que par la connaissance de la réponse.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("MESSAGE DE RÉVÉLATION (FACULTATIF)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.fallbackMessage,
                onValueChange = { viewModel.updateFallbackMessage(it) },
                placeholder = { Text("Ex: La réponse était [ville]. J'espère que tu t'en souviendras un jour...", color = theme.contentColor.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = theme.fontFamily, fontSize = 15.sp, fontStyle = FontStyle.Italic, color = theme.contentColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                    unfocusedContainerColor = theme.contentColor.copy(alpha = 0.03f),
                    focusedContainerColor = theme.contentColor.copy(alpha = 0.03f)
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ÉTAPE 3 — LE CONTENU
            Text("LE CONTENU SECRET", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
            Text(
                "Ce contenu ne sera accessible qu'après avoir répondu correctement à ta question.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.TEXT,
                    label = "✍️ Texte",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.TEXT) },
                    theme = theme
                )
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.PHOTO,
                    label = "📷 Photo",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.PHOTO) },
                    theme = theme
                )
                ContentTypeButton(
                    selected = uiState.contentType == ContentType.AUDIO,
                    label = "🎙️ Audio",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectContentType(ContentType.AUDIO) },
                    theme = theme
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
                            .background(theme.contentColor.copy(alpha = 0.05f))
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value = uiState.textContent,
                            onValueChange = { viewModel.updateTextContent(it) },
                            textStyle = TextStyle(fontFamily = theme.fontFamily, fontSize = 16.sp, color = theme.contentColor),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                ContentType.PHOTO -> {
                    if (uiState.photoUri == null) {
                        Button(
                            onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.contentColor.copy(alpha = 0.05f))
                        ) {
                            Text("Choisir une photo", color = accent)
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
                            color = accent,
                            onClick = { /* TODO */ }
                        ) {
                            Icon(Icons.Default.Mic, null, tint = theme.backgroundColor, modifier = Modifier.padding(20.dp))
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
                        Toast.makeText(context, "Confidence scellée.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                enabled = canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = theme.backgroundColor, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (uiState.isUltimateSecret) "Sceller le Secret Ultime" else "Sceller l'énigme", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TypeSelectorButton(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    theme: AppThemeState
) {
    Surface(
        modifier = modifier.height(44.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) theme.accentColor else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                null, 
                tint = if (selected) theme.backgroundColor else theme.contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) theme.backgroundColor else theme.contentColor.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspirationBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.contentColor.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Des idées de questions",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                color = theme.contentColor
            )
            Text(
                "Ces exemples sont là pour t'inspirer. La vraie question, c'est toi qui la connais.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f),
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
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.emoji, modifier = Modifier.padding(end = 12.dp))
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = theme.contentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${category.questions.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = theme.contentColor.copy(alpha = 0.3f)
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
                                            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
                                        ) {
                                            Text(
                                                text = question,
                                                modifier = Modifier.padding(12.dp),
                                                style = TextStyle(
                                                    fontFamily = theme.fontFamily,
                                                    fontStyle = FontStyle.Italic,
                                                    fontSize = 14.sp,
                                                    color = theme.contentColor
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
fun ContentTypeButton(selected: Boolean, label: String, modifier: Modifier = Modifier, onClick: () -> Unit, theme: AppThemeState) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
        ) {
            Text(label, color = theme.backgroundColor, fontSize = 12.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.contentColor.copy(alpha = 0.05f))
        ) {
            Text(label, color = theme.contentColor.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}
